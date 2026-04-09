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

package com.tencent.ai.polaris.mcp.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Flux;

/**
 * Provides lists of MCP capabilities (tools, resources, prompts) and handler method names
 * from the running MCP server. Used to decouple the contract reporter from the specific
 * MCP server type (Sync/Async, Stateful/Stateless).
 * <p>
 * <b>Note:</b> Methods in this interface may block. Do not call from Reactor threads.
 *
 * @author Haotian Zhang
 */
public interface McpServerCapabilitiesProvider {

	/**
	 * List all registered tools.
	 * @return the list of tools
	 */
	List<McpSchema.Tool> listTools();

	/**
	 * List all registered resources.
	 * @return the list of resources
	 */
	List<McpSchema.Resource> listResources();

	/**
	 * List all registered prompts.
	 * @return the list of prompts
	 */
	List<McpSchema.Prompt> listPrompts();

	/**
	 * List all request handler method names registered by the MCP server.
	 * @return the list of request handler method names (e.g. "ping", "tools/list")
	 */
	List<String> listRequestHandlerMethods();

	/**
	 * List all notification handler method names registered by the MCP server.
	 * @return the list of notification handler method names (e.g.
	 * "notifications/initialized")
	 */
	List<String> listNotificationHandlerMethods();

	/**
	 * Create a provider backed by synchronous list suppliers.
	 * @param tools supplier for tools
	 * @param resources supplier for resources
	 * @param prompts supplier for prompts
	 * @param capabilities the server capabilities used to derive handler method names
	 * @return a new capabilities provider
	 */
	static McpServerCapabilitiesProvider ofSync(Supplier<List<McpSchema.Tool>> tools,
			Supplier<List<McpSchema.Resource>> resources, Supplier<List<McpSchema.Prompt>> prompts,
			McpSchema.ServerCapabilities capabilities) {
		Objects.requireNonNull(tools, "tools supplier must not be null");
		Objects.requireNonNull(resources, "resources supplier must not be null");
		Objects.requireNonNull(prompts, "prompts supplier must not be null");
		Objects.requireNonNull(capabilities, "capabilities must not be null");
		List<String> requestHandlers = deriveRequestHandlerMethods(capabilities);
		List<String> notificationHandlers = deriveNotificationHandlerMethods();
		return new McpServerCapabilitiesProvider() {
			@Override
			public List<McpSchema.Tool> listTools() {
				return tools.get();
			}

			@Override
			public List<McpSchema.Resource> listResources() {
				return resources.get();
			}

			@Override
			public List<McpSchema.Prompt> listPrompts() {
				return prompts.get();
			}

			@Override
			public List<String> listRequestHandlerMethods() {
				return requestHandlers;
			}

			@Override
			public List<String> listNotificationHandlerMethods() {
				return notificationHandlers;
			}
		};
	}

	/**
	 * Create a provider backed by reactive (Flux) suppliers. Each method blocks the
	 * calling thread by invoking {@code collectList().block()} on its flux.
	 * @param tools supplier for tools flux
	 * @param resources supplier for resources flux
	 * @param prompts supplier for prompts flux
	 * @param capabilities the server capabilities used to derive handler method names
	 * @return a new capabilities provider that blocks on each access
	 */
	static McpServerCapabilitiesProvider ofAsync(Supplier<Flux<McpSchema.Tool>> tools,
			Supplier<Flux<McpSchema.Resource>> resources, Supplier<Flux<McpSchema.Prompt>> prompts,
			McpSchema.ServerCapabilities capabilities) {
		Objects.requireNonNull(tools, "tools supplier must not be null");
		Objects.requireNonNull(resources, "resources supplier must not be null");
		Objects.requireNonNull(prompts, "prompts supplier must not be null");
		Objects.requireNonNull(capabilities, "capabilities must not be null");
		List<String> requestHandlers = deriveRequestHandlerMethods(capabilities);
		List<String> notificationHandlers = deriveNotificationHandlerMethods();
		return new McpServerCapabilitiesProvider() {
			@Override
			public List<McpSchema.Tool> listTools() {
				return tools.get().collectList().block();
			}

			@Override
			public List<McpSchema.Resource> listResources() {
				return resources.get().collectList().block();
			}

			@Override
			public List<McpSchema.Prompt> listPrompts() {
				return prompts.get().collectList().block();
			}

			@Override
			public List<String> listRequestHandlerMethods() {
				return requestHandlers;
			}

			@Override
			public List<String> listNotificationHandlerMethods() {
				return notificationHandlers;
			}
		};
	}

	/**
	 * Derive request handler method names from {@link McpSchema.ServerCapabilities}.
	 * Mirrors the handler registration logic in the MCP SDK's
	 * {@code McpAsyncServer.prepareRequestHandlers()}.
	 * @param capabilities the server capabilities
	 * @return an unmodifiable list of request handler method names
	 */
	static List<String> deriveRequestHandlerMethods(McpSchema.ServerCapabilities capabilities) {
		List<String> methods = new ArrayList<>();
		// "ping" is always registered
		methods.add(McpSchema.METHOD_PING);

		if (capabilities.tools() != null) {
			methods.add(McpSchema.METHOD_TOOLS_LIST);
			methods.add(McpSchema.METHOD_TOOLS_CALL);
		}
		if (capabilities.resources() != null) {
			methods.add(McpSchema.METHOD_RESOURCES_LIST);
			methods.add(McpSchema.METHOD_RESOURCES_READ);
			methods.add(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST);
		}
		if (capabilities.prompts() != null) {
			methods.add(McpSchema.METHOD_PROMPT_LIST);
			methods.add(McpSchema.METHOD_PROMPT_GET);
		}
		if (capabilities.logging() != null) {
			methods.add(McpSchema.METHOD_LOGGING_SET_LEVEL);
		}
		if (capabilities.completions() != null) {
			methods.add(McpSchema.METHOD_COMPLETION_COMPLETE);
		}

		return Collections.unmodifiableList(methods);
	}

	/**
	 * Derive notification handler method names. These are always registered by the MCP
	 * server regardless of capabilities.
	 * @return an unmodifiable list of notification handler method names
	 */
	static List<String> deriveNotificationHandlerMethods() {
		return List.of(McpSchema.METHOD_NOTIFICATION_INITIALIZED,
				McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED);
	}

}
