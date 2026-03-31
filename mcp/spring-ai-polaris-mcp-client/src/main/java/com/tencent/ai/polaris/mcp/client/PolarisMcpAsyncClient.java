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

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.client.pojo.Node;

/**
 * Single-connection Polaris-backed MCP async client. Wraps an {@link McpAsyncClient}
 * together with its remote {@link Node} and Polaris call reporting.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpAsyncClient extends AbstractPolarisMcpClient<McpAsyncClient> {

	public PolarisMcpAsyncClient(McpAsyncClient client, Node node, String namespace, String serverName,
			PolarisReporter polarisReporter) {
		super(client, node, namespace, serverName, polarisReporter);
	}

	/**
	 * Calls a tool on the MCP server and reports the result to Polaris.
	 * @param callToolRequest the call tool request
	 * @return a {@link Mono} emitting the call tool result
	 */
	public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
		long startTime = System.currentTimeMillis();
		return getClient().callTool(callToolRequest).doOnNext(result -> {
			long delay = System.currentTimeMillis() - startTime;
			RetStatus retStatus = isErrorResult(result) ? RetStatus.RetFail : RetStatus.RetSuccess;
			reportCall(callToolRequest.name(), delay, retStatus);
		}).doOnError(ex -> {
			long delay = System.currentTimeMillis() - startTime;
			reportCall(callToolRequest.name(), delay, RetStatus.RetFail);
		});
	}

	/**
	 * Lists all tools available from the MCP server.
	 * @return a {@link Mono} emitting the list tools result
	 */
	public Mono<McpSchema.ListToolsResult> listTools() {
		return getClient().listTools();
	}

	@Override
	protected void closeClient() {
		getClient().close();
	}

	@Override
	protected void closeClientGracefully() {
		getClient().closeGracefully().block();
	}

}
