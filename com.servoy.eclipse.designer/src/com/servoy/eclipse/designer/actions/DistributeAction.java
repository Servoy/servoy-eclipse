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

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.DistributeRequest.Distribution;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.DesignerSelectionAction;

/**
 * An action to distribute objects.
 */
public class DistributeAction extends DesignerSelectionAction
{
	private final Distribution distribution;

	public DistributeAction(IWorkbenchPart part, Distribution distribution)
	{
		super(part, VisualFormEditor.REQ_DISTRIBUTE);
		this.distribution = distribution;
		init(distribution);
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
				break;

			case HORIZONTAL_CENTERS :
				setText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER_IMAGE);
				break;

			case HORIZONTAL_PACK :
				setText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK_IMAGE);
				break;

			case VERTICAL_SPACING :
				setText(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING_IMAGE);
				break;

			case VERTICAL_CENTERS :
				setText(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER_IMAGE);
				break;

			case VERTICAL_PACK :
				setText(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK_TEXT);
				setToolTipText(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK_TOOLTIP);
				setId(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK.getId());
				setImageDescriptor(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK_IMAGE);
				break;
		}

	}

	@Override
	protected GroupRequest createRequest(List<EditPart> objects)
	{
		DistributeRequest distributeRequest = new DistributeRequest(requestType, distribution);
		distributeRequest.setEditParts(objects);
		return distributeRequest;
	}
}
