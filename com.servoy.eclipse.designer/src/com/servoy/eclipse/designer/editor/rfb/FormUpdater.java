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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.GhostHandler;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.server.ngclient.design.DesignNGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.Utils;

public class FormUpdater implements Runnable
{
	private final Map<Form, List<IFormElement>> frms;
	private final List<Form> changedForms;
	private final INGClientWebsocketSession websocketSession;

	FormUpdater(INGClientWebsocketSession websocketSession, Map<Form, List<IFormElement>> frms, List<Form> changedForms)
	{
		this.websocketSession = websocketSession;
		this.frms = frms;
		this.changedForms = changedForms;
	}

	@Override
	public void run()
	{
		for (Entry<Form, List<IFormElement>> entry : frms.entrySet())
		{
			List<IFormController> cachedFormControllers = websocketSession.getClient().getFormManager().getCachedFormControllers(entry.getKey());
			ServoyDataConverterContext cntxt = new ServoyDataConverterContext(websocketSession.getClient());
			for (IFormController fc : cachedFormControllers)
			{
				boolean bigChange = false;
				outer : for (IFormElement persist : entry.getValue())
				{
					if (persist.getParent().getChild(persist.getUUID()) == null)
					{
						// deleted persist
						bigChange = true;
						break;
					}
					FormElement newFe = new FormElement(persist, cntxt.getSolution(), new PropertyPath(), true);

					IWebFormUI formUI = (IWebFormUI)fc.getFormUI();
					WebFormComponent webComponent = findWebComponent(formUI.getComponents(), newFe.getName());
					if (webComponent != null)
					{
						FormElement existingFe = webComponent.getFormElement();
						IPersist existingFePersist = existingFe.getPersistIfAvailable();
						if (existingFePersist != null)
						{
							IPersist existingFePersistParent = existingFePersist.getParent();
							if (existingFePersistParent != null && !existingFePersistParent.equals(persist.getParent()))
							{
								// element has been moved to a new parent
								bigChange = true;
								break;
							}
						}


						WebComponentSpecification spec = webComponent.getSpecification();
						Set<String> allKeys = new HashSet<String>();
						allKeys.addAll(newFe.getRawPropertyValues().keySet());
						allKeys.addAll(existingFe.getRawPropertyValues().keySet());
						boolean changed = false;
						for (String property : allKeys)
						{
							if (spec.getProperty(property) != null && spec.getProperty(property).getType() instanceof ValueListPropertyType)
							{
								bigChange = true;
								break outer;
							}
							Object currentPropValue = existingFe.getPropertyValue(property);
							Object newPropValue = newFe.getPropertyValue(property);
							if (!Utils.equalObjects(currentPropValue, newPropValue))
							{
								changed = true;
								if (spec.getHandler(property) != null)
								{
									// this is a handler change so a big change (component could react to a handler differently)
									bigChange = true;
									break outer;
								}
								if (property.equals("json"))
								{
									bigChange = true;
									break outer;
								}
								if (property.equals("formIndex"))
								{
									bigChange = true;
									break outer;
								}
								if (property.equals("displayType"))
								{
									bigChange = true;
									break outer;
								}
								if (property.equals("editable") && newFe.getPersistIfAvailable() instanceof Field &&
									((Field)newFe.getPersistIfAvailable()).getDisplayType() == Field.HTML_AREA)
								{
									bigChange = true;
									break outer;
								}
								if ((property.equals("visible") || property.equals("text") || property.equals("labelFor")) &&
									(fc.getForm().getView() == IFormConstants.VIEW_TYPE_TABLE || fc.getForm().getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED))
								{
									bigChange = true;
									break outer;
								}
								if ((webComponent.getParent() != formUI || persist.getParent() instanceof LayoutContainer) &&
									(property.equals("location") || property.equals("size")))
								{
									bigChange = true;
									break outer;
								}
								PropertyDescription prop = spec.getProperty(property);
								if (prop != null)
								{
									if (prop.getType() instanceof DataproviderPropertyType)
									{
										// if it is a portal based component then the dataprovider is only the last part for this webcomponent
										// so if the new value ends with the current value then it is still the same and it is not a big change (this also doesn't have to be set on the component)
										if (webComponent.getParent() == formUI ||
											!((newPropValue instanceof String) && (currentPropValue instanceof String) && ((String)newPropValue).endsWith((String)currentPropValue)))
										{
											// this is a design property change so a big change
											bigChange = true;
											break outer;
										}
										continue;
									}
									else if (WebFormComponent.isDesignOnlyProperty(prop))
									{
										// this is a design property change so a big change
										bigChange = true;
										break outer;
									}
									if (GhostHandler.isDroppable(prop, prop.getConfig()))
									{
										bigChange = true; // TODO we should really let component spec tell us what changes need a full refresh or not - instead of trying to guess which is not really possible (some simple property that is not watched for changes on client might need full refresh while
										// a very complex property might be fully watched and might have no need for a full recreate of the form); for example if the component doesn't need full recreation
										// when ghosts change, we could simply refresh ghosts only!
										break outer;
									}

									webComponent.setFormElement(newFe);
									webComponent.setProperty(property, newFe.getPropertyValueConvertedForWebComponent(property, webComponent,
										formUI.getDataAdapterList() instanceof DataAdapterList ? (DataAdapterList)formUI.getDataAdapterList() : null));

								}
							}
						}
						if (!changed && persist.getParent() instanceof LayoutContainer)
						{
							// hack if no changes are found and the parent of the persist is a layout container then the parent could be changed
							bigChange = true;
						}
					}
					else
					{
						// no webcomponent found, so new one or name change, recreate all
						bigChange = true;
						break;
					}
				}
				if (bigChange)
				{
					fc.recreateUI();
					websocketSession.getClientService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshGhosts",
						new Object[] { });
					websocketSession.getClientService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshDecorators",
						new Object[] { });
					websocketSession.getClientService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("updateForm",
						new Object[] { });
				}
			}
		}
		if (changedForms != null && changedForms.size() > 0)
		{
			for (Form changedForm : changedForms)
			{
				Form form = ModelUtils.getEditingFlattenedSolution(changedForm).getFlattenedForm(changedForm);
				List<IFormController> cachedFormControllers = websocketSession.getClient().getFormManager().getCachedFormControllers(changedForm);
				for (IFormController iFormController : cachedFormControllers)
				{
					if (iFormController.getView() != form.getView())
					{
						List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(); // should we also use these?

						boolean isVisible = iFormController.isFormVisible();
						if (isVisible) iFormController.notifyVisible(false, invokeLaterRunnables);
						((WebFormController)iFormController).initFormUI();
						if (isVisible) iFormController.notifyVisible(true, invokeLaterRunnables);

					}
					iFormController.recreateUI();
				}
				websocketSession.getClientService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshGhosts",
					new Object[] { });
				websocketSession.getClientService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall(
					"updateForm",
					new Object[] { changedForm.getName(), changedForm.getUUID().toString(), Integer.valueOf((int)form.getSize().getWidth()), Integer.valueOf((int)form.getSize().getHeight()) });
			}
		}
		else
		{
			websocketSession.getClientService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshDecorators",
				new Object[] { });
		}
	}

	private static WebFormComponent findWebComponent(Collection<WebComponent> components, String name)
	{
		if (components == null) return null;
		for (WebComponent webComponent : components)
		{
			if (webComponent.getName().equals(name) && webComponent instanceof WebFormComponent) return (WebFormComponent)webComponent;
			if (webComponent instanceof Container)
			{
				WebFormComponent comp = findWebComponent(((Container)webComponent).getComponents(), name);
				if (comp != null) return comp;
			}
		}
		return null;
	}
}