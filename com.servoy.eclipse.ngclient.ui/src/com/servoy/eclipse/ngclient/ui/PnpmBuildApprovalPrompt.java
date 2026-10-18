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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * SVY-21502: the build-time allow/deny prompt for pending pnpm dependency build scripts.
 * <p>
 * Shown (only on a Developer build, when a real UI is present) when the upfront discovery query finds packages that
 * have build scripts but are in neither the committed allowed nor denied list. The developer allows or denies each
 * pending package; the choices are written into the supplied {@link PnpmBuildApprovals}. This is pure UI and does not
 * itself read or write the committed file - the caller persists the approvals afterwards.
 *
 * @author jcompagner
 * @since 2026.12
 */
public class PnpmBuildApprovalPrompt extends Dialog
{
	private final Set<String> pending;
	// packageName -> true=allow / false=deny; defaults to ALLOW for each pending package: the developer reviews the
	// listed versions and typically approves them, so Allow is pre-selected - but the list must still be read and
	// confirmed (OK writes the choices). Deny is always one click away per entry.
	private final Map<String, Boolean> choices = new LinkedHashMap<>();

	private PnpmBuildApprovalPrompt(Shell parentShell, Set<String> pending)
	{
		super(parentShell);
		this.pending = pending;
		for (String name : pending)
		{
			choices.put(name, Boolean.TRUE);
		}
	}

	/**
	 * Opens the prompt and, on OK, applies the developer's allow/deny choices to the given approvals.
	 *
	 * @param parentShell the parent shell (may be <code>null</code>)
	 * @param pending the packages that need a decision (neither allowed nor denied yet)
	 * @param approvals the approvals to update with the choices
	 * @return <code>true</code> if the approvals were changed (OK pressed with at least one decision), else
	 *         <code>false</code> (cancelled - nothing persisted)
	 */
	public static boolean promptForPending(Shell parentShell, Set<String> pending, PnpmBuildApprovals approvals)
	{
		if (pending == null || pending.isEmpty()) return false;
		PnpmBuildApprovalPrompt dialog = new PnpmBuildApprovalPrompt(parentShell, pending);
		// Dialog.open() returns Window.OK (0) on confirm; IDialogConstants.OK_ID is also 0 and is importable here
		// (the org.eclipse.jface.window.Window type itself is access-restricted in this bundle's dependencies).
		if (dialog.open() != IDialogConstants.OK_ID) return false;
		for (Map.Entry<String, Boolean> entry : dialog.choices.entrySet())
		{
			if (Boolean.FALSE.equals(entry.getValue())) approvals.deny(entry.getKey());
			else approvals.allow(entry.getKey());
		}
		return true;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Approve pnpm dependency build scripts");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite)super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		container.setLayout(layout);

		Label heading = new Label(container, SWT.WRAP);
		GridData headingData = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		headingData.widthHint = 560;
		heading.setLayoutData(headingData);
		heading.setFont(JFaceResources.getBannerFont());
		heading.setText("Approve dependency build scripts");

		Label intro = new Label(container, SWT.WRAP);
		GridData introData = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		introData.widthHint = 560;
		intro.setLayoutData(introData);
		intro.setText(
			"The dependency versions below ship an install/build script that pnpm will not run until you approve it. " +
				"This is a supply-chain safeguard: a package only runs code on your machine during install if you have " +
				"explicitly trusted that exact version, so a compromised version published later cannot silently execute a " +
				"script on the strength of a version you trusted before.\n\n" +
				"Approval is version-scoped (name@version): approving esbuild@0.28.1 does NOT cover esbuild@0.28.2 - a new " +
				"version is always asked again. Allowed versions then run their build scripts unattended on every build, " +
				"including the headless WAR/CI export; denied versions are never run and are not asked again (the build still " +
				"proceeds - if a denied package really needed its native step, that is your call).\n\n" +
				"Your choices are stored in ng_web_packages/pnpm-build-approvals.json. Together with the generated " +
				"ng_web_packages/pnpm-lock.yaml and ng_web_packages/package.json, this file must be committed to git: the WAR " +
				"exporter does a frozen install from exactly those committed files, so a build on a server/CI machine installs " +
				"the same pinned versions and runs the same approved scripts. Review each version, then press Save decisions.");

		for (String name : pending)
		{
			Label nameLabel = new Label(container, SWT.NONE);
			nameLabel.setText(name);
			nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			Composite radios = new Composite(container, SWT.NONE);
			radios.setLayout(new GridLayout(2, false));
			Button allow = new Button(radios, SWT.RADIO);
			allow.setText("Allow");
			Button deny = new Button(radios, SWT.RADIO);
			deny.setText("Deny");
			// Allow is the default: the developer reviews the listed versions and usually approves them. Deny stays
			// one click away per entry, and OK still has to be pressed to confirm.
			allow.setSelection(true);
			allow.addListener(SWT.Selection, e -> {
				if (allow.getSelection()) choices.put(name, Boolean.TRUE);
			});
			deny.addListener(SWT.Selection, e -> {
				if (deny.getSelection()) choices.put(name, Boolean.FALSE);
			});
		}
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, "Save decisions", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}
}
