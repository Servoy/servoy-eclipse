package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.DirectorySync;

class DirectorySyncTest {
	@TempDir
	Path root;

	private DirectorySync sync;

	@AfterEach
	void destroySync() {
		if (sync != null)
			sync.destroy();
	}

	private static boolean waitFor(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + 20_000_000_000L;
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean())
				return true;
			Thread.sleep(25);
		}
		return condition.getAsBoolean();
	}

	private static boolean hasContent(Path file, String content) {
		try {
			return Files.exists(file) && Files.readString(file).equals(content);
		} catch (IOException e) {
			return false; // being written
		}
	}

	@Nested
	class StaticHelpers {
		@Test
		void createFolderCreatesMissingParents() {
			File folder = root.resolve("a/b/c").toFile();
			DirectorySync.createFolder(folder);
			assertTrue(folder.isDirectory());
		}

		@Test
		void createFolderKeepsAnExistingFolderAndItsContent() throws Exception {
			Path folder = Files.createDirectory(root.resolve("exists"));
			Files.writeString(folder.resolve("f.txt"), "keep");
			DirectorySync.createFolder(folder.toFile());
			assertEquals("keep", Files.readString(folder.resolve("f.txt")));
		}

		@Test
		void copyOrCreateFileCreatesTheFileAndItsParentFolders() throws Exception {
			DirectorySync.copyOrCreateFile("sub/dir/new.txt", root.toFile(),
					new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
			assertEquals("hello", Files.readString(root.resolve("sub/dir/new.txt")));
		}

		@Test
		void copyOrCreateFileOverwritesAnExistingFile() throws Exception {
			Files.writeString(root.resolve("f.txt"), "old content that is longer");
			DirectorySync.copyOrCreateFile("f.txt", root.toFile(),
					new ByteArrayInputStream("new".getBytes(StandardCharsets.UTF_8)));
			assertEquals("new", Files.readString(root.resolve("f.txt")));
		}
	}

	@Nested
	class Watcher {
		@Test
		@Timeout(60)
		void createdFileIsCopiedToTheTarget() throws Exception {
			Path src = Files.createDirectory(root.resolve("src"));
			Path target = Files.createDirectory(root.resolve("target"));
			sync = new DirectorySync(src.toFile(), target.toFile(), null);

			Files.writeString(src.resolve("a.txt"), "first");

			assertTrue(waitFor(() -> hasContent(target.resolve("a.txt"), "first")), "created file must be copied");
		}

		@Test
		@Timeout(60)
		void modifiedFileInASubFolderIsCopiedAgain() throws Exception {
			Path src = Files.createDirectories(root.resolve("src/sub"));
			Path target = Files.createDirectory(root.resolve("target"));
			Files.writeString(src.resolve("f.txt"), "v1");
			sync = new DirectorySync(root.resolve("src").toFile(), target.toFile(), null);

			Files.writeString(src.resolve("f.txt"), "v2-changed");

			assertTrue(waitFor(() -> hasContent(target.resolve("sub/f.txt"), "v2-changed")),
					"modified file must be copied");
		}

		@Test
		@Timeout(60)
		void newDirectoryWithContentIsCopiedAndThenWatched() throws Exception {
			Path src = Files.createDirectory(root.resolve("src"));
			Path target = Files.createDirectory(root.resolve("target"));
			sync = new DirectorySync(src.toFile(), target.toFile(), null);

			Path newDir = Files.createDirectory(src.resolve("newdir"));
			Files.writeString(newDir.resolve("inner.txt"), "inner");
			assertTrue(waitFor(() -> hasContent(target.resolve("newdir/inner.txt"), "inner")),
					"new directory is copied");

			Files.writeString(newDir.resolve("later.txt"), "later");
			assertTrue(waitFor(() -> hasContent(target.resolve("newdir/later.txt"), "later")),
					"files added to the new directory are watched");
		}

		@Test
		@Timeout(60)
		void deletedSourceFileIsCurrentlyNotRemovedFromTheTarget() throws Exception {
			Path src = Files.createDirectory(root.resolve("src"));
			Path target = Files.createDirectory(root.resolve("target"));
			Files.writeString(src.resolve("gone.txt"), "x");
			Files.writeString(target.resolve("gone.txt"), "x");
			Files.writeString(target.resolve("stays.txt"), "y");
			sync = new DirectorySync(src.toFile(), target.toFile(), null);

			// KNOWN DEFECT (reported, not fixed here):
			// the ENTRY_DELETE branch of DirectorySync only deletes the target when
			// "filename.toFile().exists()" is true, but that is the SOURCE path, which is
			// already gone when the event arrives. Deletions are therefore never
			// propagated to the target. This test pins what the code does today and must
			// be flipped (target file removed, "stays.txt" kept) when that is fixed.
			Files.delete(src.resolve("gone.txt"));

			Thread.sleep(500);
			assertTrue(Files.exists(target.resolve("gone.txt")));
			assertTrue(Files.exists(target.resolve("stays.txt")));
		}

		@Test
		@Timeout(60)
		void ignoredFolderIsNotWatched() throws Exception {
			Path src = Files.createDirectory(root.resolve("src"));
			Path ignored = Files.createDirectory(src.resolve("node_modules"));
			Path watched = Files.createDirectory(src.resolve("app"));
			Path target = Files.createDirectory(root.resolve("target"));
			sync = new DirectorySync(src.toFile(), target.toFile(), name -> name.contains("node_modules"));

			Files.writeString(ignored.resolve("ignored.txt"), "no");
			Files.writeString(watched.resolve("seen.txt"), "yes");

			assertTrue(waitFor(() -> hasContent(target.resolve("app/seen.txt"), "yes")),
					"the not ignored folder is synced");
			assertFalse(Files.exists(target.resolve("node_modules/ignored.txt")), "the ignored folder is not synced");
		}

		@Test
		@Timeout(60)
		void afterDestroyNoMoreFilesAreCopied() throws Exception {
			Path src = Files.createDirectory(root.resolve("src"));
			Path target = Files.createDirectory(root.resolve("target"));
			sync = new DirectorySync(src.toFile(), target.toFile(), null);
			Files.writeString(src.resolve("before.txt"), "before");
			assertTrue(waitFor(() -> hasContent(target.resolve("before.txt"), "before")));

			sync.destroy();
			Thread.sleep(300); // let the watcher thread observe the destroy flag
			Files.writeString(src.resolve("after.txt"), "after");
			Thread.sleep(500);

			assertFalse(Files.exists(target.resolve("after.txt")), "a destroyed sync must not copy anymore");
		}
	}
}
