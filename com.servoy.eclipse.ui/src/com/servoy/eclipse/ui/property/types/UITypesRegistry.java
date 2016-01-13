/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.eclipse.ui.property.types;

import java.util.HashMap;
import java.util.Map;

import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IAdjustablePropertyType;
import org.sablo.specification.property.IPropertyType;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;

/**
 * Provides custom behavior in developer (properties view, ...) for sablo/ng types.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class UITypesRegistry
{

	private static Map<String, ITypePropertyDescriptorFactory> typePropertyDescriptorFactories = new HashMap<>();

	static
	{
		addTypePropertyDescriptorFactory(CustomJSONArrayType.TYPE_NAME, new CustomArrayTypePropertyDescriptorFactory());
		addTypePropertyDescriptorFactory(CustomJSONObjectType.TYPE_NAME, new CustomObjectTypePropertyDescriptorFactory());
		addTypePropertyDescriptorFactory(FoundsetPropertyType.TYPE_NAME, new FoundsetPropertyTypePropertyDescriptorFactory());
	}

	/**
	 * Register a new factory that provides properties view look and feel for a specific sablo/ng type.
	 * @param typeName the name of the type. For {@link IAdjustablePropertyType} instances, give the {@link IAdjustablePropertyType#getGenericName()} here.
	 * @param factory the factory to be registered.
	 */
	public static void addTypePropertyDescriptorFactory(String typeName, ITypePropertyDescriptorFactory factory)
	{
		ITypePropertyDescriptorFactory previous = typePropertyDescriptorFactories.put(typeName, factory);
		if (previous != null)
		{
			ServoyLog.logInfo("there was already a type property descripitor factory for typename " + typeName + ": " + previous + " replaced by: " + factory);
		}
	}

	/**
	 * Returns a property descriptor factory for a specific sablo/ng type if available.
	 * @param typeName the name of the type. For {@link IAdjustablePropertyType} instances, give the {@link IAdjustablePropertyType#getGenericName()} here.
	 * @return the factory; null if no such factory is registered for the given type name.
	 */
	public static ITypePropertyDescriptorFactory getTypePropertyDescriptorFactory(String typeName)
	{
		return typePropertyDescriptorFactories.get(typeName);
	}

	/**
	 * Returns a property descriptor factory for a specific sablo/ng type if available.
	 * @param type the type to search for. In case of {@link IAdjustablePropertyType} instances, it will return the factory registered to it's {@link IAdjustablePropertyType#getGenericName()}.
	 * @return the factory; null if no such factory is registered for the given type.
	 */
	public static ITypePropertyDescriptorFactory getTypePropertyDescriptorFactory(IPropertyType< ? > type)
	{
		if (type instanceof IAdjustablePropertyType) return getTypePropertyDescriptorFactory(((IAdjustablePropertyType< ? >)type).getGenericName());
		else return getTypePropertyDescriptorFactory(type.getName());
	}

}
