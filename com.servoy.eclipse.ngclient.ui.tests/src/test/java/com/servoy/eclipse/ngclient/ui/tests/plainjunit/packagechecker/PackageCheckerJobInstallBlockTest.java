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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives.TarItem;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;
import com.servoy.eclipse.ngclient.ui.tests.utils.SpecMocks;

/**
 * SVY-21834 - PackageCheckerJob.runInstallBlock and its steps: the exact
 * npm/pnpm command sequences of the install block (SVY-21502 / SVY-21734) and
 * the approvals hooks around it.
 */
class PackageCheckerJobInstallBlockTest {
	private static final String PUBLIC_TARBALL = "servoy-public-1.2.3.tgz";
	// KNOWN DEFECT (reported, not fixed here):
	// WebPackagesListener.PackageCheckerJob#relativeTarballSpecifier documents and
	// the spec requires "./.tarballs/<name>.tgz", but "rel.startsWith(".")" is also
	// true for ".tarballs", so the "./"
	// prefix is never added. The tests pin what the code produces today.
	private static final String PNPM_PUBLIC_SPECIFIER = ".tarballs/" + PUBLIC_TARBALL;
	private static final String GONE_DEPENDENCY = "{\"dependencies\":{\"@servoy/gone\":\"file:.tarballs/servoy-gone-111.tgz\","
			+ "\"registry\":\"^1.0.0\"}}";

	@TempDir
	File tmp;

	private File projectFolder;
	private File publicTarball;
	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();
	private final IProgressMonitor monitor = new NullProgressMonitor();

	@BeforeEach
	void createProject() {
		projectFolder = new File(tmp, "proj");
		assertTrue(projectFolder.mkdirs());
		publicTarball = new File(projectFolder, ".tarballs/" + PUBLIC_TARBALL);
	}

	private PackageCheckerJob job(ITiNGExportModel war, String approvalsSolutionName) {
		return new PackageCheckerJob("test", projectFolder, war, approvalsSolutionName, env);
	}

	private IStatus install(PackageCheckerJob job, boolean sourceChanged, String... packages) {
		return job.runInstallBlock(monitor, env.console(), new LinkedHashSet<>(List.of(packages)), sourceChanged,
				publicTarball);
	}

	private void writeTarballWithVersion(String name, String version) throws IOException {
		File tarball = new File(projectFolder, ".tarballs/" + name);
		assertTrue(tarball.getParentFile().isDirectory() || tarball.getParentFile().mkdirs());
		byte[] tar = TestArchives.tar(TarItem.file("package/package.json", "{\"version\":\"" + version + "\"}"));
		Files.write(tarball.toPath(), TestArchives.gzip(tar));
	}

	private static ITiNGExportModel war() {
		return SpecMocks.war("true", Set.of(), Set.of(), Set.of());
	}

	private void failOn(String verb) {
		env.exitCode = args -> Integer.valueOf(args.get(0).equals(verb) ? 1 : 0);
	}

	private void assertWarning(IStatus status, String message) {
		assertNotNull(status);
		assertEquals(IStatus.WARNING, status.getSeverity());
		assertEquals(message, status.getMessage());
		assertEquals(PackageCheckerJob.class.getName(), status.getPlugin());
	}

	@Test
	void npmFirstInstallBuildsTheLibraryAddsTheDirectoryAndDedups() throws IOException {
		assertNull(install(job(null, null), false, "@test/a@1", "@test/b@2"));

		assertEquals(
				List.of("run build_lib_debug_nowatch", "install @test/a@1 @test/b@2 ./dist-public/", "update", "dedup"),
				env.commandLines());
		assertTrue(env.consoleText().contains("Node NPM dedup time (root node_modules/solution node_modules): "),
				env.consoleText());
		assertEquals(List.of("cmd:run build_lib_debug_nowatch", "cmd:install @test/a@1 @test/b@2 ./dist-public/",
				"cmd:update", "cmd:dedup"), env.events, "npm mode never touches the pnpm approvals");
		assertTrue(env.commands.stream().allMatch(command -> command.folder.equals(projectFolder)));
	}

	@Test
	void npmSkipsTheLibraryBuildWhenDistPublicExistsAndNothingChanged() {
		assertTrue(new File(projectFolder, "dist-public").mkdirs());

		assertNull(install(job(null, null), false));

		assertEquals(List.of("install ./dist-public/", "update", "dedup"), env.commandLines());
	}

	@Test
	void npmRebuildsTheLibraryWhenSourcesChangedEvenIfDistPublicExists() {
		assertTrue(new File(projectFolder, "dist-public").mkdirs());

		assertNull(install(job(null, null), true, "@test/a@1"));

		assertEquals(List.of("run build_lib_debug_nowatch", "install @test/a@1 ./dist-public/", "update", "dedup"),
				env.commandLines());
	}

	@Test
	void npmWarExportIsNotFrozenAndAddsLikeTheDeveloperRun() {
		assertTrue(new File(projectFolder, "dist-public").mkdirs());

		assertNull(install(job(war(), "sol"), false, "@test/a@1"));

		assertEquals(List.of("install @test/a@1 ./dist-public/", "update", "dedup"), env.commandLines(),
				"frozen installs are pnpm only");
	}

	@Test
	void pnpmDeveloperInstallPassesTheRelativeTarballAndNeverBuildsTheLibraryOrDedups() {
		env.pnpmMode = true;

		assertNull(install(job(null, null), true, "@test/a@1"));

		assertEquals(List.of("applyKnown", "cmd:install @test/a@1 " + PNPM_PUBLIC_SPECIFIER, "cmd:update", "resolve"),
				env.events,
				"known approvals first, resolving the approvals only after the successful install, no 'ci', no 'dedup', no library build");
		assertEquals(null, env.lastApprovalsSolutionName);
	}

	@Test
	void pnpmWarExportRemovesWhatIsNotExportedThenInstallsFrozenAndNeverAdds() throws IOException {
		env.pnpmMode = true;
		ProjectFiles.write(projectFolder, "package.json", GONE_DEPENDENCY);
		ProjectFiles.write(projectFolder, ".tarballs/" + "other-222.tgz", "x");

		assertNull(install(job(war(), "mysolution"), true, "@test/a@1"));

		assertEquals(List.of("applyKnown", "cmd:remove @servoy/gone", "cmd:ci", "resolve"), env.events);
		assertEquals("mysolution", env.lastApprovalsSolutionName);
		assertTrue(env.commandLines().stream()
				.noneMatch(command -> command.startsWith("install") || command.startsWith("update")));
		assertTrue(env.consoleText().contains(
				"- pnpm mode: removing 1 component(s) not exported by this WAR from package.json + lockfile: [@servoy/gone]"),
				env.consoleText());
	}

	@Test
	void pnpmWarExportKeepsComponentsWhoseTarballWasPacked() throws IOException {
		env.pnpmMode = true;
		ProjectFiles.write(projectFolder, "package.json", GONE_DEPENDENCY);
		ProjectFiles.write(projectFolder, ".tarballs/servoy-gone-111.tgz", "x");

		assertNull(install(job(war(), null), false));

		assertEquals(List.of("ci"), env.commandLines(), "nothing to remove, only the frozen install");
	}

	@Test
	void pnpmWarExportWithoutPackageJsonOrDependenciesOnlyInstallsFrozen() throws IOException {
		env.pnpmMode = true;
		assertNull(install(job(war(), null), false));
		assertEquals(List.of("ci"), env.commandLines());

		env.commands.clear();
		ProjectFiles.write(projectFolder, "package.json", "{}");
		assertNull(install(job(war(), null), false));
		assertEquals(List.of("ci"), env.commandLines());
	}

	@Test
	void aPackedTarballWithADifferentContentHashFailsTheWarExportAndSkipsTheApprovals() throws IOException {
		env.pnpmMode = true;
		ProjectFiles.write(projectFolder, "package.json", GONE_DEPENDENCY);
		writeTarballWithVersion("servoy-gone-999.tgz", "2.0.0");

		IStatus status = install(job(war(), null), false);

		assertWarning(status,
				"web package '@servoy/gone@2.0.0' differs from the committed pnpm-lock.yaml: expected servoy-gone-111.tgz but packed servoy-gone-999.tgz. "
						+ "Re-run the developer install and commit package.json + pnpm-lock.yaml.");
		assertEquals(List.of("applyKnown"), env.events,
				"no command ran, the approvals are only resolved after a successful install");
		assertTrue(env.consoleText().contains("\r\nERROR: web package '@servoy/gone@2.0.0' differs"), env.consoleText());
	}

	@Test
	void aChangedNightlyPackageIsReResolvedByAnUnfrozenInstallInsteadOfFailing() throws IOException {
		env.pnpmMode = true;
		ProjectFiles.write(projectFolder, "package.json",
				"{\"dependencies\":{\"@servoy/nightly\":\"file:.tarballs/servoy-nightly-111.tgz\","
						+ "\"@servoy/gone\":\"file:.tarballs/servoy-gone-111.tgz\"}}");
		writeTarballWithVersion("servoy-nightly-999.tgz", "2026.9.1-pre.3");

		assertNull(install(job(war(), null), false));

		assertEquals(List.of("ci-unfrozen"), env.commandLines(),
				"no 'pnpm remove' (it would read the old tarball of the changed nightly), only the unfrozen ci");
		String packageJson = Files.readString(new File(projectFolder, "package.json").toPath(), StandardCharsets.UTF_8);
		assertTrue(packageJson.contains("file:.tarballs/servoy-nightly-999.tgz"), packageJson);
		assertFalse(packageJson.contains("servoy-gone-111"), "the non-exported component is removed from package.json directly");
		assertTrue(env.consoleText().contains("web package '@servoy/nightly@2026.9.1-pre.3' differs"), env.consoleText());
	}

	@Test
	void aChangedShippedComponentTarballIsReResolvedEvenThoughItsVersionIsNotANightly() throws IOException {
		env.pnpmMode = true;
		File bundle = new File(tmp, "bundle");
		assertTrue(bundle.mkdirs());
		env.putBundleFile(bundle, "/node/tarballs/component-tarballs.json", "[\"servoy-nightly-999.tgz\"]");
		ProjectFiles.write(projectFolder, "package.json",
				"{\"dependencies\":{\"@servoy/nightly\":\"file:.tarballs/servoy-nightly-111.tgz\"}}");
		writeTarballWithVersion("servoy-nightly-999.tgz", "2025.9.0");

		assertNull(install(job(war(), null), false));

		assertEquals(List.of("ci-unfrozen"), env.commandLines(),
				"a Servoy update changes the dependency-only tarball of an internal component: that is not a lockfile error");
		String packageJson = Files.readString(new File(projectFolder, "package.json").toPath(), StandardCharsets.UTF_8);
		assertTrue(packageJson.contains("file:.tarballs/servoy-nightly-999.tgz"), packageJson);
	}

	@Test
	void aFailingRemoveThrowingAnExceptionIsAWarning() throws IOException {
		env.pnpmMode = true;
		ProjectFiles.write(projectFolder, "package.json", GONE_DEPENDENCY);
		env.failure = args -> args.get(0).equals("remove") ? new IOException("boom") : null;

		IStatus status = install(job(war(), null), false);

		assertWarning(status, "removing non-exported components failed: boom");
		assertTrue(env.consoleText().contains("Exception while removing non-exported components on the WAR path: boom"),
				env.consoleText());
		verify(env.log).error(anyString(), any(Throwable.class));
		assertEquals(List.of("applyKnown", "cmd:remove @servoy/gone"), env.events);
	}

	static Stream<Arguments> failingSteps() {
		return Stream.of(
				Arguments.of("npm library build", false, "run", List.of("run build_lib_debug_nowatch"),
						"npm run build_lib_debug_nowatch EXIT_CODE was: 1",
						"Unexpected EXIT_CODE calling build_lib_debug_nowatch: 1"),
				Arguments.of("npm add", false, "install",
						List.of("run build_lib_debug_nowatch", "install @test/a@1 ./dist-public/"),
						"npm install [many packages that are needed] EXIT_CODE was: 1",
						"Unexpected EXIT_CODE calling npm install [many packages that are needed]: 1"),
				Arguments.of("npm update", false, "update",
						List.of("run build_lib_debug_nowatch", "install @test/a@1 ./dist-public/", "update"),
						"npm update EXIT_CODE was: 1", "Unexpected EXIT_CODE calling npm update: 1"),
				Arguments.of("npm dedup", false, "dedup",
						List.of("run build_lib_debug_nowatch", "install @test/a@1 ./dist-public/", "update", "dedup"),
						"npm dedup EXIT_CODE was: 1", "Unexpected EXIT_CODE calling npm dedup: 1"),
				Arguments.of("pnpm add", true, "install", List.of("install @test/a@1 " + PNPM_PUBLIC_SPECIFIER),
						"npm install [many packages that are needed] EXIT_CODE was: 1",
						"Unexpected EXIT_CODE calling npm install [many packages that are needed]: 1"),
				Arguments.of("pnpm update", true, "update",
						List.of("install @test/a@1 " + PNPM_PUBLIC_SPECIFIER, "update"), "npm update EXIT_CODE was: 1",
						"Unexpected EXIT_CODE calling npm update: 1"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failingSteps")
	void aFailingStepStopsTheBlockAndReturnsAWarning(String name, boolean pnpm, String failingVerb,
			List<String> expectedCommands, String statusMessage, String consoleMessage) {
		env.pnpmMode = pnpm;
		failOn(failingVerb);

		IStatus status = install(job(null, null), false, "@test/a@1");

		assertWarning(status, statusMessage);
		assertEquals(expectedCommands, env.commandLines());
		assertTrue(env.consoleText().contains(consoleMessage), env.consoleText());
		assertFalse(env.events.contains("resolve"), "approvals are never resolved after a failed install");
		assertEquals(pnpm, env.events.contains("applyKnown"));
	}

	@Test
	void aFailingFrozenInstallIsAWarningAndSkipsTheApprovals() throws IOException {
		env.pnpmMode = true;
		failOn("ci");

		IStatus status = install(job(war(), null), false);

		assertWarning(status, "npm ci EXIT_CODE was: 1");
		assertEquals(List.of("applyKnown", "cmd:ci"), env.events);
		assertTrue(env.consoleText().contains("Unexpected EXIT_CODE calling npm ci: 1"), env.consoleText());
	}

	@Test
	void aFailingRemoveCommandIsAWarningAndSkipsTheFrozenInstall() throws IOException {
		env.pnpmMode = true;
		ProjectFiles.write(projectFolder, "package.json", GONE_DEPENDENCY);
		failOn("remove");

		IStatus status = install(job(war(), null), false);

		assertWarning(status, "pnpm remove of non-exported components EXIT_CODE was: 1");
		assertEquals(List.of("applyKnown", "cmd:remove @servoy/gone"), env.events);
		assertTrue(env.consoleText().contains("Unexpected EXIT_CODE calling pnpm remove: 1"), env.consoleText());
	}

	@Test
	void aCommandThatThrowsIsAWarningWithTheExceptionMessage() {
		env.failure = args -> args.get(0).equals("install") ? new IOException("boom") : null;
		assertTrue(new File(projectFolder, "dist-public").mkdirs());

		IStatus status = install(job(null, null), false, "@test/a@1");

		assertWarning(status, "npm install many, packages, that, we, need failed: boom");
		assertTrue(
				env.consoleText()
						.contains("Exception while running 'npm install many, packages, that, we, need': boom"),
				env.consoleText());
		verify(env.log).error(anyString(), any(Throwable.class));
	}

	@Test
	void anErrorFromTheApprovalsIsReturnedInPnpmMode() {
		env.pnpmMode = true;
		IStatus error = new Status(IStatus.ERROR, "test", "undecided build script");
		env.approvalsResult = error;

		assertEquals(error, install(job(war(), null), false));
		assertEquals(List.of("applyKnown", "cmd:ci", "resolve"), env.events);
	}

	@Test
	void aNonErrorFromTheApprovalsIsNotAFailure() {
		env.pnpmMode = true;
		env.approvalsResult = new Status(IStatus.WARNING, "test", "pending");

		assertNull(install(job(null, null), false));
	}

	@Test
	void theApprovalsAreResolvedWithTheSolutionNameOfTheJob() {
		env.pnpmMode = true;

		install(job(null, "exportedSolution"), false);

		assertEquals("exportedSolution", env.lastApprovalsSolutionName);
	}

	@Test
	void theStepsRunTheirOwnCommandAndReportTheirOwnFailure() {
		PackageCheckerJob job = job(null, null);
		assertNull(job.buildPublicDist(monitor, env.console()));
		assertNull(job.updatePackages(monitor, env.console()));
		assertNull(job.frozenInstall(monitor, env.console()));
		assertEquals(List.of("run build_lib_debug_nowatch", "update", "ci"), env.commandLines());

		failOn("update");
		assertWarning(job.updatePackages(monitor, env.console()), "npm update EXIT_CODE was: 1");
	}

	@Test
	void addPackagesInPnpmModeNeedsNoDirectoryAndUsesTheForwardSlashSpecifier() {
		env.pnpmMode = true;

		assertNull(job(null, null).addPackages(monitor, env.console(), Set.of(), publicTarball));

		assertEquals(List.of("install " + PNPM_PUBLIC_SPECIFIER), env.commandLines());
	}
}
