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
import com.tencent.ai.polaris.core.reporter.PolarisCallContext;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest.UnWatchServiceRequestBuilder;
import com.tencent.polaris.api.rpc.WatchServiceRequest;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.Node;

/**
 * Base class for Polaris-backed MCP clients. Manages a connection pool of MCP client
 * instances (one per healthy Polaris instance) keyed by {@code host:port}, with
 * round-robin selection, dynamic watch, and Polaris call reporting.
 * <p>
 * Subclasses provide the concrete client type ({@code McpSyncClient} or
 * {@code McpAsyncClient}) by implementing the three template methods:
 * {@link #buildClient(McpClientTransport, McpSchema.Implementation)},
 * {@link #closeClient(Object)}, and {@link #closeClientGracefully(Object)}.
 *
 * @param <C> the MCP client type
 * @author Haotian Zhang
 */
public abstract class AbstractPolarisMcpClient<C> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPolarisMcpClient.class);

	/** Return code indicating a successful call. */
	protected static final int RET_CODE_SUCCESS = 0;

	/** Return code indicating a failed call. */
	protected static final int RET_CODE_FAIL = -1;

	private final String namespace;

	private final String serverName;

	private final String scheme;

	private final String clientVersion;

	private final boolean initialized;

	private final PolarisSDKContextManager sdkContextManager;

	private final PolarisReporter polarisReporter;

	private final WebClient.Builder webClientBuilder;

	private final JacksonMcpJsonMapper jsonMapper;

	private final ConcurrentHashMap<String, C> keyToClientMap = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, Node> clientNameToNode = new ConcurrentHashMap<>();

	private final AtomicInteger index = new AtomicInteger(0);

	protected AbstractPolarisMcpClient(String namespace, String serverName, String scheme, String clientVersion,
			boolean initialized, PolarisSDKContextManager sdkContextManager, PolarisReporter polarisReporter,
			WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
		this.namespace = namespace;
		this.serverName = serverName;
		this.scheme = scheme;
		this.clientVersion = clientVersion;
		this.initialized = initialized;
		this.sdkContextManager = sdkContextManager;
		this.polarisReporter = polarisReporter;
		this.webClientBuilder = webClientBuilder;
		this.jsonMapper = new JacksonMcpJsonMapper(objectMapper);
	}

	/**
	 * Build a concrete MCP client from the given transport and client info.
	 * @param clientName the client name used for customizer matching
	 * @param transport the MCP transport
	 * @param clientInfo the client implementation info
	 * @param initialized whether to initialize the client immediately
	 * @return the client instance
	 */
	protected abstract C buildClient(String clientName, McpClientTransport transport,
			McpSchema.Implementation clientInfo, boolean initialized);

	/**
	 * Close a client immediately.
	 * @param client the client to close
	 */
	protected abstract void closeClient(C client);

	/**
	 * Close a client gracefully.
	 * @param client the client to close
	 */
	protected abstract void closeClientGracefully(C client);

	/**
	 * Returns the namespace this client operates in.
	 * @return the namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Returns the Polaris service name this client is responsible for.
	 * @return the service name
	 */
	public String getServerName() {
		return this.serverName;
	}

	/**
	 * Returns the internal client pool. Visible to subclasses for type-specific
	 * operations.
	 * @return the client pool
	 */
	protected ConcurrentHashMap<String, C> getKeyToClientMap() {
		return this.keyToClientMap;
	}

	/**
	 * Returns the client name to node mapping. Visible to subclasses for cleanup.
	 * @return the client name to node map
	 */
	protected ConcurrentHashMap<String, Node> getClientNameToNode() {
		return this.clientNameToNode;
	}

	/**
	 * Fetches healthy instances from Polaris, builds client connections, and returns the
	 * resulting client pool keyed by {@code host:port}.
	 * @return an immutable copy of the client pool
	 */
	public Map<String, C> initialize() {
		List<Instance> instances = fetchHealthyInstances();
		for (Instance instance : instances) {
			String key = instanceKey(instance);
			this.keyToClientMap.computeIfAbsent(key, k -> createClient(instance));
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
				List<String> added = new ArrayList<>();
				List<String> removed = new ArrayList<>();
				List<String> updated = new ArrayList<>();
				// Add new instances
				if (CollectionUtils.isNotEmpty(event.getAddInstances())) {
					for (Instance inst : event.getAddInstances()) {
						String key = instanceKey(inst);
						this.keyToClientMap.computeIfAbsent(key, k -> createClient(inst));
						added.add(key);
					}
				}
				// Remove deleted instances
				if (CollectionUtils.isNotEmpty(event.getDeleteInstances())) {
					for (Instance inst : event.getDeleteInstances()) {
						String key = instanceKey(inst);
						C old = this.keyToClientMap.remove(key);
						if (old != null) {
							closeClientGracefully(old);
						}
						this.clientNameToNode.remove(clientName(inst));
						removed.add(key);
					}
				}
				// Update changed instances: remove old, add new
				if (CollectionUtils.isNotEmpty(event.getUpdateInstances())) {
					event.getUpdateInstances().forEach(update -> {
						String beforeKey = instanceKey(update.getBefore());
						C old = this.keyToClientMap.remove(beforeKey);
						if (old != null) {
							closeClientGracefully(old);
						}
						this.clientNameToNode.remove(clientName(update.getBefore()));
						String afterKey = instanceKey(update.getAfter());
						this.keyToClientMap.computeIfAbsent(afterKey, k -> createClient(update.getAfter()));
						updated.add(beforeKey + "->" + afterKey);
					});
				}
				logger.info("[Polaris MCP Client] Watch event for service={}: added={}, removed={}, updated={},"
						+ " pool size={}", this.serverName, added, removed, updated,
						this.keyToClientMap.size());
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
			String key = instanceKey(instance);
			this.keyToClientMap.computeIfAbsent(key, k -> createClient(instance));
		}
		logger.info("[Polaris MCP Client] Watch registered for service={}. pool size={}", this.serverName,
				this.keyToClientMap.size());
	}

	/**
	 * Returns a healthy client using round-robin selection.
	 * @return a live client
	 * @throws IllegalStateException if no clients are available
	 */
	public C getClient() {
		List<C> clients = new ArrayList<>(this.keyToClientMap.values());
		if (CollectionUtils.isEmpty(clients)) {
			throw new IllegalStateException(
					"[Polaris MCP Client] No client available for service=" + this.serverName);
		}
		int idx = (this.index.getAndIncrement() & Integer.MAX_VALUE) % clients.size();
		return clients.get(idx);
	}

	/**
	 * Closes all client connections immediately and cancels the Polaris watch.
	 */
	public void close() {
		doClose(this::closeClient);
	}

	/**
	 * Closes all client connections gracefully and cancels the Polaris watch.
	 */
	public void closeGracefully() {
		doClose(this::closeClientGracefully);
	}

	/**
	 * Reports a service call result to Polaris.
	 * @param clientName the client name used to look up host and port
	 * @param method the tool method name
	 * @param delay the call duration in milliseconds
	 * @param retStatus the return status
	 */
	protected void reportCall(String clientName, String method, long delay, RetStatus retStatus) {
		int retCode = (retStatus == RetStatus.RetSuccess) ? RET_CODE_SUCCESS : RET_CODE_FAIL;
		reportCall(clientName, method, delay, retCode, retStatus);
	}

	/**
	 * Reports a service call result to Polaris.
	 * @param clientName the client name used to look up host and port
	 * @param method the tool method name
	 * @param delay the call duration in milliseconds
	 * @param retCode the return code (0 for success, -1 for failure)
	 * @param retStatus the return status
	 */
	protected void reportCall(String clientName, String method, long delay, int retCode, RetStatus retStatus) {
		if (this.polarisReporter == null) {
			return;
		}
		Node node = this.clientNameToNode.get(clientName);
		if (node == null) {
			return;
		}
		this.polarisReporter.report(new PolarisCallContext(this.namespace, this.serverName, node.getHost(),
				node.getPort(), method, delay, retCode, retStatus));
	}

	/**
	 * Returns {@code true} if the given result indicates an error.
	 * @param result the call tool result (may be {@code null})
	 * @return whether the result is an error
	 */
	public static boolean isErrorResult(McpSchema.CallToolResult result) {
		return result != null && result.isError() != null && result.isError();
	}

	// ---- private helpers ----

	private void doClose(Consumer<C> closer) {
		unwatch();
		this.keyToClientMap.values().forEach(closer);
		this.keyToClientMap.clear();
		this.clientNameToNode.clear();
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

	private C createClient(Instance instance) {
		String protocol = instance.getProtocol();
		String endpointPath = resolveEndpointPath(instance);
		String baseUrl = this.scheme + "://" + instance.getHost() + ":" + instance.getPort();

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

		String clientName = clientName(instance, endpointPath);
		McpSchema.Implementation clientInfo = new McpSchema.Implementation(clientName, this.clientVersion);
		this.clientNameToNode.put(clientName, new Node(instance.getHost(), instance.getPort()));

		C client = buildClient(clientName, transport, clientInfo, this.initialized);
		logger.info("[Polaris MCP Client] Built client for {}:{}", instance.getHost(), instance.getPort());
		return client;
	}

	private static String instanceKey(Instance instance) {
		return instance.getHost() + ":" + instance.getPort();
	}

	private String clientName(Instance instance) {
		return clientName(instance, resolveEndpointPath(instance));
	}

	private String clientName(Instance instance, String endpointPath) {
		return this.namespace + "_" + this.serverName + "_" + instance.getHost() + ":" + instance.getPort()
				+ endpointPath;
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

}
