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

package com.tencent.ai.polaris.mcp.common;

/**
 * Constants for MCP server registration with Polaris.
 * <p>
 * Follows the Polaris MCP registration protocol:
 * <ul>
 *   <li>Instance {@code protocol} field: {@code "mcp-sse"} or {@code "mcp-streamable-http"}</li>
 *   <li>Instance {@code metadata}: standard keys prefixed with {@code "mcp-"}</li>
 * </ul>
 *
 * @author Haotian Zhang
 */
public final class PolarisMcpMetadataKeys {

	// ---- Instance.protocol values (set on InstanceRegisterRequest.setProtocol) ----

	/** Protocol value for SSE transport. */
	public static final String PROTOCOL_MCP_SSE = "mcp-sse";

	/** Protocol value for Streamable HTTP transport. */
	public static final String PROTOCOL_MCP_STREAMABLE_HTTP = "mcp-streamable-http";

	/** Protocol value for Stdio transport (local process, no network registration). */
	public static final String PROTOCOL_MCP_STDIO = "mcp-stdio";

	// ---- Instance.metadata keys ----

	/** MCP Server name, equal to Polaris service name. Required. */
	public static final String SERVER_NAME = "mcp-server-name";

	/** MCP protocol version(s), comma-separated, e.g. {@code "2024-11-05"} or {@code "2024-11-05,2025-03-26"}. Required. */
	public static final String PROTOCOL_VERSION = "mcp-protocol-version";

	/** MCP endpoint path, e.g. {@code "/mcp"}, {@code "/sse"}. Optional. */
	public static final String ENDPOINT_PATH = "mcp-endpoint-path";

	private PolarisMcpMetadataKeys() {
	}

}
