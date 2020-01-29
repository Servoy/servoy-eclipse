package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ResourceLocator;

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
	@Override
	public IAction getExportAction()
	{
		final ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject != null && (activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SOLUTION ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.NG_CLIENT_ONLY))
			return new OpenWizardAction(ExportNGDesktopWizard.class,
				ResourceLocator.imageDescriptorFromBundle(Activator.PLUGIN_ID, "$nl$/icons/ng_export.png").orElse(null),
				"NG Desktop Export");
		return null;
	}

}
