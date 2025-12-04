package com.csg.airtel.aaa4j.common.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility for generating request trace identifiers.
 * <p>
 * The generated value is a 32-character, lowercase, hex string (UUID v4 without dashes),
 * which is widely compatible with logging/tracing systems and safe for use in headers.
 */
public final class TraceIdGenerator {

	private TraceIdGenerator() {
		// Utility class; do not instantiate
	}

	/**
	 * Generates a new trace id.
	 *
	 * @return 32-character lowercase hex string, e.g. "3f5a4c8eb2a94d17a9e6b2c3d4e5f678"
	 */
	public static String generateTraceId() {
		String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		String timePart = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		return uuidPart + "-" + timePart;}
}


