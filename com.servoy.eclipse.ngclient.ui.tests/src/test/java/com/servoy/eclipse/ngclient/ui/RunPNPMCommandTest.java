package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Tests for {@link RunPNPMCommand}'s npm-to-pnpm verb translation (SVY-21456).
 * <p>
 * The translation logic lives in the {@code private static List<String> translateToPnpmArguments(List<String>)}
 * method. There is no package-private/public seam for it, so these tests invoke it via reflection - the smallest
 * accessible seam. No processes are spawned; this is pure list-to-list logic.
 * </p>
 * <p>
 * SVY-21502 retired the SVY-21456 stopgap: {@code addUnblockDepBuildsFlag} and the unconditional
 * {@code --config.strictDepBuilds=false} appended on every package-mutating verb were removed (per-solution
 * allow/deny lists drive {@code allowBuilds} in the generated {@code pnpm-workspace.yaml} instead). These tests
 * therefore assert the verb translation no longer appends that unblock flag on ANY verb.
 * </p>
 */
class RunPNPMCommandTest
{
	/**
	 * The flag the SVY-21456 stopgap used to append to every package-mutating verb. SVY-21502 removed that stopgap,
	 * so the verb translation must NOT emit this flag anymore (asserted below).
	 */
	private static final String UNBLOCK_FLAG = "--config.strictDepBuilds=false";

	private static Method translateMethod;

	static
	{
		try
		{
			translateMethod = RunPNPMCommand.class.getDeclaredMethod("translateToPnpmArguments", List.class);
			translateMethod.setAccessible(true);
		}
		catch (NoSuchMethodException e)
		{
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> translate(List<String> commands)
	{
		try
		{
			return (List<String>)translateMethod.invoke(null, commands);
		}
		catch (IllegalAccessException | IllegalArgumentException e)
		{
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e)
		{
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new RuntimeException(cause);
		}
	}

	@Nested
	class Install
	{
		@Test
		@DisplayName("bare 'install' (no package args) maps to pnpm 'install' with no unblock flag")
		void bareInstallMapsToInstall()
		{
			List<String> result = translate(List.of("install"));
			assertIterableEquals(List.of("install"), result);
		}

		@Test
		@DisplayName("'install <pkg...> <path>' maps to pnpm 'add <pkg...> <path>' with no unblock flag")
		void installWithPackagesMapsToAdd()
		{
			List<String> result = translate(List.of("install", "pkgA", "pkgB", "./dist-public/"));
			assertIterableEquals(List.of("add", "pkgA", "pkgB", "./dist-public/"), result);
		}

		@Test
		@DisplayName("'install <singlePkg>' maps to pnpm 'add <singlePkg>' with no unblock flag")
		void installWithSinglePackageMapsToAdd()
		{
			List<String> result = translate(List.of("install", "@servoy/public"));
			assertIterableEquals(List.of("add", "@servoy/public"), result);
		}

		@Test
		@DisplayName("'add' is the verb (never 'install') whenever package args are present")
		void installWithPackagesNeverKeepsInstallVerb()
		{
			List<String> result = translate(List.of("install", "./dist-public/"));
			assertAll(
				() -> assertEquals("add", result.get(0)),
				() -> assertIterableEquals(List.of("add", "./dist-public/"), result));
		}
	}

	@Nested
	class Ci
	{
		@Test
		@DisplayName("'ci' maps to pnpm 'install --frozen-lockfile' with no unblock flag")
		void ciMapsToFrozenLockfile()
		{
			List<String> result = translate(List.of("ci"));
			assertIterableEquals(List.of("install", "--frozen-lockfile"), result);
		}

		@Test
		@DisplayName("'ci' translation ignores/keeps the frozen-lockfile flag rather than passing legacy-peer-deps")
		void ciDoesNotEmitLegacyPeerDeps()
		{
			List<String> result = translate(List.of("ci"));
			assertTrue(!result.contains("--legacy-peer-deps"));
		}
	}

	@Nested
	class Update
	{
		@Test
		@DisplayName("'update' maps to pnpm 'update' with no unblock flag")
		void updateMapsToUpdate()
		{
			List<String> result = translate(List.of("update"));
			assertIterableEquals(List.of("update"), result);
		}

		@Test
		@DisplayName("'update <pkg>' preserves trailing package args with no unblock flag")
		void updateKeepsPackageArgs()
		{
			List<String> result = translate(List.of("update", "@servoy/public"));
			assertIterableEquals(List.of("update", "@servoy/public"), result);
		}
	}

	@Nested
	class Uninstall
	{
		@Test
		@DisplayName("'uninstall <pkg>' maps to pnpm 'remove <pkg>' with no unblock flag")
		void uninstallMapsToRemove()
		{
			List<String> result = translate(List.of("uninstall", "@servoy/public"));
			assertIterableEquals(List.of("remove", "@servoy/public"), result);
		}

		@Test
		@DisplayName("'uninstall' preserves multiple package args with no unblock flag")
		void uninstallKeepsMultiplePackages()
		{
			List<String> result = translate(List.of("uninstall", "pkgA", "pkgB"));
			assertIterableEquals(List.of("remove", "pkgA", "pkgB"), result);
		}
	}

	@Nested
	class Run
	{
		@Test
		@DisplayName("'run <script>' maps to pnpm 'run <script>'")
		void runMapsToRun()
		{
			List<String> result = translate(List.of("run", "build_debug_nowatch"));
			assertIterableEquals(List.of("run", "build_debug_nowatch"), result);
		}

		@Test
		@DisplayName("'run-script <script>' maps to pnpm 'run <script>'")
		void runScriptMapsToRun()
		{
			List<String> result = translate(List.of("run-script", "build_debug"));
			assertIterableEquals(List.of("run", "build_debug"), result);
		}

		@Test
		@DisplayName("'run <script> <extraArgs...>' preserves the trailing args after the script")
		void runKeepsExtraArgs()
		{
			List<String> result = translate(List.of("run", "build", "--configuration", "production"));
			assertIterableEquals(List.of("run", "build", "--configuration", "production"), result);
		}
	}

	@Nested
	class Dedup
	{
		@Test
		@DisplayName("'dedup' is a no-op: returns null so the runner skips spawning a process")
		void dedupReturnsNull()
		{
			assertNull(translate(List.of("dedup")));
		}

		@Test
		@DisplayName("'dedup' with stray trailing args is still a no-op")
		void dedupWithArgsReturnsNull()
		{
			assertNull(translate(List.of("dedup", "somepkg")));
		}
	}

	@Nested
	class EmptyAndNull
	{
		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("null or empty input translates to an empty (non-null) list")
		void nullOrEmptyInputYieldsEmptyList(List<String> input)
		{
			List<String> result = translate(input);
			assertAll(
				() -> assertTrue(result != null, "result should not be null for null/empty input"),
				() -> assertTrue(result.isEmpty(), "result should be empty for null/empty input"));
		}
	}

	@Nested
	class NoUnblockFlag
	{
		@Test
		@DisplayName("SVY-21502: no package-mutating verb appends --config.strictDepBuilds=false anymore")
		void noVerbEmitsUnblockFlag()
		{
			assertAll(
				() -> assertFalse(translate(List.of("install")).contains(UNBLOCK_FLAG), "install"),
				() -> assertFalse(translate(List.of("install", "pkgA", "./dist-public/")).contains(UNBLOCK_FLAG), "add"),
				() -> assertFalse(translate(List.of("ci")).contains(UNBLOCK_FLAG), "ci"),
				() -> assertFalse(translate(List.of("update")).contains(UNBLOCK_FLAG), "update"),
				() -> assertFalse(translate(List.of("uninstall", "@servoy/public")).contains(UNBLOCK_FLAG), "remove"),
				() -> assertFalse(translate(List.of("run", "build_debug")).contains(UNBLOCK_FLAG), "run"));
		}
	}

	@Nested
	class Passthrough
	{
		@Test
		@DisplayName("an unrecognized verb is passed through unchanged (verb + args)")
		void unknownVerbPassthrough()
		{
			List<String> result = translate(List.of("audit", "--fix"));
			assertIterableEquals(List.of("audit", "--fix"), result);
		}

		@Test
		@DisplayName("a bare unrecognized verb is passed through unchanged")
		void bareUnknownVerbPassthrough()
		{
			List<String> result = translate(List.of("outdated"));
			assertIterableEquals(List.of("outdated"), result);
		}
	}

	@Nested
	class Immutability
	{
		@Test
		@DisplayName("translation does not mutate the input list")
		void doesNotMutateInput()
		{
			List<String> input = new ArrayList<>(Arrays.asList("install", "pkgA", "./dist-public/"));
			List<String> snapshot = new ArrayList<>(input);
			translate(input);
			assertIterableEquals(snapshot, input);
		}
	}

	static java.util.stream.Stream<Arguments> verbTranslations()
	{
		// SVY-21502: no verb appends the strictDepBuilds unblock flag anymore; the generated pnpm-workspace.yaml's
		// allowBuilds map (from the committed allow/deny lists) is what controls dependency build scripts now.
		return java.util.stream.Stream.of(
			Arguments.of(List.of("install"), List.of("install")),
			Arguments.of(List.of("install", "pkgA", "./dist-public/"), List.of("add", "pkgA", "./dist-public/")),
			Arguments.of(List.of("ci"), List.of("install", "--frozen-lockfile")),
			Arguments.of(List.of("update"), List.of("update")),
			Arguments.of(List.of("uninstall", "@servoy/public"), List.of("remove", "@servoy/public")),
			Arguments.of(List.of("run", "build_debug_nowatch"), List.of("run", "build_debug_nowatch")),
			Arguments.of(List.of("run-script", "build_debug"), List.of("run", "build_debug")));
	}

	@ParameterizedTest
	@MethodSource("verbTranslations")
	@DisplayName("verb translation table maps each npm command to its pnpm equivalent")
	void verbTranslationTable(List<String> input, List<String> expected)
	{
		assertIterableEquals(expected, translate(input));
	}

	/**
	 * Tests for the cross-volume store-dir detection (SVY-21456 §3.7.2).
	 * <p>
	 * pnpm hardlinks package files from its global store, and hardlinks cannot cross filesystem volumes. When the
	 * install target is on a different volume than pnpm's default store (on Windows the default store is under the
	 * user home = C:, so any non-C: workspace hits this), the runner must point the store at a generic per-drive
	 * store on the target's own volume so hardlinks work; otherwise it must leave pnpm's default store alone.
	 * <p>
	 * The decision method {@code private static String computeStoreDirArgIfCrossVolume(File)} takes the target
	 * folder and reads pnpm's default store location from the environment, so it is not fully hermetic. These tests
	 * therefore exercise the two pure, deterministic helpers it is built from -
	 * {@code getVolumeRoot(File)} and {@code getFileStoreOfNearestExisting(File)} - via reflection, which is where
	 * all the volume logic actually lives. No processes are spawned.
	 */
	@Nested
	class StoreDirCrossVolume
	{
		private static Method volumeRootMethod;
		private static Method fileStoreMethod;

		static
		{
			try
			{
				volumeRootMethod = RunPNPMCommand.class.getDeclaredMethod("getVolumeRoot", java.io.File.class);
				volumeRootMethod.setAccessible(true);
				fileStoreMethod = RunPNPMCommand.class.getDeclaredMethod("getFileStoreOfNearestExisting", java.io.File.class);
				fileStoreMethod.setAccessible(true);
			}
			catch (NoSuchMethodException e)
			{
				throw new ExceptionInInitializerError(e);
			}
		}

		private static java.io.File volumeRoot(java.io.File f)
		{
			try
			{
				return (java.io.File)volumeRootMethod.invoke(null, f);
			}
			catch (IllegalAccessException | IllegalArgumentException e)
			{
				throw new RuntimeException(e);
			}
			catch (InvocationTargetException e)
			{
				Throwable c = e.getCause();
				if (c instanceof RuntimeException re) throw re;
				if (c instanceof Error err) throw err;
				throw new RuntimeException(c);
			}
		}

		private static java.nio.file.FileStore fileStore(java.io.File f)
		{
			try
			{
				return (java.nio.file.FileStore)fileStoreMethod.invoke(null, f);
			}
			catch (IllegalAccessException | IllegalArgumentException e)
			{
				throw new RuntimeException(e);
			}
			catch (InvocationTargetException e)
			{
				Throwable c = e.getCause();
				if (c instanceof RuntimeException re) throw re;
				if (c instanceof Error err) throw err;
				throw new RuntimeException(c);
			}
		}

		@Test
		@DisplayName("getVolumeRoot returns the filesystem root for an absolute path")
		void volumeRootReturnsRoot()
		{
			java.io.File cwd = new java.io.File(".").getAbsoluteFile();
			java.io.File expectedRoot = cwd.toPath().getRoot().toFile();
			assertEquals(expectedRoot, volumeRoot(cwd));
		}

		@Test
		@DisplayName("getVolumeRoot returns the same root regardless of how deep the path is")
		void volumeRootStableAcrossDepth()
		{
			java.io.File shallow = new java.io.File(".").getAbsoluteFile();
			java.io.File deep = new java.io.File(shallow, "a/b/c/d/e/does/not/exist");
			assertEquals(volumeRoot(shallow), volumeRoot(deep));
		}

		@Test
		@DisplayName("getVolumeRoot returns null for a null file")
		void volumeRootNullForNull()
		{
			assertNull(volumeRoot(null));
		}

		@Test
		@DisplayName("getFileStoreOfNearestExisting resolves the volume even when the leaf path does not exist yet")
		void fileStoreWalksUpToExistingAncestor()
		{
			// the target folder does not exist at first-run time; the method must walk up to an existing ancestor
			java.io.File existing = new java.io.File(".").getAbsoluteFile();
			java.io.File nonExistentLeaf = new java.io.File(existing, "no/such/target/Test");
			java.nio.file.FileStore fromLeaf = fileStore(nonExistentLeaf);
			java.nio.file.FileStore fromExisting = fileStore(existing);
			assertAll(
				() -> assertTrue(fromLeaf != null, "should resolve a FileStore via an existing ancestor"),
				() -> assertEquals(fromExisting, fromLeaf, "leaf and ancestor resolve to the same volume"));
		}

		@Test
		@DisplayName("two paths on the same volume resolve to the same FileStore (no store override needed)")
		void sameVolumeSameFileStore()
		{
			java.io.File a = new java.io.File(".").getAbsoluteFile();
			java.io.File b = new java.io.File(System.getProperty("java.io.tmpdir"));
			// both the project dir and the temp dir are, in the test environment, on the same volume
			if (volumeRoot(a).equals(volumeRoot(b)))
			{
				assertEquals(fileStore(a), fileStore(b));
			}
		}
	}
}
