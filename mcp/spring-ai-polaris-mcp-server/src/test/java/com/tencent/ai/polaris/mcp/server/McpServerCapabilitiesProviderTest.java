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

import java.util.Collections;
import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link McpServerCapabilitiesProvider}.
 *
 * @author Haotian Zhang
 */
@DisplayName("McpServerCapabilitiesProvider")
class McpServerCapabilitiesProviderTest {

	@DisplayName("deriveRequestHandlerMethods with all capabilities enabled")
	@Test
	void testDeriveRequestHandlerMethodsAllCapabilities() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true)
			.resources(true, true)
			.prompts(true)
			.logging()
			.completions()
			.build();

		// Act
		List<String> methods = McpServerCapabilitiesProvider.deriveRequestHandlerMethods(capabilities);

		// Assert
		assertThat(methods).containsExactly(
				McpSchema.METHOD_PING,
				McpSchema.METHOD_TOOLS_LIST, McpSchema.METHOD_TOOLS_CALL,
				McpSchema.METHOD_RESOURCES_LIST, McpSchema.METHOD_RESOURCES_READ,
				McpSchema.METHOD_RESOURCES_TEMPLATES_LIST,
				McpSchema.METHOD_PROMPT_LIST, McpSchema.METHOD_PROMPT_GET,
				McpSchema.METHOD_LOGGING_SET_LEVEL,
				McpSchema.METHOD_COMPLETION_COMPLETE);
	}

	@DisplayName("deriveRequestHandlerMethods with only tools capability")
	@Test
	void testDeriveRequestHandlerMethodsToolsOnly() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true)
			.build();

		// Act
		List<String> methods = McpServerCapabilitiesProvider.deriveRequestHandlerMethods(capabilities);

		// Assert
		assertThat(methods).containsExactly(
				McpSchema.METHOD_PING,
				McpSchema.METHOD_TOOLS_LIST, McpSchema.METHOD_TOOLS_CALL);
	}

	@DisplayName("deriveRequestHandlerMethods with only resources capability")
	@Test
	void testDeriveRequestHandlerMethodsResourcesOnly() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
			.resources(false, true)
			.build();

		// Act
		List<String> methods = McpServerCapabilitiesProvider.deriveRequestHandlerMethods(capabilities);

		// Assert
		assertThat(methods).containsExactly(
				McpSchema.METHOD_PING,
				McpSchema.METHOD_RESOURCES_LIST, McpSchema.METHOD_RESOURCES_READ,
				McpSchema.METHOD_RESOURCES_TEMPLATES_LIST);
	}

	@DisplayName("deriveRequestHandlerMethods with no capabilities returns only ping")
	@Test
	void testDeriveRequestHandlerMethodsNoCapabilities() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder().build();

		// Act
		List<String> methods = McpServerCapabilitiesProvider.deriveRequestHandlerMethods(capabilities);

		// Assert
		assertThat(methods).containsExactly(McpSchema.METHOD_PING);
	}

	@DisplayName("deriveNotificationHandlerMethods returns fixed methods")
	@Test
	void testDeriveNotificationHandlerMethods() {
		// Arrange (nothing to arrange)

		// Act
		List<String> methods = McpServerCapabilitiesProvider.deriveNotificationHandlerMethods();

		// Assert
		assertThat(methods).containsExactly(
				McpSchema.METHOD_NOTIFICATION_INITIALIZED,
				McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED);
	}

	@DisplayName("ofSync returns provider with handler methods")
	@Test
	void testOfSyncWithHandlerMethods() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true)
			.build();

		// Act
		McpServerCapabilitiesProvider provider = McpServerCapabilitiesProvider.ofSync(
				Collections::emptyList, Collections::emptyList, Collections::emptyList, capabilities);

		// Assert
		assertThat(provider.listRequestHandlerMethods()).containsExactly(
				McpSchema.METHOD_PING,
				McpSchema.METHOD_TOOLS_LIST, McpSchema.METHOD_TOOLS_CALL);
		assertThat(provider.listNotificationHandlerMethods()).containsExactly(
				McpSchema.METHOD_NOTIFICATION_INITIALIZED,
				McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED);
	}

	@DisplayName("ofAsync returns provider with handler methods")
	@Test
	void testOfAsyncWithHandlerMethods() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
			.prompts(true)
			.build();

		// Act
		McpServerCapabilitiesProvider provider = McpServerCapabilitiesProvider.ofAsync(
				Flux::empty, Flux::empty, Flux::empty, capabilities);

		// Assert
		assertThat(provider.listRequestHandlerMethods()).containsExactly(
				McpSchema.METHOD_PING,
				McpSchema.METHOD_PROMPT_LIST, McpSchema.METHOD_PROMPT_GET);
		assertThat(provider.listNotificationHandlerMethods()).containsExactly(
				McpSchema.METHOD_NOTIFICATION_INITIALIZED,
				McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED);
	}

	@DisplayName("ofSync throws on null capabilities")
	@Test
	void testOfSyncThrowsOnNullCapabilities() {
		// Arrange & Act & Assert
		assertThatThrownBy(() -> McpServerCapabilitiesProvider.ofSync(
				Collections::emptyList, Collections::emptyList, Collections::emptyList, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("capabilities");
	}

	@DisplayName("ofAsync throws on null capabilities")
	@Test
	void testOfAsyncThrowsOnNullCapabilities() {
		// Arrange & Act & Assert
		assertThatThrownBy(() -> McpServerCapabilitiesProvider.ofAsync(
				Flux::empty, Flux::empty, Flux::empty, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("capabilities");
	}

	@DisplayName("deriveRequestHandlerMethods result is unmodifiable")
	@Test
	void testDeriveRequestHandlerMethodsUnmodifiable() {
		// Arrange
		McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
			.tools(true)
			.build();

		// Act
		List<String> methods = McpServerCapabilitiesProvider.deriveRequestHandlerMethods(capabilities);

		// Assert
		assertThatThrownBy(() -> methods.add("extra"))
			.isInstanceOf(UnsupportedOperationException.class);
	}

}
