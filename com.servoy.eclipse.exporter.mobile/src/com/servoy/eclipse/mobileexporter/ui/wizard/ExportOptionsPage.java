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

package com.servoy.eclipse.mobileexporter.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.mobileexporter.export.MobileExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.labelproviders.SupportNameLabelProvider;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author lvostinar
 *
 */
public class ExportOptionsPage extends WizardPage
{
	public static String SERVER_URL_KEY = "serverURL";

	private Text serverURL;
	private TreeSelectViewer solutionSelectViewer;
	private final WizardPage nextPage;
	private final MobileExporter mobileExporter;

	public ExportOptionsPage(String pageName, WizardPage nextPage, MobileExporter mobileExporter)
	{
		super(pageName);
		this.nextPage = nextPage;
		this.mobileExporter = mobileExporter;
		setTitle("Export Options");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label serverURLLabel = new Label(container, SWT.NONE);
		serverURLLabel.setText("Application Server URL");

		serverURL = new Text(container, SWT.BORDER);
		serverURL.setToolTipText("This is the URL of Servoy Application Server used by mobile client to synchronize data");

		Label solutionLabel = new Label(container, SWT.NONE);
		solutionLabel.setText("Solution");

		solutionSelectViewer = new TreeSelectViewer(container, SWT.None);
		solutionSelectViewer.setButtonText("Browse");
		solutionSelectViewer.setTitleText("Select solution");
		solutionSelectViewer.setName("warExportDialog");
		solutionSelectViewer.setContentProvider(FlatTreeContentProvider.INSTANCE);
		solutionSelectViewer.setLabelProvider(SupportNameLabelProvider.INSTANCE_DEFAULT_NONE);

		try
		{
			solutionSelectViewer.setInput(ServoyModel.getDeveloperRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS));
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		if (ServoyModelFinder.getServoyModel().getActiveProject() != null)
		{
			SolutionMetaData activeSolution = ServoyModelFinder.getServoyModel().getActiveProject().getSolutionMetaData();
			if (activeSolution != null)
			{
				solutionSelectViewer.setSelection(new StructuredSelection(activeSolution));
			}
		}
		Control solutionSelectControl = solutionSelectViewer.getControl();

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(solutionLabel).add(serverURLLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(solutionSelectControl, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE).add(serverURL,
					GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE)).addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(solutionSelectControl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(solutionLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(serverURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(serverURLLabel)).add(10)));

		container.setLayout(groupLayout);

		String defaultServerURL = getDialogSettings().get(SERVER_URL_KEY);
		if (defaultServerURL == null)
		{
			defaultServerURL = "http://127.0.0.1:8080";
		}
		serverURL.setText(defaultServerURL);

		serverURL.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				setErrorMessage(null);
				ExportOptionsPage.this.getContainer().updateMessage();
				ExportOptionsPage.this.getContainer().updateButtons();
			}
		});
	}

	private String getServerURL()
	{
		return serverURL.getText();
	}

	private String getSolution()
	{
		IStructuredSelection selection = (IStructuredSelection)solutionSelectViewer.getSelection();
		return selection.isEmpty() ? null : ((RootObjectMetaData)selection.getFirstElement()).getName();
	}

	@Override
	public String getErrorMessage()
	{
		if (getSolution() == null || "".equals(getSolution()))
		{
			return "No solution specified";
		}
		if (getServerURL() == null || "".equals(getServerURL()))
		{
			return "No server URL specified";
		}
		return super.getErrorMessage();
	}

	@Override
	public IWizardPage getNextPage()
	{
		mobileExporter.setSolutionName(getSolution());
		mobileExporter.setServerURL(getServerURL());
		return nextPage;
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return getErrorMessage() == null;
	}

}
