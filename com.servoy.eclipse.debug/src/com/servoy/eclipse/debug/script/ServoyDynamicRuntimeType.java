/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.dltk.javascript.typeinfo.IJavaScriptLikeObject;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.ImmutableType;
import org.eclipse.dltk.javascript.typeinfo.RSimpleType;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

/**
 * @author jcompagner
 *
 */
public class ServoyDynamicRuntimeType extends RSimpleType implements IJavaScriptLikeObject, ImmutableType<ServoyDynamicRuntimeType>
{
	/**
	 * @param typeSystem
	 * @param declaration
	 */
	public ServoyDynamicRuntimeType(IRTypeDeclaration declaration)
	{
		super(declaration);
	}

	/**
	 * @param typeSystem
	 * @param type
	 */
	public ServoyDynamicRuntimeType(ITypeSystem typeSystem, Type type)
	{
		super(typeSystem, type);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#isAssignableFrom(org.eclipse.dltk.javascript.typeinfo.IRType)
	 */
	@Override
	public TypeCompatibility isAssignableFrom(IRType type)
	{
		TypeCompatibility compatible = super.isAssignableFrom(type);
		if (!compatible.ok() && type instanceof ServoyDynamicRuntimeType)
		{
			IRTypeDeclaration other = ((RSimpleType)type).getDeclaration();
			return isAssignableFrom(getDeclaration(), other, new HashSet<IRTypeDeclaration>());
		}
		return compatible;
	}

	static TypeCompatibility isAssignableFrom(IRTypeDeclaration self, IRTypeDeclaration current, Set<IRTypeDeclaration> visited)
	{
		if (visited.add(current))
		{
			String selfName = self.getName();
			String currentName = current.getName();
			if (selfName.equals(currentName))
			{
				return TypeCompatibility.TRUE;
			}
			String selfConfig = null;
			String currentConfig = null;
			if (selfName.contains("<") && selfName.endsWith(">"))
			{
				selfConfig = selfName.substring(selfName.indexOf('<') + 1, selfName.length() - 1);
				selfName = selfName.substring(0, selfName.indexOf('<'));
			}
			if (currentName.contains("<") && currentName.endsWith(">"))
			{
				currentConfig = currentName.substring(currentName.indexOf('<') + 1, currentName.length() - 1);
				currentName = currentName.substring(0, currentName.indexOf('<'));
			}
			if (selfName.equals(currentName))
			{
				if (selfConfig == null)
				{
					return TypeCompatibility.TRUE;
				}
				if (selfConfig.startsWith("{") && selfConfig.endsWith("}"))
				{
					selfConfig = selfConfig.substring(1, selfConfig.length() - 1).trim();
				}
				if (currentConfig != null && currentConfig.startsWith("{") && currentConfig.endsWith("}"))
				{
					currentConfig = currentConfig.substring(1, currentConfig.length() - 1).trim();
				}
				if (selfConfig.equals(currentConfig))
				{
					return TypeCompatibility.TRUE;
				}
				if (currentConfig != null)
				{
					Map<String, String> currentConfigProperties = new HashMap<String, String>();
					String[] props = currentConfig.split(",");
					for (String prop : props)
					{
						String[] propAndValue = prop.split(":");
						currentConfigProperties.put(propAndValue[0].trim(), propAndValue.length > 1 ? propAndValue[1].trim() : "");
					}
					props = selfConfig.split(",");
					for (String prop : props)
					{
						// must be present in the current config, and must have the same value
						String[] propAndValue = prop.split(":");
						if (!currentConfigProperties.containsKey(propAndValue[0].trim()) ||
							!currentConfigProperties.get(propAndValue[0].trim()).equals(propAndValue.length > 1 ? propAndValue[1].trim() : ""))
						{
							return TypeCompatibility.FALSE;
						}
					}
					return TypeCompatibility.TRUE;
				}
				return TypeCompatibility.FALSE;
			}
			final IRTypeDeclaration superType = current.getSuperType();
			if (superType != null)
			{
				final TypeCompatibility result = isAssignableFrom(self, superType, visited);
				if (result != null)
				{
					return result;
				}
			}
			for (IRTypeDeclaration trait : current.getTraits())
			{
				final TypeCompatibility result = isAssignableFrom(self, trait, visited);
				if (result != null)
				{
					return result;
				}
			}
		}
		return TypeCompatibility.FALSE;
	}

	@Override
	public ServoyDynamicRuntimeType makeImmutable(Map<Object, Object> visited)
	{
		IRTypeDeclaration declaration = getDeclaration();
		if (declaration instanceof ImmutableType< ? > t)
		{
			declaration = (IRTypeDeclaration)t.makeImmutable(visited);
			return new ServoyDynamicRuntimeType(declaration);
		}
		return this;
	}
}
