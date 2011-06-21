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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

@SuppressWarnings("nls")
public class ValueCollectionProvider implements IMemberEvaluator
{
	private static final Map<IFile, Pair<Long, IValueCollection>> scriptCache = new ConcurrentHashMap<IFile, Pair<Long, IValueCollection>>();


	@SuppressWarnings("restriction")
	public IValueCollection valueOf(ITypeInfoContext context, Element member)
	{
		IValueCollection collection = (IValueCollection)member.getAttribute(TypeCreator.VALUECOLLECTION);
		if (collection != null) return collection;
		Form form = (Form)member.getAttribute(TypeCreator.LAZY_VALUECOLLECTION);
		if (form != null)
		{
			String scriptPath = SolutionSerializer.getScriptPath(form, false);
			IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			collection = ValueCollectionFactory.createValueCollection();
			ValueCollectionFactory.copyInto(collection, getValueCollection(file, true));
			collection = getSuperFormContext(context, form, collection);
			if (member.getAttribute(IReferenceAttributes.SUPER_SCOPE) != null)
			{
				((IValueProvider)collection).getValue().setAttribute(IReferenceAttributes.SUPER_SCOPE, Boolean.TRUE);
			}
			return collection;
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
			FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
			if (fs != null)
			{
				IValueCollection superForms = ValueCollectionFactory.createScopeValueCollection();
				Form superForm = fs.getForm(form.getExtendsID());
				List<IValueCollection> superCollections = new ArrayList<IValueCollection>();
				while (superForm != null)
				{
					String scriptPath = SolutionSerializer.getScriptPath(superForm, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					IValueCollection vc = getValueCollection(file, false);
					if (vc != null)
					{
						Set<String> children = vc.getDirectChildren();
						for (String child : children)
						{
							IValueReference chld = vc.getChild(child);
							chld.setAttribute(IReferenceAttributes.HIDE_ALLOWED, Boolean.TRUE);
							Object attribute = chld.getAttribute(IReferenceAttributes.PARAMETERS);
							if (attribute == null) attribute = chld.getAttribute(IReferenceAttributes.VARIABLE);
							if (attribute instanceof IMember && ((IMember)attribute).isPrivate())
							{
								chld.setAttribute(IReferenceAttributes.PRIVATE, Boolean.TRUE);
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
			if (resource != null)
			{
				if (resource.getName().endsWith("globals.js"))
				{
					FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
					if (fs != null)
					{
						IValueCollection globalsValeuCollection = getGlobalModulesValueCollection(context, fs, ValueCollectionFactory.createValueCollection());
						if (fullGlobalScope.get().booleanValue())
						{
							ValueCollectionFactory.copyInto(globalsValeuCollection, getValueCollection((IFile)resource, true));
						}
						return globalsValeuCollection;
					}
				}
				else
				{
					Form form = TypeCreator.getForm(context);
					if (form != null)
					{
						return getSuperFormContext(context, form, null);
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
	public static IValueCollection getGlobalModulesValueCollection(ITypeInfoContext context, FlattenedSolution fs, IValueCollection collection)
	{
		Solution[] modules = fs.getModules();
		if (modules != null)
		{
			for (Solution module : modules)
			{
				ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(module.getName());
				IFile file = project.getProject().getFile("globals.js"); //$NON-NLS-1$
				IValueCollection moduleCollection = getValueCollection(file, true);
				if (moduleCollection != null)
				{
					ValueCollectionFactory.copyInto(collection, moduleCollection);
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

	public static IValueCollection getValueCollection(IFile file, boolean makeImmutable)
	{
		IValueCollection collection = null;
		try
		{
			Pair<Long, IValueCollection> pair = scriptCache.get(file);
			if (pair == null || pair.getLeft().longValue() != file.getModificationStamp())
			{
				Set<IFile> set = creatingCollection.get();
				if (set.contains(file))
				{
					if (pair != null) return pair.getRight();
					return null;
				}

				set.add(file);
				try
				{
					collection = ValueCollectionFactory.createValueCollection(file, false);
					if (makeImmutable)
					{
						collection = ValueCollectionFactory.makeImmutable(collection);
						if (file.getName().equals("globals.js"))
						{
							scriptCache.put(file, new Pair<Long, IValueCollection>(new Long(file.getModificationStamp()), collection));
						}
					}

				}
				finally
				{
					set.remove(file);
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
		return collection;
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
