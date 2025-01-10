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
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.RfbSelectionListener;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author user
 *
 */
public class SetSelectionHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final RfbSelectionListener selectionListener;
	private final ISelectionProvider selectionProvider;

	/**
	 * @param editorPart
	 * @param selectionListener
	 * @param selectionProvider
	 */
	public SetSelectionHandler(BaseVisualFormEditor editorPart, RfbSelectionListener selectionListener, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionListener = selectionListener;
		this.selectionProvider = selectionProvider;
	}

	/**
	 * @param methodName
	 * @param args
	 * @throws JSONException
	 */
	public Object executeMethod(String methodName, final JSONObject args) throws JSONException
	{
		JSONArray json = args.getJSONArray("selection");
		final List<Object> selection = new ArrayList<Object>();
		for (int i = 0; i < json.length(); i++)
		{
			String uuid = json.getString(i);

			IPersist searchPersist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), uuid);
			if (searchPersist != null)
			{
				selection.add(PersistContext.create(searchPersist, editorPart.getForm()));
			}
			else
			{
				//check if group
				FormElementGroup group = new FormElementGroup(uuid, ModelUtils.getEditingFlattenedSolution(editorPart.getForm()), editorPart.getForm());
				if (group.getElements().hasNext()) selection.add(group);
			}
		}
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				IStructuredSelection structuredSelection = new StructuredSelection(selection);
				selectionListener.setLastSelection(structuredSelection);
				selectionProvider.setSelection(selection.size() == 0 ? null : structuredSelection);

				// if a set selection came from the browser, then this should have focus and should be the active part
				if (selection.size() > 0 && editorPart != PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart())
				{
					// if that is not the case make it active, can happen in certain conditions
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editorPart);
				}

			}
		});
		return null;
	}
}
