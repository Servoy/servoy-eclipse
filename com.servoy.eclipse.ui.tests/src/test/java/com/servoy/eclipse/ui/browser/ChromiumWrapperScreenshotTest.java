package com.servoy.eclipse.ui.browser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChromiumWrapper#decodeScreenshotResult(byte[])} (SVY-21460).
 * <p>
 * These cover the pure normalization logic of the Chromium screenshot capture, which is the part
 * that carries actual risk: the Equo {@code captureScreenshot()} future does not yield raw PNG bytes
 * but the UTF-8 bytes of a Base64 string, so the wrapper must decode them. The decode is extracted
 * into a package-private static helper precisely so it can be exercised without a live CEF browser;
 * this test lives in the same package (via the {@code com.servoy.eclipse.ui} fragment) to reach it.
 * </p>
 */
@DisplayName("ChromiumWrapper.decodeScreenshotResult — screenshot byte normalization (SVY-21460)")
class ChromiumWrapperScreenshotTest {

	/** The 8-byte PNG signature that every real PNG starts with. */
	private static final byte[] PNG_SIGNATURE = { (byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

	@Test
	@DisplayName("Base64 text bytes (the Equo contract) are decoded back to the raw PNG bytes")
	void testDecodesBase64EncodedPngToRawBytes() {
		// Simulate what Equo hands back: a PNG's raw bytes, Base64-encoded, then exposed as the
		// UTF-8 bytes of that Base64 text.
		byte[] rawPng = samplePngBytes();
		String base64Text = Base64.getEncoder().encodeToString(rawPng);
		byte[] equoResult = base64Text.getBytes(StandardCharsets.UTF_8);

		byte[] decoded = ChromiumWrapper.decodeScreenshotResult(equoResult);

		assertArrayEquals(rawPng, decoded, "Base64 result should be decoded to the original raw PNG bytes");
	}

	@Test
	@DisplayName("null result yields null")
	void testNullResultYieldsNull() {
		assertNull(ChromiumWrapper.decodeScreenshotResult(null), "null capture result should stay null");
	}

	@Test
	@DisplayName("empty result yields null (nothing was captured)")
	void testEmptyResultYieldsNull() {
		assertNull(ChromiumWrapper.decodeScreenshotResult(new byte[0]), "empty capture result should become null");
	}

	@Test
	@DisplayName("non-Base64 bytes are returned unchanged (defensive fallback for a future raw-PNG contract)")
	void testRawPngFallbackWhenNotBase64() {
		// The PNG signature contains bytes (0x89, 0x1A ...) that are not valid Base64, so if a future
		// Equo release ever returns raw PNG bytes directly, decoding must not corrupt or drop them.
		byte[] rawPng = samplePngBytes();

		byte[] result = ChromiumWrapper.decodeScreenshotResult(rawPng);

		assertSame(rawPng, result, "non-Base64 bytes should be passed through unchanged as a fallback");
	}

	/**
	 * Builds a small but realistic PNG-like byte array: the real PNG signature followed by some
	 * payload bytes. The signature bytes are intentionally not valid Base64 so the fallback branch
	 * is genuinely exercised.
	 */
	private static byte[] samplePngBytes() {
		byte[] payload = "the-rendered-form-pixels".getBytes(StandardCharsets.UTF_8);
		byte[] png = new byte[PNG_SIGNATURE.length + payload.length];
		System.arraycopy(PNG_SIGNATURE, 0, png, 0, PNG_SIGNATURE.length);
		System.arraycopy(payload, 0, png, PNG_SIGNATURE.length, payload.length);
		return png;
	}
}
