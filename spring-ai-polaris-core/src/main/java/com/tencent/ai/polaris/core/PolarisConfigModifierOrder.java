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

/**
 * Order constants for {@link PolarisConfigModifier} implementations. Lower values execute
 * first.
 *
 * @author Haotian Zhang
 */
public final class PolarisConfigModifierOrder {

	/**
	 * Order for the server address config modifier.
	 */
	public static final int ADDRESS_ORDER = 0;

	/**
	 * Order for the admin config modifier.
	 */
	public static final int ADMIN_ORDER = 1;

	/**
	 * Order for the stat reporter config modifier.
	 */
	public static final int REPORTER_ORDER = 2;

	private PolarisConfigModifierOrder() {
	}

}
