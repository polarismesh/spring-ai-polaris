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

package com.tencent.ai.polaris.autoconfigure.mcp.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisMcpServerProperties}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpServerProperties")
class PolarisMcpServerPropertiesTest {

	private PolarisMcpServerProperties properties;

	@BeforeEach
	void setUp() {
		this.properties = new PolarisMcpServerProperties();
	}

	@DisplayName("default values are set correctly")
	@Test
	void testDefaultValues() {
		// Arrange (defaults set in constructor)

		// Act & Assert
		assertThat(this.properties.isEnabled()).isTrue();
		assertThat(this.properties.getServiceName()).isNull();
		assertThat(this.properties.isStrictCompatible()).isFalse();
		assertThat(this.properties.getHost()).isNull();
		assertThat(this.properties.getPort()).isEqualTo(-1);
	}

	@DisplayName("custom values are set correctly via setters")
	@Test
	void testCustomValues() {
		// Arrange
		this.properties.setEnabled(false);
		this.properties.setServiceName("my-mcp");
		this.properties.setStrictCompatible(true);
		this.properties.setHost("127.0.0.1");
		this.properties.setPort(9090);

		// Act & Assert
		assertThat(this.properties.isEnabled()).isFalse();
		assertThat(this.properties.getServiceName()).isEqualTo("my-mcp");
		assertThat(this.properties.isStrictCompatible()).isTrue();
		assertThat(this.properties.getHost()).isEqualTo("127.0.0.1");
		assertThat(this.properties.getPort()).isEqualTo(9090);
	}

	@DisplayName("toString contains all fields")
	@Test
	void testToString() {
		// Arrange (use defaults)

		// Act
		String result = this.properties.toString();

		// Assert
		assertThat(result).contains("PolarisMcpServerProperties");
		assertThat(result).contains("enabled=");
		assertThat(result).contains("serviceName=");
		assertThat(result).contains("strictCompatible=");
		assertThat(result).contains("host=");
		assertThat(result).contains("port=");
	}

}
