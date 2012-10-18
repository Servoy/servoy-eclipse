/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.dltk.javascript.structure.IStructureHandler;
import org.eclipse.dltk.javascript.structure.IStructureVisitor;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceExtensionFactory;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandlerFactory;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ReferenceSource;
import org.eclipse.dltk.javascript.validation.IValidatorExtension;

import com.servoy.eclipse.model.repository.SolutionSerializer;

public class ServoyTypeInferenceFactory implements ITypeInferenceHandlerFactory, ITypeInferenceExtensionFactory
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandlerFactory#create(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext,
	 * org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor)
	 */
	public ITypeInferenceHandler create(ITypeInfoContext context, ITypeInferencerVisitor visitor)
	{
		if (SolutionSerializer.getDataSourceForCalculationJSFile(context.getModelElement().getResource()) != null)
		{
			return new CalculationsTypeInferenceHandler(visitor);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeInferenceExtensionFactory#createExtension(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext,
	 * org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor, java.lang.Class)
	 */
	public Object createExtension(ITypeInfoContext context, ITypeInferencerVisitor visitor, Class< ? > extensionClass)
	{
		if (extensionClass == IValidatorExtension.class)
		{
			return new ServoyScriptValidator(context, visitor);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeInferenceExtensionFactory#createExtension(org.eclipse.core.runtime.IAdaptable, java.lang.Class,
	 * java.lang.Object)
	 */
	public Object createExtension(IAdaptable context, Class< ? > extensionClass, Object visitor)
	{
		ReferenceSource rs = (ReferenceSource)context.getAdapter(ReferenceSource.class);
		if (rs != null && extensionClass == IStructureHandler.class)
		{
			if (SolutionSerializer.getDataSourceForCalculationJSFile(rs.getModelElement().getResource()) != null)
			{
				return new CalculationsTypeStructureHandler(rs, (IStructureVisitor)visitor);
			}
		}
		return null;
	}

}
