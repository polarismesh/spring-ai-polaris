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

package com.tencent.ai.polaris.autoconfigure.core.report;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.ai.polaris.autoconfigure.core.PolarisSDKContextAutoConfiguration;
import com.tencent.ai.polaris.core.PolarisConfigModifier;
import com.tencent.ai.polaris.core.PolarisConfigModifierOrder;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;

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

	@DisplayName("reporterConfigModifier disables stat reporter when enabled=false")
	@Test
	void testReporterConfigModifierDisabled() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisConfigModifier modifier = ctx.getBean(PolarisConfigModifier.class);
			ConfigurationImpl config = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();
			modifier.modify(config);
			StatReporterConfigImpl statConfig =
					(StatReporterConfigImpl) config.getGlobal().getStatReporter();
			assertThat(statConfig.isEnable()).isFalse();
		});
	}

	@DisplayName("reporterConfigModifier configures pull mode with prometheus path")
	@Test
	void testReporterConfigModifierPullMode() {
		// Arrange & Act & Assert
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisReporterAutoConfiguration.class, PolarisSDKContextAutoConfiguration.class))
			.withPropertyValues(
					"spring.ai.polaris.reporter.enabled=true",
					"spring.ai.polaris.reporter.type=pull",
					"spring.ai.polaris.reporter.prometheus.path=/custom-metrics")
			.run(ctx -> {
				PolarisConfigModifier modifier = ctx.getBean("reporterConfigModifier",
						PolarisConfigModifier.class);
				ConfigurationImpl config = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();
				modifier.modify(config);
				StatReporterConfigImpl statConfig =
						(StatReporterConfigImpl) config.getGlobal().getStatReporter();
				assertThat(statConfig.isEnable()).isTrue();
				PrometheusHandlerConfig promConfig = statConfig.getPluginConfig(
						StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
				assertThat(promConfig.getType()).isEqualTo("pull");
				assertThat(promConfig.getPath()).isEqualTo("/custom-metrics");
			});
	}

	@DisplayName("reporterConfigModifier configures push mode with pushgateway settings")
	@Test
	void testReporterConfigModifierPushMode() {
		// Arrange & Act & Assert
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisReporterAutoConfiguration.class, PolarisSDKContextAutoConfiguration.class))
			.withPropertyValues(
					"spring.ai.polaris.reporter.enabled=true",
					"spring.ai.polaris.reporter.type=push",
					"spring.ai.polaris.reporter.push-gateway.address=http://127.0.0.1:9091",
					"spring.ai.polaris.reporter.push-gateway.namespace=TestNS",
					"spring.ai.polaris.reporter.push-gateway.service=test.pushgateway",
					"spring.ai.polaris.reporter.push-gateway.push-interval=5000",
					"spring.ai.polaris.reporter.push-gateway.open-gzip=true")
			.run(ctx -> {
				PolarisConfigModifier modifier = ctx.getBean("reporterConfigModifier",
						PolarisConfigModifier.class);
				ConfigurationImpl config = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();
				modifier.modify(config);
				StatReporterConfigImpl statConfig =
						(StatReporterConfigImpl) config.getGlobal().getStatReporter();
				assertThat(statConfig.isEnable()).isTrue();
				PrometheusHandlerConfig promConfig = statConfig.getPluginConfig(
						StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
				assertThat(promConfig.getType()).isEqualTo("push");
				assertThat(promConfig.getAddress()).isEqualTo(List.of("http://127.0.0.1:9091"));
				assertThat(promConfig.getNamespace()).isEqualTo("TestNS");
				assertThat(promConfig.getService()).isEqualTo("test.pushgateway");
				assertThat(promConfig.getPushInterval()).isEqualTo(5000L);
				assertThat(promConfig.isOpenGzip()).isTrue();
			});
	}

	@DisplayName("reporterConfigModifier getOrder returns REPORTER_ORDER")
	@Test
	void testReporterConfigModifierOrder() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisConfigModifier modifier = ctx.getBean(PolarisConfigModifier.class);
			assertThat(modifier.getOrder()).isEqualTo(PolarisConfigModifierOrder.REPORTER_ORDER);
		});
	}

}
