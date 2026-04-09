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

package com.tencent.ai.polaris.mcp.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.polaris.api.plugin.server.InterfaceDescriptor;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ServiceFeature;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;

/**
 * Reports MCP Server service contract (service features) to Polaris.
 * <p>
 * Builds a {@link ReportServiceContractRequest} from the MCP Server's registered tools,
 * resources, and prompts, then reports it via
 * {@code ProviderAPI.reportServiceContract()}.
 * <p>
 * This is a plain POJO with no Spring dependency.
 *
 * @author Haotian Zhang
 */
public final class PolarisMcpServerContractReporter {

	private static final Logger logger = LoggerFactory.getLogger(PolarisMcpServerContractReporter.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final PolarisSDKContextManager sdkContextManager;

	private final String namespace;

	private final String serviceName;

	private final String version;

	/**
	 * Create a new {@link PolarisMcpServerContractReporter}.
	 * @param sdkContextManager the Polaris SDK context manager
	 * @param namespace the Polaris namespace
	 * @param serviceName the Polaris service name
	 * @param version the MCP server version (may be {@code null})
	 */
	public PolarisMcpServerContractReporter(PolarisSDKContextManager sdkContextManager, String namespace,
			String serviceName, String version) {
		this.sdkContextManager = Objects.requireNonNull(sdkContextManager, "sdkContextManager must not be null");
		this.namespace = Objects.requireNonNull(namespace, "namespace must not be null");
		this.serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null");
		this.version = version;
	}

	/**
	 * Report the service contract with service features and interface descriptors.
	 * @param protocol the contract protocol (e.g. "mcp-sse", "mcp-streamable-http")
	 * @param endpointPath the MCP server endpoint path (may be {@code null} for stdio)
	 * @param tools the registered tools (may be empty or null)
	 * @param resources the registered resources (may be empty or null)
	 * @param prompts the registered prompts (may be empty or null)
	 * @param requestHandlerMethods the request handler method names (may be empty or
	 * null)
	 * @param notificationHandlerMethods the notification handler method names (may be
	 * empty or null)
	 */
	public void reportContract(String protocol, String endpointPath, List<McpSchema.Tool> tools,
			List<McpSchema.Resource> resources, List<McpSchema.Prompt> prompts,
			List<String> requestHandlerMethods, List<String> notificationHandlerMethods) {
		Objects.requireNonNull(protocol, "protocol must not be null");
		List<McpSchema.Tool> safeTools = (tools != null) ? tools : Collections.emptyList();
		List<McpSchema.Resource> safeResources = (resources != null) ? resources : Collections.emptyList();
		List<McpSchema.Prompt> safePrompts = (prompts != null) ? prompts : Collections.emptyList();
		List<String> safeRequestHandlers = (requestHandlerMethods != null)
				? requestHandlerMethods : Collections.emptyList();
		List<String> safeNotificationHandlers = (notificationHandlerMethods != null)
				? notificationHandlerMethods : Collections.emptyList();

		try {
			ReportServiceContractRequest request = new ReportServiceContractRequest();
			request.setNamespace(this.namespace);
			request.setService(this.serviceName);
			request.setName(this.serviceName);
			request.setProtocol(protocol);
			request.setVersion(this.version);

			request.setServiceFeatures(buildServiceFeatures(safeTools, safeResources, safePrompts));
			request.setInterfaceDescriptors(
					buildInterfaceDescriptors(endpointPath, safeRequestHandlers, safeNotificationHandlers));

			if (logger.isDebugEnabled()) {
				logger.debug("Reporting MCP server contract: tools={}, resources={}, prompts={},"
						+ " requestHandlers={}, notificationHandlers={}",
						safeTools, safeResources, safePrompts,
						safeRequestHandlers, safeNotificationHandlers);
			}

			this.sdkContextManager.getProviderAPI().reportServiceContract(request);

			logger.info(
					"MCP server contract reported to Polaris: namespace={}, service={},"
							+ " tools={}, resources={}, prompts={},"
							+ " requestHandlers={}, notificationHandlers={}",
					this.namespace, this.serviceName, safeTools.size(), safeResources.size(), safePrompts.size(),
					safeRequestHandlers.size(), safeNotificationHandlers.size());
		}
		catch (Exception ex) {
			logger.error("Failed to report MCP server contract to Polaris.", ex);
		}
	}

	private List<ServiceFeature> buildServiceFeatures(List<McpSchema.Tool> tools,
			List<McpSchema.Resource> resources, List<McpSchema.Prompt> prompts) {
		int totalSize = tools.size() + resources.size() + prompts.size();
		List<ServiceFeature> features = new ArrayList<>(totalSize);

		for (McpSchema.Tool tool : tools) {
			features.add(buildFeature(
					ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool,
					tool.name(), tool.description(), serializeToolContent(tool)));
		}

		for (McpSchema.Resource resource : resources) {
			features.add(buildFeature(
					ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Resource,
					resource.name(), resource.description(), serializeResourceContent(resource)));
		}

		for (McpSchema.Prompt prompt : prompts) {
			features.add(buildFeature(
					ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Prompt,
					prompt.name(), prompt.description(), serializePromptContent(prompt)));
		}

		return features;
	}

	private ServiceFeature buildFeature(ServiceContractProto.ServiceFeatureType type,
			String name, String description, String content) {
		ServiceFeature feature = new ServiceFeature();
		feature.setType(type);
		feature.setName(name);
		feature.setDescription(description);
		feature.setContent(content);
		feature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
		return feature;
	}

	private String serializeToolContent(McpSchema.Tool tool) {
		try {
			return OBJECT_MAPPER.writeValueAsString(tool);
		}
		catch (JsonProcessingException ex) {
			logger.warn("Failed to serialize tool for tool={}", tool.name(), ex);
			return "{}";
		}
	}

	private String serializeResourceContent(McpSchema.Resource resource) {
		try {
			return OBJECT_MAPPER.writeValueAsString(resource);
		}
		catch (JsonProcessingException ex) {
			logger.warn("Failed to serialize resource for resource={}", resource.name(), ex);
			return "{}";
		}
	}

	private String serializePromptContent(McpSchema.Prompt prompt) {
		try {
			return OBJECT_MAPPER.writeValueAsString(prompt);
		}
		catch (JsonProcessingException ex) {
			logger.warn("Failed to serialize prompt for prompt={}", prompt.name(), ex);
			return "{}";
		}
	}

	private List<InterfaceDescriptor> buildInterfaceDescriptors(String endpointPath,
			List<String> requestHandlerMethods, List<String> notificationHandlerMethods) {
		int totalSize = requestHandlerMethods.size() + notificationHandlerMethods.size();
		List<InterfaceDescriptor> descriptors = new ArrayList<>(totalSize);

		Stream.concat(requestHandlerMethods.stream(), notificationHandlerMethods.stream())
			.forEach(method -> descriptors.add(buildDescriptor(method, endpointPath)));

		return descriptors;
	}

	private InterfaceDescriptor buildDescriptor(String method, String endpointPath) {
		InterfaceDescriptor descriptor = new InterfaceDescriptor();
		descriptor.setMethod(method);
		descriptor.setPath(endpointPath);
		return descriptor;
	}

}
