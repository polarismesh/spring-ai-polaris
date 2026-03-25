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

package com.tencent.polaris.ai.core.reporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tencent.polaris.ai.core.PolarisSDKContextManager;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisReporter}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisReporter")
@ExtendWith(MockitoExtension.class)
class PolarisReporterTest {

	@Mock
	private PolarisSDKContextManager sdkContextManager;

	@Mock
	private ConsumerAPI consumerAPI;

	@DisplayName("report calls updateServiceCallResult with correct parameters")
	@Test
	void testReportCallsUpdateServiceCallResult() {
		// Arrange
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		PolarisReporter reporter = new PolarisReporter(this.sdkContextManager);
		PolarisCallContext ctx = new PolarisCallContext("default", "test-service", "127.0.0.1", 8080, 100L,
				RetStatus.RetSuccess);

		// Act
		reporter.report(ctx);

		// Assert
		verify(this.consumerAPI).updateServiceCallResult(any(ServiceCallResult.class));
	}

	@DisplayName("report handles exception gracefully")
	@Test
	void testReportHandlesPolarisException() {
		// Arrange
		when(this.sdkContextManager.getConsumerAPI()).thenReturn(this.consumerAPI);
		PolarisReporter reporter = new PolarisReporter(this.sdkContextManager);
		PolarisCallContext ctx = new PolarisCallContext("default", "test-service", "127.0.0.1", 8080, 100L,
				RetStatus.RetFail);
		doThrow(new PolarisException(ErrorCode.INTERNAL_ERROR, "mock polaris error")).when(this.consumerAPI)
			.updateServiceCallResult(any(ServiceCallResult.class));

		// Act & Assert
		assertThatCode(() -> reporter.report(ctx)).doesNotThrowAnyException();
		verify(this.consumerAPI).updateServiceCallResult(any(ServiceCallResult.class));
	}

}
