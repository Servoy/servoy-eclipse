/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.build.documentation.apigen;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import freemarker.template.TemplateException;

/**
 * @author acostescu
 */
public interface INGPackageInfoGenerator
{

	public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryName, boolean service)
		throws TemplateException, IOException;

	/**
	 * This method will be called after {@link #generateComponentOrServiceInfo(Map, File, String, String, boolean)} calls happened for all components/services of this package.
	 * If the webpackage.json file was not found, this will not be called.
	 */
	public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType)
		throws TemplateException, IOException;


	public void currentPackageWasProcessed();

	public boolean shouldTurnAPIDocsIntoMarkdown();

	public boolean shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt();

}
