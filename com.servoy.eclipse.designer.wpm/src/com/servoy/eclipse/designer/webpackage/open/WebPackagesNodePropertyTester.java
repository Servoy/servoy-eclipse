package com.servoy.eclipse.designer.webpackage.open;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

public class WebPackagesNodePropertyTester extends org.eclipse.core.expressions.PropertyTester
{

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
	{
		if (receiver instanceof SimpleUserNode)
		{
			return (((SimpleUserNode)receiver).getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES) ||
				((((SimpleUserNode)receiver).getType() == UserNodeType.SOLUTION) && ((SimpleUserNode)receiver).getRealObject() != null);
		}
		return false;
	}

}
