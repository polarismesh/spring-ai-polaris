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

package com.tencent.ai.polaris.autoconfigure.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.mock.env.MockEnvironment;

import com.tencent.polaris.logging.LoggingConsts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisLoggingApplicationListener}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisLoggingApplicationListener")
class PolarisLoggingApplicationListenerTest {

	private static final String TEST_DEFAULT_PATH = "target/test-polaris-logs";

	private PolarisLoggingApplicationListener listener;

	private String originalLoggingPath;

	@BeforeEach
	void setUp() {
		this.listener = new PolarisLoggingApplicationListener();
		this.originalLoggingPath = System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY);
	}

	@AfterEach
	void tearDown() {
		if (this.originalLoggingPath != null) {
			System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY, this.originalLoggingPath);
		}
		else {
			System.clearProperty(LoggingConsts.LOGGING_PATH_PROPERTY);
		}
	}

	@DisplayName("supports ApplicationEnvironmentPreparedEvent")
	@Test
	void testSupportsApplicationEnvironmentPreparedEvent() {
		// Arrange
		ResolvableType type = ResolvableType.forClass(ApplicationEnvironmentPreparedEvent.class);

		// Act
		boolean result = this.listener.supportsEventType(type);

		// Assert
		assertThat(result).isTrue();
	}

	@DisplayName("supports ApplicationFailedEvent")
	@Test
	void testSupportsApplicationFailedEvent() {
		// Arrange
		ResolvableType type = ResolvableType.forClass(ApplicationFailedEvent.class);

		// Act
		boolean result = this.listener.supportsEventType(type);

		// Assert
		assertThat(result).isTrue();
	}

	@DisplayName("supports WebServerInitializedEvent")
	@Test
	void testSupportsWebServerInitializedEvent() {
		// Arrange
		ResolvableType type = ResolvableType.forClass(WebServerInitializedEvent.class);

		// Act
		boolean result = this.listener.supportsEventType(type);

		// Assert
		assertThat(result).isTrue();
	}

	@DisplayName("does not support unsupported event type")
	@Test
	void testDoesNotSupportUnsupportedEventType() {
		// Arrange
		ResolvableType type = ResolvableType.forClass(ApplicationStartedEvent.class);

		// Act
		boolean result = this.listener.supportsEventType(type);

		// Assert
		assertThat(result).isFalse();
	}

	@DisplayName("does not support null raw class")
	@Test
	void testDoesNotSupportNullRawClass() {
		// Arrange
		ResolvableType type = ResolvableType.NONE;

		// Act
		boolean result = this.listener.supportsEventType(type);

		// Assert
		assertThat(result).isFalse();
	}

	@DisplayName("order is LoggingApplicationListener.DEFAULT_ORDER + 2")
	@Test
	void testGetOrder() {
		// Arrange

		// Act
		int order = this.listener.getOrder();

		// Assert
		assertThat(order).isEqualTo(LoggingApplicationListener.DEFAULT_ORDER + 2);
	}

	@DisplayName("sets logging path from environment on ApplicationEnvironmentPreparedEvent")
	@Test
	void testSetsLoggingPathFromEnvironment() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.ai.polaris.logging.path", "/tmp/polaris-logs");
		ApplicationEnvironmentPreparedEvent event = mock(ApplicationEnvironmentPreparedEvent.class);
		when(event.getEnvironment()).thenReturn(environment);

		// Act
		assertThatCode(() -> this.listener.onApplicationEvent(event)).doesNotThrowAnyException();

		// Assert
		assertThat(System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY)).isEqualTo("/tmp/polaris-logs");
	}

	@DisplayName("does not set logging path when property is blank")
	@Test
	void testDoesNotSetLoggingPathWhenBlank() {
		// Arrange
		System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY, TEST_DEFAULT_PATH);
		MockEnvironment environment = new MockEnvironment();
		ApplicationEnvironmentPreparedEvent event = mock(ApplicationEnvironmentPreparedEvent.class);
		when(event.getEnvironment()).thenReturn(environment);

		// Act
		assertThatCode(() -> this.listener.onApplicationEvent(event)).doesNotThrowAnyException();

		// Assert
		// The environment has no logging path configured, so the property should
		// remain at the test default, not be changed to a custom value.
		assertThat(System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY))
				.isEqualTo(TEST_DEFAULT_PATH);
	}

	@DisplayName("handles ApplicationFailedEvent with context")
	@Test
	void testHandlesApplicationFailedEventWithContext() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.ai.polaris.logging.path", "/tmp/polaris-fail-logs");
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		when(context.getEnvironment()).thenReturn(environment);
		ApplicationFailedEvent event = mock(ApplicationFailedEvent.class);
		when(event.getApplicationContext()).thenReturn(context);

		// Act
		assertThatCode(() -> this.listener.onApplicationEvent(event)).doesNotThrowAnyException();

		// Assert
		assertThat(System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY)).isEqualTo("/tmp/polaris-fail-logs");
	}

	@DisplayName("handles ApplicationFailedEvent with null context")
	@Test
	void testHandlesApplicationFailedEventWithNullContext() {
		// Arrange
		System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY, TEST_DEFAULT_PATH);
		ApplicationFailedEvent event = mock(ApplicationFailedEvent.class);
		when(event.getApplicationContext()).thenReturn(null);

		// Act
		assertThatCode(() -> this.listener.onApplicationEvent(event)).doesNotThrowAnyException();

		// Assert
		assertThat(System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY))
				.isEqualTo(TEST_DEFAULT_PATH);
	}

	@DisplayName("handles WebServerInitializedEvent without setting logging path")
	@Test
	void testHandlesWebServerInitializedEvent() {
		// Arrange
		System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY, TEST_DEFAULT_PATH);
		WebServerInitializedEvent event = mock(WebServerInitializedEvent.class);

		// Act
		assertThatCode(() -> this.listener.onApplicationEvent(event)).doesNotThrowAnyException();

		// Assert
		assertThat(System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY))
				.isEqualTo(TEST_DEFAULT_PATH);
	}

}
