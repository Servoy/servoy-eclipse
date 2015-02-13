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

package com.servoy.eclipse.designer.editor.rfb;

import java.util.HashMap;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentsHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.GetPartStylesHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.GhostHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.KeyPressedHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.MoveInResponsiveLayoutHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenContainedFormHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenElementWizardHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenScriptHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetPropertiesHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetSelectionHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetTabSequenceCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SpacingCentersPack;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.UpdateFieldPositioner;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.ZOrderCommand;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Handle requests from the rfb html editor.
 *
 * @author rgansevles
 *
 */
public class EditorServiceHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;
	private final RfbSelectionListener selectionListener;
	private final IFieldPositioner fieldPositioner;

	private final HashMap<String, IServerService> configuredHandlers = new HashMap<String, IServerService>();

	public EditorServiceHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider, RfbSelectionListener selectionListener,
		IFieldPositioner fieldPositioner)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
		this.selectionListener = selectionListener;
		this.fieldPositioner = fieldPositioner;
		configureHandlers();
	}

	private void configureHandlers()
	{
		configuredHandlers.put("getGhostComponents", new GhostHandler(editorPart));
		configuredHandlers.put("setSelection", new SetSelectionHandler(editorPart, selectionListener, selectionProvider));
		configuredHandlers.put("setTabSequence", new SetTabSequenceCommand(editorPart, selectionProvider));

		configuredHandlers.put("z_order_bring_to_front_one_step", new ZOrderCommand(editorPart, selectionProvider));
		configuredHandlers.put("z_order_send_to_back_one_step", new ZOrderCommand(editorPart, selectionProvider));
		configuredHandlers.put("z_order_bring_to_front", new ZOrderCommand(editorPart, selectionProvider));
		configuredHandlers.put("z_order_send_to_back", new ZOrderCommand(editorPart, selectionProvider));

		configuredHandlers.put("horizontal_spacing", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("vertical_spacing", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("horizontal_centers", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("vertical_centers", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("horizontal_pack", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("vertical_pack", new SpacingCentersPack(editorPart, selectionProvider));

		configuredHandlers.put("keyPressed", new KeyPressedHandler(editorPart, selectionProvider));
		configuredHandlers.put("setProperties", new SetPropertiesHandler(editorPart));
		configuredHandlers.put("moveComponent", new MoveInResponsiveLayoutHandler(editorPart));
		configuredHandlers.put("createComponent", new CreateComponentHandler(editorPart, selectionProvider));
		configuredHandlers.put("getPartsStyles", new GetPartStylesHandler(editorPart));
		configuredHandlers.put("createComponents", new CreateComponentsHandler(editorPart, selectionProvider));
		configuredHandlers.put("openElementWizard", new OpenElementWizardHandler(editorPart, fieldPositioner, selectionProvider));
		configuredHandlers.put("updateFieldPositioner", new UpdateFieldPositioner(fieldPositioner));
		configuredHandlers.put("openScript", new OpenScriptHandler(editorPart));
		configuredHandlers.put("openContainedForm", new OpenContainedFormHandler(editorPart));
		configuredHandlers.put("setInlineEditMode", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args.has("inlineEdit"))
				{
					boolean inlineEdit = args.getBoolean("inlineEdit");
					if (inlineEdit)
					{
						editorPart.deactivateEditorContext();
					}
					else
					{
						editorPart.activateEditorContext();
					}
				}
				return null;
			}
		});

		configuredHandlers.put("isInheritedForm", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				return editorPart.getForm().getExtendsID() > 0;
			}

		});
		configuredHandlers.put("getShortcuts", new ShortcutsHandler(editorPart));
	}

	/**
	 * @param uuid
	 * @return
	 */

	@Override
	public Object executeMethod(final String methodName, final JSONObject args)
	{
		try
		{
			return configuredHandlers.get(methodName).executeMethod(methodName, args);
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}
}