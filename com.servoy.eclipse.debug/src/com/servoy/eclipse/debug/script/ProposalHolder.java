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

import org.eclipse.core.resources.IFile;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;

/**
 * @author jcompagner
 * 
 */
public class ProposalHolder implements IProposalHolder
{

	private final Object object;
	private final String[] parameterNames;
	private final String proposalInfo;
	private final boolean functionRef;
	private final URL imageUrl;
	private final IFile sourceFile;
	private final String returnType;

	/**
	 * @param object
	 * @param parameterNames
	 */
	public ProposalHolder(Object object, String[] parameterNames, String proposalInfo, boolean functionRef, URL imageUrl)
	{
		this(object, parameterNames, proposalInfo, functionRef, imageUrl, null);
	}

	public ProposalHolder(Object object, String[] parameterNames, String proposalInfo, boolean functionRef, URL imageUrl, IFile sourceFile)
	{
		this(object, parameterNames, null, proposalInfo, functionRef, imageUrl, sourceFile);
	}

	public ProposalHolder(Object object, String[] parameterNames, String returnType, String proposalInfo, boolean functionRef, URL imageUrl, IFile sourceFile)
	{
		this.object = object;
		this.parameterNames = parameterNames;
		if (returnType != null)
		{
			this.returnType = returnType;
		}
		else
		{
			Class< ? > type = null;
			if (object instanceof NativeJavaMethod)
			{
				MemberBox[] methods = ((NativeJavaMethod)object).getMethods();
				if (methods != null && methods.length > 0)
				{
					type = methods[0].getReturnType();
				}
			}
			this.returnType = FormDomProvider.getFormattedType(type);
		}
		this.proposalInfo = proposalInfo;
		this.functionRef = functionRef;
		this.imageUrl = imageUrl;
		this.sourceFile = sourceFile;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getParameterNames()
	 */
	public char[][] getParameterNames()
	{
		if (parameterNames != null && parameterNames.length > 0)
		{
			char[][] nms = new char[parameterNames.length][];
			for (int i = 0; i < parameterNames.length; i++)
			{
				nms[i] = parameterNames[i].toCharArray();
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
		return returnType;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getImageURL()
	 */
	public URL getImageURL()
	{
		return imageUrl;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getObject()
	 */
	public Object getObject()
	{
		return object;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getProposalInfo()
	 */
	public String getProposalInfo()
	{
		return proposalInfo;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	public boolean isFunctionRef()
	{
		return functionRef;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getSourceFile()
	 */
	public IFile getSourceFile()
	{
		return sourceFile;
	}
}