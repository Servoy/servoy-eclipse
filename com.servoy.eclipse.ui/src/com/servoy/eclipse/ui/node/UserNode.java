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
package com.servoy.eclipse.ui.node;

import com.servoy.j2db.persistence.Form;

/**
 * A variant of SimpleUserNode that overrides the hashCode() and equals() methods to make nodes with same content match in hash & list approaches.
 */
public class UserNode extends SimpleUserNode
{
	public UserNode(String displayName, UserNodeType type, Object realObject, Form form, Object icon)
	{
		super(displayName, type, realObject, form, icon);
	}

	public UserNode(String displayName, UserNodeType type, Object realObject, Object icon)
	{
		super(displayName, type, realObject, icon);
	}

	public UserNode(String displayName, UserNodeType type, String codeFragment, String toolTip, Object realObject, Object icon)
	{
		super(displayName, type, new SimpleDeveloperFeedback(codeFragment, null, toolTip), realObject, icon);
	}

	public UserNode(String displayName, UserNodeType type, String codeFragment, String sampleCode, String toolTip, Object realObject, Object icon)
	{
		super(displayName, type, new SimpleDeveloperFeedback(codeFragment, sampleCode, toolTip), realObject, icon);
	}

	public UserNode(String displayName, UserNodeType type, IDeveloperFeedback developerFeedback, Object realObject, Object icon)
	{
		super(displayName, type, developerFeedback, realObject, icon);
	}

	public UserNode(String displayName, UserNodeType type, String codeFragment, String toolTip)
	{
		super(displayName, type, new SimpleDeveloperFeedback(codeFragment, null, toolTip), (Object)null, (Object)null);
	}

	public UserNode(String displayName, UserNodeType type)
	{
		super(displayName, type);
	}

	@Override
	public int hashCode()
	{
		return UserNodeComparer.hashCode(this);
	}

	@Override
	public boolean equals(Object obj)
	{
		return UserNodeComparer.equals(this, obj);
	}

}