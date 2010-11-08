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

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.editor.AlignmentFeedbackHelper;
import com.servoy.eclipse.designer.editor.CreateElementRequest;

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
			IFigure feedbackLayer = getFeedbackLayer();
			if (feedbackLayer != null)
			{
				alignmentFeedbackHelper = new AlignmentFeedbackHelper(feedbackLayer);
			}
		}
		return alignmentFeedbackHelper;
	}

	protected IFigure getFeedbackLayer()
	{
		EditPart targetEditPart = getTargetEditPart();
		if (targetEditPart == null || targetEditPart.getViewer() == null)
		{
			return null;
		}
		return LayerManager.Helper.find(targetEditPart).getLayer(LayerConstants.FEEDBACK_LAYER);
	}

	@Override
	protected void showTargetFeedback()
	{
		super.showTargetFeedback();
		AlignmentFeedbackHelper helper = getAlignmentFeedbackHelper();
		if (helper != null)
		{
			helper.showElementAlignmentFeedback(getTargetRequest());
		}
	}

	@Override
	protected void eraseTargetFeedback()
	{
		super.eraseTargetFeedback();
		AlignmentFeedbackHelper helper = getAlignmentFeedbackHelper();
		if (helper != null)
		{
			helper.eraseElementAlignmentFeedback();
		}
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

	/**
	 * Creates a {@link CreateRequest} and sets this tool's factory on the
	 * request.
	 * 
	 * @see org.eclipse.gef.tools.TargetingTool#createTargetRequest()
	 */
	@Override
	protected Request createTargetRequest()
	{
		CreateElementRequest request = new CreateElementRequest(getFactory());
		request.setResizable(true);
		return request;
	}

	/**
	 * Executes the current command and selects the newly created object. The
	 * button that was released to cause this creation is passed in, but since
	 * {@link #handleButtonDown(int)} goes into the invalid state if the button
	 * pressed is not button 1, this will always be button 1.
	 * 
	 * @param button
	 *            the button that was pressed
	 */
	@Override
	protected void performCreation(int button)
	{
		final EditPartViewer viewer = getCurrentViewer();
		executeCurrentCommand();
		Display.getDefault().asyncExec(new Runnable()
		{
			// select the object later, it has not been created yet
			public void run()
			{
				selectAddedObject(viewer);
			}
		});
	}

	/*
	 * Add the newly created object to the viewer's selected objects.
	 */
	protected void selectAddedObject(EditPartViewer viewer)
	{
		final Object model = getCreateRequest().getNewObject();
		if (model == null || viewer == null) return;
		Object editpart = viewer.getEditPartRegistry().get(model);
		if (editpart instanceof EditPart)
		{
			// Force the new object to get positioned in the viewer.
			viewer.flush();
			viewer.select((EditPart)editpart);
		}
	}
}
