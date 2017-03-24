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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;

import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class ModulesSelectionPage extends ListSelectionPage
{
	public static final String DB_DOWN_WARNING = "Error markers will be ignored because the DB seems to be offline (.dbi files will be used instead).";

	private final ExportSolutionWizard exportSolutionWizard;
	public int projectProblemsType = BuilderUtils.HAS_NO_MARKERS;
	private boolean moduleDbDownErrors = false;

	public ModulesSelectionPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page3", "Choose modules to export", "Select additional modules that you want to have exported too");
		this.exportSolutionWizard = exportSolutionWizard;
	}

	@Override
	String[] getEntries()
	{
		String[] moduleNames = null;
		try
		{
			Map<String, Solution> modules = new HashMap<String, Solution>();
			exportSolutionWizard.getActiveSolution().getReferencedModulesRecursive(modules);
			if (modules.containsKey(exportSolutionWizard.getActiveSolution().getName())) modules.remove(exportSolutionWizard.getActiveSolution().getName());
			moduleNames = modules.keySet().toArray(new String[modules.keySet().size()]);
		}
		catch (Exception e)
		{
			Debug.error("Failed to retrieve referenced modules for solution.", e);
		}
		Arrays.sort(moduleNames);

		return moduleNames;
	}

	public void checkStateChanged(CheckStateChangedEvent event)
	{
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

		if (isCurrentPage()) getWizard().getContainer().updateButtons();

		exportSolutionWizard.getExportOptionsPage().refreshDBIDownFlag(hasDBDownErrors());

		if (treeViewer.getCheckedElements().length == treeViewer.getTree().getItemCount() && treeViewer.getCheckedElements().length == 0)
		{
			selectAllButtons.disableButtons();
		}
		else if (treeViewer.getCheckedElements().length < treeViewer.getTree().getItemCount())
		{
			selectAllButtons.enableAll();
		}
		else
		{
			selectAllButtons.disableSelectAll();
		}
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
		Object[] currentSelection = treeViewer.getCheckedElements();
		if (currentSelection.length > 0)
		{
			String[] moduleNames = new String[currentSelection.length];
			for (int i = 0; i < currentSelection.length; i++)
				moduleNames[i] = ((String)currentSelection[i]);
			exportSolutionWizard.getModel().setModulesToExport(moduleNames);
		}
		else exportSolutionWizard.getModel().setModulesToExport(null);
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return (projectProblemsType == BuilderUtils.HAS_NO_MARKERS || projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS) && super.canFlipToNextPage();
	}
}