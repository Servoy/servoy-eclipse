package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.utils.DeterministicTarball;
import com.servoy.eclipse.ngclient.ui.utils.TarEntry;
import com.servoy.eclipse.ngclient.ui.utils.TarInputStream;

/**
 * SVY-21502: a packed web-package tarball must be byte-identical for the same logical content, whatever the
 * checkout did to mtimes, line endings or file order (its name and lockfile integrity depend on those bytes).
 */
class DeterministicTarballTest
{
	@TempDir
	Path tmp;

	@Test
	void sameContentDifferentMtimeCrlfOrderAndTimestampGivesIdenticalBytes() throws Exception
	{
		File a = new File(tmp.toFile(), "a.tgz");
		File b = new File(tmp.toFile(), "b.tgz");
		// checkout 1: LF, files in one order, one mtime, a .timestamp
		writeTgz(a, 1000L, "package/package.json", "{\n \"name\": \"x\"\n}\n", "package/src/a.ts", "export const a = 1;\n",
			"package/.timestamp", "111");
		// checkout 2: CRLF, other order, other mtime, other .timestamp
		writeTgz(b, 999999L, "package/src/a.ts", "export const a = 1;\r\n", "package/package.json", "{\r\n \"name\": \"x\"\r\n}\r\n",
			"package/.timestamp", "222");
		assertFalse(java.util.Arrays.equals(Files.readAllBytes(a.toPath()), Files.readAllBytes(b.toPath())), "precondition: inputs differ");

		DeterministicTarball.normalize(a);
		DeterministicTarball.normalize(b);

		assertArrayEquals(Files.readAllBytes(a.toPath()), Files.readAllBytes(b.toPath()));
	}

	@Test
	void timestampDroppedAndTextNormalizedButContentChangeIsKept() throws Exception
	{
		File a = new File(tmp.toFile(), "a.tgz");
		File b = new File(tmp.toFile(), "b.tgz");
		writeTgz(a, 5L, "package/package.json", "one\r\ntwo\r\n", "package/.timestamp", "1");
		writeTgz(b, 5L, "package/package.json", "one\r\nTWO\r\n", "package/.timestamp", "1");
		DeterministicTarball.normalize(a);
		DeterministicTarball.normalize(b);

		List<String> names = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		read(a, names, texts);
		assertTrue(names.equals(List.of("package/package.json")), "only package.json expected but was " + names);
		assertTrue(texts.get(0).equals("one\ntwo\n"), "CRLF must become LF but was " + texts.get(0));
		assertFalse(java.util.Arrays.equals(Files.readAllBytes(a.toPath()), Files.readAllBytes(b.toPath())), "a real content change must still differ");
	}

	@Test
	void binaryContentIsLeftUntouched()
	{
		byte[] bin = new byte[] { 1, 0, '\r', '\n', 2 };
		assertArrayEquals(bin, DeterministicTarball.normalizeContent(bin));
	}

	private static void read(File tgz, List<String> names, List<String> texts) throws Exception
	{
		try (InputStream in = new GZIPInputStream(Files.newInputStream(tgz.toPath())))
		{
			TarInputStream tar = new TarInputStream(in);
			TarEntry e;
			while ((e = tar.getNextEntry()) != null)
			{
				ByteArrayOutputStream o = new ByteArrayOutputStream();
				byte[] buf = new byte[512];
				int r;
				while ((r = tar.read(buf, 0, buf.length)) > 0)
					o.write(buf, 0, r);
				names.add(e.getName());
				texts.add(o.toString(StandardCharsets.UTF_8));
			}
		}
	}

	/** minimal ustar writer: pairs of (name, content) */
	private static void writeTgz(File f, long mtime, String... nameContent) throws IOException
	{
		ByteArrayOutputStream tar = new ByteArrayOutputStream();
		for (int i = 0; i < nameContent.length; i += 2)
		{
			byte[] data = nameContent[i + 1].getBytes(StandardCharsets.UTF_8);
			byte[] h = new byte[512];
			put(h, 0, nameContent[i]);
			put(h, 100, String.format("%07o", 0755));
			put(h, 108, String.format("%07o", 1000));
			put(h, 116, String.format("%07o", 1000));
			put(h, 124, String.format("%011o", data.length));
			put(h, 136, String.format("%011o", mtime));
			for (int k = 148; k < 156; k++)
				h[k] = ' ';
			h[156] = '0';
			put(h, 257, "ustar");
			long sum = 0;
			for (byte x : h)
				sum += x & 0xff;
			put(h, 148, String.format("%06o", sum));
			h[154] = 0;
			h[155] = ' ';
			tar.write(h);
			tar.write(data);
			tar.write(new byte[(512 - data.length % 512) % 512]);
		}
		tar.write(new byte[1024]);
		try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(f.toPath())))
		{
			out.write(tar.toByteArray());
		}
	}

	private static void put(byte[] h, int off, String s)
	{
		byte[] b = s.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(b, 0, h, off, b.length);
	}
}
