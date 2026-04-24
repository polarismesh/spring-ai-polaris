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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.client.pojo.Node;

/**
 * Connection pool of {@link PolarisMcpSyncClient} instances for a Polaris service.
 * Each healthy instance gets its own single-connection wrapper; tool calls and listings
 * are delegated to a round-robin selected wrapper.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpSyncClientCluster extends AbstractPolarisMcpClientCluster<PolarisMcpSyncClient> {

	private static final Logger logger = LoggerFactory.getLogger(PolarisMcpSyncClientCluster.class);

	private final List<McpSyncClientCustomizer> customizers;

	public PolarisMcpSyncClientCluster(String namespace, String serverName, String clientName, String scheme,
			String clientVersion, boolean initialized, PolarisSDKContextManager sdkContextManager,
			PolarisReporter polarisReporter, WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			List<McpSyncClientCustomizer> customizers) {
		super(namespace, serverName, clientName, scheme, clientVersion, initialized, sdkContextManager,
				polarisReporter, webClientBuilder, objectMapper);
		this.customizers = customizers != null ? customizers : List.of();
	}

	@Override
	protected PolarisMcpSyncClient createClientWrapper(McpClientTransport transport,
			McpSchema.Implementation clientInfo, String connectedName, Node node, boolean initialized) {
		McpClient.SyncSpec spec = McpClient.sync(transport).clientInfo(clientInfo);
		for (McpSyncClientCustomizer customizer : this.customizers) {
			customizer.customize(connectedName, spec);
		}
		McpSyncClient client = spec.build();
		if (initialized) {
			// Failure is tolerated: SDK will retry initialize lazily on first business call
			try {
				client.initialize();
			}
			catch (Exception ex) {
				logger.warn("[Polaris MCP Client] Eager initialize failed for {}; will retry lazily on first call",
						connectedName, ex);
			}
		}
		return new PolarisMcpSyncClient(client, node, getNamespace(), getServerName(),
				getPolarisReporter());
	}

	/**
	 * Calls a tool on a round-robin selected MCP server instance.
	 * @param callToolRequest the call tool request
	 * @return the call tool result
	 */
	public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
		return getClient().callTool(callToolRequest);
	}

	/**
	 * Lists all tools from a round-robin selected MCP server instance.
	 * @return the list tools result
	 */
	public McpSchema.ListToolsResult listTools() {
		return getClient().listTools();
	}

}
