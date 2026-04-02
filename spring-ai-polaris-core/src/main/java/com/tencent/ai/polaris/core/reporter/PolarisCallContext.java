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

package com.tencent.ai.polaris.core.reporter;

import java.util.Objects;

import com.tencent.polaris.api.pojo.InstanceType;
import com.tencent.polaris.api.pojo.RetStatus;

/**
 * Captures the context of a single remote service call for reporting to Polaris.
 *
 * @author Haotian Zhang
 */
public final class PolarisCallContext {

	private final String namespace;

	private final String serviceName;

	private final String host;

	private final int port;

	private final String method;

	private final long delayMs;

	private final int retCode;

	private final RetStatus retStatus;

	private final InstanceType instanceType;

	private PolarisCallContext(Builder builder) {
		this.namespace = builder.namespace;
		this.serviceName = builder.serviceName;
		this.host = builder.host;
		this.port = builder.port;
		this.method = builder.method;
		this.delayMs = builder.delayMs;
		this.retCode = builder.retCode;
		this.retStatus = builder.retStatus;
		this.instanceType = builder.instanceType;
	}

	/**
	 * Creates a new {@link Builder} instance.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getMethod() {
		return this.method;
	}

	public long getDelayMs() {
		return this.delayMs;
	}

	public int getRetCode() {
		return this.retCode;
	}

	public RetStatus getRetStatus() {
		return this.retStatus;
	}

	public InstanceType getInstanceType() {
		return this.instanceType;
	}

	@Override
	public String toString() {
		return "PolarisCallContext{" + "namespace='" + this.namespace + '\'' + ", serviceName='" + this.serviceName
				+ '\'' + ", host='" + this.host + '\'' + ", port=" + this.port + ", method='" + this.method + '\''
				+ ", delayMs=" + this.delayMs + ", retCode=" + this.retCode + ", retStatus=" + this.retStatus
				+ ", instanceType=" + this.instanceType + '}';
	}

	/**
	 * Builder for {@link PolarisCallContext}.
	 */
	public static final class Builder {

		private String namespace;

		private String serviceName;

		private String host;

		private int port;

		private String method;

		private long delayMs;

		private int retCode;

		private RetStatus retStatus;

		private InstanceType instanceType = InstanceType.MICROSERVICE;

		private Builder() {
		}

		/**
		 * Set the Polaris namespace.
		 * @param namespace the namespace
		 * @return this builder
		 */
		public Builder namespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Set the Polaris service name.
		 * @param serviceName the service name
		 * @return this builder
		 */
		public Builder serviceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		/**
		 * Set the host address of the called instance.
		 * @param host the host address
		 * @return this builder
		 */
		public Builder host(String host) {
			this.host = host;
			return this;
		}

		/**
		 * Set the port of the called instance.
		 * @param port the port number
		 * @return this builder
		 */
		public Builder port(int port) {
			this.port = port;
			return this;
		}

		/**
		 * Set the method or path of the call.
		 * @param method the method name
		 * @return this builder
		 */
		public Builder method(String method) {
			this.method = method;
			return this;
		}

		/**
		 * Set the call duration in milliseconds.
		 * @param delayMs the delay in milliseconds
		 * @return this builder
		 */
		public Builder delayMs(long delayMs) {
			this.delayMs = delayMs;
			return this;
		}

		/**
		 * Set the return code of the call.
		 * @param retCode the return code
		 * @return this builder
		 */
		public Builder retCode(int retCode) {
			this.retCode = retCode;
			return this;
		}

		/**
		 * Set the return status of the call.
		 * @param retStatus the return status
		 * @return this builder
		 */
		public Builder retStatus(RetStatus retStatus) {
			this.retStatus = retStatus;
			return this;
		}

		/**
		 * Set the instance type. Defaults to {@link InstanceType#MICROSERVICE}.
		 * @param instanceType the instance type
		 * @return this builder
		 */
		public Builder instanceType(InstanceType instanceType) {
			this.instanceType = instanceType;
			return this;
		}

		/**
		 * Builds a new {@link PolarisCallContext} from this builder.
		 * @return the constructed context
		 * @throws NullPointerException if required fields are not set
		 */
		public PolarisCallContext build() {
			Objects.requireNonNull(this.namespace, "namespace must not be null");
			Objects.requireNonNull(this.serviceName, "serviceName must not be null");
			Objects.requireNonNull(this.host, "host must not be null");
			Objects.requireNonNull(this.method, "method must not be null");
			Objects.requireNonNull(this.retStatus, "retStatus must not be null");
			return new PolarisCallContext(this);
		}

	}

}
