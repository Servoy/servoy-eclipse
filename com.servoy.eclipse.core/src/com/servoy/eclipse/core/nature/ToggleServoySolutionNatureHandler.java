package com.servoy.eclipse.core.nature;

import org.eclipse.dltk.javascript.core.JavaScriptNature;

import com.servoy.eclipse.model.nature.ServoyProject;

public class ToggleServoySolutionNatureHandler extends ToggleNatureHandler
{

	public ToggleServoySolutionNatureHandler()
	{
		super(ServoyProject.NATURE_ID, new String[] { JavaScriptNature.NATURE_ID });
	}

}
