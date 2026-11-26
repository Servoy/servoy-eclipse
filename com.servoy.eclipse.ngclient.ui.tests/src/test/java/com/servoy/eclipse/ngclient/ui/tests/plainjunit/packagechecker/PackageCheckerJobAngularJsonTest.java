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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sablo.specification.CssLib;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob.SpecInfo;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.GeneratedProject;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;

/**
 * SVY-21834 - PackageCheckerJob.updateAngularJsonStyles /
 * updateAngularJsonAssets: what ends up in angular.json and when the file is
 * (not) rewritten.
 */
class PackageCheckerJobAngularJsonTest {
	private static final String FA = "@fortawesome/fontawesome-free/css/all.css";

	@TempDir
	File projectFolder;

	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();

	private PackageCheckerJob job() {
		return new PackageCheckerJob("test", projectFolder, null, null, env);
	}

	private static SpecInfo cssInfo(boolean fontAwesomePro, String... urls) {
		SpecInfo info = new SpecInfo();
		info.hasFontAwesomePro = fontAwesomePro;
		for (String url : urls)
			info.cssLibs.add(new CssLib(url));
		return info;
	}

	private static SpecInfo assetInfo(String... assets) {
		SpecInfo info = new SpecInfo();
		info.assetsToAdd.addAll(List.of(assets));
		return info;
	}

	private static List<String> strings(JSONArray array) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < array.length(); i++)
			result.add(array.get(i).toString());
		return result;
	}

	private JSONArray options(String name) throws IOException {
		return new JSONObject(ProjectFiles.read(projectFolder, "angular.json")).getJSONObject("projects")
				.getJSONObject("ngclient2").getJSONObject("architect").getJSONObject("build").getJSONObject("options")
				.getJSONArray(name);
	}

	private void angularJson(String styles, String assets) throws IOException {
		ProjectFiles.write(projectFolder, "angular.json",
				"{\"projects\":{\"ngclient2\":{\"architect\":{\"build\":{\"options\":{\"styles\":" + styles
						+ ",\"assets\":" + assets + "}}}}}}");
	}

	@Test
	void withoutCssLibrariesAngularJsonIsNotTouched() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		File angularJson = new File(projectFolder, "angular.json");
		assertTrue(angularJson.delete(), "not even read when there is nothing to add");

		assertFalse(job().updateAngularJsonStyles(cssInfo(false), env.console()));

		assertFalse(angularJson.exists());
		assertEquals("", env.consoleText());
	}

	@Test
	void newLibrariesAreWrittenBetweenFontAwesomeAndTheApplicationStyles() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");

		assertTrue(job().updateAngularJsonStyles(cssInfo(false, "~@test/x/x.css"), env.console()));

		assertEquals(List.of(FA, "./node_modules/@test/x/x.css", "src/styles.css"), strings(options("styles")));
		assertTrue(env.consoleText().contains("- styles source changed"));
	}

	@Test
	void libraryOrderFollowsThePriorityDescendingAndAnUpToDateFileIsNotRewritten() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		SpecInfo info = cssInfo(false, "~@test/low/l.css;priority=9", "~@test/high/h.css;priority=1");
		assertTrue(job().updateAngularJsonStyles(info, env.console()));
		List<String> written = strings(options("styles"));
		assertEquals(List.of(FA, "./node_modules/@test/low/l.css", "./node_modules/@test/high/h.css", "src/styles.css"),
				written,
				"a higher priority number is written first: priority 0 is the most important and must come last");
		String content = ProjectFiles.read(projectFolder, "angular.json");
		FakePackageCheckerEnvironment second = new FakePackageCheckerEnvironment();

		assertFalse(new PackageCheckerJob("test", projectFolder, null, null, second).updateAngularJsonStyles(info,
				second.console()));

		assertEquals(content, ProjectFiles.read(projectFolder, "angular.json"));
		assertEquals("", second.consoleText());
	}

	@Test
	void fontAwesomeProReplacesTheFreeFontAwesomeStyle() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");

		assertTrue(job().updateAngularJsonStyles(cssInfo(true, "~@fa/pro.css"), env.console()));

		assertEquals(List.of("./node_modules/@fa/pro.css", "src/styles.css"), strings(options("styles")));
	}

	@Test
	void aMissingLibraryIsDetectedEvenWhenTheNumberOfStylesMatches() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		angularJson("[\"" + FA + "\",\"other.css\",\"src/styles.css\"]", "[]");

		assertTrue(job().updateAngularJsonStyles(cssInfo(false, "mine.css"), env.console()));

		assertEquals(List.of(FA, "mine.css", "src/styles.css"), strings(options("styles")),
				"the unknown style is removed, everything is rewritten");
	}

	@Test
	void fontAwesomeProKeepsAFileThatAlreadyHasAllLibraries() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		angularJson("[\"./node_modules/@fa/pro.css\",\"src/styles.css\"]", "[]");
		String content = ProjectFiles.read(projectFolder, "angular.json");

		assertFalse(job().updateAngularJsonStyles(cssInfo(true, "~@fa/pro.css"), env.console()));

		assertEquals(content, ProjectFiles.read(projectFolder, "angular.json"));
	}

	@Test
	void internalDefaultComponentCssIsTheLiveSourceInPnpmModeAndNodeModulesInNpmMode() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		env.pnpmMode = true;
		assertTrue(job().updateAngularJsonStyles(cssInfo(false, "~@servoy/servoydefault/css/x.css", "~@test/ext/e.css"),
				env.console()));
		List<String> pnpmStyles = strings(options("styles"));
		assertTrue(pnpmStyles.contains("./projects/servoydefault/css/x.css"), pnpmStyles.toString());
		assertTrue(pnpmStyles.contains("./node_modules/@test/ext/e.css"), pnpmStyles.toString());

		GeneratedProject.create(projectFolder, false, "{}");
		env.pnpmMode = false;
		assertTrue(job().updateAngularJsonStyles(cssInfo(false, "~@servoy/servoydefault/css/x.css"), env.console()));
		assertTrue(strings(options("styles")).contains("./node_modules/@servoy/servoydefault/css/x.css"));
	}

	@Test
	void aMissingAngularJsonIsReportedForStyles() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, "angular.json").delete());

		assertFalse(job().updateAngularJsonStyles(cssInfo(false, "~@test/x/x.css"), env.console()));

		assertTrue(env.consoleText().contains("Exception while adjusting angular.json styles: "), env.consoleText());
	}

	@Test
	void newAssetsAreAppendedAndKnownOnesAreNot() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(job().updateAngularJsonAssets(assetInfo("a.png", "{\"glob\":\"**/*\"}"), env.console()));

		List<String> assets = strings(options("assets"));
		assertEquals(2, assets.size(), assets.toString());
		assertTrue(assets.contains("a.png"));
		assertTrue(options("assets").toString().contains("\"glob\":\"**/*\""),
				"a json asset is added as an object, not as a string");
		assertTrue(env.consoleText().contains("- assets source changed"));
		FakePackageCheckerEnvironment second = new FakePackageCheckerEnvironment();

		assertFalse(new PackageCheckerJob("test", projectFolder, null, null, second)
				.updateAngularJsonAssets(assetInfo("a.png", "{\"glob\":\"**/*\"}"), second.console()));

		assertEquals(2, options("assets").length());
	}

	@Test
	void noAssetsLeaveAngularJsonUntouched() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, "angular.json").delete());

		assertFalse(job().updateAngularJsonAssets(assetInfo(), env.console()));

		assertFalse(new File(projectFolder, "angular.json").exists());
	}

	@Test
	void anInvalidJsonAssetIsReportedAndNotAdded() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");

		assertTrue(job().updateAngularJsonAssets(assetInfo("{bad}"), env.console()));

		assertEquals(0, options("assets").length());
		assertTrue(env.consoleText().contains("Can't add asset object to angular.json: {bad}: "), env.consoleText());
	}

	@Test
	void aMissingAngularJsonIsReportedForAssets() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, "angular.json").delete());

		assertFalse(job().updateAngularJsonAssets(assetInfo("a.png"), env.console()));

		assertTrue(env.consoleText().contains("Exception while checking assets: "), env.consoleText());
	}
}
