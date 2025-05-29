/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ui.svygen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.util.Utils;

/**
 * Reads and provides the template mappings (components, ...) that the AI can use when generating the solution.
 *
 * @author acostescu
 */
public class TemplateForAIReader
{

	private final Map<String, ComponentTemplateDefinition> componentTemplateDefinitions = new HashMap<>();
	private final Map<String, BaseFormTemplateDefinition> baseFormTemplateDefinitions = new HashMap<>();

	public TemplateForAIReader(File templatesDir)
	{
		// read component templates
		File[] templateDirs = new File(templatesDir, AISolutionGenerator.SERVOY_COMPONENT_TEMPLATE_DIR).listFiles();
		for (File templateDir : templateDirs)
		{
			String templateStyleToAddToSolution = Utils.getTXTFileContent(new File(templateDir, "style.less"));
			String templateJSON = Utils.getTXTFileContent(new File(templateDir, "template.json"));

			ComponentTemplateDefinition td = new ComponentTemplateDefinition(templateStyleToAddToSolution, templateJSON);
			componentTemplateDefinitions.put(td.getName(), td);
		}

		// read base form templates
		templateDirs = new File(templatesDir, AISolutionGenerator.SERVOY_BASE_FORMS_TEMPLATE_DIR).listFiles();
		for (File templateDir : templateDirs)
		{
			String templateJSON = Utils.getTXTFileContent(new File(templateDir, "template.json"));

			BaseFormTemplateDefinition td = new BaseFormTemplateDefinition(templateJSON);
			baseFormTemplateDefinitions.put(td.getName(), td);
		}
	}

	public ComponentTemplateDefinition getComponentTemplateDefinition(String templateRef)
	{
		return componentTemplateDefinitions.get(templateRef);
	}

	public List<ComponentTemplateDefinition> getAllComponentTemplateDefinitions()
	{
		return new ArrayList<>(componentTemplateDefinitions.values());
	}

	public BaseFormTemplateDefinition getBaseFormTemplateDefinition(String extendedFormTemplateRef)
	{
		return baseFormTemplateDefinitions.get(extendedFormTemplateRef);
	}

}
