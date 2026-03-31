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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for resolving the local host IP address. Supports both IPv4 and IPv6.
 * Iterates over network interfaces to find the first non-loopback address with the
 * lowest interface index.
 *
 * @author Haotian Zhang
 */
public final class PolarisInetUtils {

	private static final Logger logger = LoggerFactory.getLogger(PolarisInetUtils.class);

	private PolarisInetUtils() {
	}

	/**
	 * Finds the first non-loopback IP address. Tries IPv4 first, falls back to IPv6.
	 * If no suitable address is found, falls back to
	 * {@link InetAddress#getLocalHost()}.
	 * @return the resolved IP address string, or {@code null} if resolution fails
	 */
	public static String findFirstNonLoopbackAddress() {
		InetAddress result = findFirstNonLoopbackAddressByIpType(false);
		if (result == null) {
			result = findFirstNonLoopbackAddressByIpType(true);
		}
		if (result == null) {
			try {
				result = InetAddress.getLocalHost();
			}
			catch (UnknownHostException ex) {
				logger.warn("Unable to retrieve localhost address.", ex);
				return null;
			}
		}
		return stripZoneId(result.getHostAddress());
	}

	/**
	 * Resolves an IP address string for the given IP type.
	 * @param ipv6 {@code true} for IPv6, {@code false} for IPv4
	 * @return the resolved IP address string, or {@code null} if not found
	 */
	public static String getIpString(boolean ipv6) {
		InetAddress result = findFirstNonLoopbackAddressByIpType(ipv6);
		if (result == null) {
			return null;
		}
		return stripZoneId(result.getHostAddress());
	}

	/**
	 * Finds the first non-loopback address matching the requested IP type by iterating
	 * over all network interfaces. Prefers the interface with the lowest index.
	 * @param ipv6 {@code true} to find IPv6 addresses, {@code false} for IPv4
	 * @return the first matching {@link InetAddress}, or {@code null} if none found
	 */
	static InetAddress findFirstNonLoopbackAddressByIpType(boolean ipv6) {
		InetAddress result = null;
		try {
			int lowest = Integer.MAX_VALUE;
			for (Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces(); nics.hasMoreElements();) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp()) {
					logger.trace("Testing interface: {}", ifc.getDisplayName());
					if (ifc.getIndex() < lowest || result == null) {
						lowest = ifc.getIndex();
					}
					else {
						continue;
					}
					for (Enumeration<InetAddress> addrs = ifc
							.getInetAddresses(); addrs.hasMoreElements();) {
						InetAddress address = addrs.nextElement();
						if (ipv6) {
							if (address instanceof Inet6Address
									&& !address.isLinkLocalAddress()
									&& !address.isLoopbackAddress()) {
								logger.trace("Found non-loopback IPv6 interface: {}",
										ifc.getDisplayName());
								result = address;
							}
						}
						else {
							if (address instanceof Inet4Address
									&& !address.isLoopbackAddress()) {
								logger.trace("Found non-loopback IPv4 interface: {}",
										ifc.getDisplayName());
								result = address;
							}
						}
					}
				}
			}
		}
		catch (IOException ex) {
			logger.error("Cannot get first non-loopback address.", ex);
		}
		return result;
	}

	/**
	 * Strips the IPv6 zone identifier (e.g. {@code %eth0}) from a host address string.
	 * @param hostAddress the host address string
	 * @return the address without zone identifier
	 */
	private static String stripZoneId(String hostAddress) {
		if (hostAddress != null && hostAddress.contains("%")) {
			return hostAddress.split("%")[0];
		}
		return hostAddress;
	}

}
