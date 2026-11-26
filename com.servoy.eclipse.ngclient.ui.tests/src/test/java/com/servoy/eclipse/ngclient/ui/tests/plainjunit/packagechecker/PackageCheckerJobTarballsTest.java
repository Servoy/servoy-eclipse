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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob.PackageCheckResult;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob.SpecInfo;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;
import com.servoy.eclipse.ngclient.ui.tests.utils.SpecMocks;

/**
 * SVY-21834 - PackageCheckerJob.checkRuntimeAvailable and
 * prepareTarballsAndCheckPackages (SVY-21734 / SVY-21502: the
 * shipped @servoy/public tarball is copied before checkPackage; which packages
 * need an install).
 */
class PackageCheckerJobTarballsTest {
	private static final String SERVICE_JSON = "{\"packageName\":\"@test/svc\",\"serviceName\":\"SvcA\",\"entryPoint\":\"projects/svc\"}";
	private static final String TARBALL_NAME = "servoy-public-1.2.3.tgz";

	@TempDir
	File tmp;

	private File projectFolder;
	private File bundle;
	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();

	@BeforeEach
	void createFolders() throws IOException {
		projectFolder = new File(tmp, "proj");
		bundle = new File(tmp, "bundle");
		assertTrue(projectFolder.mkdirs());
		assertTrue(bundle.mkdirs());
	}

	private PackageCheckerJob job() {
		return new PackageCheckerJob("test", projectFolder, null, null, env);
	}

	private void shipPublicTarball() throws IOException {
		env.putBundleFile(bundle, "/node/projects/servoy-public/package.json", "{\"version\":\"1.2.3\"}");
		env.putBundleFile(bundle, "/node/tarballs/" + TARBALL_NAME, "tarball-bytes");
	}

	private SpecInfo servicePackage(String version) {
		SpecInfo info = new SpecInfo();
		info.ng2Services.put(SpecMocks.spec("svcpkg-a", "svcpkg", SERVICE_JSON), SpecMocks.reader("svcpkg", version));
		return info;
	}

	private void packageJson(String dependencies) throws IOException {
		ProjectFiles.write(projectFolder, "package.json", "{\"dependencies\":" + dependencies + "}");
	}

	@Test
	void npmModeNeedsNoRuntimeCheck() {
		assertNull(job().checkRuntimeAvailable(env.console()));

		assertEquals(0, env.nodeExtractionWaits);
	}

	@Test
	void pnpmModeWithNodeAndPnpmIsAvailableAfterWaitingForTheExtraction() throws IOException {
		env.pnpmMode = true;
		env.node = Files.createFile(new File(tmp, "node").toPath()).toFile();
		env.pnpm = Files.createFile(new File(tmp, "pnpm").toPath()).toFile();

		assertNull(job().checkRuntimeAvailable(env.console()));

		assertEquals(1, env.nodeExtractionWaits);
		assertEquals("", env.consoleText());
	}

	@Test
	void pnpmModeWithoutAnExistingNodeOrPnpmIsAWarningThatIsLoggedAndPrinted() throws IOException {
		env.pnpmMode = true;
		env.node = new File(tmp, "missing-node");
		env.pnpm = Files.createFile(new File(tmp, "pnpm").toPath()).toFile();
		String message = WebPackagesListener.missingPnpmRuntimeMessage(env.node, env.pnpm);

		IStatus status = job().checkRuntimeAvailable(env.console());

		assertNotNull(status);
		assertAll(() -> assertEquals(IStatus.WARNING, status.getSeverity()),
				() -> assertEquals(message, status.getMessage()),
				() -> assertEquals(PackageCheckerJob.class.getName(), status.getPlugin()),
				() -> assertTrue(env.consoleText().contains("\r\nERROR: " + message + "\r\n"), env.consoleText()));
		verify(env.log).error(message);
	}

	@Test
	void withoutAnyServicesOrComponentsNothingIsCheckedAndNoPackageJsonIsRead() {
		PackageCheckResult result = job().prepareTarballsAndCheckPackages(new SpecInfo(), env.console());

		assertFalse(result.sourceChanged, "no dist folder, but nothing to build for either");
		assertTrue(result.packageToInstall.isEmpty());
		assertNull(result.copiedPublicTarball);
		assertEquals("", env.consoleText());
	}

	@Test
	void aMissingDistIndexTriggersTheBuildAndAnExistingOneDoesNot() throws IOException {
		packageJson("{}");
		PackageCheckResult first = job().prepareTarballsAndCheckPackages(servicePackage("1.0"), env.console());
		assertTrue(first.sourceChanged);
		assertTrue(env.consoleText()
				.contains("- build will be triggered; no previously build-generated files detected..."));

		ProjectFiles.write(projectFolder, "dist/app/browser/index.html", "<html></html>");
		FakePackageCheckerEnvironment second = new FakePackageCheckerEnvironment();
		PackageCheckResult result = new PackageCheckerJob("test", projectFolder, null, null, second)
				.prepareTarballsAndCheckPackages(servicePackage("1.0"), second.console());
		assertFalse(result.sourceChanged);
		assertFalse(second.consoleText().contains("build will be triggered"));
	}

	@Test
	void aServiceThatIsNotInstalledInTheRequestedVersionIsInstalled() throws IOException {
		packageJson("{\"other\":\"1.0\"}");

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(servicePackage("1.0"), env.console());

		assertEquals(Set.of("@test/svc@1.0"), result.packageToInstall);
		assertTrue(env.consoleText().contains("- need to install package @test/svc@1.0"), env.consoleText());
	}

	@Test
	void aServiceThatIsInstalledInTheRequestedVersionIsNotInstalledAgain() throws IOException {
		packageJson("{\"@test/svc\":\"^1.0\"}");

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(servicePackage("1.0"), env.console());

		assertTrue(result.packageToInstall.isEmpty());
		assertFalse(env.consoleText().contains("need to install"));
	}

	@Test
	void aPrereleaseManifestVersionIsMatchedAsAnNpmPrereleaseVersion() throws IOException {
		packageJson("{\"@test/svc\":\"1.0.0\"}");
		PackageCheckResult stable = job().prepareTarballsAndCheckPackages(servicePackage("1.0.0.rc1"), env.console());
		assertEquals(Set.of("@test/svc@1.0.0-rc1"), stable.packageToInstall,
				"x.y.z.rc1 becomes x.y.z-rc1, which is not the installed x.y.z");

		packageJson("{\"@test/svc\":\"1.0.0-rc1\"}");
		assertTrue(job().prepareTarballsAndCheckPackages(servicePackage("1.0.0.rc1"), env.console()).packageToInstall
				.isEmpty());
	}

	@Test
	void aServiceWithoutEntryPointIsNeverInstalled() throws IOException {
		packageJson("{}");
		SpecInfo info = new SpecInfo();
		info.ng2Services.put(
				SpecMocks.spec("svcpkg-a", "svcpkg", "{\"packageName\":\"@test/svc\",\"serviceName\":\"SvcA\"}"),
				SpecMocks.reader("svcpkg", "1.0"));

		assertTrue(job().prepareTarballsAndCheckPackages(info, env.console()).packageToInstall.isEmpty());
	}

	@Test
	void componentPackagesAreInstalledByTheirNpmName() throws IOException {
		packageJson("{\"@test/other\":\"1\"}");
		SpecInfo info = new SpecInfo();
		PackageSpecification<WebObjectSpecification> missing = SpecMocks.pkg("comp", "@test/comp", "CompModule", null,
				"projects/comp", null, Map.of());
		PackageSpecification<WebObjectSpecification> installed = SpecMocks.pkg("other", "@test/other", "OtherModule",
				null, "projects/other", null, Map.of());
		PackageSpecification<WebObjectSpecification> noEntryPoint = SpecMocks.pkg("noentry", "@test/noentry", "NoEntry",
				null, null, null, Map.of());
		info.componentPackageSpecToReader.put(missing, SpecMocks.reader("comp", "2.0"));
		info.componentPackageSpecToReader.put(installed, SpecMocks.reader("other", "1"));
		info.componentPackageSpecToReader.put(noEntryPoint, SpecMocks.reader("noentry", "3"));

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(info, env.console());

		assertEquals(Set.of("@test/comp@2.0"), result.packageToInstall);
	}

	@Test
	void aMissingPackageJsonIsLoggedAndReportedWithoutInstallingAnything() {
		PackageCheckResult result = job().prepareTarballsAndCheckPackages(servicePackage("1.0"), env.console());

		assertTrue(result.packageToInstall.isEmpty());
		assertTrue(env.consoleText().contains("Exception while checking what packages should be installed: "),
				env.consoleText());
		verify(env.log).error(anyString(), any(Throwable.class));
	}

	@Test
	void npmModeDoesNotCopyTheShippedTarball() throws IOException {
		shipPublicTarball();
		packageJson("{}");

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(servicePackage("1.0"), env.console());

		assertNull(result.copiedPublicTarball);
		assertFalse(new File(projectFolder, ".tarballs").exists());
	}

	@Test
	void pnpmModeCopiesTheVersionNamedShippedTarballIntoTheTarballsFolder() throws IOException {
		env.pnpmMode = true;
		shipPublicTarball();

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(new SpecInfo(), env.console());

		File expected = new File(new File(projectFolder, ".tarballs"), TARBALL_NAME);
		assertEquals(expected, result.copiedPublicTarball);
		assertEquals("tarball-bytes", Files.readString(expected.toPath()));
		assertTrue(env.consoleText().contains("- pnpm mode: copied shipped @servoy/public tarball " + TARBALL_NAME
				+ " into .tarballs/ (no ng build)"), env.consoleText());
	}

	@Test
	void theShippedTarballIsInPlaceBeforeThePackagesAreChecked() throws IOException {
		env.pnpmMode = true;
		shipPublicTarball();
		packageJson("{}");
		SpecInfo info = new SpecInfo();
		IPackageReader reader = SpecMocks.reader("svcpkg", "1.0");
		AtomicBoolean tarballPresentWhenChecked = new AtomicBoolean(false);
		doAnswer(invocation -> {
			tarballPresentWhenChecked.set(new File(new File(projectFolder, ".tarballs"), TARBALL_NAME).isFile());
			return "1.0";
		}).when(reader).getVersion();
		info.ng2Services.put(SpecMocks.spec("svcpkg-a", "svcpkg", SERVICE_JSON), reader);

		job().prepareTarballsAndCheckPackages(info, env.console());

		assertTrue(tarballPresentWhenChecked.get(), "SVY-21734: copy before checkPackage");
	}

	@Test
	void aMissingShippedTarballIsReportedAndNothingIsCopied() throws IOException {
		env.pnpmMode = true;

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(new SpecInfo(), env.console());

		assertNull(result.copiedPublicTarball);
		assertTrue(
				env.consoleText().contains(
						"- WARNING: shipped tarball /node/tarballs/servoy-public.tgz was not found in the plugin"),
				"without a readable version the unversioned name is used: " + env.consoleText());
		assertTrue(new File(projectFolder, ".tarballs").isDirectory());
	}

	@Test
	void aShippedTarballThatCannotBeReadIsReportedAndLogged() throws IOException {
		env.pnpmMode = true;
		env.putBundleFile(bundle, "/node/projects/servoy-public/package.json", "{\"version\":\"1.2.3\"}");
		env.bundleEntries.put("/node/tarballs/" + TARBALL_NAME, new File(bundle, "does-not-exist.tgz").toURI().toURL());

		PackageCheckResult result = job().prepareTarballsAndCheckPackages(new SpecInfo(), env.console());

		assertNull(result.copiedPublicTarball);
		assertTrue(env.consoleText().contains(
				"Exception while copying shipped @servoy/public tarball " + TARBALL_NAME + " into .tarballs/: "),
				env.consoleText());
		verify(env.log).error(anyString(), any(Throwable.class));
	}
}
