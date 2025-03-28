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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
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
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.eclipse.ui.editors.AddMethodButtonsComposite;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.wizards.NewModuleWizard;
import com.servoy.eclipse.ui.wizards.NewOAuthConfigWizard;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyJSONObject;

public class ComboboxPropertyAuthenticator<T> extends ComboboxPropertyController<T>
{
	/**
	 *
	 */
	private static final String OAUTH_CONFIG_METHOD_PROPERTY = "getOAuthConfig";
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
				openButton.setEnabled(value.equals(servoyCloud.getValue()) || value.equals(AUTHENTICATOR_TYPE.OAUTH.getValue()) ||
					value.equals(AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR.getValue()));

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
						int value = ((Integer)doGetValue()).intValue();
						if (value == AUTHENTICATOR_TYPE.SERVOY_CLOUD.getValue())
						{
							redirectToTheCloud();
						}
						else if (value == AUTHENTICATOR_TYPE.OAUTH.getValue())
						{
							Display.getDefault().asyncExec(() -> {
								OpenWizardAction action = new OpenWizardAction(NewOAuthConfigWizard.class, null,
									"Configure Stateless Login with an OAuth provider");
								action.run();
							});
						}
						else if (value == AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR.getValue())
						{
							Display.getDefault().asyncExec(() -> {
								selectGetOAuthConfigMethod();
							});
						}
					}

					public void selectGetOAuthConfigMethod()
					{
						try
						{
							ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
							Solution mainSolution = activeProject.getSolution();
							Solution authenticatorModule = findOrCreateAuthenticatorModule(mainSolution);
							if (authenticatorModule == null)
							{
								return;
							}
							PersistContext persistContext = PersistContext.create(authenticatorModule);
							JSONObject properties = new ServoyJSONObject(mainSolution.getCustomProperties(), true);
							MethodWithArguments m = new MethodWithArguments(properties.optInt(OAUTH_CONFIG_METHOD_PROPERTY, -1), null);

							MethodDialog dialog = new MethodDialog(Display.getDefault().getActiveShell(),
								new MethodLabelProvider(persistContext, false, true, true),
								new MethodDialog.MethodTreeContentProvider(
									persistContext),
								new StructuredSelection(new Object[] { m.methodId == -1 ? MethodWithArguments.METHOD_NONE : m }),
								new MethodListOptions(false, false, false, true, false, null), SWT.NONE, "Select Method", null);
							dialog.setOptionsAreaFactory(new IControlFactory()
							{
								public Control createControl(Composite comp)
								{
									final AddMethodButtonsComposite buttons = new AddMethodButtonsComposite(comp, SWT.NONE);
									buttons.setContext(persistContext, OAUTH_CONFIG_METHOD_PROPERTY);
									buttons.setDialog(dialog);
									buttons.searchSelectedScope((IStructuredSelection)dialog.getTreeViewer().getViewer().getSelection());
									dialog.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener()
									{
										public void selectionChanged(SelectionChangedEvent event)
										{
											buttons.searchSelectedScope((IStructuredSelection)event.getSelection());
										}
									});
									return buttons;
								}

							});
							dialog.open();

							if (dialog.getReturnCode() == Window.CANCEL)
							{
								return;
							}

							MethodWithArguments selected = (MethodWithArguments)((StructuredSelection)dialog.getSelection()).getFirstElement();
							properties.put(OAUTH_CONFIG_METHOD_PROPERTY, selected.methodId);
							activeProject.getEditingSolution().setCustomProperties(ServoyJSONObject.toString(properties, true, true, true));
							EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
							repository.updateNodesInWorkspace(new IPersist[] { activeProject.getEditingSolution() }, false);
						}
						catch (RepositoryException ex)
						{
							ServoyLog.logError(ex);
						}
					}

					public Solution findOrCreateAuthenticatorModule(Solution mainSolution) throws RepositoryException
					{
						Solution authenticatorModule = null;
						// Assuming getModules() returns a list of modules in the solution
						IRepository localRepository = ApplicationServerRegistry.get().getLocalRepository();
						List<String> modulesList = Arrays.asList(mainSolution.getModulesNames().split(","));
						for (String moduleName : modulesList)
						{
							Solution module = (Solution)localRepository.getActiveRootObject(moduleName, IRepository.SOLUTIONS);
							if (module != null && module.getSolutionType() == SolutionMetaData.AUTHENTICATOR)
							{
								authenticatorModule = module;
								break;
							}
						}
						if (authenticatorModule == null)
						{
							String newModuleName = null;
							boolean createModule = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
								"Create Authenticator Module",
								"The authenticator module is missing. Do you want to create a new one?");
							if (createModule)
							{
								IAction newModule = new OpenWizardAction(NewModuleWizard.class, Activator.loadImageDescriptorFromBundle("module.png"),
									"Create new module");
								newModule.run();

								for (String moduleName : Arrays.asList(mainSolution.getModulesNames().split(",")))
								{
									if (!modulesList.contains(moduleName))
									{
										newModuleName = moduleName;
										break;
									}
								}
							}
							if (newModuleName == null)
							{
								return null;
							}
							authenticatorModule = (Solution)localRepository.getActiveRootObject(newModuleName, IRepository.SOLUTIONS);
							authenticatorModule.setSolutionType(SolutionMetaData.AUTHENTICATOR);
						}
						return authenticatorModule;
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
						openButton.setEnabled("SERVOY_CLOUD".equals(selection) || "OAUTH".equals(selection) || "OAUTH_AUTHENTICATOR".equals(selection));
						switch (selection)
						{
							case "SERVOY_CLOUD" :
								openButton.setToolTipText("Go to Servoy Cloud");
								break;
							case "OAUTH" :
								openButton.setToolTipText("Configure Stateless Login with an OAuth provider");
								break;
							case "OAUTH_AUTHENTICATOR" :
								openButton.setToolTipText("Select the configuration method");
								break;
							default :
								openButton.setToolTipText("");
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