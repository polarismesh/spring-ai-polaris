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
import com.tencent.polaris.api.pojo.RetStatus;

/**
 * Polaris-backed MCP async client. Extends {@link AbstractPolarisMcpClient} with
 * reactive {@link McpAsyncClient} instances.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpAsyncClient extends AbstractPolarisMcpClient<McpAsyncClient> {

	private final List<McpAsyncClientCustomizer> customizers;

	public PolarisMcpAsyncClient(String namespace, String serverName, String scheme, String clientVersion,
			boolean initialized, PolarisSDKContextManager sdkContextManager, PolarisReporter polarisReporter,
			WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			List<McpAsyncClientCustomizer> customizers) {
		super(namespace, serverName, scheme, clientVersion, initialized, sdkContextManager, polarisReporter,
				webClientBuilder, objectMapper);
		this.customizers = customizers != null ? customizers : List.of();
	}

	@Override
	protected McpAsyncClient buildClient(String clientName, McpClientTransport transport,
			McpSchema.Implementation clientInfo, boolean initialized) {
		McpClient.AsyncSpec spec = McpClient.async(transport).clientInfo(clientInfo);
		for (McpAsyncClientCustomizer customizer : this.customizers) {
			customizer.customize(clientName, spec);
		}
		McpAsyncClient client = spec.build();
		if (initialized) {
			client.initialize().block();
		}
		return client;
	}

	@Override
	protected void closeClient(McpAsyncClient client) {
		client.close();
	}

	@Override
	protected void closeClientGracefully(McpAsyncClient client) {
		client.closeGracefully().block();
	}

	/**
	 * Lists all tools available from a selected MCP server instance.
	 * @return a {@link Mono} emitting the list tools result
	 */
	public Mono<McpSchema.ListToolsResult> listTools() {
		return getClient().listTools();
	}

	/**
	 * Calls a tool on a selected MCP server instance and reports the result to Polaris.
	 * @param callToolRequest the call tool request
	 * @return a {@link Mono} emitting the call tool result
	 */
	public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
		return Mono.defer(() -> {
			McpAsyncClient client = getClient();
			String clientName = client.getClientInfo().name();
			long startTime = System.currentTimeMillis();
			return client.callTool(callToolRequest)
				.doOnNext(result -> {
					long delay = System.currentTimeMillis() - startTime;
					RetStatus retStatus = isErrorResult(result) ? RetStatus.RetFail : RetStatus.RetSuccess;
					reportCall(clientName, callToolRequest.name(), delay, retStatus);
				}).doOnError(ex -> {
					long delay = System.currentTimeMillis() - startTime;
					reportCall(clientName, callToolRequest.name(), delay, RetStatus.RetFail);
				});
		});
	}

}
