package com.servoy.eclipse.aibridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.ui.tweaks.IconPreferences;

public class ActivateOnCompletion extends Action
{

	public ActivateOnCompletion()
	{
		super("Activate on completion", SWT.TOGGLE);
		boolean isDarkTheme = IconPreferences.getInstance().getUseDarkThemeIcons();
		String iconPath = isDarkTheme ? "darkicons/bringtofront.png" : "icons/bringtofront.png";
		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", iconPath);

		setImageDescriptor(imageDescriptor);
	}
}
