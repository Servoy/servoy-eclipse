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

package com.servoy.eclipse.designer.rfb.endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.server.ngclient.AngularFormGenerator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;

/**
 * Produces the stateless content needed to render the real runtime DOM of a form
 * without any open editor, client, session or websocket (SVY-21460).
 *
 * <p>All lookups use the active project's editing flattened solution and a form name that
 * is unique across it. Nothing here opens an editor or a runtime client. The three pieces of
 * content are meant to be served by {@link FormTemplateHttpEndpoint} and consumed by the
 * Angular {@code formtemplate} route:
 * <ul>
 * <li>{@link #getFormDataJS(String)} - the {@code AngularFormGenerator.generateJS()} form-state JSON;</li>
 * <li>{@link #getComponentSpecsJSON(String)} - the per-component client-side specs the form needs
 * (same shape {@code DesignerWebsocketSession} sends via {@code addComponentClientSideSpecs});</li>
 * <li>{@link #getSolutionStyleSheet()} - the active solution's CSS, produced the design-time way.</li>
 * </ul>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class HeadlessFormTemplateContent
{
	private FlattenedSolution getEditingFlattenedSolution()
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject == null) return null;
		return activeProject.getEditingFlattenedSolution();
	}

	/**
	 * Returns the {@code AngularFormGenerator.generateJS()}-shaped form-state JSON for the given form
	 * in the active editing flattened solution, headless (no editor, no client). Returns {@code null}
	 * if the form cannot be found.
	 */
	public String getFormDataJS(String formName) throws IOException
	{
		FlattenedSolution fs = getEditingFlattenedSolution();
		if (fs == null) return null;
		Form form = fs.getForm(formName);
		if (form == null) return null;
		Form flattenedForm = fs.getFlattenedForm(form);
		if (flattenedForm == null) return null;

		// match MobileExporter's headless generation which sets TESTING_MODE so no running client is needed
		String prevValue = Settings.getInstance().getProperty(Settings.TESTING_MODE, "false");
		try
		{
			Settings.getInstance().setProperty(Settings.TESTING_MODE, "true");
			AngularFormGenerator generator = new AngularFormGenerator(fs, flattenedForm, form.getName(), false, null);
			// no messages manager: a stable, data-free projection with raw i18n keys
			return generator.generateJS(new ServoyDataConverterContext(fs, null));
		}
		finally
		{
			Settings.getInstance().setProperty(Settings.TESTING_MODE, prevValue);
		}
	}

	/**
	 * Returns the per-component client-side specs (JSON object keyed by spec name) for the components
	 * used by the given form, in the same shape {@code TypesRegistry.addComponentClientSideSpecs} expects.
	 * Returns {@code null} if the form cannot be found or none of its components need client-side specs.
	 */
	public String getComponentSpecsJSON(String formName)
	{
		FlattenedSolution fs = getEditingFlattenedSolution();
		if (fs == null) return null;
		Form form = fs.getForm(formName);
		if (form == null) return null;
		Form flattenedForm = fs.getFlattenedForm(form);
		if (flattenedForm == null) return null;

		try
		{
			ServoyDataConverterContext context = new ServoyDataConverterContext(fs);
			FormWrapper wrapper = new FormWrapper(flattenedForm, flattenedForm.getName(), false, context, true, null);
			Collection<IFormElement> baseComponents = new ArrayList<IFormElement>(wrapper.getBaseComponents());

			Set<String> alreadyAdded = new HashSet<>();
			EmbeddableJSONWriter compSpecsToSend = null;
			for (IFormElement baseComponent : baseComponents)
			{
				FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
				// null-safe spec lookup: a form may reference a component whose spec is not available;
				// skip such components instead of throwing (would otherwise cause an HTTP 500 / NPE)
				WebObjectSpecification spec = fe.getWebComponentSpec(false);
				if (spec == null) continue;
				String specName = spec.getName();
				if (specName == null) continue;
				compSpecsToSend = appendComponentSpecIfNeeded(compSpecsToSend, alreadyAdded, spec, specName);
			}
			if (compSpecsToSend == null) return null;
			compSpecsToSend.endObject();
			return compSpecsToSend.toJSONString();
		}
		catch (RuntimeException e)
		{
			// a missing/unavailable component spec (or any assembly failure) must not produce a raw HTTP 500;
			// omit the specs blob rather than propagating
			Debug.error("Can't assemble component specs for form template '" + formName + "'", e);
			return null;
		}
	}

	private EmbeddableJSONWriter appendComponentSpecIfNeeded(EmbeddableJSONWriter compSpecsToSend, Set<String> alreadyAdded, WebObjectSpecification spec,
		String specName)
	{
		EmbeddableJSONWriter compSpecsToSendLocal = compSpecsToSend;
		if (!alreadyAdded.contains(specName))
		{
			alreadyAdded.add(specName);
			EmbeddableJSONWriter clSideTypesForThisComponent = WebComponentSpecProvider.getInstance().getClientSideTypeCache().getClientSideSpecFor(
				spec);
			if (clSideTypesForThisComponent != null)
			{
				if (compSpecsToSendLocal == null)
				{
					compSpecsToSendLocal = new EmbeddableJSONWriter();
					compSpecsToSendLocal.object();
				}
				compSpecsToSendLocal.key(specName).value(clSideTypesForThisComponent);
			}
		}
		return compSpecsToSendLocal;
	}

	/**
	 * Returns the active solution's stylesheet references as relative resource URLs, in the exact order
	 * and form the runtime NG client and the form designer use them: one URL per solution/module sheet,
	 * parent-last (the {@code PersistHelper.getOrderedStyleSheets} order reversed), preferring the
	 * {@code _ng2} variant, pointing at {@code MediaResourcesServlet}'s flattened-solution access path.
	 * <p>
	 * These are meant to be emitted as separate {@code <link rel="stylesheet">} elements (exactly like
	 * {@code DesignerWebsocketSession.getSolutionStyleSheets} does and like {@code ApplicationService.setStyleSheets}
	 * does at runtime), so the browser applies the same cascade - including {@code @import} rules, which only
	 * work at the top of their own sheet and would be dropped if the sheets were concatenated server-side.
	 * Designer-only sheets are not included. Returns an empty array if the solution has no stylesheets.
	 */
	public String[] getSolutionStyleSheetPaths()
	{
		FlattenedSolution fs = getEditingFlattenedSolution();
		if (fs == null) return new String[0];
		List<String> styleSheets = PersistHelper.getOrderedStyleSheets(fs);
		if (styleSheets == null || styleSheets.isEmpty()) return new String[0];
		// getOrderedStyleSheets returns them parent-first; the runtime/designer applies them reversed
		Collections.reverse(styleSheets);
		List<String> paths = new ArrayList<>(styleSheets.size());
		String solutionName = fs.getSolution().getName();
		for (String stylesheetName : styleSheets)
		{
			String name = stylesheetName;
			// prefer the _ng2 variant like the designer/runtime does
			int lastPoint = name.lastIndexOf('.');
			if (lastPoint > 0)
			{
				String ng2StylesheetName = name.substring(0, lastPoint) + "_ng2" + name.substring(lastPoint);
				if (fs.getMedia(ng2StylesheetName) != null)
				{
					name = ng2StylesheetName;
				}
			}
			paths.add("resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solutionName + "/" +
				name + "?t=" + Long.toHexString(System.currentTimeMillis()));
		}
		return paths.toArray(new String[0]);
	}
}
