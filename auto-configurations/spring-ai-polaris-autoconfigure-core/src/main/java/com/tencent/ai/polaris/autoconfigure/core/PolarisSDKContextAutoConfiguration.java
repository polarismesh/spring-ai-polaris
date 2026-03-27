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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.tencent.ai.polaris.autoconfigure.core.admin.PolarisAdminAutoConfiguration;
import com.tencent.ai.polaris.autoconfigure.core.report.PolarisReporterAutoConfiguration;
import com.tencent.ai.polaris.core.PolarisConfigModifier;
import com.tencent.ai.polaris.core.PolarisSDKContextManager;

/**
 * Auto-configuration that creates the Polaris SDK context manager after all
 * modifier-producing configurations have been loaded.
 *
 * @author Haotian Zhang
 */
@AutoConfiguration(after = { PolarisCoreAutoConfiguration.class, PolarisAdminAutoConfiguration.class,
		PolarisReporterAutoConfiguration.class })
@ConditionalOnPolarisEnabled
public class PolarisSDKContextAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisSDKContextManager polarisSDKContextManager(
			ObjectProvider<PolarisConfigModifier> modifiers) {
		return new PolarisSDKContextManager(modifiers.stream().toList());
	}

}
