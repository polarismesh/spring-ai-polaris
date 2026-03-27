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

package com.tencent.ai.polaris.core;

import com.tencent.polaris.factory.config.ConfigurationImpl;

/**
 * Modifier interface for customizing Polaris SDK configuration. Implement and register as
 * a Spring bean to contribute custom config.
 *
 * @author Haotian Zhang
 */
public interface PolarisConfigModifier {

	/**
	 * Modify the configuration before SDK context initialization.
	 * @param configuration the mutable configuration
	 */
	void modify(ConfigurationImpl configuration);

	/**
	 * Order value for this modifier. Lower values execute first.
	 * @return the order value
	 */
	default int getOrder() {
		return 0;
	}

}
