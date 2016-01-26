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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.ui.keys.IBindingService;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.ToggleAnchoringActionDelegateHandler.ToggleAnchoringBottom;
import com.servoy.eclipse.designer.editor.commands.ToggleAnchoringActionDelegateHandler.ToggleAnchoringLeft;
import com.servoy.eclipse.designer.editor.commands.ToggleAnchoringActionDelegateHandler.ToggleAnchoringRight;
import com.servoy.eclipse.designer.editor.commands.ToggleAnchoringActionDelegateHandler.ToggleAnchoringTop;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;

/**
 * Send the shortcuts for the editor context menu.
 * @author emera
 */
public class ShortcutsHandler implements IServerService
{

	private final BaseVisualFormEditor editorPart;

	private static final Set<String> shortcutNames = new HashSet<String>(Arrays.asList(
		new String[] { OpenScriptAction.OPEN_SCRIPT_ID, DesignerActionFactory.SET_TAB_SEQUENCE_ID, DesignerActionFactory.SAME_WIDTH_ID, DesignerActionFactory.SAME_HEIGHT_ID, ToggleAnchoringTop.TOGGLE_ANCHORING_TOP_ID, ToggleAnchoringRight.TOGGLE_ANCHORING_RIGHT_ID, ToggleAnchoringBottom.TOGGLE_ANCHORING_BOTTOM_ID, ToggleAnchoringLeft.TOGGLE_ANCHORING_LEFT_ID, DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_ID, DesignerActionFactory.SEND_TO_BACK_ONE_STEP_ID, DesignerActionFactory.BRING_TO_FRONT_ID, DesignerActionFactory.SEND_TO_BACK_ID }));

	public ShortcutsHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.websocket.IServerService#executeMethod(java.lang.String, org.json.JSONObject)
	 */
	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		StringWriter stringWriter = new StringWriter();
		final JSONWriter writer = new JSONWriter(stringWriter);
		writer.object();
		IBindingService bindingService = editorPart.getSite().getService(IBindingService.class);
		Set<String> set = new HashSet<String>();
		for (Binding binding : bindingService.getBindings())
		{
			ParameterizedCommand cmd = binding.getParameterizedCommand();
			if (cmd != null && shortcutNames.contains(cmd.getId()) && !set.contains(cmd.getId())) //binding.getParameterizedCommand().getCommand().getCategory().getName().equals("Servoy"))
			{
				writer.key(cmd.getId()).value(binding.getTriggerSequence().toString());
				set.add(cmd.getId());
			}
		}
		writer.endObject();
		return new JSONObject(stringWriter.getBuffer().toString());
	}
}
