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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.sablo.specification.CssLib;
import org.sablo.specification.NG2Config;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;

/**
 * Builds the sablo spec objects (mocks with a real {@link NG2Config}) and the
 * real {@link SpecProviderState} the PackageCheckerJob tests feed through the
 * fake environment.
 */
public final class SpecMocks {
	private SpecMocks() {
	}

	public static WebObjectSpecification spec(String name, String packageName, String ng2ConfigJson) {
		WebObjectSpecification spec = mock(WebObjectSpecification.class);
		doReturn(name).when(spec).getName();
		doReturn(packageName).when(spec).getPackageName();
		doReturn(new NG2Config(new JSONObject(ng2ConfigJson))).when(spec).getNG2Config();
		return spec;
	}

	public static IPackageReader reader(String packageName, String version) {
		IPackageReader reader = mock(IPackageReader.class);
		doReturn(packageName).when(reader).getPackageName();
		doReturn(version).when(reader).getVersion();
		return reader;
	}

	@SuppressWarnings("unchecked")
	public static <T extends WebObjectSpecification> PackageSpecification<T> pkg(String packageName, String npmName,
			String ng2Module, String ng2Components, String entryPoint, Set<CssLib> cssLibs,
			Map<String, T> specifications) {
		PackageSpecification<T> pkg = mock(PackageSpecification.class);
		doReturn(packageName).when(pkg).getPackageName();
		doReturn(npmName).when(pkg).getNpmPackageName();
		doReturn(ng2Module).when(pkg).getNg2Module();
		doReturn(ng2Components).when(pkg).getNg2Components();
		doReturn(Boolean.valueOf(ng2Components != null && !ng2Components.isEmpty())).when(pkg)
				.hasStandaloneComponents();
		doReturn(entryPoint).when(pkg).getEntryPoint();
		doReturn(cssLibs).when(pkg).getNg2CssLibrary();
		doReturn(specifications).when(pkg).getSpecifications();
		return pkg;
	}

	public static WebLayoutSpecification layout(String name, String tagType, String... directives) {
		WebLayoutSpecification spec = mock(WebLayoutSpecification.class);
		doReturn(name).when(spec).getName();
		doReturn(List.of(directives)).when(spec).getDirectives();
		if (tagType != null) {
			PropertyDescription tag = mock(PropertyDescription.class);
			doReturn(tagType).when(tag).getDefaultValue();
			doReturn(tag).when(spec).getProperty("tagType");
		}
		return spec;
	}

	public static ITiNGExportModel war(String exportMode, Set<String> packages, Set<String> components,
			Set<String> services) {
		ITiNGExportModel war = mock(ITiNGExportModel.class);
		doReturn(exportMode).when(war).exportNG2Mode();
		doReturn(packages).when(war).getExportedPackagesExceptSablo();
		doReturn(components).when(war).getAllExportedComponents();
		doReturn(services).when(war).getAllExportedServicesWithoutSabloServices();
		return war;
	}

	public static SpecProviderState emptyState() {
		return new SpecProviderState(Map.of(), Map.of(), Map.of(), List.of());
	}

	public static SpecProviderState state(Map<String, PackageSpecification<WebObjectSpecification>> packages,
			Map<String, PackageSpecification<WebLayoutSpecification>> layouts, List<WebObjectSpecification> specs,
			List<IPackageReader> readers) {
		Map<String, WebObjectSpecification> byName = new HashMap<>();
		for (WebObjectSpecification spec : specs) {
			byName.put(spec.getName(), spec);
		}
		return new SpecProviderState(packages, layouts, byName, readers);
	}
}
