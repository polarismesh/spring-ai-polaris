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

import java.util.List;

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

	@DisplayName("default address is 127.0.0.1:8091")
	@Test
	void testDefaultAddressIsUsed() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisCoreProperties props = ctx.getBean(PolarisCoreProperties.class);
			assertThat(props.getAddress()).containsExactly("127.0.0.1:8091");
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

	@DisplayName("addressConfigModifier sets server connector addresses")
	@Test
	void testAddressConfigModifierSetsAddresses() {
		// Arrange & Act & Assert
		this.contextRunner
			.withPropertyValues("spring.ai.polaris.address=127.0.0.1:8091,127.0.0.2:8091")
			.run(ctx -> {
				PolarisConfigModifier modifier = ctx.getBean(PolarisConfigModifier.class);
				ConfigurationImpl config = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();
				modifier.modify(config);
				List<String> addresses = config.getGlobal().getServerConnector().getAddresses();
				assertThat(addresses).containsExactly("127.0.0.1:8091", "127.0.0.2:8091");
			});
	}

	@DisplayName("addressConfigModifier getOrder returns ADDRESS_ORDER")
	@Test
	void testAddressConfigModifierOrder() {
		// Arrange & Act & Assert
		this.contextRunner.run(ctx -> {
			PolarisConfigModifier modifier = ctx.getBean(PolarisConfigModifier.class);
			assertThat(modifier.getOrder()).isEqualTo(PolarisConfigModifierOrder.ADDRESS_ORDER);
		});
	}

}
