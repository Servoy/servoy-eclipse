package com.servoy.eclipse.extension.compatibility;

import com.servoy.eclipse.core.IPluginBaseClassLoaderProvider;

public class PluginBaseClassLoaderProvider implements IPluginBaseClassLoaderProvider 
{
	@Override
	public ClassLoader getClassLoader() 
	{
		return getClass().getClassLoader();
	}
}
