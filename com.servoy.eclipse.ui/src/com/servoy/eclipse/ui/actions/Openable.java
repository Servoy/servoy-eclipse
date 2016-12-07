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
package com.servoy.eclipse.ui.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IActionFilter;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Openable items for Open menu actions.
 *
 * @author rgansevles
 *
 */
public class Openable implements IActionFilter
{
	private final Object data;

	private Openable(Object data)
	{
		this.data = data;
	}

	public Object getData()
	{
		return data;
	}

	private Map<String, String> attributes = null;

	public void setAttribute(String name, String value)
	{
		if (attributes == null) attributes = new HashMap<String, String>();
		attributes.put(name, value);
	}

	@Override
	public boolean testAttribute(Object target, String name, String value)
	{
		return attributes != null && Utils.equalObjects(value, attributes.get(name));
	}

	public static Openable getOpenable(Object data)
	{
		// create specific subclasses, see objectClass visibility for objectContribution extension point
		if (data instanceof IPersist)
		{
			IPersist persist = (IPersist)data;
			if (persist instanceof ScriptMethod)
			{
				return new OpenableScriptMethod((ScriptMethod)persist);
			}
			Form form = (Form)persist.getAncestor(IRepository.FORMS);
			if (form != null)
			{
				return new OpenableForm(form);
			}
		}
		if (data instanceof Pair< ? , ? > && ((Pair< ? , ? >)data).getLeft() instanceof Solution && ((Pair< ? , ? >)data).getRight() instanceof String)
		{
			return new OpenableGlobalScope((Pair<Solution, String>)data);
		}
		return new Openable(data);
	}

	public static class OpenableForm extends Openable
	{
		public OpenableForm(Form data)
		{
			super(data);
		}

		@Override
		public Form getData()
		{
			return (Form)super.getData();
		}
	}

	public static class OpenableGlobalScope extends Openable
	{
		public OpenableGlobalScope(Pair<Solution, String> data)
		{
			super(data);
		}

		@Override
		public Pair<Solution, String> getData()
		{
			return (Pair<Solution, String>)super.getData();
		}
	}

	public static class OpenableScriptMethod extends Openable
	{
		public OpenableScriptMethod(ScriptMethod data)
		{
			super(data);
		}

		@Override
		public ScriptMethod getData()
		{
			return (ScriptMethod)super.getData();
		}
	}
}
