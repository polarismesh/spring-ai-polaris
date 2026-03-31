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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.polaris.api.utils.StringUtils;

/**
 * Utility class for parsing Polaris server addresses. Strips protocol prefixes (e.g.
 * {@code grpc://}) and extracts the {@code host:port} authority from each address.
 *
 * @author Haotian Zhang
 */
public final class AddressUtils {

	private static final String ADDRESS_SEPARATOR = ",";

	private AddressUtils() {
	}

	/**
	 * Parses a comma-separated address string into a list of {@code host:port} strings.
	 * Each address is treated as a URI; only the authority part is kept. For example,
	 * {@code "grpc://127.0.0.1:8091"} becomes {@code "127.0.0.1:8091"}.
	 * <p>
	 * If the input is {@code null} or blank, an empty list is returned.
	 * @param addressInfo comma-separated address string
	 * @return list of parsed {@code host:port} strings
	 */
	public static List<String> parseAddressList(String addressInfo) {
		if (StringUtils.isBlank(addressInfo)) {
			return Collections.emptyList();
		}
		List<String> addressList = new ArrayList<>();
		String[] addresses = addressInfo.split(ADDRESS_SEPARATOR);
		for (String address : addresses) {
			String trimmed = address.trim();
			try {
				URI uri = URI.create(trimmed);
				String authority = uri.getAuthority();
				if (authority != null) {
					addressList.add(authority);
				}
				else {
					// No scheme present, use as-is (e.g. "127.0.0.1:8091")
					addressList.add(trimmed);
				}
			}
			catch (IllegalArgumentException ex) {
				// Not a valid URI, use as-is
				addressList.add(trimmed);
			}
		}
		return addressList;
	}

}
