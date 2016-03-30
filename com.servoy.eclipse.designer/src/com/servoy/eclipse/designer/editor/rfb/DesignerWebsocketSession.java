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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.impl.ClientService;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.server.ngclient.template.IFormElementValidator;
import com.servoy.j2db.server.ngclient.template.PartWrapper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

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

	/**
	 * @param uuid
	 */
	public DesignerWebsocketSession(String uuid, Form form)
	{
		super(uuid);
		this.form = form;
		registerServerService("$editor", this);
	}

	private String[] getSolutionStyleSheets(FlattenedSolution fs)
	{
		List<String> styleSheets = PersistHelper.getOrderedStyleSheets(fs);
		if (styleSheets != null && styleSheets.size() > 0)
		{
			Collections.reverse(styleSheets);
			for (int i = 0; i < styleSheets.size(); i++)
			{
				styleSheets.set(i,
					"resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + fs.getSolution().getName() + "/" + styleSheets.get(i));
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
		FormWrapper wrapper = new FormWrapper(flattenedForm, flattenedForm.getName(), false, new IFormElementValidator()
		{

			@Override
			public boolean isComponentSpecValid(IFormElement formElement)
			{
				return true;
			}
		}, context, true);
		switch (methodName)
		{
			case "getData" :
			{
				JSONWriter writer = new JSONStringer();
				writer.object();
				writer.key("formProperties");
				writer.value(wrapper.getPropertiesString());
				Collection<BaseComponent> baseComponents = wrapper.getBaseComponents();
				Collection<BaseComponent> deleted = Collections.emptyList();
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
					Collection<BaseComponent> baseComponents = wrapper.getBaseComponents();
					for (BaseComponent baseComponent : baseComponents)
					{
						FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
						if (fe.getDesignId().equals(name) || fe.getName().equals(name))
						{
							if (!form.isResponsiveLayout())
								FormLayoutGenerator.generateFormElementWrapper(w, fe, true, flattenedForm, form.isResponsiveLayout());
							FormLayoutGenerator.generateFormElement(w, fe, flattenedForm, true);
							if (!form.isResponsiveLayout()) FormLayoutGenerator.generateEndDiv(w);
							if (form.isResponsiveLayout())
							{
								parentuuid = fe.getPersistIfAvailable().getParent().getUUID();
								insertBeforeUUID = findNextSibling(fe.getPersistIfAvailable());
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
							if (child.getParent().equals(form)) parentuuid = null;
							insertBeforeUUID = findNextSibling(child);
							FormLayoutStructureGenerator.generateLayoutContainer((LayoutContainer)child, flattenedForm, context, w, true);
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
	 * @param fe
	 * @return
	 */
	private UUID findNextSibling(IPersist persist)
	{
		if (persist != null && persist.getParent() instanceof LayoutContainer)
		{
			LayoutContainer layoutContainer = (LayoutContainer)persist.getParent();
			List<IPersist> hierarchyChildren = layoutContainer.getHierarchyChildren();
			Collections.sort(hierarchyChildren, PositionComparator.XY_PERSIST_COMPARATOR);
			int indexOf = hierarchyChildren.indexOf(persist);
			if (indexOf > -1 && (indexOf + 1) < hierarchyChildren.size()) return hierarchyChildren.get(indexOf + 1).getUUID();
		}
		return null;
	}

	public String getComponentsJSON(FlattenedSolution fs, List<IPersist> persists)
	{
		Set<BaseComponent> baseComponents = new HashSet<>();
		Set<BaseComponent> deletedComponents = new HashSet<>();
		Set<LayoutContainer> deletedLayoutContainers = new HashSet<>();
		Set<Part> parts = new HashSet<>();
		Set<LayoutContainer> containers = new HashSet<>();
		boolean renderGhosts = false;
		for (IPersist persist : persists)
		{
			if (persist instanceof BaseComponent)
			{
				if (persist.getParent().getChild(persist.getUUID()) != null)
				{
					ISupportChilds parent = persist.getParent();
					if (parent instanceof LayoutContainer)
					{
						baseComponents.add((BaseComponent)persist);
					}
					else if (parent instanceof Form)
					{
						baseComponents.add((BaseComponent)persist);
					}
					else while (parent instanceof BaseComponent)
					{
						if (parent.getParent() instanceof Form)
						{
							baseComponents.add((BaseComponent)parent);
							break;
						}
						else
						{
							parent = parent.getParent();
						}

					}
				}
				else
				{
					deletedComponents.add((BaseComponent)persist);
				}
			}
			else if (persist instanceof Part)
			{
				parts.add((Part)persist);
			}
			else if (persist instanceof LayoutContainer)
			{
				if (persist.getParent().getChild(persist.getUUID()) != null)
				{
					containers.add((LayoutContainer)persist);
				}
				else
				{
					deletedLayoutContainers.add((LayoutContainer)persist);
				}
			}
			else if (!(persist instanceof Form))
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
		JSONWriter writer = new JSONStringer();
		writer.object();
		sendComponents(fs, writer, baseComponents, deletedComponents);
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
			writer.key("containers");
			writer.object();
			for (LayoutContainer container : containers)
			{
				// TODO what the send over, if new then just the id? but what about the properties?
				Map<String, String> attributes = container.getAttributes();
				writer.key(container.getUUID().toString());
				writer.object();
				for (Entry<String, String> attribute : attributes.entrySet())
				{
					writer.key(attribute.getKey());
					writer.value(attribute.getValue());
				}
				writer.endObject();
			}
			writer.endObject();
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
				if (parent instanceof LayoutContainer)
				{
					ArrayList<IPersist> children = new ArrayList<IPersist>();
					Iterator<IPersist> it = parent.getAllObjects();
					while (it.hasNext())
					{
						IPersist persist = it.next();
						if (persist instanceof ISupportBounds)
						{
							children.add(persist);
						}
					}
					IPersist[] sortedChildArray = children.toArray(new IPersist[0]);
					Arrays.sort(sortedChildArray, PositionComparator.XY_PERSIST_COMPARATOR);
					children = new ArrayList<IPersist>(Arrays.asList(sortedChildArray));
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

	/**
	 * @param fs
	 * @param writer
	 * @param baseComponents
	 */
	private void sendComponents(FlattenedSolution fs, JSONWriter writer, Collection<BaseComponent> baseComponents, Collection<BaseComponent> deletedComponents)
	{
		if (baseComponents.size() > 0)
		{
			writer.key("components");
			writer.object();
			// TODO is this really all the data? or are there properties that would normally go through the webcomponents..
			for (BaseComponent baseComponent : baseComponents)
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
			}
			writer.endObject();
		}
		if (deletedComponents.size() > 0)
		{
			writer.key("deleted");
			writer.array();
			for (BaseComponent baseComponent : deletedComponents)
			{
				FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
				writer.value(FormLayoutGenerator.getDesignId(fe));
			}
			writer.endArray();
		}
	}
}
