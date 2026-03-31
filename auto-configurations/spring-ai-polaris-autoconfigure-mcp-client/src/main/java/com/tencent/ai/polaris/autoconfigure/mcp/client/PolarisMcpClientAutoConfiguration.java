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

package com.tencent.ai.polaris.autoconfigure.mcp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.autoconfigure.core.ConditionalOnPolarisEnabled;
import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.autoconfigure.core.PolarisSDKContextAutoConfiguration;
import com.tencent.ai.polaris.autoconfigure.mcp.client.PolarisMcpClientProperties.PolarisMcpParameters;
import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.ai.polaris.mcp.client.PolarisMcpAsyncClientCluster;
import com.tencent.ai.polaris.mcp.client.PolarisMcpSyncClientCluster;
import com.tencent.ai.polaris.mcp.client.tool.PolarisMcpAsyncToolCallbackProvider;
import com.tencent.ai.polaris.mcp.client.tool.PolarisMcpSyncToolCallbackProvider;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * Auto-configuration for Polaris MCP client discovery. Creates
 * {@link PolarisMcpSyncClientCluster} or {@link PolarisMcpAsyncClientCluster} instances
 * for each configured service, along with their corresponding
 * {@code ToolCallbackProvider}.
 * <p>
 * Client type (SYNC/ASYNC) is determined by {@code spring.ai.mcp.client.type} from
 * Spring AI's {@link McpClientCommonProperties}. Each service can override the global
 * namespace and scheme via its {@link PolarisMcpClientProperties.PolarisMcpParameters}.
 *
 * @author Haotian Zhang
 */
@AutoConfiguration(after = PolarisSDKContextAutoConfiguration.class)
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(prefix = "spring.ai.polaris.mcp.client", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@ConditionalOnBean(McpClientCommonProperties.class)
@EnableConfigurationProperties(PolarisMcpClientProperties.class)
public class PolarisMcpClientAutoConfiguration {

	// ---- Sync client clusters ----

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public List<PolarisMcpSyncClientCluster> polarisMcpSyncClientClusters(
			PolarisMcpClientProperties clientProperties, PolarisCoreProperties coreProperties,
			McpClientCommonProperties mcpClientProperties, PolarisSDKContextManager sdkContextManager,
			ObjectProvider<PolarisReporter> reporter, ObjectProvider<WebClient.Builder> webClientBuilder,
			ObjectProvider<ObjectMapper> objectMapper,
			ObjectProvider<List<McpSyncClientCustomizer>> customizers) {
		List<PolarisMcpSyncClientCluster> clusters = new ArrayList<>();
		for (Map.Entry<String, PolarisMcpParameters> entry : clientProperties.getServices().entrySet()) {
			String name = entry.getKey();
			PolarisMcpParameters params = entry.getValue();
			String serviceName = resolveServiceName(name, params);
			String namespace = resolveNamespace(params, clientProperties, coreProperties);
			String scheme = resolveScheme(params);
			PolarisMcpSyncClientCluster cluster = new PolarisMcpSyncClientCluster(namespace, serviceName,
					mcpClientProperties.getName(), scheme, mcpClientProperties.getVersion(),
					mcpClientProperties.isInitialized(), sdkContextManager, reporter.getIfAvailable(),
					webClientBuilder.getIfAvailable(WebClient::builder),
					objectMapper.getIfAvailable(ObjectMapper::new),
					customizers.getIfAvailable(List::of));
			cluster.initialize();
			cluster.watch();
			clusters.add(cluster);
		}
		return clusters;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
			matchIfMissing = true)
	public PolarisMcpSyncToolCallbackProvider polarisMcpSyncToolCallbackProvider(
			List<PolarisMcpSyncClientCluster> clientClusters) {
		return new PolarisMcpSyncToolCallbackProvider(clientClusters);
	}

	// ---- Async client clusters ----

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public List<PolarisMcpAsyncClientCluster> polarisMcpAsyncClientClusters(
			PolarisMcpClientProperties clientProperties, PolarisCoreProperties coreProperties,
			McpClientCommonProperties mcpClientProperties, PolarisSDKContextManager sdkContextManager,
			ObjectProvider<PolarisReporter> reporter, ObjectProvider<WebClient.Builder> webClientBuilder,
			ObjectProvider<ObjectMapper> objectMapper,
			ObjectProvider<List<McpAsyncClientCustomizer>> customizers) {
		List<PolarisMcpAsyncClientCluster> clusters = new ArrayList<>();
		for (Map.Entry<String, PolarisMcpParameters> entry : clientProperties.getServices().entrySet()) {
			String name = entry.getKey();
			PolarisMcpParameters params = entry.getValue();
			String serviceName = resolveServiceName(name, params);
			String namespace = resolveNamespace(params, clientProperties, coreProperties);
			String scheme = resolveScheme(params);
			PolarisMcpAsyncClientCluster cluster = new PolarisMcpAsyncClientCluster(namespace, serviceName,
					mcpClientProperties.getName(), scheme, mcpClientProperties.getVersion(),
					mcpClientProperties.isInitialized(), sdkContextManager, reporter.getIfAvailable(),
					webClientBuilder.getIfAvailable(WebClient::builder),
					objectMapper.getIfAvailable(ObjectMapper::new),
					customizers.getIfAvailable(List::of));
			cluster.initialize();
			cluster.watch();
			clusters.add(cluster);
		}
		return clusters;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
	public PolarisMcpAsyncToolCallbackProvider polarisMcpAsyncToolCallbackProvider(
			List<PolarisMcpAsyncClientCluster> clientClusters) {
		return new PolarisMcpAsyncToolCallbackProvider(clientClusters);
	}

	// ---- Private helpers ----

	/**
	 * Resolves the Polaris service name. Uses {@link PolarisMcpParameters#serviceName()}
	 * when set, otherwise falls back to the map key.
	 */
	private static String resolveServiceName(String name, PolarisMcpParameters params) {
		if (params != null && StringUtils.isNotBlank(params.serviceName())) {
			return params.serviceName();
		}
		return name;
	}

	/**
	 * Resolves the namespace with the following priority: per-service parameter >
	 * client-level property > core-level property.
	 */
	private static String resolveNamespace(PolarisMcpParameters params,
			PolarisMcpClientProperties clientProperties, PolarisCoreProperties coreProperties) {
		if (params != null && StringUtils.isNotBlank(params.namespace())) {
			return params.namespace();
		}
		if (StringUtils.isNotBlank(clientProperties.getNamespace())) {
			return clientProperties.getNamespace();
		}
		return coreProperties.getNamespace();
	}

	/**
	 * Resolves the scheme from the per-service parameter, falling back to
	 * {@link PolarisMcpMetadataKeys#DEFAULT_SCHEME}.
	 */
	private static String resolveScheme(PolarisMcpParameters params) {
		if (params != null && StringUtils.isNotBlank(params.scheme())) {
			return params.scheme();
		}
		return PolarisMcpMetadataKeys.DEFAULT_SCHEME;
	}

}
