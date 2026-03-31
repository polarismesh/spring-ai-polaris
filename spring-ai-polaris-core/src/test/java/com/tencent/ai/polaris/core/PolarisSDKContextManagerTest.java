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

package com.tencent.ai.polaris.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tencent.polaris.test.mock.discovery.NamingServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link PolarisSDKContextManager}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisSDKContextManager")
class PolarisSDKContextManagerTest {

	private static NamingServer namingServer;

	private static int port;

	@BeforeAll
	static void beforeAll() throws IOException {
		namingServer = NamingServer.startNamingServer(-1);
		port = namingServer.getPort();
	}

	@AfterAll
	static void afterAll() {
		if (namingServer != null) {
			namingServer.terminate();
		}
	}

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@DisplayName("initService with invalid address throws IllegalStateException")
	@Test
	void testInitServiceWithInvalidAddressThrowsIllegalStateException() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector().setAddresses(List.of())));

		// Act & Assert
		assertThatThrownBy(manager::initService).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Failed to initialize Polaris SDK context");
	}

	@DisplayName("modifiers are applied in order during initService")
	@Test
	void testModifiersAppliedInOrder() {
		// Arrange
		List<String> executionOrder = new ArrayList<>();
		PolarisConfigModifier first = new PolarisConfigModifier() {
			@Override
			public void modify(com.tencent.polaris.factory.config.ConfigurationImpl configuration) {
				executionOrder.add("first");
			}

			@Override
			public int getOrder() {
				return 0;
			}
		};
		PolarisConfigModifier second = new PolarisConfigModifier() {
			@Override
			public void modify(com.tencent.polaris.factory.config.ConfigurationImpl configuration) {
				executionOrder.add("second");
				// Set empty addresses to force init failure so we can verify order
				configuration.getGlobal().getServerConnector().setAddresses(List.of());
			}

			@Override
			public int getOrder() {
				return 10;
			}
		};
		// Pass in reverse order to prove sorting works
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(second, first));

		// Act
		assertThatThrownBy(manager::initService).isInstanceOf(IllegalStateException.class);

		// Assert
		assertThat(executionOrder).containsExactly("first", "second");
	}

	@DisplayName("innerDestroy does not throw when no context is initialized")
	@Test
	void testInnerDestroyWithNoContext() {
		// Arrange & Act & Assert
		assertThatCode(PolarisSDKContextManager::innerDestroy).doesNotThrowAnyException();
	}

	@DisplayName("getConsumerAPI returns non-null after successful init with mock server")
	@Test
	void testGetConsumerAPIReturnsNonNull() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector()
					.setAddresses(List.of("127.0.0.1:" + port))));

		// Act & Assert
		assertThat(manager.getConsumerAPI()).isNotNull();
	}

	@DisplayName("getProviderAPI returns non-null after successful init with mock server")
	@Test
	void testGetProviderAPIReturnsNonNull() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector()
					.setAddresses(List.of("127.0.0.1:" + port))));

		// Act & Assert
		assertThat(manager.getProviderAPI()).isNotNull();
	}

	@DisplayName("getSDKContext returns non-null after successful init with mock server")
	@Test
	void testGetSDKContextReturnsNonNull() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector()
					.setAddresses(List.of("127.0.0.1:" + port))));

		// Act & Assert
		assertThat(manager.getSDKContext()).isNotNull();
	}

	@DisplayName("innerDestroy cleans up after successful init")
	@Test
	void testInnerDestroyAfterInit() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector()
					.setAddresses(List.of("127.0.0.1:" + port))));
		manager.initService();

		// Act & Assert
		assertThatCode(PolarisSDKContextManager::innerDestroy).doesNotThrowAnyException();
	}

	@DisplayName("second initService call is a no-op when already initialized")
	@Test
	void testInitServiceIdempotent() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector()
					.setAddresses(List.of("127.0.0.1:" + port))));
		manager.initService();

		// Act & Assert — second call should not throw
		assertThatCode(manager::initService).doesNotThrowAnyException();
	}

}
