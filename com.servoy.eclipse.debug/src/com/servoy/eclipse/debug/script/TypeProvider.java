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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeProvider;
import org.eclipse.dltk.javascript.typeinfo.TypeMode;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.ICustomType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class TypeProvider implements ITypeProvider
{
	private final TypeCreator TYPES = new TypeCreator();

	public boolean initialize(ITypeInfoContext context)
	{
		return true;
	}

	public Type getType(ITypeInfoContext context, TypeMode mode, String typeName)
	{
		// use the projectname of the resource, also when the file does not belong to the active solution
		return TYPES.findType(ElementResolver.getProjectName(context), typeName);
	}

	public Set<String> listTypes(ITypeInfoContext context, TypeMode mode, String prefix)
	{
		FlattenedSolution flattenedSolution = ElementResolver.getFlattenedSolution(context);
		if (flattenedSolution == null) return Collections.emptySet();
		Set<String> names = TYPES.getTypeNames(prefix);
		if (prefix != null && prefix.trim().length() != 0)
		{
			String prefixLower = prefix.toLowerCase();
			if (mode == TypeMode.JSDOC && flattenedSolution != null)
			{
				if (prefix.indexOf('.') != -1)
				{
					String[] scopes = prefix.split("\\.");
					if (scopes[0].equals("scopes"))
					{
						Collection<String> scopeNames = flattenedSolution.getScopeNames();
						if (scopes.length > 1 && scopeNames.contains(scopes[1]))
						{
							Iterator<ScriptMethod> scriptMethods = flattenedSolution.getScriptMethods(scopes[1], false);
							for (ScriptMethod method : Utils.iterate(scriptMethods))
							{
								if (method.isConstructor() && !method.isPrivate())
								{
									String name = "scopes." + scopes[1] + '.' + method.getName();
									if (name.toLowerCase().startsWith(prefixLower))
									{
										names.add(name);
									}
								}
							}
						}
						else
						{
							for (String scopeName : scopeNames)
							{
								String name = "scopes." + scopeName;
								if (name.toLowerCase().startsWith(prefixLower))
								{
									names.add(name);
								}
							}
						}
					}
					else if (scopes[0].equals("forms"))
					{
						ArrayList<String> formNames = new ArrayList<String>(64);
						Iterator<Form> forms = flattenedSolution.getForms(false);
						for (Form form : Utils.iterate(forms))
						{
							formNames.add(form.getName());
						}
						if (scopes.length > 1 && formNames.contains(scopes[1]))
						{
							Iterator<ScriptMethod> scriptMethods = flattenedSolution.getForm(scopes[1]).getScriptMethods(false);
							for (ScriptMethod method : Utils.iterate(scriptMethods))
							{
								if (method.isConstructor() && !method.isPrivate())
								{
									String name = "forms." + scopes[1] + '.' + method.getName();
									if (name.toLowerCase().startsWith(prefixLower))
									{
										names.add(name);
									}
								}
							}
						}
						else
						{
							for (String formName : formNames)
							{
								String name = "forms." + formName;
								if (name.toLowerCase().startsWith(prefixLower) &&
									!PersistEncapsulation.isHideInScriptingModuleScope(flattenedSolution.getForm(formName), flattenedSolution))
								{
									names.add(name);
								}
							}
						}
					}
					return names;
				}
				else
				{
					Form currentForm = ElementResolver.getForm(context);
					String currentScope = null;
					if (currentForm == null)
					{
						IResource resource = context.getModelElement().getResource();
						if (resource != null)
						{
							IPath path = resource.getProjectRelativePath();
							if (path.segmentCount() == 1 && path.lastSegment().endsWith(".js"))
							{
								currentScope = path.lastSegment().substring(0, path.lastSegment().length() - 3);
							}
						}
					}
					Iterator<ScriptMethod> scriptMethods = flattenedSolution.getScriptMethods(false);
					while (scriptMethods.hasNext())
					{
						ScriptMethod sm = scriptMethods.next();
						if (sm.isConstructor() && !sm.isPrivate() && !sm.getScopeName().equals(currentScope))
						{
							String name = sm.getName();
							if (name.toLowerCase().startsWith(prefixLower))
							{
								names.add("scopes." + sm.getScopeName() + '.' + sm.getName());
							}
						}
					}
					Iterator<Form> forms = flattenedSolution.getForms(false);
					while (forms.hasNext())
					{
						Form form = forms.next();
						if (currentForm != form && !PersistEncapsulation.isHideInScriptingModuleScope(form, flattenedSolution))
						{
							scriptMethods = form.getScriptMethods(false);
							while (scriptMethods.hasNext())
							{
								ScriptMethod sm = scriptMethods.next();
								if (sm.isConstructor() && !sm.isPrivate())
								{
									String name = sm.getName();
									if (name.toLowerCase().startsWith(prefixLower))
									{
										names.add("forms." + form.getName() + '.' + sm.getName());
									}
								}
							}
						}
					}
				}
			}
			String datasourcePrefix = null;
			if (Record.JS_RECORD.toLowerCase().startsWith(prefixLower))
			{
				names.add(Record.JS_RECORD);
				if (prefixLower.equalsIgnoreCase(Record.JS_RECORD)) datasourcePrefix = Record.JS_RECORD + '<';
			}
			if (FoundSet.JS_FOUNDSET.toLowerCase().startsWith(prefixLower))
			{
				names.add(FoundSet.JS_FOUNDSET);
				if (prefixLower.equalsIgnoreCase(FoundSet.JS_FOUNDSET)) datasourcePrefix = FoundSet.JS_FOUNDSET + '<';
			}
			if (QBSelect.class.getSimpleName().toLowerCase().startsWith(prefixLower))
			{
				names.add(QBSelect.class.getSimpleName());
				if (prefixLower.equalsIgnoreCase(QBSelect.class.getSimpleName())) datasourcePrefix = QBSelect.class.getSimpleName() + '<';
			}
			if (datasourcePrefix != null && mode == TypeMode.JSDOC)
			{
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				String[] serverNames = serverManager.getServerNames(true, true, false, false);
				for (String serverName : serverNames)
				{
					try
					{
						List<String> tableAndViewNames = serverManager.getServer(serverName).getTableAndViewNames(true);
						for (String tableName : tableAndViewNames)
						{
							names.add(datasourcePrefix + "db:/" + serverName + '/' + tableName + '>');
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
			}
			if (TypeCreator.CUSTOM_TYPE.toLowerCase().startsWith(prefixLower))
			{
				WebObjectSpecification[] webComponentSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
				WebObjectSpecification[] webServiceSpecifications = NGUtils.getAllWebServiceSpecificationsThatCanBeAddedToJavaPluginsList(
					WebServiceSpecProvider.getSpecProviderState());
				Collection<WebObjectSpecification> specs = new ArrayList<WebObjectSpecification>();
				Collections.addAll(specs, webComponentSpecifications);
				Collections.addAll(specs, webServiceSpecifications);
				for (WebObjectSpecification webComponentSpecification : specs)
				{
					Map<String, ICustomType< ? >> foundTypes = webComponentSpecification.getDeclaredCustomObjectTypes();
					for (ICustomType< ? > type : foundTypes.values())
					{
						names.add(TypeCreator.CUSTOM_TYPE + '<' + type.getName() + '>');
					}
				}
			}
			if (TypeCreator.RUNTIME_WEB_COMPONENT.toLowerCase().startsWith(prefixLower))
			{
				WebObjectSpecification[] webComponentSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
				for (WebObjectSpecification webComponentSpecification : webComponentSpecifications)
				{
					names.add(TypeCreator.RUNTIME_WEB_COMPONENT + '<' + webComponentSpecification.getName() + '>');
				}
			}
			if ("form".startsWith(prefixLower) || "runtimeform".startsWith(prefixLower))
			{
				names.add("RuntimeForm");
				if (mode == TypeMode.JSDOC && (prefixLower.equals("form") || prefixLower.equals("runtimeform")))
				{
					FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
					Iterator<Form> forms = fs.getForms(false);
					for (Form form : Utils.iterate(forms))
					{
						names.add("RuntimeForm<" + form.getName() + '>');
					}
				}
			}
			if ("continuation".startsWith(prefixLower)) names.add("Continuation");
			if (mode == TypeMode.JSDOC)
			{
				if ("scopes".startsWith(prefix)) names.add("scopes");
				if ("forms".startsWith(prefix)) names.add("forms");
			}

		}
		else
		{
			names.add(Record.JS_RECORD);
			names.add(FoundSet.JS_FOUNDSET);
//			names.add("Form");
			names.add("RuntimeForm");
			names.add("Continuation");
			if (mode == TypeMode.JSDOC)
			{
				names.add("scopes");
				names.add("forms");
			}
		}
		return names;
	}

	/**
	 * Gets the TypeCreator instance for accessing type information with merged documentation.
	 * 
	 * @return the TypeCreator instance
	 */
	public TypeCreator getTypeCreator()
	{
		return TYPES;
	}
}
