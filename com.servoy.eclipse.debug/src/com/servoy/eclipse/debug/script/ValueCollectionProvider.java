package com.servoy.eclipse.debug.script;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.IMemberEvaluator;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Member;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

@SuppressWarnings("nls")
public class ValueCollectionProvider implements IMemberEvaluator
{
	private static final Map<IFile, Pair<Long, IValueCollection>> scriptCache = new ConcurrentHashMap<IFile, Pair<Long, IValueCollection>>();

	public IValueCollection valueOf(ITypeInfoContext context, Member member)
	{
		IValueCollection collection = (IValueCollection)member.getAttribute(TypeCreator.VALUECOLLECTION);
		if (collection != null) return collection;
		Form form = (Form)member.getAttribute(TypeCreator.LAZY_VALUECOLLECTION);
		if (form != null)
		{
			String scriptPath = SolutionSerializer.getScriptPath(form, false);
			IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			return getSuperFormContext(context, form, getValueCollection(context, file));
		}
		return null;
	}

	/**
	 * @param context
	 * @param form
	 * @param formCollection
	 */
	private IValueCollection getSuperFormContext(ITypeInfoContext context, Form form, IValueCollection formCollection)
	{
		if (form.getExtendsFormID() > 0)
		{
			FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
			if (fs != null)
			{
				IValueCollection superForms = ValueCollectionFactory.createScopeValueCollection();
				Form superForm = fs.getForm(form.getExtendsFormID());
				while (superForm != null)
				{
					String scriptPath = SolutionSerializer.getScriptPath(superForm, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					ValueCollectionFactory.copyInto(superForms, getValueCollection(context, file));
					superForm = fs.getForm(superForm.getExtendsFormID());
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
			if (context.getModelElement().getResource().getName().endsWith("globals.js"))
			{
				FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
				if (fs != null)
				{
					return getGlobalModulesValueCollection(context, fs, ValueCollectionFactory.createValueCollection());
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
				ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(module.getName());
				IFile file = project.getProject().getFile("globals.js"); //$NON-NLS-1$
				IValueCollection moduleCollection = getValueCollection(context, file);
				if (moduleCollection != null)
				{
					ValueCollectionFactory.copyInto(collection, moduleCollection);
				}
			}
		}
		return collection;
	}

	public static IValueCollection getValueCollection(ITypeInfoContext context, IFile file)
	{
		if (context.getModelElement().getResource().equals(file))
		{
			return null;
		}
		IValueCollection collection = null;
		try
		{
			Pair<Long, IValueCollection> pair = scriptCache.get(file);
			if (pair == null || pair.getLeft().longValue() != file.getModificationStamp())
			{
				collection = ValueCollectionFactory.createValueCollection(file, false);
				scriptCache.put(file, new Pair<Long, IValueCollection>(new Long(file.getModificationStamp()), collection));
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
}
