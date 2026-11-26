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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;

/**
 * SVY-21834 - PackageCheckerJob.dedupAgainstRootNodeModules / npmDedup:
 * packages of the solution node_modules that also exist in the root
 * node_modules (the parent folder of the project) are deleted.
 */
class PackageCheckerJobDedupTest {
	@TempDir
	File tmp;

	private File projectFolder;
	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();

	@BeforeEach
	void createProject() {
		projectFolder = new File(tmp, "proj");
		assertTrue(projectFolder.mkdirs());
	}

	private void pkg(String root, String path) throws IOException {
		ProjectFiles.write(new File(tmp, root), "node_modules/" + path + "/package.json", "{}");
		ProjectFiles.write(new File(tmp, root), "node_modules/" + path + "/index.js", "x");
	}

	private boolean exists(String root, String path) {
		return new File(tmp, root + "/node_modules/" + path).exists();
	}

	@Test
	void aPackageThatAlsoExistsInTheRootIsDeletedFromTheSolution() throws IOException {
		pkg("proj", "rxjs");
		pkg("proj", "only-local");
		pkg("", "rxjs");

		PackageCheckerJob.dedupAgainstRootNodeModules(projectFolder, env.console());

		assertFalse(exists("proj", "rxjs"));
		assertTrue(exists("proj", "only-local"));
		assertTrue(exists("", "rxjs"), "the root copy is never touched");
		assertEquals("", env.consoleText());
	}

	@Test
	void inAGroupFolderOnlyTheNestedDuplicatesAreDeleted() throws IOException {
		pkg("proj", "@angular/common");
		pkg("proj", "@angular/animations");
		pkg("", "@angular/common");
		pkg("", "@angular/core");

		PackageCheckerJob.dedupAgainstRootNodeModules(projectFolder, env.console());

		assertFalse(exists("proj", "@angular/common"));
		assertTrue(exists("proj", "@angular/animations"));
		assertTrue(exists("proj", "@angular"), "the group folder itself stays");
		assertTrue(exists("", "@angular/common"));
	}

	@Test
	void aDuplicateNameThatIsAPlainFileIsLeftAlone() throws IOException {
		ProjectFiles.write(projectFolder, "node_modules/.package-lock.json", "{}");
		ProjectFiles.write(tmp, "node_modules/.package-lock.json", "{}");

		PackageCheckerJob.dedupAgainstRootNodeModules(projectFolder, env.console());

		assertTrue(exists("proj", ".package-lock.json"));
	}

	@Test
	void aGroupFolderWithoutAPackageJsonThatMissesInTheRootKeepsEverything() throws IOException {
		pkg("proj", "@scope/a");
		ProjectFiles.write(tmp, "node_modules/@scope/other/package.json", "{}");

		PackageCheckerJob.dedupAgainstRootNodeModules(projectFolder, env.console());

		assertTrue(exists("proj", "@scope/a"));
	}

	@Test
	void withoutANodeModulesFolderOnEitherSideNothingHappens() throws IOException {
		assertDoesNotThrow(() -> PackageCheckerJob.dedupAgainstRootNodeModules(projectFolder, env.console()));

		pkg("proj", "rxjs");
		assertDoesNotThrow(() -> PackageCheckerJob.dedupAgainstRootNodeModules(projectFolder, env.console()),
				"no root node_modules");
		assertTrue(exists("proj", "rxjs"));
	}

	@Test
	void npmDedupRunsTheDedupCommandAndThenTheOwnPass() throws IOException {
		pkg("proj", "rxjs");
		pkg("", "rxjs");

		IStatus status = new PackageCheckerJob("test", projectFolder, null, null, env)
				.npmDedup(new NullProgressMonitor(), env.console());

		assertNull(status);
		assertEquals(List.of("dedup"), env.commandLines());
		assertFalse(exists("proj", "rxjs"));
		assertTrue(env.consoleText().contains("Node NPM dedup time (root node_modules/solution node_modules): "),
				env.consoleText());
	}

	@Test
	void aFailingDedupCommandSkipsTheOwnPass() throws IOException {
		pkg("proj", "rxjs");
		pkg("", "rxjs");
		env.exitCode = args -> Integer.valueOf(1);

		IStatus status = new PackageCheckerJob("test", projectFolder, null, null, env)
				.npmDedup(new NullProgressMonitor(), env.console());

		assertNotNull(status);
		assertEquals(IStatus.WARNING, status.getSeverity());
		assertEquals("npm dedup EXIT_CODE was: 1", status.getMessage());
		assertTrue(exists("proj", "rxjs"), "the own pass only runs after a successful 'npm dedup'");
		assertFalse(env.consoleText().contains("Node NPM dedup time"));
	}
}
