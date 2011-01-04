/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * An action to change the tab sequence of selected objects.
 */
public class SetTabSequenceAction extends DesignerSelectionAction
{

	public SetTabSequenceAction(IWorkbenchPart part)
	{
		super(part, null);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.SET_TAB_SEQUENCE_TEXT);
		setToolTipText(DesignerActionFactory.SET_TAB_SEQUENCE_TOOLTIP);
		setId(DesignerActionFactory.SET_TAB_SEQUENCE.getId());
		setImageDescriptor(DesignerActionFactory.SET_TAB_SEQUENCE_IMAGE);
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		return createSetTabSeqRequests(selected);
	}

	public static Map<EditPart, Request> createSetTabSeqRequests(List<EditPart> selected)
	{
		if (selected == null || selected.size() < 2)
		{
			return null;
		}

		EditPart first = selected.get(0);
		Object model = first.getModel();
		if (!(model instanceof IPersist))
		{
			return null;
		}
		Map<Object, EditPart> editPartRegistry = first.getViewer().getEditPartRegistry();

		// find all edit parts of the current form
		List<EditPart> tabSeqEditParts = new ArrayList<EditPart>();

		Iterator<IPersist> allObjects = ((IPersist)model).getParent().getAllObjects();
		while (allObjects.hasNext())
		{
			IPersist ipersist = allObjects.next();
			if (ipersist instanceof ISupportTabSeq)
			{
				EditPart ep = editPartRegistry.get(ipersist);
				if (ep != null)
				{
					tabSeqEditParts.add(ep);
				}
			}
		}

		Map<EditPart, Request> requests = new HashMap<EditPart, Request>(tabSeqEditParts.size());
		for (EditPart editPart : tabSeqEditParts)
		{
			int index = selected.indexOf(editPart);
			requests.put(
				editPart,
				new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(), Integer.valueOf(index < 0
					? ISupportTabSeq.SKIP : index + 1), "set tab sequence"));
		}

		return requests;
	}

	@Override
	protected boolean calculateEnabled()
	{
		return !DesignerUtil.containsInheritedElement(getSelectedObjects()) && super.calculateEnabled();
	}
}
