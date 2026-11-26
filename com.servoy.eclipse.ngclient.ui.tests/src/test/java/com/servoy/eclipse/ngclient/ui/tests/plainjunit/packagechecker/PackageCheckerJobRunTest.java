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

package com.servoy.eclipse.ngclient.ui.tests.plainjunit.packagechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.GeneratedProject;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;
import com.servoy.eclipse.ngclient.ui.tests.utils.SpecMocks;

/**
 * SVY-21834 - drives PackageCheckerJob.run() end to end with a recording fake
 * environment (no npm/pnpm process, no workbench): the command sequence and
 * status of every build scenario (SVY-21502 / SVY-21734).
 */
class PackageCheckerJobRunTest {
	private static final String TARBALL_NAME = "servoy-public-1.2.3.tgz";
	// without the "./" prefix on purpose: see the KNOWN DEFECT comment in
	// PackageCheckerJobInstallBlockTest
	private static final String PNPM_PUBLIC_SPECIFIER = ".tarballs/" + TARBALL_NAME;
	private static final String SERVICE_JSON = "{\"packageName\":\"@test/svc\",\"serviceName\":\"SvcA\",\"entryPoint\":\"projects/svc\"}";
	private static final String DEV_BUILD = "run build_debug_nowatch";

	@TempDir
	File tmp;

	private File projectFolder;
	private File bundle;
	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();

	@BeforeEach
	void createFolders() {
		// the body of run() is skipped (CANCEL) while
		// Activator.isNodeExtractionAndTitaniumBuildDisabled(): a test must never pass
		// silently because of that, so this fails loudly instead (the flag is
		// ModelUtils.isTestRunning() at class load)
		assertFalse(Activator.isNodeExtractionAndTitaniumBuildDisabled(),
				"PackageCheckerJob.run() is switched off by the node extraction / titanium build guard; these tests cannot exercise it");
		projectFolder = new File(tmp, "proj");
		bundle = new File(tmp, "bundle");
		assertTrue(projectFolder.mkdirs());
		assertTrue(bundle.mkdirs());
	}

	private IStatus run(ITiNGExportModel war, String approvalsSolutionName) throws InterruptedException {
		PackageCheckerJob job = new PackageCheckerJob("test", projectFolder, war, approvalsSolutionName, env);
		job.schedule();
		job.join();
		IStatus result = job.getResult();
		assertNotNull(result);
		if (result.getException() != null)
			throw new AssertionError("the job crashed: " + result.getMessage(), result.getException());
		return result;
	}

	private void serviceInSpecs() {
		env.serviceState = SpecMocks.state(Map.of(), Map.of(),
				List.of(SpecMocks.spec("svcpkg-a", "svcpkg", SERVICE_JSON)),
				List.of(SpecMocks.reader("svcpkg", "1.0")));
	}

	private void pnpmRuntime() throws IOException {
		env.pnpmMode = true;
		env.node = Files.createFile(new File(tmp, "node").toPath()).toFile();
		env.pnpm = Files.createFile(new File(tmp, "pnpm").toPath()).toFile();
	}

	private void shipPublicTarball() throws IOException {
		env.putBundleFile(bundle, "/node/projects/servoy-public/package.json", "{\"version\":\"1.2.3\"}");
		env.putBundleFile(bundle, "/node/tarballs/" + TARBALL_NAME, "tarball-bytes");
	}

	private static ITiNGExportModel war() {
		return SpecMocks.war("dev", java.util.Set.of(), java.util.Set.of(), java.util.Set.of());
	}

	private void assertWarning(IStatus status, String message) {
		assertEquals(IStatus.WARNING, status.getSeverity());
		assertEquals(message, status.getMessage());
		assertEquals(PackageCheckerJob.class.getName(), status.getPlugin());
	}

	@Test
	void npmDeveloperFirstBuildInstallsDedupsAndBuilds() throws Exception {
		ProjectFilesSetup.dependencies(projectFolder);
		serviceInSpecs();
		List<Boolean> buildRunningDuringCommands = new ArrayList<>();
		env.onCommand = command -> buildRunningDuringCommands
				.add(Boolean.valueOf(WebPackagesListener.isBuildRunning()));

		IStatus status = run(null, null);

		assertEquals(IStatus.OK, status.getSeverity(), status.getMessage());
		List<String> commands = env.commandLines();
		assertEquals(List.of("run build_lib_debug_nowatch", "install @test/svc@1.0 ./dist-public/", "update", "dedup"),
				commands.subList(0, 4));
		assertEquals(5, commands.size());
		assertTrue(commands.get(4).matches("run build_(debug_nowatch|sourcemap)"),
				"the Angular build comes last: " + commands);
		assertEquals(List.of("refreshEditors"), env.events.subList(0, 1),
				"editors are refreshed before the install for a developer run");
		assertEquals(1, env.editorRefreshes);
		assertTrue(ProjectFiles.read(projectFolder, GeneratedProject.SERVICES)
				.contains("import { SvcA } from '@test/svc';"));
		String console = env.consoleText();
		assertTrue(console.startsWith("---- Starting Titanium NGClient solution/dependencies source check ("), console);
		assertTrue(console.contains("- need to install package @test/svc@1.0"), console);
		assertTrue(console.contains("- services ts file changed"), console);
		assertTrue(console.contains("Total time to check/install Titanium NG solution/dependencies into target folder: "
				+ projectFolder + " is "), console);
		assertTrue(env.consoleClosed, "the console is always closed at the end of the job");
		assertFalse(buildRunningDuringCommands.contains(Boolean.FALSE), "isBuildRunning is true while the job runs");
		assertFalse(WebPackagesListener.isBuildRunning(), "and false once it finished");
	}

	@Test
	void pnpmDeveloperBuildCopiesTheShippedTarballBeforeTheFirstCommandAndInstallsItRelative() throws Exception {
		ProjectFilesSetup.dependencies(projectFolder);
		serviceInSpecs();
		pnpmRuntime();
		shipPublicTarball();
		AtomicBoolean tarballThereAtFirstCommand = new AtomicBoolean();
		env.onCommand = command -> {
			if (env.commands.size() == 1)
				tarballThereAtFirstCommand.set(new File(projectFolder, PNPM_PUBLIC_SPECIFIER).isFile());
		};

		IStatus status = run(null, null);

		assertEquals(IStatus.OK, status.getSeverity(), status.getMessage());
		List<String> events = env.events;
		assertEquals(List.of("refreshEditors", "applyKnown", "cmd:install @test/svc@1.0 " + PNPM_PUBLIC_SPECIFIER,
				"cmd:update", "resolve"), events.subList(0, 5));
		assertEquals(6, events.size());
		assertTrue(events.get(5).matches("cmd:run build_(debug_nowatch|sourcemap)"), events.toString());
		assertTrue(tarballThereAtFirstCommand.get(),
				"SVY-21734: the shipped tarball is copied before any pnpm command runs");
		assertEquals(1, env.nodeExtractionWaits);
		assertFalse(env.commandLines().contains("dedup") || env.commandLines().contains("ci"));
	}

	@Test
	void pnpmWarExportRemovesThenInstallsFrozenThenBuildsAndNeverAdds() throws Exception {
		ProjectFilesSetup.warProject(projectFolder);
		pnpmRuntime();
		shipPublicTarball();

		IStatus status = run(war(), "exported");

		assertEquals(IStatus.OK, status.getSeverity(), status.getMessage());
		assertEquals(List.of("applyKnown", "cmd:remove @servoy/gone", "cmd:ci", "resolve", "cmd:" + DEV_BUILD),
				env.events);
		assertEquals("exported", env.lastApprovalsSolutionName);
		assertEquals(0, env.editorRefreshes, "no editors are refreshed for a WAR export");
		assertTrue(
				env.commandLines().stream()
						.noneMatch(command -> command.startsWith("install") || command.equals("update")),
				"SVY-21502: a WAR export never runs an unfrozen add/update");
		assertEquals("",
				ProjectFiles.read(projectFolder, GeneratedProject.POLYFILLS).replace("import 'zone.js';\r\n", ""),
				"a WAR export stays zoneless: polyfills.ts is not touched");
	}

	@Test
	void aBuildThatNeedsNothingSkipsTheInstallAndTheBuild() throws Exception {
		GeneratedProject.create(projectFolder, true, "{\"dependencies\":{}}");

		IStatus status = run(null, null);

		assertEquals(IStatus.OK, status.getSeverity());
		assertTrue(env.commands.isEmpty(), env.commandLines().toString());
		assertEquals(0, env.editorRefreshes);
		assertTrue(env.consoleText().contains("Skipping install / build as no changes were found."), env.consoleText());
		assertTrue(env.consoleText().contains("Total time to check/install Titanium NG solution/dependencies"),
				env.consoleText());
		assertTrue(env.consoleClosed);
	}

	@Test
	void aMissingPnpmRuntimeAbortsBeforeAnythingElse() throws Exception {
		ProjectFilesSetup.dependencies(projectFolder);
		env.pnpmMode = true;
		env.node = new File(tmp, "no-node");
		env.pnpm = new File(tmp, "no-pnpm");
		serviceInSpecs();

		IStatus status = run(null, null);

		assertWarning(status, WebPackagesListener.missingPnpmRuntimeMessage(env.node, env.pnpm));
		assertTrue(env.commands.isEmpty());
		assertFalse(new File(projectFolder, ".tarballs").exists());
		assertEquals(GeneratedProject.SERVICES_MARKERS, ProjectFiles.read(projectFolder, GeneratedProject.SERVICES),
				"no file is generated");
		assertTrue(env.consoleText().contains("\r\nERROR: " + status.getMessage() + "\r\n"), env.consoleText());
		assertTrue(env.consoleClosed);
	}

	@Test
	void aMissingShippedTarballAbortsTheBuildBeforeAnyFileOrCommand() throws Exception {
		ProjectFilesSetup.dependencies(projectFolder);
		pnpmRuntime();
		serviceInSpecs();

		IStatus status = run(null, null);

		assertWarning(status, WebPackagesListener.packFailuresMessage(List.of("@servoy/public")));
		assertTrue(env.commands.isEmpty());
		assertEquals(GeneratedProject.SERVICES_MARKERS, ProjectFiles.read(projectFolder, GeneratedProject.SERVICES));
		assertTrue(env.consoleText().contains("\r\nERROR: " + status.getMessage() + "\r\n"), env.consoleText());
		assertFalse(env.events.contains("applyKnown"));
	}

	@Test
	void aFailingAddIsAWarningThatSkipsTheApprovalsAndTheBuild() throws Exception {
		ProjectFilesSetup.dependencies(projectFolder);
		serviceInSpecs();
		pnpmRuntime();
		shipPublicTarball();
		env.exitCode = args -> Integer.valueOf(args.get(0).equals("install") ? 1 : 0);

		IStatus status = run(null, null);

		assertWarning(status, "npm install [many packages that are needed] EXIT_CODE was: 1");
		assertEquals(List.of("refreshEditors", "applyKnown", "cmd:install @test/svc@1.0 " + PNPM_PUBLIC_SPECIFIER),
				env.events);
		assertTrue(env.consoleClosed);
	}

	@Test
	void aFailingAngularBuildIsAWarning() throws Exception {
		ProjectFilesSetup.dependencies(projectFolder);
		env.exitCode = args -> Integer
				.valueOf(args.get(0).equals("run") && !args.get(1).startsWith("build_lib") ? 1 : 0);

		IStatus status = run(war(), null);

		assertWarning(status, "npm " + DEV_BUILD + " EXIT_CODE was: 1");
		assertEquals(DEV_BUILD, env.commandLines().get(env.commands.size() - 1));
		assertTrue(env.consoleText().contains("Unexpected EXIT_CODE calling npm build_debug_nowatch: 1"),
				env.consoleText());
		assertFalse(env.consoleText().contains("Total time to check/install"), "the job stops at the failed build");
	}

	@Test
	void anErrorFromTheApprovalsOfAWarExportFailsTheBuildBeforeTheAngularBuild() throws Exception {
		ProjectFilesSetup.warProject(projectFolder);
		pnpmRuntime();
		shipPublicTarball();
		env.approvalsResult = new org.eclipse.core.runtime.Status(IStatus.ERROR, "test", "undecided build script");

		IStatus status = run(war(), null);

		assertEquals(IStatus.ERROR, status.getSeverity());
		assertEquals("undecided build script", status.getMessage());
		assertFalse(env.commandLines().contains(DEV_BUILD));
	}

	@Test
	void finishAndRescheduleAlwaysClosesTheConsoleAndSurvivesACloseFailure() throws Exception {
		env.closeFailure = new IOException("cannot close");
		PackageCheckerJob job = new PackageCheckerJob("test", projectFolder, null, null, env);

		job.finishAndReschedule(env.console());

		assertTrue(env.consoleClosed);
		assertFalse(WebPackagesListener.isBuildRunning(),
				"a single request leaves the counter at 0: nothing is rescheduled");
	}
}
