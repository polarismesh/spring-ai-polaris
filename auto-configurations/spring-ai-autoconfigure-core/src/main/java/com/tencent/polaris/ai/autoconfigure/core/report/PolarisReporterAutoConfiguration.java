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

package com.tencent.polaris.ai.autoconfigure.core.report;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.tencent.polaris.ai.autoconfigure.core.ConditionalOnPolarisEnabled;
import com.tencent.polaris.ai.core.PolarisConfigModifier;
import com.tencent.polaris.ai.core.PolarisConfigModifierOrder;
import com.tencent.polaris.ai.core.PolarisSDKContextManager;
import com.tencent.polaris.ai.core.reporter.PolarisReporter;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;

/**
 * Auto-configuration for Polaris stat reporting.
 *
 * @author Haotian Zhang
 */
@AutoConfiguration
@ConditionalOnPolarisEnabled
@EnableConfigurationProperties(PolarisReporterProperties.class)
public class PolarisReporterAutoConfiguration {

	@Bean
	public PolarisConfigModifier reporterConfigModifier(PolarisReporterProperties properties) {
		return new PolarisConfigModifier() {
			@Override
			public void modify(ConfigurationImpl configuration) {
				StatReporterConfigImpl statReporterConfig =
						(StatReporterConfigImpl) configuration.getGlobal().getStatReporter();
				statReporterConfig.setEnable(properties.isEnabled());
				if (properties.isEnabled()) {
					PrometheusHandlerConfig prometheusConfig = statReporterConfig.getPluginConfig(
							StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
					prometheusConfig.setType(properties.getType());
					if ("pull".equals(properties.getType())) {
						prometheusConfig.setPath(properties.getPrometheus().getPath());
					}
					else {
						PolarisReporterProperties.PushGateway pg = properties.getPushGateway();
						prometheusConfig.setAddress(pg.getAddress());
						prometheusConfig.setNamespace(pg.getNamespace());
						prometheusConfig.setService(pg.getService());
						prometheusConfig.setPushInterval(pg.getPushInterval());
						prometheusConfig.setOpenGzip(pg.isOpenGzip());
					}
				}
			}

			@Override
			public int getOrder() {
				return PolarisConfigModifierOrder.REPORTER_ORDER;
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.ai.polaris.reporter", name = "enabled", havingValue = "true")
	public PolarisReporter polarisReporter(PolarisSDKContextManager manager) {
		return new PolarisReporter(manager);
	}

}
