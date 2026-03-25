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

package com.tencent.polaris.ai.core;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;

/**
 * Manages the lifecycle of the Polaris SDK context. Holds static references to
 * SDKContext, ProviderAPI and ConsumerAPI. Initialization is lazy (first call to any
 * getter triggers it).
 *
 * @author Haotian Zhang
 */
public class PolarisSDKContextManager {

	private static final Logger logger = LoggerFactory.getLogger(PolarisSDKContextManager.class);

	/**
	 * Flag used by the JVM shutdown hook to wait until deregistration completes.
	 */
	public static volatile boolean isRegistered = false;

	private static volatile SDKContext sdkContext;

	private static volatile ProviderAPI providerAPI;

	private static volatile ConsumerAPI consumerAPI;

	private final List<PolarisConfigModifier> modifiers;

	public PolarisSDKContextManager(List<PolarisConfigModifier> modifiers) {
		this.modifiers = modifiers;
	}

	/**
	 * Called by the shutdown hook. Do not call directly.
	 */
	public static void innerDestroy() {
		if (Objects.nonNull(sdkContext)) {
			try {
				if (Objects.nonNull(providerAPI)) {
					((AutoCloseable) providerAPI).close();
					providerAPI = null;
				}
				if (Objects.nonNull(consumerAPI)) {
					((AutoCloseable) consumerAPI).close();
					consumerAPI = null;
				}
				if (Objects.nonNull(sdkContext)) {
					sdkContext.destroy();
					sdkContext = null;
				}
				logger.info("Polaris SDK context destroyed.");
			}
			catch (Throwable t) {
				logger.error("Failed to destroy Polaris SDK context.", t);
			}
		}
	}

	public ProviderAPI getProviderAPI() {
		initService();
		return providerAPI;
	}

	public ConsumerAPI getConsumerAPI() {
		initService();
		return consumerAPI;
	}

	public SDKContext getSDKContext() {
		initService();
		return sdkContext;
	}

	public synchronized void initService() {
		if (sdkContext != null) {
			return;
		}
		try {
			ConfigurationImpl config = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();
			this.modifiers.stream()
				.sorted(Comparator.comparingInt(PolarisConfigModifier::getOrder))
				.forEach(modifier -> modifier.modify(config));
			sdkContext = SDKContext.initContextByConfig(config);
			sdkContext.init();
			providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);
			consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				long start = System.currentTimeMillis();
				while (isRegistered && System.currentTimeMillis() - start < 60_000) {
					Thread.onSpinWait();
				}
				innerDestroy();
			}));
			logger.info("Polaris SDK context initialized.");
		}
		catch (Throwable t) {
			logger.error("Failed to initialize Polaris SDK context.", t);
			throw new IllegalStateException("Failed to initialize Polaris SDK context", t);
		}
	}

}
