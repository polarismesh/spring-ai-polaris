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

package com.tencent.ai.polaris.example.mcp.server.streamablewebmvc;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import org.springframework.stereotype.Service;

import com.tencent.polaris.api.utils.StringUtils;

/**
 * Example service exposing weather tools, resources and prompts via MCP.
 *
 * @author Haotian Zhang
 */
@Service
public class WeatherService {

	private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

	// ---- Tool ----

	@McpTool(name = "getWeather", description = "Get current weather for a city")
	public String getWeather(
			@McpToolParam(description = "City name") String city) {
		logger.info("Received getWeather request for city={}", city);
		// Stub implementation for demo purposes
		String result = "Sunny, 25°C in " + city;
		logger.info("Returning weather result: {}", result);
		return result;
	}

	// ---- Resources ----

	@McpResource(uri = "weather://cities", name = "Supported Cities",
			description = "List of cities supported by the weather service")
	public String getSupportedCities() {
		logger.info("Received resource request for weather://cities");
		return "Beijing, Shanghai, Shenzhen, Guangzhou, Hangzhou, Chengdu, New York, London, Tokyo, Paris";
	}

	@McpResource(uri = "weather://city/{city}", name = "City Weather",
			description = "Detailed weather data for a specific city")
	public String getCityWeather(String city) {
		logger.info("Received resource request for weather://city/{}", city);
		return String.format("{\"city\":\"%s\",\"temperature\":25,\"unit\":\"celsius\","
				+ "\"humidity\":60,\"wind\":\"NE 12km/h\",\"condition\":\"Sunny\"}", city);
	}

	// ---- Prompt ----

	@McpPrompt(name = "weather-forecast",
			description = "Generate a prompt that asks an LLM to write a weather forecast summary")
	public McpSchema.GetPromptResult weatherForecast(
			@McpArg(name = "city", description = "City name", required = true) String city,
			@McpArg(name = "language", description = "Language for the forecast, defaults to English") String language) {
		logger.info("Received prompt request for weather-forecast, city={}, language={}", city, language);

		String lang = StringUtils.isNotBlank(language) ? language : "English";
		String weatherData = getCityWeather(city);

		String promptText = String.format(
				"Based on the following weather data, write a concise and friendly weather "
						+ "forecast summary in %s for the city of %s:\n\n%s",
				lang, city, weatherData);

		return new McpSchema.GetPromptResult("Weather Forecast for " + city,
				List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
						new McpSchema.TextContent(promptText))));
	}

}
