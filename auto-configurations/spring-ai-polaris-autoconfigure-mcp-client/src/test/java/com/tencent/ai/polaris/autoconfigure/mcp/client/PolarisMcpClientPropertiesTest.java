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

package com.tencent.ai.polaris.autoconfigure.mcp.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tencent.ai.polaris.autoconfigure.mcp.client.PolarisMcpClientProperties.PolarisMcpParameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisMcpClientProperties}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpClientProperties")
class PolarisMcpClientPropertiesTest {

	@DisplayName("default values are set correctly")
	@Test
	void testDefaultValues() {
		// Arrange
		PolarisMcpClientProperties properties = new PolarisMcpClientProperties();

		// Act & Assert
		assertThat(properties.isEnabled()).isTrue();
		assertThat(properties.getNamespace()).isNull();
		assertThat(properties.getServices()).isEmpty();
	}

	@DisplayName("setters and getters work correctly")
	@Test
	void testSettersAndGetters() {
		// Arrange
		PolarisMcpClientProperties properties = new PolarisMcpClientProperties();
		Map<String, PolarisMcpParameters> services = new LinkedHashMap<>();
		services.put("svc-a", new PolarisMcpParameters(null, null, null));
		services.put("svc-b", new PolarisMcpParameters("my-real-svc", "prod-ns", "https"));

		// Act
		properties.setEnabled(false);
		properties.setNamespace("test-ns");
		properties.setServices(services);

		// Assert
		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.getNamespace()).isEqualTo("test-ns");
		assertThat(properties.getServices()).hasSize(2);
		assertThat(properties.getServices()).containsKeys("svc-a", "svc-b");
		assertThat(properties.getServices().get("svc-b").serviceName()).isEqualTo("my-real-svc");
		assertThat(properties.getServices().get("svc-b").namespace()).isEqualTo("prod-ns");
		assertThat(properties.getServices().get("svc-b").scheme()).isEqualTo("https");
	}

	@DisplayName("toString contains all fields")
	@Test
	void testToString() {
		// Arrange
		PolarisMcpClientProperties properties = new PolarisMcpClientProperties();
		properties.setEnabled(true);
		properties.setNamespace("my-ns");
		Map<String, PolarisMcpParameters> services = new LinkedHashMap<>();
		services.put("svc-a", new PolarisMcpParameters(null, null, null));
		properties.setServices(services);

		// Act
		String result = properties.toString();

		// Assert
		assertThat(result).contains("enabled=true");
		assertThat(result).contains("namespace='my-ns'");
		assertThat(result).contains("svc-a");
	}

	@DisplayName("PolarisMcpParameters record accessors work")
	@Test
	void testPolarisMcpParametersRecord() {
		// Arrange
		PolarisMcpParameters params = new PolarisMcpParameters("my-svc", "my-ns", "https");

		// Act & Assert
		assertThat(params.serviceName()).isEqualTo("my-svc");
		assertThat(params.namespace()).isEqualTo("my-ns");
		assertThat(params.scheme()).isEqualTo("https");
	}

	@DisplayName("PolarisMcpParameters allows null fields")
	@Test
	void testPolarisMcpParametersNullFields() {
		// Arrange
		PolarisMcpParameters params = new PolarisMcpParameters(null, null, null);

		// Act & Assert
		assertThat(params.serviceName()).isNull();
		assertThat(params.namespace()).isNull();
		assertThat(params.scheme()).isNull();
	}

}
