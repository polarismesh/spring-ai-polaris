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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tencent.ai.polaris.mcp.client.PolarisMcpSyncClient;
import com.tencent.ai.polaris.mcp.client.PolarisMcpSyncClientCluster;
import com.tencent.ai.polaris.mcp.client.tool.PolarisMcpSyncToolCallbackProvider;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * REST controller that exposes MCP capabilities as HTTP endpoints.
 *
 * <p>
 * Provides endpoints for:
 * <ul>
 * <li>{@code POST /mcp/chat} — sends a user message to the LLM with all discovered MCP
 * tools available for automatic tool calling.</li>
 * <li>{@code POST /mcp/tool/list} — lists all available MCP tools from discovered
 * servers.</li>
 * <li>{@code POST /mcp/tool/call} — directly invokes a specific MCP tool by name without
 * going through the LLM.</li>
 * <li>{@code POST /mcp/resource/list} — lists all available MCP resources from discovered
 * servers.</li>
 * <li>{@code POST /mcp/resource/read} — reads a specific MCP resource by URI.</li>
 * <li>{@code POST /mcp/prompt/list} — lists all available MCP prompts from discovered
 * servers.</li>
 * <li>{@code POST /mcp/prompt/get} — retrieves a specific MCP prompt by name with
 * arguments.</li>
 * </ul>
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/client/mcp")
public class McpClientController {

	private static final Logger LOG = LoggerFactory.getLogger(McpClientController.class);

	private final ChatClient chatClient;

	private final List<PolarisMcpSyncClientCluster> polarisMcpSyncClientClusters;

	public McpClientController(ChatModel chatModel, PolarisMcpSyncToolCallbackProvider toolCallbackProvider,
			List<PolarisMcpSyncClientCluster> polarisMcpSyncClientClusters) {
		this.chatClient = ChatClient.builder(chatModel)
			.defaultToolCallbacks(toolCallbackProvider)
			.build();
		this.polarisMcpSyncClientClusters = polarisMcpSyncClientClusters;
	}

	/**
	 * Chat endpoint that sends a user message to the LLM. The LLM can automatically
	 * invoke MCP tools during reasoning.
	 *
	 * <p>
	 * Optionally integrates MCP resources and prompts:
	 * <ul>
	 * <li>If {@code resourceUris} is provided, the resource contents are read and
	 * prepended to the user message as context.</li>
	 * <li>If {@code promptName} is provided, the MCP prompt result is used as the user
	 * message instead of the raw {@code message} field.</li>
	 * </ul>
	 * @param request the chat request containing the user message and optional
	 * resource/prompt parameters
	 * @return the LLM response as a string
	 */
	@PostMapping("/chat")
	public String chat(@RequestBody ChatRequest request) {
		StringBuilder userMessage = new StringBuilder();

		// Read MCP resources and include as context
		if (CollectionUtils.isNotEmpty(request.getResourceUris())) {
			String resourceContext = readResourceContents(request.getResourceUris());
			if (StringUtils.isNotBlank(resourceContext)) {
				userMessage.append("Context from MCP resources:\n")
					.append(resourceContext)
					.append("\n\n");
			}
		}

		// Use MCP prompt or raw message
		if (StringUtils.isNotBlank(request.getPromptName())) {
			String promptContent = getPromptContent(request.getPromptName(), request.getPromptArguments());
			if (StringUtils.isNotBlank(promptContent)) {
				userMessage.append(promptContent);
			}
			else {
				userMessage.append(request.getMessage());
			}
		}
		else {
			userMessage.append(request.getMessage());
		}

		LOG.info("User message content: {}", userMessage);
		return this.chatClient.prompt(userMessage.toString()).call().content();
	}

	/**
	 * Lists all available MCP tools from discovered servers.
	 * @return a list of tools aggregated from all connected MCP servers
	 */
	@PostMapping("/tool/list")
	public Object listTools() {
		List<McpSchema.Tool> allTools = new ArrayList<>();
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			McpSchema.ListToolsResult result = cluster.listTools();
			allTools.addAll(result.tools());
		}
		return allTools;
	}

	/**
	 * Direct tool call endpoint that invokes a specific MCP tool by name.
	 * @param request the tool call request containing tool name and arguments
	 * @return the tool call result content
	 */
	@PostMapping("/tool/call")
	public Object directToolCall(@RequestBody ToolCallRequest request) {
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			PolarisMcpSyncClient client = cluster.getClient();
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

	// ---- Resource endpoints ----

	/**
	 * Lists all available MCP resources from discovered servers.
	 * @return a list of resources aggregated from all connected MCP servers
	 */
	@PostMapping("/resource/list")
	public Object listResources() {
		List<McpSchema.Resource> allResources = new ArrayList<>();
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			McpSyncClient mcpClient = cluster.getClient().getDelegate();
			McpSchema.ListResourcesResult result = mcpClient.listResources();
			allResources.addAll(result.resources());
		}
		return allResources;
	}

	/**
	 * Reads a specific MCP resource by URI.
	 * @param request the resource read request containing the URI
	 * @return the resource contents
	 */
	@PostMapping("/resource/read")
	public Object readResource(@RequestBody ResourceReadRequest request) {
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			McpSyncClient mcpClient = cluster.getClient().getDelegate();
			McpSchema.ListResourcesResult resources = mcpClient.listResources();
			boolean hasTarget = resources.resources()
				.stream()
				.anyMatch(r -> r.uri().equals(request.getUri()));
			if (hasTarget) {
				McpSchema.ReadResourceResult result = mcpClient
					.readResource(new McpSchema.ReadResourceRequest(request.getUri(), null));
				return result.contents();
			}
		}
		return "Resource not found: " + request.getUri();
	}

	// ---- Prompt endpoints ----

	/**
	 * Lists all available MCP prompts from discovered servers.
	 * @return a list of prompts aggregated from all connected MCP servers
	 */
	@PostMapping("/prompt/list")
	public Object listPrompts() {
		List<McpSchema.Prompt> allPrompts = new ArrayList<>();
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			McpSyncClient mcpClient = cluster.getClient().getDelegate();
			McpSchema.ListPromptsResult result = mcpClient.listPrompts();
			allPrompts.addAll(result.prompts());
		}
		return allPrompts;
	}

	/**
	 * Retrieves a specific MCP prompt by name with arguments.
	 * @param request the prompt get request containing the name and arguments
	 * @return the prompt result with messages
	 */
	@PostMapping("/prompt/get")
	public Object getPrompt(@RequestBody PromptGetRequest request) {
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			McpSyncClient mcpClient = cluster.getClient().getDelegate();
			McpSchema.ListPromptsResult prompts = mcpClient.listPrompts();
			boolean hasTarget = prompts.prompts()
				.stream()
				.anyMatch(p -> p.name().equals(request.getName()));
			if (hasTarget) {
				Map<String, Object> arguments = request.getArguments() != null ? request.getArguments() : Map.of();
				McpSchema.GetPromptResult result = mcpClient
					.getPrompt(new McpSchema.GetPromptRequest(request.getName(), arguments, null));
				return result;
			}
		}
		return "Prompt not found: " + request.getName();
	}

	// ---- Private helpers ----

	private String readResourceContents(List<String> uris) {
		StringBuilder sb = new StringBuilder();
		for (String uri : uris) {
			for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
				McpSyncClient mcpClient = cluster.getClient().getDelegate();
				try {
					McpSchema.ReadResourceResult result = mcpClient
						.readResource(new McpSchema.ReadResourceRequest(uri, null));
					for (McpSchema.ResourceContents content : result.contents()) {
						if (content instanceof McpSchema.TextResourceContents text) {
							sb.append("[").append(uri).append("]\n").append(text.text()).append("\n\n");
						}
					}
					break;
				}
				catch (Exception ex) {
					LOG.debug("Failed to read resource {} from cluster, trying next: {}", uri, ex.getMessage());
				}
			}
		}
		return sb.toString();
	}

	private String getPromptContent(String promptName, Map<String, Object> arguments) {
		Map<String, Object> args = arguments != null ? arguments : Map.of();
		for (PolarisMcpSyncClientCluster cluster : this.polarisMcpSyncClientClusters) {
			McpSyncClient mcpClient = cluster.getClient().getDelegate();
			try {
				McpSchema.ListPromptsResult prompts = mcpClient.listPrompts();
				boolean hasTarget = prompts.prompts()
					.stream()
					.anyMatch(p -> p.name().equals(promptName));
				if (hasTarget) {
					McpSchema.GetPromptResult result = mcpClient
						.getPrompt(new McpSchema.GetPromptRequest(promptName, args, null));
					return result.messages()
						.stream()
						.map(msg -> {
							if (msg.content() instanceof McpSchema.TextContent text) {
								return text.text();
							}
							return "";
						})
						.collect(Collectors.joining("\n"));
				}
			}
			catch (Exception ex) {
				LOG.debug("Failed to get prompt {} from cluster, trying next: {}", promptName, ex.getMessage());
			}
		}
		return null;
	}

}
