package com.servoy.eclipse.aibridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ActivateOnCompletion extends Action {

	public ActivateOnCompletion() {
		super("Activate on completion", SWT.TOGGLE);
		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", "icons/bringtofront.png");
        setImageDescriptor(imageDescriptor);
	}
}
