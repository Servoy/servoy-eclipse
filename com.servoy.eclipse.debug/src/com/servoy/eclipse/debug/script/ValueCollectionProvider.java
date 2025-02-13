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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.internal.javascript.ti.IValueProvider;
import org.eclipse.dltk.internal.javascript.ti.JSVariable;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.IMemberEvaluator;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IMember;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.RecordType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Pair;

public class ValueCollectionProvider implements IMemberEvaluator
{
	public static final String PRIVATE = "PRIVATE";
	public static final String SUPER_SCOPE = "SUPER_SCOPE";

	private static final Map<IFile, ValueCollectionCacheItem> scriptCache = new ConcurrentHashMap<>();

	private static final ThreadLocal<Deque<Set<IFile>>> depedencyStack = new ThreadLocal<Deque<Set<IFile>>>()
	{
		@Override
		protected Deque<Set<IFile>> initialValue()
		{
			return new ArrayDeque<Set<IFile>>();
		}
	};

	public static void clear()
	{
		scriptCache.clear();
	}

	@SuppressWarnings("restriction")
	public IValueCollection valueOf(ITypeInfoContext context, Element member)
	{
		Object attribute = member.getAttribute(TypeCreator.LAZY_VALUECOLLECTION);
		if (attribute instanceof Form)
		{
			Form form = (Form)attribute;
			String scriptPath = SolutionSerializer.getScriptPath(form, false);
			IValueCollection valueCollection = null;
			IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			if (file.exists())
			{
				valueCollection = getValueCollection(file);
			}
			if (valueCollection == null && form.getExtendsID() > 0)
			{
				valueCollection = ValueCollectionFactory.createValueCollection();
			}
			if (valueCollection != null)
			{
				IValueCollection collection = ValueCollectionFactory.createValueCollection();
				ValueCollectionFactory.copyInto(collection, valueCollection);
				collection = getSuperFormContext(context, form, collection);
				if (member.getAttribute(SUPER_SCOPE) != null)
				{
					((IValueProvider)collection).getValue().setAttribute(SUPER_SCOPE, Boolean.TRUE);
				}
				return ValueCollectionFactory.shallowCloneValueCollection(collection);
			}
			return null;
		}

		String typeName = null;
		if (member instanceof Member && ((Member)member).getType() != null)
		{
			typeName = ((Member)member).getType().getName();
		}
		else if (member instanceof Type)
		{
			typeName = member.getName();
		}
		if (typeName != null)
		{
			if (typeName.startsWith(FoundSet.JS_FOUNDSET + '<') && typeName.endsWith(">"))
			{
				FlattenedSolution editingFlattenedSolution = ElementResolver.getFlattenedSolution(context);
				if (editingFlattenedSolution != null)
				{
					String config = typeName.substring(FoundSet.JS_FOUNDSET.length() + 1, typeName.length() - 1);
					// config is either a dataSource or a relation name

					String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(config);
					if (dbServernameTablename == null) dbServernameTablename = DataSourceUtils.getMemServernameTablename(config);
					String dataSource = null;
					if (dbServernameTablename == null)
					{
						// try relation
						Relation relation = editingFlattenedSolution.getRelation(config);
						if (relation != null)
						{
							dataSource = relation.getForeignDataSource();
						}
					}
					else
					{
						// datasource
						dataSource = config;
					}

					if (dataSource != null)
					{
						Iterator<TableNode> tableNodes = editingFlattenedSolution.getTableNodes(dataSource);
						IValueCollection valueCollection = ValueCollectionFactory.createValueCollection();
						while (tableNodes.hasNext())
						{
							TableNode tableNode = tableNodes.next();
							IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getScriptPath(tableNode, false)));
							if (file.exists())
							{
								ValueCollectionFactory.copyInto(valueCollection, getValueCollection(file));
							}
						}
						return ValueCollectionFactory.shallowCloneValueCollection(valueCollection);
					}
				}
			}

			else if (typeName.startsWith("Scope<") && typeName.endsWith(">"))
			{
				// Scope<solutionName/scopeName>
				FlattenedSolution editingFlattenedSolution = ElementResolver.getFlattenedSolution(context);
				if (editingFlattenedSolution != null)
				{
					String config = typeName.substring("Scope<".length(), typeName.length() - 1);
					String[] split = config.split("/");
					String solutionName = split[0];
					String scopeName = null;
					// this is when only Scope<scopeName> is given
					if (split.length == 1)
					{
						scopeName = solutionName;
						for (Pair<String, IRootObject> scope : editingFlattenedSolution.getScopes())
						{
							if (scope.getLeft().equals(solutionName))
							{
								solutionName = scope.getRight().getName();
								break;
							}
						}
					}
					else
					{
						scopeName = split[1];
					}

					if (editingFlattenedSolution.getMainSolutionMetaData().getName().equals(solutionName) || editingFlattenedSolution.hasModule(solutionName))
					{
						ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
						if (project != null)
						{
							String fileName = scopeName + SolutionSerializer.JS_FILE_EXTENSION;
							IFile file = project.getProject().getFile(fileName);
							IValueCollection globalsValueCollection = null;
							if (file.exists()) globalsValueCollection = ValueCollectionProvider.getValueCollection(file);

							if (globalsValueCollection == null && !fileName.equals(SolutionSerializer.GLOBALS_FILE))
							{
								return null;
							}

							IValueCollection collection = ValueCollectionFactory.createScopeValueCollection();
							if (globalsValueCollection != null)
							{
								ValueCollectionFactory.copyInto(collection, globalsValueCollection);
							}

							// Currently only the old globals scope is merged with entries from modules
							if (ScriptVariable.GLOBAL_SCOPE.equals(scopeName))
							{
								ValueCollectionProvider.getGlobalModulesValueCollection(editingFlattenedSolution, fileName, collection);
							}
							// scopes other than globals are not merged
							return ValueCollectionFactory.shallowCloneValueCollection(collection);
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param context
	 * @param form
	 * @param formCollection
	 */
	@SuppressWarnings("restriction")
	private IValueCollection getSuperFormContext(ITypeInfoContext context, Form form, IValueCollection formCollection)
	{
		if (form.getExtendsID() > 0)
		{
			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
			if (fs != null)
			{
				Form superForm = fs.getForm(form.getExtendsID());

				IValueCollection vc = null;
				if (superForm != null)
				{
					String scriptPath = SolutionSerializer.getScriptPath(superForm, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					while (!file.exists())
					{
						// super form without a js file.
						superForm = fs.getForm(superForm.getExtendsID());
						if (superForm == null) return formCollection;
						scriptPath = SolutionSerializer.getScriptPath(superForm, false);
						file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					}
					Deque<Set<IFile>> stack = depedencyStack.get();
					Set<IFile> depedencies = stack.peek();

					ValueCollectionCacheItem fromCache = getFromScriptCache(file);
					if (fromCache == null)
					{
						getValueCollection(file);
						fromCache = getFromScriptCache(file);
					}
					if (fromCache != null)
					{
						if (depedencies != null)
						{
							depedencies.addAll(fromCache.files());
						}
						IValueCollection collection = fromCache.get();
						if (collection != null)
						{
							vc = ValueCollectionFactory.createScopeValueCollection();
							((IValueProvider)vc).getValue().addValue(((IValueProvider)collection).getValue());
						}
						if (vc != null)
						{
							Set<String> children = vc.getDirectChildren();
							for (String child : children)
							{
								IValueReference chld = vc.getChild(child);
								// check if this type is a type def that can be used in a subform.
								// copy the record type over to the current context
								Object variable = chld.getAttribute(IReferenceAttributes.VARIABLE);
								if (variable instanceof JSVariable && ((JSVariable)variable).getTypeDef() instanceof RecordType)
								{
									RecordType type = (RecordType)((JSVariable)variable).getTypeDef();
									context.registerRecordType(type);
								}
								chld.setAttribute(IReferenceAttributes.HIDE_ALLOWED, Boolean.TRUE);
								// don't set the child to private if the form itself did also implement it.
								if (form.getScriptMethod(child) == null && form.getScriptVariable(child) == null)
								{
									Object attribute = chld.getAttribute(IReferenceAttributes.METHOD);
									if (attribute == null) attribute = chld.getAttribute(IReferenceAttributes.VARIABLE);
									if (attribute instanceof IMember && ((IMember)attribute).getVisibility() == Visibility.PRIVATE)
									{
										chld.setAttribute(PRIVATE, Boolean.TRUE);
									}
								}
							}
							if (formCollection != null) ValueCollectionFactory.copyInto(vc, formCollection);
							return vc;
						}
					}
				}
			}
		}
		return formCollection;
	}

	public Collection<IFile> getDependencies(ISourceModule sourceModule)
	{
		IResource resource = sourceModule.getResource();
		ValueCollectionCacheItem sr = scriptCache.get(resource);
		if ((sr == null || sr.get() == null) && resource instanceof IFile && resource.getName().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
		{
			// quicky create it.
			getValueCollection((IFile)resource);
			sr = scriptCache.get(resource);
		}
		if (sr != null && sr.files() != null)
		{
			Set<IFile> files = sr.files();
			// this includes its own so we need to strip that.
			Set<IFile> collect = files.stream().filter(file -> !file.equals(resource)).collect(Collectors.toSet());
			return collect;
		}
		return Collections.emptyList();
	}

	public IValueCollection getTopValueCollection(ITypeInfoContext context)
	{
		if (context.getModelElement() != null)
		{
			IFile resource = (IFile)context.getModelElement().getResource();
			if (resource != null && resource.getName().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
			{
				// javascript file
				FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
				if (fs != null)
				{
					IPath path = resource.getProjectRelativePath();
					if (path.segmentCount() == 1)
					{
						if (path.segment(0).equals(SolutionSerializer.GLOBALS_FILE))
						{
							// globals scope
							IValueCollection globalsValueCollection = getGlobalModulesValueCollection(fs, path.segment(0),
								ValueCollectionFactory.createValueCollection());
							if (fullGlobalScope.get().booleanValue())
							{
								ValueCollectionFactory.copyInto(globalsValueCollection, getValueCollection(resource));
							}
							return globalsValueCollection;
						}
						return null;
					}

					String formName = SolutionSerializer.getFormNameFromFile(resource);
					if (formName != null)
					{
						// forms/formname.js
						Form form = fs.getForm(formName);
						if (form == null)
						{
							return null;
						}
						// superform
						return getSuperFormContext(context, form, null);
					}

					String[] serverTablename = SolutionSerializer.getDataSourceForCalculationJSFile(resource);
					boolean isCalc = serverTablename != null;
					if (!isCalc)
					{
						serverTablename = SolutionSerializer.getDataSourceForFoundsetJSFile(resource);
					}
					if (serverTablename != null)
					{
						return getTableNodeModulesValueCollection(fs, serverTablename[0], serverTablename[1], isCalc,
							ValueCollectionFactory.createValueCollection());
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param context
	 * @param fs
	 * @param collection
	 */
	public static IValueCollection getGlobalModulesValueCollection(FlattenedSolution fs, String filename, IValueCollection collection)
	{
		Solution[] modules = fs.getModules();
		if (modules != null)
		{
			// don't resolve all the globals scopes when filling one.
			for (Solution module : modules)
			{
				ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(module.getName());
				IFile file = project.getProject().getFile(filename);
				if (file.exists())
				{
					ValueCollectionFactory.copyInto(collection, getValueCollection(file));
				}
			}
		}
		return collection;
	}

	public static IValueCollection getTableNodeModulesValueCollection(FlattenedSolution fs, String serverName, String tableName, boolean calcs,
		IValueCollection collection)
	{
		Solution[] modules = fs.getModules();
		if (modules != null)
		{
			for (Solution module : modules)
			{
				ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(module.getName());
				if (project != null && project.getProject() != null)
				{
					IFolder folder = project.getProject().getFolder(SolutionSerializer.DATASOURCES_DIR_NAME);
					if (folder.exists())
					{
						IFolder serverFolder = folder.getFolder(serverName);
						if (serverFolder.exists())
						{
							ValueCollectionFactory.copyInto(collection, getValueCollection(
								serverFolder.getFile(tableName + (calcs ? SolutionSerializer.CALCULATIONS_POSTFIX : SolutionSerializer.FOUNDSET_POSTFIX))));
						}
					}
				}
			}
		}
		return collection;
	}

	private static IValueCollection getValueCollection(IFile file)
	{
		ValueCollectionCacheItem item = null;
		try
		{
			Deque<Set<IFile>> stack = depedencyStack.get();
			item = getFromScriptCache(file);
			if (item == null && stack.size() > 0)
			{
				if (stack.stream().anyMatch(set -> set.contains(file)))
				{
					// this is a circulair references from the beginning (very likely getTopCollection()), can't be resolved.
					return null;
				}
			}
			Set<IFile> current = stack.peek();
			if (current != null)
			{
				current.add(file);
			}
			if (item == null)
			{
				// its starting, setup the stack for generating the new Set of files.
				HashSet<IFile> depedencies = new HashSet<>();
				depedencies.add(file);
				stack.push(depedencies);

				IValueCollection collection = ValueCollectionFactory.createValueCollection(file, true, false, (fl, col) -> {
					// put in cache for recursion.
					scriptCache.put(fl, new ValueCollectionCacheItem(depedencies, col));
					return !file.getName().equalsIgnoreCase("globals.js");
				});
				stack.pop();
				// we need to do this again because the depedencies did change and ValueCollectionCacheItem needs to recalculate the current timestamp
				scriptCache.put(file, new ValueCollectionCacheItem(depedencies, collection));
				return collection;
			}
			else
			{
				return item.get();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return ValueCollectionFactory.createScopeValueCollection();
	}

	/**
	 * @param resource
	 * @return
	 */
	private static ValueCollectionCacheItem getFromScriptCache(IResource resource)
	{
		ValueCollectionCacheItem sr = scriptCache.get(resource);
		if (sr != null && sr.get() == null)
		{
			// if the cache is found but not valid anymore, the we need to also flush all depedencies, the once that uses this file.
			scriptCache.values().removeIf(item -> item.files().contains(resource));
			scriptCache.remove(resource);
			sr = null;
		}
		return sr;
	}

	private static final ThreadLocal<Boolean> fullGlobalScope = new ThreadLocal<Boolean>()
	{
		@Override
		protected Boolean initialValue()
		{
			return Boolean.FALSE;
		}
	};

	/**
	 * @param b
	 */
	public static void setGenerateFullGlobalCollection(Boolean b)
	{
		fullGlobalScope.set(b);
	}

	public static boolean getGenerateFullGlobalCollection()
	{
		return fullGlobalScope.get().booleanValue();
	}
}
