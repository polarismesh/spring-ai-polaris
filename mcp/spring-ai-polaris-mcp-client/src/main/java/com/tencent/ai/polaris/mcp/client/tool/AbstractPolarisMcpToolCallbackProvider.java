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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;

import com.tencent.ai.polaris.mcp.client.AbstractPolarisMcpClient;
import com.tencent.polaris.api.utils.CollectionUtils;

/**
 * Base {@link ToolCallbackProvider} that aggregates tools from multiple
 * {@link AbstractPolarisMcpClient} instances. Subclasses provide the concrete
 * tool collection strategy via {@link #collectToolCallbacks(AbstractPolarisMcpClient, List)}.
 *
 * @param <C> the concrete Polaris MCP client type
 * @author Haotian Zhang
 */
public abstract class AbstractPolarisMcpToolCallbackProvider<C extends AbstractPolarisMcpClient<?>>
		implements ToolCallbackProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPolarisMcpToolCallbackProvider.class);

	private final List<C> mcpClients;

	protected AbstractPolarisMcpToolCallbackProvider(List<C> mcpClients) {
		Assert.notNull(mcpClients, "mcpClients must not be null");
		this.mcpClients = mcpClients;
	}

	@Override
	public ToolCallback[] getToolCallbacks() {
		List<ToolCallback> callbacks = new ArrayList<>();
		for (C client : this.mcpClients) {
			try {
				collectToolCallbacks(client, callbacks);
			}
			catch (Exception ex) {
				logger.warn("Failed to list tools from service={}, skipping", client.getServerName(), ex);
			}
		}
		ToolCallback[] array = callbacks.toArray(new ToolCallback[0]);
		List<String> duplicates = ToolUtils.getDuplicateToolNames(array);
		if (CollectionUtils.isNotEmpty(duplicates)) {
			throw new IllegalStateException(
					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicates)));
		}
		return array;
	}

	/**
	 * List tools from the given client and add the corresponding callbacks to the list.
	 * @param client the MCP client
	 * @param callbacks the list to add tool callbacks to
	 */
	protected abstract void collectToolCallbacks(C client, List<ToolCallback> callbacks);

}
