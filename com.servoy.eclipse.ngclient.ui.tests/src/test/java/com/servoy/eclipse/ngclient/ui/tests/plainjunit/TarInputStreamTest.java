package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives.TarItem;
import com.servoy.eclipse.ngclient.ui.utils.TarEntry;
import com.servoy.eclipse.ngclient.ui.utils.TarException;
import com.servoy.eclipse.ngclient.ui.utils.TarInputStream;

class TarInputStreamTest {
	private static TarInputStream open(byte[] tar) throws TarException, IOException {
		return new TarInputStream(new ByteArrayInputStream(tar));
	}

	private static String readAll(TarInputStream in) throws IOException {
		return new String(in.readNBytes(1 << 20), StandardCharsets.UTF_8);
	}

	@Nested
	class Entries {
		@Test
		void regularFileHasNameModeSizeTimeAndType() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.file("dir/hello.txt", "hello world", 0755)))) {
				TarEntry entry = in.getNextEntry();
				assertAll(() -> assertEquals("dir/hello.txt", entry.getName()),
						() -> assertEquals(0755, entry.getMode()), () -> assertEquals(11, entry.getSize()),
						// the header mtime is not parsed: the time is the creation time of the
						// entry in seconds (current behaviour)
						() -> assertTrue(Math.abs(entry.getTime() - System.currentTimeMillis() / 1000) < 60),
						() -> assertEquals(TarEntry.FILE, entry.getFileType()), () -> assertFalse(entry.isDirectory()),
						() -> assertFalse(entry.isLink()), () -> assertFalse(entry.isSymbolicLink()));
				assertEquals("hello world", readAll(in));
			}
		}

		@Test
		void directoryEntryIsReportedAsDirectoryWithZeroSize() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.directory("some/dir/")))) {
				TarEntry entry = in.getNextEntry();
				assertAll(() -> assertEquals("some/dir/", entry.getName()), () -> assertTrue(entry.isDirectory()),
						() -> assertEquals(0, entry.getSize()));
				assertEquals(-1, in.read());
			}
		}

		@Test
		void symbolicLinkCarriesItsTarget() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.symlink("bin/pnpm", "../dist/pnpm.cjs")))) {
				TarEntry entry = in.getNextEntry();
				assertAll(() -> assertTrue(entry.isSymbolicLink()),
						() -> assertEquals("../dist/pnpm.cjs", entry.getLinkName()));
			}
		}

		@Test
		void ustarPrefixIsPrependedToTheName() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.withPrefix("very/long/prefix", "file.txt", "x")))) {
				assertEquals("very/long/prefix/file.txt", in.getNextEntry().getName());
			}
		}

		@Test
		void gnuLongNameReplacesTheNameOfTheFollowingEntry() throws Exception {
			String longName = "a/".repeat(80) + "file.txt";
			try (TarInputStream in = open(
					TestArchives.tar(TarItem.gnuLongName(longName), TarItem.file("truncated", "content")))) {
				TarEntry entry = in.getNextEntry();
				assertEquals(longName, entry.getName());
				assertEquals("content", readAll(in));
			}
		}

		@Test
		void entriesAreReturnedInOrderAndEndOfArchiveYieldsNull() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.file("one", "1"), TarItem.file("two", "22")))) {
				assertEquals("one", in.getNextEntry().getName());
				assertEquals("two", in.getNextEntry().getName());
				assertNull(in.getNextEntry());
			}
		}
	}

	@Nested
	class Reading {
		@Test
		void contentOfEachEntryIsSeparatedAcrossBlockPadding() throws Exception {
			String first = "a".repeat(700); // not a multiple of 512, so padded
			String second = "b".repeat(512); // exact block, no padding
			String third = "c";
			try (TarInputStream in = open(
					TestArchives.tar(TarItem.file("1", first), TarItem.file("2", second), TarItem.file("3", third)))) {
				assertEquals("1", in.getNextEntry().getName());
				assertEquals(first, readAll(in));
				assertEquals("2", in.getNextEntry().getName());
				assertEquals(second, readAll(in));
				assertEquals("3", in.getNextEntry().getName());
				assertEquals(third, readAll(in));
			}
		}

		@Test
		void unreadContentIsSkippedWhenMovingToTheNextEntry() throws Exception {
			try (TarInputStream in = open(
					TestArchives.tar(TarItem.file("big", "x".repeat(1300)), TarItem.file("next", "after")))) {
				assertEquals("big", in.getNextEntry().getName());
				in.read(new byte[10], 0, 10); // consume only a part
				assertEquals("next", in.getNextEntry().getName());
				assertEquals("after", readAll(in));
			}
		}

		@Test
		void readReturnsMinusOneAtTheEndOfAnEntryAndForZeroSizeEntries() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.file("empty", ""), TarItem.file("one", "z")))) {
				in.getNextEntry();
				assertEquals(-1, in.read());
				assertEquals(-1, in.read(new byte[4], 0, 4));
				in.getNextEntry();
				assertEquals('z', in.read());
				assertEquals(-1, in.read());
			}
		}

		@Test
		void bulkReadIsLimitedToTheCurrentEntry() throws Exception {
			try (TarInputStream in = open(TestArchives.tar(TarItem.file("a", "abc"), TarItem.file("b", "defgh")))) {
				in.getNextEntry();
				byte[] buffer = new byte[100];
				int read = in.read(buffer, 0, buffer.length);
				assertEquals(3, read);
				assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Arrays.copyOf(buffer, read));
			}
		}
	}

	@Nested
	class InvalidInput {
		@Test
		void emptyArchiveIsNotInTarFormat() {
			TarException e = assertThrows(TarException.class, () -> open(new byte[1024]));
			assertEquals("not in tar format", e.getMessage());
		}

		@Test
		void corruptedHeaderChecksumIsRejected() {
			byte[] tar = TestArchives.tar(TarItem.file("file", "data"));
			tar[0] = 'X'; // change the name, checksum no longer matches
			TarException e = assertThrows(TarException.class, () -> open(tar));
			assertEquals("not in tar format", e.getMessage());
		}

		@Test
		void truncatedHeaderIsAnEarlyEndOfStream() {
			byte[] tar = Arrays.copyOf(TestArchives.tar(TarItem.file("file", "data")), 100);
			IOException e = assertThrows(IOException.class, () -> open(tar));
			assertEquals("early end of stream", e.getMessage());
		}

		@Test
		void archiveWithoutEndMarkerFailsOnTheNextEntry() throws Exception {
			byte[] full = TestArchives.tar(TarItem.file("file", "data"));
			byte[] noEnd = Arrays.copyOf(full, 1024); // header + one data block, no zero blocks
			try (TarInputStream in = open(noEnd)) {
				assertNotNull(in.getNextEntry());
				assertThrows(IOException.class, in::getNextEntry);
			}
		}

		@Test
		void invalidModeFieldIsATarException() {
			byte[] header = TestArchives.header(TarItem.file("file", "data"));
			header[100] = '9'; // not an octal digit
			header[101] = '9';
			// recompute the checksum so only the mode is invalid
			for (int i = 148; i < 156; i++) {
				header[i] = ' ';
			}
			long sum = 0;
			for (byte b : header) {
				sum += b & 0xff;
			}
			byte[] checksum = String.format("%06o", sum).getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(checksum, 0, header, 148, 6);
			header[154] = 0;
			byte[] tar = new byte[header.length + 1024];
			System.arraycopy(header, 0, tar, 0, header.length);
			TarException e = assertThrows(TarException.class, () -> open(tar));
			assertEquals("Not a valid tar format", e.getMessage());
		}
	}

	@Nested
	class TarEntryBehaviour {
		@Test
		void newEntryHasDefaultsAndSettersChangeThem() {
			TarEntry entry = new TarEntry("name");
			assertAll(() -> assertEquals("name", entry.getName()), () -> assertEquals(0644, entry.getMode()),
					() -> assertEquals(TarEntry.FILE, entry.getFileType()), () -> assertEquals(0, entry.getSize()));

			entry.setFileType(TarEntry.LINK);
			entry.setMode(0700);
			entry.setSize(42);
			entry.setTime(5);
			entry.setLinkName("target");
			assertAll(() -> assertTrue(entry.isLink()), () -> assertFalse(entry.isSymbolicLink()),
					() -> assertEquals(0700, entry.getMode()), () -> assertEquals(42, entry.getSize()),
					() -> assertEquals(5, entry.getTime()), () -> assertEquals("target", entry.getLinkName()));
			entry.setFileType(TarEntry.DIRECTORY);
			assertTrue(entry.isDirectory());
		}
	}
}
