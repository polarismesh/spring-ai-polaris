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

/**
 * Request DTO for the chat endpoint.
 *
 * <p>
 * Supports optional MCP resource and prompt integration:
 * <ul>
 * <li>{@code resourceUris} — resource URIs to read and include as context.</li>
 * <li>{@code promptName} / {@code promptArguments} — MCP prompt to use as the user
 * message instead of {@code message}.</li>
 * </ul>
 *
 * @author Haotian Zhang
 */
public class ChatRequest {

	private String message;

	private List<String> resourceUris;

	private String promptName;

	private Map<String, Object> promptArguments;

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<String> getResourceUris() {
		return this.resourceUris;
	}

	public void setResourceUris(List<String> resourceUris) {
		this.resourceUris = resourceUris;
	}

	public String getPromptName() {
		return this.promptName;
	}

	public void setPromptName(String promptName) {
		this.promptName = promptName;
	}

	public Map<String, Object> getPromptArguments() {
		return this.promptArguments;
	}

	public void setPromptArguments(Map<String, Object> promptArguments) {
		this.promptArguments = promptArguments;
	}

}
