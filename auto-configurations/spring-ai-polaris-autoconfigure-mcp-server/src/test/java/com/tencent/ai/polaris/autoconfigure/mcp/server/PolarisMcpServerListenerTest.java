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

import java.util.Collections;
import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;

import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.ai.polaris.mcp.server.McpServerCapabilitiesProvider;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerContractReporter;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerRegistry;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpServerListener}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpServerListener")
@ExtendWith(MockitoExtension.class)
class PolarisMcpServerListenerTest {

	@Mock
	private PolarisMcpServerRegistry registry;

	@Mock
	private WebServerInitializedEvent event;

	@Mock
	private WebServer webServer;

	@Mock
	private WebServerApplicationContext applicationContext;

	@Mock
	private PolarisMcpServerContractReporter contractReporter;

	@Mock
	private McpServerCapabilitiesProvider capabilitiesProvider;

	private PolarisMcpServerProperties properties;

	private PolarisCoreProperties coreProperties;

	private PolarisMcpServerListener listener;

	@BeforeEach
	void setUp() {
		this.properties = new PolarisMcpServerProperties();
		this.coreProperties = new PolarisCoreProperties();
		lenient().when(this.registry.getProtocol()).thenReturn(PolarisMcpMetadataKeys.PROTOCOL_MCP_SSE);
		lenient().when(this.registry.getEndpointPath()).thenReturn("/sse");
		this.listener = new PolarisMcpServerListener(this.registry, this.properties, this.coreProperties,
				this.contractReporter, this.capabilitiesProvider);
	}

	@DisplayName("register is called on application event")
	@Test
	void testRegisterCalledOnApplicationEvent() {
		// Arrange
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);
		lenient().when(this.capabilitiesProvider.listTools()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listResources()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listPrompts()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listRequestHandlerMethods()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listNotificationHandlerMethods()).thenReturn(Collections.emptyList());

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(anyString(), eq(8080));
	}

	@DisplayName("register is skipped for management server")
	@Test
	void testRegisterSkippedForManagementServer() {
		// Arrange
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn("management");

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry, never()).register(anyString(), anyInt());
	}

	@DisplayName("deregister is called on destroy")
	@Test
	void testDeregisterCalledOnDestroy() {
		// Arrange (no setup needed)

		// Act
		this.listener.destroy();

		// Assert
		verify(this.registry).deregister();
	}

	@DisplayName("register failure is caught and logged")
	@Test
	void testRegisterFailureIsCaught() {
		// Arrange
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);
		doThrow(new RuntimeException("register failed")).when(this.registry)
			.register(anyString(), eq(8080));

		// Act & Assert (should not throw)
		assertThatCode(() -> this.listener.onApplicationEvent(this.event)).doesNotThrowAnyException();
	}

	@DisplayName("custom host is used when set in properties")
	@Test
	void testCustomHostUsed() {
		// Arrange
		this.properties.setHost("127.0.0.1");
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);
		lenient().when(this.capabilitiesProvider.listTools()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listResources()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listPrompts()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listRequestHandlerMethods()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listNotificationHandlerMethods()).thenReturn(Collections.emptyList());

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(eq("127.0.0.1"), eq(8080));
	}

	@DisplayName("custom port is used when set in properties")
	@Test
	void testCustomPortUsed() {
		// Arrange
		this.properties.setPort(9090);
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		lenient().when(this.capabilitiesProvider.listTools()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listResources()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listPrompts()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listRequestHandlerMethods()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listNotificationHandlerMethods()).thenReturn(Collections.emptyList());

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(anyString(), eq(9090));
	}

	@DisplayName("custom host and port are both used when set in properties")
	@Test
	void testCustomHostAndPortUsed() {
		// Arrange
		this.properties.setHost("127.0.0.1");
		this.properties.setPort(9090);
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		lenient().when(this.capabilitiesProvider.listTools()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listResources()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listPrompts()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listRequestHandlerMethods()).thenReturn(Collections.emptyList());
		lenient().when(this.capabilitiesProvider.listNotificationHandlerMethods()).thenReturn(Collections.emptyList());

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(eq("127.0.0.1"), eq(9090));
	}

	@DisplayName("contract is reported after successful registration")
	@Test
	void testContractReportedAfterRegistration() {
		// Arrange
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);
		when(this.capabilitiesProvider.listTools()).thenReturn(Collections.emptyList());
		when(this.capabilitiesProvider.listResources()).thenReturn(Collections.emptyList());
		when(this.capabilitiesProvider.listPrompts()).thenReturn(Collections.emptyList());
		List<String> requestHandlers = List.of(McpSchema.METHOD_PING, McpSchema.METHOD_TOOLS_LIST);
		List<String> notificationHandlers = List.of(McpSchema.METHOD_NOTIFICATION_INITIALIZED);
		when(this.capabilitiesProvider.listRequestHandlerMethods()).thenReturn(requestHandlers);
		when(this.capabilitiesProvider.listNotificationHandlerMethods()).thenReturn(notificationHandlers);

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(anyString(), eq(8080));
		verify(this.contractReporter).reportContract(
				eq(PolarisMcpMetadataKeys.PROTOCOL_MCP_SSE), eq("/sse"),
				eq(Collections.emptyList()), eq(Collections.emptyList()), eq(Collections.emptyList()),
				eq(requestHandlers), eq(notificationHandlers));
	}

	@DisplayName("contract reporting is skipped when reporter is null")
	@Test
	void testContractReportingSkippedWhenReporterNull() {
		// Arrange
		PolarisMcpServerListener listenerNoReporter = new PolarisMcpServerListener(
				this.registry, this.properties, this.coreProperties, null, this.capabilitiesProvider);
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);

		// Act
		listenerNoReporter.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(anyString(), eq(8080));
		verify(this.contractReporter, never()).reportContract(any(), any(), any(), any(), any(), any(), any());
	}

	@DisplayName("contract reporting failure does not affect registration")
	@Test
	void testContractReportingFailureDoesNotAffectRegistration() {
		// Arrange
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);
		when(this.capabilitiesProvider.listTools()).thenThrow(new RuntimeException("list failed"));

		// Act & Assert
		assertThatCode(() -> this.listener.onApplicationEvent(this.event)).doesNotThrowAnyException();
		verify(this.registry).register(anyString(), eq(8080));
	}

}
