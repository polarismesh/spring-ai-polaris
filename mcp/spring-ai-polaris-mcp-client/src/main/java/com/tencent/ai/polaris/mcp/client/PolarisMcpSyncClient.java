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
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.api.pojo.RetStatus;

/**
 * Polaris-backed MCP sync client. Extends {@link AbstractPolarisMcpClient} with
 * synchronous {@link McpSyncClient} instances.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpSyncClient extends AbstractPolarisMcpClient<McpSyncClient> {

	private final List<McpSyncClientCustomizer> customizers;

	public PolarisMcpSyncClient(String namespace, String serverName, String scheme, String clientVersion,
			boolean initialized, PolarisSDKContextManager sdkContextManager, PolarisReporter polarisReporter,
			WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			List<McpSyncClientCustomizer> customizers) {
		super(namespace, serverName, scheme, clientVersion, initialized, sdkContextManager, polarisReporter,
				webClientBuilder, objectMapper);
		this.customizers = customizers != null ? customizers : List.of();
	}

	@Override
	protected McpSyncClient buildClient(String clientName, McpClientTransport transport,
			McpSchema.Implementation clientInfo, boolean initialized) {
		McpClient.SyncSpec spec = McpClient.sync(transport).clientInfo(clientInfo);
		for (McpSyncClientCustomizer customizer : this.customizers) {
			customizer.customize(clientName, spec);
		}
		McpSyncClient client = spec.build();
		if (initialized) {
			client.initialize();
		}
		return client;
	}

	@Override
	protected void closeClient(McpSyncClient client) {
		client.close();
	}

	@Override
	protected void closeClientGracefully(McpSyncClient client) {
		client.closeGracefully();
	}

	/**
	 * Lists all tools available from a selected MCP server instance.
	 * @return the list tools result
	 */
	public McpSchema.ListToolsResult listTools() {
		return getClient().listTools();
	}

	/**
	 * Calls a tool on a selected MCP server instance and reports the result to Polaris.
	 * @param callToolRequest the call tool request
	 * @return the call tool result
	 */
	public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
		McpSyncClient client = getClient();
		String clientName = client.getClientInfo().name();
		long startTime = System.currentTimeMillis();
		RetStatus retStatus = RetStatus.RetSuccess;
		try {
			McpSchema.CallToolResult result = client.callTool(callToolRequest);
			if (isErrorResult(result)) {
				retStatus = RetStatus.RetFail;
			}
			return result;
		}
		catch (Exception ex) {
			retStatus = RetStatus.RetFail;
			throw ex;
		}
		finally {
			long delay = System.currentTimeMillis() - startTime;
			reportCall(clientName, callToolRequest.name(), delay, retStatus);
		}
	}

}
