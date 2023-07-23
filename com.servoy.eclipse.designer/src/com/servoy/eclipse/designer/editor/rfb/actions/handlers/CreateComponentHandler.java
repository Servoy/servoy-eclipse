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

import static com.servoy.j2db.persistence.CSSPositionUtils.isCSSPositionContainer;
import static org.eclipse.jface.dialogs.MessageDialog.openQuestion;

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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.RagtestCommand.RagtestOptions;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.autowizard.PropertyWizardDialogConfigurator;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;

/**
 * @author user
 *
 */
public class CreateComponentHandler implements IServerService
{
	protected final ISelectionProvider selectionProvider;
	protected final BaseVisualFormEditor editorPart;
	private final AtomicInteger id = new AtomicInteger();

	public CreateComponentHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	public Object executeMethod(String methodName, final JSONObject args)
	{
		if (editorPart.getGraphicaleditor() instanceof RfbVisualFormEditorDesignPage &&
			((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).getShowedContainer() != null && args.has("name") && !args.has("dropTargetUUID"))
		{
			// if we drop directly on form while zoomed in , do nothing
			return null;
		}
		if (args.has("name") && ("component".equals(args.getString("name")) || "template".equals(args.getString("name")) || "*".equals(args.getString("name"))))
		{
			String name = args.getString("name");
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
				ServoyLog.logError(e);
			}
			return null;
		}

		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				RagtestCommand command = new RagtestCommand(editorPart.getForm(), RagtestOptions.fromJson(args), id);
				editorPart.getCommandStack().execute(command);

				IPersist[] newPersists = command.getNewPersists();
				if (newPersists != null)
				{
					IStructuredSelection newSelection = null;
					if (!args.optBoolean("keepOldSelection", false))
					{
						newSelection = new StructuredSelection(
							newPersists.length > 0 ? PersistContext.create(newPersists[0], editorPart.getForm()) : newPersists);
					}

					if (newPersists.length == 1 && newPersists[0] instanceof LayoutContainer && isCSSPositionContainer((LayoutContainer)newPersists[0]))
					{
						if (openQuestion(UIUtils.getActiveShell(), "Edit css position container",
							"Do you want to zoom into the layout container so you can edit it ?"))
						{
							BaseVisualFormEditor editor = DesignerUtil.getActiveEditor();
							if (editor != null)
							{
								BaseVisualFormEditorDesignPage activePage = editor.getGraphicaleditor();
								if (activePage instanceof RfbVisualFormEditorDesignPage)
									((RfbVisualFormEditorDesignPage)activePage).showContainer((LayoutContainer)newPersists[0]);
							}
						}
					}

					if (newSelection != null) selectionProvider.setSelection(newSelection);
				}
			}
		});
		return null;
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
				Display current = Display.getCurrent();
				if (current == null) current = Display.getDefault();

				PersistContext context = PersistContext.create(webComponent, parentSupportingElements);
				FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
				ITable table = ServoyModelFinder.getServoyModel().getDataSourceManager()
					.getDataSource(flattenedSolution.getFlattenedForm(form).getDataSource());

				PropertyWizardDialogConfigurator dialogConfigurator = new PropertyWizardDialogConfigurator(current.getActiveShell(), context,
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
	}
}
