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

import org.eclipse.gef.Disposable;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

/** 
 * Base class for actions that show a dropdown menu in form designer toolbar.
 * 
 * @author rgansevles
 */
public abstract class ViewerDropdownPropertyAction extends Action implements Disposable
{
	final protected GraphicalViewer diagramViewer;
	private Menu menu;

	public ViewerDropdownPropertyAction(GraphicalViewer diagramViewer, String actionId, String text, String tooltip, ImageDescriptor imageDescriptor)
	{
		super(text, IAction.AS_DROP_DOWN_MENU);
		this.diagramViewer = diagramViewer;
		setToolTipText(tooltip);
		setId(actionId);
		setActionDefinitionId(actionId);
		setImageDescriptor(imageDescriptor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void runWithEvent(Event event)
	{
		disposeMenu();
		menu = new Menu(diagramViewer.getControl().getShell());
		fillMenu(menu);
		// Determine where to put the dropdown list
		ToolItem item = (ToolItem)event.widget;
		Rectangle rect = item.getBounds();
		Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
		menu.setLocation(pt.x, pt.y + rect.height);
		menu.setVisible(true);
	}

	public void dispose()
	{
		disposeMenu();
	}

	public void disposeMenu()
	{
		if (menu != null && !menu.isDisposed())
		{
			menu.dispose();
		}
		menu = null;
	}

	abstract protected void fillMenu(Menu m);

	/**
	 * Adds an item to the dropdown list
	 * 
	 * @param item the item to add
	 */
	public void add(final IAction action)
	{
		MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
		menuItem.setText(action.getText());
		menuItem.setEnabled(action.isEnabled());
		menuItem.setSelection(action.isChecked());
		menuItem.setImage(com.servoy.eclipse.ui.Activator.getDefault().getImage(action.getImageDescriptor()));
		menuItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				action.run();
			}
		});
	}
}
