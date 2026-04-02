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

package com.tencent.ai.polaris.example.mcp.client;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tencent.ai.polaris.mcp.client.PolarisMcpSyncClient;
import com.tencent.ai.polaris.mcp.client.tool.PolarisMcpSyncToolCallbackProvider;

/**
 * REST controller that exposes MCP capabilities as HTTP endpoints.
 *
 * <p>
 * Provides two endpoints:
 * <ul>
 * <li>{@code POST /mcp/chat} — sends a user message to the LLM with all discovered MCP
 * tools available for automatic tool calling.</li>
 * <li>{@code POST /mcp/tool/call} — directly invokes a specific MCP tool by name without
 * going through the LLM.</li>
 * </ul>
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/client/mcp")
public class McpClientController {

	private final ChatClient chatClient;

	private final List<PolarisMcpSyncClient> polarisMcpSyncClients;

	public McpClientController(ChatModel chatModel, PolarisMcpSyncToolCallbackProvider toolCallbackProvider,
			List<PolarisMcpSyncClient> polarisMcpSyncClients) {
		this.chatClient = ChatClient.builder(chatModel)
			.defaultToolCallbacks(toolCallbackProvider)
			.build();
		this.polarisMcpSyncClients = polarisMcpSyncClients;
	}

	/**
	 * Chat endpoint that sends a user message to the LLM. The LLM can automatically
	 * invoke MCP tools during reasoning.
	 * @param request the chat request containing the user message
	 * @return the LLM response as a string
	 */
	@PostMapping("/chat")
	public String chat(@RequestBody ChatRequest request) {
		return this.chatClient.prompt(request.getMessage()).call().content();
	}

	/**
	 * Direct tool call endpoint that invokes a specific MCP tool by name.
	 * @param request the tool call request containing tool name and arguments
	 * @return the tool call result content
	 */
	@PostMapping("/tool/call")
	public Object directToolCall(@RequestBody ToolCallRequest request) {
		for (PolarisMcpSyncClient client : this.polarisMcpSyncClients) {
			McpSchema.ListToolsResult tools = client.listTools();
			boolean hasTarget = tools.tools()
				.stream()
				.anyMatch(tool -> tool.name().equals(request.getToolName()));
			if (hasTarget) {
				Map<String, Object> arguments = request.getArguments() != null ? request.getArguments() : Map.of();
				McpSchema.CallToolResult result = client
					.callTool(new McpSchema.CallToolRequest(request.getToolName(), arguments));
				return result.content();
			}
		}
		return "Tool not found: " + request.getToolName();
	}

}
