package com.servoy.eclipse.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import com.servoy.eclipse.model.nature.ServoyProject;

public class ActiveSolutionProvider extends AbstractSourceProvider
{

	public static final String MOBILE_STATE = "com.servoy.eclipse.core.mobileState"; //$NON-NLS-1$
	private ServoyModel sm;
	private IActiveProjectListener listener;

	public ActiveSolutionProvider()
	{
		sm = ServoyModelManager.getServoyModelManager().getServoyModel();
		sm.addActiveProjectListener(listener = new IActiveProjectListener()
		{

			@Override
			public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
			{
				return true;
			}

			@Override
			public void activeProjectChanged(ServoyProject activeProject)
			{
				fireSourceChanged(ISources.WORKBENCH, MOBILE_STATE, getMobileVariableState());
			}

			@Override
			public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
			{
			}

		});
	}

	@Override
	public void dispose()
	{
		sm.removeActiveProjectListener(listener);
		sm = null;
	}

	@Override
	public Map<String, Object> getCurrentState()
	{
		Map<String, Object> map = new HashMap<String, Object>(1);
		map.put(MOBILE_STATE, getMobileVariableState());
		return map;
	}

	private Boolean getMobileVariableState()
	{
		return Boolean.valueOf(sm.isActiveSolutionMobile());
	}

	@Override
	public String[] getProvidedSourceNames()
	{
		return new String[] { MOBILE_STATE };
	}

}
