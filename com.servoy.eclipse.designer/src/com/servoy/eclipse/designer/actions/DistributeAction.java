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
package com.servoy.eclipse.designer.actions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.DistributeRequest.Distribution;
import com.servoy.eclipse.designer.editor.PersistGraphicalEditPart;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.MultipleSelectionAction;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.PositionComparator;

/**
 * An action to distribute objects.
 */
public class DistributeAction extends MultipleSelectionAction
{
	public static final Comparator< ? super EditPart> XY_POSITION_COMPARATOR = new EditorPartPositionComparator(true);
	public static final Comparator< ? super EditPart> YX_POSITION_COMPARATOR = new EditorPartPositionComparator(false);

	private final Distribution distribution;
	private Comparator< ? super EditPart> comparator;

	public DistributeAction(IWorkbenchPart part, Distribution distribution)
	{
		super(part, VisualFormEditor.REQ_DISTRIBUTE);
		this.distribution = distribution;
		init(distribution);
	}

	public static class EditorPartPositionComparator implements Comparator<EditPart>
	{
		private final boolean xy;

		private EditorPartPositionComparator(boolean xy)
		{
			this.xy = xy;
		}

		public int compare(EditPart e1, EditPart e2)
		{
			if (e1 instanceof PersistGraphicalEditPart && e2 instanceof PersistGraphicalEditPart)
			{
				IPersist o1 = ((PersistGraphicalEditPart)e1).getPersist();
				IPersist o2 = ((PersistGraphicalEditPart)e2).getPersist();
				if (o1 instanceof ISupportBounds && o2 instanceof ISupportBounds)
				{
					return PositionComparator.comparePoint(xy, CSSPosition.getLocation((ISupportBounds)o1), CSSPosition.getLocation((ISupportBounds)o2));
				}
				if (o1 instanceof ISupportBounds && !(o2 instanceof ISupportBounds))
				{
					return -1;
				}
				if (!(o1 instanceof ISupportBounds) && o2 instanceof ISupportBounds)
				{
					return 1;
				}
			}
			return 0;
		}
	}


	/**
	 * Initializes this action's text and images.
	 */
	protected void init(Distribution dist)
	{
		switch (dist)
		{
			case HORIZONTAL_SPACING :
				setText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_SPACING_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_SPACING_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_SPACING.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_SPACING_IMAGE);
				comparator = XY_POSITION_COMPARATOR;
				break;

			case HORIZONTAL_CENTERS :
				setText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER_IMAGE);
				comparator = XY_POSITION_COMPARATOR;
				break;

			case HORIZONTAL_PACK :
				setText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK_IMAGE);
				comparator = XY_POSITION_COMPARATOR;
				break;

			case VERTICAL_SPACING :
				setText(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING_IMAGE);
				comparator = YX_POSITION_COMPARATOR;
				break;

			case VERTICAL_CENTERS :
				setText(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER_IMAGE);
				comparator = YX_POSITION_COMPARATOR;
				break;

			case VERTICAL_PACK :
				setText(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK_IMAGE);
				comparator = YX_POSITION_COMPARATOR;
				break;
		}

	}

	@Override
	protected GroupRequest createRequest(List<EditPart> objects)
	{
		DistributeRequest distributeRequest = new DistributeRequest(requestType, distribution);
		Collections.sort(objects, comparator);
		distributeRequest.setEditParts(objects);
		return distributeRequest;
	}
}
