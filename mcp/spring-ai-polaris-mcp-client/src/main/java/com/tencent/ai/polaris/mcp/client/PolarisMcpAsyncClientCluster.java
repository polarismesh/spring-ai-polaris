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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.client.pojo.Node;

/**
 * Connection pool of {@link PolarisMcpAsyncClient} instances for a Polaris service.
 * Each healthy instance gets its own single-connection wrapper; tool calls and listings
 * are delegated to a round-robin selected wrapper.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpAsyncClientCluster extends AbstractPolarisMcpClientCluster<PolarisMcpAsyncClient> {

	private final List<McpAsyncClientCustomizer> customizers;

	public PolarisMcpAsyncClientCluster(String namespace, String serverName, String clientName, String scheme,
			String clientVersion, boolean initialized, PolarisSDKContextManager sdkContextManager,
			PolarisReporter polarisReporter, WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			List<McpAsyncClientCustomizer> customizers) {
		super(namespace, serverName, clientName, scheme, clientVersion, initialized, sdkContextManager,
				polarisReporter, webClientBuilder, objectMapper);
		this.customizers = customizers != null ? customizers : List.of();
	}

	@Override
	protected PolarisMcpAsyncClient createClientWrapper(McpClientTransport transport,
			McpSchema.Implementation clientInfo, String connectedName, Node node, boolean initialized) {
		McpClient.AsyncSpec spec = McpClient.async(transport).clientInfo(clientInfo);
		for (McpAsyncClientCustomizer customizer : this.customizers) {
			customizer.customize(connectedName, spec);
		}
		McpAsyncClient client = spec.build();
		if (initialized) {
			client.initialize().block();
		}
		return new PolarisMcpAsyncClient(client, node, getNamespace(), getServerName(),
				getPolarisReporter());
	}

	/**
	 * Calls a tool on a round-robin selected MCP server instance.
	 * @param callToolRequest the call tool request
	 * @return a {@link Mono} emitting the call tool result
	 */
	public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
		return getClient().callTool(callToolRequest);
	}

	/**
	 * Lists all tools from a round-robin selected MCP server instance.
	 * @return a {@link Mono} emitting the list tools result
	 */
	public Mono<McpSchema.ListToolsResult> listTools() {
		return getClient().listTools();
	}

}
