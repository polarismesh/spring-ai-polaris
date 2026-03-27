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

package com.tencent.ai.polaris.autoconfigure.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggingConsts;
import com.tencent.polaris.logging.PolarisLogging;

/**
 * Reload of Polaris logging configuration.
 * <p>
 * Listens for {@link ApplicationEnvironmentPreparedEvent} to configure the Polaris
 * logging path from {@code spring.ai.polaris.logging.path}, and reloads the Polaris
 * logging subsystem so that Polaris SDK internal logs are written to the correct
 * directory.
 *
 * @author Haotian Zhang
 */
public class PolarisLoggingApplicationListener implements GenericApplicationListener {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisLoggingApplicationListener.class);

	private static final int ORDER = LoggingApplicationListener.DEFAULT_ORDER + 2;

	@Override
	public boolean supportsEventType(ResolvableType resolvableType) {
		Class<?> type = resolvableType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(type)
				|| ApplicationFailedEvent.class.isAssignableFrom(type)
				|| WebServerInitializedEvent.class.isAssignableFrom(type);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		ConfigurableEnvironment environment = null;

		if (applicationEvent instanceof ApplicationEnvironmentPreparedEvent event) {
			environment = event.getEnvironment();
		}
		else if (applicationEvent instanceof ApplicationFailedEvent event) {
			ConfigurableApplicationContext context = event.getApplicationContext();
			if (context != null) {
				environment = context.getEnvironment();
			}
		}
		// WebServerInitializedEvent: environment is not extracted because the logging
		// path has already been set during ApplicationEnvironmentPreparedEvent.
		// We still reload here because Spring's LoggingSystem may have re-initialized
		// after web server startup, overriding the Polaris logging configuration.

		if (environment != null) {
			String loggingPath = environment.getProperty("spring.ai.polaris.logging.path");
			if (StringUtils.isNotBlank(loggingPath)) {
				System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY, loggingPath);
			}
		}
		LOG.info("Polaris logging configuration reloaded in {}.",
				applicationEvent.getClass().getSimpleName());
		PolarisLogging.getInstance().loadConfiguration();
	}

}
