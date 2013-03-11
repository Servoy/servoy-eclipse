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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.internal.javascript.ti.IValueProvider;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.IMemberEvaluator;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IMember;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
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
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class ValueCollectionProvider implements IMemberEvaluator
{
	public static final String PRIVATE = "PRIVATE";
	public static final String SUPER_SCOPE = "SUPER_SCOPE";

	private static final int MAX_SCRIPT_CACHE_SIZE = Utils.getAsInteger(System.getProperty("servoy.script.cache.size", "300"));

	private static final Map<IFile, SoftReference<Pair<Long, IValueCollection>>> scriptCache = new ConcurrentHashMap<IFile, SoftReference<Pair<Long, IValueCollection>>>();
	private static final Map<IFile, SoftReference<Pair<Long, IValueCollection>>> globalScriptCache = new ConcurrentHashMap<IFile, SoftReference<Pair<Long, IValueCollection>>>();

	public static void clear()
	{
		scriptCache.clear();
		globalScriptCache.clear();
	}

	@SuppressWarnings("restriction")
	public IValueCollection valueOf(ITypeInfoContext context, Element member)
	{
		Object attribute = member.getAttribute(TypeCreator.LAZY_VALUECOLLECTION);
		if (attribute instanceof Form)
		{
			Form form = (Form)attribute;
			String scriptPath = SolutionSerializer.getScriptPath(form, false);
			IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			IValueCollection valueCollection = getValueCollection(file);
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
				return collection;
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
							ValueCollectionFactory.copyInto(valueCollection, getValueCollection(file));
						}
						return valueCollection;
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
					if (split.length == 2)
					{
						String solutionName = split[0];
						String scopeName = split[1];

						if (editingFlattenedSolution.getMainSolutionMetaData().getName().equals(solutionName) ||
							editingFlattenedSolution.hasModule(solutionName))
						{
							ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
							if (project != null)
							{
								String fileName = scopeName + SolutionSerializer.JS_FILE_EXTENSION;
								IFile file = project.getProject().getFile(fileName);
								IValueCollection globalsValueCollection = ValueCollectionProvider.getValueCollection(file);

								if (globalsValueCollection == null)
								{
									return null;
								}

								IValueCollection collection = ValueCollectionFactory.createScopeValueCollection();
								ValueCollectionFactory.copyInto(collection, globalsValueCollection);

								// Currently only the old globals scope is merged with entries from modules
								if (ScriptVariable.GLOBAL_SCOPE.equals(scopeName))
								{
									ValueCollectionProvider.getGlobalModulesValueCollection(editingFlattenedSolution, fileName, collection);
								}
								// scopes other than globals are not merged
								return collection;
							}
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
				IValueCollection superForms = null;
				Form superForm = fs.getForm(form.getExtendsID());

				superForms = ValueCollectionFactory.createScopeValueCollection();
				List<IValueCollection> superCollections = new ArrayList<IValueCollection>();
				while (superForm != null)
				{
					String scriptPath = SolutionSerializer.getScriptPath(superForm, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					IValueCollection vc = null;
					IValueCollection collection = getValueCollection(file);
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
						superCollections.add(vc);
					}
					superForm = fs.getForm(superForm.getExtendsID());
				}
				for (int i = superCollections.size(); --i >= 0;)
				{
					ValueCollectionFactory.copyInto(superForms, ValueCollectionFactory.makeImmutable(superCollections.get(i)));
				}
				if (formCollection != null) ValueCollectionFactory.copyInto(superForms, formCollection);
				return superForms;
			}
		}
		return formCollection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IElementResolver#getTopValueCollection(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext)
	 */
	public IValueCollection getTopValueCollection(ITypeInfoContext context)
	{
		if (context.getModelElement() != null)
		{
			IResource resource = context.getModelElement().getResource();
			if (resource != null && resource.getName().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
			{
				Pair<Long, IValueCollection> pair = getFromScriptCache(resource);
				removeFromScriptCache(resource, pair);
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
								ValueCollectionFactory.copyInto(globalsValueCollection, getValueCollection((IFile)resource));
							}
							return globalsValueCollection;
						}
						return null;
					}

					String formName = SolutionSerializer.getFormNameForJSFile(resource);
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
			Boolean resolving = resolve.get();
			resolve.set(Boolean.TRUE);
			try
			{
				for (Solution module : modules)
				{
					ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(module.getName());
					IFile file = project.getProject().getFile(filename);
					IValueCollection moduleCollection = getValueCollection(file);
					if (moduleCollection != null)
					{
						ValueCollectionFactory.copyInto(collection, moduleCollection);
					}
				}
			}
			finally
			{
				if (resolving != null) resolve.set(resolving);
				resolve.remove();
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
							IValueCollection moduleCollection = getValueCollection(serverFolder.getFile(tableName +
								(calcs ? SolutionSerializer.CALCULATIONS_POSTFIX : SolutionSerializer.FOUNDSET_POSTFIX)));
							if (moduleCollection != null)
							{
								ValueCollectionFactory.copyInto(collection, moduleCollection);
							}
						}
					}
				}
			}
		}
		return collection;
	}

	private static final ThreadLocal<Set<IFile>> creatingCollection = new ThreadLocal<Set<IFile>>()
	{
		@Override
		protected java.util.Set<IFile> initialValue()
		{
			return new HashSet<IFile>();
		}
	};

	private static final ThreadLocal<Boolean> resolve = new ThreadLocal<Boolean>();

	public static IValueCollection getValueCollection(IFile file)
	{
		IValueCollection collection = null;
		synchronized (scriptCache)
		{
			try
			{
				Pair<Long, IValueCollection> pair = getFromScriptCache(file);
				if (pair == null || pair.getLeft().longValue() != file.getModificationStamp())
				{
					Set<IFile> set = creatingCollection.get();
					if (set.contains(file))
					{
						if (pair != null) return pair.getRight();
						return null;
					}
					removeFromScriptCache(file, pair);
					boolean globalsFile = file.getName().equals(SolutionSerializer.GLOBALS_FILE);
					if (!globalsFile)
					{
						// if the current thread set size is 0 (first request, so not in recursion)
						if (set.size() == 0)
						{
							// and the scriptCache size is bigger then the default 300 or the system property
							if (scriptCache.size() > MAX_SCRIPT_CACHE_SIZE)
							{
								// clear the cache to help the garbage collector.
								scriptCache.clear();
							}
						}
					}
					set.add(file);
					boolean doResolve = false;
					Boolean resolving = resolve.get();
					if (resolving == null)
					{
						doResolve = true;
						resolve.set(Boolean.TRUE);
					}
					try
					{
						collection = ValueCollectionFactory.createValueCollection(file, doResolve);
						collection = ValueCollectionFactory.makeImmutable(collection);
						if (globalsFile)
						{
							globalScriptCache.put(file,
								new SoftReference<Pair<Long, IValueCollection>>(new Pair<Long, IValueCollection>(new Long(file.getModificationStamp()),
									collection)));
						}
						else
						{
							scriptCache.put(file,
								new SoftReference<Pair<Long, IValueCollection>>(new Pair<Long, IValueCollection>(new Long(file.getModificationStamp()),
									collection)));
						}
					}
					finally
					{
						set.remove(file);
						if (doResolve) resolve.remove();
					}
				}
				else
				{
					collection = pair.getRight();
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}

		return collection;
	}

	/**
	 * @param resource
	 * @return
	 */
	private static Pair<Long, IValueCollection> getFromScriptCache(IResource resource)
	{
		SoftReference<Pair<Long, IValueCollection>> sr = null;
		if (resource.getName().equals(SolutionSerializer.GLOBALS_FILE))
		{
			sr = globalScriptCache.get(resource);
		}
		else
		{
			sr = scriptCache.get(resource);
		}
		return sr != null ? sr.get() : null;
	}

	/**
	 * @param file
	 * @param pair
	 */
	private static void removeFromScriptCache(IResource file, Pair<Long, IValueCollection> pair)
	{
		if (pair != null && pair.getLeft().longValue() != file.getModificationStamp())
		{
			if (file.getName().equals(SolutionSerializer.GLOBALS_FILE))
			{
				globalScriptCache.remove(file);
			}
			else
			{
				scriptCache.remove(file);
			}
		}
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
