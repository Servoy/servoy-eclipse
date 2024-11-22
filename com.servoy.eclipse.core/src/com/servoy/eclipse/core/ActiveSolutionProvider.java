package com.servoy.eclipse.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import com.servoy.eclipse.model.nature.ServoyProject;

public class ActiveSolutionProvider extends AbstractSourceProvider
{
	public static final String NG_STATE = "com.servoy.eclipse.core.ngClientState";
	public static final String MOBILE_STATE = "com.servoy.eclipse.core.mobileState";
	public static final String WEB_STATE = "com.servoy.eclipse.core.webClientState";
	public static final String SMART_STATE = "com.servoy.eclipse.core.smartClientState";
	public static final String NGDESKTOP_STATE = "com.servoy.eclipse.core.ngDesktopClientState";

	private IDeveloperServoyModel sm;
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

			private void setStartMenuButtonsState()
			{
				fireSourceChanged(ISources.WORKBENCH, NG_STATE, getNGVariableState());
				fireSourceChanged(ISources.WORKBENCH, MOBILE_STATE, getMobileVariableState());
				fireSourceChanged(ISources.WORKBENCH, WEB_STATE, getWebVariableState());
				fireSourceChanged(ISources.WORKBENCH, SMART_STATE, getSmartClientVariableState());
				fireSourceChanged(ISources.WORKBENCH, NGDESKTOP_STATE, getNGDesktopVariableState());
			}

			@Override
			public void activeProjectChanged(ServoyProject activeProject)
			{
				setStartMenuButtonsState();
			}

			@Override
			public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
			{
				if (updateInfo == IActiveProjectListener.SOLUTION_TYPE_CHANGED) setStartMenuButtonsState();
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
		Map<String, Object> map = new HashMap<String, Object>(2);
		map.put(MOBILE_STATE, getMobileVariableState());
		map.put(WEB_STATE, getWebVariableState());
		map.put(SMART_STATE, getSmartClientVariableState());
		map.put(NG_STATE, getNGVariableState());
		map.put(NGDESKTOP_STATE, getNGDesktopVariableState());

		return map;
	}

	private String translateEnablement(boolean source)
	{
		if (source) return "ENABLED";
		else return "DISABLED";
	}

	private String getMobileVariableState()
	{
		return translateEnablement(sm.isActiveSolutionMobile());
	}


	private String getSmartClientVariableState()
	{
		return translateEnablement(sm.isActiveSolutionSmartClient());
	}


	private String getWebVariableState()
	{
		return translateEnablement(sm.isActiveSolutionWeb());
	}

	private String getNGVariableState()
	{
		return translateEnablement(sm.isActiveSolutionNGClient() || sm.isActiveSolutionMobile());
	}

	private String getNGDesktopVariableState()
	{
		return translateEnablement(sm.isActiveSolutionNGClient());
	}

	@Override
	public String[] getProvidedSourceNames()
	{
		return new String[] { NG_STATE, MOBILE_STATE, WEB_STATE, SMART_STATE, NGDESKTOP_STATE };
	}

}
