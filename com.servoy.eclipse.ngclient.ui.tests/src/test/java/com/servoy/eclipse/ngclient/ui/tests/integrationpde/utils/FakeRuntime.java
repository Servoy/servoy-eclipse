package com.servoy.eclipse.ngclient.ui.tests.integrationpde.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.servoy.eclipse.ngclient.ui.StringOutputStream;

/**
 * Fake node / npm / pnpm executables (a .cmd on Windows, an sh script
 * elsewhere) that print their arguments and the SVY_EXTRA environment variable,
 * optionally sleep, and exit with the value of SVY_EXIT (default 0).
 */
public final class FakeRuntime {
	private FakeRuntime() {
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	/**
	 * Creates the fake executable named {@code name} in {@code dir}; when
	 * {@code sleep} the script waits a few seconds first.
	 */
	public static Path create(Path dir, String name, boolean sleep) throws IOException {
		if (isWindows()) {
			Path script = dir.resolve(name + ".cmd");
			String content = "@echo off\r\n" + "echo ARGS:%*\r\n" + "echo EXTRA:%SVY_EXTRA%\r\n"
					+ "echo DEMOTION:%pnpm_config_strict_dep_builds%\r\n"
					+ (sleep ? "ping -n 6 127.0.0.1 >nul\r\n" : "") + "if \"%SVY_EXIT%\"==\"\" exit /b 0\r\n"
					+ "exit /b %SVY_EXIT%\r\n";
			Files.writeString(script, content);
			return script;
		}
		Path script = dir.resolve(name);
		String content = "#!/bin/sh\n" + "echo \"ARGS:$*\"\n" + "echo \"EXTRA:$SVY_EXTRA\"\n"
				+ "echo \"DEMOTION:$pnpm_config_strict_dep_builds\"\n" + (sleep ? "sleep 5\n" : "")
				+ "exit ${SVY_EXIT:-0}\n";
		Files.writeString(script, content);
		script.toFile().setExecutable(true);
		return script;
	}

	/** Collects everything written to the console. */
	public static class RecordingConsole implements StringOutputStream {
		private final StringBuffer text = new StringBuffer();
		private volatile boolean closed;

		@Override
		public void write(CharSequence chars) {
			text.append(chars);
		}

		@Override
		public void close() {
			closed = true;
		}

		public String text() {
			return text.toString();
		}

		public boolean isClosed() {
			return closed;
		}
	}
}
