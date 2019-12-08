package com.servoy.eclipse.ui.tweaks.bytecode.weave;

import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

public class ServoyWeavingHook implements WeavingHook
{
	private final BasicWeaver weaver = new ImageDescriptorWeaver();

	@Override
	public void weave(WovenClass wovenClass)
	{
		if (wovenClass.getClassName().equals("org.eclipse.jface.resource.ImageDescriptor"))
		{
			weaver.weaveClass(wovenClass);
		}
	}

}
