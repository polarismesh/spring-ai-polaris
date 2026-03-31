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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisCallContext;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.client.pojo.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpAsyncClient}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpAsyncClient")
@ExtendWith(MockitoExtension.class)
class PolarisMcpAsyncClientTest {

	@Mock
	private PolarisSDKContextManager sdkContextManager;

	@Mock
	private PolarisReporter reporter;

	@Mock
	private McpAsyncClient clientA;

	@Mock
	private McpAsyncClient clientB;

	private PolarisMcpAsyncClient createClient() {
		return new PolarisMcpAsyncClient("default", "test-service", "http", "0.0.1", true, this.sdkContextManager,
				this.reporter, WebClient.builder(), new ObjectMapper(), List.of());
	}

	private PolarisMcpAsyncClient createClientWithoutReporter() {
		return new PolarisMcpAsyncClient("default", "test-service", "http", "0.0.1", true, this.sdkContextManager,
				null, WebClient.builder(), new ObjectMapper(), List.of());
	}

	@DisplayName("getClient throws when pool is empty")
	@Test
	void testGetClientThrowsWhenPoolEmpty() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();

		// Act & Assert
		assertThatThrownBy(client::getClient).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No client available");
	}

	@DisplayName("getClient round-robins across pool entries")
	@Test
	void testGetClientRoundRobins() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);
		pool.put("127.0.0.2:8080", this.clientB);

		// Act
		McpAsyncClient first = client.getClient();
		McpAsyncClient second = client.getClient();

		// Assert
		assertThat(first).isNotSameAs(second);
	}

	@DisplayName("getNamespace returns the constructor value")
	@Test
	void testGetNamespaceReturnsConstructorValue() {
		// Arrange & Act & Assert
		assertThat(createClient().getNamespace()).isEqualTo("default");
	}

	@DisplayName("getServerName returns the constructor value")
	@Test
	void testGetServerNameReturnsConstructorValue() {
		// Arrange & Act & Assert
		assertThat(createClient().getServerName()).isEqualTo("test-service");
	}

	@DisplayName("close clears the client pool")
	@Test
	void testCloseClearsPool() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);

		// Act
		client.close();

		// Assert
		assertThat(pool).isEmpty();
	}

	@DisplayName("closeGracefully clears pool")
	@Test
	void testCloseGracefullyClearsPool() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);
		when(this.clientA.closeGracefully()).thenReturn(Mono.empty());

		// Act
		client.closeGracefully();

		// Assert
		assertThat(pool).isEmpty();
	}

	@DisplayName("callTool reports success to Polaris on successful call")
	@Test
	void testCallToolReportsSuccessOnSuccess() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);
		client.getClientNameToNode()
			.put("default_test-service_127.0.0.1:8080/sse", new Node("127.0.0.1", 8080));
		McpSchema.Implementation clientInfo = new McpSchema.Implementation("default_test-service_127.0.0.1:8080/sse", "0.0.1");
		when(this.clientA.getClientInfo()).thenReturn(clientInfo);
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		McpSchema.CallToolResult expectedResult = new McpSchema.CallToolResult(List.of(), false);
		when(this.clientA.callTool(request)).thenReturn(Mono.just(expectedResult));

		// Act
		McpSchema.CallToolResult result = client.callTool(request).block();

		// Assert
		assertThat(result).isSameAs(expectedResult);
		ArgumentCaptor<PolarisCallContext> captor = ArgumentCaptor.forClass(PolarisCallContext.class);
		verify(this.reporter).report(captor.capture());
		PolarisCallContext ctx = captor.getValue();
		assertThat(ctx.getHost()).isEqualTo("127.0.0.1");
		assertThat(ctx.getPort()).isEqualTo(8080);
		assertThat(ctx.getMethod()).isEqualTo("myTool");
		assertThat(ctx.getRetCode()).isEqualTo(0);
		assertThat(ctx.getRetStatus()).isEqualTo(RetStatus.RetSuccess);
	}

	@DisplayName("callTool reports failure to Polaris on error signal")
	@Test
	void testCallToolReportsFailureOnError() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);
		client.getClientNameToNode()
			.put("default_test-service_127.0.0.1:8080/sse", new Node("127.0.0.1", 8080));
		McpSchema.Implementation clientInfo = new McpSchema.Implementation("default_test-service_127.0.0.1:8080/sse", "0.0.1");
		when(this.clientA.getClientInfo()).thenReturn(clientInfo);
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		when(this.clientA.callTool(request)).thenReturn(Mono.error(new RuntimeException("connection error")));

		// Act & Assert
		assertThatThrownBy(() -> client.callTool(request).block()).isInstanceOf(RuntimeException.class)
			.hasMessage("connection error");
		ArgumentCaptor<PolarisCallContext> captor = ArgumentCaptor.forClass(PolarisCallContext.class);
		verify(this.reporter).report(captor.capture());
		PolarisCallContext ctx = captor.getValue();
		assertThat(ctx.getRetCode()).isEqualTo(-1);
		assertThat(ctx.getRetStatus()).isEqualTo(RetStatus.RetFail);
	}

	@DisplayName("callTool reports failure when result indicates error")
	@Test
	void testCallToolReportsFailureOnErrorResult() {
		// Arrange
		PolarisMcpAsyncClient client = createClient();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);
		client.getClientNameToNode()
			.put("default_test-service_127.0.0.1:8080/sse", new Node("127.0.0.1", 8080));
		McpSchema.Implementation clientInfo = new McpSchema.Implementation("default_test-service_127.0.0.1:8080/sse", "0.0.1");
		when(this.clientA.getClientInfo()).thenReturn(clientInfo);
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		McpSchema.CallToolResult errorResult = new McpSchema.CallToolResult(List.of(), true);
		when(this.clientA.callTool(request)).thenReturn(Mono.just(errorResult));

		// Act
		McpSchema.CallToolResult result = client.callTool(request).block();

		// Assert
		assertThat(result).isSameAs(errorResult);
		ArgumentCaptor<PolarisCallContext> captor = ArgumentCaptor.forClass(PolarisCallContext.class);
		verify(this.reporter).report(captor.capture());
		PolarisCallContext ctx = captor.getValue();
		assertThat(ctx.getRetCode()).isEqualTo(-1);
		assertThat(ctx.getRetStatus()).isEqualTo(RetStatus.RetFail);
	}

	@DisplayName("reportCall does nothing when reporter is null")
	@Test
	void testReportCallSkipsWhenReporterNull() {
		// Arrange
		PolarisMcpAsyncClient client = createClientWithoutReporter();
		ConcurrentHashMap<String, McpAsyncClient> pool = client.getKeyToClientMap();
		pool.put("127.0.0.1:8080", this.clientA);
		McpSchema.Implementation clientInfo = new McpSchema.Implementation("default_test-service_127.0.0.1:8080/sse", "0.0.1");
		when(this.clientA.getClientInfo()).thenReturn(clientInfo);
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		McpSchema.CallToolResult expectedResult = new McpSchema.CallToolResult(List.of(), false);
		when(this.clientA.callTool(request)).thenReturn(Mono.just(expectedResult));

		// Act
		client.callTool(request).block();

		// Assert
		verifyNoInteractions(this.reporter);
	}

}
