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

package com.tencent.ai.polaris.mcp.client;

import io.modelcontextprotocol.spec.McpSchema;

import com.tencent.ai.polaris.core.reporter.PolarisCallContext;
import com.tencent.ai.polaris.core.reporter.PolarisReporter;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.client.pojo.Node;

/**
 * Base class for a single Polaris-backed MCP client connection. Holds the underlying
 * MCP client, the remote {@link Node}, and handles Polaris call reporting.
 * <p>
 * Subclasses provide the concrete client type ({@code McpSyncClient} or
 * {@code McpAsyncClient}) and implement the close methods.
 *
 * @param <C> the MCP client type
 * @author Haotian Zhang
 */
public abstract class AbstractPolarisMcpClient<C> {

	/** Return code indicating a successful call. */
	protected static final int RET_CODE_SUCCESS = 0;

	/** Return code indicating a failed call. */
	protected static final int RET_CODE_FAIL = -1;

	private final C client;

	private final Node node;

	private final String namespace;

	private final String serverName;

	private final PolarisReporter polarisReporter;

	protected AbstractPolarisMcpClient(C client, Node node, String namespace, String serverName,
			PolarisReporter polarisReporter) {
		this.client = client;
		this.node = node;
		this.namespace = namespace;
		this.serverName = serverName;
		this.polarisReporter = polarisReporter;
	}

	/**
	 * Returns the underlying MCP client.
	 * @return the client
	 */
	public C getClient() {
		return this.client;
	}

	/**
	 * Reports a service call result to Polaris.
	 * @param method the tool method name
	 * @param delay the call duration in milliseconds
	 * @param retStatus the return status
	 */
	protected void reportCall(String method, long delay, RetStatus retStatus) {
		if (this.polarisReporter == null) {
			return;
		}
		int retCode = (retStatus == RetStatus.RetSuccess) ? RET_CODE_SUCCESS : RET_CODE_FAIL;
		this.polarisReporter.report(new PolarisCallContext(this.namespace, this.serverName, this.node.getHost(),
				this.node.getPort(), method, delay, retCode, retStatus));
	}

	/**
	 * Returns {@code true} if the given result indicates an error.
	 * @param result the call tool result (may be {@code null})
	 * @return whether the result is an error
	 */
	public static boolean isErrorResult(McpSchema.CallToolResult result) {
		return result != null && result.isError() != null && result.isError();
	}

	/**
	 * Close the underlying client immediately.
	 */
	protected abstract void closeClient();

	/**
	 * Close the underlying client gracefully.
	 */
	protected abstract void closeClientGracefully();

}
