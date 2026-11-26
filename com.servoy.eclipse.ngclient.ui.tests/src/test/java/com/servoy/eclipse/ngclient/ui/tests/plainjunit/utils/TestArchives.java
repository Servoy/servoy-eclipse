package com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * Builds small tar / zip / tar.gz / tar.xz archives in memory for the archive
 * tests.
 */
public final class TestArchives {
	private TestArchives() {
	}

	/** One entry of a tar archive. */
	public record TarItem(String name, String prefix, int type, long mode, byte[] data, String linkName) {
		public static TarItem file(String name, String content) {
			return new TarItem(name, null, '0', 0644, content.getBytes(StandardCharsets.UTF_8), null);
		}

		public static TarItem file(String name, String content, long mode) {
			return new TarItem(name, null, '0', mode, content.getBytes(StandardCharsets.UTF_8), null);
		}

		public static TarItem directory(String name) {
			return new TarItem(name, null, '5', 0755, new byte[0], null);
		}

		public static TarItem symlink(String name, String target) {
			return new TarItem(name, null, '2', 0777, new byte[0], target);
		}

		public static TarItem withPrefix(String prefix, String name, String content) {
			return new TarItem(name, prefix, '0', 0644, content.getBytes(StandardCharsets.UTF_8), null);
		}

		/**
		 * GNU long name pseudo entry that carries the real name of the entry that
		 * follows it.
		 */
		public static TarItem gnuLongName(String longName) {
			byte[] nameBytes = longName.getBytes(StandardCharsets.UTF_8);
			byte[] data = new byte[nameBytes.length + 1];
			System.arraycopy(nameBytes, 0, data, 0, nameBytes.length);
			return new TarItem("././@LongLink", null, 'L', 0644, data, null);
		}
	}

	/** Builds an uncompressed ustar archive, terminated by two zero blocks. */
	public static byte[] tar(TarItem... items) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (TarItem item : items) {
			out.writeBytes(header(item));
			out.writeBytes(item.data());
			int pad = (512 - (item.data().length % 512)) % 512;
			out.writeBytes(new byte[pad]);
		}
		out.writeBytes(new byte[1024]);
		return out.toByteArray();
	}

	/** Builds a single 512 byte tar header with a valid checksum. */
	public static byte[] header(TarItem item) {
		byte[] header = new byte[512];
		put(header, 0, 100, item.name());
		put(header, 100, 8, String.format("%07o", item.mode()));
		put(header, 108, 8, "0000000");
		put(header, 116, 8, "0000000");
		put(header, 124, 12, String.format("%011o", (long) item.data().length));
		put(header, 136, 12, String.format("%011o", 1700000000L));
		header[156] = (byte) item.type();
		if (item.linkName() != null) {
			put(header, 157, 100, item.linkName());
		}
		put(header, 257, 6, "ustar");
		if (item.prefix() != null) {
			put(header, 345, 155, item.prefix());
		}
		for (int i = 148; i < 156; i++) {
			header[i] = ' ';
		}
		long sum = 0;
		for (byte b : header) {
			sum += b & 0xff;
		}
		put(header, 148, 8, String.format("%06o", sum));
		header[154] = 0;
		header[155] = ' ';
		return header;
	}

	private static void put(byte[] header, int offset, int length, String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(bytes, 0, header, offset, Math.min(bytes.length, length));
	}

	public static byte[] gzip(byte[] data) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
			out.write(data);
		}
		return bytes.toByteArray();
	}

	public static byte[] xz(byte[] data) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (XZOutputStream out = new XZOutputStream(bytes, new LZMA2Options())) {
			out.write(data);
		}
		return bytes.toByteArray();
	}

	/**
	 * Builds a zip from alternating name / content pairs; a name ending in '/' is a
	 * directory entry (content ignored).
	 */
	public static byte[] zip(String... nameAndContent) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream out = new ZipOutputStream(bytes)) {
			for (int i = 0; i < nameAndContent.length; i += 2) {
				ZipEntry entry = new ZipEntry(nameAndContent[i]);
				entry.setTime(1700000000000L);
				out.putNextEntry(entry);
				if (!nameAndContent[i].endsWith("/")) {
					out.write(nameAndContent[i + 1].getBytes(StandardCharsets.UTF_8));
				}
				out.closeEntry();
			}
		}
		return bytes.toByteArray();
	}
}
