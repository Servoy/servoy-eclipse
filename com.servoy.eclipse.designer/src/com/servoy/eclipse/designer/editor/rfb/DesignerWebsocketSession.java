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

import java.util.Collection;
import java.util.Locale;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IServerService;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
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
	/**
	 * @param uuid
	 */
	public DesignerWebsocketSession(String uuid)
	{
		super(uuid);
		registerServerService("$editor", this);
	}

	@Override
	public Locale getLocale()
	{
		return Locale.getDefault();
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
					FlattenedSolution fs = new FlattenedSolution(solutionMetaData, new AbstractActiveSolutionHandler(as)
					{
						@Override
						public IRepository getRepository()
						{
							return ApplicationServerRegistry.get().getLocalRepository();
						}
					});

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
					writer.key("components");
					writer.object();
					for (BaseComponent baseComponent : baseComponents)
					{
						FormElement fe = FormElementHelper.INSTANCE.getFormElement(baseComponent, context, null);
						writer.key(fe.getName());
						fe.propertiesAsTemplateJSON(writer, new FormElementContext(fe));
					}
					writer.endObject();
					writer.endObject();
					return writer.toString();
				}
				break;
			}
		}
		return null;
	}
}
