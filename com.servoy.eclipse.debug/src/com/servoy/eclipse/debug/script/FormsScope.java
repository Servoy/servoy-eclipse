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
import java.util.Iterator;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author jcompagner
 * 
 */
class FormsScope extends DefaultScope
{
	/**
	 * @param parent
	 */
	FormsScope(Scriptable parent)
	{
		super(parent);
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return FormsScope.class.getName();
	}


	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();
		al.add("allnames"); //$NON-NLS-1$
		al.add("length"); //$NON-NLS-1$
		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		Iterator<Form> forms = fs.getForms(false);
		while (forms.hasNext())
		{
			al.add(forms.next().getName());
		}
		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (name.equals("allnames") || name.equals("length")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			String doc = FormDomProvider.getDoc(name, com.servoy.eclipse.core.scripting.docs.Forms.class, ""); //$NON-NLS-1$
			return new ProposalHolder(null, null, name.equals("allnames") ? "Array" : "Number", doc, false, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		Form form = fs.getForm(name);
		if (form != null)
		{
			return new FormScope(getParentScope(), form);
		}
		return Scriptable.NOT_FOUND;
	}
}
