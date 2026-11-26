/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110, USA
*/

package com.servoy.eclipse.ngclient.ui.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

/**
 * Makes a <code>.tgz</code> (as produced by <code>pnpm pack</code>) byte-for-byte reproducible (SVY-21502).
 * <p>
 * The name AND the <code>integrity</code> hash that pnpm records in <code>pnpm-lock.yaml</code> for a
 * <code>file:</code> tarball depend on the exact bytes of that tarball. Those bytes otherwise depend on things a git
 * checkout (of a customer, of Jenkins, of a developer) can change without any real content change: file
 * modification times, CRLF/LF line endings, file permissions, entry order and the gzip/deflate implementation.
 * This class rewrites the tarball with: entries sorted by name, fixed mtime/uid/gid/mode, text files normalized to
 * LF, the generated <code>.timestamp</code> change-detection file dropped, a fixed gzip header and stored (not
 * compressed) deflate blocks - so the result does not even depend on the zlib version of the running JRE.
 */
public final class DeterministicTarball
{
	/** the (non packaged) change detection file that is written into a package folder by the developer/exporter. */
	public static final String TIMESTAMP_FILE = ".timestamp";

	/** same fixed date npm uses for reproducible packs: 1985-10-26T08:15:00Z */
	private static final long FIXED_MTIME = 499162500L;

	private DeterministicTarball()
	{
	}

	/**
	 * Returns the bytes with CRLF line endings converted to LF if the content looks like text (no NUL bytes in the
	 * first 8000 bytes), otherwise the same array unchanged.
	 */
	public static byte[] normalizeContent(byte[] data)
	{
		int check = Math.min(data.length, 8000);
		boolean hasCr = false;
		for (int i = 0; i < check; i++)
		{
			if (data[i] == 0) return data; // binary
		}
		for (byte b : data)
		{
			if (b == '\r')
			{
				hasCr = true;
				break;
			}
		}
		if (!hasCr) return data;
		ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
		for (int i = 0; i < data.length; i++)
		{
			if (data[i] == '\r' && i + 1 < data.length && data[i + 1] == '\n') continue;
			out.write(data[i]);
		}
		return out.toByteArray();
	}

	/**
	 * Rewrites the given tgz in place into its deterministic form.
	 */
	public static void normalize(File tgz) throws IOException
	{
		Map<String, byte[]> entries = new TreeMap<>();
		try (InputStream in = new GZIPInputStream(Files.newInputStream(tgz.toPath())))
		{
			TarInputStream tar = new TarInputStream(in);
			TarEntry entry;
			while ((entry = tar.getNextEntry()) != null)
			{
				if (entry.getFileType() != TarEntry.FILE) continue; // directories etc. are implied by the file paths
				String name = entry.getName();
				if (name.equals("package/" + TIMESTAMP_FILE) || name.equals(TIMESTAMP_FILE)) continue;
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				int read;
				while ((read = tar.read(buffer, 0, buffer.length)) > 0)
				{
					content.write(buffer, 0, read);
				}
				entries.put(name, normalizeContent(content.toByteArray()));
			}
		}
		catch (TarException e)
		{
			throw new IOException("Not a valid tar archive: " + tgz, e);
		}

		ByteArrayOutputStream tarBytes = new ByteArrayOutputStream();
		for (Map.Entry<String, byte[]> e : entries.entrySet())
		{
			writeHeader(tarBytes, e.getKey(), e.getValue().length);
			tarBytes.write(e.getValue());
			int pad = (512 - (e.getValue().length % 512)) % 512;
			tarBytes.write(new byte[pad]);
		}
		tarBytes.write(new byte[1024]); // end of archive

		File tmp = new File(tgz.getParentFile(), tgz.getName() + ".tmp");
		try (OutputStream out = Files.newOutputStream(tmp.toPath()))
		{
			writeStoredGzip(out, tarBytes.toByteArray());
		}
		Files.move(tmp.toPath(), tgz.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Returns the content of <code>package/package.json</code> inside the given tgz, or <code>null</code> if it has none.
	 */
	public static String readPackageJson(File tgz) throws IOException
	{
		try (InputStream in = new GZIPInputStream(Files.newInputStream(tgz.toPath())))
		{
			TarInputStream tar = new TarInputStream(in);
			TarEntry entry;
			while ((entry = tar.getNextEntry()) != null)
			{
				if (!"package/package.json".equals(entry.getName())) continue;
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				int read;
				while ((read = tar.read(buffer, 0, buffer.length)) > 0)
				{
					content.write(buffer, 0, read);
				}
				return content.toString(StandardCharsets.UTF_8);
			}
			return null;
		}
		catch (TarException e)
		{
			throw new IOException("Not a valid tar archive: " + tgz, e);
		}
	}

	private static void writeHeader(ByteArrayOutputStream out, String path, long size) throws IOException
	{
		byte[] nameBytes = path.getBytes(StandardCharsets.UTF_8);
		String name = path;
		String prefix = "";
		if (nameBytes.length > 100)
		{
			// split at a '/' so that prefix <= 155 and name <= 100 bytes (ustar)
			int idx = path.lastIndexOf('/');
			while (idx > 0)
			{
				String p = path.substring(0, idx);
				String n = path.substring(idx + 1);
				if (p.getBytes(StandardCharsets.UTF_8).length <= 155 && n.getBytes(StandardCharsets.UTF_8).length <= 100)
				{
					prefix = p;
					name = n;
					break;
				}
				idx = path.lastIndexOf('/', idx - 1);
			}
			if (prefix.isEmpty()) throw new IOException("Path too long for a ustar tar entry: " + path);
		}
		byte[] header = new byte[512];
		put(header, 0, 100, name);
		putOctal(header, 100, 8, 0644);
		putOctal(header, 108, 8, 0);
		putOctal(header, 116, 8, 0);
		putOctal(header, 124, 12, size);
		putOctal(header, 136, 12, FIXED_MTIME);
		for (int i = 148; i < 156; i++)
			header[i] = ' ';
		header[156] = '0';
		put(header, 257, 6, "ustar");
		header[262] = 0;
		header[263] = '0';
		header[264] = '0';
		put(header, 345, 155, prefix);
		long sum = 0;
		for (byte b : header)
			sum += b & 0xff;
		String chk = String.format("%06o", Long.valueOf(sum));
		put(header, 148, 6, chk);
		header[154] = 0;
		header[155] = ' ';
		out.write(header);
	}

	private static void put(byte[] header, int offset, int length, String value)
	{
		byte[] b = value.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(b, 0, header, offset, Math.min(b.length, length));
	}

	private static void putOctal(byte[] header, int offset, int length, long value)
	{
		String s = String.format("%0" + (length - 1) + "o", Long.valueOf(value));
		put(header, offset, length - 1, s);
		header[offset + length - 1] = 0;
	}

	/**
	 * Writes a gzip stream with a fixed header and the data in stored (uncompressed) deflate blocks, which is
	 * identical regardless of the zlib version.
	 */
	private static void writeStoredGzip(OutputStream out, byte[] data) throws IOException
	{
		// magic, deflate, no flags, mtime 0, no extra flags, OS unknown
		out.write(new byte[] { 0x1f, (byte)0x8b, 8, 0, 0, 0, 0, 0, 0, (byte)0xff });
		int pos = 0;
		do
		{
			int len = Math.min(65535, data.length - pos);
			boolean last = pos + len >= data.length;
			out.write(last ? 1 : 0);
			out.write(len & 0xff);
			out.write((len >> 8) & 0xff);
			out.write(~len & 0xff);
			out.write((~len >> 8) & 0xff);
			out.write(data, pos, len);
			pos += len;
		}
		while (pos < data.length);
		CRC32 crc = new CRC32();
		crc.update(data);
		writeIntLE(out, crc.getValue());
		writeIntLE(out, data.length);
	}

	private static void writeIntLE(OutputStream out, long v) throws IOException
	{
		out.write((int)(v & 0xff));
		out.write((int)((v >> 8) & 0xff));
		out.write((int)((v >> 16) & 0xff));
		out.write((int)((v >> 24) & 0xff));
	}
}
