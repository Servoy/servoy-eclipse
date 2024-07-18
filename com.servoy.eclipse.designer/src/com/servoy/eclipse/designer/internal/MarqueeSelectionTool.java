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
package com.servoy.eclipse.designer.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.editor.FormGraphicalEditPart;
import com.servoy.eclipse.designer.util.EditpartDistanceComparator;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * A Tool which selects multiple objects inside a rectangular area of a Graphical Viewer. If the SHIFT key is pressed at the beginning of the drag, the enclosed
 * items will be appended to the current selection. If the MOD1 key is pressed at the beginning of the drag, the enclosed items will have their selection state
 * inverted.
 * <P>
 * By default, only editparts whose figure's are on the primary layer will be considered within the enclosed rectangle.
 */
public class MarqueeSelectionTool extends AbstractTool
{

	/**
	 * The property to be used in {@link AbstractTool#setProperties(java.util.Map)} for {@link #setMarqueeBehavior(int)}.
	 */
	public static final Object PROPERTY_MARQUEE_BEHAVIOR = "marqueeBehavior";

	/**
	 * This behaviour selects nodes completely encompassed by the marquee rectangle. This is the default behaviour for this tool.
	 *
	 * @since 3.1
	 */
	public static final int BEHAVIOR_NODES_CONTAINED = new Integer(1).intValue();
	/**
	 * This behaviour selects connections that intersect the marquee rectangle.
	 *
	 * @since 3.1
	 */
	public static final int BEHAVIOR_CONNECTIONS_TOUCHED = new Integer(2).intValue();
	/**
	 * This behaviour selects nodes completely encompassed by the marquee rectangle, and all connections between those nodes.
	 *
	 * @since 3.1
	 */
	public static final int BEHAVIOR_NODES_AND_CONNECTIONS = new Integer(3).intValue();

	static final int DEFAULT_MODE = 0;
	static final int TOGGLE_MODE = 1;
	static final int APPEND_MODE = 2;

	private Figure marqueeRectangleFigure;
	private final Set allChildren = new HashSet();
	private Collection selectedEditParts;
	private Request targetRequest;
	private int marqueeBehavior = BEHAVIOR_NODES_CONTAINED;
	private int mode;
	private GraphicalEditPart startEditpart;

	private static final Request MARQUEE_REQUEST = new Request(RequestConstants.REQ_SELECTION);

	/**
	 * Creates a new MarqueeSelectionTool of default type {@link #BEHAVIOR_NODES_CONTAINED}.
	 */
	public MarqueeSelectionTool()
	{
		setDefaultCursor(Cursors.CROSS);
		setUnloadWhenFinished(false);
	}


	/**
	 * @param startEditpart the startEditpart to set
	 */
	public void setStartEditpart(GraphicalEditPart startEditpart)
	{
		this.startEditpart = startEditpart;
	}

	/**
	 * @return the startEditpart
	 */
	public GraphicalEditPart getStartEditpart()
	{
		return startEditpart;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#applyProperty(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void applyProperty(Object key, Object value)
	{
		if (PROPERTY_MARQUEE_BEHAVIOR.equals(key))
		{
			if (value instanceof Integer) setMarqueeBehavior(((Integer)value).intValue());
			return;
		}
		super.applyProperty(key, value);
	}

	private void calculateConnections(Collection newSelections, Collection deselections)
	{
		// determine the currently selected nodes minus the ones that are to be deselected
		Collection currentNodes = new HashSet();
		if (getSelectionMode() != DEFAULT_MODE)
		{ // everything is deselected in default mode
			Iterator iter = getCurrentViewer().getSelectedEditParts().iterator();
			while (iter.hasNext())
			{
				EditPart selected = (EditPart)iter.next();
				if (!(selected instanceof ConnectionEditPart) && !deselections.contains(selected)) currentNodes.add(selected);
			}
		}
		// add new connections to be selected to newSelections
		Collection connections = new ArrayList();
		for (Object newSelection : newSelections)
		{
			GraphicalEditPart node = (GraphicalEditPart)newSelection;
			for (Object element : node.getSourceConnections())
			{
				ConnectionEditPart sourceConn = (ConnectionEditPart)element;
				if (sourceConn.getSelected() == EditPart.SELECTED_NONE &&
					(newSelections.contains(sourceConn.getTarget()) || currentNodes.contains(sourceConn.getTarget()))) connections.add(sourceConn);
			}
			for (Object element : node.getTargetConnections())
			{
				ConnectionEditPart targetConn = (ConnectionEditPart)element;
				if (targetConn.getSelected() == EditPart.SELECTED_NONE &&
					(newSelections.contains(targetConn.getSource()) || currentNodes.contains(targetConn.getSource()))) connections.add(targetConn);
			}
		}
		newSelections.addAll(connections);
		// add currently selected connections that are to be deselected to deselections
		connections = new HashSet();
		for (Object deselection : deselections)
		{
			GraphicalEditPart node = (GraphicalEditPart)deselection;
			for (Object element : node.getSourceConnections())
			{
				ConnectionEditPart sourceConn = (ConnectionEditPart)element;
				if (sourceConn.getSelected() != EditPart.SELECTED_NONE) connections.add(sourceConn);
			}
			for (Object element : node.getTargetConnections())
			{
				ConnectionEditPart targetConn = (ConnectionEditPart)element;
				if (targetConn.getSelected() != EditPart.SELECTED_NONE) connections.add(targetConn);
			}
		}
		deselections.addAll(connections);
	}

	private void calculateNewSelection(Collection newSelections, Collection deselections)
	{
		boolean marqueeSelectOuter = new DesignerPreferences().getMarqueeSelectOuter();
		Rectangle marqueeRect = getMarqueeSelectionRectangle();
		for (Object element : getAllChildren())
		{
			GraphicalEditPart child = (GraphicalEditPart)element;
			IFigure figure = child.getFigure();
			if (!child.isSelectable() || child.getTargetEditPart(MARQUEE_REQUEST) != child
//				|| !isFigureVisible(figure) // may be selected but scrolled out of the viewable area
				|| !figure.isShowing()) continue;

			Rectangle r = figure.getBounds().getCopy();
			figure.translateToAbsolute(r);
			boolean included = false;
			if (child instanceof ConnectionEditPart && marqueeRect.intersects(r))
			{
				Rectangle relMarqueeRect = Rectangle.SINGLETON;
				figure.translateToRelative(relMarqueeRect.setBounds(marqueeRect));
				included = ((PolylineConnection)figure).getPoints().intersects(relMarqueeRect);
			}
			else if (!(child instanceof FormGraphicalEditPart)) // form should not be selected via marquee
			{
				included = marqueeSelectOuter ? marqueeRect.contains(r) : marqueeRect.intersects(r);
			}

			if (included)
			{
				if (child.getSelected() == EditPart.SELECTED_NONE || getSelectionMode() != TOGGLE_MODE) newSelections.add(child);
				else deselections.add(child);
			}
		}

		if (marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS) calculateConnections(newSelections, deselections);
	}

	private Request createTargetRequest()
	{
		return MARQUEE_REQUEST;
	}

	/**
	 * Erases feedback if necessary and puts the tool into the terminal state.
	 */
	@Override
	public void deactivate()
	{
		if (isInState(STATE_DRAG_IN_PROGRESS))
		{
			eraseMarqueeFeedback();
			eraseTargetFeedback();
		}
		super.deactivate();
		allChildren.clear();
		setState(STATE_TERMINAL);
	}

	private void eraseMarqueeFeedback()
	{
		if (marqueeRectangleFigure != null)
		{
			removeFeedback(marqueeRectangleFigure);
			marqueeRectangleFigure = null;
		}
	}

	private void eraseTargetFeedback()
	{
		if (selectedEditParts == null) return;
		Iterator oldEditParts = selectedEditParts.iterator();
		while (oldEditParts.hasNext())
		{
			EditPart editPart = (EditPart)oldEditParts.next();
			editPart.eraseTargetFeedback(getTargetRequest());
		}
	}

	private Set getAllChildren()
	{
		if (allChildren.isEmpty()) getAllChildren(getCurrentViewer().getRootEditPart(), allChildren);
		return allChildren;
	}

	private void getAllChildren(EditPart editPart, Set allChildren)
	{
		List children = editPart.getChildren();
		for (Object child2 : children)
		{
			GraphicalEditPart child = (GraphicalEditPart)child2;
			if (marqueeBehavior == BEHAVIOR_NODES_CONTAINED || marqueeBehavior == BEHAVIOR_NODES_AND_CONNECTIONS) allChildren.add(child);
			if (marqueeBehavior == BEHAVIOR_CONNECTIONS_TOUCHED)
			{
				allChildren.addAll(child.getSourceConnections());
				allChildren.addAll(child.getTargetConnections());
			}
			getAllChildren(child, allChildren);
		}
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#getCommandName()
	 */
	@Override
	protected String getCommandName()
	{
		return REQ_SELECTION;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#getDebugName()
	 */
	@Override
	protected String getDebugName()
	{
		return "Marquee Tool: " + marqueeBehavior;
	}

	protected IFigure getMarqueeFeedbackFigure()
	{
		if (marqueeRectangleFigure == null)
		{
			marqueeRectangleFigure = new MarqueeRectangleFigure();
			addFeedback(marqueeRectangleFigure);
		}
		return marqueeRectangleFigure;
	}

	private Rectangle getMarqueeSelectionRectangle()
	{
		return new Rectangle(getStartLocation(), getLocation());
	}

	private int getSelectionMode()
	{
		return mode;
	}

	private Request getTargetRequest()
	{
		if (targetRequest == null) targetRequest = createTargetRequest();
		return targetRequest;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#handleButtonDown(int)
	 */
	@Override
	protected boolean handleButtonDown(int button)
	{
		if (!isGraphicalViewer()) return true;
		if (button != 1)
		{
			setState(STATE_INVALID);
			handleInvalidInput();
		}
		if (stateTransition(STATE_INITIAL, STATE_DRAG_IN_PROGRESS))
		{
			if (getCurrentInput().isModKeyDown(SWT.MOD1)) setSelectionMode(TOGGLE_MODE);
			// else if (getCurrentInput().isShiftKeyDown()) setSelectionMode(APPEND_MODE); // shift-down was used to trigger marquee select in stead of editpart dragging
			else setSelectionMode(DEFAULT_MODE);
		}
		return true;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#handleButtonUp(int)
	 */
	@Override
	protected boolean handleButtonUp(int button)
	{
		if (stateTransition(STATE_DRAG_IN_PROGRESS, STATE_TERMINAL))
		{
			eraseTargetFeedback();
			eraseMarqueeFeedback();
			performMarqueeSelect();
		}
		handleFinished();
		return true;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#handleDragInProgress()
	 */
	@Override
	protected boolean handleDragInProgress()
	{
		if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS))
		{
			showMarqueeFeedback();
			eraseTargetFeedback();
			calculateNewSelection(selectedEditParts = new ArrayList(), new ArrayList());
			showTargetFeedback();
		}
		return true;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#handleFocusLost()
	 */
	@Override
	protected boolean handleFocusLost()
	{
		if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS))
		{
			handleFinished();
			return true;
		}
		return false;
	}

	/**
	 * This method is called when mouse or keyboard input is invalid and erases the feedback.
	 *
	 * @return <code>true</code>
	 */
	@Override
	protected boolean handleInvalidInput()
	{
		eraseTargetFeedback();
		eraseMarqueeFeedback();
		return true;
	}

	/**
	 * Handles high-level processing of a key down event. KeyEvents are forwarded to the current viewer's {@link KeyHandler}, via
	 * {@link KeyHandler#keyPressed(KeyEvent)}.
	 *
	 * @see AbstractTool#handleKeyDown(KeyEvent)
	 */
	@Override
	protected boolean handleKeyDown(KeyEvent e)
	{
		if (super.handleKeyDown(e)) return true;
		if (getCurrentViewer().getKeyHandler() != null) return getCurrentViewer().getKeyHandler().keyPressed(e);
		return false;
	}

	private boolean isFigureVisible(IFigure fig)
	{
		Rectangle figBounds = fig.getBounds().getCopy();
		IFigure walker = fig.getParent();
		while (!figBounds.isEmpty() && walker != null)
		{
			walker.translateToParent(figBounds);
			figBounds.intersect(walker.getBounds());
			walker = walker.getParent();
		}
		return !figBounds.isEmpty();
	}

	private boolean isGraphicalViewer()
	{
		return getCurrentViewer() instanceof GraphicalViewer;
	}

	/**
	 * MarqueeSelectionTool is only interested in GraphicalViewers, not TreeViewers.
	 *
	 * @see org.eclipse.gef.tools.AbstractTool#isViewerImportant(org.eclipse.gef.EditPartViewer)
	 */
	@Override
	protected boolean isViewerImportant(EditPartViewer viewer)
	{
		return viewer instanceof GraphicalViewer;
	}

	private void performMarqueeSelect()
	{
		EditPartViewer viewer = getCurrentViewer();
		// sort the selected edit parts, the ones closest to the current location to the end so that the
		// order goes from start marquee select to the end
		Collection<EditPart> newSelections = new TreeSet<EditPart>(new EditpartDistanceComparator(getLocation(), false));
		Collection<EditPart> deselections = new HashSet<EditPart>();
		calculateNewSelection(newSelections, deselections);
		if (getSelectionMode() != DEFAULT_MODE)
		{
			newSelections.addAll(viewer.getSelectedEditParts());
			newSelections.removeAll(deselections);
		}
		else if (newSelections.isEmpty() && getCurrentInput().isShiftKeyDown() && getStartEditpart() != null &&
			getStartLocation().getDistance(getCurrentInput().getMouseLocation()) < 5)
		{
			// emulate shift-click of regular editpart drag tracker.

			// shift-select: select all edit parts inbetween the current selection and the source edit part
			List< ? extends EditPart> editParts = viewer.getContents().getChildren();
			// get the bounding box of all currently selected edit parts
			Rectangle selectedRectangle = null;
			for (EditPart editpart : editParts)
			{
				if (editpart == getStartEditpart() || (editpart.getSelected() != EditPart.SELECTED_NONE && editpart instanceof GraphicalEditPart))
				{
					if (selectedRectangle == null)
					{
						// make a copy, the union method modifies the rectangle
						selectedRectangle = ((GraphicalEditPart)editpart).getFigure().getBounds().getCopy();
					}
					else
					{
						selectedRectangle = selectedRectangle.union(((GraphicalEditPart)editpart).getFigure().getBounds());
					}
				}
			}

			if (selectedRectangle != null)
			{
				// select all edit parts that touch this box unioned with the current edit part
				selectedRectangle = selectedRectangle.union(getStartEditpart().getFigure().getBounds());
				for (EditPart editpart : editParts)
				{
					if (editpart instanceof GraphicalEditPart && ((GraphicalEditPart)editpart).isSelectable() &&
						selectedRectangle.intersects(((GraphicalEditPart)editpart).getFigure().getBounds()))
					{
						newSelections.add(editpart);
					}
				}
			}
		}
		viewer.setSelection(new StructuredSelection(newSelections.toArray()));
	}

	/**
	 * Sets the type of parts that this tool will select. This method should only be invoked once: when the tool is being initialized.
	 *
	 * @param type {@link #BEHAVIOR_CONNECTIONS_TOUCHED} or {@link #BEHAVIOR_NODES_CONTAINED} or {@link #BEHAVIOR_NODES_AND_CONNECTIONS}
	 * @since 3.1
	 */
	public void setMarqueeBehavior(int type)
	{
		if (type != BEHAVIOR_CONNECTIONS_TOUCHED && type != BEHAVIOR_NODES_CONTAINED && type != BEHAVIOR_NODES_AND_CONNECTIONS)
			throw new IllegalArgumentException(
				"Invalid marquee behaviour specified.");
		marqueeBehavior = type;
	}

	private void setSelectionMode(int mode)
	{
		this.mode = mode;
	}

	/**
	 * @see org.eclipse.gef.Tool#setViewer(org.eclipse.gef.EditPartViewer)
	 */
	@Override
	public void setViewer(EditPartViewer viewer)
	{
		if (viewer == getCurrentViewer()) return;
		super.setViewer(viewer);
		if (viewer instanceof GraphicalViewer) setDefaultCursor(Cursors.CROSS);
		else setDefaultCursor(Cursors.NO);
	}

	private void showMarqueeFeedback()
	{
		Rectangle rect = getMarqueeSelectionRectangle().getCopy();
		getMarqueeFeedbackFigure().translateToRelative(rect);
		getMarqueeFeedbackFigure().setBounds(rect);
	}

	private void showTargetFeedback()
	{
		for (Object selectedEditPart : selectedEditParts)
		{
			EditPart editPart = (EditPart)selectedEditPart;
			editPart.showTargetFeedback(getTargetRequest());
		}
	}

	class MarqueeRectangleFigure extends Figure
	{

		private static final int DELAY = 110; //animation delay in millisecond
		private int offset = 0;
		private boolean schedulePaint = true;

		/**
		 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
		 */
		@Override
		protected void paintFigure(Graphics graphics)
		{
			Rectangle bounds = getBounds().getCopy();
			graphics.translate(getLocation());

			graphics.setForegroundColor(ColorConstants.black);
			graphics.setBackgroundColor(ColorConstants.white);

			graphics.setLineStyle(Graphics.LINE_DOT);

			int[] points = new int[6];

			points[0] = 0 + offset;
			points[1] = 0;
			points[2] = bounds.width - 1;
			points[3] = 0;
			points[4] = bounds.width - 1;
			points[5] = bounds.height - 1;

			graphics.drawPolyline(points);

			points[0] = 0;
			points[1] = 0 + offset;
			points[2] = 0;
			points[3] = bounds.height - 1;
			points[4] = bounds.width - 1;
			points[5] = bounds.height - 1;

			graphics.drawPolyline(points);

			// background is not drawn, so we draw polyline again shifted 3 ppixels and reversed bg/fg
			// Note: on mac background.foreground seem to be switched around, marquee on white bg was invisible

			graphics.setForegroundColor(ColorConstants.white);
			graphics.setBackgroundColor(ColorConstants.black);

			points[0] = 0 + offset + 3;
			points[1] = 0;
			points[2] = bounds.width - 1;
			points[3] = 0;
			points[4] = bounds.width - 1;
			points[5] = bounds.height - 1;

			graphics.drawPolyline(points);

			points[0] = 0;
			points[1] = 0 + offset + 3;
			points[2] = 0;
			points[3] = bounds.height - 1;
			points[4] = bounds.width - 1;
			points[5] = bounds.height - 1;

			graphics.drawPolyline(points);

			graphics.translate(getLocation().getNegated());

			if (schedulePaint)
			{
				Display.getCurrent().timerExec(DELAY, new Runnable()
				{
					public void run()
					{
						offset++;
						if (offset > 5) offset = 0;

						schedulePaint = true;
						repaint();
					}
				});
			}

			schedulePaint = false;
		}
	}
}
