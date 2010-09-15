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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.ast.statements.Statement;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.internal.javascript.ti.IValueReference;
import org.eclipse.dltk.internal.javascript.ti.JSMethod;
import org.eclipse.dltk.internal.javascript.validation.JavaScriptValidations;
import org.eclipse.dltk.javascript.ast.ArrayInitializer;
import org.eclipse.dltk.javascript.ast.BooleanLiteral;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.DecimalLiteral;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.JSNode;
import org.eclipse.dltk.javascript.ast.NewExpression;
import org.eclipse.dltk.javascript.ast.ObjectInitializer;
import org.eclipse.dltk.javascript.ast.ObjectInitializerPart;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.typeinfo.IElementResolver;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IParameter;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;

/**
 * Class that resolves names in javascript like application or controller to a {@link Property} with a reference to the {@link Type} of that name.
 * It also lists all possible global names for the current context for the code completion to use.
 * 
 * @author jcompagner
 * @since 6.0
 */
@SuppressWarnings("nls")
public class ElementResolver extends TypeCreator implements IElementResolver
{
	private static final List<String> noneConstantTypes = Arrays.asList(new String[] { "application", "security", "solutionModel", "databaseManager", "controller", "currentcontroller", "i18n", "history", "utils", "foundset", "forms", "elements", "globals" });

	private final Map<String, ITypeNameCreator> typeNameCreators = new HashMap<String, ElementResolver.ITypeNameCreator>();

	public ElementResolver()
	{
		addType("application", JSApplication.class);
		addType("security", JSSecurity.class);
		addType("i18n", JSI18N.class);
		addType("history", HistoryProvider.class);
		addType("utils", JSUtils.class);
		addType("jsunit", JSUnitAssertFunctions.class);
		addType("solutionModel", JSSolutionModel.class);
		addType("databaseManager", JSDatabaseManager.class);
		addType("controller", JSForm.class);
		addType("currentcontroller", JSForm.class);

		addScopeType("forms", new FormsScopeCreator());
		addScopeType("globals", new GlobalScopeCreator());

		typeNameCreators.put("foundset", new FoundsetTypeNameCreator());
		typeNameCreators.put("plugins", new PluginsTypeNameCreator());
		typeNameCreators.put("elements", new ElementsTypeNameCreator());
		typeNameCreators.put("_super", new SuperTypeNameCreator());

	}

	@Override
	protected boolean constantsOnly(String name)
	{
		return !noneConstantTypes.contains(name);
	}

	public Set<String> listGlobals(ITypeInfoContext context, String prefix)
	{
		Set<String> typeNames = getTypeNames(prefix);
		Form form = getForm(context);
		if (form != null)
		{
			typeNames.addAll(typeNameCreators.keySet());
			FlattenedSolution fs = getFlattenedSolution(context);
			Form formToUse = form;
			typeNames.remove("currentcontroller");
			if (form.getExtendsFormID() > 0)
			{
				try
				{
					formToUse = fs.getFlattenedForm(form);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cant get super flattened form for " + form, e);
				}
			}
			else
			{
				typeNames.remove("_super");
			}
// TODO is this needed? should already be done
//			Iterator<ScriptMethod> scriptMethods = formToUse.getScriptMethods(false);
//			while (scriptMethods.hasNext())
//			{
//				ScriptMethod sm = scriptMethods.next();
//			}
//			formToUse.getScriptVariables(false)
// data providers
			try
			{
				Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(formToUse.getTable());
				if (allDataProvidersForTable != null)
				{
					typeNames.addAll(allDataProvidersForTable.keySet());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Cant get dataproviders of " + form, e);
			}

			try
			{
				Iterator<Relation> relations = fs.getRelations(formToUse.getTable(), true, false);
				while (relations.hasNext())
				{
					typeNames.add(relations.next().getName());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Cant get relations of " + form, e);
			}
		}
		else
		{
			// global, remove the few form only things.
			typeNames.remove("controller");
			typeNames.remove("globals");
			typeNames.add("plugins");
			// or calculation???
		}
		return typeNames;
	}

	public Element resolveElement(ITypeInfoContext context, String name)
	{
		Type type = null;
		String typeName = getTypeName(context, name);
		if (typeName != null)
		{
			type = context.getType(typeName);
		}
		else
		{
			type = getType(context, name);
		}
		boolean readOnly = true;
		ImageDescriptor image = null;
		Object resource = null;
		if (type == null)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Relation relation = fs.getRelation(name);
				if (relation != null)
				{
					type = context.getType(FoundSet.JS_FOUNDSET + "<" + relation.getName() + ">");
					image = RELATION_IMAGE;
					resource = relation;
				}
				else
				{
					Form form = getForm(context);
					if (form != null)
					{
						IDataProvider provider = form.getScriptVariable(name);
						if (provider == null)
						{
							try
							{
								Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(form.getTable());
								if (allDataProvidersForTable != null)
								{
									provider = allDataProvidersForTable.get(name);
									image = COLUMN_IMAGE;
									if (provider instanceof AggregateVariable)
									{
										image = COLUMN_AGGR_IMAGE;
									}
									else if (provider instanceof ScriptCalculation)
									{
										image = COLUMN_CALC_IMAGE;
									}
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							image = FORM_VARIABLE_IMAGE;
						}
						if (provider != null)
						{
							readOnly = false;
							type = getDataPRoviderType(context, provider);
							resource = provider;
						}
					}
				}

			}
		}
		else
		{
			image = (ImageDescriptor)type.getAttribute(IMAGE_DESCRIPTOR);
		}

		if (type != null)
		{
			return createProperty(name, readOnly, type, type.getDescription(), image, resource);
		}
		return null;
	}

	private String getTypeName(ITypeInfoContext context, String name)
	{
		ITypeNameCreator nameCreator = typeNameCreators.get(name);
		if (nameCreator != null) return nameCreator.getTypeName(context, name);
		return null;
	}

	/**
	 * @param context
	 * @param type
	 * @param provider
	 * @return
	 */
	private Type getDataPRoviderType(ITypeInfoContext context, IDataProvider provider)
	{
		Type type = null;
		switch (provider.getDataProviderType())
		{
			case IColumnTypes.DATETIME :
				type = context.getType("Date");
				break;
			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :
				type = context.getType("Number");
				break;
			case IColumnTypes.TEXT :
				type = context.getType("String");
				break;
			case IColumnTypes.MEDIA :
				if (provider instanceof ScriptVariable)
				{
					Object object = ((ScriptVariable)provider).getRuntimeProperty(IScriptProvider.INITIALIZER);
					if (object instanceof ObjectInitializer)
					{
						String typeName = provider.getDataProviderID();
						if (typeName.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) typeName = typeName.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
						type = context.getType(typeName);
						EList<Member> members = type.getMembers();
						if (members.size() == 0)
						{
							List<ObjectInitializerPart> initializers = ((ObjectInitializer)object).getInitializers();
							for (ObjectInitializerPart objectInitializer : initializers)
							{
								List childs = objectInitializer.getChilds();
								if (childs.size() == 2) // identifier and value.
								{
									Object identifier = childs.get(0);
									if (identifier instanceof Identifier)
									{
										Object value = childs.get(1);
										Type t = null;
										if (value instanceof DecimalLiteral)
										{
											t = context.getType("Number");
										}
										else if (value instanceof StringLiteral)
										{
											t = context.getType("String");
										}
										else if (value instanceof BooleanLiteral)
										{
											t = context.getType("Boolean");
										}
										else if (value instanceof CallExpression)
										{
											ASTNode callExpression = ((CallExpression)value).getExpression();
											if (callExpression instanceof NewExpression)
											{
												String objectclass = null;
												Expression objectClassExpression = ((NewExpression)callExpression).getObjectClass();
												if (objectClassExpression instanceof Identifier)
												{
													objectclass = ((Identifier)objectClassExpression).getName();
												}
												if ("String".equals(objectclass)) //$NON-NLS-1$
												{
													t = context.getType("String");
												}
												else if ("Date".equals(objectclass)) //$NON-NLS-1$
												{
													t = context.getType("Date");
												}
											}
										}

										members.add(createProperty(((Identifier)identifier).getName(), true, t, null, null));
									}
								}
								//members.add(cre)
							}
						}
					}
					else if (object instanceof ArrayInitializer)
					{
						type = context.getType("Array");
					}
					else if (object instanceof CallExpression)
					{
						ASTNode callExpression = ((CallExpression)object).getExpression();
						if (callExpression instanceof NewExpression)
						{
							String objectclass = null;
							Expression objectClassExpression = ((NewExpression)callExpression).getObjectClass();
							if (objectClassExpression instanceof Identifier)
							{
								objectclass = ((Identifier)objectClassExpression).getName();
							}
							type = context.getType(objectclass);
							EList<Member> members = type.getMembers();
							if (members.size() == 0)
							{
								// find this node.
								ASTNode parent = ((CallExpression)object).getParent();
								while (parent instanceof JSNode && !(parent instanceof Script))
								{
									parent = ((JSNode)parent).getParent();
								}
								if (parent instanceof Script)
								{
									try
									{
										parent.traverse(new ASTVisitor()
										{
											@Override
											public boolean visitGeneral(ASTNode node) throws Exception
											{
												return true;
											}

											@Override
											public boolean visit(org.eclipse.dltk.ast.expressions.Expression s) throws Exception
											{
												return super.visit(s);
											}

											@Override
											public boolean visit(Statement s) throws Exception
											{
												// TODO Auto-generated method stub
												return super.visit(s);
											}
										});
									}
									catch (Exception e)
									{
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}
					}
					else if (object instanceof IValueReference)
					{
						IValueReference functionType = (IValueReference)object;
						String className = functionType.getName();
						type = context.getType(className);
						EList<Member> members = type.getMembers();
						if (members.size() == 0)
						{
							Set<String> directChildren = functionType.getDirectChildren();
							for (String fieldName : directChildren)
							{
								if (fieldName.equals(IValueReference.FUNCTION_OP)) continue;
								IValueReference child = functionType.getChild(fieldName);
								// test if it is a function.
								if (child.hasChild(IValueReference.FUNCTION_OP))
								{
									Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
									method.setName(fieldName);
									method.setType(JavaScriptValidations.typeOf(child));

									JSMethod jsmethod = (JSMethod)child.getAttribute(IReferenceAttributes.PARAMETERS, true);
									if (jsmethod != null && jsmethod.getParameterCount() > 0)
									{
										EList<Parameter> parameters = method.getParameters();
										List<IParameter> jsParameters = jsmethod.getParameters();
										for (IParameter jsParameter : jsParameters)
										{
											Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
											parameter.setKind(ParameterKind.OPTIONAL);
											parameter.setType(jsParameter.getType());
											parameter.setName(jsParameter.getName());
											parameters.add(parameter);
										}
									}
									members.add(method);
								}
								else
								{
									Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
									property.setName(fieldName);
									property.setType(JavaScriptValidations.typeOf(child));
									members.add(property);
								}
							}
						}
					}
				}
				break;
		}
		return type;
	}

	private class FormsScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName("forms");
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, FORMS);

			EList<Member> members = type.getMembers();
			members.add(createProperty(context, "allnames", true, "Array", "All form names as an array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "length", true, "Number", "Number of forms", PROPERTY));

			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Iterator<Form> forms = fs.getForms(false);

				while (forms.hasNext())
				{
					Form form = forms.next();
					members.add(createProperty(context, form.getName(), true, "Form<" + form.getName() + '>',
						"Form based on datasource: " + form.getDataSource(), FORM_IMAGE, form));
				}
			}
			return type;
		}

	}

	private class GlobalScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName("globals");
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, GLOBALS);

			EList<Member> members = type.getMembers();

			members.add(createProperty(context, "allmethods", true, "Array", "Returns all global method names in an Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allvariables", true, "Array", "Returns all global variable names in an Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allrelations", true, "Array", "Returns all global relation names in an Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "currentcontroller", true, "controller", "The current active main forms controller", PROPERTY));

			FlattenedSolution fs = getFlattenedSolution(context);

			if (fs != null)
			{
				Iterator<ScriptVariable> scriptVariables = fs.getScriptVariables(false);
				while (scriptVariables.hasNext())
				{
					ScriptVariable sv = scriptVariables.next();

					members.add(createProperty(sv.getName(), false, getDataPRoviderType(context, sv), getParsedComment(sv.getComment()), GLOBAL_VAR_IMAGE,
						sv.getSerializableRuntimeProperty(IScriptProvider.FILENAME)));
				}

				Iterator<ScriptMethod> scriptMethods = fs.getScriptMethods(false);
				while (scriptMethods.hasNext())
				{
					ScriptMethod sm = scriptMethods.next();
					members.add(createMethod(context, sm, GLOBAL_METHOD_IMAGE, sm.getSerializableRuntimeProperty(IScriptProvider.FILENAME)));
				}
			}
			return type;

		}
	}

	private class ElementsTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				return "Elements<" + form.getName() + '>';
			}
			return "Elements";
		}
	}

	private class PluginsTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			return "Plugins";
		}
	}

	private class FoundsetTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				try
				{
					Table table = form.getTable();
					if (table != null) return FoundSet.JS_FOUNDSET + '<' + table.getServerName() + '.' + table.getName() + '>';
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}

			}
			return FoundSet.JS_FOUNDSET;
		}
	}

	private class SuperTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				if (form.getExtendsFormID() > 0)
				{
					FlattenedSolution fs = getFlattenedSolution(context);
					Form superForm = fs.getForm(form.getExtendsFormID());
					if (superForm != null) return "Super<" + superForm.getName() + '>';
				}
			}
			return "Super";
		}
	}

	public interface ITypeNameCreator
	{
		public String getTypeName(ITypeInfoContext context, String typeName);
	}
}
