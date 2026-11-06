package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener;

/**
 * Tests for the pnpm runtime resolution rules of {@link WebPackagesListener} (SVY-21734): a configured
 * {@code servoy.pnpmPath} wins over the bundled pnpm, and the Titanium NG build fails loud (instead of letting
 * {@code NoOpNPMCommand} silently "succeed") when node/pnpm is missing or a web-package tarball could not be created.
 * The three rules are public statics on {@link WebPackagesListener} so no OSGi/Activator context is needed.
 */
class PnpmRuntimeResolutionTest
{
	private File tempDir;
	private File node;
	private File pnpm;

	@BeforeEach
	void createRuntimeFiles() throws IOException
	{
		tempDir = Files.createTempDirectory("pnpm-runtime-test").toFile();
		node = new File(tempDir, "node.exe");
		pnpm = new File(tempDir, "pnpm.exe");
		Files.writeString(node.toPath(), "node");
		Files.writeString(pnpm.toPath(), "pnpm");
	}

	@AfterEach
	void deleteRuntimeFiles() throws IOException
	{
		try (Stream<Path> paths = Files.walk(tempDir.toPath()))
		{
			paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	@Nested
	class PreferConfiguredPnpm
	{
		@Test
		@DisplayName("a configured servoy.pnpmPath overrides the bundled pnpm")
		void configuredPathWinsOverBundled()
		{
			File bundled = new File(tempDir, "bundled/pnpm.exe");
			assertEquals(new File("custom/pnpm.exe"), WebPackagesListener.preferConfiguredPnpm("custom/pnpm.exe", bundled));
		}

		@Test
		@DisplayName("a configured servoy.pnpmPath is used when there is no bundled pnpm (source checkout)")
		void configuredPathUsedWithoutBundled()
		{
			assertEquals(new File("custom/pnpm.exe"), WebPackagesListener.preferConfiguredPnpm("custom/pnpm.exe", null));
		}

		@Test
		@DisplayName("without a configured path the bundled pnpm is kept as is")
		void bundledKeptWithoutConfiguredPath()
		{
			assertSame(pnpm, WebPackagesListener.preferConfiguredPnpm(null, pnpm));
		}

		@Test
		@DisplayName("without a configured path and without a bundled pnpm the result is null")
		void nothingConfiguredNothingBundled()
		{
			assertNull(WebPackagesListener.preferConfiguredPnpm(null, null));
		}
	}

	@Nested
	class MissingPnpmRuntimeMessage
	{
		@Test
		@DisplayName("existing node and pnpm files give no error")
		void bothPresent()
		{
			assertNull(WebPackagesListener.missingPnpmRuntimeMessage(node, pnpm));
		}

		@Test
		@DisplayName("a null pnpm gives an error naming pnpm as missing and the settings to check")
		void pnpmNull()
		{
			String message = WebPackagesListener.missingPnpmRuntimeMessage(node, null);
			assertNotNull(message);
			assertAll(() -> assertTrue(message.contains("pnpm (null)"), message),
				() -> assertTrue(message.contains("servoy.pnpmPath"), message),
				() -> assertTrue(message.contains("aborted"), message));
		}

		@Test
		@DisplayName("a null node gives an error naming node as missing")
		void nodeNull()
		{
			String message = WebPackagesListener.missingPnpmRuntimeMessage(null, pnpm);
			assertNotNull(message);
			assertTrue(message.contains("node (null)"), message);
		}

		@Test
		@DisplayName("a pnpm path that does not exist on disk gives an error containing that path")
		void pnpmPathDoesNotExist()
		{
			File missing = new File(tempDir, "does-not-exist/pnpm.exe");
			String message = WebPackagesListener.missingPnpmRuntimeMessage(node, missing);
			assertNotNull(message);
			assertTrue(message.contains(missing.toString()), message);
		}

		@Test
		@DisplayName("a directory in place of the pnpm executable gives an error containing that path")
		void pnpmIsDirectory()
		{
			String message = WebPackagesListener.missingPnpmRuntimeMessage(node, tempDir);
			assertNotNull(message);
			assertTrue(message.contains(tempDir.toString()), message);
		}

		@Test
		@DisplayName("a node path that does not exist on disk gives an error containing that path")
		void nodePathDoesNotExist()
		{
			File missing = new File(tempDir, "does-not-exist/node.exe");
			String message = WebPackagesListener.missingPnpmRuntimeMessage(missing, pnpm);
			assertNotNull(message);
			assertTrue(message.contains(missing.toString()), message);
		}

		@Test
		@DisplayName("when both node and pnpm are null the error names both")
		void bothNull()
		{
			String message = WebPackagesListener.missingPnpmRuntimeMessage(null, null);
			assertNotNull(message);
			assertAll(() -> assertTrue(message.contains("node (null)"), message),
				() -> assertTrue(message.contains("pnpm (null)"), message));
		}
	}

	@Nested
	class PackFailuresMessage
	{
		@Test
		@DisplayName("null and empty failure lists give no error")
		void noFailures()
		{
			assertAll(() -> assertNull(WebPackagesListener.packFailuresMessage(null)),
				() -> assertNull(WebPackagesListener.packFailuresMessage(List.of())));
		}

		@Test
		@DisplayName("failed packages are listed in the error and the build is reported as aborted")
		void failuresListed()
		{
			String message = WebPackagesListener.packFailuresMessage(List.of("@servoy/12grid", "@servoy/nggrids"));
			assertNotNull(message);
			assertAll(() -> assertTrue(message.contains("@servoy/12grid"), message),
				() -> assertTrue(message.contains("@servoy/nggrids"), message),
				() -> assertTrue(message.contains("aborted"), message));
		}

		@Test
		@DisplayName("a missing shipped public tarball is reported by its package name")
		void publicTarballFailure()
		{
			String message = WebPackagesListener.packFailuresMessage(Set.of("@servoy/public"));
			assertNotNull(message);
			assertTrue(message.contains("@servoy/public"), message);
		}
	}
}
