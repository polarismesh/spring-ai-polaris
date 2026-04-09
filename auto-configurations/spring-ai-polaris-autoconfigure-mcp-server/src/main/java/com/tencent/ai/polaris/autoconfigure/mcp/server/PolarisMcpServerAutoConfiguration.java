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

package com.tencent.ai.polaris.autoconfigure.mcp.server;

import java.util.List;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProviderBase;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;

import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

import com.tencent.ai.polaris.autoconfigure.core.ConditionalOnPolarisEnabled;
import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.autoconfigure.core.PolarisSDKContextAutoConfiguration;
import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.ai.polaris.mcp.server.McpServerCapabilitiesProvider;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerContractReporter;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerRegistry;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * Auto-configuration for MCP Server registration with Polaris. Creates
 * {@link PolarisMcpServerRegistry}, {@link PolarisMcpServerListener},
 * {@link McpServerCapabilitiesProvider}, and {@link PolarisMcpServerContractReporter}
 * beans when Polaris and MCP server registration are enabled.
 * <p>
 * Supports three MCP server modes:
 * <ul>
 *   <li>SSE — {@link McpSyncServer} or {@link McpAsyncServer}</li>
 *   <li>Streamable HTTP — {@link McpSyncServer} or {@link McpAsyncServer}</li>
 *   <li>Stateless — {@link McpStatelessSyncServer} or {@link McpStatelessAsyncServer}</li>
 * </ul>
 * <p>
 * Note: Uses {@link McpServerAutoConfiguration.NonStatelessServerCondition} and
 * {@link McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition} from
 * Spring AI. These are public API classes also used by other extensions
 * (e.g. spring-ai-alibaba).
 *
 * @author Haotian Zhang
 */
@AutoConfiguration(after = { PolarisSDKContextAutoConfiguration.class, McpServerAutoConfiguration.class,
		McpServerStatelessAutoConfiguration.class })
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(prefix = "spring.ai.polaris.mcp.server", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@ConditionalOnBean(McpServerProperties.class)
@EnableConfigurationProperties(PolarisMcpServerProperties.class)
public class PolarisMcpServerAutoConfiguration {

	private final Environment environment;

	public PolarisMcpServerAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	// ---- SSE / Streamable (stateful) ----

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public PolarisMcpServerRegistry polarisMcpServerRegistrySync(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpServerProperties mcpServerProperties,
			McpSyncServer mcpSyncServer, McpServerTransportProviderBase transportProvider,
			ObjectProvider<McpServerSseProperties> sseProperties,
			ObjectProvider<McpServerStreamableHttpProperties> streamableHttpProperties) {
		McpSchema.Implementation serverInfo = mcpSyncServer.getServerInfo();
		return buildRegistry(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
				mcpServerProperties, serverInfo, transportProvider, sseProperties, streamableHttpProperties);
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public PolarisMcpServerRegistry polarisMcpServerRegistryAsync(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpServerProperties mcpServerProperties,
			McpAsyncServer mcpAsyncServer, McpServerTransportProviderBase transportProvider,
			ObjectProvider<McpServerSseProperties> sseProperties,
			ObjectProvider<McpServerStreamableHttpProperties> streamableHttpProperties) {
		McpSchema.Implementation serverInfo = mcpAsyncServer.getServerInfo();
		return buildRegistry(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
				mcpServerProperties, serverInfo, transportProvider, sseProperties, streamableHttpProperties);
	}

	// ---- Stateless ----

	@Bean
	@ConditionalOnBean(McpStatelessSyncServer.class)
	@Conditional(McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public PolarisMcpServerRegistry polarisMcpServerRegistryStatelessSync(
			PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpServerProperties mcpServerProperties,
			McpStatelessSyncServer mcpStatelessSyncServer, McpStatelessServerTransport statelessTransport,
			ObjectProvider<McpServerStreamableHttpProperties> streamableHttpProperties) {
		McpSchema.Implementation serverInfo = mcpStatelessSyncServer.getServerInfo();
		return buildStatelessRegistry(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
				mcpServerProperties, serverInfo, statelessTransport, streamableHttpProperties);
	}

	@Bean
	@ConditionalOnBean(McpStatelessAsyncServer.class)
	@Conditional(McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public PolarisMcpServerRegistry polarisMcpServerRegistryStatelessAsync(
			PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpServerProperties mcpServerProperties,
			McpStatelessAsyncServer mcpStatelessAsyncServer, McpStatelessServerTransport statelessTransport,
			ObjectProvider<McpServerStreamableHttpProperties> streamableHttpProperties) {
		McpSchema.Implementation serverInfo = mcpStatelessAsyncServer.getServerInfo();
		return buildStatelessRegistry(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
				mcpServerProperties, serverInfo, statelessTransport, streamableHttpProperties);
	}

	// ---- Capabilities Provider ----

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public McpServerCapabilitiesProvider mcpServerCapabilitiesProviderSync(McpSyncServer server) {
		return McpServerCapabilitiesProvider.ofSync(server::listTools, server::listResources, server::listPrompts,
				server.getServerCapabilities());
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public McpServerCapabilitiesProvider mcpServerCapabilitiesProviderAsync(McpAsyncServer server) {
		return McpServerCapabilitiesProvider.ofAsync(server::listTools, server::listResources, server::listPrompts,
				server.getServerCapabilities());
	}

	@Bean
	@ConditionalOnBean(McpStatelessSyncServer.class)
	@Conditional(McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public McpServerCapabilitiesProvider mcpServerCapabilitiesProviderStatelessSync(
			McpStatelessSyncServer server) {
		return McpServerCapabilitiesProvider.ofSync(server::listTools, server::listResources, server::listPrompts,
				server.getServerCapabilities());
	}

	@Bean
	@ConditionalOnBean(McpStatelessAsyncServer.class)
	@Conditional(McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public McpServerCapabilitiesProvider mcpServerCapabilitiesProviderStatelessAsync(
			McpStatelessAsyncServer server) {
		return McpServerCapabilitiesProvider.ofAsync(server::listTools, server::listResources, server::listPrompts,
				server.getServerCapabilities());
	}

	// ---- Contract Reporter ----

	@Bean
	@ConditionalOnBean(McpSyncServer.class)
	@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public PolarisMcpServerContractReporter polarisMcpServerContractReporterSync(
			PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties,
			McpSyncServer mcpSyncServer) {
		if (!polarisMcpServerProperties.getContract().isEnabled()) {
			return null;
		}
		return buildContractReporter(sdkContextManager, polarisCoreProperties,
				polarisMcpServerProperties, mcpSyncServer.getServerInfo());
	}

	@Bean
	@ConditionalOnBean(McpAsyncServer.class)
	@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public PolarisMcpServerContractReporter polarisMcpServerContractReporterAsync(
			PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties,
			McpAsyncServer mcpAsyncServer) {
		if (!polarisMcpServerProperties.getContract().isEnabled()) {
			return null;
		}
		return buildContractReporter(sdkContextManager, polarisCoreProperties,
				polarisMcpServerProperties, mcpAsyncServer.getServerInfo());
	}

	@Bean
	@ConditionalOnBean(McpStatelessSyncServer.class)
	@Conditional(McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public PolarisMcpServerContractReporter polarisMcpServerContractReporterStatelessSync(
			PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties,
			McpStatelessSyncServer mcpStatelessSyncServer) {
		if (!polarisMcpServerProperties.getContract().isEnabled()) {
			return null;
		}
		return buildContractReporter(sdkContextManager, polarisCoreProperties,
				polarisMcpServerProperties, mcpStatelessSyncServer.getServerInfo());
	}

	@Bean
	@ConditionalOnBean(McpStatelessAsyncServer.class)
	@Conditional(McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class)
	@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public PolarisMcpServerContractReporter polarisMcpServerContractReporterStatelessAsync(
			PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties,
			McpStatelessAsyncServer mcpStatelessAsyncServer) {
		if (!polarisMcpServerProperties.getContract().isEnabled()) {
			return null;
		}
		return buildContractReporter(sdkContextManager, polarisCoreProperties,
				polarisMcpServerProperties, mcpStatelessAsyncServer.getServerInfo());
	}

	// ---- Listener ----

	@Bean
	@ConditionalOnBean(PolarisMcpServerRegistry.class)
	public PolarisMcpServerListener polarisMcpServerListener(PolarisMcpServerRegistry registry,
			PolarisMcpServerProperties properties, PolarisCoreProperties coreProperties,
			ObjectProvider<PolarisMcpServerContractReporter> contractReporter,
			ObjectProvider<McpServerCapabilitiesProvider> capabilitiesProvider) {
		return new PolarisMcpServerListener(registry, properties, coreProperties,
				contractReporter.getIfAvailable(), capabilitiesProvider.getIfAvailable());
	}

	// ---- Private helpers ----

	private PolarisMcpServerContractReporter buildContractReporter(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties,
			McpSchema.Implementation serverInfo) {
		String serviceName = resolveServiceName(polarisMcpServerProperties);
		return new PolarisMcpServerContractReporter(sdkContextManager,
				polarisCoreProperties.getNamespace(), serviceName, serverInfo.version());
	}

	private PolarisMcpServerRegistry buildRegistry(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpServerProperties mcpServerProperties,
			McpSchema.Implementation serverInfo, McpServerTransportProviderBase transportProvider,
			ObjectProvider<McpServerSseProperties> sseProperties,
			ObjectProvider<McpServerStreamableHttpProperties> streamableHttpProperties) {
		if (transportProvider instanceof StdioServerTransportProvider) {
			return buildStdioRegistry(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
					serverInfo, transportProvider.protocolVersions());
		}

		String protocol;
		String endpointPath;
		McpServerProperties.ServerProtocol serverProtocol = mcpServerProperties.getProtocol();

		if (serverProtocol == McpServerProperties.ServerProtocol.SSE) {
			protocol = PolarisMcpMetadataKeys.PROTOCOL_MCP_SSE;
			McpServerSseProperties sse = sseProperties.getIfAvailable();
			if (sse == null) {
				throw new IllegalStateException(
						"McpServerSseProperties is required for SSE protocol"
								+ " but not found in the application context.");
			}
			endpointPath = sse.getSseEndpoint();
		}
		else {
			protocol = PolarisMcpMetadataKeys.PROTOCOL_MCP_STREAMABLE_HTTP;
			McpServerStreamableHttpProperties streamable = streamableHttpProperties.getIfAvailable();
			if (streamable == null) {
				throw new IllegalStateException(
						"McpServerStreamableHttpProperties is required for Streamable HTTP protocol"
								+ " but not found in the application context.");
			}
			endpointPath = streamable.getMcpEndpoint();
		}

		String mcpProtocolVersion = resolveProtocolVersion(transportProvider.protocolVersions());
		return doBuild(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
				protocol, endpointPath, serverInfo.version(), mcpProtocolVersion);
	}

	private PolarisMcpServerRegistry buildStatelessRegistry(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpServerProperties mcpServerProperties,
			McpSchema.Implementation serverInfo, McpStatelessServerTransport statelessTransport,
			ObjectProvider<McpServerStreamableHttpProperties> streamableHttpProperties) {
		McpServerStreamableHttpProperties streamable = streamableHttpProperties.getIfAvailable();
		if (streamable == null) {
			throw new IllegalStateException(
					"McpServerStreamableHttpProperties is required for Stateless protocol"
							+ " but not found in the application context.");
		}

		String mcpProtocolVersion = resolveProtocolVersion(statelessTransport.protocolVersions());
		return doBuild(sdkContextManager, polarisCoreProperties, polarisMcpServerProperties,
				PolarisMcpMetadataKeys.PROTOCOL_MCP_STREAMABLE_HTTP, streamable.getMcpEndpoint(),
				serverInfo.version(), mcpProtocolVersion);
	}

	private PolarisMcpServerRegistry buildStdioRegistry(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties, McpSchema.Implementation serverInfo,
			List<String> protocolVersions) {
		String serviceName = resolveServiceName(polarisMcpServerProperties);
		String mcpProtocolVersion = resolveProtocolVersion(protocolVersions);
		return PolarisMcpServerRegistry.builder()
			.sdkContextManager(sdkContextManager)
			.namespace(polarisCoreProperties.getNamespace())
			.serviceName(serviceName)
			.protocol(PolarisMcpMetadataKeys.PROTOCOL_MCP_STDIO)
			.version(serverInfo.version())
			.protocolVersion(mcpProtocolVersion)
			.strictCompatible(polarisMcpServerProperties.isStrictCompatible())
			.build();
	}

	private PolarisMcpServerRegistry doBuild(PolarisSDKContextManager sdkContextManager,
			PolarisCoreProperties polarisCoreProperties,
			PolarisMcpServerProperties polarisMcpServerProperties,
			String protocol, String endpointPath, String version, String protocolVersion) {
		String contextPath = resolveContextPath();
		String fullEndpointPath = contextPath + endpointPath;
		String serviceName = resolveServiceName(polarisMcpServerProperties);
		return PolarisMcpServerRegistry.builder()
			.sdkContextManager(sdkContextManager)
			.namespace(polarisCoreProperties.getNamespace())
			.serviceName(serviceName)
			.protocol(protocol)
			.endpointPath(fullEndpointPath)
			.version(version)
			.protocolVersion(protocolVersion)
			.strictCompatible(polarisMcpServerProperties.isStrictCompatible())
			.build();
	}

	/**
	 * Resolves the context path from servlet or webflux environment properties. For
	 * servlet-based applications, reads {@code server.servlet.context-path}. For
	 * WebFlux-based applications, reads {@code spring.webflux.base-path}.
	 * @return the context path or empty string if not configured
	 */
	private String resolveContextPath() {
		String servletContextPath = this.environment.getProperty("server.servlet.context-path", "");
		if (StringUtils.isNotBlank(servletContextPath)) {
			return servletContextPath;
		}
		return this.environment.getProperty("spring.webflux.base-path", "");
	}

	/**
	 * Resolves the MCP server service name. If
	 * {@code spring.ai.polaris.mcp.server.service-name} is not set, falls back to
	 * {@code spring.application.name}.
	 * @param properties the MCP server properties
	 * @return the resolved service name
	 * @throws IllegalStateException if no service name can be resolved
	 */
	private String resolveServiceName(PolarisMcpServerProperties properties) {
		String serviceName = properties.getServiceName();
		if (StringUtils.isNotBlank(serviceName)) {
			return serviceName;
		}
		String appName = this.environment.getProperty("spring.application.name");
		if (StringUtils.isNotBlank(appName)) {
			return appName;
		}
		throw new IllegalStateException(
				"MCP server service name is not configured. Set 'spring.ai.polaris.mcp.server.service-name'"
						+ " or 'spring.application.name'.");
	}

	/**
	 * Resolves the MCP protocol versions from the transport provider's supported versions.
	 * Joins all versions with comma separator.
	 * @param versions the protocol versions supported by the transport
	 * @return comma-separated protocol versions, or {@code null} if the list is empty
	 */
	private String resolveProtocolVersion(List<String> versions) {
		if (CollectionUtils.isEmpty(versions)) {
			return null;
		}
		return String.join(",", versions);
	}

}
