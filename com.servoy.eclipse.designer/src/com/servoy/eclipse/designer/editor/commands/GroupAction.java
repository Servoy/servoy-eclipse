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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.GroupGraphicalEditPart;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * An action to group selected objects.
 */
public class GroupAction extends DesignerSelectionAction
{
	private static final class FormElementComparator implements Comparator<IFormElement>
	{
		public final static FormElementComparator INSTANCE = new FormElementComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(IFormElement o1, IFormElement o2)
		{
			return o1.getFormIndex() - o2.getFormIndex();
		}
	}

	public GroupAction(IWorkbenchPart part)
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
		setText(DesignerActionFactory.GROUP_TEXT);
		setToolTipText(DesignerActionFactory.GROUP_TOOLTIP);
		setId(DesignerActionFactory.GROUP.getId());
		setImageDescriptor(DesignerActionFactory.GROUP_IMAGE);
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		return createGroupingRequests(selected);
	}

	public static Map<EditPart, Request> createGroupingRequests(List<EditPart> selected)
	{
		// check existing groups
		String groupID = null;
		int ngroups = 0;
		List<EditPart> affectedEditparts = new ArrayList<EditPart>();
		for (EditPart editPart : selected)
		{
			if (editPart instanceof GroupGraphicalEditPart)
			{
				ngroups++;
				groupID = ((GroupGraphicalEditPart)editPart).getGroup().getGroupID();
				affectedEditparts.addAll(editPart.getChildren());
			}
			else
			{
				affectedEditparts.add(editPart);
			}
		}

		if (affectedEditparts.size() == 0)
		{
			return null;
		}

		if (ngroups != 1)
		{
			// reuse the group if only 1 was part of the selection
			groupID = UUID.randomUUID().toString();
		}

		Map<EditPart, Request> requests = new HashMap<EditPart, Request>(affectedEditparts.size());
		for (EditPart editPart : affectedEditparts)
		{
			requests.put(editPart, new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName(),
				groupID, "group"));
		}

		return requests;
	}

	@Override
	protected Command createCommand(List objects)
	{
		if (objects.isEmpty()) return null;
		if (!(objects.get(0) instanceof EditPart)) return null;

		Map<EditPart, Request> requests = createGroupingRequests(objects);
		CompoundCommand compoundCmd = null;
		EditPartViewer viewer = null;
		if (requests != null)
		{
			for (Entry<EditPart, Request> entry : requests.entrySet())
			{
				if (viewer == null) viewer = entry.getKey().getViewer();
				Command cmd = entry.getKey().getCommand(entry.getValue());
				if (cmd != null)
				{
					if (compoundCmd == null)
					{
						compoundCmd = new CompoundCommand();
					}
					compoundCmd.add(cmd);
				}
			}
		}

		Map<EditPart, Request> zOrderRequests = createZOrderRequests(objects);

		if (zOrderRequests != null)
		{
			for (Entry<EditPart, Request> entry : zOrderRequests.entrySet())
			{
				if (viewer == null) viewer = entry.getKey().getViewer();
				Command cmd = entry.getKey().getCommand(entry.getValue());
				if (cmd != null)
				{
					if (compoundCmd == null)
					{
						compoundCmd = new CompoundCommand();
					}
					compoundCmd.add(cmd);
				}
			}
		}

		if (compoundCmd == null)
		{
			return null;
		}
		return new SelectModelsCommandWrapper(viewer, getToRefresh(requests.keySet()), compoundCmd.unwrap());
	}

	protected static Map<EditPart, Request> createZOrderRequests(List<EditPart> selected)
	{
		EditPart firstEditPart = selected.get(0);
		Map<Object, EditPart> editPartMap = firstEditPart.getViewer().getEditPartRegistry();

		EditPart formEditPart = firstEditPart.getParent();
		while (formEditPart != null && !(formEditPart.getModel() instanceof Form))
		{
			formEditPart = formEditPart.getParent();
		}

		if (formEditPart == null) return null;

		Form flattenedForm = ModelUtils.getEditingFlattenedSolution((Form)formEditPart.getModel()).getFlattenedForm((Form)formEditPart.getModel());

		List<IFormElement> groupModels = null;
		Set<UUID> groupUUIDSet = null;

		IFormElement highestIndexOfGroupElement = null;

		for (EditPart editPart : selected)
		{
			if (editPart instanceof GraphicalEditPart && editPart.getModel() instanceof IFormElement)
			{
				if (groupModels == null)
				{
					groupModels = new ArrayList<IFormElement>();
					groupUUIDSet = new HashSet<UUID>();
				}
				IFormElement model = (IFormElement)editPart.getModel();
				groupModels.add(model);
				groupUUIDSet.add(model.getUUID());
				if (highestIndexOfGroupElement == null || highestIndexOfGroupElement.getFormIndex() < model.getFormIndex())
				{
					highestIndexOfGroupElement = model;
				}
			}
		}

		if (groupModels == null) return null;
		if (groupModels.size() > 1)
		{
			Collections.sort(groupModels, FormElementComparator.INSTANCE);
		}

		List<IFormElement> higherModels = null;
		Set<UUID> higherUUIDSet = null;
		List<IFormElement> lowerModels = null;
		Set<UUID> lowerUUIDSet = null;

		for (IFormElement bc : groupModels)
		{
			List<IFormElement> overlappingElements = ElementUtil.getOverlappingFormElements(flattenedForm, bc);
			if (overlappingElements != null && !overlappingElements.isEmpty())
			{
				for (IFormElement overlapping : overlappingElements)
				{
					if (overlapping.getFormIndex() >= highestIndexOfGroupElement.getFormIndex() && !groupUUIDSet.contains(overlapping.getUUID()))
					{
						if (higherModels == null)
						{
							higherModels = new ArrayList<IFormElement>();
							higherUUIDSet = new HashSet<UUID>();
						}
						if (higherUUIDSet.add(overlapping.getUUID()))
						{
							higherModels.add(overlapping);
						}
					}
					else if (overlapping.getFormIndex() < highestIndexOfGroupElement.getFormIndex() && !groupUUIDSet.contains(overlapping.getUUID()))
					{
						if (lowerModels == null)
						{
							lowerModels = new ArrayList<IFormElement>();
							lowerUUIDSet = new HashSet<UUID>();
						}
						if (lowerUUIDSet.add(overlapping.getUUID()))
						{
							lowerModels.add(overlapping);
						}
					}
				}
			}
		}

		int index = 0;
		Map<EditPart, Request> requests = new HashMap<EditPart, Request>();
		if (lowerModels != null)
		{
			if (lowerModels.size() > 1)
			{
				Collections.sort(lowerModels, FormElementComparator.INSTANCE);
			}

			for (IFormElement bc : lowerModels)
			{
				requests.put(
					editPartMap.get(bc),
					new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(),
						Integer.valueOf(index++), "")); //$NON-NLS-1$
			}
		}

		for (IFormElement bc : groupModels)
		{
			requests.put(
				editPartMap.get(bc),
				new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(),
					Integer.valueOf(index++), "")); //$NON-NLS-1$
		}

		if (higherModels != null)
		{
			if (higherModels.size() > 1)
			{
				Collections.sort(higherModels, FormElementComparator.INSTANCE);
			}
			for (IFormElement bc : higherModels)
			{
				requests.put(
					editPartMap.get(bc),
					new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(),
						Integer.valueOf(index++), "")); //$NON-NLS-1$
			}
		}

		return requests;
	}

	@Override
	protected Iterable<EditPart> getToRefresh(Iterable<EditPart> affected)
	{
		return DesignerUtil.getFormEditparts(affected);
	}
}
