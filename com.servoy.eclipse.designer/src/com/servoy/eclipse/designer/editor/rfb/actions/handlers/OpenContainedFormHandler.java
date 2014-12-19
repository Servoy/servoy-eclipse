/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.Collection;

import org.eclipse.swt.widgets.Display;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.GhostBean;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea@servoy.com
 *
 */
public class OpenContainedFormHandler implements IServerService
{

	public OpenContainedFormHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	private final BaseVisualFormEditor editorPart;

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(String methodName, final JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{


			public void run()
			{
				if (args.has("uuid"))
				{
					try
					{
						IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getString("uuid"));
						Solution s = (Solution)editorPart.getForm().getParent();
						boolean open = false;
						if (persist != null)
						{

							if (persist instanceof Tab)
							{
								open = openFormDesignEditor(s, ((Tab)persist).getContainsFormID());
								Debug.log("Cannot open form with id " + ((Tab)persist).getContainsFormID() + "in design editor (Tab uuid " +
									args.getString("uuid") + ")");
							}
							else if (persist instanceof GhostBean)
							{
								GhostBean ghost = (GhostBean)persist;
								JSONObject beanXML = new ServoyJSONObject(ghost.getBeanXML(), false);


								WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
									ghost.getParentBean().getBeanClassName());
								if (spec != null)
								{
									Collection<PropertyDescription> forms = null;
									IPropertyType< ? > iPropertyType = spec.getFoundTypes().get(ghost.getTypeName());
									if (iPropertyType instanceof ICustomType)
									{
										forms = ((ICustomType< ? >)iPropertyType).getCustomJSONTypeDefinition().getProperties(FormPropertyType.INSTANCE);
									}
									else
									{
										Debug.warn("Unexpected propertyType " + iPropertyType.getName());
									}

									for (PropertyDescription pd : Utils.iterate(forms))
									{
										open = openFormDesignEditor(s, beanXML.opt(pd.getName()));
										if (!open)
										{
											Debug.log("Cannot open form with id " + beanXML.opt(pd.getName()) + "in design editor (Container uuid " +
												args.getString("uuid") + ")");
										}
									}
								}
							}
							else if (persist instanceof Bean)
							{
								Bean bean = (Bean)persist;
								WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
									((Bean)persist).getBeanClassName());
								if (spec != null)
								{
									Collection<PropertyDescription> forms = spec.getProperties(FormPropertyType.INSTANCE);
									for (PropertyDescription pd : forms)
									{
										open = openFormDesignEditor(s, bean.getProperty(pd.getName()));
										if (!open)
										{
											Debug.log("Cannot open form with id " + bean.getProperty(pd.getName()) + "in design editor (container uuid " +
												args.getString("uuid") + ")");
										}
									}
								}
							}

						}
						else
						{
							Debug.log("Cannot open form in design editor. Container uuid " + args.getString("uuid") + " was not found");
						}
					}
					catch (JSONException ex)
					{
						Debug.error(ex);
					}
				}
			}

			/**
			 * @param s
			 * @param value
			 * @return
			 */
			private boolean openFormDesignEditor(Solution s, Object value)
			{
				if (value != null)
				{
					Form toOpen = s.getForm((Integer)value);
					if (toOpen != null)
					{
						return EditorUtil.openFormDesignEditor(toOpen) != null;
					}
				}
				return false;
			}
		});
		return null;
	}


}
