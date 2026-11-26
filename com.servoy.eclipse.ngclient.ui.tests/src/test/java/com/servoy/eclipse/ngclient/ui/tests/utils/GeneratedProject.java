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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener;

/**
 * Creates a minimal generated Titanium NG project folder: every file
 * PackageCheckerJob rewrites, already in the form the job generates when there
 * are no components, services, styles or assets (so a run on it changes
 * nothing).
 */
public final class GeneratedProject {
	public static final String COMPONENT_MARKERS = "<!-- component template generate start -->\n<!-- component template generate end -->\n"
			+ "// component viewchild template generate start\n// component viewchild template generate end\n";
	public static final String STRUCTURE_MARKERS = "<!-- structure template generate start -->\n<!-- structure template generate end -->\n"
			+ "// structure viewchild template generate start\n// structure viewchild template generate end\n";
	public static final String SERVICES_MARKERS = "// generated imports start\n// generated imports end\n"
			+ "// generated services start\n// generated services end\n// generated providers start\n// generated providers end\n"
			+ "// generated modules start\n// generated modules end\n";
	public static final String ANGULAR_JSON = "{\"projects\":{\"ngclient2\":{\"architect\":{\"build\":{\"options\":"
			+ "{\"styles\":[\"@fortawesome/fontawesome-free/css/all.css\",\"src/styles.css\"],\"assets\":[]}}}}}}";

	public static final String FORM = "src/ngclient/form/form_component.component.ts";
	public static final String DESIGNER = "src/designer/designform_component.component.ts";
	public static final String LFC = "src/servoycore/listformcomponent/listformcomponent.ts";
	public static final String FORM_TEMPLATE = "src/formtemplate/formtemplate_component.component.ts";
	public static final String SERVICES = "src/ngclient/allservices.service.ts";
	public static final String MODULE = "src/ngclient/allcomponents.module.ts";
	public static final String POLYFILLS = "src/polyfills.ts";

	private GeneratedProject() {
	}

	public static void create(File root, boolean withDist, String packageJson) throws IOException {
		ProjectFiles.write(root, "package.json", packageJson);
		ProjectFiles.write(root, SERVICES, SERVICES_MARKERS);
		ProjectFiles.write(root, FORM, COMPONENT_MARKERS + STRUCTURE_MARKERS);
		ProjectFiles.write(root, DESIGNER, COMPONENT_MARKERS + STRUCTURE_MARKERS);
		ProjectFiles.write(root, LFC, STRUCTURE_MARKERS);
		ProjectFiles.write(root, MODULE, WebPackagesListener.generateAllComponentsModuleContent(List.of()));
		ProjectFiles.write(root, "angular.json", ANGULAR_JSON);
		ProjectFiles.write(root, POLYFILLS, "import 'zone.js';\r\n");
		ProjectFiles.write(root, "tsconfig.json", "{\"compilerOptions\":{\"paths\":{}}}");
		if (withDist)
			ProjectFiles.write(root, "dist/app/browser/index.html", "<html></html>");
	}
}
