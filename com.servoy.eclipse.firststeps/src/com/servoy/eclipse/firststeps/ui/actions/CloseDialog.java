package com.servoy.eclipse.firststeps.ui.actions;

import com.servoy.eclipse.firststeps.DialogManager;

public class CloseDialog implements IAction
{
	@Override
	public void run(String arguments)
	{
		DialogManager.getInstance().close();
	}
}
