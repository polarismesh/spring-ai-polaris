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

import java.util.Objects;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.springframework.ai.tool.ToolCallback;

import com.tencent.ai.polaris.mcp.client.PolarisMcpSyncClientCluster;

/**
 * {@link ToolCallback} implementation that delegates tool calls to a
 * {@link PolarisMcpSyncClientCluster}. Adapts MCP tools discovered via Polaris service
 * discovery to Spring AI's tool callback interface.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpSyncToolCallback extends AbstractPolarisMcpToolCallback {

	private final PolarisMcpSyncClientCluster clientCluster;

	public PolarisMcpSyncToolCallback(PolarisMcpSyncClientCluster clientCluster, Tool tool) {
		super(Objects.requireNonNull(clientCluster, "clientCluster must not be null").getServerName(), tool);
		this.clientCluster = clientCluster;
	}

	@Override
	protected CallToolResult doCallTool(CallToolRequest request) {
		return this.clientCluster.callTool(request);
	}

}
