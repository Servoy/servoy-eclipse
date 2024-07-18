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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Added IPostSelectionProvider to existing ScrollingGraphicalViewer. Needed for some selection listeners (for instance history view).
 *
 * @author rgansevles
 *
 */
public class ModifiedScrollingGraphicalViewer extends ScrollingGraphicalViewer implements IPostSelectionProvider
{
	private static final Point STATIC_POINT = new Point(); // reuse object

	protected List<ISelectionChangedListener> postSelectionListeners = new ArrayList<ISelectionChangedListener>(1);

	private final Listener menuDetectListener = new Listener()
	{
		// changed behaviour of right-click (show menu) in form designer, set selection to element under mouse.
		public void handleEvent(Event event)
		{
			org.eclipse.swt.graphics.Point swtPoint = getControl().toControl(event.x, event.y);
			EditPart editPart = findObjectAt(new Point(swtPoint.x, swtPoint.y));
			if (editPart.getSelected() == EditPart.SELECTED_NONE && editPart.isSelectable())
			{
				setSelection(editPart instanceof RootEditPart ? StructuredSelection.EMPTY : new StructuredSelection(editPart));
			}
		}
	};

	public void addPostSelectionChangedListener(ISelectionChangedListener listener)
	{
		postSelectionListeners.add(listener);
	}

	public void removePostSelectionChangedListener(ISelectionChangedListener listener)
	{
		postSelectionListeners.remove(listener);
	}

	/**
	 * Fires selection changed to the registered listeners at the time called.
	 */
	protected void firePostSelectionChanged()
	{
		if (postSelectionListeners.size() > 0)
		{
			Object listeners[] = postSelectionListeners.toArray();
			SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
			for (Object element : listeners)
			{
				((ISelectionChangedListener)element).selectionChanged(event);
			}
		}
	}

	@Override
	protected void fireSelectionChanged()
	{
		super.fireSelectionChanged();
		firePostSelectionChanged();
	}

	/**
	 * Override the default behavior of always selecting the top-most element. When an element is selected but is obscured by another element, return it, for
	 * example small element [selected] under large element will return the small one. when an element is selected and a smaller element is placed on top,
	 * return the smaller element.
	 */
	@Override
	public EditPart findObjectAtExcluding(Point pt, Collection exclude, final Conditional condition)
	{
		GraphicalEditPart selectedEditPart = null;
		List< ? extends EditPart> selectedEditParts = getSelectedEditParts();
		for (EditPart editPart : selectedEditParts)
		{
			if (editPart instanceof GraphicalEditPart)
			{
				// translate the point to take the scrollbar position into  account
				STATIC_POINT.setLocation(pt);
				((GraphicalEditPart)editPart).getFigure().translateToRelative(STATIC_POINT);

				if (((GraphicalEditPart)editPart).getFigure().findFigureAt(STATIC_POINT.x, STATIC_POINT.y) != null &&
					(exclude == null || !exclude.contains(editPart)) && (condition == null || condition.evaluate(editPart)))
				{
					if (selectedEditPart == null || getSquareSize(selectedEditPart) > getSquareSize((GraphicalEditPart)editPart))
					{
						selectedEditPart = (GraphicalEditPart)editPart;
					}
				}
			}
		}
		EditPart topEditPart = super.findObjectAtExcluding(pt, exclude, condition);
		if (selectedEditPart == null || selectedEditPart == topEditPart)
		{
			return topEditPart;
		}
		// 2 editparts, 1 selected and 1 on top, take the smallest one, the bigger one can be selected by clicking outside the smaller one.
		if (topEditPart.isSelectable() && topEditPart instanceof GraphicalEditPart &&
			getSquareSize((GraphicalEditPart)topEditPart) < getSquareSize(selectedEditPart))
		{
			return topEditPart;
		}
		return selectedEditPart;
	}

	public static int getSquareSize(GraphicalEditPart editPart)
	{
		Dimension size = editPart.getFigure().getSize();
		return size.height * size.width;
	}

	public void scrollTo(int x, int y)
	{
		FigureCanvas figureCanvas = getFigureCanvas();
		if (figureCanvas != null)
		{
			figureCanvas.scrollTo(x, y);
		}
	}

	@Override
	protected void hookControl()
	{
		super.hookControl();
		getControl().addListener(SWT.MenuDetect, menuDetectListener);
	}

	@Override
	protected void unhookControl()
	{
		getControl().removeListener(SWT.MenuDetect, menuDetectListener);
		super.unhookControl();
	}
}