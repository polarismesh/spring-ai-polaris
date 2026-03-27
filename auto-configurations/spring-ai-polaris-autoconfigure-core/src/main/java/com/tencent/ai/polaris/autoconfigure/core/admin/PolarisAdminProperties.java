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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Polaris SDK admin HTTP server.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.ai.polaris.admin")
public class PolarisAdminProperties {

	/**
	 * Admin HTTP server listening host.
	 */
	private String host = "0.0.0.0";

	/**
	 * Admin HTTP server listening port. Set to 0 to let the OS auto-assign an available
	 * port.
	 */
	private int port = 28080;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "PolarisAdminProperties{"
				+ "host='" + this.host + '\''
				+ ", port=" + this.port
				+ '}';
	}

}
