/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * @author Diana
 *
 */
public class ModuleListValueListSelectionDialog extends ModuleListSelectionDialog
{

	private Text valueListText;

	private String valueListName;

	private final String message;

	private IStatus status;

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * @param shell
	 * @param title
	 */
	public ModuleListValueListSelectionDialog(Shell shell, String dialogTitle,
		String dialogMessage)
	{
		super(shell, dialogTitle);
		this.message = dialogMessage;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createExtendedContentArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createExtendedContentArea(Composite parent)
	{
		if (message != null)
		{
			Label label = new Label(parent, SWT.NORMAL);
			label.setText(message);
		}

		GridData dataFirstName = new GridData();
		dataFirstName.grabExcessHorizontalSpace = true;
		dataFirstName.horizontalAlignment = GridData.FILL;

		valueListText = new Text(parent, getInputTextStyle());
		valueListText.setLayoutData(dataFirstName);
		valueListText.addListener(SWT.Modify, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				handleSelected(getSelectedItems());
			}
		});
		valueListText.forceFocus();
		return parent;
	}

	@Override
	protected void okPressed()
	{
		valueListName = valueListText.getText();
		super.okPressed();
	}

	/**
	 * @return the valueListName
	 */
	public String getValueListName()
	{
		return valueListName;
	}

	/*
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#updateStatus(org.eclipse.core. runtime.IStatus)
	 */
	@Override
	protected void updateStatus(IStatus status)
	{
		this.status = status;
		if (!isValueListNameSet())
		{
			this.status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.ERROR, EMPTY_STRING, null);
		}
		super.updateStatus(this.status);
	}

	private boolean isValueListNameSet()
	{
		String valueList = valueListText.getText();
		return valueList != null && valueList.trim().length() > 0;
	}

	private int getInputTextStyle()
	{
		return SWT.SINGLE | SWT.BORDER;
	}

}
