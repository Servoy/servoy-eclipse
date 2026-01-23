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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand.CreateComponentOptions;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.PersistFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.autowizard.PropertyWizardDialogConfigurator;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author user
 *
 */
public class CreateComponentHandler implements IServerService
{
	protected final ISelectionProvider selectionProvider;
	protected final BaseVisualFormEditor editorPart;

	public CreateComponentHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	public Object executeMethod(String methodName, final JSONObject args)
	{
		CreateComponentOptions options = CreateComponentOptions.fromJson(args);

		if (editorPart.getGraphicaleditor() instanceof RfbVisualFormEditorDesignPage &&
			((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).getShowedContainer() != null && options.getName() != null &&
			options.getDropTarget() == null)
		{
			// if we drop directly on form while zoomed in , do nothing
			return null;
		}
		final String[] menuName = new String[] { null };
		final String[] menuPropertyName = new String[] { null };
		if (options.getName() != null && options.getName().startsWith("servoymenu-"))
		{
			List<WebObjectSpecification> specs = new ArrayList<WebObjectSpecification>();
			WebObjectSpecification[] webComponentSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
			for (WebObjectSpecification webComponentSpec : webComponentSpecifications)
			{
				if (webComponentSpec.isDeprecated()) continue;
				if (webComponentSpec.getProperties(MenuPropertyType.INSTANCE).size() > 0)
				{
					specs.add(webComponentSpec);
				}
			}
			TreeSelectDialog dialog = new TreeSelectDialog(editorPart.getEditorSite().getShell(), true, true, TreePatternFilter.FILTER_LEAFS,
				FlatTreeContentProvider.INSTANCE, new LabelProvider()
				{
					@Override
					public String getText(Object element)
					{
						String displayName = ((WebObjectSpecification)element).getDisplayName();
						if (Utils.stringIsEmpty(displayName))
						{
							displayName = ((WebObjectSpecification)element).getName();
							int index = displayName.indexOf("-");
							if (index != -1)
							{
								displayName = displayName.substring(index + 1);
							}
						}
						return displayName + " [" + ((WebObjectSpecification)element).getPackageName() + "]";
					}
				}, null, null, SWT.NONE, "Select JSMenu compatible component", specs.toArray(new WebObjectSpecification[0]),
				null, false, "SpecDialog", null, false);
			if (dialog.open() == Window.CANCEL)
			{
				return null;
			}
			WebObjectSpecification spec = (WebObjectSpecification)((StructuredSelection)dialog.getSelection()).getFirstElement();
			menuName[0] = options.getName().split("servoymenu-")[1];
			options.setName(spec.getName());
			options.setPackageName(spec.getPackageName());
			menuPropertyName[0] = spec.getProperties(MenuPropertyType.INSTANCE).iterator().next().getName();
		}
		if (options.getName() != null && ("component".equals(options.getName()) || "template".equals(options.getName()) || "*".equals(options.getName())))
		{
			String name = options.getName();
			IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			Command command = (PlatformUI.getWorkbench().getService(ICommandService.class)).getCommand(AddContainerCommand.COMMAND_ID);
			ExecutionEvent executionEvent = null;
			try
			{
				if ("*".equals(name) || "component".equals(name))
				{
					final Event trigger = new Event();
					executionEvent = handlerService.createExecutionEvent(command, trigger);
				}
				else if ("template".equals(name))
				{

					Map<String, String> parameters = new HashMap<>();
					parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.template", "*");
					executionEvent = new ExecutionEvent(command, parameters, new Event(), null);
				}
				command.executeWithChecks(executionEvent);
			}
			catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e)
			{
				Debug.log(e);
			}
			return null;
		}

		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				final IStructuredSelection[] newSelection = new IStructuredSelection[1];
				editorPart.getCommandStack().execute(new CreateComponentCommand(editorPart.getForm(), options, newSelection));
				if (menuName[0] != null)
				{
					WebComponent webComponent = (WebComponent)((PersistContext)newSelection[0].getFirstElement()).getPersist();
					FlattenedSolution flattenedSolution = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
					Menu menu = flattenedSolution.getMenu(menuName[0]);
					if (menu != null)
					{
						webComponent.setProperty(menuPropertyName[0], menu.getUUID());
					}
				}
				if (newSelection[0] != null) selectionProvider.setSelection(newSelection[0]);
			}
		});
		return null;
	}

	public static void autoShowDataProviderSelection(PropertyDescription pd, Form form, AbstractBase webComponent, String propertyName)
	{
		Shell activeShell = UIUtils.getActiveShell();
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);

		Pair<String, ITable> forFoundset = PersistPropertySource.calculateFoundsetTable(pd, PersistContext.create(webComponent, form), flattenedSolution, form);
		ITable table = forFoundset.getRight();
		DataProviderOptions options = new DataProviderOptions(true, true, true, true, true, true, true, true, null, true, true, null);
		DataProviderCellEditor dialog = new DataProviderCellEditor(activeShell, new DataProviderLabelProvider(true), null,
			form, flattenedSolution, false, options, null, table,
			"Select Data Provider - " + (propertyName.endsWith("ID") ? propertyName.substring(0, propertyName.length() - 2) : propertyName));
		Object result = dialog.openDialogBox(activeShell);
		if (result != null)
		{
			if (!result.toString().contains("$NoDataProvider"))
			{
				webComponent.setProperty(propertyName, result.toString());
			}
		}

		UIUtils.restoreFocusToChromium();
	}

	public static void autoshowWizard(ISupportFormElements parentSupportingElements, WebObjectSpecification spec,
		WebComponent webComponent, PropertyDescription property, Form form, AtomicInteger id)
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
				PersistContext context = PersistContext.create(webComponent, parentSupportingElements);
				FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
				ITable table = ServoyModelFinder.getServoyModel().getDataSourceManager()
					.getDataSource(flattenedSolution.getFlattenedForm(form).getDataSource());

				PropertyWizardDialogConfigurator dialogConfigurator = new PropertyWizardDialogConfigurator(UIUtils.getActiveShell(),
					context,
					flattenedSolution, property).withTable(table).withProperties(wizardProperties);
				if (dialogConfigurator.open() == Window.OK)
				{
					List<Map<String, Object>> result = dialogConfigurator.getResult();
					String typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(customObjectType.getName());
					for (int i = 0; i < result.size(); i++)
					{
						Map<String, Object> row = result.get(i);
						String customTypeName = typeName + "_" + id.incrementAndGet();
						while (!PersistFinder.INSTANCE.checkName(form, customTypeName))
						{
							customTypeName = typeName + "_" + id.incrementAndGet();
						}
						WebCustomType bean = AddContainerCommand.addCustomType(webComponent, property.getName(), customTypeName, i, null);
						row.forEach((key, value) -> bean.setProperty(key, value));
					}
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

		UIUtils.restoreFocusToChromium();
	}

}
