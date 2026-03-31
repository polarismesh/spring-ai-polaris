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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tencent.ai.polaris.core.PolarisSDKContextManager;
import com.tencent.ai.polaris.mcp.common.PolarisMcpMetadataKeys;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisMcpServerRegistry}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisMcpServerRegistry")
@ExtendWith(MockitoExtension.class)
class PolarisMcpServerRegistryTest {

	private static final String TEST_NAMESPACE = "default";

	private static final String TEST_SERVICE = "mcp-server";

	private static final String TEST_PROTOCOL = PolarisMcpMetadataKeys.PROTOCOL_MCP_SSE;

	private static final String TEST_ENDPOINT = "/sse";

	private static final String TEST_VERSION = "2025-03-26";

	private static final String TEST_HOST = "127.0.0.1";

	private static final int TEST_PORT = 8080;

	@Mock
	private PolarisSDKContextManager sdkContextManager;

	@Mock
	private ProviderAPI providerAPI;

	@Mock
	private ConsumerAPI consumerAPI;

	private PolarisMcpServerRegistry registry;

	@BeforeEach
	void setUp() {
		this.registry = PolarisMcpServerRegistry.builder()
			.sdkContextManager(this.sdkContextManager)
			.namespace(TEST_NAMESPACE)
			.serviceName(TEST_SERVICE)
			.protocol(TEST_PROTOCOL)
			.endpointPath(TEST_ENDPOINT)
			.protocolVersion(TEST_VERSION)
			.strictCompatible(false)
			.build();
	}

	@DisplayName("register succeeds with no existing instances")
	@Test
	void testRegisterWithNoExistingInstances() {
		// Arrange
		InstancesResponse instancesResponse = mock(InstancesResponse.class);
		when(instancesResponse.getInstances()).thenReturn(new Instance[0]);
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		when(this.consumerAPI.getHealthyInstances(any(GetHealthyInstancesRequest.class))).thenReturn(instancesResponse);

		InstanceRegisterResponse registerResponse = mock(InstanceRegisterResponse.class);
		when(registerResponse.getInstanceId()).thenReturn("instance-1");
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		when(this.providerAPI.registerInstance(any(InstanceRegisterRequest.class))).thenReturn(registerResponse);

		// Act
		this.registry.register(TEST_HOST, TEST_PORT);

		// Assert
		verify(this.providerAPI).registerInstance(any(InstanceRegisterRequest.class));
		assertThat(PolarisSDKContextManager.isRegistered()).isTrue();
	}

	@DisplayName("register succeeds with compatible existing instances")
	@Test
	void testRegisterWithCompatibleInstances() {
		// Arrange
		Instance existingInstance = mock(Instance.class);
		Map<String, String> metadata = new HashMap<>();
		metadata.put(PolarisMcpMetadataKeys.PROTOCOL_VERSION, TEST_VERSION);
		when(existingInstance.getMetadata()).thenReturn(metadata);
		when(existingInstance.getProtocol()).thenReturn(TEST_PROTOCOL);

		InstancesResponse instancesResponse = mock(InstancesResponse.class);
		when(instancesResponse.getInstances()).thenReturn(new Instance[] { existingInstance });
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		when(this.consumerAPI.getHealthyInstances(any(GetHealthyInstancesRequest.class))).thenReturn(instancesResponse);

		InstanceRegisterResponse registerResponse = mock(InstanceRegisterResponse.class);
		when(registerResponse.getInstanceId()).thenReturn("instance-1");
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		when(this.providerAPI.registerInstance(any(InstanceRegisterRequest.class))).thenReturn(registerResponse);

		// Act & Assert
		assertThatCode(() -> this.registry.register(TEST_HOST, TEST_PORT)).doesNotThrowAnyException();
		verify(this.providerAPI).registerInstance(any(InstanceRegisterRequest.class));
	}

	@DisplayName("non-strict mode logs warn but continues on version mismatch")
	@Test
	void testNonStrictModeWarnsOnVersionMismatch() {
		// Arrange
		Instance existingInstance = mock(Instance.class);
		Map<String, String> metadata = new HashMap<>();
		metadata.put(PolarisMcpMetadataKeys.PROTOCOL_VERSION, "2024-11-05");
		when(existingInstance.getMetadata()).thenReturn(metadata);
		when(existingInstance.getProtocol()).thenReturn(TEST_PROTOCOL);

		InstancesResponse instancesResponse = mock(InstancesResponse.class);
		when(instancesResponse.getInstances()).thenReturn(new Instance[] { existingInstance });
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		when(this.consumerAPI.getHealthyInstances(any(GetHealthyInstancesRequest.class))).thenReturn(instancesResponse);

		InstanceRegisterResponse registerResponse = mock(InstanceRegisterResponse.class);
		when(registerResponse.getInstanceId()).thenReturn("instance-1");
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);
		when(this.providerAPI.registerInstance(any(InstanceRegisterRequest.class))).thenReturn(registerResponse);

		// Act & Assert
		assertThatCode(() -> this.registry.register(TEST_HOST, TEST_PORT)).doesNotThrowAnyException();
		verify(this.providerAPI).registerInstance(any(InstanceRegisterRequest.class));
	}

	@DisplayName("strict mode throws on incompatible protocol version")
	@Test
	void testStrictModeThrowsOnVersionMismatch() {
		// Arrange
		PolarisMcpServerRegistry strictRegistry = PolarisMcpServerRegistry.builder()
			.sdkContextManager(this.sdkContextManager)
			.namespace(TEST_NAMESPACE)
			.serviceName(TEST_SERVICE)
			.protocol(TEST_PROTOCOL)
			.endpointPath(TEST_ENDPOINT)
			.protocolVersion(TEST_VERSION)
			.strictCompatible(true)
			.build();

		Instance existingInstance = mock(Instance.class);
		Map<String, String> metadata = new HashMap<>();
		metadata.put(PolarisMcpMetadataKeys.PROTOCOL_VERSION, "2024-11-05");
		when(existingInstance.getMetadata()).thenReturn(metadata);
		when(existingInstance.getProtocol()).thenReturn(TEST_PROTOCOL);

		InstancesResponse instancesResponse = mock(InstancesResponse.class);
		when(instancesResponse.getInstances()).thenReturn(new Instance[] { existingInstance });
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		when(this.consumerAPI.getHealthyInstances(any(GetHealthyInstancesRequest.class))).thenReturn(instancesResponse);

		// Act & Assert
		assertThatThrownBy(() -> strictRegistry.register(TEST_HOST, TEST_PORT))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Protocol version mismatch");
	}

	@DisplayName("strict mode throws on incompatible transport protocol")
	@Test
	void testStrictModeThrowsOnProtocolMismatch() {
		// Arrange
		PolarisMcpServerRegistry strictRegistry = PolarisMcpServerRegistry.builder()
			.sdkContextManager(this.sdkContextManager)
			.namespace(TEST_NAMESPACE)
			.serviceName(TEST_SERVICE)
			.protocol(TEST_PROTOCOL)
			.endpointPath(TEST_ENDPOINT)
			.protocolVersion(TEST_VERSION)
			.strictCompatible(true)
			.build();

		Instance existingInstance = mock(Instance.class);
		Map<String, String> metadata = new HashMap<>();
		metadata.put(PolarisMcpMetadataKeys.PROTOCOL_VERSION, TEST_VERSION);
		when(existingInstance.getMetadata()).thenReturn(metadata);
		when(existingInstance.getProtocol()).thenReturn(PolarisMcpMetadataKeys.PROTOCOL_MCP_STREAMABLE_HTTP);

		InstancesResponse instancesResponse = mock(InstancesResponse.class);
		when(instancesResponse.getInstances()).thenReturn(new Instance[] { existingInstance });
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		when(this.consumerAPI.getHealthyInstances(any(GetHealthyInstancesRequest.class))).thenReturn(instancesResponse);

		// Act & Assert
		assertThatThrownBy(() -> strictRegistry.register(TEST_HOST, TEST_PORT))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Transport protocol mismatch");
	}

	@DisplayName("deregister calls ProviderAPI.deRegister")
	@Test
	void testDeregisterCallsProviderApi() throws NoSuchFieldException, IllegalAccessException {
		// Arrange
		setPrivateField(this.registry, "registeredInstanceId", "instance-1");
		setPrivateField(this.registry, "registeredHost", TEST_HOST);
		setPrivateField(this.registry, "registeredPort", TEST_PORT);
		when(this.sdkContextManager.getProviderAPI()).thenReturn(this.providerAPI);

		// Act
		this.registry.deregister();

		// Assert
		verify(this.providerAPI).deRegister(any(InstanceDeregisterRequest.class));
	}

	@DisplayName("deregister is no-op when not registered")
	@Test
	void testDeregisterNoOpWhenNotRegistered() {
		// Arrange (registeredInstanceId is null by default)

		// Act
		this.registry.deregister();

		// Assert
		verify(this.providerAPI, never()).deRegister(any(InstanceDeregisterRequest.class));
	}

	private static void setPrivateField(Object object, String fieldName, Object value)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(object, value);
	}

}
