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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.tencent.ai.polaris.core.PolarisConfigModifier;
import com.tencent.ai.polaris.core.PolarisConfigModifierOrder;
import com.tencent.ai.polaris.core.utils.AddressUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;

/**
 * Auto-configuration for the Polaris SDK core properties and address modifier.
 *
 * @author Haotian Zhang
 */
@AutoConfiguration
@ConditionalOnPolarisEnabled
@EnableConfigurationProperties(PolarisCoreProperties.class)
public class PolarisCoreAutoConfiguration {

	@Bean
	public PolarisConfigModifier addressConfigModifier(PolarisCoreProperties properties) {
		return new PolarisConfigModifier() {
			@Override
			public void modify(ConfigurationImpl configuration) {
				List<String> addresses = new ArrayList<>();
				for (String address : properties.getAddress()) {
					addresses.addAll(AddressUtils.parseAddressList(address));
				}
				configuration.getGlobal().getServerConnector().setAddresses(addresses);
			}

			@Override
			public int getOrder() {
				return PolarisConfigModifierOrder.ADDRESS_ORDER;
			}
		};
	}

}
