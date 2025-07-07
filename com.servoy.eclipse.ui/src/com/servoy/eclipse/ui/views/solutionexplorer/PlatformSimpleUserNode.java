/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionFilter;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.IDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IPersist;

public class PlatformSimpleUserNode extends SimpleUserNode implements IAdaptable, IActionFilter
{
	public static final String TYPE = "type";

	public PlatformSimpleUserNode(String displayName, UserNodeType type)
	{
		super(displayName, type);
	}

	public PlatformSimpleUserNode(String displayName, UserNodeType type, Object realObject, Image icon, Class< ? > realType, IDeveloperFeedback feedback)
	{
		super(displayName, type, realObject, icon, realType, feedback);
	}

	public PlatformSimpleUserNode(String displayName, UserNodeType type, Object realObject, Image icon)
	{
		super(displayName, type, realObject, icon);
	}

	public PlatformSimpleUserNode(String displayName, UserNodeType type, String codeFragment, String toolTip, Object realObject, Image icon)
	{
		super(displayName, type, new SimpleDeveloperFeedback(codeFragment, null, toolTip), realObject, icon);
	}

	public PlatformSimpleUserNode(String displayName, UserNodeType type, Object realObject, IPersist containingPersist, Image icon)
	{
		super(displayName, type, realObject, containingPersist, icon);
	}

	public PlatformSimpleUserNode(String displayName, UserNodeType type, String codeFragment, String sampleCode, String toolTip, Object realObject, Image icon)
	{
		super(displayName, type, new SimpleDeveloperFeedback(codeFragment, sampleCode, toolTip), realObject, icon);
	}

	/**
	 * @see com.servoy.j2db.develop.node.SimpleUserNode#getIcon()
	 */
	@Override
	public Image getIcon()
	{
		Image icon = super.getIcon();
		if (hidden)
		{
			icon = Activator.getDefault().createGrayImage(getName(), icon);
		}
		return icon;
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		return AdapterManager.getDefault().getAdapter(this, adapter);
	}

	public boolean testAttribute(Object target, String name, String value)
	{
		if (name != null && name.toLowerCase().equals(TYPE))
		{
			if (value != null && value.toLowerCase().equals("all_solutions") && getType() == UserNodeType.ALL_SOLUTIONS) return true;
		}
		return false;
	}
}
