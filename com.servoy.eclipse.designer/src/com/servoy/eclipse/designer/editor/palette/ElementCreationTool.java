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

package com.servoy.eclipse.designer.editor.palette;

import org.eclipse.gef.tools.CreationTool;

import com.servoy.eclipse.designer.editor.AlignmentFeedbackHelper;

/**
 * Tool for creating elements from the palette in the form editor.
 * 
 * @author rgansevles
 *
 */
public class ElementCreationTool extends CreationTool
{
	private AlignmentFeedbackHelper alignmentFeedbackHelper;

	/**
	 * @return the alignmentFeedbackHelper
	 */
	public AlignmentFeedbackHelper getAlignmentFeedbackHelper()
	{
		if (alignmentFeedbackHelper == null)
		{
			alignmentFeedbackHelper = new AlignmentFeedbackHelper(getTargetEditPart());
		}
		return alignmentFeedbackHelper;
	}

	@Override
	protected void showTargetFeedback()
	{
		super.showTargetFeedback();
		getAlignmentFeedbackHelper().showElementAlignmentFeedback(getTargetRequest());
	}

	@Override
	protected void eraseTargetFeedback()
	{
		super.eraseTargetFeedback();
		getAlignmentFeedbackHelper().eraseElementAlignmentFeedback();
	}

	/**
	 * @see org.eclipse.gef.Tool#deactivate()
	 */
	@Override
	public void deactivate()
	{
		super.deactivate();
		alignmentFeedbackHelper = null;
	}
}
