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

import java.util.Collections;
import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ReportServiceContractResponse;
import com.tencent.polaris.api.plugin.server.ServiceFeature;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpServerContractReporter}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpServerContractReporter")
@ExtendWith(MockitoExtension.class)
class PolarisMcpServerContractReporterTest {

	private static final String TEST_NAMESPACE = "default";

	private static final String TEST_SERVICE = "mcp-server";

	private static final String TEST_PROTOCOL = "mcp-sse";

	private static final String TEST_VERSION = "1.0.0";

	@Mock
	private PolarisSDKContextManager sdkContextManager;

	@Mock
	private ProviderAPI providerAPI;

	@Captor
	private ArgumentCaptor<ReportServiceContractRequest> requestCaptor;

	private PolarisMcpServerContractReporter reporter;

	@BeforeEach
	void setUp() {
		this.reporter = new PolarisMcpServerContractReporter(
				this.sdkContextManager, TEST_NAMESPACE, TEST_SERVICE, TEST_VERSION);
	}

	@DisplayName("constructor throws on null sdkContextManager")
	@Test
	void testConstructorThrowsOnNullSdkContextManager() {
		// Arrange & Act & Assert
		assertThatThrownBy(() -> new PolarisMcpServerContractReporter(
				null, TEST_NAMESPACE, TEST_SERVICE, TEST_VERSION))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("sdkContextManager");
	}

	@DisplayName("constructor throws on null namespace")
	@Test
	void testConstructorThrowsOnNullNamespace() {
		// Arrange & Act & Assert
		assertThatThrownBy(() -> new PolarisMcpServerContractReporter(
				this.sdkContextManager, null, TEST_SERVICE, TEST_VERSION))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("namespace");
	}

	@DisplayName("constructor throws on null serviceName")
	@Test
	void testConstructorThrowsOnNullServiceName() {
		// Arrange & Act & Assert
		assertThatThrownBy(() -> new PolarisMcpServerContractReporter(
				this.sdkContextManager, TEST_NAMESPACE, null, TEST_VERSION))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("serviceName");
	}

	@DisplayName("constructor allows null version")
	@Test
	void testConstructorAllowsNullVersion() {
		// Arrange & Act & Assert
		assertThatCode(() -> new PolarisMcpServerContractReporter(
				this.sdkContextManager, TEST_NAMESPACE, TEST_SERVICE, null))
			.doesNotThrowAnyException();
	}

	@DisplayName("reportContract throws on null protocol")
	@Test
	void testReportContractThrowsOnNullProtocol() {
		// Arrange & Act & Assert
		assertThatThrownBy(() -> this.reporter.reportContract(
				null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("protocol");
	}

	@DisplayName("reportContract reports tools as MCP_Tool features")
	@Test
	void testReportContractWithTools() {
		// Arrange
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		ReportServiceContractResponse response = mock(ReportServiceContractResponse.class);
		when(this.providerAPI.reportServiceContract(any())).thenReturn(response);

		McpSchema.Tool tool = McpSchema.Tool.builder()
			.name("get_weather")
			.description("Get weather info")
			.build();

		// Act
		this.reporter.reportContract(TEST_PROTOCOL, List.of(tool), Collections.emptyList(), Collections.emptyList());

		// Assert
		verify(this.providerAPI).reportServiceContract(this.requestCaptor.capture());
		ReportServiceContractRequest request = this.requestCaptor.getValue();
		assertThat(request.getNamespace()).isEqualTo(TEST_NAMESPACE);
		assertThat(request.getService()).isEqualTo(TEST_SERVICE);
		assertThat(request.getProtocol()).isEqualTo(TEST_PROTOCOL);
		assertThat(request.getVersion()).isEqualTo(TEST_VERSION);

		List<ServiceFeature> features = request.getServiceFeatures();
		assertThat(features).hasSize(1);
		assertThat(features.get(0).getName()).isEqualTo("get_weather");
		assertThat(features.get(0).getDescription()).isEqualTo("Get weather info");
		assertThat(features.get(0).getType())
			.isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
		assertThat(features.get(0).getStatus())
			.isEqualTo(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
	}

	@DisplayName("reportContract reports resources as MCP_Resource features")
	@Test
	void testReportContractWithResources() {
		// Arrange
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		ReportServiceContractResponse response = mock(ReportServiceContractResponse.class);
		when(this.providerAPI.reportServiceContract(any())).thenReturn(response);

		McpSchema.Resource resource = McpSchema.Resource.builder()
			.uri("file:///data.csv")
			.name("data")
			.description("Data file")
			.mimeType("text/csv")
			.build();

		// Act
		this.reporter.reportContract(TEST_PROTOCOL, Collections.emptyList(), List.of(resource), Collections.emptyList());

		// Assert
		verify(this.providerAPI).reportServiceContract(this.requestCaptor.capture());
		List<ServiceFeature> features = this.requestCaptor.getValue().getServiceFeatures();
		assertThat(features).hasSize(1);
		assertThat(features.get(0).getName()).isEqualTo("data");
		assertThat(features.get(0).getType())
			.isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Resource);
		assertThat(features.get(0).getContent()).contains("file:///data.csv");
		assertThat(features.get(0).getContent()).contains("text/csv");
	}

	@DisplayName("reportContract reports prompts as MCP_Prompt features")
	@Test
	void testReportContractWithPrompts() {
		// Arrange
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		ReportServiceContractResponse response = mock(ReportServiceContractResponse.class);
		when(this.providerAPI.reportServiceContract(any())).thenReturn(response);

		McpSchema.Prompt prompt = new McpSchema.Prompt("greeting", "A greeting prompt",
				List.of(new McpSchema.PromptArgument("name", "User name", true)));

		// Act
		this.reporter.reportContract(TEST_PROTOCOL, Collections.emptyList(), Collections.emptyList(), List.of(prompt));

		// Assert
		verify(this.providerAPI).reportServiceContract(this.requestCaptor.capture());
		List<ServiceFeature> features = this.requestCaptor.getValue().getServiceFeatures();
		assertThat(features).hasSize(1);
		assertThat(features.get(0).getName()).isEqualTo("greeting");
		assertThat(features.get(0).getType())
			.isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Prompt);
		assertThat(features.get(0).getContent()).contains("name");
	}

	@DisplayName("reportContract with all empty lists reports empty features")
	@Test
	void testReportContractWithEmptyLists() {
		// Arrange
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		ReportServiceContractResponse response = mock(ReportServiceContractResponse.class);
		when(this.providerAPI.reportServiceContract(any())).thenReturn(response);

		// Act
		this.reporter.reportContract(TEST_PROTOCOL, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

		// Assert
		verify(this.providerAPI).reportServiceContract(this.requestCaptor.capture());
		assertThat(this.requestCaptor.getValue().getServiceFeatures()).isEmpty();
	}

	@DisplayName("reportContract catches exception from ProviderAPI")
	@Test
	void testReportContractCatchesProviderException() {
		// Arrange
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		when(this.providerAPI.reportServiceContract(any()))
			.thenThrow(new RuntimeException("connection refused"));

		// Act & Assert
		assertThatCode(() -> this.reporter.reportContract(
				TEST_PROTOCOL, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()))
			.doesNotThrowAnyException();
	}

	@DisplayName("reportContract with mixed tools, resources and prompts")
	@Test
	void testReportContractWithMixedFeatures() {
		// Arrange
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		ReportServiceContractResponse response = mock(ReportServiceContractResponse.class);
		when(this.providerAPI.reportServiceContract(any())).thenReturn(response);

		McpSchema.Tool tool = McpSchema.Tool.builder().name("tool1").description("Tool 1").build();
		McpSchema.Resource resource = McpSchema.Resource.builder()
			.uri("file:///r1").name("res1").description("Resource 1").build();
		McpSchema.Prompt prompt = new McpSchema.Prompt("prompt1", "Prompt 1", Collections.emptyList());

		// Act
		this.reporter.reportContract(TEST_PROTOCOL, List.of(tool), List.of(resource), List.of(prompt));

		// Assert
		verify(this.providerAPI).reportServiceContract(this.requestCaptor.capture());
		List<ServiceFeature> features = this.requestCaptor.getValue().getServiceFeatures();
		assertThat(features).hasSize(3);
		assertThat(features.get(0).getType())
			.isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
		assertThat(features.get(1).getType())
			.isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Resource);
		assertThat(features.get(2).getType())
			.isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Prompt);
	}

}
