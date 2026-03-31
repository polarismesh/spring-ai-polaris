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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.mcp.client.PolarisMcpSyncClientCluster;
import com.tencent.ai.polaris.mcp.client.tool.PolarisMcpAsyncToolCallbackProvider;
import com.tencent.ai.polaris.mcp.client.tool.PolarisMcpSyncToolCallbackProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisMcpClientAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpClientAutoConfiguration")
class PolarisMcpClientAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisMcpClientAutoConfiguration.class));

	@DisplayName("beans are not created when polaris mcp client is disabled")
	@Test
	void testBeansNotCreatedWhenDisabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.mcp.client.enabled=false")
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpSyncToolCallbackProvider.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpAsyncToolCallbackProvider.class);
			});
	}

	@DisplayName("beans are not created when polaris is globally disabled")
	@Test
	void testBeansNotCreatedWhenPolarisDisabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.enabled=false")
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpSyncToolCallbackProvider.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpAsyncToolCallbackProvider.class);
			});
	}

	@DisplayName("beans are not created when McpClientCommonProperties bean is absent")
	@Test
	void testBeansNotCreatedWithoutMcpClientCommonProperties() {
		// Arrange & Act & Assert
		this.contextRunner
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpSyncToolCallbackProvider.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpAsyncToolCallbackProvider.class);
			});
	}

	@DisplayName("properties bean is registered when McpClientCommonProperties is present")
	@Test
	void testPropertiesBeanRegistered() {
		// Arrange & Act & Assert
		this.contextRunner
			.withBean(McpClientCommonProperties.class, McpClientCommonProperties::new)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> mock(PolarisSDKContextManager.class))
			.run(ctx -> assertThat(ctx).hasSingleBean(PolarisMcpClientProperties.class));
	}

	@DisplayName("sync client list is empty when no services configured")
	@Test
	@SuppressWarnings("unchecked")
	void testSyncClientsEmptyWhenNoServices() {
		// Arrange & Act & Assert
		this.contextRunner
			.withBean(McpClientCommonProperties.class, McpClientCommonProperties::new)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> mock(PolarisSDKContextManager.class))
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpSyncToolCallbackProvider.class);
				List<PolarisMcpSyncClientCluster> clusters = ctx.getBean(
						"polarisMcpSyncClientClusters", List.class);
				assertThat(clusters).isEmpty();
			});
	}

	@DisplayName("async beans are not created when type is SYNC (default)")
	@Test
	void testAsyncBeansNotCreatedWhenTypeSync() {
		// Arrange & Act & Assert
		this.contextRunner
			.withBean(McpClientCommonProperties.class, McpClientCommonProperties::new)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> mock(PolarisSDKContextManager.class))
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpSyncToolCallbackProvider.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpAsyncToolCallbackProvider.class);
			});
	}

	@DisplayName("sync beans are not created when type is ASYNC")
	@Test
	void testSyncBeansNotCreatedWhenTypeAsync() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.mcp.client.type=ASYNC")
			.withBean(McpClientCommonProperties.class, McpClientCommonProperties::new)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> mock(PolarisSDKContextManager.class))
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpSyncToolCallbackProvider.class);
				assertThat(ctx).hasSingleBean(PolarisMcpAsyncToolCallbackProvider.class);
			});
	}

}
