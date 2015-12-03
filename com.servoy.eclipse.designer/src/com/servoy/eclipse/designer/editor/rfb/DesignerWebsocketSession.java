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

import java.util.ArrayList;
import java.util.Collection;
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
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.server.ngclient.template.IFormElementValidator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class DesignerWebsocketSession extends BaseWebsocketSession implements IServerService
{
	public static final String EDITOR_CONTENT_SERVICE = "$editorContentService";

	private static final WebComponentSpecification EDITOR_CONTENT_SERVICE_SPECIFICATION = new WebComponentSpecification(EDITOR_CONTENT_SERVICE, "",
		EDITOR_CONTENT_SERVICE, null, null, null, "", null);

	/**
	 * @param uuid
	 */
	public DesignerWebsocketSession(String uuid)
	{
		super(uuid);
		registerServerService("$editor", this);
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
		switch (methodName)
		{
			case "getData" :
			{
				String solutionName = args.getString("solution");
				String formName = args.getString("form");
				IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
				SolutionMetaData solutionMetaData = (SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(solutionName,
					IRepository.SOLUTIONS);
				if (solutionMetaData == null)
				{
					Debug.error("Solution '" + solutionName + "' was not found.");
				}
				else
				{
					ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);

					FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
					Form form = fs.getForm(formName);
					Form flattenedForm = fs.getFlattenedForm(form);
					ServoyDataConverterContext context = new ServoyDataConverterContext(fs);
					FormWrapper wrapper = new FormWrapper(flattenedForm, formName, false, new IFormElementValidator()
					{

						@Override
						public boolean isComponentSpecValid(IFormElement formElement)
						{
							return true;
						}
					}, context, true);
					JSONWriter writer = new JSONStringer();
					writer.object();
					writer.key("formProperties");
					writer.value(wrapper.getPropertiesString());
					Collection<BaseComponent> baseComponents = wrapper.getBaseComponents();
					sendComponents(fs, writer, baseComponents);
					writer.endObject();
					return writer.toString();
				}
				break;
			}
		}
		return null;
	}

	public String getComponentsJSON(FlattenedSolution fs, List<IPersist> persists)
	{
		List<BaseComponent> baseComponents = new ArrayList<>();
		for (IPersist persist : persists)
		{
			if (persist instanceof BaseComponent)
			{
				baseComponents.add((BaseComponent)persist);
			}
			else
			{
				// TODO go to a parent? and serialize that?
			}
		}
		JSONWriter writer = new JSONStringer();
		writer.object();
		sendComponents(fs, writer, baseComponents);
		writer.endObject();
		return writer.toString();
	}

	/**
	 * @param fs
	 * @param writer
	 * @param baseComponents
	 */
	private void sendComponents(FlattenedSolution fs, JSONWriter writer, Collection<BaseComponent> baseComponents)
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
}
