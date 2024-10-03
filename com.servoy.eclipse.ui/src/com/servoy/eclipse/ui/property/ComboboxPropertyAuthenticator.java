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
package com.servoy.eclipse.ui.property;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.wizards.NewOAuthConfigWizard;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;

public class ComboboxPropertyAuthenticator<T> extends ComboboxPropertyController<T>
{
	public static final String CLOUD_BASE_URL = System.getProperty("servoy.cloud_base.url", "https://admin.servoy-cloud.eu");

	public ComboboxPropertyAuthenticator(Object id, String displayName, IComboboxPropertyModel<T> model, String unresolved)
	{
		this(id, displayName, model, unresolved, null);
	}

	public ComboboxPropertyAuthenticator(Object id, String displayName, IComboboxPropertyModel<T> model, String unresolved, IValueEditor valueEditor)
	{
		super(id, displayName, model, unresolved, valueEditor);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		IComboboxPropertyModel<T> model = this.getModel();
		ModifiedComboBoxCellEditor editor = new ModifiedComboBoxCellEditor(parent, EMPTY_STRING_ARRAY, SWT.READ_ONLY, model.getDefaultValueIndex() == 0)
		{
			private Button openButton;

			@Override
			public void activate()
			{
				// set the items at activation, values may have changed
				Object value = doGetValue();
				setItems(model.getDisplayValues());
				doSetValue(value);
				AUTHENTICATOR_TYPE servoyCloud = AUTHENTICATOR_TYPE.SERVOY_CLOUD;
				openButton.setEnabled(value.equals(servoyCloud.getValue()) || value.equals(AUTHENTICATOR_TYPE.OAUTH.getValue()));

				super.activate();
			}

			@Override
			public String getErrorMessage()
			{
				String warningMessage = getWarningMessage();
				if (warningMessage == null || warningMessage.length() == 0)
				{
					return super.getErrorMessage();
				}
				return warningMessage;
			}

			@Override
			protected Control createControl(Composite parent)
			{
				Composite composite = new Composite(parent, SWT.NONE);
				CCombo combo = (CCombo)super.createControl(composite);
				openButton = new Button(composite, SWT.FLAT);

				openButton.setText("...");
				openButton.setEnabled(false);
				String tooltipText = ((Integer)doGetValue()).intValue() == AUTHENTICATOR_TYPE.OAUTH.getValue()
					? "Configure Stateless Login with an OAuth provider"
					: "Go to Servoy Cloud";
				openButton.setToolTipText(tooltipText);

				GroupLayout groupLayout = new GroupLayout(composite);
				SequentialGroup sequentialGroup = groupLayout.createSequentialGroup();
				sequentialGroup.add(combo, GroupLayout.PREFERRED_SIZE, 135, Integer.MAX_VALUE);
				sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(openButton);
				groupLayout.setHorizontalGroup(sequentialGroup);

				ParallelGroup parallelGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false);
				parallelGroup.add(openButton, 0, 0, Integer.MAX_VALUE);
				parallelGroup.add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
				groupLayout.setVerticalGroup(parallelGroup);

				composite.setLayout(groupLayout);

				openButton.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseDown(MouseEvent e)
					{
						Integer value = (Integer)doGetValue();
						if (value.intValue() == AUTHENTICATOR_TYPE.SERVOY_CLOUD.getValue())
						{
							redirectToTheCloud();
						}
						else if (value.intValue() == AUTHENTICATOR_TYPE.OAUTH.getValue())
						{
							Display.getDefault().asyncExec(() -> {
								OpenWizardAction action = new OpenWizardAction(NewOAuthConfigWizard.class, null,
									"Configure Stateless Login with an OAuth provider");
								action.run();
							});
						}
					}

					private void redirectToTheCloud()
					{
						String solutionUUID = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getUUID().toString();
						String loginToken = ServoyLoginDialog.getLoginToken();
						if (loginToken == null) loginToken = new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).doLogin();
						if (loginToken == null)
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									MessageDialog.openInformation(
										Display.getDefault().getActiveShell(),
										"Login Required",
										"You need to log in if you want to be redirected to Servoy Cloud.");
								}
							});
						}

						if (loginToken != null && solutionUUID != null)
						{
							String url = CLOUD_BASE_URL + "/solution/svyCloud/index.html?loginToken=" + loginToken +
								"&applicationUUID=" + solutionUUID +
								"&navigateTo=setupCloudSecurity";
							try
							{
								PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
							}
							catch (PartInitException | MalformedURLException e1)
							{
								ServoyLog.logError(e1);
							}
						}
					}
				});


				combo.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent event)
					{
						// the selection is already updated at this point using the SelectionAdapter created in super.createControl()
						String selection = ((CCombo)event.getSource()).getItems()[((CCombo)event.getSource()).getSelectionIndex()];
						openButton.setEnabled("SERVOY_CLOUD".equals(selection) || "OAUTH".equals(selection));
						if ("SERVOY_CLOUD".equals(selection))
						{
							openButton.setToolTipText("Go to Servoy Cloud");
						}
						else if ("OAUTH".equals(selection))
						{
							openButton.setToolTipText("Configure Stateless Login with an OAuth provider");
						}
						fireApplyEditorValue();
					}
				});
				return composite;
			}
		};
		return editor;
	}
}