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

package com.tencent.ai.polaris.autoconfigure.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCoreProperties}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisCoreProperties")
class PolarisCorePropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisCoreAutoConfiguration.class));

	@DisplayName("default values are bound correctly")
	@Test
	void testDefaultValues() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisCoreProperties props = ctx.getBean(PolarisCoreProperties.class);
			assertThat(props.getAddress()).containsExactly("127.0.0.1:8091");
			assertThat(props.getNamespace()).isEqualTo("default");
			assertThat(props.isPreferIpv6()).isFalse();
		});
	}

	@DisplayName("custom property values are bound correctly")
	@Test
	void testCustomValues() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues(
					"spring.ai.polaris.address=127.0.0.1:8091,127.0.0.2:8091",
					"spring.ai.polaris.namespace=production")
			.run(ctx -> {
				PolarisCoreProperties props = ctx.getBean(PolarisCoreProperties.class);
				assertThat(props.getAddress()).containsExactly("127.0.0.1:8091", "127.0.0.2:8091");
				assertThat(props.getNamespace()).isEqualTo("production");
			});
	}

	@DisplayName("toString contains all fields")
	@Test
	void testToString() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisCoreProperties props = ctx.getBean(PolarisCoreProperties.class);
			String result = props.toString();
			assertThat(result).contains("PolarisCoreProperties");
			assertThat(result).contains("address=");
			assertThat(result).contains("namespace=");
			assertThat(result).contains("preferIpv6=");
		});
	}

}
