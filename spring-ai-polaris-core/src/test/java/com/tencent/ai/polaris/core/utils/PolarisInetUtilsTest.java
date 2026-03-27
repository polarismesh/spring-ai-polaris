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

package com.tencent.ai.polaris.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisInetUtils}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisInetUtils")
class PolarisInetUtilsTest {

	@DisplayName("findFirstNonLoopbackAddress returns non-null address")
	@Test
	void testFindFirstNonLoopbackAddressReturnsNonNull() {
		// Arrange & Act
		String address = PolarisInetUtils.findFirstNonLoopbackAddress();

		// Assert
		assertThat(address).isNotNull();
		assertThat(address).isNotEmpty();
	}

	@DisplayName("findFirstNonLoopbackAddress does not return loopback address")
	@Test
	void testFindFirstNonLoopbackAddressNotLoopback() {
		// Arrange & Act
		String address = PolarisInetUtils.findFirstNonLoopbackAddress();

		// Assert
		assertThat(address).isNotNull();
		assertThat(address).doesNotStartWith("127.");
	}

	@DisplayName("findFirstNonLoopbackAddress does not contain zone id")
	@Test
	void testFindFirstNonLoopbackAddressNoZoneId() {
		// Arrange & Act
		String address = PolarisInetUtils.findFirstNonLoopbackAddress();

		// Assert
		assertThat(address).isNotNull();
		assertThat(address).doesNotContain("%");
	}

	@DisplayName("getIpString with ipv4 returns valid address or null")
	@Test
	void testGetIpStringIpv4() {
		// Arrange & Act
		String ipv4 = PolarisInetUtils.getIpString(false);

		// Assert
		if (ipv4 != null) {
			assertThat(ipv4).doesNotContain(":");
			assertThat(ipv4).doesNotContain("%");
		}
	}

	@DisplayName("getIpString with ipv6 returns valid address or null")
	@Test
	void testGetIpStringIpv6() {
		// Arrange & Act
		String ipv6 = PolarisInetUtils.getIpString(true);

		// Assert
		if (ipv6 != null) {
			assertThat(ipv6).doesNotContain("%");
		}
	}

}
