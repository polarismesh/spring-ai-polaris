# MCP Example

*English | [简体中文](README-zh.md)*

This example demonstrates how to use **spring-ai-polaris** to let a Spring AI MCP Client, via Polaris service discovery, simultaneously connect to **multiple MCP Servers with different transports and different capabilities**, and expose the aggregated MCP capabilities through a unified set of HTTP endpoints.

## Module Layout

```
examples/mcp-example/
├── mcp-example-client/                          # MCP client (Spring AI ChatClient + REST Controller)
└── mcp-example-server/
    ├── mcp-example-sse-webflux-server/          # MCP time service (SSE + WebFlux)
    └── mcp-example-streamable-webmvc-server/    # MCP weather service (Streamable HTTP + WebMvc)
```

| Module | Port | Polaris Service Name | Transport | Domain |
| --- | --- | --- | --- | --- |
| `mcp-example-client` | 48080 | `mcp-example-client` | — | Aggregates both MCP servers and exposes REST API |
| `mcp-example-sse-webflux-server` | 48081 | `mcp-example-sse-webflux-server` | SSE (WebFlux) | Time |
| `mcp-example-streamable-webmvc-server` | 48082 | `mcp-example-streamable-webmvc-server` | Streamable HTTP (WebMvc) | Weather |

## Quick Start

### Prerequisites

- A running Polaris service (default `127.0.0.1:8091`)
- An OpenAI-compatible LLM endpoint (configure via the client's `OPENAI_BASE_URL` / `OPENAI_API_KEY` environment variables)

### Startup Order

1. Start `mcp-example-sse-webflux-server` (time service)
2. Start `mcp-example-streamable-webmvc-server` (weather service)
3. Start `mcp-example-client` (the REST API; it is more reliable to start it after the two servers have registered with Polaris)

## Client HTTP API

`McpClientController` exposes MCP (Model Context Protocol) capabilities as HTTP endpoints, supporting LLM-driven automatic tool calling, direct tool invocation, resource reading, and prompt retrieval.

- Base URL: `http://localhost:48080`
- Context path: `/client/mcp`
- All endpoints use `POST`; both request and response bodies are `application/json`

### Backend MCP Server Topology

The client uses Polaris service discovery to simultaneously subscribe to two groups of MCP servers:

| Cluster Alias | Polaris Service Name | Transport | Domain | Main Capabilities |
| --- | --- | --- | --- | --- |
| `my-weather` | `mcp-example-streamable-webmvc-server` | Streamable HTTP (WebMvc) | Weather | `getWeather` tool, `weather://cities` / `weather://city/{city}` resources, `weather-forecast` prompt |
| `my-time` | `mcp-example-sse-webflux-server` | SSE (WebFlux) | Time | `getCurrentTime` / `convertTimezone` tools, `timezone://list` resource, `time-query` prompt |

The client's `/tool/list` / `/resource/list` / `/prompt/list` aggregate capabilities from both server groups; `/tool/call` / `/resource/read` / `/prompt/get` automatically route to the owning server by name/URI.

### Endpoint Summary

| Method | Path | Description |
| --- | --- | --- |
| POST | `/client/mcp/chat` | Send a message to the LLM, which can automatically invoke discovered MCP tools |
| POST | `/client/mcp/tool/list` | List all discovered MCP tools |
| POST | `/client/mcp/tool/call` | Directly invoke a specific MCP tool by name (bypassing the LLM) |
| POST | `/client/mcp/resource/list` | List all discovered MCP resources |
| POST | `/client/mcp/resource/read` | Read an MCP resource by URI |
| POST | `/client/mcp/prompt/list` | List all discovered MCP prompts |
| POST | `/client/mcp/prompt/get` | Get the rendered result of an MCP prompt by name |

---

## 1. Chat — LLM + MCP Tools

### Request

`POST /client/mcp/chat`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `message` | string | yes* | User message. May be omitted when `promptName` is provided and a prompt content is successfully fetched |
| `resourceUris` | string[] | no | MCP resource URIs; their text contents are read and prepended to the message as context |
| `promptName` | string | no | MCP prompt name. When provided, the rendered prompt replaces `message` |
| `promptArguments` | object | no | Prompt arguments as key-value pairs |

### Response

A string containing the LLM's reply.

### Examples

A weather question (routed to `my-weather`):

```bash
curl -X POST http://localhost:48080/client/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the weather in Beijing today?"
  }'
```

Example response:

```
Beijing is sunny today with a temperature of 25°C.
```

A time question (routed to `my-time`):

```bash
curl -X POST http://localhost:48080/client/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What time is it in Tokyo now?"
  }'
```

Combining resources and a prompt:

```bash
curl -X POST http://localhost:48080/client/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Summarize the supported cities",
    "resourceUris": ["weather://cities"],
    "promptName": "weather-forecast",
    "promptArguments": {"city": "Beijing", "language": "English"}
  }'
```

---

## 2. List Tools

### Request

`POST /client/mcp/tool/list`

No request body.

### Response

A list of `McpSchema.Tool`, aggregated from all connected MCP servers.

### Example

```bash
curl -X POST http://localhost:48080/client/mcp/tool/list
```

Example response (aggregated from both servers):

```json
[
  {
    "name": "getWeather",
    "title": "getWeather",
    "description": "Get current weather for a city",
    "inputSchema": {
      "type": "object",
      "properties": {
        "arg0": { "type": "string", "description": "City name" }
      },
      "required": ["arg0"]
    }
  },
  {
    "name": "getCurrentTime",
    "title": "getCurrentTime",
    "description": "Get current time for a given IANA timezone",
    "inputSchema": {
      "type": "object",
      "properties": {
        "arg0": { "type": "string", "description": "IANA timezone id, e.g. Asia/Shanghai, UTC, America/New_York" }
      },
      "required": ["arg0"]
    }
  },
  {
    "name": "convertTimezone",
    "title": "convertTimezone",
    "description": "Convert a time from one IANA timezone to another",
    "inputSchema": {
      "type": "object",
      "properties": {
        "arg0": { "type": "string", "description": "Time in ISO-8601 format, e.g. 2026-04-23T10:00:00" },
        "arg1": { "type": "string", "description": "Source IANA timezone id" },
        "arg2": { "type": "string", "description": "Target IANA timezone id" }
      },
      "required": ["arg0", "arg1", "arg2"]
    }
  }
]
```

> Note: parameter names follow `inputSchema.properties`. The example servers' tool methods do not declare explicit parameter names, so for methods with multiple parameters the generated names are `arg0` / `arg1` / `arg2` in order.

---

## 3. Call a Tool Directly

### Request

`POST /client/mcp/tool/call`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `toolName` | string | yes | Tool name; must match a `name` returned by `/tool/list` |
| `arguments` | object | no | Tool arguments as key-value pairs; must match the tool's `inputSchema` |

### Response

The tool's `content` array. If no such tool exists, returns the string `Tool not found: <name>`.

### Examples

Invoke the weather tool:

```bash
curl -X POST http://localhost:48080/client/mcp/tool/call \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "getWeather",
    "arguments": { "arg0": "Beijing" }
  }'
```

Example response:

```json
[
  { "text": "Sunny, 25°C in Beijing" }
]
```

Invoke the current-time tool:

```bash
curl -X POST http://localhost:48080/client/mcp/tool/call \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "getCurrentTime",
    "arguments": { "arg0": "Asia/Shanghai" }
  }'
```

Example response:

```json
[
  { "text": "2026-04-23 15:32:10 CST" }
]
```

Invoke the timezone conversion tool:

```bash
curl -X POST http://localhost:48080/client/mcp/tool/call \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "convertTimezone",
    "arguments": {
      "arg0": "2026-04-23T10:00:00",
      "arg1": "Asia/Shanghai",
      "arg2": "America/New_York"
    }
  }'
```

---

## 4. List Resources

### Request

`POST /client/mcp/resource/list`

No request body.

### Response

A list of `McpSchema.Resource`.

### Example

```bash
curl -X POST http://localhost:48080/client/mcp/resource/list
```

Example response (aggregated from both servers):

```json
[
  {
    "uri": "weather://cities",
    "name": "Supported Cities",
    "description": "List of cities supported by the weather service",
    "mimeType": "text/plain"
  },
  {
    "uri": "timezone://list",
    "name": "Supported Timezones",
    "description": "List of commonly used IANA timezones",
    "mimeType": "text/plain"
  }
]
```

---

## 5. Read a Resource

### Request

`POST /client/mcp/resource/read`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `uri` | string | yes | Resource URI |

### Response

The resource `contents` array. If not found, returns `Resource not found: <uri>`.

### Examples

Read the weather resource:

```bash
curl -X POST http://localhost:48080/client/mcp/resource/read \
  -H "Content-Type: application/json" \
  -d '{ "uri": "weather://cities" }'
```

Example response:

```json
[
  {
    "uri": "weather://cities",
    "mimeType": "text/plain",
    "text": "Beijing, Shanghai, Shenzhen, Guangzhou, Hangzhou, Chengdu, New York, London, Tokyo, Paris"
  }
]
```

Read the timezone resource:

```bash
curl -X POST http://localhost:48080/client/mcp/resource/read \
  -H "Content-Type: application/json" \
  -d '{ "uri": "timezone://list" }'
```

---

## 6. List Prompts

### Request

`POST /client/mcp/prompt/list`

No request body.

### Response

A list of `McpSchema.Prompt`.

### Example

```bash
curl -X POST http://localhost:48080/client/mcp/prompt/list
```

Example response (aggregated from both servers):

```json
[
  {
    "name": "weather-forecast",
    "title": "",
    "description": "Generate a prompt that asks an LLM to write a weather forecast summary",
    "arguments": [
      { "name": "city", "description": "City name", "required": true },
      { "name": "language", "description": "Language for the forecast, defaults to English", "required": false }
    ]
  },
  {
    "name": "time-query",
    "title": "",
    "description": "Generate a prompt that asks an LLM to describe the current time in a given timezone",
    "arguments": [
      { "name": "timezone", "description": "IANA timezone id", "required": true },
      { "name": "language", "description": "Language for the description, defaults to English", "required": false }
    ]
  }
]
```

---

## 7. Get a Prompt

### Request

`POST /client/mcp/prompt/get`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `name` | string | yes | Prompt name |
| `arguments` | object | no | Prompt arguments as key-value pairs |

### Response

A `McpSchema.GetPromptResult` containing the rendered `messages`. If not found, returns `Prompt not found: <name>`.

### Examples

Get the weather prompt:

```bash
curl -X POST http://localhost:48080/client/mcp/prompt/get \
  -H "Content-Type: application/json" \
  -d '{
    "name": "weather-forecast",
    "arguments": {
      "city": "Beijing",
      "language": "English"
    }
  }'
```

Example response:

```json
{
  "description": "Weather Forecast for Beijing",
  "messages": [
    {
      "role": "user",
      "content": {
        "type": "text",
        "text": "Based on the following weather data, write a concise and friendly weather forecast summary in English for the city of Beijing:\n\n{\"city\":\"Beijing\",\"temperature\":25,\"unit\":\"celsius\",\"humidity\":60,\"wind\":\"NE 12km/h\",\"condition\":\"Sunny\"}"
      }
    }
  ]
}
```

Get the time prompt:

```bash
curl -X POST http://localhost:48080/client/mcp/prompt/get \
  -H "Content-Type: application/json" \
  -d '{
    "name": "time-query",
    "arguments": {
      "timezone": "Asia/Tokyo",
      "language": "English"
    }
  }'
```

---

## Error Handling

- When a tool / resource / prompt is not found, the endpoint returns a plain-text hint with HTTP 200. Callers should check the response type to decide whether it represents an error.
- Internal failures (for example an unreachable MCP server) are surfaced as 5xx by Spring's default error handling.
