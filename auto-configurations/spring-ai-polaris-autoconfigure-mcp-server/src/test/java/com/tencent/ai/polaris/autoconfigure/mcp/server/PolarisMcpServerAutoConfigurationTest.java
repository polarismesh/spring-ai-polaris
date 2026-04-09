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

import java.lang.reflect.Field;
import java.util.List;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProviderBase;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpServerAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpServerAutoConfiguration")
class PolarisMcpServerAutoConfigurationTest {

	private static final McpSchema.ServerCapabilities TEST_CAPABILITIES = McpSchema.ServerCapabilities.builder()
		.tools(true)
		.build();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisMcpServerAutoConfiguration.class))
		.withPropertyValues("spring.application.name=test-service");

	@DisplayName("beans are not created when polaris mcp server is disabled")
	@Test
	void testBeansNotCreatedWhenDisabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.mcp.server.enabled=false")
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("beans are not created when polaris is globally disabled")
	@Test
	void testBeansNotCreatedWhenPolarisDisabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.enabled=false")
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("beans are not created when McpServerProperties bean is absent")
	@Test
	void testBeansNotCreatedWithoutMcpServerProperties() {
		// Arrange & Act & Assert
		this.contextRunner
			.run(ctx -> {
				assertThat(ctx).doesNotHaveBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).doesNotHaveBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("properties bean is registered when McpServerProperties is present")
	@Test
	void testPropertiesBeanRegisteredWithMcpServerProperties() {
		// Arrange & Act & Assert
		this.contextRunner
			.withBean(McpServerProperties.class, McpServerProperties::new)
			.run(ctx -> assertThat(ctx).hasSingleBean(PolarisMcpServerProperties.class));
	}

	@DisplayName("sync registry bean is created with SSE protocol")
	@Test
	void testSyncRegistryBeanCreatedWithSseProtocol() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.SSE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerSseProperties.class, McpServerSseProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).hasSingleBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("sync registry bean is created with STREAMABLE protocol")
	@Test
	void testSyncRegistryBeanCreatedWithStreamableProtocol() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.STREAMABLE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.mcp.server.protocol=STREAMABLE")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerStreamableHttpProperties.class, McpServerStreamableHttpProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).hasSingleBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("async registry bean is created with SSE protocol")
	@Test
	void testAsyncRegistryBeanCreatedWithSseProtocol() {
		// Arrange
		McpAsyncServer mcpAsyncServer = mock(McpAsyncServer.class);
		when(mcpAsyncServer.getServerInfo())
			.thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpAsyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.SSE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.mcp.server.type=ASYNC")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpAsyncServer.class, () -> mcpAsyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerSseProperties.class, McpServerSseProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).hasSingleBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("stateless sync registry bean is created")
	@Test
	void testStatelessSyncRegistryBeanCreated() {
		// Arrange
		McpStatelessSyncServer mcpStatelessSyncServer = mock(McpStatelessSyncServer.class);
		when(mcpStatelessSyncServer.getServerInfo())
			.thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpStatelessSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.STATELESS);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.mcp.server.protocol=STATELESS")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpStatelessSyncServer.class, () -> mcpStatelessSyncServer)
			.withBean(McpStatelessServerTransport.class,
					PolarisMcpServerAutoConfigurationTest::mockStatelessTransport)
			.withBean(McpServerStreamableHttpProperties.class, McpServerStreamableHttpProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).hasSingleBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("stateless async registry bean is created")
	@Test
	void testStatelessAsyncRegistryBeanCreated() {
		// Arrange
		McpStatelessAsyncServer mcpStatelessAsyncServer = mock(McpStatelessAsyncServer.class);
		when(mcpStatelessAsyncServer.getServerInfo())
			.thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpStatelessAsyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.STATELESS);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.mcp.server.protocol=STATELESS", "spring.ai.mcp.server.type=ASYNC")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpStatelessAsyncServer.class, () -> mcpStatelessAsyncServer)
			.withBean(McpStatelessServerTransport.class,
					PolarisMcpServerAutoConfigurationTest::mockStatelessTransport)
			.withBean(McpServerStreamableHttpProperties.class, McpServerStreamableHttpProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				assertThat(ctx).hasSingleBean(PolarisMcpServerListener.class);
			});
	}

	@DisplayName("sync registry bean uses stdio protocol when StdioServerTransportProvider is used")
	@Test
	void testSyncRegistryBeanStdioProtocol() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setStdio(true);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockStdioTransportProvider)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				PolarisMcpServerRegistry registry = ctx.getBean(PolarisMcpServerRegistry.class);
				assertThat(registry.getProtocol()).isEqualTo(PolarisMcpMetadataKeys.PROTOCOL_MCP_STDIO);
			});
	}

	@DisplayName("async registry bean uses stdio protocol when StdioServerTransportProvider is used")
	@Test
	void testAsyncRegistryBeanStdioProtocol() {
		// Arrange
		McpAsyncServer mcpAsyncServer = mock(McpAsyncServer.class);
		when(mcpAsyncServer.getServerInfo())
			.thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpAsyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setStdio(true);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.mcp.server.type=ASYNC")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpAsyncServer.class, () -> mcpAsyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockStdioTransportProvider)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				PolarisMcpServerRegistry registry = ctx.getBean(PolarisMcpServerRegistry.class);
				assertThat(registry.getProtocol()).isEqualTo(PolarisMcpMetadataKeys.PROTOCOL_MCP_STDIO);
			});
	}

	@DisplayName("endpoint path includes context path when configured")
	@Test
	void testEndpointPathIncludesContextPath() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.SSE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("server.servlet.context-path=/api")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerSseProperties.class, McpServerSseProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				PolarisMcpServerRegistry registry = ctx.getBean(PolarisMcpServerRegistry.class);
				String endpointPath = getPrivateField(registry, "endpointPath");
				assertThat(endpointPath).isEqualTo("/api/sse");
			});
	}

	@DisplayName("endpoint path includes webflux base path when configured")
	@Test
	void testEndpointPathIncludesWebfluxBasePath() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.SSE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("spring.webflux.base-path=/reactive")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerSseProperties.class, McpServerSseProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				PolarisMcpServerRegistry registry = ctx.getBean(PolarisMcpServerRegistry.class);
				String endpointPath = getPrivateField(registry, "endpointPath");
				assertThat(endpointPath).isEqualTo("/reactive/sse");
			});
	}

	@DisplayName("servlet context path takes precedence over webflux base path")
	@Test
	void testServletContextPathTakesPrecedence() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.SSE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withPropertyValues("server.servlet.context-path=/servlet", "spring.webflux.base-path=/reactive")
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerSseProperties.class, McpServerSseProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				PolarisMcpServerRegistry registry = ctx.getBean(PolarisMcpServerRegistry.class);
				String endpointPath = getPrivateField(registry, "endpointPath");
				assertThat(endpointPath).isEqualTo("/servlet/sse");
			});
	}

	@DisplayName("endpoint path has no prefix when context path is not configured")
	@Test
	void testEndpointPathWithoutContextPath() {
		// Arrange
		McpSyncServer mcpSyncServer = mock(McpSyncServer.class);
		when(mcpSyncServer.getServerInfo()).thenReturn(new McpSchema.Implementation("test-server", null, "2025-03-26"));
		when(mcpSyncServer.getServerCapabilities()).thenReturn(TEST_CAPABILITIES);

		McpServerProperties mcpServerProperties = new McpServerProperties();
		mcpServerProperties.setProtocol(McpServerProperties.ServerProtocol.SSE);

		PolarisSDKContextManager sdkContextManager = mock(PolarisSDKContextManager.class);

		// Act & Assert
		this.contextRunner
			.withBean(McpServerProperties.class, () -> mcpServerProperties)
			.withBean(McpSyncServer.class, () -> mcpSyncServer)
			.withBean(McpServerTransportProviderBase.class,
					PolarisMcpServerAutoConfigurationTest::mockTransportProvider)
			.withBean(McpServerSseProperties.class, McpServerSseProperties::new)
			.withBean(PolarisSDKContextManager.class, () -> sdkContextManager)
			.withBean(PolarisCoreProperties.class, PolarisCoreProperties::new)
			.run(ctx -> {
				assertThat(ctx).hasSingleBean(PolarisMcpServerRegistry.class);
				PolarisMcpServerRegistry registry = ctx.getBean(PolarisMcpServerRegistry.class);
				String endpointPath = getPrivateField(registry, "endpointPath");
				assertThat(endpointPath).isEqualTo("/sse");
			});
	}

	@SuppressWarnings("unchecked")
	private static <T> T getPrivateField(Object object, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(object);
	}

	private static McpServerTransportProviderBase mockTransportProvider() {
		McpServerTransportProviderBase provider = mock(McpServerTransportProviderBase.class);
		when(provider.protocolVersions()).thenReturn(List.of("2024-11-05"));
		return provider;
	}

	private static McpServerTransportProviderBase mockStdioTransportProvider() {
		StdioServerTransportProvider provider = mock(StdioServerTransportProvider.class);
		when(provider.protocolVersions()).thenReturn(List.of("2024-11-05"));
		return provider;
	}

	private static McpStatelessServerTransport mockStatelessTransport() {
		McpStatelessServerTransport transport = mock(McpStatelessServerTransport.class);
		when(transport.protocolVersions()).thenReturn(List.of("2024-11-05"));
		return transport;
	}

}
