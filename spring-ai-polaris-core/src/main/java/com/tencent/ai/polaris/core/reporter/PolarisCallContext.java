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

import com.tencent.polaris.api.pojo.RetStatus;

/**
 * Captures the context of a single remote service call for reporting to Polaris.
 *
 * @author Haotian Zhang
 */
public class PolarisCallContext {

	private final String namespace;

	private final String serviceName;

	private final String host;

	private final int port;

	private final String method;

	private final long delayMs;

	private final int retCode;

	private final RetStatus retStatus;

	public PolarisCallContext(String namespace, String serviceName, String host, int port, String method,
			long delayMs, int retCode, RetStatus retStatus) {
		this.namespace = namespace;
		this.serviceName = serviceName;
		this.host = host;
		this.port = port;
		this.method = method;
		this.delayMs = delayMs;
		this.retCode = retCode;
		this.retStatus = retStatus;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getMethod() {
		return this.method;
	}

	public long getDelayMs() {
		return this.delayMs;
	}

	public int getRetCode() {
		return this.retCode;
	}

	public RetStatus getRetStatus() {
		return this.retStatus;
	}

}
