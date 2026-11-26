package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.RunPNPMCommand;

/**
 * The store-dir / import-method decisions of {@link RunPNPMCommand}
 * (SVY-21456), driven with an explicit default store so no pnpm runs.
 */
class RunPNPMCommandVolumeTest {
	private static final String HARDLINK = "--config.package-import-method=hardlink";

	@TempDir
	Path root;

	@Nested
	class StoreDirArg {
		@Test
		void sameVolumeNeedsNoOverride() throws Exception {
			File target = Files.createDirectory(root.resolve("workspace")).toFile();
			File store = Files.createDirectory(root.resolve("store")).toFile();
			assertNull(RunPNPMCommand.computeStoreDirArgIfCrossVolume(target, store));
		}

		@Test
		void notYetExistingTargetOnTheSameVolumeNeedsNoOverride() throws Exception {
			File target = new File(root.toFile(), "not/yet/created");
			File store = Files.createDirectory(root.resolve("store")).toFile();
			assertNull(RunPNPMCommand.computeStoreDirArgIfCrossVolume(target, store));
		}

		@Test
		void unknownTargetOrStoreMeansNoOverride() {
			assertNull(RunPNPMCommand.computeStoreDirArgIfCrossVolume(null, root.toFile()));
			assertNull(RunPNPMCommand.computeStoreDirArgIfCrossVolume(root.toFile(), null));
		}
	}

	@Nested
	class HardlinkArg {
		@Test
		void aPerDriveStoreOverrideAlwaysAllowsHardlinks() {
			assertEquals(HARDLINK, RunPNPMCommand.computeHardlinkImportMethodArgIfSameVolume(root.toFile(),
					"--config.store-dir=X:/.pnpm-store", null));
		}

		@Test
		void defaultStoreOnTheSameVolumeAllowsHardlinks() throws Exception {
			File store = Files.createDirectory(root.resolve("store")).toFile();
			assertEquals(HARDLINK,
					RunPNPMCommand.computeHardlinkImportMethodArgIfSameVolume(root.toFile(), null, store));
		}

		@Test
		void undeterminedStoreKeepsPnpmsDefault() {
			assertNull(RunPNPMCommand.computeHardlinkImportMethodArgIfSameVolume(root.toFile(), null, null));
			assertNull(RunPNPMCommand.computeHardlinkImportMethodArgIfSameVolume(null, null, root.toFile()));
		}
	}

	@Nested
	class DefaultStore {
		@Test
		void defaultStoreDirIsBelowTheUserHome() {
			File store = RunPNPMCommand.getDefaultPnpmStoreDir();
			assertNotNull(store);
			assertTrue(store.getAbsolutePath().startsWith(new File(System.getProperty("user.home")).getAbsolutePath()),
					store.toString());
		}
	}
}
