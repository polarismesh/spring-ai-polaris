/*
 * Tencent is pleased to support the open source community by making spring-ai-polaris available.
 *
 * Copyright (C) 2025 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.ai.polaris.mcp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest.UnWatchServiceRequestBuilder;
import com.tencent.polaris.api.rpc.WatchServiceRequest;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.Node;

/**
 * Base class for a connection pool of Polaris-backed MCP client wrappers. Manages a pool
 * of {@link AbstractPolarisMcpClient} instances (one per healthy Polaris instance) keyed
 * by {@link Node}, with round-robin selection and dynamic watch.
 * <p>
 * Subclasses provide the concrete client wrapper type by implementing
 * {@link #createClientWrapper(McpClientTransport, McpSchema.Implementation, String, Node, boolean)}.
 *
 * @param <T> the concrete single-connection wrapper type
 * @author Haotian Zhang
 */
public abstract class AbstractPolarisMcpClientCluster<T extends AbstractPolarisMcpClient<?>> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPolarisMcpClientCluster.class);

	private final String namespace;

	private final String serverName;

	private final String clientName;

	private final String scheme;

	private final String clientVersion;

	private final boolean initialized;

	private final PolarisSDKContextManager sdkContextManager;

	private final PolarisReporter polarisReporter;

	private final WebClient.Builder webClientBuilder;

	private final JacksonMcpJsonMapper jsonMapper;

	private final ConcurrentHashMap<Node, T> keyToClientMap = new ConcurrentHashMap<>();

	private final AtomicInteger index = new AtomicInteger(0);

	protected AbstractPolarisMcpClientCluster(String namespace, String serverName, String clientName, String scheme,
			String clientVersion, boolean initialized, PolarisSDKContextManager sdkContextManager,
			PolarisReporter polarisReporter, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
		this.namespace = namespace;
		this.serverName = serverName;
		this.clientName = clientName;
		this.scheme = scheme;
		this.clientVersion = clientVersion;
		this.initialized = initialized;
		this.sdkContextManager = sdkContextManager;
		this.polarisReporter = polarisReporter;
		this.webClientBuilder = webClientBuilder;
		this.jsonMapper = new JacksonMcpJsonMapper(objectMapper);
	}

	/**
	 * Create a single-connection wrapper for the given transport and node.
	 * @param transport the MCP transport
	 * @param clientInfo the client implementation info
	 * @param connectedName the connected name identifying this connection
	 * @param node the remote node
	 * @param initialized whether to initialize the client immediately
	 * @return the wrapper instance
	 */
	protected abstract T createClientWrapper(McpClientTransport transport, McpSchema.Implementation clientInfo,
			String connectedName, Node node, boolean initialized);

	/**
	 * Returns the namespace this cluster operates in.
	 * @return the namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Returns the Polaris service name this cluster is responsible for.
	 * @return the service name
	 */
	public String getServerName() {
		return this.serverName;
	}

	/**
	 * Returns the internal client pool. Visible for testing.
	 * @return the client pool
	 */
	protected ConcurrentHashMap<Node, T> getKeyToClientMap() {
		return this.keyToClientMap;
	}

	/**
	 * Returns a healthy client wrapper using round-robin selection.
	 * @return a live client wrapper
	 * @throws IllegalStateException if no clients are available
	 */
	public T getClient() {
		List<T> clients = new ArrayList<>(this.keyToClientMap.values());
		if (CollectionUtils.isEmpty(clients)) {
			throw new IllegalStateException(
					"[Polaris MCP Client] No client available for service=" + this.serverName);
		}
		int idx = (this.index.getAndIncrement() & Integer.MAX_VALUE) % clients.size();
		return clients.get(idx);
	}

	/**
	 * Fetches healthy instances from Polaris, builds client connections, and returns the
	 * resulting client pool keyed by {@link Node}.
	 * @return an immutable copy of the client pool
	 */
	public Map<Node, T> initialize() {
		List<Instance> instances = fetchHealthyInstances();
		for (Instance instance : instances) {
			Node key = instanceNode(instance);
			this.keyToClientMap.computeIfAbsent(key, k -> createWrapper(instance, key));
		}
		logger.info("[Polaris MCP Client] Initialized {} clients for service={}", this.keyToClientMap.size(),
				this.serverName);
		return Map.copyOf(this.keyToClientMap);
	}

	/**
	 * Registers a Polaris {@code ServiceListener} and performs a final
	 * {@code getHealthyInstances} sweep to cover the race window between watch
	 * registration and the initial fetch.
	 */
	public void watch() {
		WatchServiceRequest watchReq = WatchServiceRequest.builder()
			.namespace(this.namespace)
			.service(this.serverName)
			.listeners(List.of(event -> {
				try {
					List<Node> added = new ArrayList<>();
					List<Node> removed = new ArrayList<>();
					List<String> updated = new ArrayList<>();
					// Add new instances
					if (CollectionUtils.isNotEmpty(event.getAddInstances())) {
						for (Instance inst : event.getAddInstances()) {
							if (!inst.isHealthy()) {
								continue;
							}
							Node key = instanceNode(inst);
							this.keyToClientMap.computeIfAbsent(key, k -> createWrapper(inst, key));
							added.add(key);
						}
					}
					// Remove deleted instances
					if (CollectionUtils.isNotEmpty(event.getDeleteInstances())) {
						for (Instance inst : event.getDeleteInstances()) {
							Node key = instanceNode(inst);
							T old = this.keyToClientMap.remove(key);
							if (old != null) {
								old.closeClientGracefully();
							}
							removed.add(key);
						}
					}
					// Update changed instances: remove old, add new
					if (CollectionUtils.isNotEmpty(event.getUpdateInstances())) {
						event.getUpdateInstances().forEach(update -> {
							Node beforeKey = instanceNode(update.getBefore());
							T old = this.keyToClientMap.remove(beforeKey);
							if (old != null) {
								old.closeClientGracefully();
							}
							if (update.getAfter().isHealthy()) {
								Node afterKey = instanceNode(update.getAfter());
								this.keyToClientMap.computeIfAbsent(afterKey,
										k -> createWrapper(update.getAfter(), afterKey));
								updated.add(beforeKey + "->" + afterKey);
							}
							else {
								updated.add(beforeKey + "->removed(unhealthy)");
							}
						});
					}
					logger.info("[Polaris MCP Client] Watch event for service={}: added={}, removed={}, updated={},"
							+ " pool size={}", this.serverName, added, removed, updated,
							this.keyToClientMap.size());
				}
				catch (Exception ex) {
					logger.warn("[Polaris MCP Client] Error processing watch event for service={}",
							this.serverName, ex);
				}
			}))
			.build();
		try {
			this.sdkContextManager.getConsumerAPI().watchService(watchReq);
		}
		catch (PolarisException ex) {
			logger.warn("[Polaris MCP Client] Failed to register watch for service={}", this.serverName, ex);
		}
		// Race-window sweep: add any instances that appeared between init() and
		// watchService()
		for (Instance instance : fetchHealthyInstances()) {
			Node key = instanceNode(instance);
			this.keyToClientMap.computeIfAbsent(key, k -> createWrapper(instance, key));
		}
		logger.info("[Polaris MCP Client] Watch registered for service={}. pool size={}", this.serverName,
				this.keyToClientMap.size());
	}

	/**
	 * Closes all client connections immediately and cancels the Polaris watch.
	 */
	public void close() {
		doClose(T::closeClient);
	}

	/**
	 * Closes all client connections gracefully and cancels the Polaris watch.
	 */
	public void closeGracefully() {
		doClose(T::closeClientGracefully);
	}

	// ---- private helpers ----

	private void doClose(Consumer<T> closer) {
		unwatch();
		this.keyToClientMap.values().forEach(wrapper -> {
			try {
				closer.accept(wrapper);
			}
			catch (Exception ex) {
				logger.warn("[Polaris MCP Client] Failed to close client for service={}", this.serverName, ex);
			}
		});
		this.keyToClientMap.clear();
	}

	private void unwatch() {
		try {
			UnWatchServiceRequest unWatchReq = UnWatchServiceRequestBuilder.anUnWatchServiceRequest()
				.namespace(this.namespace)
				.service(this.serverName)
				.removeAll(true)
				.build();
			this.sdkContextManager.getConsumerAPI().unWatchService(unWatchReq);
		}
		catch (Exception ex) {
			logger.warn("[Polaris MCP Client] Failed to unwatch service={}", this.serverName, ex);
		}
	}

	private List<Instance> fetchHealthyInstances() {
		GetHealthyInstancesRequest req = new GetHealthyInstancesRequest();
		req.setNamespace(this.namespace);
		req.setService(this.serverName);
		try {
			InstancesResponse response = this.sdkContextManager.getConsumerAPI().getHealthyInstances(req);
			if (response == null || CollectionUtils.isEmpty(response.getInstances())) {
				return List.of();
			}
			return List.of(response.getInstances());
		}
		catch (PolarisException ex) {
			logger.warn("[Polaris MCP Client] Failed to fetch healthy instances for service={}", this.serverName, ex);
			return List.of();
		}
	}

	private T createWrapper(Instance instance, Node node) {
		String protocol = instance.getProtocol();
		String endpointPath = resolveEndpointPath(instance);
		String baseUrl = this.scheme + "://" + node.getHost() + ":" + node.getPort();

		WebClient.Builder builder = this.webClientBuilder.clone().baseUrl(baseUrl);
		McpClientTransport transport;

		if (PolarisMcpMetadataKeys.PROTOCOL_MCP_STREAMABLE_HTTP.equals(protocol)) {
			transport = WebClientStreamableHttpTransport.builder(builder)
				.endpoint(endpointPath)
				.jsonMapper(this.jsonMapper)
				.build();
		}
		else {
			// SSE (default)
			transport = WebFluxSseClientTransport.builder(builder)
				.sseEndpoint(endpointPath)
				.jsonMapper(this.jsonMapper)
				.build();
		}

		String connectedName = connectedName(instance, endpointPath);
		McpSchema.Implementation clientInfo = new McpSchema.Implementation(connectedName, this.clientVersion);

		T wrapper = createClientWrapper(transport, clientInfo, connectedName, node, this.initialized);
		logger.info("[Polaris MCP Client] Built client for {}:{}", node.getHost(), node.getPort());
		return wrapper;
	}

	private static Node instanceNode(Instance instance) {
		return new Node(instance.getHost(), instance.getPort());
	}

	private String connectedName(Instance instance, String endpointPath) {
		return this.clientName + "_" + this.namespace + "_" + this.serverName + "_" + instance.getHost() + ":"
				+ instance.getPort() + endpointPath;
	}

	private static String resolveEndpointPath(Instance instance) {
		String explicit = instance.getMetadata().get(PolarisMcpMetadataKeys.ENDPOINT_PATH);
		if (StringUtils.isNotBlank(explicit)) {
			return explicit;
		}
		if (PolarisMcpMetadataKeys.PROTOCOL_MCP_STREAMABLE_HTTP.equals(instance.getProtocol())) {
			return PolarisMcpMetadataKeys.DEFAULT_STREAMABLE_HTTP_ENDPOINT_PATH;
		}
		return PolarisMcpMetadataKeys.DEFAULT_SSE_ENDPOINT_PATH;
	}

	/**
	 * Returns the Polaris reporter. Visible to subclasses for creating wrappers.
	 * @return the Polaris reporter (may be {@code null})
	 */
	protected PolarisReporter getPolarisReporter() {
		return this.polarisReporter;
	}

}
