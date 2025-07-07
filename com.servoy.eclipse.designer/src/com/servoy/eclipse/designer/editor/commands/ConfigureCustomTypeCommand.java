/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IPropertyType;

import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetCustomArrayPropertiesCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.autowizard.FormComponentTreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.autowizard.PropertyWizardDialogConfigurator;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;

/**
 * @author emera
 */
public class ConfigureCustomTypeCommand extends AbstractHandler implements IHandler
{
	public static final String COMMAND_ID = "com.servoy.eclipse.designer.rfb.config";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		PersistContext persistContext = getPersistContext();
		if (persistContext != null)
		{
			final BaseVisualFormEditor activeEditor = DesignerUtil.getActiveEditor();
			WebComponent webComponent = (WebComponent)persistContext.getPersist();
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
				.getWebObjectSpecification(webComponent.getTypeName());
			String propertyName = event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.config.type");
			if (propertyName == null)
			{
				propertyName = getWizardProperties().get(0);
			}

			PropertyDescription property = spec.getProperty(propertyName);
			if (property != null)
			{
				if ("autoshow".equals(property.getTag("wizard")))
				{
					if (property.getType() == FormComponentPropertyType.INSTANCE && property.getConfig() instanceof ComponentTypeConfig &&
						((ComponentTypeConfig)property.getConfig()).forFoundset != null)
					{
						// list form component
						Display.getDefault().asyncExec(() -> {
							RelatedForm relatedForm = FormComponentTreeSelectDialog.selectFormComponent(webComponent, activeEditor.getForm());
							if (relatedForm != null)
							{
								activeEditor.getCommandStack()
									.execute(
										new BaseRestorableCommand("createComponent")
										{
											@Override
											public void execute()
											{
												FormComponentTreeSelectDialog.setFormComponentProperty(webComponent, activeEditor.getForm(), relatedForm);
											}
										});
							}
						});
					}
					else
					{
						// prop type should be an array of a custom type..
						IPropertyType< ? > propType = property.getType();
						if (propType instanceof CustomJSONArrayType< ? , ? >)
						{

							CustomJSONObjectType< ? , ? > customObjectType = (CustomJSONObjectType< ? , ? >)((CustomJSONArrayType< ? , ? >)propType)
								.getCustomJSONTypeDefinition().getType();
							PropertyDescription customObjectDefinition = customObjectType.getCustomJSONTypeDefinition();
							Collection<PropertyDescription> wizardProperties = customObjectDefinition.getTaggedProperties("wizard");
							if (wizardProperties.size() > 0)
							{
								// feed this wizardProperties into the wizard
								FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(activeEditor.getForm());
								ITable table = ServoyModelFinder.getServoyModel().getDataSourceManager()
									.getDataSource(flattenedSolution.getFlattenedForm(activeEditor.getForm()).getDataSource());

								List<Map<String, Object>> input = new ArrayList<>();
								List<Map<String, Object>> originalInput = new ArrayList<>();
								Object prop = webComponent.getProperty(propertyName);
								Set<Object> previousColumns = new HashSet<>();
								if (prop instanceof IChildWebObject[])
								{
									IChildWebObject[] arr = (IChildWebObject[])prop;
									for (IChildWebObject obj : arr)
									{
										if (obj instanceof WebCustomType)
										{
											WebCustomType wct = (WebCustomType)obj;
											JSONObject object = wct.getFlattenedJson();
											Map<String, Object> map = getAsMap(object);
											previousColumns.add(object.get("svyUUID"));
											input.add(map);
											Map<String, Object> originalMap = new HashMap<String, Object>();
											map.forEach((key, value) -> originalMap.put(key, value));
											originalInput.add(originalMap);
										}
									}
								}

								PropertyWizardDialogConfigurator dialogConfigurator = new PropertyWizardDialogConfigurator(UIUtils.getActiveShell(),
									persistContext,
									flattenedSolution, property).withTable(table).withProperties(wizardProperties).withInput(input);
								if (dialogConfigurator.open() != Window.OK) return null;
								List<Map<String, Object>> newProperties = dialogConfigurator.getResult();

								if (!newProperties.equals(originalInput))
								{
									final PersistContext ctx = persistContext;
									final String _propertyName = propertyName;
									Display.getDefault().asyncExec(() -> {
										activeEditor.getCommandStack()
											.execute(
												new SetCustomArrayPropertiesCommand(_propertyName, ctx, newProperties, previousColumns, activeEditor));
									});
								}
							}
							else
							{
								ServoyLog.logWarning("auto show wizard property " + property + " of custom type " + customObjectType +
									"\nhas no wizard properties\n" + propType, null);
							}
						}
						else
						{
							ServoyLog.logWarning("wizard:autoshow enabled for property " + property + " of component " + spec +
								" that is not an custom array type " + propType, null);
						}
					}
				}

			}
		}
		return null;
	}

	public PersistContext getPersistContext()
	{
		PersistContext persistContext = null;
		final BaseVisualFormEditor activeEditor = DesignerUtil.getActiveEditor();
		if (activeEditor != null)
		{
			final ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();

			if (DesignerUtil.getContentOutlineSelection() != null)
			{
				persistContext = DesignerUtil.getContentOutlineSelection();
			}
			else
			{
				IStructuredSelection sel = (IStructuredSelection)selectionProvider.getSelection();
				if (!sel.isEmpty())
				{
					Object[] selection = sel.toArray();
					persistContext = selection[0] instanceof PersistContext ? (PersistContext)selection[0]
						: PersistContext.create((IPersist)selection[0], activeEditor.getForm());
				}
			}
		}
		return persistContext;
	}

	private Map<String, Object> getAsMap(JSONObject object)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		for (String key : object.keySet())
		{
			map.put(key, JSONObject.NULL.equals(object.get(key)) ? null : object.get(key));
		}
		return map;
	}

	@Override
	public boolean isEnabled()
	{
		List<String> props = getWizardProperties();
		return props != null && props.size() == 1;
	}

	public List<String> getWizardProperties()
	{
		PersistContext persistContext = getPersistContext();
		if (persistContext != null)
		{
			if (persistContext.getPersist() instanceof WebComponent)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
					.getWebObjectSpecification(((WebComponent)persistContext.getPersist()).getTypeName());
				return spec.getAllPropertiesNames().stream()//
					.filter(prop -> spec.getProperty(prop) != null && "autoshow".equals(spec.getProperty(prop).getTag("wizard"))).collect(Collectors.toList());
			}
		}
		return null;
	}
}
