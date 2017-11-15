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

import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.compiler.problem.IValidationStatus;
import org.eclipse.dltk.compiler.problem.ValidationStatus;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.javascript.core.JavaScriptProblems;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinference.ReferenceKind;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IVariable;
import org.eclipse.dltk.javascript.typeinfo.IRMember;
import org.eclipse.dltk.javascript.typeinfo.IRMethod;
import org.eclipse.dltk.javascript.typeinfo.IRProperty;
import org.eclipse.dltk.javascript.typeinfo.IRVariable;
import org.eclipse.dltk.javascript.typeinfo.ReferenceSource;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;
import org.eclipse.dltk.javascript.validation.IMemberValidationEvent;
import org.eclipse.dltk.javascript.validation.IValidatorExtension2;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.ui.search.ScriptVariableSearch;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author jcompagner
 *
 */
public class ServoyScriptValidator implements IValidatorExtension2
{
	private final ReferenceSource context;

	/**
	 * @param rs
	 * @param visitor2
	 */
	public ServoyScriptValidator(ReferenceSource rs)
	{
		this.context = rs;
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
		Form form = getForm();
		if (form != null)
		{
			Visibility visibility = null;
			String name = null;
			IRVariable variable = (IRVariable)reference.getAttribute(IReferenceAttributes.R_VARIABLE);
			if (variable != null)
			{
				visibility = variable.getVisibility();
				name = variable.getName();
			}
			else
			{
				IVariable var = (IVariable)reference.getAttribute(IReferenceAttributes.VARIABLE);
				if (var != null)
				{
					visibility = var.getVisibility();
					name = var.getName();
				}
			}
			if (visibility != null && visibility == Visibility.PRIVATE)
			{
				ScriptVariable scriptVariable = form.getScriptVariable(name);
				if (scriptVariable != null)
				{
					ScriptVariableSearch search = new ScriptVariableSearch(scriptVariable, false, true);
					search.run(null);
					if (search.getMatchCount() > 0)
					{
						return UnusedVariableValidation.FALSE;
					}
				}
			}
			else return UnusedVariableValidation.FALSE;

		}
		return null;
	}

	public IValidationStatus validateAccessibility(Member member)
	{
		Visibility visibility = member.getVisibility();
		if (visibility == Visibility.PRIVATE)
		{
			return generateValidationStatus(member.getName(), member instanceof Method);
		}
		else if (visibility == Visibility.INTERNAL)
		{
			if (member.getAttribute(TypeCreator.HIDDEN_IN_RELATED) != null)
			{
				return new ValidationStatus(JavaScriptProblems.PRIVATE_FUNCTION, "The function " + member.getName() + " is not valid for a related foundset");
			}
			return generateValidationStatusForCurrentSolutionType(member.getName(), member instanceof Method);
		}
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
			if (element instanceof IRMember)
			{
				member = (IRMember)element;
			}
			else if (element instanceof Member)
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
			else if (element instanceof IRProperty)
			{
				name = ((IRProperty)element).getName();
				visibility = ((IRProperty)element).getVisibility();
				if (!((IRProperty)element).isVisible())
				{
					visibility = Visibility.INTERNAL; // Assume that this is hidden because of mobile
				}
				method = false;
			}
		}
		if (member != null)
		{
			visibility = member.getVisibility();
			name = member.getName();
			method = member instanceof IRMethod;
		}
		if (visibility == null && reference != null)
		{
			String refName = reference.getName();
			if ((refName.equals("java") || refName.equals("javax") || refName.equals("Packages")) &&
				ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile())
			{
				name = refName;
				visibility = Visibility.INTERNAL;
			}
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
			if (reference.getParent() != null && reference.getParent().getAttribute(ValueCollectionProvider.SUPER_SCOPE) == null &&
				reference.getParent().getKind() != ReferenceKind.THIS && !reference.getParent().getName().equals("prototype"))
			{
				return generateValidationStatus(name, method);
			}
		}
		else if (visibility == Visibility.INTERNAL)
		{
			return generateValidationStatusForCurrentSolutionType(name, method);
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
			return new ValidationStatus(JavaScriptProblems.PRIVATE_VARIABLE, "The property " + name + " is private");
		}
	}

	private ValidationStatus generateValidationStatusForCurrentSolutionType(String name, boolean isMethod)
	{
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			int solutionType = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getSolutionType();
			String solTypeString = null;
			for (int i = 0; i < SolutionMetaData.solutionTypes.length; i++)
			{
				if (SolutionMetaData.solutionTypes[i] == solutionType)
				{
					solTypeString = SolutionMetaData.solutionTypeNames[i];
					break;
				}
			}
			if (isMethod)
			{
				return new ValidationStatus(JavaScriptProblems.PRIVATE_FUNCTION,
					"The function " + name + "() is not available in solutions of type " + solTypeString);
			}
			else
			{
				return new ValidationStatus(JavaScriptProblems.PRIVATE_VARIABLE,
					"The property " + name + " is not available in solutions of type " + solTypeString);
			}
		}
		return null;
	}

	private Form getForm()
	{
		String formName = SolutionSerializer.getFormNameFromFile((IResource)context.getAdapter(IResource.class));
		if (formName == null)
		{
			ReferenceSource rs = (ReferenceSource)context.getAdapter(ReferenceSource.class);
			if (rs != null && rs.getSourceModule() != null) formName = SolutionSerializer.getFormNameFromFile(rs.getSourceModule().getResource());
		}
		if (formName != null)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				return fs.getForm(formName);
			}
		}
		return null;
	}

	/**
	 * @param context2
	 * @return
	 */
	private FlattenedSolution getFlattenedSolution(ReferenceSource rs)
	{
		IResource resource = rs.getSourceModule().getResource();
		if (resource != null)
		{
			return ElementResolver.getFlattenedSolution(resource.getProject().getName());
		}
		return null;
	}
}
