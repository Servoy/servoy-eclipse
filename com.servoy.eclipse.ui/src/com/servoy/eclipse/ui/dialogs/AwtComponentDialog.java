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
package com.servoy.eclipse.ui.dialogs;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.util.EditorUtil;

/**
 * Show a dialog that holds an AWT component;
 * 
 * @author rob
 * 
 */
public class AwtComponentDialog extends Dialog
{
	private final Component component;
	private final String title;
	private Frame frame;
	private final String name;

	public AwtComponentDialog(Shell shell, java.awt.Component component, String title, String name)
	{
		super(shell);
		this.component = component;
		this.title = title;
		this.name = name;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText(title);

		// SWT.EMBEDDED flag has to be set
		Composite composite = new Composite(parent, SWT.EMBEDDED);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		applyDialogFont(composite);
		composite.setLayout(new FillLayout());

		frame = SWT_AWT.new_Frame(composite);
		frame.setLayout(new FlowLayout());
		frame.add(component);

		java.awt.Dimension dim = component.getPreferredSize();
		if (dim != null)
		{
			composite.setSize(dim.width, dim.height);
		}

		return composite;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		IDialogSettings settings = EditorUtil.getDialogSettings(name);
		if (settings.get("DIALOG_WIDTH") == null)
		{
			//getDialogArea().pack();
			Rectangle areaBounds = getDialogArea().getBounds();
			getButtonBar().pack();
			Rectangle buttonBounds = getButtonBar().getBounds();
			settings.put("DIALOG_WIDTH", Math.max(areaBounds.width, buttonBounds.width));
			settings.put("DIALOG_HEIGHT", areaBounds.height + buttonBounds.height + 40);
		}
		return settings;
	}

	@Override
	public int open()
	{
		int open = super.open();
		frame.dispose();
		return open;
	}
}
