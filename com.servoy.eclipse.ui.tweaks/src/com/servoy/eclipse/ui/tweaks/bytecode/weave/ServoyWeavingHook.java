package com.servoy.eclipse.ui.tweaks.bytecode.weave;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

import com.servoy.eclipse.model.util.ServoyLog;

public class ServoyWeavingHook implements WeavingHook
{

	public static final String WEAVING_HOOK_EXTENSION_ID = "com.servoy.eclipse.ui.tweaks.weavingHook";
	public static final String CLASS_NAME_TO_WEAVE_KEY = "fullClassNameToWeave";
	public static final String WEAVER_CLASS_KEY = "weaverClass";

	Map<String, BasicWeaver> classWeavers = new HashMap<>();

	public ServoyWeavingHook()
	{
		// read extension points
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(WEAVING_HOOK_EXTENSION_ID);
		IExtension[] extensions = ep.getExtensions();

		for (IExtension extension : extensions)
		{
			IConfigurationElement[] ces = extension.getConfigurationElements();
			for (IConfigurationElement ce : ces)
			{
				String classNameToWeave = ce.getAttribute(CLASS_NAME_TO_WEAVE_KEY);
				try
				{
					BasicWeaver weaverClass = (BasicWeaver)ce.createExecutableExtension(WEAVER_CLASS_KEY);
					classWeavers.put(classNameToWeave, weaverClass);
				}
				catch (CoreException e)
				{
					ServoyLog.logError("Could not load extension (extension point " + WEAVER_CLASS_KEY + ", " + ce.getAttribute(WEAVER_CLASS_KEY) + ")", e);
				}
				catch (ClassCastException e)
				{
					ServoyLog.logError("Extension class has wrong type (extension point " + WEAVER_CLASS_KEY + ", " + ce.getAttribute(WEAVER_CLASS_KEY) + ")",
						e);
				}
			}
		}
	}

	@Override
	public void weave(WovenClass wovenClass)
	{
		BasicWeaver weaver = classWeavers.get(wovenClass.getClassName());
		if (weaver != null)
		{
			weaver.weaveClass(wovenClass);
		}
	}

}
