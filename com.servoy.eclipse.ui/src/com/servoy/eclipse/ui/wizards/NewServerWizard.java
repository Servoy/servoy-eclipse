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

package com.servoy.eclipse.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewServerAction;
import com.servoy.j2db.persistence.ServerConfig;

/**
 * Wizard for creating a new database server.
 *
 * @author gerzse
 */
public class NewServerWizard extends Wizard implements INewWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewServerWizard";

	private ServerTypeSelectionPage typeSelectionPage;

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setWindowTitle("New Database Server");
	}

	@Override
	public boolean performFinish()
	{
		String selectedServerType = typeSelectionPage.getSelectedServerType();
		ServerConfig template = ServerConfig.TEMPLATES.get(selectedServerType).getTemplate();
		NewServerAction newServer = new NewServerAction(selectedServerType, template);
		newServer.run();
		return true;
	}

	@Override
	public void addPages()
	{
		typeSelectionPage = new ServerTypeSelectionPage("serverTypeSelection");
		this.addPage(typeSelectionPage);
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	private class ServerTypeSelectionPage extends WizardPage
	{
		private Combo serverTypesCombo;
		private String selectedServerType;

		protected ServerTypeSelectionPage(String pageName)
		{
			super(pageName);
			setTitle("Choose the Type of the new Database Server");
			setPageComplete(false);
		}

		public String getSelectedServerType()
		{
			return selectedServerType;
		}

		public void createControl(Composite parent)
		{
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayout(new GridLayout(2, false));

			Label sourceServerLabel = new Label(topLevel, SWT.NONE);
			sourceServerLabel.setText("Server type");

			serverTypesCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(serverTypesCombo);
			serverTypesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			List<String> availableTypes = new ArrayList<String>();
			availableTypes.add("Please choose a server type...");
			availableTypes.addAll(ServerConfig.TEMPLATES.keySet());
			serverTypesCombo.setItems(availableTypes.toArray(new String[availableTypes.size()]));
			serverTypesCombo.select(0);

			serverTypesCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					if (serverTypesCombo.getSelectionIndex() > 0)
					{
						selectedServerType = serverTypesCombo.getItem(serverTypesCombo.getSelectionIndex());
						setPageComplete(true);
					}
					else
					{
						setPageComplete(false);
					}
				}
			});

			setControl(topLevel);
		}
	}

}
