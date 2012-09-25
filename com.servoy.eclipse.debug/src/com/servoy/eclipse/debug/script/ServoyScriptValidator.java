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

import org.eclipse.dltk.compiler.problem.IValidationStatus;
import org.eclipse.dltk.compiler.problem.ValidationStatus;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.javascript.core.JavaScriptProblems;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinfo.IRMember;
import org.eclipse.dltk.javascript.typeinfo.IRMethod;
import org.eclipse.dltk.javascript.typeinfo.IRVariable;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;
import org.eclipse.dltk.javascript.validation.IMemberValidationEvent;
import org.eclipse.dltk.javascript.validation.IValidatorExtension2;

import com.servoy.eclipse.ui.search.ScriptVariableSearch;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * @author jcompagner
 *
 */
public class ServoyScriptValidator implements IValidatorExtension2
{

	private final ITypeInfoContext context;
	private final ITypeInferencerVisitor visitor;

	/**
	 * @param context
	 * @param visitor
	 */
	public ServoyScriptValidator(ITypeInfoContext context, ITypeInferencerVisitor visitor)
	{
		this.context = context;
		this.visitor = visitor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.validation.IValidatorExtension2#validateTypeExpression(org.eclipse.dltk.javascript.typeinfo.model.JSType)
	 */
	public IValidationStatus validateTypeExpression(JSType type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public IValidationStatus validateAccessibility(Type type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.validation.IValidatorExtension#canInstantiate(org.eclipse.dltk.javascript.typeinfo.model.Type,
	 * org.eclipse.dltk.javascript.typeinference.IValueReference)
	 */
	public IValidationStatus canInstantiate(Type type, IValueReference typeReference)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.validation.IValidatorExtension#canValidateUnusedVariable(org.eclipse.dltk.javascript.typeinference.IValueCollection,
	 * org.eclipse.dltk.javascript.typeinference.IValueReference)
	 */
	public UnusedVariableValidation canValidateUnusedVariable(IValueCollection collection, IValueReference reference)
	{
		Form form = ElementResolver.getForm(context);
		if (form != null)
		{
			IRVariable variable = (IRVariable)reference.getAttribute(IReferenceAttributes.R_VARIABLE);
			if (variable != null)
			{
				if (variable.getVisibility() == Visibility.PRIVATE)
				{
					ScriptVariable scriptVariable = form.getScriptVariable(variable.getName());
					if (scriptVariable != null)
					{
						ScriptVariableSearch search = new ScriptVariableSearch(scriptVariable, false);
						search.run(null);
						if (search.getMatchCount() > 0)
						{
							return UnusedVariableValidation.FALSE;
						}
					}
				}
				else return UnusedVariableValidation.FALSE;
			}

		}
		return null;
	}

	public IValidationStatus validateAccessibility(Member member)
	{
		return null;
	}

	public IValidationStatus validateAccessibility(IMemberValidationEvent event)
	{
		Visibility visibility = null;
		String name = null;
		boolean method = false;
		IRMember member = event.getRMember();
		IValueReference reference = event.getReference();
		if (member == null)
		{
			Object element = reference.getAttribute(IReferenceAttributes.ELEMENT);
			if (element instanceof Member)
			{
				visibility = ((Member)element).getVisibility();
				name = ((Member)element).getName();
				method = element instanceof Method;
			}
			else if (element instanceof Type)
			{
				name = ((Type)element).getName();
				method = false;
				if (!((Type)element).isVisible())
				{
					visibility = Visibility.PRIVATE;
				}
			}
		}
		else
		{
			visibility = member.getVisibility();
			name = member.getName();
			method = member instanceof IRMethod;
		}
		if (visibility == Visibility.PRIVATE)
		{
			// private methods of a super form.. see ValueCollectionProvider.getSuperFormContext
			if (reference.getParent() != null || reference.getAttribute(ValueCollectionProvider.PRIVATE) == Boolean.TRUE)
			{
				return generateValidationStatus(name, method);
			}
		}
		else if (visibility == Visibility.PROTECTED)
		{
			if (reference.getParent() != null && reference.getParent().getAttribute(ValueCollectionProvider.SUPER_SCOPE) == null)
			{
				return generateValidationStatus(name, method);
			}
		}
		return null;
	}

	/**
	 * @param expression
	 * @param member
	 */
	private ValidationStatus generateValidationStatus(String name, boolean isMethod)
	{
		if (isMethod)
		{
			return new ValidationStatus(JavaScriptProblems.PRIVATE_FUNCTION, "The function " + name + "() is private");
		}
		else
		{
			return new ValidationStatus(JavaScriptProblems.PRIVATE_VARIABLE, "The variable " + name + " is private");
		}
	}

}
