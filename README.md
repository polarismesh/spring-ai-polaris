# Spring AI Polaris

[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

English | [简体中文](./README-zh.md)

README:

- [Introduction](#introduction)
- [How to Build](#how-to-build)

## Introduction

Spring AI Polaris is a [Spring AI](https://spring.io/projects/spring-ai) extension that integrates with [Polaris](https://polarismesh.cn/) (PolarisMesh) for MCP Server registration, discovery and governance.

- [Polaris Github](https://github.com/polarismesh/polaris)

Spring AI with Polaris can solve these problems:

- **MCP Server Registration** — Automatically register MCP Server instances to Polaris on startup, deregister on shutdown
- **MCP Client Discovery** — Discover MCP Server instances from Polaris with dynamic updates via Watch mechanism
- **Load Balancing** — Round-robin selection across healthy MCP Server instances
- **Call Reporting** — Report call results to Polaris for circuit-breaking and load-balancing statistics

## How to Build

**Linux and Mac**

```bash
./mvnw clean install
```

**Windows**

```bash
.\mvnw.cmd clean install
```

## License

This project is licensed under the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause).
