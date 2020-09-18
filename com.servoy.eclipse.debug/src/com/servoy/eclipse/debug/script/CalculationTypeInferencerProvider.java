/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencer2;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencerVisitor;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinfo.JSTypeSet;

import com.servoy.eclipse.model.extensions.ICalculationTypeInferencer;
import com.servoy.eclipse.model.extensions.ICalculationTypeInferencerProvider;

/**
 * Used to get the type of a calculation
 * @author emera
 */
public class CalculationTypeInferencerProvider implements ICalculationTypeInferencerProvider
{

	@Override
	public ICalculationTypeInferencer parse(Script script, IFile resource)
	{
		return new CalculationTypeInferencer(script, resource);
	}

	public class CalculationTypeInferencer implements ICalculationTypeInferencer
	{
		private final IValueCollection collection;

		public CalculationTypeInferencer(Script script, IFile resource)
		{
			TypeInferencer2 inferencer = new TypeInferencer2();
			inferencer.setModelElement(DLTKCore.createSourceModuleFrom(resource));
			inferencer.setDoResolve(true);
			inferencer.setVisitFunctionBody(true);
			inferencer.setVisitor(new TypeInferencerVisitor(inferencer, true));
			inferencer.doInferencing(script);
			collection = inferencer.getCollection();
		}

		@Override
		public List<String> getReturnedType(String calculationName)
		{
			IValueReference ref = collection.getChild(calculationName);
			if (ref != null)
			{
				JSTypeSet typeSet = (JSTypeSet)ref.getAttribute("returnTypes");
				if (typeSet != null)
				{
					List<String> result = new ArrayList<String>();
					typeSet.iterator().forEachRemaining(t -> result.add(t.getName()));
					return result;
				}
			}
			return null;
		}
	}
}
