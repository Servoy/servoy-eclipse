/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.wizards.exportsolution.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.util.string.Strings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class ModulesSelectionPage extends WizardPage implements Listener
{
	public static final String DB_DOWN_WARNING = "Error markers will be ignored because the DB seems to be offline (.dbi files will be used instead).";

	private final ExportSolutionWizard exportSolutionWizard;
	public int projectProblemsType = BuilderUtils.HAS_NO_MARKERS;
	private boolean moduleDbDownErrors = false;
	public boolean solutionVersionsPresent = false;
	private boolean applyChanges = true;

	private ArrayList<Button> checks;
	private Button checkAll;
	private final ArrayList<Label> warnLabels = new ArrayList<Label>();
	private final EclipseRepository repository;
	private String[] referencedModules;

	public ModulesSelectionPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page3");
		setTitle("Choose modules to export");
		setDescription("Select additional modules that you want to have exported too");
		this.exportSolutionWizard = exportSolutionWizard;
		referencedModules = getEntries();
		repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
	}

	String[] getEntries()
	{
		referencedModules = null;
		try
		{
			Map<String, Solution> modules = new HashMap<String, Solution>();
			exportSolutionWizard.getActiveSolution().getReferencedModulesRecursive(modules);
			if (modules.containsKey(exportSolutionWizard.getActiveSolution().getName())) modules.remove(exportSolutionWizard.getActiveSolution().getName());
			referencedModules = modules.keySet().toArray(new String[modules.keySet().size()]);
		}
		catch (Exception e)
		{
			Debug.error("Failed to retrieve referenced modules for solution.", e);
		}
		Arrays.sort(referencedModules);

		return referencedModules;
	}

	public void handleEvent(Event event)
	{
		if (!applyChanges) return;

		initializeModulesToExport();
		projectProblemsType = BuilderUtils.getMarkers(exportSolutionWizard.getModel().getModulesToExport());
		if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
		{
			moduleDbDownErrors = TableDefinitionUtils.hasDbDownErrorMarkers(exportSolutionWizard.getModel().getModulesToExport());
		}
		else
		{
			moduleDbDownErrors = false;
		}

		setErrorMessage(null);
		setMessage(null);
		if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
		{
			if (hasDBDownErrors())
			{
				projectProblemsType = BuilderUtils.HAS_WARNING_MARKERS;
				setMessage(DB_DOWN_WARNING, IMessageProvider.WARNING);
			}
			else setErrorMessage(
				"There are errors in the modules that will prevent the solution from functioning well. Please solve errors (problems view) first.");
		}
		else if (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS)
		{
			setMessage(
				"There are warnings in the modules that may prevent the solution from functioning well. You may want to solve warnings (problems view) first.",
				IMessageProvider.WARNING);
		}
		if (!solutionVersionsPresent)
		{
			setMessage("Please set a version number for the exported modules to be able to complete the export.", IMessageProvider.WARNING);
		}

		if (isCurrentPage()) getWizard().getContainer().updateButtons();

		exportSolutionWizard.getExportOptionsPage().refreshDBIDownFlag(hasDBDownErrors());

		applyChanges = false;
		checkAll.setSelection(getSelectedModules().length == referencedModules.length);
		if (getSelectedModules().length == 0)
		{
			//hide all warnings if no modules are selected
			warnLabels.stream().forEach(label -> label.setVisible(false));
		}
		applyChanges = true;
	}

	/**
	 * True if either ACTIVE solution or MODULES have db down error markers.
	 */
	public boolean hasDBDownErrors()
	{
		return exportSolutionWizard.hasActiveSolutionDbDownErrors() || moduleDbDownErrors;
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (exportSolutionWizard.getModel().isProtectWithPassword()) return exportSolutionWizard.getPasswordPage();
		else if (exportSolutionWizard.getModel().useImportSettings()) return exportSolutionWizard.getImportPage();
		else return null;
	}

	protected void initializeModulesToExport()
	{
		String[] moduleNames = getSelectedModules();
		if (moduleNames.length > 0)
		{
			exportSolutionWizard.getModel().setModulesToExport(moduleNames);
		}
		else exportSolutionWizard.getModel().setModulesToExport(null);
		final IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		solutionVersionsPresent = !Strings.isEmpty(exportSolutionWizard.getActiveSolution().getVersion()) && (moduleNames.length == 0 ||
			Arrays.stream(moduleNames).noneMatch(name -> Strings.isEmpty(servoyModel.getServoyProject(name).getSolution().getVersion())));
	}

	protected String[] getSelectedModules()
	{
		return checks.stream().filter(check -> check.getSelection()).map(check -> check.getText()).toArray(String[]::new);
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return solutionVersionsPresent && (projectProblemsType == BuilderUtils.HAS_NO_MARKERS || projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS) &&
			super.canFlipToNextPage();
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.export_solution_module_selection");
	}

	@Override
	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;

		Composite composite = new Composite(parent, SWT.NONE);
		Color backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		composite.setBackground(backgroundColor);
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridData gd = new GridData(SWT.LEFT, SWT.BEGINNING, true, false);
		gd.horizontalIndent = 10;
		gd.widthHint = 200;

		FontDescriptor descriptor = FontDescriptor.createFrom(parent.getFont());
		descriptor = descriptor.setStyle(SWT.BOLD);
		Font font = descriptor.createFont(getShell().getDisplay());

		checkAll = new Button(composite, SWT.CHECK);
		checkAll.setText("Select/Deselect All");
		checkAll.setFont(font);
		checkAll.setBackground(backgroundColor);
		checkAll.addListener(SWT.Selection, e -> {
			checks.stream().forEach(check -> check.setSelection(checkAll.getSelection()));
			handleEvent(null);
		});

		Label versionLabel = new Label(composite, SWT.NONE);
		versionLabel.setText("Version");
		versionLabel.setFont(font);
		versionLabel.setBackground(backgroundColor);
		versionLabel.setLayoutData(gd);
		new Label(composite, SWT.NONE);

		final IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		Image warn = Activator.getDefault().loadImageFromBundle("warning.png");

		checks = new ArrayList<Button>();
		for (String module : getEntries())
		{
			Button moduleCheck = new Button(composite, SWT.CHECK);
			moduleCheck.setBackground(backgroundColor);
			moduleCheck.setText(module);
			moduleCheck.setSelection(exportSolutionWizard.getModel().getModulesToExport() == null ? true
				: Arrays.stream(exportSolutionWizard.getModel().getModulesToExport()).allMatch(name -> module.equals(name)));
			moduleCheck.addListener(SWT.Selection, this);
			checks.add(moduleCheck);

			Text version = new Text(composite, SWT.BORDER);
			Label label = new Label(composite, SWT.ICON);
			String v = servoyModel.getServoyProject(module).getSolution().getVersion();
			version.setText(v == null ? "" : v);
			version.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			version.addListener(SWT.FocusOut, event -> {
				Solution solution = servoyModel.getServoyProject(module).getEditingSolution();
				solution.setVersion(version.getText());
				try
				{
					repository.updateRootObject(solution);
					handleEvent(null);
					if (isCurrentPage()) getWizard().getContainer().updateButtons();
				}
				catch (RepositoryException ex)
				{
					Debug.error(ex);
					Display.getDefault().syncExec(() -> {
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot set the version for module '" + module + "'.",
							ex.getMessage());
					});
				}
				label.setVisible(Strings.isEmpty(version.getText()));
			});

			label.setImage(warn);
			label.setVisible(Strings.isEmpty(v));
			label.setToolTipText("Please set a version for  module '" + module + "'.");
			label.setBackground(backgroundColor);
			label.setLayoutData(gd);
			warnLabels.add(label);
		}
		initializeModulesToExport();

		setControl(composite);
	}
}