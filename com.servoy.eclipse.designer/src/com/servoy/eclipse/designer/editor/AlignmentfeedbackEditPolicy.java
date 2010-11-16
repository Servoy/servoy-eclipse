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

package com.servoy.eclipse.designer.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Handle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.handles.MoveHandle;
import org.eclipse.gef.handles.ResizeHandle;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.gef.tools.ResizeTracker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Menu;

import com.servoy.eclipse.designer.actions.ModifyAnchoringAction;
import com.servoy.eclipse.designer.actions.SetAnchoringAction;
import com.servoy.eclipse.designer.util.AbsoluteLocator;
import com.servoy.eclipse.designer.util.AnchoringFigure;
import com.servoy.eclipse.designer.util.FigureMovedTracker;
import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * Edit policy for moving/resizing elements.
 * Alignment feedback is given.
 * 
 * @author rgansevles
 *
 */
final public class AlignmentfeedbackEditPolicy extends ResizableEditPolicy
{
	/**
	 * A viewer property indicating whether the anchor feedback is enabled. The value must  be a Boolean.
	 */
	public static final String PROPERTY_ANCHOR_FEEDBACK_ENABLED = "AlignmentfeedbackEditPolicy.anchorFeedbackEnabled"; //$NON-NLS-1$

	/**
	 * A viewer property indicating whether the same size feedback is enabled. The value must  be a Boolean.
	 */
	public static final String PROPERTY_SAME_SIZE_FEEDBACK_ENABLED = "AlignmentfeedbackEditPolicy.sameSizeFeedbackEnabled"; //$NON-NLS-1$

	private AlignmentFeedbackHelper alignmentFeedbackHelper;

	/**
	 * the feedback figure for the selected element.
	 */
	protected IFigure selectedElementFeedbackFigure;

	/**
	 * the figure for anchoring.
	 */
	protected Clickable anchoringFigure;

	private final FormGraphicalEditPart container;

	private MenuManager menuManager;

	private boolean isSelected;

	private final PropertyChangeListener viewerPropertyListener = new PropertyChangeListener()
	{

		public void propertyChange(PropertyChangeEvent evt)
		{
			String property = evt.getPropertyName();
			if (isSelected && property.equals(AlignmentfeedbackEditPolicy.PROPERTY_ANCHOR_FEEDBACK_ENABLED))
			{
				addAnchoringFigure();
			}
		}
	};

	public AlignmentfeedbackEditPolicy(FormGraphicalEditPart container)
	{
		this.container = container;
	}

	@Override
	public void activate()
	{
		super.activate();
		getHost().getViewer().addPropertyChangeListener(viewerPropertyListener);
	}

	@Override
	public void deactivate()
	{
		super.deactivate();
		getHost().getViewer().removePropertyChangeListener(viewerPropertyListener);
	}

	/**
	 * @return the alignmentFeedbackHelper
	 */
	public AlignmentFeedbackHelper getAlignmentFeedbackHelper()
	{
		if (alignmentFeedbackHelper == null)
		{
			alignmentFeedbackHelper = new AlignmentFeedbackHelper(getFeedbackLayer());
		}
		return alignmentFeedbackHelper;
	}

	@Override
	public GraphicalEditPart getHost()
	{
		return (GraphicalEditPart)super.getHost();
	}

	@Override
	protected List<Handle> createSelectionHandles()
	{
		List<Handle> list = new ArrayList<Handle>();
		GraphicalEditPart part = getHost();

		MoveHandle moveHandler = new MoveHandle(part)
		{
			@Override
			protected DragTracker createDragTracker()
			{
				DragEditPartsTracker tracker = new DragEditPartsTracker(getOwner())
				{
					/*
					 * No direct edit from move handle
					 */
					@Override
					protected void performDirectEdit()
					{
					}
				};
				tracker.setDefaultCursor(getCursor());
				return tracker;
			}
		};
		list.add(moveHandler);
		list.add(createResizeHandle(part, PositionConstants.EAST));
		list.add(createResizeHandle(part, PositionConstants.SOUTH_EAST));
		list.add(createResizeHandle(part, PositionConstants.SOUTH));
		list.add(createResizeHandle(part, PositionConstants.SOUTH_WEST));
		list.add(createResizeHandle(part, PositionConstants.WEST));
		list.add(createResizeHandle(part, PositionConstants.NORTH_WEST));
		list.add(createResizeHandle(part, PositionConstants.NORTH));
		list.add(createResizeHandle(part, PositionConstants.NORTH_EAST));

		return list;
	}

	protected Handle createResizeHandle(GraphicalEditPart owner, final int direction)
	{
		ResizeHandle resizeHandle = new ResizeHandle(owner, direction);
		resizeHandle.setDragTracker(new ResizeTracker(owner, direction)
		{
			@Override
			protected void updateSourceRequest()
			{
				super.updateSourceRequest();
				BasePersistGraphicalEditPart.limitChangeBoundsRequest((ChangeBoundsRequest)getSourceRequest());
			}
		});
		return resizeHandle;
	}

	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.showChangeBoundsFeedback(request);
		removeSelectedElementFeedbackFigure();
		removeAnchoringFigure();
		getAlignmentFeedbackHelper().showElementAlignmentFeedback(request);
	}

	@Override
	protected void eraseChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.eraseChangeBoundsFeedback(request);
		getAlignmentFeedbackHelper().eraseElementAlignmentFeedback();
		addSelectedElementFeedbackFigure();
		addAnchoringFigure();
	}

	@Override
	protected void hideSelection()
	{
		super.hideSelection();
		removeSelectedElementFeedbackFigure();
		removeAnchoringFigure();
		isSelected = false;
	}

	@Override
	protected void showSelection()
	{
		super.showSelection();
		addSelectedElementFeedbackFigure();
		addAnchoringFigure();
		isSelected = true;
	}

	protected void removeSelectedElementFeedbackFigure()
	{
		if (selectedElementFeedbackFigure != null)
		{
			getLayer(FormGraphicalRootEditPart.SELECTED_ELEMENT_FEEDBACK_LAYER).remove(selectedElementFeedbackFigure);
			selectedElementFeedbackFigure = null;
		}
	}

	/**
	 * Adds the alignment to the feedback layer.
	 */
	protected void addSelectedElementFeedbackFigure()
	{
		removeSelectedElementFeedbackFigure();
		getLayer(FormGraphicalRootEditPart.SELECTED_ELEMENT_FEEDBACK_LAYER).add(
			selectedElementFeedbackFigure = new SelectedElementFeedbackFigure(container, getHost()));
	}

	protected void removeAnchoringFigure()
	{
		if (anchoringFigure != null)
		{
			getLayer(LayerConstants.HANDLE_LAYER).remove(anchoringFigure);
			anchoringFigure = null;
		}
	}

	/**
	 * Adds the an anchoring figure to the feedback layer.
	 */
	protected void addAnchoringFigure()
	{
		if (!Boolean.TRUE.equals(getHost().getViewer().getProperty(AlignmentfeedbackEditPolicy.PROPERTY_ANCHOR_FEEDBACK_ENABLED)))
		{
			removeAnchoringFigure();
			return;
		}

		if (anchoringFigure == null && getHost().getModel() instanceof ISupportAnchors)
		{
			anchoringFigure = new Clickable(new AnchoringFigure((ISupportAnchors)getHost().getModel()), SWT.NONE);
			getLayer(LayerConstants.HANDLE_LAYER).add(anchoringFigure);

			getHost().getFigure().addAncestorListener(new FigureMovedTracker(anchoringFigure, new AbsoluteLocator(getHost().getFigure(), false, 4, true, 2)));
			anchoringFigure.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					showAnchoringmenu();
				}
			});
		}
	}

	protected void showAnchoringmenu()
	{
		if (menuManager == null)
		{
			menuManager = new MenuManager();
			menuManager.setRemoveAllWhenShown(true);
			menuManager.addMenuListener(new IMenuListener()
			{
				public void menuAboutToShow(IMenuManager mgr)
				{
					fillAnchoringMenu();
				}

			});
			menuManager.setVisible(true);
		}
		Menu popup = menuManager.createContextMenu(container.getViewer().getControl());
		Rectangle clickableBounds = anchoringFigure.getBounds().getCopy();
		anchoringFigure.translateToAbsolute(clickableBounds);
		Point location = container.getViewer().getControl().toDisplay(clickableBounds.x, clickableBounds.y + clickableBounds.height);
		popup.setLocation(location);
		popup.setVisible(true);
	}

	protected void fillAnchoringMenu()
	{
		menuManager.add(new ModifyAnchoringAction(container.getEditorPart(), getHost(), AnchorPropertySource.TOP));
		menuManager.add(new ModifyAnchoringAction(container.getEditorPart(), getHost(), AnchorPropertySource.RIGHT));
		menuManager.add(new ModifyAnchoringAction(container.getEditorPart(), getHost(), AnchorPropertySource.BOTTOM));
		menuManager.add(new ModifyAnchoringAction(container.getEditorPart(), getHost(), AnchorPropertySource.LEFT));

		menuManager.add(new Separator());

		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.NORTH | IAnchorConstants.EAST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.SOUTH | IAnchorConstants.EAST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.SOUTH | IAnchorConstants.WEST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.NORTH | IAnchorConstants.WEST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.NORTH | IAnchorConstants.WEST | IAnchorConstants.SOUTH |
			IAnchorConstants.EAST));
	}
}