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

package com.tencent.ai.polaris.autoconfigure.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.ai.polaris.autoconfigure.core.admin.PolarisAdminAutoConfiguration;
import com.tencent.ai.polaris.autoconfigure.core.report.PolarisReporterAutoConfiguration;
import com.tencent.ai.polaris.core.PolarisConfigModifier;
import com.tencent.ai.polaris.core.PolarisSDKContextManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisSDKContextAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisSDKContextAutoConfiguration")
class PolarisSDKContextAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisCoreAutoConfiguration.class,
				PolarisAdminAutoConfiguration.class, PolarisReporterAutoConfiguration.class,
				PolarisSDKContextAutoConfiguration.class));

	@DisplayName("PolarisSDKContextManager bean is registered")
	@Test
	void testSDKContextManagerBeanRegistered() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(PolarisSDKContextManager.class));
	}

	@DisplayName("all modifiers from all configurations are collected")
	@Test
	void testAllModifiersCollected() {
		// Arrange & Act & Assert
		// address (core) + admin + reporter = 3 modifiers
		this.contextRunner
			.run(ctx -> assertThat(ctx).getBeans(PolarisConfigModifier.class).hasSize(3));
	}

	@DisplayName("all beans are disabled when spring.ai.polaris.enabled=false")
	@Test
	void testDisabledWhenPolarisNotEnabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.enabled=false")
			.run(ctx -> assertThat(ctx).doesNotHaveBean(PolarisSDKContextManager.class));
	}

}
