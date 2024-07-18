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

	/**
	 * @param deprecationString null if not deprecated, either string "true" or a deprecation message if it is deprecated
	 * @param replacementInCaseOfDeprecation replacementInCaseOfDeprecation can be either null or the component with which it could be replaced in which case this component should be considered as being deprecated even if deprecationString is null
	 */
	public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryName, boolean service,
		String deprecationString, String replacementInCaseOfDeprecation)
		throws TemplateException, IOException;

	/**
	 * This method will be called after {@link #generateComponentOrServiceInfo(Map, File, String, String, boolean)} calls happened for all components/services of this package.
	 * If the webpackage.json file was not found, this will not be called.
	 * @param root can contain extra objects needed by the templates already; for example "utils" could be given as just an utility objects that has content/methods that are needed in the template
	 */
	public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType,
		Map<String, Object> root)
		throws TemplateException, IOException;


	public void currentPackageWasProcessed();

	public boolean shouldTurnAPIDocsIntoMarkdown();

	public boolean shouldSetJSDocGeneratedFromSpecEvenIfThereIsNoDescriptionInIt();

}
