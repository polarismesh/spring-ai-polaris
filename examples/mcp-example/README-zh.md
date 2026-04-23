# MCP Example

*[English](README.md) | 简体中文*

本示例演示如何基于 **spring-ai-polaris**，让一个 Spring AI MCP Client 通过 Polaris 服务发现，同时接入 **多个不同协议、不同功能** 的 MCP Server，并通过 HTTP 接口统一对外暴露 MCP 能力。

## 模块结构

```
examples/mcp-example/
├── mcp-example-client/                          # MCP 客户端（Spring AI ChatClient + REST Controller）
└── mcp-example-server/
    ├── mcp-example-sse-webflux-server/          # MCP 时间服务（SSE + WebFlux）
    └── mcp-example-streamable-webmvc-server/    # MCP 天气服务（Streamable HTTP + WebMvc）
```

| 模块 | 端口 | Polaris 服务名 | 传输协议 | 功能 |
| --- | --- | --- | --- | --- |
| `mcp-example-client` | 48080 | `mcp-example-client` | — | 聚合两个 MCP Server，对外提供 REST API |
| `mcp-example-sse-webflux-server` | 48081 | `mcp-example-sse-webflux-server` | SSE (WebFlux) | 时间 |
| `mcp-example-streamable-webmvc-server` | 48082 | `mcp-example-streamable-webmvc-server` | Streamable HTTP (WebMvc) | 天气 |

## 快速开始

### 前置依赖

- 本地已启动 Polaris 服务（默认 `127.0.0.1:8091`）
- 一个兼容 OpenAI 协议的 LLM endpoint（配置在 client 的环境变量 `OPENAI_BASE_URL` / `OPENAI_API_KEY`）

### 启动顺序

1. 启动 `mcp-example-sse-webflux-server`（时间服务）
2. 启动 `mcp-example-streamable-webmvc-server`（天气服务）
3. 启动 `mcp-example-client`（客户端 + REST API，等待前两者在 Polaris 注册完成后启动更稳妥）

## Client 对外接口

`McpClientController` 将 MCP（Model Context Protocol）能力以 HTTP 接口形式暴露，支持通过 LLM 自动调用 MCP 工具、直接调用工具、读取资源、获取 Prompt 等。

- Base URL: `http://localhost:48080`
- 上下文路径: `/client/mcp`
- 所有接口均为 `POST`，请求体与响应体均为 `application/json`

### 后端 MCP Server 拓扑

Client 通过 Polaris 服务发现，同时订阅两组 MCP Server：

| cluster 别名 | Polaris 服务名 | 传输协议 | 功能 | 主要能力 |
| --- | --- | --- | --- | --- |
| `my-weather` | `mcp-example-streamable-webmvc-server` | Streamable HTTP (WebMvc) | 天气 | `getWeather` 工具、`weather://cities` / `weather://city/{city}` 资源、`weather-forecast` Prompt |
| `my-time` | `mcp-example-sse-webflux-server` | SSE (WebFlux) | 时间 | `getCurrentTime` / `convertTimezone` 工具、`timezone://list` 资源、`time-query` Prompt |

Client 的 `/tool/list` / `/resource/list` / `/prompt/list` 会聚合两组 server 的能力；`/tool/call` / `/resource/read` / `/prompt/get` 会按名称/URI 自动路由到拥有该能力的 server。

### 接口列表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/client/mcp/chat` | 发送消息给 LLM，LLM 可自动调用已发现的 MCP 工具 |
| POST | `/client/mcp/tool/list` | 列出所有已发现的 MCP 工具 |
| POST | `/client/mcp/tool/call` | 按名称直接调用指定 MCP 工具（不经 LLM） |
| POST | `/client/mcp/resource/list` | 列出所有已发现的 MCP 资源 |
| POST | `/client/mcp/resource/read` | 按 URI 读取指定 MCP 资源 |
| POST | `/client/mcp/prompt/list` | 列出所有已发现的 MCP Prompt |
| POST | `/client/mcp/prompt/get` | 按名称获取指定 MCP Prompt 的渲染结果 |

---

## 1. Chat —— LLM + MCP 工具

### 请求

`POST /client/mcp/chat`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `message` | string | 是* | 用户消息。若指定了 `promptName` 且成功获取到 Prompt 内容，可省略 |
| `resourceUris` | string[] | 否 | MCP 资源 URI 列表，会读取其文本内容并作为上下文拼接到消息前 |
| `promptName` | string | 否 | MCP Prompt 名称。指定后将用 Prompt 渲染结果替代 `message` |
| `promptArguments` | object | 否 | Prompt 参数键值对 |

### 响应

字符串，LLM 的回复内容。

### 示例

天气类提问（命中 `my-weather`）：

```bash
curl -X POST http://localhost:48080/client/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "帮我查一下北京今天的天气"
  }'
```

响应示例：

```
北京今天天气晴朗，气温25°C。
```

时间类提问（命中 `my-time`）：

```bash
curl -X POST http://localhost:48080/client/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "现在东京是几点？"
  }'
```

结合资源与 Prompt：

```bash
curl -X POST http://localhost:48080/client/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "总结一下支持的城市",
    "resourceUris": ["weather://cities"],
    "promptName": "weather-forecast",
    "promptArguments": {"city": "Beijing", "language": "Chinese"}
  }'
```

---

## 2. 列出工具

### 请求

`POST /client/mcp/tool/list`

无请求体。

### 响应

`McpSchema.Tool` 列表，聚合自所有已连接的 MCP 服务端。

### 示例

```bash
curl -X POST http://localhost:48080/client/mcp/tool/list
```

响应示例（两个 server 聚合后的结果）：

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

> 注意：参数名以 `inputSchema.properties` 为准。示例服务端的工具方法未显式声明参数名，多参数时依次生成为 `arg0` / `arg1` / `arg2`。

---

## 3. 直接调用工具

### 请求

`POST /client/mcp/tool/call`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `toolName` | string | 是 | 工具名称，需与 `/tool/list` 返回的 `name` 一致 |
| `arguments` | object | 否 | 工具入参，键值对形式，需符合工具 `inputSchema` |

### 响应

工具返回的 `content` 数组。若未找到工具，返回字符串 `Tool not found: <name>`。

### 示例

调用天气工具：

```bash
curl -X POST http://localhost:48080/client/mcp/tool/call \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "getWeather",
    "arguments": { "arg0": "Beijing" }
  }'
```

响应示例：

```json
[
  { "text": "Sunny, 25°C in Beijing" }
]
```

调用时间工具：

```bash
curl -X POST http://localhost:48080/client/mcp/tool/call \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "getCurrentTime",
    "arguments": { "arg0": "Asia/Shanghai" }
  }'
```

响应示例：

```json
[
  { "text": "2026-04-23 15:32:10 CST" }
]
```

调用时区转换：

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

## 4. 列出资源

### 请求

`POST /client/mcp/resource/list`

无请求体。

### 响应

`McpSchema.Resource` 列表。

### 示例

```bash
curl -X POST http://localhost:48080/client/mcp/resource/list
```

响应示例（两个 server 聚合后的结果）：

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

## 5. 读取资源

### 请求

`POST /client/mcp/resource/read`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `uri` | string | 是 | 资源 URI |

### 响应

资源 `contents` 数组。若未找到，返回 `Resource not found: <uri>`。

### 示例

读取天气资源：

```bash
curl -X POST http://localhost:48080/client/mcp/resource/read \
  -H "Content-Type: application/json" \
  -d '{ "uri": "weather://cities" }'
```

响应示例：

```json
[
  {
    "uri": "weather://cities",
    "mimeType": "text/plain",
    "text": "Beijing, Shanghai, Shenzhen, Guangzhou, Hangzhou, Chengdu, New York, London, Tokyo, Paris"
  }
]
```

读取时区资源：

```bash
curl -X POST http://localhost:48080/client/mcp/resource/read \
  -H "Content-Type: application/json" \
  -d '{ "uri": "timezone://list" }'
```

---

## 6. 列出 Prompt

### 请求

`POST /client/mcp/prompt/list`

无请求体。

### 响应

`McpSchema.Prompt` 列表。

### 示例

```bash
curl -X POST http://localhost:48080/client/mcp/prompt/list
```

响应示例（两个 server 聚合后的结果）：

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

## 7. 获取 Prompt

### 请求

`POST /client/mcp/prompt/get`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 是 | Prompt 名称 |
| `arguments` | object | 否 | Prompt 入参，键值对形式 |

### 响应

`McpSchema.GetPromptResult`，包含渲染后的 `messages`。若未找到，返回 `Prompt not found: <name>`。

### 示例

获取天气 Prompt：

```bash
curl -X POST http://localhost:48080/client/mcp/prompt/get \
  -H "Content-Type: application/json" \
  -d '{
    "name": "weather-forecast",
    "arguments": {
      "city": "Beijing",
      "language": "Chinese"
    }
  }'
```

响应示例：

```json
{
  "description": "Weather Forecast for Beijing",
  "messages": [
    {
      "role": "user",
      "content": {
        "type": "text",
        "text": "Based on the following weather data, write a concise and friendly weather forecast summary in Chinese for the city of Beijing:\n\n{\"city\":\"Beijing\",\"temperature\":25,\"unit\":\"celsius\",\"humidity\":60,\"wind\":\"NE 12km/h\",\"condition\":\"Sunny\"}"
      }
    }
  ]
}
```

获取时间 Prompt：

```bash
curl -X POST http://localhost:48080/client/mcp/prompt/get \
  -H "Content-Type: application/json" \
  -d '{
    "name": "time-query",
    "arguments": {
      "timezone": "Asia/Tokyo",
      "language": "Chinese"
    }
  }'
```

---

## 错误处理说明

- 工具 / 资源 / Prompt 未找到时，接口返回字符串形式的提示信息（HTTP 200）。调用方应根据返回类型判断是否为错误。
- 内部异常（如 MCP 服务端不可达）会由 Spring 默认机制返回 5xx。
