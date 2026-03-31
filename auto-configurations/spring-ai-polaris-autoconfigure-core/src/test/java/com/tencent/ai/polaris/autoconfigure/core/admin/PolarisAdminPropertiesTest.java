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

package com.tencent.ai.polaris.autoconfigure.core.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisAdminProperties}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisAdminProperties")
class PolarisAdminPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisAdminAutoConfiguration.class));

	@DisplayName("default values are bound correctly")
	@Test
	void testDefaultValues() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisAdminProperties props = ctx.getBean(PolarisAdminProperties.class);
			assertThat(props.getHost()).isEqualTo("0.0.0.0");
			assertThat(props.getPort()).isEqualTo(28080);
		});
	}

	@DisplayName("custom property values are bound correctly")
	@Test
	void testCustomValues() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues(
					"spring.ai.polaris.admin.host=127.0.0.1",
					"spring.ai.polaris.admin.port=9090")
			.run(ctx -> {
				PolarisAdminProperties props = ctx.getBean(PolarisAdminProperties.class);
				assertThat(props.getHost()).isEqualTo("127.0.0.1");
				assertThat(props.getPort()).isEqualTo(9090);
			});
	}

	@DisplayName("toString contains all fields")
	@Test
	void testToString() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisAdminProperties props = ctx.getBean(PolarisAdminProperties.class);
			String result = props.toString();
			assertThat(result).contains("PolarisAdminProperties");
			assertThat(result).contains("host=");
			assertThat(result).contains("port=");
		});
	}

}
