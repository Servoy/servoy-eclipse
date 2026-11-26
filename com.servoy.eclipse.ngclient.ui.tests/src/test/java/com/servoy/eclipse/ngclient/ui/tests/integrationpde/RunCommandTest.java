package com.servoy.eclipse.ngclient.ui.tests.integrationpde;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.RunNPMCommand;
import com.servoy.eclipse.ngclient.ui.RunPNPMCommand;
import com.servoy.eclipse.ngclient.ui.tests.integrationpde.utils.FakeRuntime;
import com.servoy.eclipse.ngclient.ui.tests.integrationpde.utils.FakeRuntime.RecordingConsole;

/**
 * Runs {@link RunPNPMCommand#runCommand} and {@link RunNPMCommand#runCommand}
 * against fake executables.
 * <p>
 * This is a PDE test because the {@code RunNPMCommand}/{@code RunPNPMCommand}
 * constructors extend {@code WorkspaceJob}, which needs the Eclipse workspace:
 * in the plain runner 14 of the 15 tests fail in {@code RunNPMCommand.<init>}
 * with "Workspace is already closed or not ready yet".
 */
class RunCommandTest {
	@TempDir
	Path root;

	private Path project;
	private Path bin;
	private RecordingConsole console;
	private IProgressMonitor monitor;

	@BeforeEach
	void setUp() throws IOException {
		project = Files.createDirectory(root.resolve("project"));
		bin = Files.createDirectory(root.resolve("bin"));
		console = new RecordingConsole();
		monitor = mock(IProgressMonitor.class);
	}

	@Nested
	class Pnpm {
		private RunPNPMCommand command(boolean sleep, List<String> args) throws IOException {
			Path pnpm = FakeRuntime.create(bin, "pnpm", sleep);
			Path node = FakeRuntime.create(bin, "node", false);
			RunPNPMCommand cmd = new RunPNPMCommand(pnpm.toFile(), node.toFile(), project.toFile(), args);
			cmd.setOutputStream(console);
			return cmd;
		}

		@Test
		void exitCodeIsMinusOneBeforeTheCommandRan() throws Exception {
			assertEquals(-1, command(false, List.of("run", "build")).getExitCode());
		}

		@Test
		void runTranslatesVerbCapturesOutputAndReturnsExitCodeZero() throws Exception {
			RunPNPMCommand cmd = command(false, List.of("run-script", "build_debug"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(0, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("ARGS:run build_debug"), console.text()),
					() -> assertTrue(console.text().contains("Finished running"), console.text()),
					() -> assertTrue(console.isClosed(), "console is closed at the end"));
		}

		@Test
		void runCommandDoesNotGetStoreOrImportMethodFlags() throws Exception {
			RunPNPMCommand cmd = command(false, List.of("run", "build"));
			cmd.runCommand(monitor);
			assertFalse(console.text().contains("--config."), console.text());
		}

		@Test
		void nonZeroExitCodeIsPropagatedAndReported() throws Exception {
			RunPNPMCommand cmd = command(false, List.of("run", "build"));
			cmd.setExtraEnvironment(Map.of("SVY_EXIT", "3"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(3, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("EXIT_CODE was NOT zero but: 3"), console.text()));
		}

		@Test
		void extraEnvironmentIsPassedToTheProcess() throws Exception {
			RunPNPMCommand cmd = command(false, List.of("run", "build"));
			cmd.setExtraEnvironment(Map.of("SVY_EXTRA", "hello-env"));
			cmd.runCommand(monitor);
			assertTrue(console.text().contains("EXTRA:hello-env"), console.text());
		}

		@Test
		void discoveryDemotionWindowSetsStrictDepBuildsFalseOnlyWhileOpen() throws Exception {
			RunPNPMCommand.beginBuildScriptDiscoveryDemotion();
			try {
				RunPNPMCommand inWindow = command(false, List.of("run", "build"));
				inWindow.runCommand(monitor);
				assertTrue(console.text().contains("DEMOTION:false"), console.text());
			} finally {
				RunPNPMCommand.endBuildScriptDiscoveryDemotion();
			}
			RecordingConsole second = new RecordingConsole();
			RunPNPMCommand afterWindow = command(false, List.of("run", "build"));
			afterWindow.setOutputStream(second);
			afterWindow.runCommand(monitor);
			assertFalse(second.text().contains("DEMOTION:false"),
					"demotion must be off after the window: " + second.text());
		}

		@Test
		void dedupIsSkippedWithoutStartingAProcess() throws Exception {
			RunPNPMCommand cmd = command(false, List.of("dedup"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(0, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("Skipping 'dedup ' in pnpm mode (no-op)."),
							console.text()),
					() -> assertNull(cmd.getProcess()));
		}

		@Test
		void alreadyCancelledMonitorSkipsTheCommand() throws Exception {
			when(monitor.isCanceled()).thenReturn(true);
			RunPNPMCommand cmd = command(false, List.of("run", "build"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(RunPNPMCommand.EXIT_CODE_CANCELLED, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("Cancel was requested; skipping command"), console.text()),
					() -> assertNull(cmd.getProcess()));
		}

		@Test
		@Timeout(60)
		void cancelWhileRunningStopsTheProcess() throws Exception {
			when(monitor.isCanceled()).thenReturn(false, true);
			RunPNPMCommand cmd = command(true, List.of("run", "build"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(RunPNPMCommand.EXIT_CODE_CANCELLED, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("Cancel requested by user"), console.text()));
		}
	}

	@Nested
	class Npm {
		private RunNPMCommand command(boolean sleep, List<String> args) throws IOException {
			Path node = FakeRuntime.create(bin, "node", sleep);
			File npm = Files.writeString(bin.resolve("npm-cli.js"), "// fake").toFile();
			RunNPMCommand cmd = new RunNPMCommand(node.toFile(), npm, project.toFile(), args);
			cmd.setOutputStream(console);
			return cmd;
		}

		@Test
		void passesTheNpmScriptAndTheArgumentsToNode() throws Exception {
			RunNPMCommand cmd = command(false, List.of("run", "build_debug"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(0, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("npm-cli.js run build_debug"), console.text()),
					() -> assertTrue(console.isClosed()));
		}

		@Test
		void nonZeroExitCodeIsPropagated() throws Exception {
			RunNPMCommand cmd = command(false, List.of("install"));
			cmd.setExtraEnvironment(Map.of("SVY_EXIT", "7"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(7, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("EXIT_CODE was NOT zero but: 7"), console.text()));
		}

		@Test
		void extraEnvironmentIsPassedToTheProcess() throws Exception {
			RunNPMCommand cmd = command(false, List.of("install"));
			cmd.setExtraEnvironment(Map.of("SVY_EXTRA", "npm-env"));
			cmd.runCommand(monitor);
			assertTrue(console.text().contains("EXTRA:npm-env"), console.text());
		}

		@Test
		void alreadyCancelledMonitorSkipsTheCommand() throws Exception {
			when(monitor.isCanceled()).thenReturn(true);
			RunNPMCommand cmd = command(false, List.of("install"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(RunNPMCommand.EXIT_CODE_CANCELLED, cmd.getExitCode()),
					() -> assertNull(cmd.getProcess()));
		}

		@Test
		@Timeout(60)
		void cancelWhileRunningStopsTheProcess() throws Exception {
			when(monitor.isCanceled()).thenReturn(false, true);
			RunNPMCommand cmd = command(true, List.of("run", "build"));
			cmd.runCommand(monitor);
			assertAll(() -> assertEquals(RunNPMCommand.EXIT_CODE_CANCELLED, cmd.getExitCode()),
					() -> assertTrue(console.text().contains("Cancel requested by user"), console.text()));
		}

		@Test
		void commandArgsToStringQuotesArgumentsWithSpaces() {
			assertEquals("run \"a b\" ", RunNPMCommand.commandArgsToString(List.of("run", "a b")));
		}
	}
}
