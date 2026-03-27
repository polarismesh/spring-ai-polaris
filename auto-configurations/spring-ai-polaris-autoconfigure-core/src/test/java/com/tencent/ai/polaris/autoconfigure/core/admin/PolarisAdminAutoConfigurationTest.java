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

package com.tencent.ai.polaris.autoconfigure.core.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.tencent.ai.polaris.core.PolarisConfigModifier;
import com.tencent.ai.polaris.core.PolarisConfigModifierOrder;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisAdminAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisAdminAutoConfiguration")
class PolarisAdminAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PolarisAdminAutoConfiguration.class));

	@DisplayName("admin properties bean is registered with default values")
	@Test
	void testAdminPropertiesRegistered() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			assertThat(ctx).hasSingleBean(PolarisAdminProperties.class);
			PolarisAdminProperties props = ctx.getBean(PolarisAdminProperties.class);
			assertThat(props.getHost()).isEqualTo("0.0.0.0");
			assertThat(props.getPort()).isEqualTo(28080);
		});
	}

	@DisplayName("admin properties can be customized")
	@Test
	void testAdminPropertiesCustomized() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.admin.host=127.0.0.1", "spring.ai.polaris.admin.port=9090")
			.run(ctx -> {
				PolarisAdminProperties props = ctx.getBean(PolarisAdminProperties.class);
				assertThat(props.getHost()).isEqualTo("127.0.0.1");
				assertThat(props.getPort()).isEqualTo(9090);
			});
	}

	@DisplayName("adminConfigModifier bean is registered")
	@Test
	void testAdminConfigModifierRegistered() {
		// Arrange & Act & Assert
		this.contextRunner
			.run(ctx -> assertThat(ctx).hasSingleBean(PolarisConfigModifier.class));
	}

	@DisplayName("all beans are disabled when spring.ai.polaris.enabled=false")
	@Test
	void testDisabledWhenPolarisNotEnabled() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.enabled=false")
			.run(ctx -> assertThat(ctx).doesNotHaveBean(PolarisConfigModifier.class));
	}

	@DisplayName("adminConfigModifier sets host and port on configuration")
	@Test
	void testAdminConfigModifierSetsHostAndPort() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.admin.host=127.0.0.1", "spring.ai.polaris.admin.port=9090")
			.run(ctx -> {
				PolarisConfigModifier modifier = ctx.getBean(PolarisConfigModifier.class);
				ConfigurationImpl config = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();
				modifier.modify(config);
				assertThat(config.getGlobal().getAdmin().getHost()).isEqualTo("127.0.0.1");
				assertThat(config.getGlobal().getAdmin().getPort()).isEqualTo(9090);
			});
	}

	@DisplayName("adminConfigModifier getOrder returns ADMIN_ORDER")
	@Test
	void testAdminConfigModifierOrder() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisConfigModifier modifier = ctx.getBean(PolarisConfigModifier.class);
			assertThat(modifier.getOrder()).isEqualTo(PolarisConfigModifierOrder.ADMIN_ORDER);
		});
	}

}
