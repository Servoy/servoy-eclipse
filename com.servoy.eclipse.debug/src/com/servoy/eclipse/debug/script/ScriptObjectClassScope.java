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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.ScriptObjectRegistry;

/**
 * @author jcompagner
 * 
 */
public class ScriptObjectClassScope extends DefaultScope implements IProposalHolder
{
	protected final static URL METHOD = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/function.gif"), null); //$NON-NLS-1$
	private final static URL VARIABLE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/properties_icon.gif"), null); //$NON-NLS-1$
	private final static URL CONSTANT = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/constant.gif"), null); //$NON-NLS-1$
	private static JavaMembers objectMembers;

	private final Class< ? > scriptObjectClass;
	private final String name;
	private final String[] parameterNames;
	private final String doc;
	private final boolean functionRef;
	private boolean returnOnlyConstants;
	private final URL image;


	/**
	 * @param parent
	 */
	public ScriptObjectClassScope(Scriptable parent, Class< ? > scriptObjectClass, String name)
	{
		this(parent, scriptObjectClass, name, null, null, false);
	}

	public ScriptObjectClassScope(Scriptable parent, Class< ? > scriptObjectClass, String name, boolean constant)
	{
		this(parent, scriptObjectClass, name, null, null, false);
		returnOnlyConstants = constant;
	}

	/**
	 * @param scope
	 * @param returnType
	 * @param key
	 * @param parameterNames
	 * @param doc
	 */
	public ScriptObjectClassScope(Scriptable parent, Class< ? > scriptObjectClass, String name, String[] parameterNames, String doc)
	{
		this(parent, scriptObjectClass, name, parameterNames, doc, true);
	}

	public ScriptObjectClassScope(Scriptable parent, Class< ? > scriptObjectClass, String name, String[] parameterNames, String doc, boolean functionRef)
	{
		this(parent, scriptObjectClass, name, parameterNames, doc, functionRef, null);
	}

	/**
	 * @param scriptObjectClassScope
	 * @param returnType
	 * @param name2
	 * @param parameterNames2
	 * @param sample2
	 * @param b
	 * @param image
	 */
	public ScriptObjectClassScope(Scriptable parent, Class< ? > scriptObjectClass, String name, String[] parameterNames, String doc, boolean functionRef,
		URL image)
	{
		super(parent);
		this.scriptObjectClass = scriptObjectClass;
		this.name = name;
		this.parameterNames = parameterNames;
		this.doc = doc;
		this.functionRef = functionRef;
		if (image == null)
		{
			if (functionRef)
			{
				this.image = METHOD;
			}
			else
			{
				this.image = VARIABLE;
			}
		}
		else
		{
			this.image = image;
		}
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getReturnType()
	 */
	public String getReturnType()
	{
		return FormDomProvider.getFormattedType(scriptObjectClass);
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return ScriptObjectClassScope.class.getName();
	}

	/**
	 * @return the scriptObjectClass
	 */
	public Class< ? > getScriptObjectClass()
	{
		return scriptObjectClass;
	}


	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
		if (javaMembers != null)
		{
			if (!returnOnlyConstants)
			{
				Object[] members = javaMembers.getIds(false);
				for (Object element : members)
				{
					if (scriptObject != null && scriptObject.isDeprecated((String)element)) continue;
					al.add((String)element);
				}
			}
			if (IConstantsObject.class.isAssignableFrom(scriptObjectClass))
			{
				Object[] members = javaMembers.getIds(true);
				for (Object element : members)
				{
					if (scriptObject != null && scriptObject.isDeprecated((String)element)) continue;
					al.add((String)element);
				}
			}
		}
		CalculationModeHandler cm = CalculationModeHandler.getInstance();
		if (cm.hasPartialList(name))
		{
			for (int i = al.size(); --i >= 0;)
			{
				if (cm.hide(name, al.get(i)))
				{
					al.remove(i);
				}
			}
		}
		if (javaMembers instanceof InstanceJavaMembers)
		{
			al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
		}
		else
		{
			if (objectMembers == null)
			{
				objectMembers = new JavaMembers(null, Object.class);
			}
			al.removeAll(objectMembers.getMethodIds(false));
		}
		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String nameId, Scriptable start)
	{
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
		if (javaMembers != null)
		{
			URL proposalImage = null;
			Object object = javaMembers.getMethod(nameId, false);
			if (object == null)
			{
				object = javaMembers.getField(nameId, false);
				if (object == null)
				{
					object = javaMembers.getField(nameId, true);
					if (object != null)
					{
						proposalImage = CONSTANT;
					}
				}
				else proposalImage = VARIABLE;
			}
			else proposalImage = METHOD;
			if (object != null)
			{
				Class< ? > returnType = FormDomProvider.getReturnType(object, this);
				if (returnType != null)
				{
					if (returnType == com.servoy.j2db.scripting.FormScope.class) return new FormScope(this, null, FormDomProvider.getParameterNames(nameId,
						scriptObjectClass), FormDomProvider.getDoc(nameId, scriptObjectClass, this.name), object instanceof NativeJavaMethod, proposalImage);
					return new ScriptObjectClassScope(this, returnType, nameId, FormDomProvider.getParameterNames(nameId, scriptObjectClass),
						FormDomProvider.getDoc(nameId, scriptObjectClass, this.name), object instanceof NativeJavaMethod, proposalImage);
				}
				else
				{
					return new ProposalHolder(object, FormDomProvider.getParameterNames(nameId, scriptObjectClass), FormDomProvider.getDoc(nameId,
						scriptObjectClass, this.name), false, proposalImage);
				}
			}
		}
		return super.get(nameId, start);
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
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getProposalInfo()
	 */
	public String getProposalInfo()
	{
		return doc;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getImageURL()
	 */
	public URL getImageURL()
	{
		return image;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	public boolean isFunctionRef()
	{
		return functionRef;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ScriptObjectClassScope)
		{
			ScriptObjectClassScope socs = (ScriptObjectClassScope)obj;
			if (scriptObjectClass == socs.scriptObjectClass && name.equals(socs.name))
			{
				if ((doc == null && socs.doc == null) || (doc != null && doc.equals(socs.doc)))
				{
					if (parameterNames == null && socs.parameterNames == null) return true;
					if (parameterNames != null && socs.parameterNames != null && parameterNames.length == socs.parameterNames.length)
					{
						for (int i = 0; i < parameterNames.length; i++)
						{
							if (!parameterNames[i].equals(socs.parameterNames[i])) return false;
						}
						return true;
					}
				}
			}
		}
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
