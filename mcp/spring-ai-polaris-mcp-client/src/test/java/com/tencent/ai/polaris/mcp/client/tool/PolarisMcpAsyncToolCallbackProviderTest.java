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

package com.tencent.ai.polaris.mcp.client.tool;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.ai.tool.ToolCallback;

import com.tencent.ai.polaris.mcp.client.PolarisMcpAsyncClientCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpAsyncToolCallbackProvider}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpAsyncToolCallbackProvider")
@ExtendWith(MockitoExtension.class)
class PolarisMcpAsyncToolCallbackProviderTest {

	@Mock
	private PolarisMcpAsyncClientCluster clientA;

	@Mock
	private PolarisMcpAsyncClientCluster clientB;

	@DisplayName("getToolCallbacks aggregates from all clients")
	@Test
	void testGetToolCallbacksAggregatesFromAllClients() {
		// Arrange
		McpSchema.Tool toolA = mock(McpSchema.Tool.class);
		when(toolA.name()).thenReturn("tool-a");
		McpSchema.Tool toolB = mock(McpSchema.Tool.class);
		when(toolB.name()).thenReturn("tool-b");
		when(this.clientA.getServerName()).thenReturn("service-a");
		when(this.clientB.getServerName()).thenReturn("service-b");
		when(this.clientA.listTools()).thenReturn(
				Mono.just(new McpSchema.ListToolsResult(List.of(toolA), null)));
		when(this.clientB.listTools()).thenReturn(
				Mono.just(new McpSchema.ListToolsResult(List.of(toolB), null)));

		// Act
		PolarisMcpAsyncToolCallbackProvider provider =
				new PolarisMcpAsyncToolCallbackProvider(List.of(this.clientA, this.clientB));
		ToolCallback[] callbacks = provider.getToolCallbacks();

		// Assert
		assertThat(callbacks).hasSize(2);
	}

	@DisplayName("getToolCallbacks returns empty when no clients")
	@Test
	void testGetToolCallbacksReturnsEmptyWhenNoClients() {
		// Arrange & Act
		PolarisMcpAsyncToolCallbackProvider provider =
				new PolarisMcpAsyncToolCallbackProvider(List.of());
		ToolCallback[] callbacks = provider.getToolCallbacks();

		// Assert
		assertThat(callbacks).isEmpty();
	}

	@DisplayName("getToolCallbacks skips client that throws exception")
	@Test
	void testGetToolCallbacksSkipsFailingClient() {
		// Arrange
		McpSchema.Tool toolB = mock(McpSchema.Tool.class);
		when(toolB.name()).thenReturn("tool-b");
		when(this.clientA.getServerName()).thenReturn("service-a");
		when(this.clientA.listTools()).thenReturn(Mono.error(new RuntimeException("connection error")));
		when(this.clientB.getServerName()).thenReturn("service-b");
		when(this.clientB.listTools()).thenReturn(
				Mono.just(new McpSchema.ListToolsResult(List.of(toolB), null)));

		// Act
		PolarisMcpAsyncToolCallbackProvider provider =
				new PolarisMcpAsyncToolCallbackProvider(List.of(this.clientA, this.clientB));
		ToolCallback[] callbacks = provider.getToolCallbacks();

		// Assert
		assertThat(callbacks).hasSize(1);
	}

	@DisplayName("getToolCallbacks throws on duplicate tool name across clients")
	@Test
	void testGetToolCallbacksThrowsOnDuplicateToolName() {
		// Arrange
		McpSchema.Tool tool = mock(McpSchema.Tool.class);
		when(tool.name()).thenReturn("same-tool");
		when(this.clientA.getServerName()).thenReturn("service-a");
		when(this.clientB.getServerName()).thenReturn("service-a");
		when(this.clientA.listTools()).thenReturn(
				Mono.just(new McpSchema.ListToolsResult(List.of(tool), null)));
		when(this.clientB.listTools()).thenReturn(
				Mono.just(new McpSchema.ListToolsResult(List.of(tool), null)));

		// Act & Assert
		PolarisMcpAsyncToolCallbackProvider provider =
				new PolarisMcpAsyncToolCallbackProvider(List.of(this.clientA, this.clientB));
		assertThatThrownBy(provider::getToolCallbacks)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Multiple tools");
	}

}
