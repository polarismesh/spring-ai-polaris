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

package com.tencent.polaris.ai.core;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link PolarisSDKContextManager}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisSDKContextManager")
class PolarisSDKContextManagerTest {

	@DisplayName("initService with invalid address throws IllegalStateException")
	@Test
	void testInitServiceWithInvalidAddressThrowsIllegalStateException() {
		// Arrange
		PolarisSDKContextManager manager = new PolarisSDKContextManager(List.of(
				config -> config.getGlobal().getServerConnector().setAddresses(List.of("grpc://127.0.0.1:1"))));

		// Act & Assert
		assertThatThrownBy(manager::initService).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Failed to initialize Polaris SDK context");
	}

}
