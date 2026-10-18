/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * SVY-21502: the manual review/edit view for a solution's pnpm dependency build-script allow/deny lists.
 * <p>
 * Opened from the context menu on the <code>ng_web_packages</code> Solution Explorer node. Shows the committed
 * <code>allowed</code> and <code>denied</code> lists side by side and lets the developer move a package between the
 * two lists or remove an entry (making it pending again). On OK the edited lists are written back into the given
 * {@link PnpmBuildApprovals}; the caller persists them. Pure UI.
 *
 * @author jcompagner
 * @since 2026.12
 */
public class PnpmBuildApprovalsDialog extends Dialog
{
	private final PnpmBuildApprovals approvals;
	private final java.util.List<String> allowed = new ArrayList<>();
	private final java.util.List<String> denied = new ArrayList<>();


	private org.eclipse.swt.widgets.List allowedList;
	private org.eclipse.swt.widgets.List deniedList;

	public PnpmBuildApprovalsDialog(Shell parentShell, PnpmBuildApprovals approvals)
	{
		super(parentShell);
		this.approvals = approvals;
		this.allowed.addAll(approvals.getAllowed());
		this.denied.addAll(approvals.getDenied());
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("pnpm dependency build-script approvals");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite)super.createDialogArea(parent);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		container.setLayout(layout);

		Label intro = new Label(container, SWT.WRAP);
		GridData introData = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
		introData.widthHint = 540;
		intro.setLayoutData(introData);
		intro.setText("Allowed versions (name@version, pnpm allowBuilds: true) run their install/build scripts on every " +
			"build. Denied versions (allowBuilds: false) do not and are not re-prompted. Removing an entry makes that exact " +
			"version pending again. Changes are saved to ng_web_packages/pnpm-build-approvals.json.");

		Label allowedHeader = new Label(container, SWT.NONE);
		allowedHeader.setText("Allowed (name@version)");
		new Label(container, SWT.NONE);
		Label deniedHeader = new Label(container, SWT.NONE);
		deniedHeader.setText("Denied (name@version)");

		allowedList = new org.eclipse.swt.widgets.List(container, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData allowedData = new GridData(SWT.FILL, SWT.FILL, true, true);
		allowedData.widthHint = 240;
		allowedData.heightHint = 220;
		allowedList.setLayoutData(allowedData);

		Composite buttons = new Composite(container, SWT.NONE);
		buttons.setLayout(new GridLayout(1, true));
		buttons.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		Button toDeny = new Button(buttons, SWT.PUSH);
		toDeny.setText("Deny >>");
		toDeny.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button toAllow = new Button(buttons, SWT.PUSH);
		toAllow.setText("<< Allow");
		toAllow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button removeAllowed = new Button(buttons, SWT.PUSH);
		removeAllowed.setText("Remove");
		removeAllowed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		deniedList = new org.eclipse.swt.widgets.List(container, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData deniedData = new GridData(SWT.FILL, SWT.FILL, true, true);
		deniedData.widthHint = 240;
		deniedData.heightHint = 220;
		deniedList.setLayoutData(deniedData);

		refreshLists();

		toDeny.addListener(SWT.Selection, e -> {
			for (String name : allowedList.getSelection())
			{
				allowed.remove(name);
				if (!denied.contains(name)) denied.add(name);
			}
			refreshLists();
		});
		toAllow.addListener(SWT.Selection, e -> {
			for (String name : deniedList.getSelection())
			{
				denied.remove(name);
				if (!allowed.contains(name)) allowed.add(name);
			}
			refreshLists();
		});
		removeAllowed.addListener(SWT.Selection, e -> {
			for (String name : allowedList.getSelection())
			{
				allowed.remove(name);
			}
			for (String name : deniedList.getSelection())
			{
				denied.remove(name);
			}
			refreshLists();
		});

		return container;
	}

	private void refreshLists()
	{
		allowedList.setItems(allowed.toArray(new String[0]));
		deniedList.setItems(denied.toArray(new String[0]));
	}

	@Override
	protected void okPressed()
	{
		// rebuild the approvals from the edited lists
		for (String name : new ArrayList<>(approvals.getAllowed()))
		{
			approvals.remove(name);
		}
		for (String name : new ArrayList<>(approvals.getDenied()))
		{
			approvals.remove(name);
		}
		for (String name : allowed)
		{
			approvals.allow(name);
		}
		for (String name : denied)
		{
			approvals.deny(name);
		}
		super.okPressed();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}
}
