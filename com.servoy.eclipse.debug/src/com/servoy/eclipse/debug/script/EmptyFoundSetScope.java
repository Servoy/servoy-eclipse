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
package com.servoy.eclipse.debug.script;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

/**
 * @author jcompagner
 * 
 */
public class EmptyFoundSetScope extends ScriptObjectClassScope implements IProposalHolder
{
	/**
	 * @param parent
	 */
	public EmptyFoundSetScope(Scriptable parent, Class scriptObjectClass)
	{
		super(parent, scriptObjectClass, "foundset");
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return EmptyFoundSetScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<Object> al = new ArrayList<Object>();
		al.add("alldataproviders"); //$NON-NLS-1$

		al.add("recordIndex"); //$NON-NLS-1$
		al.add("selectedIndex"); //$NON-NLS-1$
		al.add("maxRecordIndex"); //$NON-NLS-1$

		al.addAll(Arrays.asList(super.getIds()));

		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (name.equals("alldataproviders"))
		{
			return new NativeArray();
		}
		if (name.equals("recordIndex"))
		{
			return name;
		}
		if (name.equals("selectedIndex"))
		{
			return name;
		}
		if (name.equals("maxRecordIndex"))
		{
			return name;
		}
		return super.get(name, start);
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getObject()
	 */
	@Override
	public Object getObject()
	{
		return this;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getParameterNames()
	 */
	@Override
	public String[] getParameterNames()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getProposalInfo()
	 */
	@Override
	public String getProposalInfo()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	@Override
	public boolean isFunctionRef()
	{
		// relations and foundset are fields
		return false;
	}

	/**
	 * @see com.servoy.eclipse.debug.script.ScriptObjectClassScope#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof EmptyFoundSetScope)
		{
			return true;
		}
		return false;
	}
}
