# Spring AI Polaris

[![Codecov](https://codecov.io/gh/polarismesh/spring-ai-polaris/branch/main/graph/badge.svg)](https://codecov.io/gh/polarismesh/spring-ai-polaris)
[![Testing](https://github.com/polarismesh/spring-ai-polaris/actions/workflows/testing.yml/badge.svg)](https://github.com/polarismesh/spring-ai-polaris/actions/workflows/testing.yml)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

[English](./README.md) | 简体中文

README:

- [介绍](#介绍)
- [如何构建](#如何构建)

## 介绍

Spring AI Polaris 是一个 [Spring AI](https://spring.io/projects/spring-ai) 扩展项目，集成 [北极星](https://polarismesh.cn/)（PolarisMesh）实现 MCP Server 的注册、发现与治理。

- [北极星 Github](https://github.com/polarismesh/polaris)

Spring AI 集成北极星可以解决以下问题：

- **MCP Server 注册** — 应用启动时自动将 MCP Server 实例注册到北极星，关闭时自动反注册
- **MCP Client 发现** — 通过北极星发现 MCP Server 实例，支持 Watch 机制动态更新
- **负载均衡** — 在健康的 MCP Server 实例之间进行轮询选择
- **调用上报** — 将调用结果上报至北极星，用于熔断和负载均衡统计

## 如何构建

**Linux and Mac**

```bash
./mvnw clean install
```

**Windows**

```bash
.\mvnw.cmd clean install
```

## 许可证

本项目基于 [BSD 3-Clause 许可证](https://opensource.org/licenses/BSD-3-Clause) 开源。
