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

import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.util.Assert;

import com.tencent.ai.polaris.mcp.client.AbstractPolarisMcpClient;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * Base {@link ToolCallback} implementation for Polaris MCP clients. Handles tool
 * definition creation, input validation, error handling, and result serialization.
 * Subclasses only need to provide the server name and the concrete
 * {@link #doCallTool(CallToolRequest)} implementation.
 *
 * @author Haotian Zhang
 */
public abstract class AbstractPolarisMcpToolCallback implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPolarisMcpToolCallback.class);

	private final Tool tool;

	private final ToolDefinition toolDefinition;

	protected AbstractPolarisMcpToolCallback(String serverName, Tool tool) {
		Assert.hasText(serverName, "serverName must not be blank");
		Assert.notNull(tool, "tool must not be null");
		this.tool = tool;
		String prefixedToolName = McpToolUtils.prefixedToolName(serverName, tool.name());
		this.toolDefinition = McpToolUtils.createToolDefinition(prefixedToolName, this.tool);
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public String call(String toolInput) {
		if (StringUtils.isBlank(toolInput)) {
			logger.warn("Tool call arguments are null or empty for MCP tool: {}. Using empty JSON object as default.",
					this.tool.name());
			toolInput = "{}";
		}

		Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(toolInput);

		CallToolResult response;
		try {
			CallToolRequest request = new CallToolRequest(this.tool.name(), arguments);
			response = doCallTool(request);
		}
		catch (Exception ex) {
			logger.error("Exception while tool calling: ", ex);
			throw new ToolExecutionException(getToolDefinition(), ex);
		}

		if (AbstractPolarisMcpClient.isErrorResult(response)) {
			logger.error("Error calling tool: {}", response.content());
			throw new ToolExecutionException(getToolDefinition(),
					new IllegalStateException("Error calling tool: " + response.content()));
		}
		return ModelOptionsUtils.toJsonString(response != null ? response.content() : null);
	}

	/**
	 * Execute the tool call. Subclasses delegate to the underlying sync or async MCP
	 * client.
	 * @param request the call tool request
	 * @return the call tool result
	 */
	protected abstract CallToolResult doCallTool(CallToolRequest request);

}
