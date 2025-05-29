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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RenameSolutionAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author acostescu
 */
public class AISolutionGenerator
{

	private static final String AI_GENERATION_TEMPLATE_PROVIDED_CONTENT_FOLLOWS = "\n\n// AI Generation template-provided content follows; DO NOT ADD ANY CUSTOM CSS AFTER THIS LINE AS IT WILL BE REMOVED when regenerating solution content";

	private static final String SERVOY_PRIMITIVES_TEMPLATE_DIR = "servoy-primitives";
	private static final String TEMPLATES_DIR = "templates";
	private static final String SVYGEN_PATH_CUSTOM_SOLUTION_PROP = "svygen_path";
	private static final String AI_GENERATED_APP_JSON = "application.json";

	/**
	 * Regenerates based on AI output on the existing project. Might also need to rename the solution.
	 */
	public static void generateSolutionFromAIContent(ServoyProject activeProject)
	{
		Solution editingSolution = activeProject.getEditingSolution();
		String templateAndAiGeneratedDir = (String)editingSolution.getCustomProperty(new String[] { SVYGEN_PATH_CUSTOM_SOLUTION_PROP });
		JSONObject aiGeneratedContent = getAIGeneratedJSON(templateAndAiGeneratedDir);

		// check to see if we need to rename the solution
		String solutionNameFromJSON = aiGeneratedContent.getString("projectName");
		if (!editingSolution.getName().equals(solutionNameFromJSON))
		{
			RenameSolutionAction.renameSolution(activeProject, solutionNameFromJSON, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), true);
			activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			editingSolution = activeProject.getEditingSolution();
		}

		// start generating solution content
		generateSolutionSubContent(editingSolution, aiGeneratedContent, getTemplateReader(templateAndAiGeneratedDir),
			activeProject.getEditingFlattenedSolution());

		// save editing solution changes to disk
		List<IPersist> allGeneratedPersists = editingSolution.getAllObjectsAsList();
		try
		{
			activeProject.saveEditingSolutionNodes(allGeneratedPersists.toArray(new IPersist[allGeneratedPersists.size()]), true, true);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}


	private static JSONObject getAIGeneratedJSON(String templateAndAiGeneratedDir)
	{
		return new JSONObject(Utils.getTXTFileContent(new File(templateAndAiGeneratedDir, AI_GENERATED_APP_JSON)));
	}


	public static Solution createSolutionFromAIContent(String templateAndAiGeneratedDir) throws Exception
	{
		JSONObject aiGeneratedContent = getAIGeneratedJSON(templateAndAiGeneratedDir);
		EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
		Solution solution = null;
		try
		{
			solution = (Solution)repository.createNewRootObject(aiGeneratedContent.optString("projectName", "new_ai_gen_solution"),
				IRepository.SOLUTIONS);
		}
		catch (RepositoryException e)
		{
			throw new Exception("Error creating new solution from AI generated content", e);
		}
		return solution;
	}


	private static TemplateForAIReader getTemplateReader(String templateAndAiGeneratedDir)
	{
		return new TemplateForAIReader(new File(new File(templateAndAiGeneratedDir, TEMPLATES_DIR), SERVOY_PRIMITIVES_TEMPLATE_DIR));
	}

	private static void generateSolutionSubContent(Solution solution, JSONObject aiGeneratedContent, TemplateForAIReader templateForAIReader,
		FlattenedSolution flattenedSolution)
	{
		// add css/less from templates into solution level css/less
		addSolutionCSSorLessContributionsFromTemplates(solution, templateForAIReader);

		JSONArray formsJSON = aiGeneratedContent.getJSONArray("forms");
		formsJSON.forEach((formJ) -> {
			JSONObject formJSON = (JSONObject)formJ;
			String formName = formJSON.getString("name");
			String formDatasource = formJSON.getString("datasource");
			String formClassName = formJSON.optString("styleClass", null);
			String styleToBeAddedToFormCssOrLess = formJSON.getJSONObject("style").getString("inline");
			JSONArray usedTemplates = formJSON.getJSONArray("children");

			try
			{
				Form form = solution.createNewForm(new ScriptNameValidator(flattenedSolution), null, formName, formDatasource, false, null);
				form.setResponsiveLayout(true);
				form.setStyleClass(formClassName);

				LayoutContainer rootLayoutContainer = form.createNewLayoutContainer();
				rootLayoutContainer.setPackageName("12grid");
				rootLayoutContainer.setSpecName("div");

				form.setFormCss(styleToBeAddedToFormCssOrLess);
				ResourcesUtils.createFileAndParentContainers(getFormCSSFile(form), new ByteArrayInputStream(styleToBeAddedToFormCssOrLess.getBytes("UTF8")),
					true);

				usedTemplates.forEach((tJSON) -> {
					JSONObject templateJSON = (JSONObject)tJSON;
					String templateRef = templateJSON.getString("templateRef");
					String templateName = templateJSON.getString("name");

					TemplateDefinition templateDef = templateForAIReader.getTemplateDefinition(templateRef);

					try
					{
						LayoutContainer templateLayoutContainer = rootLayoutContainer.createNewLayoutContainer();
						templateLayoutContainer.setPackageName("12grid");
						templateLayoutContainer.setSpecName("div");
						templateLayoutContainer.setName(templateName + "Div");

						WebComponent component = templateLayoutContainer.createNewWebComponent(templateName, templateDef.getRealSpecType());
						component.setStyleClass(templateDef.getStyleHookRoot());

						JSONObject propertiesJSON = templateJSON.getJSONObject("properties");

						propertiesJSON.keySet().forEach(aiPropertyName -> {
							String realPropertyName = templateDef.getRealPropertyFor(aiPropertyName);
							component.setProperty(realPropertyName, propertiesJSON.get(aiPropertyName));
						});
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}

				});
			}
			catch (RepositoryException | CoreException | UnsupportedEncodingException e)
			{
				ServoyLog.logError(e);
			}
		});
	}

	private static void addSolutionCSSorLessContributionsFromTemplates(Solution solution, TemplateForAIReader templateForAIReader)
	{
		int solutionCSSOrLessMediaID = solution.getStyleSheetID();
		Media solutionCSSOrLessMedia = solution.getMedia(solutionCSSOrLessMediaID);
		String oldCssOrLessContent;
		if (solutionCSSOrLessMedia == null)
		{
			try
			{
				solutionCSSOrLessMedia = solution.createNewMedia(new ScriptNameValidator(), solution.getName() + ".less");
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			solutionCSSOrLessMedia.setMimeType("text/css");
			oldCssOrLessContent = "";
			solution.setStyleSheetID(solutionCSSOrLessMedia.getID());
		}
		else
		{
			oldCssOrLessContent = new String(solutionCSSOrLessMedia.getMediaData(), Charset.forName("UTF-8"));
		}

		int previousAIGeneratedTemplatePRovidedContentStartIndex = oldCssOrLessContent.indexOf(AI_GENERATION_TEMPLATE_PROVIDED_CONTENT_FOLLOWS);
		if (previousAIGeneratedTemplatePRovidedContentStartIndex >= 0)
			oldCssOrLessContent = oldCssOrLessContent.substring(0, previousAIGeneratedTemplatePRovidedContentStartIndex);

		StringBuilder newCssOrLessContent = new StringBuilder(oldCssOrLessContent);
		newCssOrLessContent.append(AI_GENERATION_TEMPLATE_PROVIDED_CONTENT_FOLLOWS);

		templateForAIReader.getAllTemplateDefinitions().forEach(td -> {
			String templateCssOrLess = td.getTemplateStyleToAddToSolution();
			if (templateCssOrLess != null) newCssOrLessContent.append("\n" + templateCssOrLess);
		});

		solutionCSSOrLessMedia.setPermMediaData(newCssOrLessContent.toString().getBytes(Charset.forName("UTF-8")));
		Pair<String, String> mfp = SolutionSerializer.getFilePath(solutionCSSOrLessMedia, false);
		IFile mediaIFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(mfp.getLeft() + mfp.getRight()));
		try
		{
			ResourcesUtils.createOrWriteFile(mediaIFile, new ByteArrayInputStream(newCssOrLessContent.toString().getBytes("UTF8")),
				true);
		}
		catch (UnsupportedEncodingException | CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public static IFile getFormCSSFile(Form form)
	{
		Pair<String, String> formFilePath = SolutionSerializer.getFilePath(form, false);
		IFile file = ServoyModel.getWorkspace().getRoot()
			.getFile(new Path(formFilePath.getLeft() + form.getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION));
		return file;
	}

}
