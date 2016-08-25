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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.impl.ClientService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.util.WebFormComponentChildType;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.server.ngclient.template.PartWrapper;
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

	private static final WebObjectSpecification EDITOR_CONTENT_SERVICE_SPECIFICATION = new WebObjectSpecification(EDITOR_CONTENT_SERVICE, "",
		EDITOR_CONTENT_SERVICE, null, null, null, "", null);

	private final Form form;

	private final BaseVisualFormEditor editor;

	/**
	 * @param uuid
	 */
	public DesignerWebsocketSession(String uuid, BaseVisualFormEditor editor)
	{
		super(uuid);
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
				styleSheets.set(i, "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + fs.getSolution().getName() + "/" +
					styleSheets.get(i) + "?t=" + Long.toHexString(System.currentTimeMillis()));
			}
		}
		return styleSheets.toArray(new String[0]);
	}


	@Override
	protected IClientService createClientService(String name)
	{
		if (EDITOR_CONTENT_SERVICE.equals(name))
		{
			return new ClientService(EDITOR_CONTENT_SERVICE, EDITOR_CONTENT_SERVICE_SPECIFICATION);
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

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(form.getSolution().getName());
		FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
		Form flattenedForm = fs.getFlattenedForm(form);

		ServoyDataConverterContext context = new ServoyDataConverterContext(fs);
		FormWrapper wrapper = new FormWrapper(flattenedForm, flattenedForm.getName(), false, null, context, true);
		switch (methodName)
		{
			case "getData" :
			{
				JSONWriter writer = new JSONStringer();
				writer.object();
				writer.key("formProperties");
				writer.value(wrapper.getPropertiesString());
				Collection<IFormElement> baseComponents = new ArrayList<IFormElement>(wrapper.getBaseComponents());
				Collection<IFormElement> deleted = Collections.emptyList();
				sendComponents(fs, writer, baseComponents, deleted);
				writer.key("solutionProperties");
				writer.object();
				writer.key("styleSheets");
				writer.value(getSolutionStyleSheets(fs));
				writer.endObject();
				generateParts(flattenedForm, context, writer, wrapper.getParts());
				writer.endObject();
				return writer.toString();
			}
			case "getTemplate" :
			{
				String name = args.optString("name", null);
				StringWriter htmlTemplate = new StringWriter(512);
				PrintWriter w = new PrintWriter(htmlTemplate);
				UUID parentuuid = null;
				UUID insertBeforeUUID = null;

				boolean componentFound = false;
				if (name != null)
				{
					Collection<IFormElement> baseComponents = wrapper.getBaseComponents();
					for (IFormElement baseComponent : baseComponents)
					{
						FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
						if (Utils.equalObjects(fe.getDesignId(), name) || Utils.equalObjects(fe.getName(), name))
						{
							if (!form.isResponsiveLayout()) FormLayoutGenerator.generateFormElementWrapper(w, fe, flattenedForm, form.isResponsiveLayout());
							FormLayoutGenerator.generateFormElement(w, fe, flattenedForm);
							if (!form.isResponsiveLayout()) FormLayoutGenerator.generateEndDiv(w);
							if (form.isResponsiveLayout())
							{
								if (((ISupportExtendsID)fe.getPersistIfAvailable()).getExtendsID() > 0 &&
									fe.getPersistIfAvailable().getParent() instanceof Form)
								{
									IPersist p = PersistHelper.getSuperPersist((ISupportExtendsID)fe.getPersistIfAvailable());
									parentuuid = p.getParent().getUUID();
									insertBeforeUUID = findNextSibling(fe.getPersistIfAvailable());
								}
								else
								{
									parentuuid = fe.getPersistIfAvailable().getParent().getUUID();
									insertBeforeUUID = findNextSibling(fe.getPersistIfAvailable());
								}
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
							parentuuid = child.getParent().getUUID();
							if (((ISupportExtendsID)child).getExtendsID() > 0 && child.getParent() instanceof Form)
							{
								IPersist p = PersistHelper.getSuperPersist((ISupportExtendsID)child);
								parentuuid = p.getParent().getUUID();
								insertBeforeUUID = findNextSibling(p);
							}
							else
							{
								if (child.getParent().equals(form)) parentuuid = null;
								insertBeforeUUID = findNextSibling(child);
							}
							FormLayoutStructureGenerator.generateLayoutContainer((LayoutContainer)child, flattenedForm, context.getSolution(), w, true,
								FormElementHelper.INSTANCE);
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
				if (insertBeforeUUID != null)
				{
					writer.key("insertBeforeUUID");
					writer.value(insertBeforeUUID);
				}
				writer.endObject();
				return writer.toString();
			}
		}
		return null;
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
	 * @param persist
	 * @return
	 */
	private UUID findNextSibling(IPersist persist)
	{
		if (persist != null && persist.getParent() instanceof AbstractContainer)
		{
			AbstractContainer persistParent = (AbstractContainer)PersistHelper.getFlattenedPersist(ModelUtils.getEditingFlattenedSolution(persist), form,
				persist.getParent());
			ArrayList<IPersist> children = persistParent.getSortedChildren();
			int indexOf = children.indexOf(persist);
			if (indexOf > -1 && (indexOf + 1) < children.size()) return children.get(indexOf + 1).getUUID();
		}
		return null;
	}

	public String getComponentsJSON(FlattenedSolution fs, Set<IPersist> persists)
	{
		Set<IFormElement> baseComponents = new HashSet<>();
		Set<IFormElement> refreshTemplate = new HashSet<>();
		Set<IFormElement> deletedComponents = new HashSet<>();
		Set<String> updatedFormComponentsDesignId = new HashSet<>();
		Set<IFormElement> formComponentsComponents = new HashSet<>();
		Set<LayoutContainer> deletedLayoutContainers = new HashSet<>();
		Set<Part> parts = new HashSet<>();
		List<LayoutContainer> containers = new ArrayList<>();
		Set<IFormElement> compAttributes = new HashSet<>();
		boolean renderGhosts = editor.isRenderGhosts();
		editor.setRenderGhosts(false);
		for (IPersist persist : persists)
		{
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
				IFormElement baseComponent = (IFormElement)persist;
				if (baseComponent.getGroupID() != null)
				{
					compAttributes.add(baseComponent);
				}

				if (persist instanceof ISupportExtendsID)
				{
					IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
					if (superPersist instanceof IFormElement) deletedComponents.add((IFormElement)superPersist);
				}


				if (formComponentChild || persist.getParent().getChild(persist.getUUID()) != null)
				{
					ISupportChilds parent = persist.getParent();
					if (parent instanceof AbstractContainer)
					{
						baseComponents.add(baseComponent);
					}
					else if (parent instanceof Form)
					{
						baseComponents.add(baseComponent);
					}
					else while (parent instanceof BaseComponent)
					{
						if (parent.getParent() instanceof Form)
						{
							baseComponents.add((IFormElement)parent);
							break;
						}
						else
						{
							parent = parent.getParent();
						}

					}
					if (persist instanceof Field)
					{
						Field oldField = (Field)ServoyModelFinder.getServoyModel().getFlattenedSolution().searchPersist(persist.getUUID());
						if (oldField != null && ((Field)persist).getDisplayType() != oldField.getDisplayType())
						{
							refreshTemplate.add(baseComponent);
						}
					}

					checkFormComponents(updatedFormComponentsDesignId, formComponentsComponents,
						FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true), fs);
				}
				else
				{
					deletedComponents.add(baseComponent);
				}
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
					containers.add((LayoutContainer)persist);
				}
				else
				{
					deletedLayoutContainers.add((LayoutContainer)persist);
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
					if (parent instanceof BaseComponent)
					{
						baseComponents.add((BaseComponent)parent);
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

		JSONWriter writer = new JSONStringer();
		writer.object();
		sendComponents(fs, writer, baseComponents, deletedComponents);

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
			for (LayoutContainer container : containersA)
			{
				// TODO what the send over, if new then just the id? but what about the properties?
				Map<String, String> attributes = container.getAttributes();
				writer.object();
				writer.key("uuid");
				writer.value(container.getUUID().toString());
				for (Entry<String, String> attribute : attributes.entrySet())
				{
					writer.key(attribute.getKey());
					writer.value(attribute.getValue());
				}
				writer.endObject();
			}
			writer.endArray();
		}
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
				ISupportChilds parent = p.getParent();
				writer.key(p.getUUID().toString());
				writer.object();
				writer.key("uuid");
				if (parent instanceof Form)
				{
					writer.value(null);
				}
				else
				{
					writer.value(parent.getUUID().toString());
				}
				writer.key("index");
				if (parent instanceof AbstractContainer && form.isResponsiveLayout())
				{
					ArrayList<IPersist> children = ((AbstractContainer)parent).getSortedChildren();
					writer.value(children.indexOf(p));
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

	private void checkFormComponents(Set<String> updatedFormComponentsDesignId, Set<IFormElement> formComponentsComponents, FormElement formElement,
		FlattenedSolution fs)
	{
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
					updatedFormComponentsDesignId.add(
						formElement.getName(formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName()));
					FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, (JSONObject)propertyValue, frm, fs);
					for (FormElement element : cache.getFormComponentElements())
					{
						formComponentsComponents.add((IFormElement)element.getPersistIfAvailable());
						checkFormComponents(updatedFormComponentsDesignId, formComponentsComponents, element, fs);
					}
				}
			}
		}
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

	/**
	 * @param fs
	 * @param writer
	 * @param baseComponents
	 */
	private void sendComponents(FlattenedSolution fs, JSONWriter writer, Collection<IFormElement> baseComponents, Collection<IFormElement> deletedComponents)
	{
		if (baseComponents.size() > 0)
		{
			Map<String, String> formComponentTemplates = new HashMap<String, String>();
			List<IFormElement> components = new ArrayList<IFormElement>(baseComponents);
			Collections.sort(components, PositionComparator.XY_PERSIST_COMPARATOR);
			writer.key("components");
			writer.object();
			// TODO is this really all the data? or are there properties that would normally go through the webcomponents..
			for (IFormElement baseComponent : components)
			{
				FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
				if (fe.getDesignId() != null)
				{
					writer.key(fe.getDesignId());
				}
				else
				{
					writer.key(fe.getName());
				}
				fe.propertiesAsTemplateJSON(writer, new FormElementContext(fe));

				Collection<PropertyDescription> properties = fe.getProperties(FormComponentPropertyType.INSTANCE);
				if (properties.size() > 0)
				{
					for (PropertyDescription pd : properties)
					{
						Object propertyValue = fe.getPropertyValue(pd.getName());
						Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
						if (frm == null) continue;
						FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, fs);
						formComponentTemplates.put(cache.getCacheUUID(), cache.getTemplate());
					}
				}
			}
			writer.endObject();
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
}
