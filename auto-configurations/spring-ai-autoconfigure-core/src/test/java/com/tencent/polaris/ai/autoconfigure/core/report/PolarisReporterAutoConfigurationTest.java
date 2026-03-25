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

package com.tencent.polaris.ai.autoconfigure.core.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.polaris.ai.autoconfigure.core.PolarisSDKContextAutoConfiguration;
import com.tencent.polaris.ai.core.PolarisConfigModifier;
import com.tencent.polaris.ai.core.reporter.PolarisReporter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisReporterAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisReporterAutoConfiguration")
class PolarisReporterAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisReporterAutoConfiguration.class));

	@DisplayName("PolarisReporter bean is not registered when reporter is disabled")
	@Test
	void testReporterBeanNotRegisteredWhenDisabled() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			assertThat(ctx).doesNotHaveBean(PolarisReporter.class);
			assertThat(ctx).hasSingleBean(PolarisConfigModifier.class);
		});
	}

	@DisplayName("context fails when reporter is enabled without PolarisSDKContextManager")
	@Test
	void testReporterEnabledFailsWithoutSDKContext() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.reporter.enabled=true")
			.run(ctx -> assertThat(ctx).hasFailed());
	}

	@DisplayName("PolarisReporter bean is created when enabled with full context")
	@Test
	void testReporterEnabledWithFullContext() {
		// Arrange & Act & Assert
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisReporterAutoConfiguration.class, PolarisSDKContextAutoConfiguration.class))
			.withPropertyValues("spring.ai.polaris.reporter.enabled=true")
			.run(ctx -> assertThat(ctx).hasSingleBean(PolarisReporter.class));
	}

}
