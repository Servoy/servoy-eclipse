/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
import java.util.Arrays;
import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.CheckGroupDialog;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.PersistFinder;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetCssAnchoringCommand;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;

/**
 * @author emera
 */
public class SetCssAnchoringHandler implements IServerService
{
	private static final String LEFT = "left";
	private static final String BOTTOM = "bottom";
	private static final String RIGHT = "right";
	private static final String TOP = "top";
	private final BaseVisualFormEditor editorPart;

	public SetCssAnchoringHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		JSONArray selection = args.optJSONArray("selection");
		JSONObject anchors = args.optJSONObject("anchors");
		String top;
		String right;
		String bottom;
		String left;
		if (anchors != null)
		{
			top = anchors.getString(TOP);
			right = anchors.getString(RIGHT);
			bottom = anchors.getString(BOTTOM);
			left = anchors.getString(LEFT);
		}
		else
		{
			List<String> checked = getAnchorsForSelectedElements(selection);
			CheckGroupDialog dialog = new CheckGroupDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Select anchors",
				"Set the following anchors:", new String[] { TOP, RIGHT, BOTTOM, LEFT }, checked.toArray(new String[checked.size()]));
			if (dialog.open() != Window.OK) return null;

			List<Object> sel = Arrays.asList(dialog.getSelected());
			top = sel.contains(TOP) ? "0" : "-1";
			right = sel.contains(RIGHT) ? "0" : "-1";
			bottom = sel.contains(BOTTOM) ? "0" : "-1";
			left = sel.contains(LEFT) ? "0" : "-1";
		}

		CompoundCommand cc = new CompoundCommand();
		List<IPersist> changedPersists = new ArrayList<IPersist>();
		selection.forEach(uuid -> {
			IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), PersistIdentifier.fromJSONString((String)uuid));
			if (persist != null)
			{
				cc.add(new SetCssAnchoringCommand(top, right, bottom, left, persist));
				changedPersists.add(persist);
			}
		});
		if (!cc.isEmpty()) editorPart.getCommandStack().execute(cc);
		if (changedPersists.size() > 0)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changedPersists);
		}
		return null;
	}

	protected List<String> getAnchorsForSelectedElements(JSONArray selection)
	{
		boolean[] selectedAnchors = new boolean[] { true, true, true, true };
		selection.forEach(uuid -> {
			IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), PersistIdentifier.fromJSONString((String)uuid));
			if (persist instanceof BaseComponent)
			{
				CSSPosition position = ((BaseComponent)persist).getCssPosition();
				selectedAnchors[0] = selectedAnchors[0] && CSSPositionUtils.isSet(position.top);
				selectedAnchors[1] = selectedAnchors[1] && CSSPositionUtils.isSet(position.right);
				selectedAnchors[2] = selectedAnchors[2] && CSSPositionUtils.isSet(position.bottom);
				selectedAnchors[3] = selectedAnchors[3] && CSSPositionUtils.isSet(position.left);
			}
		});
		List<String> checked = new ArrayList<>();
		if (selectedAnchors[0]) checked.add(TOP);
		if (selectedAnchors[1]) checked.add(RIGHT);
		if (selectedAnchors[2]) checked.add(BOTTOM);
		if (selectedAnchors[3]) checked.add(LEFT);
		return checked;
	}
}