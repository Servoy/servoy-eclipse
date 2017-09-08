package com.servoy.eclipse.firststeps.ui.actions;

import java.util.Properties;

import com.servoy.eclipse.cheatsheets.OpenCheatSheet;
import com.servoy.eclipse.firststeps.DialogManager;

public class NewForm implements IAction
{
	@Override
	public void run(String arguments)
	{
		DialogManager.getInstance().close();
		Properties p = new Properties();
		p.setProperty("id", "com.servoy.eclipse.ui.cheatsheet.firstcontact");
		new OpenCheatSheet().run(null, p);
	}
}