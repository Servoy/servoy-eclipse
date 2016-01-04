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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.specification.WebComponentSpecification;
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
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.server.ngclient.template.IFormElementValidator;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
public class DesignerWebsocketSession extends BaseWebsocketSession implements IServerService
{
	public static final String EDITOR_CONTENT_SERVICE = "$editorContentService";

	private static final WebComponentSpecification EDITOR_CONTENT_SERVICE_SPECIFICATION = new WebComponentSpecification(EDITOR_CONTENT_SERVICE, "",
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

	private String getSolutionCSSURL(FlattenedSolution fs)
	{
		int styleSheetID = fs.getSolution().getStyleSheetID();
		if (styleSheetID > 0)
		{
			Media styleSheetMedia = fs.getMedia(styleSheetID);
			if (styleSheetMedia != null)
			{
				return "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + fs.getSolution().getName() + "/" + styleSheetMedia.getName();
			}
		}
		return null;
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
				writer.key("styleSheet");
				writer.value(getSolutionCSSURL(fs));
				writer.endObject();
				writer.endObject();
				return writer.toString();
			}
			case "getTemplate" :
			{
				String name = args.getString("name");
				boolean highlight = !args.isNull("highlight") && args.getBoolean("highlight");
				StringWriter htmlTemplate = new StringWriter(512);
				PrintWriter w = new PrintWriter(htmlTemplate);
				UUID parentuuid = null;
				UUID insertBeforeUUID = null;

				Collection<BaseComponent> baseComponents = wrapper.getBaseComponents();
				for (BaseComponent baseComponent : baseComponents)
				{
					FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, fs, null, true);
					if (fe.getName().equals(name))
					{
						if (!form.isResponsiveLayout()) FormLayoutGenerator.generateFormElementWrapper(w, fe, true, flattenedForm);
						FormLayoutGenerator.generateFormElement(w, fe, flattenedForm, true, highlight);
						if (!form.isResponsiveLayout()) FormLayoutGenerator.generateEndDiv(w);
						if (form.isResponsiveLayout())
						{
							parentuuid = fe.getPersistIfAvailable().getParent().getUUID();
							insertBeforeUUID = findNextSibling(fe);
						}

						break;
					}
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
	 * @param fe
	 * @return
	 */
	private UUID findNextSibling(FormElement fe)
	{
		if (fe.getPersistIfAvailable() != null && fe.getPersistIfAvailable().getParent() instanceof LayoutContainer)
		{
			LayoutContainer layoutContainer = (LayoutContainer)fe.getPersistIfAvailable().getParent();
			List<IPersist> hierarchyChildren = layoutContainer.getHierarchyChildren();
			hierarchyChildren.sort(PositionComparator.XY_PERSIST_COMPARATOR);
			int indexOf = hierarchyChildren.indexOf(fe.getPersistIfAvailable());
			if (indexOf > -1 && (indexOf + 1) < hierarchyChildren.size()) return hierarchyChildren.get(indexOf + 1).getUUID();
		}
		return null;
	}

	public String getComponentsJSON(FlattenedSolution fs, List<IPersist> persists)
	{
		List<BaseComponent> baseComponents = new ArrayList<>();
		List<BaseComponent> deletedComponents = new ArrayList<>();
		for (IPersist persist : persists)
		{
			if (persist instanceof BaseComponent)
			{
				if (persist.getParent().getChild(persist.getUUID()) != null)
				{
					baseComponents.add((BaseComponent)persist);
				}
				else
				{
					deletedComponents.add((BaseComponent)persist);
				}
			}
			else
			{
				// TODO go to a parent? and serialize that?
			}
		}
		JSONWriter writer = new JSONStringer();
		writer.object();
		sendComponents(fs, writer, baseComponents, deletedComponents);
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
				writer.key(fe.getName());
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
