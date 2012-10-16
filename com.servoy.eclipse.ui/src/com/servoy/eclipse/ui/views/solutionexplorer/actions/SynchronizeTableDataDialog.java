/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SynchronizeTableDataDialog extends Dialog
{
	public static final int IMPORT_TO_DB = 99;
	public static final int SAVE_TO_WS = 100;

	protected SynchronizeTableDataDialog(Shell parentShell)
	{
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);
		shell.setText("Choose update action");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite area = (Composite)super.createDialogArea(parent);
		area.setLayout(new FillLayout(SWT.HORIZONTAL));
		Composite dialogComposite = new Composite(area, SWT.NONE);
		FillLayout fl_dialogComposite = new FillLayout(SWT.HORIZONTAL);
		fl_dialogComposite.spacing = 5;
		fl_dialogComposite.marginWidth = 10;
		fl_dialogComposite.marginHeight = 10;
		dialogComposite.setLayout(fl_dialogComposite);

		Composite composite_1 = new Composite(dialogComposite, SWT.BORDER);
		FillLayout fl_composite_1 = new FillLayout(SWT.HORIZONTAL);
		fl_composite_1.spacing = 5;
		fl_composite_1.marginWidth = 10;
		fl_composite_1.marginHeight = 10;
		composite_1.setLayout(fl_composite_1);

		Label wslabel = new Label(composite_1, SWT.CENTER);
		wslabel.setText("Workspace\nfile");

		Composite composite = new Composite(dialogComposite, SWT.NONE);
		FillLayout fl_composite = new FillLayout(SWT.VERTICAL);
		fl_composite.spacing = 20;
		fl_composite.marginWidth = 10;
		fl_composite.marginHeight = 10;
		composite.setLayout(fl_composite);

		Button import2db = new Button(composite, SWT.NONE);
		import2db.setToolTipText("load workspace data in database table");
		import2db.setText("-->");

		Button save2ws = new Button(composite, SWT.NONE);
		save2ws.setToolTipText("Save data from database table in workspace");
		save2ws.setText("<--");
		save2ws.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setReturnCode(SAVE_TO_WS);
				close();
			}
		});
		import2db.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setReturnCode(IMPORT_TO_DB);
				close();
			}
		});

		Composite composite_2 = new Composite(dialogComposite, SWT.BORDER);
		FillLayout fl_composite_2 = new FillLayout(SWT.HORIZONTAL);
		fl_composite_2.marginWidth = 10;
		fl_composite_2.marginHeight = 10;
		composite_2.setLayout(fl_composite_2);

		Label tablelabel = new Label(composite_2, SWT.CENTER);
		tablelabel.setText("Database\ntable");

		return dialogComposite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		// just a cancel button
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
}