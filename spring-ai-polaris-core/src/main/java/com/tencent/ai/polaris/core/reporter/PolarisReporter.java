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

package com.tencent.ai.polaris.core.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.polaris.api.rpc.ServiceCallResult;

/**
 * Reports service call results to Polaris for circuit-breaking and load-balancing
 * statistics. The SDK context is lazily initialized on first use, not during bean
 * creation.
 *
 * @author Haotian Zhang
 */
public class PolarisReporter {

	private static final Logger logger = LoggerFactory.getLogger(PolarisReporter.class);

	private final PolarisSDKContextManager sdkContextManager;

	public PolarisReporter(PolarisSDKContextManager sdkContextManager) {
		this.sdkContextManager = sdkContextManager;
	}

	/**
	 * Report a completed service call.
	 * @param ctx call context containing service coordinates and outcome
	 */
	public void report(PolarisCallContext ctx) {
		ServiceCallResult result = new ServiceCallResult();
		result.setNamespace(ctx.getNamespace());
		result.setService(ctx.getServiceName());
		result.setHost(ctx.getHost());
		result.setPort(ctx.getPort());
		result.setMethod(ctx.getMethod());
		result.setDelay(ctx.getDelayMs());
		result.setRetCode(ctx.getRetCode());
		result.setRetStatus(ctx.getRetStatus());
		result.setInstanceType(ctx.getInstanceType());
		if (logger.isDebugEnabled()) {
			logger.debug("Reporting service call result to Polaris. {}", ctx);
		}
		try {
			this.sdkContextManager.getConsumerAPI().updateServiceCallResult(result);
		}
		catch (Throwable e) {
			logger.warn("Failed to report service call result to Polaris. service={}, host={}:{}",
					ctx.getServiceName(), ctx.getHost(), ctx.getPort(), e);
		}
	}

}
