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

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Flux;

/**
 * Provides lists of MCP capabilities (tools, resources, prompts) from the running MCP
 * server. Used to decouple the contract reporter from the specific MCP server type
 * (Sync/Async, Stateful/Stateless).
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
	 * Create a provider backed by synchronous list suppliers.
	 * @param tools supplier for tools
	 * @param resources supplier for resources
	 * @param prompts supplier for prompts
	 * @return a new capabilities provider
	 */
	static McpServerCapabilitiesProvider ofSync(Supplier<List<McpSchema.Tool>> tools,
			Supplier<List<McpSchema.Resource>> resources, Supplier<List<McpSchema.Prompt>> prompts) {
		Objects.requireNonNull(tools, "tools supplier must not be null");
		Objects.requireNonNull(resources, "resources supplier must not be null");
		Objects.requireNonNull(prompts, "prompts supplier must not be null");
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
		};
	}

	/**
	 * Create a provider backed by reactive (Flux) suppliers. Each method blocks the
	 * calling thread by invoking {@code collectList().block()} on its flux.
	 * @param tools supplier for tools flux
	 * @param resources supplier for resources flux
	 * @param prompts supplier for prompts flux
	 * @return a new capabilities provider that blocks on each access
	 */
	static McpServerCapabilitiesProvider ofAsync(Supplier<Flux<McpSchema.Tool>> tools,
			Supplier<Flux<McpSchema.Resource>> resources, Supplier<Flux<McpSchema.Prompt>> prompts) {
		Objects.requireNonNull(tools, "tools supplier must not be null");
		Objects.requireNonNull(resources, "resources supplier must not be null");
		Objects.requireNonNull(prompts, "prompts supplier must not be null");
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
		};
	}

}
