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

package com.tencent.ai.polaris.mcp.client;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

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

	private static final Node TEST_NODE = new Node("127.0.0.1", 8080);

	@Mock
	private PolarisReporter reporter;

	@Mock
	private McpAsyncClient mcpClient;

	private PolarisMcpAsyncClient client;

	@BeforeEach
	void setUp() {
		this.client = new PolarisMcpAsyncClient(this.mcpClient, TEST_NODE, "default", "test-service",
				this.reporter);
	}

	@DisplayName("getDelegate returns the underlying MCP client")
	@Test
	void testGetDelegateReturnsUnderlyingClient() {
		// Arrange & Act & Assert
		assertThat(this.client.getDelegate()).isSameAs(this.mcpClient);
	}

	@DisplayName("callTool reports success to Polaris on successful call")
	@Test
	void testCallToolReportsSuccessOnSuccess() {
		// Arrange
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		McpSchema.CallToolResult expectedResult = new McpSchema.CallToolResult(List.of(), false);
		when(this.mcpClient.callTool(request)).thenReturn(Mono.just(expectedResult));

		// Act
		McpSchema.CallToolResult result = this.client.callTool(request).block();

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
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		when(this.mcpClient.callTool(request)).thenReturn(Mono.error(new RuntimeException("connection error")));

		// Act & Assert
		assertThatThrownBy(() -> this.client.callTool(request).block()).isInstanceOf(RuntimeException.class)
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
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		McpSchema.CallToolResult errorResult = new McpSchema.CallToolResult(List.of(), true);
		when(this.mcpClient.callTool(request)).thenReturn(Mono.just(errorResult));

		// Act
		McpSchema.CallToolResult result = this.client.callTool(request).block();

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
		PolarisMcpAsyncClient clientWithoutReporter = new PolarisMcpAsyncClient(this.mcpClient, TEST_NODE,
				"default", "test-service", null);
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", Map.of());
		McpSchema.CallToolResult expectedResult = new McpSchema.CallToolResult(List.of(), false);
		when(this.mcpClient.callTool(request)).thenReturn(Mono.just(expectedResult));

		// Act
		clientWithoutReporter.callTool(request).block();

		// Assert
		verifyNoInteractions(this.reporter);
	}

	@DisplayName("listTools delegates to underlying client")
	@Test
	void testListToolsDelegatesToClient() {
		// Arrange
		McpSchema.ListToolsResult expected = new McpSchema.ListToolsResult(List.of(), null);
		when(this.mcpClient.listTools()).thenReturn(Mono.just(expected));

		// Act
		McpSchema.ListToolsResult result = this.client.listTools().block();

		// Assert
		assertThat(result).isSameAs(expected);
	}

	@DisplayName("closeClient delegates to underlying client")
	@Test
	void testCloseClientDelegatesToClient() {
		// Arrange & Act
		this.client.closeClient();

		// Assert
		verify(this.mcpClient).close();
	}

	@DisplayName("closeClientGracefully delegates to underlying client")
	@Test
	void testCloseClientGracefullyDelegatesToClient() {
		// Arrange
		when(this.mcpClient.closeGracefully()).thenReturn(Mono.empty());

		// Act
		this.client.closeClientGracefully();

		// Assert
		verify(this.mcpClient).closeGracefully();
	}

}
