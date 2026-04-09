/*
 * Tencent is pleased to support the open source community by making spring-ai-polaris available.
 *
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.ai.polaris.mcp.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesResponse;

/**
 * Registers and deregisters an MCP server instance with Polaris service discovery.
 * <p>
 * This is a plain POJO with no Spring dependency. It delegates to the Polaris
 * {@code ProviderAPI} for registration and {@code ConsumerAPI} for compatibility
 * checks. Use {@link #builder()} to create instances.
 *
 * @author Haotian Zhang
 */
public final class PolarisMcpServerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(PolarisMcpServerRegistry.class);

	private final PolarisSDKContextManager sdkContextManager;

	private final String namespace;

	private final String serviceName;

	private final String protocol;

	private final String endpointPath;

	private final String version;

	private final String protocolVersion;

	private final boolean strictCompatible;

	private volatile String registeredInstanceId;

	private volatile String registeredHost;

	private volatile int registeredPort;

	private PolarisMcpServerRegistry(Builder builder) {
		this.sdkContextManager = builder.sdkContextManager;
		this.namespace = builder.namespace;
		this.serviceName = builder.serviceName;
		this.protocol = builder.protocol;
		this.endpointPath = builder.endpointPath;
		this.version = builder.version;
		this.protocolVersion = builder.protocolVersion;
		this.strictCompatible = builder.strictCompatible;
	}

	/**
	 * Create a new {@link Builder} instance.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Return the transport protocol of this registry.
	 * @return the protocol string
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Return the endpoint path of this registry.
	 * @return the endpoint path, or {@code null} if not set (e.g. for stdio transport)
	 */
	public String getEndpointPath() {
		return this.endpointPath;
	}

	/**
	 * Register this MCP server instance with Polaris.
	 * @param host the host address of this server
	 * @param port the port of this server
	 */
	public void register(String host, int port) {
		checkCompatible();

		Map<String, String> metadata = buildMetadata();

		InstanceRegisterRequest request = new InstanceRegisterRequest();
		request.setNamespace(this.namespace);
		request.setService(this.serviceName);
		request.setHost(host);
		request.setPort(port);
		request.setProtocol(this.protocol);
		request.setVersion(this.version);
		request.setMetadata(metadata);
		request.setAutoHeartbeat(true);

		InstanceRegisterResponse response = this.sdkContextManager.getProviderAPI().registerInstance(request);
		this.registeredInstanceId = response.getInstanceId();
		this.registeredHost = host;
		this.registeredPort = port;
		PolarisSDKContextManager.setRegistered(true);

		logger.info("MCP server registered with Polaris: instanceId={}, namespace={}, service={},"
				+ " host={}, port={}, protocol={}, metadata={}",
				this.registeredInstanceId, this.namespace, this.serviceName,
				host, port, this.protocol, metadata);
	}

	/**
	 * Deregister this MCP server instance from Polaris.
	 */
	public void deregister() {
		if (this.registeredInstanceId == null || !PolarisSDKContextManager.isRegistered()) {
			logger.debug("MCP server was not registered, skipping deregistration.");
			return;
		}

		try {
			InstanceDeregisterRequest request = new InstanceDeregisterRequest();
			request.setNamespace(this.namespace);
			request.setService(this.serviceName);
			request.setHost(this.registeredHost);
			request.setPort(this.registeredPort);

			this.sdkContextManager.getProviderAPI().deRegister(request);
			PolarisSDKContextManager.setRegistered(false);

			logger.info("MCP server deregistered from Polaris: instanceId={}, namespace={}, service={},"
					+ " host={}, port={}",
					this.registeredInstanceId, this.namespace, this.serviceName,
					this.registeredHost, this.registeredPort);
		}
		finally {
			this.registeredInstanceId = null;
		}
	}

	/**
	 * Check compatibility with the first existing healthy instance. If strict mode is
	 * enabled, throws an exception on incompatible versions.
	 */
	private void checkCompatible() {
		try {
			GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
			request.setNamespace(this.namespace);
			request.setService(this.serviceName);

			InstancesResponse response = this.sdkContextManager.getConsumerAPI().getHealthyInstances(request);
			Instance[] instances = response.getInstances();

			if (instances == null || instances.length == 0) {
				logger.debug("No existing instances found for service={}, skipping compatibility check.",
						this.serviceName);
				return;
			}

			Instance instance = instances[0];
			Map<String, String> metadata = instance.getMetadata();
			String existingProtocol = instance.getProtocol();

			if (existingProtocol != null && !existingProtocol.equals(this.protocol)) {
				String message = String.format(
						"Transport protocol mismatch: existing=%s, registering=%s (service=%s)",
						existingProtocol, this.protocol, this.serviceName);
				if (this.strictCompatible) {
					logger.error(message);
					throw new IllegalStateException(message);
				}
				logger.warn(message);
			}

			if (metadata != null) {
				String existingVersion = metadata.get(PolarisMcpMetadataKeys.PROTOCOL_VERSION);
				if (existingVersion != null && !hasCommonVersion(existingVersion, this.protocolVersion)) {
					String message = String.format(
							"Protocol version mismatch: existing=%s, registering=%s (service=%s)", existingVersion,
							this.protocolVersion, this.serviceName);
					if (this.strictCompatible) {
						logger.error(message);
						throw new IllegalStateException(message);
					}
					logger.warn(message);
				}
			}
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Exception ex) {
			logger.warn("Failed to check compatibility with existing instances, proceeding with registration.", ex);
		}
	}

	private Map<String, String> buildMetadata() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put(PolarisMcpMetadataKeys.SERVER_NAME, this.serviceName);
		if (this.protocolVersion != null) {
			metadata.put(PolarisMcpMetadataKeys.PROTOCOL_VERSION, this.protocolVersion);
		}
		if (this.endpointPath != null) {
			metadata.put(PolarisMcpMetadataKeys.ENDPOINT_PATH, this.endpointPath);
		}
		return metadata;
	}

	/**
	 * Checks whether two comma-separated version strings share at least one common
	 * version.
	 * @param versions1 the first comma-separated version string
	 * @param versions2 the second comma-separated version string
	 * @return {@code true} if there is at least one common version
	 */
	private boolean hasCommonVersion(String versions1, String versions2) {
		if (versions1 == null || versions2 == null) {
			return false;
		}
		Set<String> set1 = Arrays.stream(versions1.split(","))
			.map(String::trim)
			.collect(Collectors.toSet());
		return Arrays.stream(versions2.split(","))
			.map(String::trim)
			.anyMatch(set1::contains);
	}

	/**
	 * Builder for {@link PolarisMcpServerRegistry}.
	 */
	public static final class Builder {

		private PolarisSDKContextManager sdkContextManager;

		private String namespace;

		private String serviceName;

		private String protocol;

		private String endpointPath;

		private String version;

		private String protocolVersion;

		private boolean strictCompatible;

		private Builder() {
		}

		/**
		 * Set the Polaris SDK context manager.
		 * @param sdkContextManager the SDK context manager
		 * @return this builder
		 */
		public Builder sdkContextManager(PolarisSDKContextManager sdkContextManager) {
			this.sdkContextManager = sdkContextManager;
			return this;
		}

		/**
		 * Set the Polaris namespace.
		 * @param namespace the namespace
		 * @return this builder
		 */
		public Builder namespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Set the Polaris service name.
		 * @param serviceName the service name
		 * @return this builder
		 */
		public Builder serviceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		/**
		 * Set the MCP transport protocol.
		 * @param protocol the protocol value
		 * @return this builder
		 */
		public Builder protocol(String protocol) {
			this.protocol = protocol;
			return this;
		}

		/**
		 * Set the MCP endpoint path.
		 * @param endpointPath the endpoint path
		 * @return this builder
		 */
		public Builder endpointPath(String endpointPath) {
			this.endpointPath = endpointPath;
			return this;
		}

		/**
		 * Set the MCP server version (set on Polaris instance version field).
		 * @param version the server version
		 * @return this builder
		 */
		public Builder version(String version) {
			this.version = version;
			return this;
		}

		/**
		 * Set the MCP protocol version (stored in instance metadata).
		 * @param protocolVersion the protocol version
		 * @return this builder
		 */
		public Builder protocolVersion(String protocolVersion) {
			this.protocolVersion = protocolVersion;
			return this;
		}

		/**
		 * Set whether strict compatibility mode is enabled.
		 * @param strictCompatible true for strict mode
		 * @return this builder
		 */
		public Builder strictCompatible(boolean strictCompatible) {
			this.strictCompatible = strictCompatible;
			return this;
		}

		/**
		 * Build a new {@link PolarisMcpServerRegistry} instance.
		 * @return the registry instance
		 * @throws NullPointerException if required fields are not set
		 */
		public PolarisMcpServerRegistry build() {
			Objects.requireNonNull(this.sdkContextManager, "sdkContextManager must not be null");
			Objects.requireNonNull(this.namespace, "namespace must not be null");
			Objects.requireNonNull(this.serviceName, "serviceName must not be null");
			Objects.requireNonNull(this.protocol, "protocol must not be null");
			return new PolarisMcpServerRegistry(this);
		}

	}

}
