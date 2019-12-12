package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.exporter.ngdesktop.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.wizards.IExportSolutionWizardProvider;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author gboros
 */
public class NGDesktopExporterProvider implements IExportSolutionWizardProvider
{
	public IAction getExportAction()
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject != null && (activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SOLUTION ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.NG_CLIENT_ONLY))
		{
			return new OpenWizardAction(ExportNGDesktopWizard.class, AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "$nl$/icons/ng_export.png"),
				"NG Desktop Export");
		}
		return null;
	}

}
