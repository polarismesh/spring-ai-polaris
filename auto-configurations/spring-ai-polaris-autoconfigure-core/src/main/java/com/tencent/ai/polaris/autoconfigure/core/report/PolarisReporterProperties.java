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

package com.tencent.ai.polaris.autoconfigure.core.report;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Polaris stat reporting.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.ai.polaris.reporter")
public class PolarisReporterProperties {

	/**
	 * Whether to enable stat reporting to Polaris.
	 */
	private boolean enabled = false;

	/**
	 * Stat reporting mode: "pull" (Prometheus scrape) or "push" (PushGateway).
	 */
	private String type = "pull";

	/**
	 * Prometheus scrape configuration (pull mode only).
	 */
	private Prometheus prometheus = new Prometheus();

	/**
	 * PushGateway configuration (push mode only).
	 */
	private PushGateway pushGateway = new PushGateway();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Prometheus getPrometheus() {
		return this.prometheus;
	}

	public void setPrometheus(Prometheus prometheus) {
		this.prometheus = prometheus;
	}

	public PushGateway getPushGateway() {
		return this.pushGateway;
	}

	public void setPushGateway(PushGateway pushGateway) {
		this.pushGateway = pushGateway;
	}

	@Override
	public String toString() {
		return "PolarisReporterProperties{"
				+ "enabled=" + this.enabled
				+ ", type='" + this.type + '\''
				+ ", prometheus=" + this.prometheus
				+ ", pushGateway=" + this.pushGateway
				+ '}';
	}

	/**
	 * Prometheus scrape configuration properties (pull mode only).
	 */
	public static class Prometheus {

		/**
		 * HTTP path for Prometheus to scrape metrics.
		 */
		private String path = "/metrics";

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public String toString() {
			return "Prometheus{"
					+ "path='" + this.path + '\''
					+ '}';
		}

	}

	/**
	 * PushGateway configuration properties (push mode only).
	 */
	public static class PushGateway {

		/**
		 * PushGateway addresses, comma-separated.
		 */
		private List<String> address;

		/**
		 * Namespace label used when pushing to PushGateway.
		 */
		private String namespace = "Polaris";

		/**
		 * Service label used when pushing to PushGateway.
		 */
		private String service = "polaris.pushgateway";

		/**
		 * Interval in milliseconds between pushes to PushGateway.
		 */
		private long pushInterval = 10000L;

		/**
		 * Whether to enable gzip compression when pushing to PushGateway.
		 */
		private boolean openGzip = false;

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

		public String getService() {
			return this.service;
		}

		public void setService(String service) {
			this.service = service;
		}

		public long getPushInterval() {
			return this.pushInterval;
		}

		public void setPushInterval(long pushInterval) {
			this.pushInterval = pushInterval;
		}

		public boolean isOpenGzip() {
			return this.openGzip;
		}

		public void setOpenGzip(boolean openGzip) {
			this.openGzip = openGzip;
		}

		@Override
		public String toString() {
			return "PushGateway{"
					+ "address=" + this.address
					+ ", namespace='" + this.namespace + '\''
					+ ", service='" + this.service + '\''
					+ ", pushInterval=" + this.pushInterval
					+ ", openGzip=" + this.openGzip
					+ '}';
		}

	}

}
