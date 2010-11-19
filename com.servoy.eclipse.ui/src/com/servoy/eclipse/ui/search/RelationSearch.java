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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.codeassist.ISelectionEngine;
import org.eclipse.dltk.codeassist.ISelectionRequestor;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Solution;

/**
 * An {@link ISearchQuery} implementation for finding relations in frm and js files.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class RelationSearch extends AbstractPersistSearch
{

	private final Relation relation;

	public RelationSearch(Relation relation)
	{
		this.relation = relation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("nls")
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		IResource[] scopes = getScopes((Solution)relation.getRootObject());
		TextSearchRequestor collector = getResultCollector();

		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm" }, true);
		TextSearchEngine.create().search(scope, collector, Pattern.compile("\\b" + relation.getName() + "\\b"), monitor);

		scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.js" }, true);
		((RelationSearchResultCollector)collector).setEngine(DLTKLanguageManager.getSelectionEngine(JavaScriptNature.NATURE_ID));
		TextSearchEngine.create().search(scope, collector, Pattern.compile("\\b" + relation.getName() + "\\b"), monitor);

		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.search.AbstractPersistSearch#createTextSearchCollector(org.eclipse.search.ui.text.AbstractTextSearchResult)
	 */
	@Override
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		return new RelationSearchResultCollector(searchResult, relation);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to relation '" + relation.getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static class RelationSearchResultCollector extends TextSearchResultCollector
	{
		private ISelectionEngine engine;
		private final Relation relation;
		private boolean found;

		public RelationSearchResultCollector(AbstractTextSearchResult result, Relation relation)
		{
			super(result);
			this.relation = relation;
		}

		/**
		 * @param engine the engine to set
		 */
		public void setEngine(ISelectionEngine engine)
		{
			this.engine = engine;
			if (engine != null) engine.setRequestor(new ISelectionRequestor()
			{
				public void acceptModelElement(IModelElement element)
				{
				}

				public void acceptForeignElement(Object object)
				{
					if (object instanceof Element)
					{
						// TODO refactor this is the constant of TypeProvider.RESOURCE
						if (RelationSearchResultCollector.this.relation.equals(((Element)object).getAttribute("servoy.RESOURCE"))) //$NON-NLS-1$
						{
							found = true;
						}
					}
				}
			});
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.ui.search.TextSearchResultCollector#createFileMatch(org.eclipse.search.core.text.TextSearchMatchAccess, int,
		 * com.servoy.eclipse.ui.search.LineElement)
		 */
		@Override
		protected FileMatch createFileMatch(TextSearchMatchAccess matchRequestor, int matchOffset, LineElement lineElement)
		{
			FileMatch match = super.createFileMatch(matchRequestor, matchOffset, lineElement);
			if (engine != null)
			{
				found = false;
				engine.select((IModuleSource)DLTKCore.createSourceModuleFrom(matchRequestor.getFile()), matchOffset + 1, matchOffset + 1);
				if (!found)
				{
					match.setPossibleMatch(true);
				}
			}
			return match;
		}
	}
}
