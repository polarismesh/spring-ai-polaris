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

package com.tencent.ai.polaris.mcp.client.tool;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.ai.tool.execution.ToolExecutionException;

import com.tencent.ai.polaris.mcp.client.PolarisMcpAsyncClientCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpAsyncToolCallback}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpAsyncToolCallback")
@ExtendWith(MockitoExtension.class)
class PolarisMcpAsyncToolCallbackTest {

	@Mock
	private PolarisMcpAsyncClientCluster mcpClient;

	@DisplayName("getToolDefinition returns prefixed tool name")
	@Test
	void testGetToolDefinitionReturnsPrefixedToolName() {
		// Arrange
		when(this.mcpClient.getServerName()).thenReturn("my-service");
		McpSchema.Tool tool = mock(McpSchema.Tool.class);
		when(tool.name()).thenReturn("my-tool");

		// Act
		PolarisMcpAsyncToolCallback callback = new PolarisMcpAsyncToolCallback(this.mcpClient, tool);

		// Assert
		assertThat(callback.getToolDefinition().name()).isEqualTo("m_s_my_tool");
	}

	@DisplayName("call returns JSON string on success")
	@Test
	void testCallReturnsJsonStringOnSuccess() {
		// Arrange
		when(this.mcpClient.getServerName()).thenReturn("my-service");
		McpSchema.Tool tool = mock(McpSchema.Tool.class);
		when(tool.name()).thenReturn("my-tool");
		McpSchema.CallToolResult result = new McpSchema.CallToolResult(
				List.of(new McpSchema.TextContent("hello")), false);
		when(this.mcpClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(Mono.just(result));
		PolarisMcpAsyncToolCallback callback = new PolarisMcpAsyncToolCallback(this.mcpClient, tool);

		// Act
		String output = callback.call("{\"key\":\"value\"}");

		// Assert
		assertThat(output).isNotNull();
	}

	@DisplayName("call throws ToolExecutionException on error result")
	@Test
	void testCallThrowsOnErrorResult() {
		// Arrange
		when(this.mcpClient.getServerName()).thenReturn("my-service");
		McpSchema.Tool tool = mock(McpSchema.Tool.class);
		when(tool.name()).thenReturn("my-tool");
		McpSchema.CallToolResult result = new McpSchema.CallToolResult(
				List.of(new McpSchema.TextContent("error detail")), true);
		when(this.mcpClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(Mono.just(result));
		PolarisMcpAsyncToolCallback callback = new PolarisMcpAsyncToolCallback(this.mcpClient, tool);

		// Act & Assert
		assertThatThrownBy(() -> callback.call("{\"key\":\"value\"}"))
			.isInstanceOf(ToolExecutionException.class);
	}

	@DisplayName("call uses empty JSON when input is blank")
	@Test
	void testCallUsesEmptyJsonWhenInputBlank() {
		// Arrange
		when(this.mcpClient.getServerName()).thenReturn("my-service");
		McpSchema.Tool tool = mock(McpSchema.Tool.class);
		when(tool.name()).thenReturn("my-tool");
		McpSchema.CallToolResult result = new McpSchema.CallToolResult(
				List.of(new McpSchema.TextContent("ok")), false);
		when(this.mcpClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(Mono.just(result));
		PolarisMcpAsyncToolCallback callback = new PolarisMcpAsyncToolCallback(this.mcpClient, tool);

		// Act
		String output = callback.call("");

		// Assert
		assertThat(output).isNotNull();
	}

	@DisplayName("call throws ToolExecutionException on client error signal")
	@Test
	void testCallThrowsOnClientErrorSignal() {
		// Arrange
		when(this.mcpClient.getServerName()).thenReturn("my-service");
		McpSchema.Tool tool = mock(McpSchema.Tool.class);
		when(tool.name()).thenReturn("my-tool");
		when(this.mcpClient.callTool(any(McpSchema.CallToolRequest.class)))
			.thenReturn(Mono.error(new RuntimeException("connection error")));
		PolarisMcpAsyncToolCallback callback = new PolarisMcpAsyncToolCallback(this.mcpClient, tool);

		// Act & Assert
		assertThatThrownBy(() -> callback.call("{\"key\":\"value\"}"))
			.isInstanceOf(ToolExecutionException.class);
	}

}
