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
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.scripting.DefaultScope;

public class MethodScope extends DefaultScope implements IProposalHolder
{

	private final String proposalInfo;
	private final boolean functionRef;
	private final URL imageUrl;
	private final ScriptMethod scriptMethod;

	public MethodScope(Scriptable parent, ScriptMethod scriptMethod, String proposalInfo, boolean functionRef, URL imageUrl)
	{
		super(parent);
		this.scriptMethod = scriptMethod;

		this.proposalInfo = proposalInfo;
		this.functionRef = functionRef;
		this.imageUrl = imageUrl;
	}

	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();

		al.add("apply"); //$NON-NLS-1$
		al.add("toString"); //$NON-NLS-1$
		al.add("call"); //$NON-NLS-1$

		return al.toArray();
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (name.equals("toString")) //$NON-NLS-1$
		{
			return new ProposalHolder("", null, null, true, null); //$NON-NLS-1$
		}
		return new ProposalHolder(null, null, null, true, null);
	}

	public URL getImageURL()
	{
		return this.imageUrl;
	}

	public Object getObject()
	{
		return this;
	}

	public String[] getParameterNames()
	{
		MethodArgument[] arguments = scriptMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
		if (arguments != null && arguments.length > 0)
		{
			String[] nms = new String[arguments.length];
			for (int i = 0; i < arguments.length; i++)
			{
				nms[i] = arguments[i].getName();
			}
			return nms;
		}
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getReturnType()
	 */
	public String getReturnType()
	{
		// TODO regexp
		String declaration = scriptMethod.getDeclaration();
		int index = declaration.indexOf("*/"); //$NON-NLS-1$
		if (index != -1)
		{
			String comment = declaration.substring(0, index);

			index = comment.indexOf("@return"); //$NON-NLS-1$
			if (index != -1)
			{
				int endOfLine = comment.indexOf("\n", index); //$NON-NLS-1$
				if (endOfLine == -1) endOfLine = comment.length();

				comment = comment.substring(index, endOfLine);
				StringTokenizer st = new StringTokenizer(comment);
				while (st.hasMoreTokens())
				{
					String token = st.nextToken();
					if (token.startsWith("{") && token.endsWith("}")) //$NON-NLS-1$ //$NON-NLS-2$
					{
						return token.substring(1, token.length() - 1);
					}
				}
			}
		}
		return null;
	}

	public String getProposalInfo()
	{
		return this.proposalInfo;
	}

	public IFile getSourceFile()
	{
		String filename = SolutionSerializer.getScriptPath(scriptMethod, false);
		if (filename != null)
		{
			return ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(filename));
		}
		return null;
	}

	public boolean isFunctionRef()
	{
		return this.functionRef;
	}

	@Override
	public String getClassName()
	{
		return MethodScope.class.getName();
	}

}
