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

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import com.tencent.ai.polaris.autoconfigure.core.PolarisCoreProperties;
import com.tencent.ai.polaris.core.utils.PolarisInetUtils;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.ai.polaris.mcp.server.McpServerCapabilitiesProvider;
import com.tencent.ai.polaris.mcp.server.PolarisMcpServerContractReporter;
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

	private final PolarisMcpServerContractReporter contractReporter;

	private final McpServerCapabilitiesProvider capabilitiesProvider;

	public PolarisMcpServerListener(PolarisMcpServerRegistry registry, PolarisMcpServerProperties properties,
			PolarisCoreProperties coreProperties, PolarisMcpServerContractReporter contractReporter,
			McpServerCapabilitiesProvider capabilitiesProvider) {
		this.registry = registry;
		this.properties = properties;
		this.coreProperties = coreProperties;
		this.contractReporter = contractReporter;
		this.capabilitiesProvider = capabilitiesProvider;
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

			// Service contract reporting
			reportContractIfEnabled();
		}
		catch (Exception ex) {
			logger.error("Failed to register MCP server with Polaris.", ex);
		}
	}

	private void reportContractIfEnabled() {
		if (this.contractReporter == null || this.capabilitiesProvider == null) {
			logger.debug("Contract reporter or capabilities provider not available,"
					+ " skipping contract reporting.");
			return;
		}

		try {
			List<McpSchema.Tool> tools = this.capabilitiesProvider.listTools();
			List<McpSchema.Resource> resources = this.capabilitiesProvider.listResources();
			List<McpSchema.Prompt> prompts = this.capabilitiesProvider.listPrompts();
			List<String> requestHandlerMethods = this.capabilitiesProvider.listRequestHandlerMethods();
			List<String> notificationHandlerMethods = this.capabilitiesProvider.listNotificationHandlerMethods();

			this.contractReporter.reportContract(this.registry.getProtocol(),
					this.registry.getEndpointPath(), tools, resources, prompts,
					requestHandlerMethods, notificationHandlerMethods);
		}
		catch (Exception ex) {
			logger.error("Failed to report MCP server contract to Polaris.", ex);
		}
	}

	@Override
	public void destroy() {
		this.registry.deregister();
	}

}
