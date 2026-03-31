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

package com.tencent.ai.polaris.autoconfigure.mcp.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Polaris MCP client discovery.
 * <p>
 * Each entry in {@link #services} maps a Polaris service name to its per-service
 * {@link PolarisMcpParameters}. Per-service parameters (namespace, scheme) override the
 * global defaults when set.
 *
 * <pre>
 * spring.ai.polaris.mcp.client:
 *   namespace: default
 *   services:
 *     my-service-a:
 *       scheme: https
 *     my-service-b: {}
 * </pre>
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.ai.polaris.mcp.client")
public class PolarisMcpClientProperties {

	/**
	 * Whether Polaris MCP client discovery is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Default Polaris namespace for MCP server discovery. If not set, falls back to
	 * {@code spring.ai.polaris.namespace}. Individual services can override this via
	 * {@link PolarisMcpParameters#namespace()}.
	 */
	private String namespace;

	/**
	 * Map of Polaris service names to per-service parameters. The map key is the service
	 * name, and the value contains optional overrides for namespace and scheme.
	 */
	private Map<String, PolarisMcpParameters> services = new LinkedHashMap<>();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Map<String, PolarisMcpParameters> getServices() {
		return this.services;
	}

	public void setServices(Map<String, PolarisMcpParameters> services) {
		this.services = services;
	}

	@Override
	public String toString() {
		return "PolarisMcpClientProperties{"
				+ "enabled=" + this.enabled
				+ ", namespace='" + this.namespace + '\''
				+ ", services=" + this.services
				+ '}';
	}

	/**
	 * Per-service parameters for a Polaris MCP client connection. When a field is
	 * {@code null}, the global default from {@link PolarisMcpClientProperties} is used.
	 *
	 * @param namespace Polaris namespace for this service, overrides the global default
	 * @param scheme URL scheme for this service (e.g. {@code https}), overrides the
	 * global default
	 */
	public record PolarisMcpParameters(String namespace, String scheme) {
	}

}
