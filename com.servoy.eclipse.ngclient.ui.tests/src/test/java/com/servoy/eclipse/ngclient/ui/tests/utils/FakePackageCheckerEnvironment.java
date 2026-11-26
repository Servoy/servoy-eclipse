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

package com.servoy.eclipse.ngclient.ui.tests.utils;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sablo.specification.SpecProviderState;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.IRunNPMCommand;
import com.servoy.eclipse.ngclient.ui.StringOutputStream;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;

/**
 * A recording {@link PackageCheckerJob.Environment}: no Activator, no
 * workbench, no real npm/pnpm process. Commands are recorded (and answered with
 * scripted exit codes / failures), the console goes into a string, and the
 * approvals hooks only leave a marker in {@link #events}.
 */
public class FakePackageCheckerEnvironment implements PackageCheckerJob.Environment {
	public static final class RecordedCommand {
		public final File folder;
		public final List<String> args;

		RecordedCommand(File folder, List<String> args) {
			this.folder = folder;
			this.args = args;
		}

		@Override
		public String toString() {
			return String.join(" ", args);
		}
	}

	public final StringBuilder consoleText = new StringBuilder();
	public volatile boolean consoleClosed;
	public IOException closeFailure;
	public final List<RecordedCommand> commands = new CopyOnWriteArrayList<>();
	/**
	 * ordered markers: "cmd:<args>", "applyKnown", "resolve" and whatever a test
	 * adds itself
	 */
	public final List<String> events = new CopyOnWriteArrayList<>();
	public final Map<String, URL> bundleEntries = new HashMap<>();
	public final ILog log = mock(ILog.class);

	public boolean pnpmMode;
	public File node;
	public File pnpm;
	public SpecProviderState componentState = SpecMocks.emptyState();
	public SpecProviderState serviceState = SpecMocks.emptyState();
	public Function<List<String>, Integer> exitCode = args -> Integer.valueOf(0);
	public Function<List<String>, IOException> failure = args -> null;
	public Consumer<RecordedCommand> onCommand = command -> {
	};
	public IStatus approvalsResult = Status.OK_STATUS;
	public int nodeExtractionWaits;
	public int editorRefreshes;
	public String lastApprovalsSolutionName;

	private final StringOutputStream console = new StringOutputStream() {
		@Override
		public void write(CharSequence chars) throws IOException {
			synchronized (consoleText) {
				consoleText.append(chars);
			}
		}

		@Override
		public void close() throws IOException {
			consoleClosed = true;
			if (closeFailure != null)
				throw closeFailure;
		}
	};

	public String consoleText() {
		synchronized (consoleText) {
			return consoleText.toString();
		}
	}

	public List<String> commandLines() {
		return commands.stream().map(RecordedCommand::toString).toList();
	}

	/**
	 * writes a file into dir and serves it as the bundle entry for the given entry
	 * path
	 */
	public void putBundleFile(File dir, String entryPath, String content) throws IOException {
		File file = ProjectFiles.write(dir, entryPath.replace('/', '_'), content);
		bundleEntries.put(entryPath, file.toURI().toURL());
	}

	@Override
	public StringOutputStream console() {
		return console;
	}

	@Override
	public boolean isPnpmMode() {
		return pnpmMode;
	}

	@Override
	public void waitForNodeExtraction() {
		nodeExtractionWaits++;
	}

	@Override
	public File nodePath() {
		return node;
	}

	@Override
	public File pnpmPath() {
		return pnpm;
	}

	@Override
	public IRunNPMCommand createCommand(File folder, List<String> args) {
		List<String> copy = List.copyOf(args);
		return new IRunNPMCommand() {
			private int exit = -1;

			@Override
			public void runCommand(IProgressMonitor monitor) throws IOException, InterruptedException {
				RecordedCommand command = new RecordedCommand(folder, copy);
				commands.add(command);
				events.add("cmd:" + command);
				onCommand.accept(command);
				IOException failed = failure.apply(copy);
				if (failed != null)
					throw failed;
				exit = exitCode.apply(copy).intValue();
			}

			@Override
			public int getExitCode() {
				return exit;
			}

			@Override
			public void setUser(boolean b) {
			}

			@Override
			public void schedule() {
			}

			@Override
			public void join() throws InterruptedException {
			}

			@Override
			public void setExtraEnvironment(Map<String, String> unmodifiableMap) {
			}

			@Override
			public Process getProcess() {
				return null;
			}

			@Override
			public boolean cancel() {
				return false;
			}

			@Override
			public void setOutputStream(StringOutputStream outputStream) {
			}
		};
	}

	@Override
	public ILog log() {
		return log;
	}

	@Override
	public SpecProviderState specProviderState() {
		return componentState;
	}

	@Override
	public SpecProviderState serviceSpecProviderState() {
		return serviceState;
	}

	@Override
	public URL bundleEntry(String path) {
		return bundleEntries.get(path);
	}

	@Override
	public void refreshOpenEditors() {
		editorRefreshes++;
		events.add("refreshEditors");
	}

	@Override
	public void applyKnownPnpmBuildApprovals(File projectFolder, ITiNGExportModel warExportModel,
			String approvalsSolutionName, StringOutputStream out) {
		lastApprovalsSolutionName = approvalsSolutionName;
		events.add("applyKnown");
	}

	@Override
	public IStatus resolvePnpmBuildScriptApprovals(File projectFolder, ITiNGExportModel warExportModel,
			String approvalsSolutionName, IProgressMonitor monitor, StringOutputStream out) {
		events.add("resolve");
		return approvalsResult;
	}
}
