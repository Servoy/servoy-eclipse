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

import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author jcompagner
 * 
 */
class EmptyFormScope extends DefaultScope implements IProposalHolder
{
	/**
	 * @param parent
	 */
	EmptyFormScope(Scriptable parent)
	{
		super(parent);
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return EmptyFormScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();

		// first the fixed.
		al.add("allnames"); //$NON-NLS-1$
		al.add("alldataproviders");//$NON-NLS-1$
		al.add("allmethods");//$NON-NLS-1$
		al.add("allrelations");//$NON-NLS-1$
		al.add("allvariables");//$NON-NLS-1$

		// controller, elements and foundset
		al.add("controller");//$NON-NLS-1$
		al.add("foundset");//$NON-NLS-1$

		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (name.equals("allnames") || name.equals("alldataproviders") || name.equals("allmethods") || name.equals("allrelations") || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			name.equals("allvariables")) //$NON-NLS-1$
		{
			return new String[0];
		}
		if (name.equals("controller"))//$NON-NLS-1$
		{
			return new ScriptObjectClassScope(this, JSForm.class, "controller");//$NON-NLS-1$
		}
		if (name.equals("foundset"))//$NON-NLS-1$
		{
			return new EmptyFoundSetScope(this, FoundSet.class);
		}
		return Scriptable.NOT_FOUND;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getObject()
	 */
	public Object getObject()
	{
		return this;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getParameterNames()
	 */
	public String[] getParameterNames()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getReturnType()
	 */
	public String getReturnType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getImageURL()
	 */
	public URL getImageURL()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getProposalInfo()
	 */
	public String getProposalInfo()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	public boolean isFunctionRef()
	{
		return false;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getSourceFile()
	 */
	public IFile getSourceFile()
	{
		return null;
	}
}
