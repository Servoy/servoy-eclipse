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

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;
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

	public ScriptVariableSearch(ScriptVariable variable)
	{
		this(variable, true);
	}

	public ScriptVariableSearch(ScriptVariable variable, boolean searchInJavaScript)
	{
		this.variable = variable;
		this.searchInJavaScript = searchInJavaScript;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("nls")
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		IResource[] scopes = getScopes((Solution)variable.getRootObject());
		final TextSearchRequestor collector = getResultCollector();

		if (variable.getParent() instanceof Solution)
		{
			FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm", "*.rel" }, true);
			if (ScriptVariable.GLOBAL_SCOPE.equals(variable.getScopeName()))
			{
				// legacy globals.xx, also matches scopes.globals.xx
				TextSearchEngine.create().search(scope, collector,
					ScriptMethodSearch.createSearchPattern(ScriptVariable.GLOBALS_DOT_PREFIX + variable.getName()), monitor);
			}
			else
			{
				// scopes.scopename.xx
				TextSearchEngine.create().search(scope, collector, ScriptMethodSearch.createSearchPattern(ScopesUtils.getScopeString(variable)), monitor);
			}
		}
		else
		{
			((VariableSearchResultCollector)collector).setFormToMatch((Form)variable.getParent());
			FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm" }, true);
			TextSearchEngine.create().search(scope, collector, Pattern.compile("dataProviderID:\"" + variable.getName() + "\""), monitor);
			((VariableSearchResultCollector)collector).setFormToMatch(null);
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
		return "Searching references to variable '" + variable.getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
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
		private Form form;

		/**
		 * @param result
		 */
		private VariableSearchResultCollector(AbstractTextSearchResult result)
		{
			super(result);
		}

		public void setFormToMatch(Form form)
		{
			this.form = form;
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
			if (form != null)
			{
				Pair<String, String> fileName = SolutionSerializer.getFilePath(form, false);
				IFile file = searchMatch.getFile();
				if (!file.getFullPath().toPortableString().endsWith(fileName.getLeft() + fileName.getRight()))
				{
					return null;
				}
			}
			FileMatch fileMatch = super.createFileMatch(searchMatch, matchOffset, lineElement);
			if (searchMatch instanceof SearchMatchAccess)
			{
				fileMatch.setPossibleMatch(((SearchMatchAccess)searchMatch).isPossibleMatch());
			}
			return fileMatch;
		}
	}


}
