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

import org.json.JSONObject;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * @author acostescu
 */
public class AISolutionGenerator
{

	private static final String SVYGEN_PATH_CUSTOM_SOLUTION_PROP = "svygen_path";
	private static final String PRODUCT_APP_JSON = "productApp.json";

	/**
	 * Regenerates based on AI output on the existing project. Might also need to rename the solution.
	 */
	public static void regenerateSolutionFromAIContent(ServoyProject activeProject)
	{
		Solution editingSolution = activeProject.getEditingSolution();
		String templateAndAiGeneratedDir = (String)editingSolution.getCustomProperty(new String[] { SVYGEN_PATH_CUSTOM_SOLUTION_PROP });
		JSONObject aiGeneratedContent = getAIGeneratedJSON(templateAndAiGeneratedDir);

		// check to see if we need to rename the solution
		// TODO

		// start generating solution content
		generateSolutionSubContent(editingSolution, aiGeneratedContent, getTemplateReader(templateAndAiGeneratedDir));

		// TODO save editing solution changes to disk
	}


	private static JSONObject getAIGeneratedJSON(String templateAndAiGeneratedDir)
	{
		return new JSONObject(Utils.getTXTFileContent(new File(templateAndAiGeneratedDir, PRODUCT_APP_JSON)));
	}


	public static void createSolutionFromAIContent(String templateAndAiGeneratedDir)
	{
		JSONObject aiGeneratedContent = getAIGeneratedJSON(templateAndAiGeneratedDir);

		// TODO CREATE A NEW SOLUTION with name given by productApp.json, similar to how create new solution wizard does it
		Solution solution = null;

		// start generating solution content
		generateSolutionSubContent(solution, aiGeneratedContent,
			getTemplateReader(templateAndAiGeneratedDir));
	}


	private static TemplateForAIReader getTemplateReader(String templateAndAiGeneratedDir)
	{
		return new TemplateForAIReader(new File(new File(templateAndAiGeneratedDir, "templates"), "servoy-primitives"));
	}

	private static void generateSolutionSubContent(Solution solution, JSONObject aiGeneratedContent, TemplateForAIReader templateForAIReader)
	{
		// TODO

	}

}
