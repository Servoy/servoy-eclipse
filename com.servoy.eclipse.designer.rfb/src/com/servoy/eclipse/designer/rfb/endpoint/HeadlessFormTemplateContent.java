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

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.AngularFormGenerator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
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
	/**
	 * Max nesting depth followed when collecting nested forms (SVY-21610), so a deeply nested - or pathologically
	 * constructed - form hierarchy cannot blow up the generated page size.
	 */
	private static final int MAX_NESTED_FORM_DEPTH = 5;

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
		return getFormDataJS(formName, false);
	}

	/**
	 * Returns the {@code AngularFormGenerator.generateJS()}-shaped form-state JSON for the given form in the active
	 * editing flattened solution, headless (no editor, no client). Returns {@code null} if the form cannot be found.
	 *
	 * @param sampleData SVY-21610: when true the property types additionally write synthetic, deterministic sample
	 *            values (a dataprovider value, 3 foundset rows) so the render shows data instead of empty component
	 *            chrome. When false the output is identical to the pre-SVY-21610 one.
	 */
	public String getFormDataJS(String formName, boolean sampleData) throws IOException
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
			ServoyDataConverterContext context = new ServoyDataConverterContext(fs, null);

			// SVY-21610: the generated JSON is an object keyed by form name, and the Angular route builds a FormCache
			// per key - so one blob can carry the requested form AND every form nested in it through a form-typed
			// property (a tabpanel tab, a tablesspanel, an accordion, a splitpane, a formcontainer). Without those the
			// nested render is gated out client-side: the generated template guards it with isFormAvailable(name),
			// which asks FormService.hasFormCacheEntry(name), and a form that was never generated has no cache entry.
			// The REQUESTED form is generated first and NOT best-effort: if it cannot be generated the caller must
			// still see that (a 404/500), exactly as before - swallowing it here would serve an empty page instead.
			AngularFormGenerator requestedGenerator = new AngularFormGenerator(fs, flattenedForm, form.getName(), false, true, sampleData, null);
			JSONObject merged = new JSONObject(requestedGenerator.generateJS(context));

			for (Form formToGenerate : collectFormsToGenerate(flattenedForm, fs))
			{
				// the requested form is already in 'merged' (it is always the first entry of the collected list)
				if (merged.has(formToGenerate.getName())) continue;

				// a NESTED form is best-effort: one broken nested form must not take the whole page down
				String generated = generateFormDataJS(formToGenerate, fs, context, sampleData);
				if (generated == null) continue;
				JSONObject generatedObject = new JSONObject(generated);
				for (String key : generatedObject.keySet())
				{
					// the requested form wins on a name clash (it cannot clash in practice - form names are unique
					// across the solution - but merging must be deterministic regardless)
					if (!merged.has(key)) merged.put(key, generatedObject.get(key));
				}
			}
			return merged.toString();
		}
		finally
		{
			Settings.getInstance().setProperty(Settings.TESTING_MODE, prevValue);
		}
	}

	/**
	 * Generates the {@code AngularFormGenerator.generateJS()} JSON for one NESTED form, or {@code null} when it cannot
	 * be generated. Best-effort by design: a single broken nested form must not take the whole page down, it just
	 * means that tab/container renders empty. The requested form is generated by the caller instead, deliberately
	 * without this shield so its failure still surfaces as an error response.
	 */
	private String generateFormDataJS(Form form, FlattenedSolution fs, ServoyDataConverterContext context, boolean sampleData)
	{
		try
		{
			Form flattenedForm = form instanceof FlattenedForm ? form : fs.getFlattenedForm(form);
			if (flattenedForm == null) return null;
			// SVY-21460: form-template mode (isDesigner=false, formTemplate=true) so custom-array / custom-object typed
			// properties (e.g. a datagrid's 'columns', a tabpanel's 'tabs') are written into the headless template JSON
			AngularFormGenerator generator = new AngularFormGenerator(fs, flattenedForm, form.getName(), false, true, sampleData, null);
			// no messages manager: a stable, data-free projection with raw i18n keys
			return generator.generateJS(context);
		}
		catch (IOException | RuntimeException e)
		{
			Debug.error("Can't generate the form-template JSON for nested form '" + form.getName() + "'", e);
			return null;
		}
	}

	/**
	 * Collects the requested form plus every form reachable from it through a {@code form}-typed property, breadth
	 * first, de-duplicated by form name and capped at {@link #MAX_NESTED_FORM_DEPTH} levels. The requested form is
	 * always first.
	 *
	 * <p>
	 * A visited set is required, not just nice to have: a tabpanel whose tab shows a form that itself contains a
	 * tabpanel pointing back is legal in Servoy and would otherwise recurse forever.
	 * </p>
	 *
	 * <p>
	 * All tabs are collected, not only the active one: which tab is visible is decided client-side from the model
	 * ({@code tabIndex}/{@code activeTabIndex}), so generating only the first tab would render a form wrong whenever
	 * another tab is selected by default. The extra cost is page size, not server round-trips.
	 * </p>
	 */
	private List<Form> collectFormsToGenerate(Form requestedForm, FlattenedSolution fs)
	{
		List<Form> result = new ArrayList<>();
		Set<String> visited = new HashSet<>();

		List<Form> currentLevel = new ArrayList<>();
		currentLevel.add(requestedForm);
		visited.add(requestedForm.getName());
		result.add(requestedForm);

		for (int depth = 0; depth < MAX_NESTED_FORM_DEPTH && !currentLevel.isEmpty(); depth++)
		{
			List<Form> nextLevel = new ArrayList<>();
			for (Form form : currentLevel)
			{
				for (Form nested : getNestedForms(form, fs))
				{
					if (visited.add(nested.getName()))
					{
						result.add(nested);
						nextLevel.add(nested);
					}
				}
			}
			currentLevel = nextLevel;
		}
		return result;
	}

	/**
	 * The forms referenced by {@code form}'s components through a {@code form}-typed property, including such
	 * properties declared inside a custom type (a tabpanel's {@code tabs} array is a custom type whose
	 * {@code containedForm} is the form-typed property).
	 *
	 * <p>
	 * Note that the stored value can be either a form NAME or a form UUID - a tabpanel tab stores the UUID - which is
	 * why resolution goes through {@link FormPropertyType#getForm(Object, FlattenedSolution)}-style lookup
	 * ({@code fs.getForm} then {@code fs.searchPersist}) rather than {@code fs.getForm} alone.
	 * </p>
	 */
	private List<Form> getNestedForms(Form form, FlattenedSolution fs)
	{
		List<Form> nested = new ArrayList<>();
		Form flattenedForm = form instanceof FlattenedForm ? form : fs.getFlattenedForm(form);
		if (flattenedForm == null) return nested;

		flattenedForm.acceptVisitor((IPersist persist) -> {
			if (persist instanceof IBasicWebObject webObject)
			{
				collectFormsFromWebObject(webObject, fs, nested);
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		});
		return nested;
	}

	/**
	 * Adds every form referenced by one web object's form-typed properties, then recurses into its
	 * {@link WebCustomType} children so form-typed properties nested in a custom type (e.g. a tabpanel tab) are found
	 * too - the same shape {@code ServoyFormBuilder.addWebComponentMissingReferences} uses to walk custom-type
	 * children.
	 */
	private void collectFormsFromWebObject(IBasicWebObject webObject, FlattenedSolution fs, List<Form> nested)
	{
		PropertyDescription pd = getPropertyDescription(webObject);
		if (pd != null)
		{
			for (PropertyDescription formProperty : pd.getProperties(FormPropertyType.INSTANCE))
			{
				Form referenced = resolveForm(webObject.getProperty(formProperty.getName()), fs);
				if (referenced != null) nested.add(referenced);
			}
		}

		webObject.getAllObjects().forEachRemaining(child -> {
			if (child instanceof WebCustomType customType)
			{
				collectFormsFromWebObject(customType, fs, nested);
			}
		});
	}

	/**
	 * The property description that declares a web object's own properties: the component's spec for a
	 * {@link WebComponent}, the custom type's definition for a {@link WebCustomType}.
	 */
	private PropertyDescription getPropertyDescription(IBasicWebObject webObject)
	{
		if (webObject instanceof WebCustomType customType)
		{
			IPropertyType< ? > type = customType.getPropertyDescription() != null ? customType.getPropertyDescription().getType() : null;
			return type instanceof ICustomType< ? > custom ? custom.getCustomJSONTypeDefinition() : null;
		}
		if (webObject instanceof WebComponent webComponent)
		{
			return WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());
		}
		return null;
	}

	/** Resolves a form-typed property value, which may be a form name or a form UUID, to a {@link Form}. */
	private Form resolveForm(Object value, FlattenedSolution fs)
	{
		if (value == null) return null;
		String formId = value.toString();
		if (formId.isEmpty()) return null;

		Form form = fs.getForm(formId);
		if (form == null)
		{
			IPersist persist = fs.searchPersist(formId);
			if (persist instanceof Form foundForm) form = foundForm;
		}
		return form;
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

			Set<String> alreadyAdded = new HashSet<>();
			EmbeddableJSONWriter compSpecsToSend = null;
			// SVY-21610: union the specs across the requested form AND its nested forms (the same set getFormDataJS
			// generates), otherwise a component that only appears inside a tab has no client-side spec and its typed
			// properties silently fail to convert when that form's cache is built.
			for (Form formToScan : collectFormsToGenerate(flattenedForm, fs))
			{
				Form flattenedToScan = formToScan instanceof FlattenedForm ? formToScan : fs.getFlattenedForm(formToScan);
				if (flattenedToScan == null) continue;
				FormWrapper wrapper = new FormWrapper(flattenedToScan, flattenedToScan.getName(), false, context, true, null);
				Collection<IFormElement> baseComponents = new ArrayList<IFormElement>(wrapper.getBaseComponents());

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
