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

package com.servoy.eclipse.ui.util;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A ToolItem based on an IAction.
 * 
 * @author rgansevles
 *
 */
public class ActionToolItem extends ToolItem implements IPropertyChangeListener
{
	private IAction action;

	/**
	 * @param parent
	 * @param action
	 */
	public ActionToolItem(ToolBar parent, IAction action)
	{
		super(parent, getToolItemStyle(action));
		setAction(action);
	}

	/**
	 * @param action
	 * @return
	 */
	static int getToolItemStyle(IAction action)
	{
		switch (action.getStyle())
		{
			case IAction.AS_CHECK_BOX :
				return SWT.CHECK;
			case IAction.AS_RADIO_BUTTON :
				return SWT.RADIO;
			case IAction.AS_DROP_DOWN_MENU :
				return SWT.DROP_DOWN;
			default :
				return SWT.PUSH;
		}
	}

	/**
	 * @param action
	 */
	protected void setAction(IAction action)
	{
		this.action = action;

//		setText(action.getText());
		setToolTipText(action.getToolTipText());
		setImage(com.servoy.eclipse.ui.Activator.getDefault().getImage(action.getImageDescriptor()));
		addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ActionToolItem.this.action.run();
			}
		});
		setSelection(action.getStyle() == IAction.AS_CHECK_BOX ? action.isChecked() : action.isEnabled());
		action.addPropertyChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event)
	{
		if (isDisposed())
		{
			return; // strange, propertyChanage is still called, dispose has removed me as listener...
		}
		if (IAction.ENABLED.equals(event.getProperty()))
		{
			setEnabled(Boolean.TRUE.equals(event.getNewValue()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.ToolItem#checkSubclass()
	 */
	@Override
	protected void checkSubclass()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.ToolItem#dispose()
	 */
	@Override
	public void dispose()
	{
		action.removePropertyChangeListener(this);
		super.dispose();
	}
}
