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
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
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
import com.servoy.eclipse.designer.util.PersistChangedTracker;
import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.Part;

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
	 * A viewer property indicating whether the alignment feedback is enabled. The value must  be a Boolean.
	 */
	public static final String PROPERTY_ALIGMENT_FEEDBACK_VISIBLE = "AlignmentfeedbackEditPolicy.alignmentFeedbackVisible";
	/**
	 * A viewer property indicating whether the anchor feedback is enabled. The value must  be a Boolean.
	 */
	public static final String PROPERTY_ANCHOR_FEEDBACK_VISIBLE = "AlignmentfeedbackEditPolicy.anchorFeedbackVisible";

	/**
	 * A viewer property indicating whether the same size feedback is enabled. The value must  be a Boolean.
	 */
	public static final String PROPERTY_SAME_SIZE_FEEDBACK_VISIBLE = "AlignmentfeedbackEditPolicy.sameSizeFeedbackVisible";

	private AlignmentFeedbackHelper alignmentFeedbackHelper;

	private FigureMovedTracker alignmentSelectectElementFeedbackTracker;

	private AlignmentFeedbackHelper alignmentSelectectElementFeedbackHelper;

	/**
	 * the tracker for the anchoring figure.
	 */
	protected PersistChangedTracker anchoringFigureTracker;

	/**
	 * the trackers for same-size feedback figure.
	 */
	protected List<FigureMovedTracker> sameSizeFigureTrackers = new ArrayList<FigureMovedTracker>();

	private final FormGraphicalEditPart container;

	private MenuManager menuManager;

	private boolean isSelected;

	private final PropertyChangeListener viewerPropertyListener = new PropertyChangeListener()
	{

		public void propertyChange(PropertyChangeEvent evt)
		{
			String property = evt.getPropertyName();
			if (isSelected)
			{
				if (property.equals(AlignmentfeedbackEditPolicy.PROPERTY_ANCHOR_FEEDBACK_VISIBLE))
				{
					removeAnchoringFigure();
					addAnchoringFigure();
				}
				else if (property.equals(AlignmentfeedbackEditPolicy.PROPERTY_SAME_SIZE_FEEDBACK_VISIBLE))
				{
					removeSameSizeFeedback();
					addSameSizeFeedback();
				}
				else if (property.equals(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE))
				{
					removeSelectedElementAlignmentFeedback();
					addSelectedElementAlignmentFeedback();
				}
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

	/**
	 * @return the alignmentFeedbackHelper
	 */
	public AlignmentFeedbackHelper getAlignmentSelectedElementFeedbackHelper()
	{
		if (alignmentSelectectElementFeedbackHelper == null)
		{
			alignmentSelectectElementFeedbackHelper = new AlignmentFeedbackHelper(getLayer(FormGraphicalRootEditPart.SELECTED_ELEMENT_FEEDBACK_LAYER));
		}
		return alignmentSelectectElementFeedbackHelper;
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

			@Override
			protected Dimension getMinimumSizeFor(ChangeBoundsRequest request)
			{
				List editParts = request.getEditParts();
				if (editParts.size() == 1 && ((PersistGraphicalEditPart)editParts.get(0)).getModel() instanceof GraphicalComponent)
				{
					return new Dimension(1, 1);
				}
				return IFigure.MIN_DIMENSION;

			}
		});
		return resizeHandle;
	}

	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.showChangeBoundsFeedback(request);
		removeSameSizeFeedback();
		removeSelectedElementAlignmentFeedback();
		removeAnchoringFigure();
		getAlignmentFeedbackHelper().showElementAlignmentFeedback(request);
	}

	@Override
	protected void eraseChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.eraseChangeBoundsFeedback(request);
		getAlignmentFeedbackHelper().eraseElementAlignmentFeedback();
		addSameSizeFeedback();
		addSelectedElementAlignmentFeedback();
		addAnchoringFigure();
	}

	@Override
	protected void hideSelection()
	{
		super.hideSelection();
		removeSameSizeFeedback();
		removeSelectedElementAlignmentFeedback();
		removeAnchoringFigure();
		isSelected = false;
	}

	@Override
	protected void showSelection()
	{
		super.showSelection();
		addSameSizeFeedback();
		addSelectedElementAlignmentFeedback();
		addAnchoringFigure();
		isSelected = true;
	}

	protected void removeAnchoringFigure()
	{
		if (anchoringFigureTracker != null)
		{
			anchoringFigureTracker.unhook();
			anchoringFigureTracker.getFigure().getParent().remove(anchoringFigureTracker.getFigure());
			anchoringFigureTracker = null;
		}
	}

	/**
	 * Adds the an anchoring figure to the feedback layer.
	 */
	protected void addAnchoringFigure()
	{
		if (!Boolean.TRUE.equals(getHost().getViewer().getProperty(AlignmentfeedbackEditPolicy.PROPERTY_ANCHOR_FEEDBACK_VISIBLE)))
		{
			return;
		}

		if (container != null && container.getEditorPart().getForm().getUseCssPosition())
		{
			return;
		}
		if (anchoringFigureTracker == null && getHost().getModel() instanceof ISupportAnchors && getHost().getModel() instanceof IPersist)
		{
			Clickable anchoringFigure = new Clickable(new AnchoringFigure((ISupportAnchors)getHost().getModel()), SWT.NONE);
			anchoringFigure.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					showAnchoringmenu();
				}
			});
			getLayer(LayerConstants.HANDLE_LAYER).add(anchoringFigure);
			anchoringFigureTracker = new PersistChangedTracker(anchoringFigure, (IPersist)getHost().getModel(),
				new AbsoluteLocator(getHost().getFigure(), false, 4, true, 2));
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
		IFigure anchoringFigure = anchoringFigureTracker.getFigure();
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

		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.NORTH | IAnchorConstants.WEST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.NORTH | IAnchorConstants.EAST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.SOUTH | IAnchorConstants.EAST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.SOUTH | IAnchorConstants.WEST));
		menuManager.add(new SetAnchoringAction(container.getEditorPart(), getHost(), IAnchorConstants.ALL));
	}

	protected void removeSameSizeFeedback()
	{
		for (FigureMovedTracker tracker : sameSizeFigureTrackers)
		{
			tracker.unhook();
			tracker.getFigure().getParent().remove(tracker.getFigure());
		}
		sameSizeFigureTrackers.clear();
	}

	protected void addSameSizeFigure(IFigure hostFigure, String type)
	{
		SameSizeFeedbackFigure sameSizeFigure = new SameSizeFeedbackFigure(type);
		getLayer(FormGraphicalRootEditPart.SELECTED_ELEMENT_FEEDBACK_LAYER).add(sameSizeFigure);
		sameSizeFigureTrackers.add(new FigureMovedTracker(sameSizeFigure, hostFigure, SameSizeFeedbackFigure.getLocator(type, hostFigure)));
	}

	protected void addSameSizeFeedback()
	{
		if (!Boolean.TRUE.equals(container.getViewer().getProperty(AlignmentfeedbackEditPolicy.PROPERTY_SAME_SIZE_FEEDBACK_VISIBLE)) ||
			getHost() instanceof TabFormGraphicalEditPart)
		{
			return;
		}

		Rectangle myBounds = getHostFigure().getBounds();
		boolean addedSameWidth = false;
		boolean addedSameHeight = false;

		List< ? extends GraphicalEditPart> children = container.getChildren();
		for (EditPart child : children)
		{
			if (child.getModel() instanceof Part || child == getHost() || !(child instanceof GraphicalEditPart) || child instanceof TabFormGraphicalEditPart)
			{
				continue;
			}

			IFigure childFigure = ((GraphicalEditPart)child).getFigure();
			Rectangle childBounds = childFigure.getBounds();
			if (myBounds.width >= 5 && myBounds.width == childBounds.width)
			{
				addSameSizeFigure(childFigure, SameSizeFeedbackFigure.SAME_WIDTH);
				addedSameWidth = true;
			}
			if (myBounds.height >= 5 && myBounds.height == childBounds.height)
			{
				addSameSizeFigure(childFigure, SameSizeFeedbackFigure.SAME_HEIGHT);
				addedSameHeight = true;
			}
		}

		if (addedSameWidth)
		{
			addSameSizeFigure(getHostFigure(), SameSizeFeedbackFigure.SAME_WIDTH);
		}
		if (addedSameHeight)
		{
			addSameSizeFigure(getHostFigure(), SameSizeFeedbackFigure.SAME_HEIGHT);
		}
	}

	protected void removeSelectedElementAlignmentFeedback()
	{
		if (alignmentSelectectElementFeedbackTracker != null)
		{
			getAlignmentSelectedElementFeedbackHelper().showElementAlignmentFeedback((ElementAlignmentItem[])null);
			alignmentSelectectElementFeedbackTracker.unhook();
			alignmentSelectectElementFeedbackTracker = null;
		}
	}

	protected void addSelectedElementAlignmentFeedback()
	{
		if (alignmentSelectectElementFeedbackTracker != null ||
			!Boolean.TRUE.equals(container.getViewer().getProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE)))
		{
			return;
		}

		alignmentSelectectElementFeedbackTracker = new FigureMovedTracker(null, getHostFigure(), null)
		{
			@Override
			public void ancestorMoved(IFigure ancestor)
			{
				List<EditPart> editParts = new ArrayList<EditPart>(1);
				editParts.add(getHost());
				SnapToElementAlignment snapToElementAlignment = new SnapToElementAlignment(container);
				snapToElementAlignment.setSnapThreshold(0);
				Rectangle figureBounds = getHostFigure().getBounds();
				ElementAlignmentItem[] elementAlignment = snapToElementAlignment.getElementAlignment(figureBounds, PositionConstants.NSEW, editParts, false);
				getAlignmentSelectedElementFeedbackHelper().showElementAlignmentFeedback(elementAlignment);
			}
		};
	}
}