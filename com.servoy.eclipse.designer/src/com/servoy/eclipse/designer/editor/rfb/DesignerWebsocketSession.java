/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import java.awt.Point;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecificationBuilder;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.IWindow;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.impl.ClientService;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;

import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.ChildWebComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportsIndexedChildren;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.AngularFormGenerator;
import com.servoy.j2db.server.ngclient.ChildrenJSONGenerator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator.DesignProperties;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.server.ngclient.template.PartWrapper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class DesignerWebsocketSession extends BaseWebsocketSession implements IServerService
{
	public static final String EDITOR_CONTENT_SERVICE = "$editorContentService";

	private static final WebObjectSpecification EDITOR_CONTENT_SERVICE_SPECIFICATION = new WebObjectSpecificationBuilder().withName(
		EDITOR_CONTENT_SERVICE).withPackageType(IPackageReader.WEB_SERVICE).build();

	private final Form form;

	private final BaseVisualFormEditor editor;

	private final Set<String> clientSideSpecs = new HashSet<>();

	public DesignerWebsocketSession(WebsocketSessionKey sessionKey, BaseVisualFormEditor editor)
	{
		super(sessionKey);
		this.form = editor.getForm();
		this.editor = editor;
		registerServerService("$editor", this);
	}

	public String[] getSolutionStyleSheets(FlattenedSolution fs)
	{
		List<String> styleSheets = PersistHelper.getOrderedStyleSheets(fs);
		if (styleSheets != null && styleSheets.size() > 0)
		{
			Collections.reverse(styleSheets);
			for (int i = 0; i < styleSheets.size(); i++)
			{
				String stylesheetName = styleSheets.get(i);
				int lastPoint = stylesheetName.lastIndexOf('.');
				String ng2StylesheetName = stylesheetName.substring(0, lastPoint) + "_ng2" + stylesheetName.substring(lastPoint);
				Media media = fs.getMedia(ng2StylesheetName);
				if (media != null)
				{
					stylesheetName = ng2StylesheetName;
				}
				styleSheets.set(i, "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + fs.getSolution().getName() + "/" +
					stylesheetName + "?t=" + Long.toHexString(System.currentTimeMillis()));
			}
		}
		return styleSheets.toArray(new String[0]);
	}


	@Override
	protected IClientService createClientService(String name)
	{
		if (EDITOR_CONTENT_SERVICE.equals(name))
		{
			return new ClientService(EDITOR_CONTENT_SERVICE, EDITOR_CONTENT_SERVICE_SPECIFICATION, this);
		}
		return super.createClientService(name);
	}


	@Override
	public Locale getLocale()
	{
		return Locale.getDefault();
	}

	// session will be destroyed when editor is closed
	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public boolean checkForWindowActivity()
	{
		return false;
	}

	// designer never expires
	@Override
	public boolean shouldTest()
	{
		return false;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(form.getSolution().getName());
		FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
		Form flattenedForm = fs.getFlattenedForm(form);

		ServoyDataConverterContext context = new ServoyDataConverterContext(fs);
		FormWrapper wrapper = new FormWrapper(flattenedForm, flattenedForm.getName(), false, context, true, null);
		switch (methodName)
		{
			case "getData" :
			{
				boolean isNG2 = args.optBoolean("ng2", false);
				Collection<IFormElement> baseComponents = new ArrayList<IFormElement>(wrapper.getBaseComponents());

				// send any client-side-types that the form designer will neeed for this form's components
				EmbeddableJSONWriter compSpecsToSend = null;
				for (IFormElement baseComponent : baseComponents)
				{
					FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
					String specName = fe.getWebComponentSpec().getName();
					compSpecsToSend = sendComponentSpecToClientIfNeeded(compSpecsToSend, fe, specName);
				}
				if (compSpecsToSend != null)
				{
					compSpecsToSend.endObject();
					getTypesRegistryService().addComponentClientSideSpecs(compSpecsToSend);
				}

				// send the rest of the form data
				if (isNG2)
				{
					LayoutContainer zoomedInContainer = null;
					if (editor != null && editor.getGraphicaleditor() instanceof RfbVisualFormEditorDesignPage &&
						((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).getShowedContainer() instanceof LayoutContainer)
					{
						zoomedInContainer = (LayoutContainer)((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).getShowedContainer();
					}
					return new AngularFormGenerator(fs, flattenedForm, form.getName(), true, zoomedInContainer).generateJS(new ServoyDataConverterContext(fs,
						Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_I18N_VALUES_IN_ANGULAR_DESIGNER)
							? ServoyModelFinder.getServoyModel().getMessagesManager() : null));
				}
				else
				{
					JSONWriter writer = new JSONStringer();
					writer.object();
					writer.key("formProperties");
					writer.value(new JSONObject(wrapper.getPropertiesString()));
					writer.key("parentUuid");
					writer.value(form.extendsForm != null ? form.extendsForm.getUUID() : null);
					Collection<IFormElement> deleted = Collections.emptyList();
					sendComponents(fs, writer, baseComponents, deleted, false);
					writer.key("solutionProperties");
					writer.object();
					writer.key("styleSheets");
					writer.value(getSolutionStyleSheets(ServoyModelFinder.getServoyModel().getFlattenedSolution()));
					writer.endObject();
					generateParts(flattenedForm, context, writer, wrapper.getParts());
					writer.endObject();
					return writer.toString();
				}
			}
			case "getTemplate" :
			{
				String name = args.optString("name", null);
				StringWriter htmlTemplate = new StringWriter(512);
				PrintWriter w = new PrintWriter(htmlTemplate);
				UUID parentuuid = null;

				boolean componentFound = false;
				boolean responsive = form.isResponsiveLayout();
				String mainContainerID = null;
				if (editor != null && editor.getGraphicaleditor() instanceof RfbVisualFormEditorDesignPage)
				{
					AbstractContainer container = ((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).getShowedContainer();
					if (container instanceof LayoutContainer)
					{
						responsive = !CSSPositionUtils.isCSSPositionContainer((LayoutContainer)container);
						mainContainerID = container.getUUID().toString();
					}
				}
				if (name != null)
				{
					Collection<IFormElement> baseComponents = wrapper.getBaseComponents();
					for (IFormElement baseComponent : baseComponents)
					{
						FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
						if (Utils.equalObjects(fe.getDesignId(), name) || Utils.equalObjects(fe.getName(), name))
						{
							boolean isInCSSPositionContainer = false;
							ISupportChilds realParent = PersistHelper.getRealParent(baseComponent);
							if (realParent instanceof LayoutContainer)
							{
								isInCSSPositionContainer = CSSPositionUtils.isCSSPositionContainer((LayoutContainer)realParent);
							}
							if (!responsive || isInCSSPositionContainer)
								FormLayoutGenerator.generateFormElementWrapper(w, fe, flattenedForm, form.isResponsiveLayout());
							FormLayoutGenerator.generateFormElement(w, fe, flattenedForm);
							if (!responsive || isInCSSPositionContainer) FormLayoutGenerator.generateEndDiv(w);
							if (responsive)
							{
								IPersist parent = ((ISupportExtendsID)fe.getPersistIfAvailable()).getRealParent();
								parentuuid = parent.getUUID();
							}

							componentFound = true;
							break;
						}
					}
				}
				else
				{
					String layoutId = args.optString("layoutId");
					if (layoutId != null)
					{
						IPersist child = flattenedForm.findChild(UUID.fromString(layoutId));
						if (child instanceof LayoutContainer)
						{
							componentFound = true;
							IPersist parent = ((ISupportExtendsID)child).getRealParent();
							parentuuid = parent instanceof Form ? null : parent.getUUID();
							FormLayoutStructureGenerator.generateLayoutContainer((LayoutContainer)child, flattenedForm, context.getSolution(), w,
								new DesignProperties(mainContainerID), FormElementHelper.INSTANCE);
						}
					}
				}
				// no component is found, very likely a ghost, re render those
				if (!componentFound)
				{
					JSONWriter writer = new JSONStringer();
					writer.object();
					writer.key("renderGhosts");
					writer.value(true);
					writer.endObject();
					return writer.toString();
				}
				w.flush();
				JSONWriter writer = new JSONStringer();
				writer.object();
				writer.key("template");
				writer.value(htmlTemplate.toString());
				if (parentuuid != null)
				{
					writer.key("parentId");
					writer.value(parentuuid);
				}
				writer.endObject();
				return writer.toString();
			}
			case "getStyleSheets" :
			{
				return getDesignerStyleSheets(ServoyModelFinder.getServoyModel().getFlattenedSolution());
			}
		}
		return null;
	}


	private String[] getDesignerStyleSheets(FlattenedSolution fs)
	{
		TreeSet<String> designCssLibs = new TreeSet<>();
		SpecProviderState specProviderState = WebComponentSpecProvider.getSpecProviderState();
		for (PackageSpecification<WebLayoutSpecification> entry : specProviderState.getLayoutSpecifications().values())
		{
			Object responsiveLib = entry.getManifest().getMainAttributes().getValue("Responsive-Layout");
			if (form.isResponsiveLayout() && "True".equals(responsiveLib))
			{
				List<String> libs = entry.getNg2CssDesignLibrary();
				if (libs != null)
				{
					designCssLibs.addAll(libs);
				}
			}
		}
		// also add the solution css
		designCssLibs.addAll(Arrays.asList(getSolutionStyleSheets(fs)));
		return designCssLibs.toArray(new String[designCssLibs.size()]);
	}

	/**
	 * @param flattenedForm
	 * @param context
	 * @param writer
	 * @param parts
	 */
	private void generateParts(Form flattenedForm, ServoyDataConverterContext context, JSONWriter writer, Collection<Part> parts)
	{
		writer.key("parts");
		writer.object();
		for (Part part : parts)
		{
			PartWrapper partWrapper = new PartWrapper(part, flattenedForm, context, true);
			writer.key(partWrapper.getName() + "Style");
			writer.value(partWrapper.getStyle());
		}
		writer.endObject();
	}

	/**
	 * Function that check recursively childrens of a LayoutContainer .
	 * If LayoutContainer childrens contains BaseComponent element, will call parseFormElements and will return it's value.
	 *
	 * @return ghost
	 */
	private boolean checkLayoutHierarchyRecursively(IPersist persist, Map<IPersist, List<LayoutContainer>> persistContainers, Set<IFormElement> components,
		Set<IFormElement> compAttributes, Set<IFormElement> deletedComponents, boolean formComponentChild, Set<IFormElement> refreshTemplate,
		Set<String> updatedFormComponentsDesignId, Set<IFormElement> formComponentsComponents, boolean renderGhosts, FlattenedSolution fs,
		Map<String, String> childLayoutContainerToParentFormComponentComponentDesignIdMap, boolean skipComponents)
	{
		boolean ghost = false;
		List<LayoutContainer> containers = persistContainers.get(persist);
		if (persist instanceof LayoutContainer && !containers.contains(persist))
		{
			containers.add((LayoutContainer)persist);
			List<IPersist> children = ((LayoutContainer)persist).getAllObjectsAsList();
			for (IPersist element : children)
			{
				if (element != null)
				{
					persistContainers.put(element, containers);
					boolean auxGhost = checkLayoutHierarchyRecursively(element, persistContainers, components, compAttributes, deletedComponents,
						formComponentChild,
						refreshTemplate, updatedFormComponentsDesignId, formComponentsComponents, renderGhosts, fs,
						childLayoutContainerToParentFormComponentComponentDesignIdMap, skipComponents);
					if (auxGhost) ghost = auxGhost;
				}
			}
			return ghost;
		}
		if (!skipComponents && persist instanceof IFormElement && !components.contains(persist) && !compAttributes.contains(persist))
		{
			ghost = parseIFormElement((IFormElement)persist, components, compAttributes, deletedComponents, formComponentChild, refreshTemplate,
				updatedFormComponentsDesignId, formComponentsComponents, renderGhosts, fs, persistContainers,
				childLayoutContainerToParentFormComponentComponentDesignIdMap);
			return ghost;
		}
		return false;
	}

	/**
	 * Function that parse an IPersist when instance is IFormElement
	 *
	 * @return true is it's a 'ghost' from the editor's point of view
	 */
	private boolean parseIFormElement(final IFormElement formElementPersist, Set<IFormElement> baseComponents, Set<IFormElement> compAttributes,
		Set<IFormElement> deletedComponents, boolean formComponentChild, Set<IFormElement> refreshTemplate, Set<String> updatedFormComponentsDesignId,
		Set<IFormElement> formComponentsComponents, boolean renderGhosts, FlattenedSolution fs, Map<IPersist, List<LayoutContainer>> formContainers,
		Map<String, String> childLayoutContainerToParentFormComponentComponentDesignIdMap)
	{
		boolean ghost = renderGhosts;
		if (formElementPersist.getGroupID() != null)
		{
			compAttributes.add(formElementPersist);
		}

		IPersist superPersist = PersistHelper.getSuperPersist(formElementPersist);
		if (superPersist instanceof IFormElement)
		{
			deletedComponents.add((IFormElement)superPersist);
			if (formElementPersist instanceof WebComponent)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
					.getWebObjectSpecification(((WebComponent)formElementPersist).getTypeName());
				ghost = !spec.getProperties(FormComponentPropertyType.INSTANCE).isEmpty();
			}
		}

		ISupportChilds parent = formElementPersist.getParent();
		if (formComponentChild || parent.getChild(formElementPersist.getUUID()) != null)
		{
			if (parent instanceof AbstractContainer)
			{
				baseComponents.add(formElementPersist);
			}
			else if (parent instanceof Form)
			{
				baseComponents.add(formElementPersist);
			}
			else while (parent instanceof BaseComponent)
			{
				if (parent.getParent() instanceof Form)
				{
					baseComponents.add((IFormElement)parent);
					IPersist oldPersist = ServoyModelFinder.getServoyModel().getFlattenedSolution().searchPersist(formElementPersist.getUUID());
					if (oldPersist == null) // a new child was added, force parent refresh
					{
						refreshTemplate.add((IFormElement)parent);
					}
					break;
				}
				else
				{
					parent = parent.getParent();
				}
			}
			if (formElementPersist instanceof Field)
			{
				Field oldField = (Field)ServoyModelFinder.getServoyModel().getFlattenedSolution().searchPersist(formElementPersist.getUUID());
				if (oldField != null && ((Field)formElementPersist).getDisplayType() != oldField.getDisplayType())
				{
					refreshTemplate.add(formElementPersist);
				}
			}
			if (formElementPersist instanceof ChildWebComponent || formElementPersist.getParent() instanceof Portal)
			{
				ghost = true;
			}
			if (!form.isResponsiveLayout())
			{
				int formHeight = form.getParts().hasNext() ? form.getSize().height : 0;
				Point location = CSSPositionUtils.getLocation(formElementPersist);
				if (((location.x > form.getWidth()) || (location.y > formHeight)))
				{
					ghost = true;
				}
			}
			boolean fcGhosts = checkFormComponents(updatedFormComponentsDesignId, formComponentsComponents,
				FormElementHelper.INSTANCE.getFormElement(formElementPersist, fs, null, true), fs, new HashSet<String>(), formContainers,
				childLayoutContainerToParentFormComponentComponentDesignIdMap);
			if (fcGhosts) ghost = fcGhosts;
		}
		else
		{
			deletedComponents.add(formElementPersist);
			// if it has parent, make sure it refreshes
			while (parent instanceof BaseComponent)
			{
				if (parent.getParent() instanceof Form)
				{
					baseComponents.add((IFormElement)parent);
					refreshTemplate.add((IFormElement)parent);
					break;
				}
				else
				{
					parent = parent.getParent();
				}
			}
		}
		if (formElementPersist instanceof ISupportsIndexedChildren)
		{
			ISupportsIndexedChildren supportChilds = (ISupportsIndexedChildren)formElementPersist;
			ghost = ghost || supportChilds.getAllObjects().hasNext();
		}
		return ghost;
	}

	/**
	 * @param persists from what I can see, "persists" will always be from this direct form,
	 * so not persists from nested form component components (those result in a full refresh
	 * of this editor; see code in BaseVisualFormEditor.persistChanges(Collection<IPersist>))
	 */
	public String getComponentsJSON(FlattenedSolution fs, Set<IPersist> persists)
	{
		Set<IFormElement> baseComponents = new HashSet<>();
		Set<IFormElement> refreshTemplate = new HashSet<>();
		Set<IFormElement> deletedComponents = new HashSet<>();
		Set<String> updatedFormComponentsDesignId = new HashSet<>();
		Set<IFormElement> formComponentsComponents = new HashSet<>();
		Set<LayoutContainer> deletedLayoutContainers = new HashSet<>();
		Map<String, String> childLayoutContainerToParentFormComponentComponentDesignIdMap = new HashMap<String, String>();
		Set<Part> parts = new HashSet<>();
		List<LayoutContainer> containers = new ArrayList<>(); // so this is about LayoutContainer just from current form, not nested in form component components - see @param persists comment above
		Map<IPersist, List<LayoutContainer>> formComponentContainers = new HashMap<>();
		Set<IFormElement> compAttributes = new HashSet<>();
		boolean renderGhosts = editor.isRenderGhosts();
		editor.setRenderGhosts(false);
		AbstractContainer showedContainer = ((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).getShowedContainer();
		for (IPersist persist : persists)
		{
			formComponentContainers.put(persist, containers);
			boolean formComponentChild = false;
			if (persist instanceof WebFormComponentChildType)
			{
				IPersist formComponent = persist.getParent();
				if (formComponent instanceof IFormElement)
				{
					String elementName = getFixedFormElementName(
						formComponent.getUUID().toString() + "$" + ((WebFormComponentChildType)persist).getKey().replace('.', '$'));
					persist = getFormComponentElement(fs, (IFormElement)formComponent, elementName);
					if (persist == null) continue;
					formComponentChild = true;
				}
			}
			if (persist instanceof IFormElement)
			{
				boolean newRenderGhosts = parseIFormElement((IFormElement)persist, baseComponents, compAttributes, deletedComponents, formComponentChild,
					refreshTemplate,
					updatedFormComponentsDesignId, formComponentsComponents, renderGhosts, fs, formComponentContainers,
					childLayoutContainerToParentFormComponentComponentDesignIdMap);
				renderGhosts = renderGhosts || newRenderGhosts || formHasGhosts(persist);
			}
			else if (persist instanceof Part)
			{
				parts.add((Part)persist);
			}
			else if (persist instanceof LayoutContainer)
			{
				IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
				if (superPersist instanceof LayoutContainer)
				{
					deletedLayoutContainers.add((LayoutContainer)superPersist);
				}
				if (persist.getParent().getChild(persist.getUUID()) != null)
				{
					boolean newRenderGhosts = checkLayoutHierarchyRecursively(persist, formComponentContainers, baseComponents,
						compAttributes, deletedComponents,
						formComponentChild,
						refreshTemplate, updatedFormComponentsDesignId, formComponentsComponents, renderGhosts, fs,
						childLayoutContainerToParentFormComponentComponentDesignIdMap, false);
					renderGhosts = renderGhosts || newRenderGhosts || formHasGhosts(persist);
				}
				else
				{
					deletedLayoutContainers.add((LayoutContainer)persist);
				}
				if (deletedLayoutContainers.contains(persist) && persist.equals(showedContainer))
				{
					//showed container was deleted, need to zoom out
					((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).showContainer(null);
					return null;
				}
			}
			else
			{
				// if it is not a base component then it is a child thing, very likely the ghost must be refreshed.
				renderGhosts = true;
				ISupportChilds parent = persist.getParent();
				// do add the child base component so that it gets this new update
				while (parent != null && !(parent instanceof Form))
				{
					if (parent instanceof BaseComponent baseComponentParent)
					{
						baseComponents.add(baseComponentParent);
						IPersist superPersist = PersistHelper.getSuperPersist(baseComponentParent);
						if (superPersist instanceof IFormElement superPersistFormElement)
						{
							deletedComponents.add(superPersistFormElement);
						}
						parent = null;
					}
					else parent = parent.getParent();
				}
			}

		}

		if (formComponentsComponents.size() > 0)
		{
			baseComponents.addAll(formComponentsComponents);
		}

		EmbeddableJSONWriter compSpecsToSend = null;
		for (IFormElement baseComponent : baseComponents)
		{
			FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
			String specName = fe.getWebComponentSpec().getName();
			compSpecsToSend = sendComponentSpecToClientIfNeeded(compSpecsToSend, fe, specName);
		}
		if (compSpecsToSend != null)
		{
			compSpecsToSend.endObject();
			final EmbeddableJSONWriter clientSideSpecsToSend = compSpecsToSend;
			CurrentWindow.runForWindow(new WebsocketSessionWindows(this), new Runnable()
			{
				@Override
				public void run()
				{
					getTypesRegistryService().addComponentClientSideSpecs(clientSideSpecsToSend);
				}
			});
		}

		JSONWriter writer = new JSONStringer();
		writer.object();
		sendComponents(fs, writer, baseComponents, deletedComponents, true);

		if (formComponentsComponents.size() > 0)
		{
			writer.key("formComponentsComponents");
			writer.array();
			for (IFormElement fe : formComponentsComponents)
			{
				FormElement formElement = FormElementHelper.INSTANCE.getFormElement(fe, fs, null, true);
				writer.value(formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName());
			}
			writer.endArray();
		}

		if (updatedFormComponentsDesignId.size() > 0)
		{
			writer.key("updatedFormComponentsDesignId");
			writer.array();
			for (String id : updatedFormComponentsDesignId)
			{
				writer.value(id);
			}
			writer.endArray();
		}

		if (renderGhosts)
		{
			writer.key("renderGhosts");
			writer.value(true);
		}
		if (parts.size() > 0)
		{
			generateParts(fs.getFlattenedForm(form), new ServoyDataConverterContext(fs), writer, parts);
		}
		if (containers.size() > 0)
		{
			LayoutContainer[] containersA = containers.toArray(new LayoutContainer[containers.size()]);
			Arrays.sort(containersA, new Comparator<LayoutContainer>()
			{
				@Override
				public int compare(LayoutContainer o1, LayoutContainer o2)
				{
					if (o1 != o2)
					{
						if (o1.findChild(o2.getUUID()) != null) return 1;
						else if (o2.findChild(o1.getUUID()) != null) return -1;
					}
					return 0;
				}
			});

			writer.key("containers");
			writer.array();
			List<LayoutContainer> containersList = Arrays.asList(containersA);
			Collections.reverse(containersList);
			for (LayoutContainer container : containersList)
			{
				// TODO what the send over, if new then just the id? but what about the properties?
				Map<String, String> attributes = container.getMergedAttributes();
				writer.object();
				writer.key("uuid");
				writer.value(container.getUUID().toString());
				for (Entry<String, String> attribute : attributes.entrySet())
				{
					String key = attribute.getKey();
					String value = attribute.getValue();

					if ("class".equals(key))
					{
						WebLayoutSpecification spec = null;
						if (container.getPackageName() != null)
						{
							PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState()
								.getLayoutSpecifications()
								.get(
									container.getPackageName());
							if (pkg != null)
							{
								spec = pkg.getSpecification(container.getSpecName());
							}
						}
						List<String> containerStyleClasses = FormLayoutStructureGenerator.getStyleClassValues(spec, container.getCssClasses());

						writer.key("svy-layout-class");
						value = containerStyleClasses.stream().collect(Collectors.joining(" "));
						writer.value(value);

						writer.key("svy-solution-layout-class");
						value = Arrays.stream(container.getCssClasses().split(" "))
							.filter(cls -> !containerStyleClasses.contains(cls))
							.collect(
								Collectors.joining(" "));
						writer.value(value);

						writer.key("svy-title");
						writer.value(FormLayoutStructureGenerator.getLayouContainerTitle(container));
					}
					else
					{
						writer.key(key);
						writer.value(value);
					}
				}
				writer.endObject();
			}
			writer.endArray();

			writer.key("ng2containers");
			writer.array();
			for (LayoutContainer container : containersList)
			{
				writer.object();
				ChildrenJSONGenerator.writeLayoutContainer(writer, container, null, form, true, fs);
				writer.endObject();
			}
			writer.endArray();

		}

		writer.key("formComponentContainers");
		writer.object();
		formComponentContainers.entrySet().forEach(entry -> {
			if (entry.getValue() != containers && entry.getKey() instanceof IFormElement fe)
			{
				FormElement formElement = FormElementHelper.INSTANCE.getFormElement(fe, fs, null, true);
				String fcID = formElement.getName(formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName());
				if (updatedFormComponentsDesignId.contains(fcID))
				{
					writer.key(formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName());
					writer.array();
					for (LayoutContainer container : entry.getValue())
					{
						writer.object();
						ChildrenJSONGenerator.writeLayoutContainer(writer, container, null, form, true, fs);
						writer.endObject();
						if (!containers.contains(container)) containers.add(container);
					}
					writer.endArray();
				}
			}
		});
		writer.endObject();
		if (deletedLayoutContainers.size() > 0)
		{
			writer.key("deletedContainers");
			writer.array();
			for (LayoutContainer container : deletedLayoutContainers)
			{
				writer.value(container.getUUID().toString());
			}
			writer.endArray();
		}

		if (refreshTemplate.size() > 0)
		{
			writer.key("refreshTemplate");
			writer.array();
			for (IFormElement persist : refreshTemplate)
			{
				writer.value(persist.getUUID().toString());
			}
			writer.endArray();
		}

		if (compAttributes.size() > 0)
		{
			writer.key("compAttributes");
			writer.object();
			for (IFormElement persist : compAttributes)
			{
				writer.key(persist.getUUID().toString());
				writer.object();
				writer.key("group-id");
				writer.value(persist.getGroupID());
				writer.endObject();
			}
			writer.endObject();
		}

		ArrayList<IPersist> componentsWithParents = new ArrayList<IPersist>();
		componentsWithParents.addAll(baseComponents);
		componentsWithParents.addAll(containers);
		if (componentsWithParents.size() > 0)
		{
			writer.key("childParentMap");
			writer.object();
			for (IPersist p : componentsWithParents)
			{
				ISupportChilds parent = ((ISupportExtendsID)p).getRealParent();
				String uuid = p.getUUID().toString();
				if (formComponentsComponents.contains(p))
				{
					FormElement formElement = FormElementHelper.INSTANCE.getFormElement((IFormElement)p, fs, null, true);
					uuid = formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName();
				}
				writer.key(uuid);
				writer.object();
				writer.key("uuid");
				if (parent instanceof Form)
				{
					// in case the layout container is the root one from inside a form component component, it's parent is that form component component, not the main form
					// if it is the main form, it will write null here
					writer.value(childLayoutContainerToParentFormComponentComponentDesignIdMap.containsKey(uuid)
						? childLayoutContainerToParentFormComponentComponentDesignIdMap.get(uuid) : null);
				}
				else
				{
					// parent is another layout container
					writer.value(parent.getUUID().toString());
				}
				writer.key("location");
				if (p instanceof ISupportBounds && parent instanceof AbstractContainer && form.isResponsiveLayout())
				{
					writer.value(((ISupportBounds)p).getLocation().x);
				}
				else
				{
					writer.value(-1);
				}
				writer.key("formIndex");
				if (p instanceof IFormElement)
				{
					writer.value(((IFormElement)p).getFormIndex());
				}
				else
				{
					writer.value(-1);
				}
				writer.endObject();
			}
			writer.endObject();
		}

		writer.endObject();
		return writer.toString();
	}

	private boolean formHasGhosts(IPersist persist)
	{
		Form frm = null;
		IPersist parent = persist.getParent();
		while (parent != null && frm == null)
		{
			if (parent instanceof Form) frm = (Form)parent;
			parent = parent.getParent();
		}
		if (frm == null) return false;

		boolean hasGhost = false;
		List<IFormElement> allElements = frm.getFlattenedFormElementsAndLayoutContainers()
			.stream()
			.filter(IFormElement.class::isInstance)
			.map(IFormElement.class::cast)
			.toList();
		for (IFormElement element : allElements)
		{
			if (element instanceof WebComponent webComponent)
			{
				Iterator<IPersist> it = webComponent.getAllObjects();
				while (it.hasNext() && !hasGhost)
				{
					IPersist child = it.next();
					if (child instanceof WebCustomType) hasGhost = true;
				}
			}
			if (hasGhost) break;
		}

		return hasGhost;
	}

	private boolean checkFormComponents(Set<String> updatedFormComponentsDesignId, Set<IFormElement> formComponentsComponents, FormElement formElement,
		FlattenedSolution fs, HashSet<String> forms, Map<IPersist, List<LayoutContainer>> formContainers,
		Map<String, String> childLayoutContainerToParentFormComponentComponentDesignIdMap)
	{
		boolean ghost = false;
		WebObjectSpecification spec = formElement.getWebComponentSpec();
		if (spec != null)
		{
			Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
			if (properties.size() > 0)
			{
				for (PropertyDescription pd : properties)
				{
					Object propertyValue = formElement.getPropertyValue(pd.getName());
					Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
					if (frm == null) continue;
					if (!forms.add(frm.getName()))
					{
						Debug.error("recursive reference found between (List)FormComponents: " + forms);
						continue;
					}
					// this is a form container so give it is own list
					ArrayList<LayoutContainer> value = new ArrayList<LayoutContainer>();
					formContainers.put(formElement.getPersistIfAvailable(), value);
					updatedFormComponentsDesignId.add(
						formElement.getName(formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName()));
					FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, (JSONObject)propertyValue, frm, fs);
					for (FormElement element : cache.getFormComponentElements())
					{
						formComponentsComponents.add((IFormElement)element.getPersistIfAvailable());
						if (element.getPersistIfAvailable() instanceof ISupportsIndexedChildren &&
							((ISupportsIndexedChildren)element.getPersistIfAvailable()).getAllObjects().hasNext())
						{
							ghost = true;
						}
						formContainers.put(element.getPersistIfAvailable(), value);
						boolean fcGhosts = checkFormComponents(updatedFormComponentsDesignId, formComponentsComponents, element, fs, forms, formContainers,
							childLayoutContainerToParentFormComponentComponentDesignIdMap);
						if (fcGhosts) ghost = fcGhosts;
					}
					if (frm.isResponsiveLayout() || frm.containsResponsiveLayout())
					{
						Iterator<LayoutContainer> it = frm.getAllLayoutContainers();
						while (it.hasNext())
						{
							LayoutContainer container = it.next();
							formContainers.put(container, value);
							childLayoutContainerToParentFormComponentComponentDesignIdMap.put(container.getUUID().toString(),
								formElement.getDesignId());
							boolean fcGhosts = checkLayoutHierarchyRecursively(container, formContainers, formComponentsComponents,
								formComponentsComponents,
								formComponentsComponents,
								isValid(), formComponentsComponents, updatedFormComponentsDesignId, formComponentsComponents, isValid(), fs,
								childLayoutContainerToParentFormComponentComponentDesignIdMap,
								true);
							if (fcGhosts) ghost = fcGhosts;
						}
					}
					forms.remove(frm.getName());
				}
			}
		}
		return ghost;
	}

	private IFormElement getFormComponentElement(FlattenedSolution fs, IFormElement formComponent, String elementName)
	{
		FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement(formComponent, fs, null, true);
		WebObjectSpecification spec = formComponentEl.getWebComponentSpec();
		if (spec != null)
		{
			Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
			if (properties.size() > 0)
			{
				for (PropertyDescription pd : properties)
				{
					Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
					Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
					if (frm == null) continue;
					FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentEl, pd, (JSONObject)propertyValue, frm, fs);
					for (FormElement element : cache.getFormComponentElements())
					{
						IPersist p = element.getPersistIfAvailable();
						if (p instanceof IFormElement)
						{
							IFormElement pfe = (IFormElement)p;
							String pfeName = getFixedFormElementName(pfe.getName());

							if (pfeName.equals(elementName))
							{
								return pfe;
							}
							pfe = getFormComponentElement(fs, pfe, elementName);
							if (pfe != null) return pfe;
						}
					}
				}
			}
		}

		return null;
	}

	private static String getFixedFormElementName(String name)
	{
		String fixedFormElementName = name;
		if (Character.isDigit(fixedFormElementName.charAt(0)))
		{
			fixedFormElementName = "_" + fixedFormElementName;
		}
		return fixedFormElementName.replace('-', '_');
	}

	private void sendComponents(FlattenedSolution fs, JSONWriter writer, Collection<IFormElement> baseComponents, Collection<IFormElement> deletedComponents,
		boolean writeNG2)
	{
		if (baseComponents.size() > 0)
		{
			Map<String, String> formComponentTemplates = new HashMap<String, String>();
			List<IFormElement> components = new ArrayList<IFormElement>(baseComponents);
			Collections.sort(components, PositionComparator.XY_PERSIST_COMPARATOR);
			Collections.reverse(components);
			EmbeddableJSONWriter compSpecNames = new EmbeddableJSONWriter();
			writer.key("components");
			writer.object();
			compSpecNames.object();

			// TODO is this really all the data? or are there properties that would normally go through the webcomponents..
			for (IFormElement baseComponent : components)
			{
				FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
				if (fe.getDesignId() != null)
				{
					writer.key(fe.getDesignId());
					compSpecNames.key(fe.getDesignId());
				}
				else
				{
					writer.key(fe.getName());
					compSpecNames.key(fe.getName());
				}
				String specName = fe.getWebComponentSpec().getName();
				compSpecNames.value(specName);

				fe.propertiesAsTemplateJSON(writer,
					new FormElementContext(fe, new ServoyDataConverterContext(ServoyModelFinder.getServoyModel().getFlattenedSolution(),
						ServoyModelFinder.getServoyModel().getMessagesManager()), null),
					true);

				Collection<PropertyDescription> properties = fe.getProperties(FormComponentPropertyType.INSTANCE);
				if (properties.size() > 0)
				{
					for (PropertyDescription pd : properties)
					{
						Object propertyValue = fe.getPropertyValue(pd.getName());
						Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
						if (frm == null) continue;
						FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, fs);
						formComponentTemplates.put(cache.getHtmlTemplateUUIDForAngular(), cache.getTemplate());
					}
				}
			}
			writer.endObject();
			compSpecNames.endObject();

			writer.key("componentSpecNames");
			writer.value(compSpecNames);

			if (writeNG2)
			{
				writer.key("ng2components");
				writer.array();
				for (IFormElement baseComponent : components)
				{
					writer.object();
					FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
					ChildrenJSONGenerator.writeFormElement(writer, baseComponent, form, fe, null, new ServoyDataConverterContext(fs), true);
					writer.endObject();
				}
				writer.endArray();
			}
			if (formComponentTemplates.size() > 0)
			{
				writer.key("formcomponenttemplates");
				writer.object();
				for (Entry<String, String> entry : formComponentTemplates.entrySet())
				{
					writer.key(entry.getKey());
					writer.value(entry.getValue().replace("\\\"", "\""));
				}
				writer.endObject();
			}
		}
		if (deletedComponents.size() > 0)
		{
			writer.key("deleted");
			writer.array();
			for (IFormElement baseComponent : deletedComponents)
			{
				FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
				writer.value(FormLayoutGenerator.getDesignId(fe));
			}
			writer.endArray();
		}

	}

	private EmbeddableJSONWriter sendComponentSpecToClientIfNeeded(EmbeddableJSONWriter compSpecsToSend, FormElement fe, String specName)
	{
		if (!clientSideSpecs.contains(specName))
		{
			clientSideSpecs.add(specName);

			EmbeddableJSONWriter clSideTypesForThisComponent = WebComponentSpecProvider.getInstance().getClientSideTypeCache().getClientSideSpecFor(
				fe.getWebComponentSpec());
			if (clSideTypesForThisComponent != null)
			{
				if (compSpecsToSend == null)
				{
					compSpecsToSend = new EmbeddableJSONWriter();
					compSpecsToSend.object();
				}
				compSpecsToSend.key(specName).value(clSideTypesForThisComponent);
			}
		}
		return compSpecsToSend;
	}

	public void handleBrowserWindowRefresh()
	{
		clientSideSpecs.clear();
	}

	@Override
	protected IWindow createWindow(int windowNr, String windowName)
	{
		return new DesignerBaseWindow(this, windowNr, windowName);
	}

	@Override
	public String getLogInformation()
	{
		return "";
	}

}
