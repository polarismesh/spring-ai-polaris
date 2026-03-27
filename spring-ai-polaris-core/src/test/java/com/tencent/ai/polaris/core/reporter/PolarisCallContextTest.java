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

package com.tencent.ai.polaris.core.reporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tencent.polaris.api.pojo.RetStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCallContext}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisCallContext")
class PolarisCallContextTest {

	private static final String TEST_NAMESPACE = "default";

	private static final String TEST_SERVICE = "test-service";

	private static final String TEST_HOST = "127.0.0.1";

	private static final int TEST_PORT = 8080;

	private static final long TEST_DELAY_MS = 150L;

	@DisplayName("constructor and getters return correct values")
	@Test
	void testConstructorAndGetters() {
		// Arrange
		RetStatus retStatus = RetStatus.RetSuccess;

		// Act
		PolarisCallContext ctx = new PolarisCallContext(TEST_NAMESPACE, TEST_SERVICE, TEST_HOST, TEST_PORT,
				TEST_DELAY_MS, retStatus);

		// Assert
		assertThat(ctx.getNamespace()).isEqualTo(TEST_NAMESPACE);
		assertThat(ctx.getServiceName()).isEqualTo(TEST_SERVICE);
		assertThat(ctx.getHost()).isEqualTo(TEST_HOST);
		assertThat(ctx.getPort()).isEqualTo(TEST_PORT);
		assertThat(ctx.getDelayMs()).isEqualTo(TEST_DELAY_MS);
		assertThat(ctx.getRetStatus()).isEqualTo(RetStatus.RetSuccess);
	}

	@DisplayName("constructor with RetFail status")
	@Test
	void testConstructorWithRetFailStatus() {
		// Arrange
		RetStatus retStatus = RetStatus.RetFail;

		// Act
		PolarisCallContext ctx = new PolarisCallContext(TEST_NAMESPACE, TEST_SERVICE, TEST_HOST, TEST_PORT,
				TEST_DELAY_MS, retStatus);

		// Assert
		assertThat(ctx.getRetStatus()).isEqualTo(RetStatus.RetFail);
	}

}
