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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisReporterProperties}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisReporterProperties")
class PolarisReporterPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisReporterAutoConfiguration.class));

	@DisplayName("default values are bound correctly")
	@Test
	void testDefaultValues() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisReporterProperties props = ctx.getBean(PolarisReporterProperties.class);
			assertThat(props.isEnabled()).isFalse();
			assertThat(props.getType()).isEqualTo("pull");
			assertThat(props.getPrometheus()).isNotNull();
			assertThat(props.getPrometheus().getPath()).isEqualTo("/metrics");
			assertThat(props.getPushGateway()).isNotNull();
			assertThat(props.getPushGateway().getAddress()).isNull();
			assertThat(props.getPushGateway().getNamespace()).isEqualTo("Polaris");
			assertThat(props.getPushGateway().getService()).isEqualTo("polaris.pushgateway");
			assertThat(props.getPushGateway().getPushInterval()).isEqualTo(10000L);
			assertThat(props.getPushGateway().isOpenGzip()).isFalse();
		});
	}

	@DisplayName("custom property values are bound correctly")
	@Test
	void testCustomValues() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues(
					"spring.ai.polaris.reporter.type=push",
					"spring.ai.polaris.reporter.prometheus.path=/custom-metrics",
					"spring.ai.polaris.reporter.push-gateway.address=http://127.0.0.2:9091",
					"spring.ai.polaris.reporter.push-gateway.namespace=custom-ns",
					"spring.ai.polaris.reporter.push-gateway.service=custom-svc",
					"spring.ai.polaris.reporter.push-gateway.push-interval=5000",
					"spring.ai.polaris.reporter.push-gateway.open-gzip=true")
			.run(ctx -> {
				PolarisReporterProperties props = ctx.getBean(PolarisReporterProperties.class);
				assertThat(props.getType()).isEqualTo("push");
				assertThat(props.getPrometheus().getPath()).isEqualTo("/custom-metrics");
				assertThat(props.getPushGateway().getAddress()).containsExactly("http://127.0.0.2:9091");
				assertThat(props.getPushGateway().getNamespace()).isEqualTo("custom-ns");
				assertThat(props.getPushGateway().getService()).isEqualTo("custom-svc");
				assertThat(props.getPushGateway().getPushInterval()).isEqualTo(5000L);
				assertThat(props.getPushGateway().isOpenGzip()).isTrue();
			});
	}

	@DisplayName("toString contains all fields")
	@Test
	void testToString() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisReporterProperties props = ctx.getBean(PolarisReporterProperties.class);
			String result = props.toString();
			assertThat(result).contains("PolarisReporterProperties");
			assertThat(result).contains("enabled=");
			assertThat(result).contains("type=");
			assertThat(result).contains("prometheus=");
			assertThat(result).contains("pushGateway=");
		});
	}

	@DisplayName("Prometheus toString contains all fields")
	@Test
	void testPrometheusToString() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisReporterProperties props = ctx.getBean(PolarisReporterProperties.class);
			String result = props.getPrometheus().toString();
			assertThat(result).contains("Prometheus");
			assertThat(result).contains("path=");
		});
	}

	@DisplayName("PushGateway toString contains all fields")
	@Test
	void testPushGatewayToString() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisReporterProperties props = ctx.getBean(PolarisReporterProperties.class);
			String result = props.getPushGateway().toString();
			assertThat(result).contains("PushGateway");
			assertThat(result).contains("address=");
			assertThat(result).contains("namespace=");
			assertThat(result).contains("service=");
			assertThat(result).contains("pushInterval=");
			assertThat(result).contains("openGzip=");
		});
	}

}
