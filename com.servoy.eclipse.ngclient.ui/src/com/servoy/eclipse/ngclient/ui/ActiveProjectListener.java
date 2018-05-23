package com.servoy.eclipse.ngclient.ui;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.SolutionMetaData;

public class ActiveProjectListener implements IActiveProjectListener
{
	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}

	@Override
	public void activeProjectChanged(final ServoyProject activeProject)
	{
		if (SolutionMetaData.isServoyNGSolution(activeProject.getSolution()) && !activeProject.getSolution().getName().equals("import_placeholder"))
		{
			// TODO if project is changed the projects web packages should be copied into something..
		}
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
	}
}
