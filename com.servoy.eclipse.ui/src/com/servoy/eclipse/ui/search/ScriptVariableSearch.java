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

package com.servoy.eclipse.ui.search;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IField;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.search.IDLTKSearchConstants;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ScopesUtils;

/**
 * An {@link ISearchQuery} implementation for finding relations in frm and js files.
 *
 * @author jcompagner
 * @since 6.0
 */
public class ScriptVariableSearch extends DLTKSearchEngineSearch
{

	private final ScriptVariable variable;
	private final boolean searchInJavaScript;
	private final boolean formScopeOnly;

	public ScriptVariableSearch(ScriptVariable variable)
	{
		this(variable, true, false);
	}

	/**
	 * If search in javascript is false then it won't ask the javascript engine and it assumes that it is not used in a search view/ui
	 * so the results are not updated.
	 *
	 * @param variable
	 * @param searchInJavaScript
	 */
	public ScriptVariableSearch(ScriptVariable variable, boolean searchInJavaScript, boolean formScopeOnly)
	{
		super(searchInJavaScript);
		this.variable = variable;
		this.searchInJavaScript = searchInJavaScript;
		this.formScopeOnly = formScopeOnly;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		final TextSearchRequestor collector = getResultCollector();

		// this is a hack for eclipse, at startup eclipse will suspend the job manager to resume it a bit later when it is started up.
		// problem is that this can be triggered earlier by the script editor that is open when it starts up.
		// so when we see that the job manager is suspended we just resume it right away, so that the JobGroup.join() that the TextSearchEngine calls does work
		// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=527341
		if (Job.getJobManager().isSuspended())
		{
			Job.getJobManager().resume();
		}
		if (variable.getParent() instanceof Solution)
		{
			FileTextSearchScope scope = FileTextSearchScope.newSearchScope(getAllScopesAndActiveResourceProject(), new String[] { "*.frm", "*.rel", "*.dbi" },
				true);
			if (ScriptVariable.GLOBAL_SCOPE.equals(variable.getScopeName()))
			{
				// legacy globals.xx, also matches scopes.globals.xx
				TextSearchEngine.create().search(scope, collector,
					ScriptMethodSearch.createSearchPattern(new String[] { ScriptVariable.GLOBALS_DOT_PREFIX + variable.getName() }), monitor);
			}
			else
			{
				// scopes.scopename.xx
				TextSearchEngine.create().search(scope, collector,
					ScriptMethodSearch.createSearchPattern(new String[] { ScopesUtils.getScopeString(variable) }), monitor);
			}
		}
		else
		{
			IResource[] scopes;
			String[] pattern = null;
			if (formScopeOnly)
			{
				Form form = (Form)variable.getParent();
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getServoyProject(form.getSolution().getName());
				IFile frmFile = servoyProject.getProject().getFile("forms/" + form.getName() + ".frm");
				scopes = new IResource[] { frmFile };
			}
			else
			{
				scopes = getScopes((Solution)variable.getRootObject());
				pattern = new String[] { "*.frm" };
			}
			FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, pattern, true);
			TextSearchEngine.create().search(scope, collector, Pattern.compile("dataProviderID:\"" + variable.getName() + "\""), monitor);
		}
		if (searchInJavaScript)
		{
			String scriptPath = SolutionSerializer.getScriptPath(variable, false);
			IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			ISourceModule sourceModule = DLTKCore.createSourceModuleFrom(file);
			IField variableElement = sourceModule.getField(variable.getName());
			callDLTKSearchEngine(monitor, collector, variableElement, IDLTKSearchConstants.REFERENCES, (Solution)variable.getRootObject());
		}
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to variable '" + variable.getName() + "'";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.search.DLTKSearchEngineSearch#createTextSearchCollector(org.eclipse.search.ui.text.AbstractTextSearchResult)
	 */
	@Override
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		return new VariableSearchResultCollector(searchResult);
	}

	public int getMatchCount()
	{
		return ((AbstractTextSearchResult)getSearchResult()).getMatchCount();
	}

	/**
	 * @author jcompagner
	 *
	 */
	private static final class VariableSearchResultCollector extends TextSearchResultCollector
	{
		/**
		 * @param result
		 */
		private VariableSearchResultCollector(AbstractTextSearchResult result)
		{
			super(result);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.search.TextSearchResultCollector#createFileMatch(org.eclipse.search.core.text.TextSearchMatchAccess, int,
		 * com.servoy.eclipse.ui.search.LineElement)
		 */
		@Override
		protected FileMatch createFileMatch(TextSearchMatchAccess searchMatch, int matchOffset, LineElement lineElement)
		{
			FileMatch fileMatch = super.createFileMatch(searchMatch, matchOffset, lineElement);
			if (searchMatch instanceof SearchMatchAccess)
			{
				fileMatch.setPossibleMatch(((SearchMatchAccess)searchMatch).isPossibleMatch());
			}
			return fileMatch;
		}
	}


}
