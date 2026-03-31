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
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.client.pojo.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpAsyncClientCluster}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpAsyncClientCluster")
@ExtendWith(MockitoExtension.class)
class PolarisMcpAsyncClientClusterTest {

	@Mock
	private PolarisSDKContextManager sdkContextManager;

	@Mock
	private PolarisReporter reporter;

	private PolarisMcpAsyncClientCluster createCluster() {
		return new PolarisMcpAsyncClientCluster("default", "test-service", "spring-ai-mcp-client", "http", "0.0.1",
				true, this.sdkContextManager, this.reporter, WebClient.builder(), new ObjectMapper(), List.of());
	}

	@DisplayName("getClient throws when pool is empty")
	@Test
	void testGetClientThrowsWhenPoolEmpty() {
		// Arrange
		PolarisMcpAsyncClientCluster cluster = createCluster();

		// Act & Assert
		assertThatThrownBy(cluster::getClient).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No client available");
	}

	@DisplayName("getClient round-robins across pool entries")
	@Test
	void testGetClientRoundRobins() {
		// Arrange
		PolarisMcpAsyncClientCluster cluster = createCluster();
		ConcurrentHashMap<Node, PolarisMcpAsyncClient> pool = cluster.getKeyToClientMap();
		PolarisMcpAsyncClient wrapperA = mock(PolarisMcpAsyncClient.class);
		PolarisMcpAsyncClient wrapperB = mock(PolarisMcpAsyncClient.class);
		pool.put(new Node("127.0.0.1", 8080), wrapperA);
		pool.put(new Node("127.0.0.2", 8080), wrapperB);

		// Act
		PolarisMcpAsyncClient first = cluster.getClient();
		PolarisMcpAsyncClient second = cluster.getClient();

		// Assert
		assertThat(first).isNotSameAs(second);
	}

	@DisplayName("getNamespace returns the constructor value")
	@Test
	void testGetNamespaceReturnsConstructorValue() {
		// Arrange & Act & Assert
		assertThat(createCluster().getNamespace()).isEqualTo("default");
	}

	@DisplayName("getServerName returns the constructor value")
	@Test
	void testGetServerNameReturnsConstructorValue() {
		// Arrange & Act & Assert
		assertThat(createCluster().getServerName()).isEqualTo("test-service");
	}

	@DisplayName("close clears the client pool")
	@Test
	void testCloseClearsPool() {
		// Arrange
		PolarisMcpAsyncClientCluster cluster = createCluster();
		ConcurrentHashMap<Node, PolarisMcpAsyncClient> pool = cluster.getKeyToClientMap();
		PolarisMcpAsyncClient wrapper = mock(PolarisMcpAsyncClient.class);
		pool.put(new Node("127.0.0.1", 8080), wrapper);

		// Act
		cluster.close();

		// Assert
		assertThat(pool).isEmpty();
		verify(wrapper).closeClient();
	}

	@DisplayName("closeGracefully clears pool")
	@Test
	void testCloseGracefullyClearsPool() {
		// Arrange
		PolarisMcpAsyncClientCluster cluster = createCluster();
		ConcurrentHashMap<Node, PolarisMcpAsyncClient> pool = cluster.getKeyToClientMap();
		PolarisMcpAsyncClient wrapper = mock(PolarisMcpAsyncClient.class);
		pool.put(new Node("127.0.0.1", 8080), wrapper);

		// Act
		cluster.closeGracefully();

		// Assert
		assertThat(pool).isEmpty();
		verify(wrapper).closeClientGracefully();
	}

	@DisplayName("callTool delegates to round-robin selected wrapper")
	@Test
	void testCallToolDelegatesToWrapper() {
		// Arrange
		PolarisMcpAsyncClientCluster cluster = createCluster();
		ConcurrentHashMap<Node, PolarisMcpAsyncClient> pool = cluster.getKeyToClientMap();
		PolarisMcpAsyncClient wrapper = mock(PolarisMcpAsyncClient.class);
		pool.put(new Node("127.0.0.1", 8080), wrapper);
		McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("myTool", java.util.Map.of());
		McpSchema.CallToolResult expectedResult = new McpSchema.CallToolResult(List.of(), false);
		when(wrapper.callTool(request)).thenReturn(Mono.just(expectedResult));

		// Act
		McpSchema.CallToolResult result = cluster.callTool(request).block();

		// Assert
		assertThat(result).isSameAs(expectedResult);
	}

	@DisplayName("listTools delegates to round-robin selected wrapper")
	@Test
	void testListToolsDelegatesToWrapper() {
		// Arrange
		PolarisMcpAsyncClientCluster cluster = createCluster();
		ConcurrentHashMap<Node, PolarisMcpAsyncClient> pool = cluster.getKeyToClientMap();
		PolarisMcpAsyncClient wrapper = mock(PolarisMcpAsyncClient.class);
		pool.put(new Node("127.0.0.1", 8080), wrapper);
		McpSchema.ListToolsResult expected = new McpSchema.ListToolsResult(List.of(), null);
		when(wrapper.listTools()).thenReturn(Mono.just(expected));

		// Act
		McpSchema.ListToolsResult result = cluster.listTools().block();

		// Assert
		assertThat(result).isSameAs(expected);
	}

}
