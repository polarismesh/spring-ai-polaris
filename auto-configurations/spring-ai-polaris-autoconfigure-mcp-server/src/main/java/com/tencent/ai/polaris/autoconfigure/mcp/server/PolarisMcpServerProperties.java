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

package com.tencent.ai.polaris.autoconfigure.mcp.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MCP Server registration with Polaris.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.ai.polaris.mcp.server")
public class PolarisMcpServerProperties {

	/**
	 * Whether Polaris MCP server registration is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Polaris service name for the MCP server. If not set, falls back to
	 * {@code spring.application.name}.
	 */
	private String serviceName;

	/**
	 * Whether to enforce strict compatibility with existing instances.
	 */
	private boolean strictCompatible = false;

	/**
	 * Custom host for Polaris registration. If not set, auto-detected from local address.
	 */
	private String host;

	/**
	 * Custom port for Polaris registration. If not set ({@code -1}), auto-detected from
	 * web server.
	 */
	private int port = -1;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public boolean isStrictCompatible() {
		return this.strictCompatible;
	}

	public void setStrictCompatible(boolean strictCompatible) {
		this.strictCompatible = strictCompatible;
	}

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
		return "PolarisMcpServerProperties{"
				+ "enabled=" + this.enabled
				+ ", serviceName='" + this.serviceName + '\''
				+ ", strictCompatible=" + this.strictCompatible
				+ ", host='" + this.host + '\''
				+ ", port=" + this.port
				+ '}';
	}

}
