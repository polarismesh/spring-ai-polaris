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

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.tencent.ai.polaris.mcp.client.PolarisMcpAsyncClientCluster;

/**
 * {@link ToolCallbackProvider} that aggregates tools from multiple
 * {@link PolarisMcpAsyncClientCluster} instances. Async counterpart of
 * {@link PolarisMcpSyncToolCallbackProvider}.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpAsyncToolCallbackProvider
		extends AbstractPolarisMcpToolCallbackProvider<PolarisMcpAsyncClientCluster> {

	public PolarisMcpAsyncToolCallbackProvider(List<PolarisMcpAsyncClientCluster> clientClusters) {
		super(clientClusters);
	}

	@Override
	protected void collectToolCallbacks(PolarisMcpAsyncClientCluster clientCluster, List<ToolCallback> callbacks) {
		McpSchema.ListToolsResult result = clientCluster.listTools().block();
		if (result != null) {
			result.tools()
				.forEach(tool -> callbacks.add(new PolarisMcpAsyncToolCallback(clientCluster, tool)));
		}
	}

}
