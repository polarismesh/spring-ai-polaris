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

package com.tencent.ai.polaris.autoconfigure.core;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Polaris SDK connection.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.ai.polaris")
public class PolarisCoreProperties {

	/**
	 * Addresses of the Polaris server, comma-separated.
	 */
	private List<String> address = List.of("127.0.0.1:8091");

	/**
	 * Default namespace for all Polaris operations. Individual modules can override this
	 * with their own namespace property.
	 */
	private String namespace = "default";

	/**
	 * Whether to prefer IPv6 addresses when auto-detecting the local host address.
	 * Defaults to {@code false} (IPv4 preferred).
	 */
	private boolean preferIpv6 = false;

	public List<String> getAddress() {
		return this.address;
	}

	public void setAddress(List<String> address) {
		this.address = address;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public boolean isPreferIpv6() {
		return this.preferIpv6;
	}

	public void setPreferIpv6(boolean preferIpv6) {
		this.preferIpv6 = preferIpv6;
	}

	@Override
	public String toString() {
		return "PolarisCoreProperties{"
				+ "address=" + this.address
				+ ", namespace='" + this.namespace + '\''
				+ ", preferIpv6=" + this.preferIpv6
				+ '}';
	}

}
