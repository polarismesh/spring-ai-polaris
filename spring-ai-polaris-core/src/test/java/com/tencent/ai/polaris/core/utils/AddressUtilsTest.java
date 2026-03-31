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

package com.tencent.ai.polaris.core.utils;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AddressUtils}.
 *
 * @author Haotian Zhang
 */
@DisplayName("AddressUtils")
class AddressUtilsTest {

	@DisplayName("parseAddressList returns empty list for null input")
	@Test
	void testParseAddressListNullInput() {
		// Arrange & Act
		List<String> result = AddressUtils.parseAddressList(null);

		// Assert
		assertThat(result).isEmpty();
	}

	@DisplayName("parseAddressList returns empty list for blank input")
	@Test
	void testParseAddressListBlankInput() {
		// Arrange & Act
		List<String> result = AddressUtils.parseAddressList("  ");

		// Assert
		assertThat(result).isEmpty();
	}

	@DisplayName("parseAddressList strips grpc:// prefix")
	@Test
	void testParseAddressListStripsGrpcPrefix() {
		// Arrange
		String input = "grpc://127.0.0.1:8091";

		// Act
		List<String> result = AddressUtils.parseAddressList(input);

		// Assert
		assertThat(result).containsExactly("127.0.0.1:8091");
	}

	@DisplayName("parseAddressList keeps address without scheme as-is")
	@Test
	void testParseAddressListWithoutScheme() {
		// Arrange
		String input = "127.0.0.1:8091";

		// Act
		List<String> result = AddressUtils.parseAddressList(input);

		// Assert
		assertThat(result).containsExactly("127.0.0.1:8091");
	}

	@DisplayName("parseAddressList handles comma-separated addresses")
	@Test
	void testParseAddressListCommaSeparated() {
		// Arrange
		String input = "grpc://127.0.0.1:8091, grpc://127.0.0.2:8091";

		// Act
		List<String> result = AddressUtils.parseAddressList(input);

		// Assert
		assertThat(result).containsExactly("127.0.0.1:8091", "127.0.0.2:8091");
	}

	@DisplayName("parseAddressList handles mixed formats")
	@Test
	void testParseAddressListMixedFormats() {
		// Arrange
		String input = "grpc://127.0.0.1:8091, 127.0.0.2:8091";

		// Act
		List<String> result = AddressUtils.parseAddressList(input);

		// Assert
		assertThat(result).containsExactly("127.0.0.1:8091", "127.0.0.2:8091");
	}

	@DisplayName("parseAddressList strips http:// prefix")
	@Test
	void testParseAddressListStripsHttpPrefix() {
		// Arrange
		String input = "http://127.0.0.1:8091";

		// Act
		List<String> result = AddressUtils.parseAddressList(input);

		// Assert
		assertThat(result).containsExactly("127.0.0.1:8091");
	}

}
