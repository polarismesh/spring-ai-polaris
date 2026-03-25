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

package com.tencent.polaris.ai.autoconfigure.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.polaris.ai.core.PolarisConfigModifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCoreAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisCoreAutoConfiguration")
class PolarisCoreAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisCoreAutoConfiguration.class));

	@DisplayName("addressConfigModifier bean is registered")
	@Test
	void testAddressConfigModifierRegistered() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(PolarisConfigModifier.class));
	}

	@DisplayName("default address is grpc://127.0.0.1:8091")
	@Test
	void testDefaultAddressIsUsed() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisCoreProperties props = ctx.getBean(PolarisCoreProperties.class);
			assertThat(props.getAddress()).containsExactly("grpc://127.0.0.1:8091");
		});
	}

	@DisplayName("all beans are disabled when spring.ai.polaris.enabled=false")
	@Test
	void testDisabledWhenPolarisNotEnabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.enabled=false")
			.run(ctx -> assertThat(ctx).doesNotHaveBean(PolarisConfigModifier.class));
	}

}
