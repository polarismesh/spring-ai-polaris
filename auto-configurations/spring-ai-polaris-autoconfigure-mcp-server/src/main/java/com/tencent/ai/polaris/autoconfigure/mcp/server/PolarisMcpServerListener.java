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

package com.tencent.ai.polaris.autoconfigure.mcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.core.utils.PolarisInetUtils;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerRegistry;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * Listens for {@link WebServerInitializedEvent} to register the MCP server with Polaris
 * on startup and deregisters on shutdown.
 *
 * @author Haotian Zhang
 */
public class PolarisMcpServerListener implements ApplicationListener<WebServerInitializedEvent>, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(PolarisMcpServerListener.class);

	private final PolarisMcpServerRegistry registry;

	private final PolarisMcpServerProperties properties;

	private final PolarisCoreProperties coreProperties;

	public PolarisMcpServerListener(PolarisMcpServerRegistry registry, PolarisMcpServerProperties properties,
			PolarisCoreProperties coreProperties) {
		this.registry = registry;
		this.properties = properties;
		this.coreProperties = coreProperties;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		// Skip management server port
		if ("management".equals(event.getApplicationContext().getServerNamespace())) {
			logger.debug("Skipping Polaris registration for management server.");
			return;
		}

		// Stdio transport does not expose a network endpoint, skip registration
		if (PolarisMcpMetadataKeys.PROTOCOL_MCP_STDIO.equals(this.registry.getProtocol())) {
			logger.info("Skipping Polaris registration for stdio transport.");
			return;
		}

		try {
			String host = this.properties.getHost();
			if (StringUtils.isBlank(host)) {
				host = PolarisInetUtils.getIpString(this.coreProperties.isPreferIpv6());
			}
			if (StringUtils.isBlank(host)) {
				host = PolarisInetUtils.findFirstNonLoopbackAddress();
			}
			if (StringUtils.isBlank(host)) {
				throw new IllegalStateException(
						"Unable to resolve host address for MCP server registration."
								+ " Set 'spring.ai.polaris.mcp.server.host' explicitly.");
			}

			int port = this.properties.getPort();
			if (port <= 0) {
				port = event.getWebServer().getPort();
			}

			this.registry.register(host, port);
		}
		catch (Exception ex) {
			logger.error("Failed to register MCP server with Polaris.", ex);
		}
	}

	@Override
	public void destroy() {
		this.registry.deregister();
	}

}
