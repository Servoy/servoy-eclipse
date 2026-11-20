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
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * SVY-21642 - runs the real resources/precompress-war.mjs with a real node
 * against a fabricated WAR tree. Node is resolved from system property/env
 * servoy.nodePath, then a bundled com.servoy.eclipse.nodejs.* plugin node, then
 * PATH. When no working node is found the whole class is skipped (assumption).
 */
class PrecompressWarScriptTest {
	private static String node;
	private static Path script;

	@TempDir
	Path war;

	@BeforeAll
	static void locate() throws Exception {
		script = findScript();
		List<String> candidates = new ArrayList<>();
		for (String prop : new String[] { System.getProperty("servoy.nodePath"), System.getenv("servoy.nodePath"),
				System.getenv("SERVOY_NODEPATH") })
			if (prop != null && !prop.isBlank())
				candidates.add(prop);
		boolean win = System.getProperty("os.name").toLowerCase().contains("win");
		try {
			Path root = script.getParent().getParent().getParent();
			try (Stream<Path> dirs = Files.list(root)) {
				for (Path d : (Iterable<Path>) dirs
						.filter(p -> p.getFileName().toString().startsWith("com.servoy.eclipse.nodejs."))::iterator) {
					try (Stream<Path> walk = Files.walk(d, 6)) {
						walk.filter(Files::isRegularFile)
								.filter(p -> p.getFileName().toString().equals(win ? "node.exe" : "node"))
								.forEach(p -> candidates.add(p.toString()));
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			// bundled node layout differs: fall through to the next lookup step
		}
		candidates.add("node");
		StringBuilder tried = new StringBuilder();
		for (String c : candidates) {
			try {
				Result r = exec(List.of(c, "--version"));
				if (r.exit == 0) {
					node = c;
					return;
				}
				tried.append(c).append(" (exit ").append(r.exit).append(") ");
			} catch (IOException e) {
				tried.append(c).append(" (").append(e.getMessage()).append(") ");
			}
		}
		// no node on this machine (e.g. a CI agent without node on PATH): skip the
		// whole class
		Assumptions.assumeTrue(false,
				"No working node found, skipping PrecompressWarScriptTest. Set -Dservoy.nodePath=<node executable> or put node on PATH. Tried: "
						+ tried);
	}

	private static Path findScript() throws IOException {
		Path dir = Path.of("").toAbsolutePath();
		while (dir != null) {
			Path c = dir.resolve("com.servoy.eclipse.ngclient.ui/resources/precompress-war.mjs");
			if (Files.isRegularFile(c))
				return c;
			dir = dir.getParent();
		}
		throw new AssertionError("precompress-war.mjs not found above " + Path.of("").toAbsolutePath());
	}

	private record Result(int exit, String output) {
	}

	private static Result exec(List<String> command) throws IOException {
		Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
		p.getOutputStream().close();
		String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		try {
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				throw new AssertionError("process timed out: " + command + "\n" + out);
			}
		} catch (InterruptedException e) {
			p.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
		return new Result(p.exitValue(), out);
	}

	private Result run(String... a) throws IOException {
		List<String> cmd = new ArrayList<>(List.of(node, script.toString()));
		cmd.addAll(List.of(a));
		return exec(cmd);
	}

	private static byte[] text(int size) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() < size)
			sb.append("function foo(){return 42;} // some compressible text\n");
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private void write(String rel, byte[] data) throws IOException {
		Path p = war.resolve(rel);
		Files.createDirectories(p.getParent());
		Files.write(p, data);
	}

	private Map<String, byte[]> populate() throws IOException {
		byte[] rnd = new byte[4096];
		new Random(42).nextBytes(rnd);
		write("WEB-INF/x.js", text(4000));
		write("META-INF/y.css", text(4000));
		write("a.js", text(4000));
		write("locales/b.json", text(4000));
		write("locales/WEB-INF/z.js", text(4000));
		write("small.js", text(100));
		write("font.woff2", text(4000));
		write("rand.js", rnd);
		write("already.js.gz", text(4000));
		write("UP.CSS", text(4000));
		return snapshot();
	}

	private Map<String, byte[]> snapshot() throws IOException {
		Map<String, byte[]> m = new TreeMap<>();
		try (Stream<Path> w = Files.walk(war)) {
			for (Path p : (Iterable<Path>) w.filter(Files::isRegularFile)::iterator)
				m.put(war.relativize(p).toString().replace(File.separatorChar, '/'), Files.readAllBytes(p));
		}
		return m;
	}

	private void assertOriginalsUnchanged(Map<String, byte[]> before, Map<String, byte[]> after) {
		for (var e : before.entrySet()) {
			assertTrue(after.containsKey(e.getKey()), "original must still exist: " + e.getKey());
			assertArrayEquals(e.getValue(), after.get(e.getKey()), "original must be byte-identical: " + e.getKey());
		}
	}

	private Set<String> added(Map<String, byte[]> before, Map<String, byte[]> after) {
		Set<String> s = new TreeSet<>(after.keySet());
		s.removeAll(before.keySet());
		return s;
	}

	private byte[] decodeBrotli(Path br) throws IOException {
		Path out = Files.createTempFile(war, "dec", ".bin");
		try {
			Result r = exec(List.of(node, "-e",
					"const z=require('zlib'),f=require('fs');f.writeFileSync(process.argv[2],z.brotliDecompressSync(f.readFileSync(process.argv[1])))",
					br.toString(), out.toString()));
			assertEquals(0, r.exit, "brotli decode failed: " + r.output);
			return Files.readAllBytes(out);
		} finally {
			Files.deleteIfExists(out);
		}
	}

	private byte[] decodeGzip(byte[] gz) throws IOException {
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
			return in.readAllBytes();
		}
	}

	private static final Set<String> SELECTED = Set.of("a.js", "locales/b.json", "UP.CSS", "locales/WEB-INF/z.js");

	@Test
	@Timeout(120)
	@DisplayName("both formats: siblings only for selected files, none for WEB-INF/META-INF/small/woff2/random/already-compressed; originals intact; siblings decode to originals")
	void bothFormats() throws Exception {
		Map<String, byte[]> before = populate();
		Result r = run(war.toString(), "--brotli", "--gzip");
		assertEquals(0, r.exit, r.output);
		Map<String, byte[]> after = snapshot();
		Set<String> expected = new TreeSet<>();
		for (String s : SELECTED) {
			expected.add(s + ".br");
			expected.add(s + ".gz");
		}
		assertEquals(expected, added(before, after), "exactly these siblings must be created");
		assertOriginalsUnchanged(before, after);
		for (String s : SELECTED) {
			assertArrayEquals(before.get(s), decodeGzip(after.get(s + ".gz")), "gzip roundtrip " + s);
			assertArrayEquals(before.get(s), decodeBrotli(war.resolve(s + ".br")), "brotli roundtrip " + s);
			assertTrue(after.get(s + ".br").length < before.get(s).length, "br strictly smaller " + s);
			assertTrue(after.get(s + ".gz").length < before.get(s).length, "gz strictly smaller " + s);
		}
		assertTrue(r.output.contains("precompressed 4 files (br: 4, gz: 4)"), "summary line: " + r.output);
	}

	@Test
	@Timeout(120)
	@DisplayName("--brotli only produces .br and no .gz")
	void brotliOnly() throws Exception {
		Map<String, byte[]> before = populate();
		assertEquals(0, run(war.toString(), "--brotli").exit);
		Set<String> expected = new TreeSet<>();
		SELECTED.forEach(s -> expected.add(s + ".br"));
		assertEquals(expected, added(before, snapshot()));
	}

	@Test
	@Timeout(120)
	@DisplayName("--gzip only produces .gz and no .br")
	void gzipOnly() throws Exception {
		Map<String, byte[]> before = populate();
		assertEquals(0, run(war.toString(), "--gzip").exit);
		Set<String> expected = new TreeSet<>();
		SELECTED.forEach(s -> expected.add(s + ".gz"));
		assertEquals(expected, added(before, snapshot()));
	}

	@Test
	@Timeout(120)
	@DisplayName("a stale .br/.gz sibling of a file that does not compress smaller is deleted")
	void staleSiblingsDeleted() throws Exception {
		populate();
		write("rand.js.br", new byte[] { 1, 2, 3 });
		write("rand.js.gz", new byte[] { 1, 2, 3 });
		assertEquals(0, run(war.toString(), "--brotli", "--gzip").exit);
		assertTrue(!Files.exists(war.resolve("rand.js.br")), "stale rand.js.br must be removed");
		assertTrue(!Files.exists(war.resolve("rand.js.gz")), "stale rand.js.gz must be removed");
		assertTrue(Files.exists(war.resolve("rand.js")), "original kept");
	}

	@Test
	@Timeout(120)
	@DisplayName("only the top-level WEB-INF is skipped: a nested locales/WEB-INF/z.js is compressed")
	void nestedWebInfProcessed() throws Exception {
		populate();
		assertEquals(0, run(war.toString(), "--gzip").exit);
		assertTrue(Files.exists(war.resolve("locales/WEB-INF/z.js.gz")), "nested WEB-INF is processed");
		assertTrue(!Files.exists(war.resolve("WEB-INF/x.js.gz")), "top-level WEB-INF is skipped");
		assertTrue(!Files.exists(war.resolve("META-INF/y.css.gz")), "top-level META-INF is skipped");
	}

	private static byte[] jsLike(int size) {
		String[] words = { "function", "return", "const", "let", "this", "value", "index", "length", "push", "map",
				"filter", "if", "else", "for", "while", "new", "Object", "Array", "String", "undefined", "null", "true",
				"false" };
		Random rnd = new Random(7);
		StringBuilder sb = new StringBuilder();
		while (sb.length() < size) {
			sb.append(words[rnd.nextInt(words.length)]).append(' ').append(words[rnd.nextInt(words.length)]).append('_')
					.append(rnd.nextInt(500)).append("(x").append(rnd.nextInt(50)).append("){");
			sb.append(words[rnd.nextInt(words.length)]).append(' ').append(rnd.nextInt(100000)).append(";}\n");
		}
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Test
	@Timeout(120)
	@DisplayName("output uses max compression: .br equals brotli q11/lgwin24/text-mode and .gz equals gzip level 9, byte for byte, and differs from low levels")
	void maxCompressionLevels() throws Exception {
		write("big.js", jsLike(60000));
		assertEquals(0, run(war.toString(), "--brotli", "--gzip").exit);
		Path ref = Files.createTempDirectory(war, "ref");
		String js = "const z=require('zlib'),f=require('fs'),c=z.constants;const b=f.readFileSync(process.argv[1]);const d=process.argv[2];"
				+ "const br=q=>z.brotliCompressSync(b,{params:{[c.BROTLI_PARAM_QUALITY]:q,[c.BROTLI_PARAM_LGWIN]:24,[c.BROTLI_PARAM_SIZE_HINT]:b.length,[c.BROTLI_PARAM_MODE]:c.BROTLI_MODE_TEXT}});"
				+ "f.writeFileSync(d+'/br11',br(11));f.writeFileSync(d+'/br1',br(1));"
				+ "f.writeFileSync(d+'/gz9',z.gzipSync(b,{level:9}));f.writeFileSync(d+'/gz1',z.gzipSync(b,{level:1}));";
		Result r = exec(List.of(node, "-e", js, war.resolve("big.js").toString(), ref.toString()));
		assertEquals(0, r.exit, r.output);
		byte[] br = Files.readAllBytes(war.resolve("big.js.br"));
		byte[] gz = Files.readAllBytes(war.resolve("big.js.gz"));
		assertArrayEquals(Files.readAllBytes(ref.resolve("br11")), br,
				"brotli must be quality 11 / lgwin 24 / text mode");
		assertArrayEquals(Files.readAllBytes(ref.resolve("gz9")), gz, "gzip must be level 9");
		assertTrue(!Arrays.equals(Files.readAllBytes(ref.resolve("br1")), br), "guard: brotli q1 must differ from q11");
		assertTrue(!Arrays.equals(Files.readAllBytes(ref.resolve("gz1")), gz),
				"guard: gzip level 1 must differ from level 9");
	}

	@Test
	@Timeout(120)
	@DisplayName("size boundary: 1023-byte file is skipped, 1024-byte file is compressed")
	void sizeBoundary() throws Exception {
		write("below.js", "a".repeat(1023).getBytes(StandardCharsets.UTF_8));
		write("exact.js", "a".repeat(1024).getBytes(StandardCharsets.UTF_8));
		assertEquals(0, run(war.toString(), "--brotli", "--gzip").exit);
		assertTrue(!Files.exists(war.resolve("below.js.br")) && !Files.exists(war.resolve("below.js.gz")),
				"1023 bytes skipped");
		assertTrue(Files.exists(war.resolve("exact.js.br")) && Files.exists(war.resolve("exact.js.gz")),
				"1024 bytes qualifies");
	}

	@Test
	@Timeout(120)
	@DisplayName("extension rule: svg/wasm/ttf get siblings; png/jpg/zip/woff do not")
	void extensionRule() throws Exception {
		String[] yes = { "i.svg", "m.wasm", "f.ttf" };
		String[] no = { "i.png", "i.jpg", "a.zip", "f.woff" };
		for (String s : yes)
			write(s, text(4000));
		for (String s : no)
			write(s, text(4000));
		assertEquals(0, run(war.toString(), "--brotli", "--gzip").exit);
		for (String s : yes) {
			assertTrue(Files.exists(war.resolve(s + ".br")), s + ".br expected");
			assertTrue(Files.exists(war.resolve(s + ".gz")), s + ".gz expected");
		}
		for (String s : no) {
			assertTrue(!Files.exists(war.resolve(s + ".br")), s + ".br not expected");
			assertTrue(!Files.exists(war.resolve(s + ".gz")), s + ".gz not expected");
		}
	}

	@Test
	@Timeout(60)
	@DisplayName("exit code 2 when no directory is given")
	void noDirectory() throws Exception {
		assertEquals(2, run("--brotli").exit);
	}

	@Test
	@Timeout(60)
	@DisplayName("exit code 2 when the directory does not exist")
	void missingDirectory() throws Exception {
		assertEquals(2, run(war.resolve("nope").toString(), "--brotli").exit);
	}

	@Test
	@Timeout(60)
	@DisplayName("exit code 2 when no format flag is given, and nothing is written")
	void noFormatFlag() throws Exception {
		Map<String, byte[]> before = populate();
		assertEquals(2, run(war.toString()).exit);
		assertEquals(before.keySet(), snapshot().keySet());
	}

	@Test
	@Timeout(60)
	@DisplayName("exit code 2 for an unknown flag")
	void unknownFlag() throws Exception {
		assertEquals(2, run(war.toString(), "--brotli", "--zstd").exit);
	}

	@Test
	@Timeout(60)
	@DisplayName("exit code 2 when the path is a file, not a directory")
	void pathIsFile() throws Exception {
		write("a.js", text(4000));
		assertEquals(2, run(war.resolve("a.js").toString(), "--gzip").exit);
	}
}
