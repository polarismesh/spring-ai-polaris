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

package com.tencent.ai.polaris.autoconfigure.mcp.server;

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
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerRegistry;

import static org.assertj.core.api.Assertions.assertThatCode;
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

	private PolarisMcpServerProperties properties;

	private PolarisCoreProperties coreProperties;

	private PolarisMcpServerListener listener;

	@BeforeEach
	void setUp() {
		this.properties = new PolarisMcpServerProperties();
		this.coreProperties = new PolarisCoreProperties();
		lenient().when(this.registry.getProtocol()).thenReturn(PolarisMcpMetadataKeys.PROTOCOL_MCP_SSE);
		this.listener = new PolarisMcpServerListener(this.registry, this.properties, this.coreProperties);
	}

	@DisplayName("register is called on application event")
	@Test
	void testRegisterCalledOnApplicationEvent() {
		// Arrange
		when(this.event.getApplicationContext()).thenReturn(this.applicationContext);
		when(this.applicationContext.getServerNamespace()).thenReturn(null);
		when(this.event.getWebServer()).thenReturn(this.webServer);
		when(this.webServer.getPort()).thenReturn(8080);

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

		// Act
		this.listener.onApplicationEvent(this.event);

		// Assert
		verify(this.registry).register(eq("127.0.0.1"), eq(9090));
	}

}
