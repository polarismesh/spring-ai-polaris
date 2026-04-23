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

package com.tencent.ai.polaris.example.mcp.server.ssewebflux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
 * Example service exposing time tools, resources and prompts via MCP.
 *
 * @author Haotian Zhang
 */
@Service
public class TimeService {

	private static final Logger logger = LoggerFactory.getLogger(TimeService.class);

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

	// ---- Tools ----

	@McpTool(name = "getCurrentTime", description = "Get current time for a given IANA timezone")
	public String getCurrentTime(
			@McpToolParam(description = "IANA timezone id, e.g. Asia/Shanghai, UTC, America/New_York") String timezone) {
		logger.info("Received getCurrentTime request for timezone={}", timezone);
		ZoneId zoneId = parseZone(timezone);
		String result = ZonedDateTime.now(zoneId).format(FORMATTER);
		logger.info("Returning current time: {}", result);
		return result;
	}

	@McpTool(name = "convertTimezone",
			description = "Convert a time from one IANA timezone to another")
	public String convertTimezone(
			@McpToolParam(description = "Time in ISO-8601 format, e.g. 2026-04-23T10:00:00") String time,
			@McpToolParam(description = "Source IANA timezone id") String fromTimezone,
			@McpToolParam(description = "Target IANA timezone id") String toTimezone) {
		logger.info("Received convertTimezone request: time={}, from={}, to={}", time, fromTimezone, toTimezone);
		ZoneId fromZone = parseZone(fromTimezone);
		ZoneId toZone = parseZone(toTimezone);
		ZonedDateTime source = LocalDateTime.parse(time).atZone(fromZone);
		ZonedDateTime target = source.withZoneSameInstant(toZone);
		String result = target.format(FORMATTER);
		logger.info("Returning converted time: {}", result);
		return result;
	}

	// ---- Resource ----

	@McpResource(uri = "timezone://list", name = "Supported Timezones",
			description = "List of commonly used IANA timezones")
	public String getSupportedTimezones() {
		logger.info("Received resource request for timezone://list");
		return "UTC, Asia/Shanghai, Asia/Tokyo, Asia/Singapore, Europe/London, Europe/Paris, "
				+ "America/New_York, America/Los_Angeles, Australia/Sydney";
	}

	// ---- Prompt ----

	@McpPrompt(name = "time-query",
			description = "Generate a prompt that asks an LLM to describe the current time in a given timezone")
	public McpSchema.GetPromptResult timeQuery(
			@McpArg(name = "timezone", description = "IANA timezone id", required = true) String timezone,
			@McpArg(name = "language", description = "Language for the description, defaults to English") String language) {
		logger.info("Received prompt request for time-query, timezone={}, language={}", timezone, language);

		String lang = StringUtils.isNotBlank(language) ? language : "English";
		String currentTime = getCurrentTime(timezone);

		String promptText = String.format(
				"Please describe the following date and time in %s for people living in %s, "
						+ "including whether it is morning, afternoon, evening, or night:\n\n%s",
				lang, timezone, currentTime);

		return new McpSchema.GetPromptResult("Time Query for " + timezone,
				List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
						new McpSchema.TextContent(promptText))));
	}

	private ZoneId parseZone(String timezone) {
		if (StringUtils.isBlank(timezone)) {
			return DEFAULT_ZONE;
		}
		try {
			return ZoneId.of(timezone);
		}
		catch (Exception ex) {
			logger.warn("Invalid timezone '{}', falling back to UTC", timezone);
			return DEFAULT_ZONE;
		}
	}

}
