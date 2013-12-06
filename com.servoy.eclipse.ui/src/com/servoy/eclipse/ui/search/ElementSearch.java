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
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;

/**
 * An {@link ISearchQuery} implementation for finding elements js files.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class ElementSearch extends AbstractPersistSearch
{

	private final BaseComponent component;

	public ElementSearch(BaseComponent component)
	{
		this.component = component;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		IResource[] scopes = getScopes((Solution)component.getRootObject());
		TextSearchRequestor collector = getResultCollector();

		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.js" }, true);
		((ElementSearchResultCollector)collector).setEngine(DLTKLanguageManager.getSelectionEngine(JavaScriptNature.NATURE_ID));
		TextSearchEngine.create().search(scope, collector, Pattern.compile("\\b" + component.getName() + "\\b"), monitor);

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
		return new ElementSearchResultCollector(searchResult, component);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to element '" + component.getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static class ElementSearchResultCollector extends TextSearchResultCollector
	{
		private ISelectionEngine engine;
		private final BaseComponent component;
		private boolean found;

		public ElementSearchResultCollector(AbstractTextSearchResult result, BaseComponent component)
		{
			super(result);
			this.component = component;
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

				public void acceptElement(Object element, ISourceRange range)
				{
				}

				public void acceptForeignElement(Object object)
				{
					if (object instanceof Property)
					{
						Type declaringType = ((Property)object).getDeclaringType();
						if (declaringType != null)
						{
							Form form = (Form)ElementSearchResultCollector.this.component.getParent();
							String typeName = declaringType.getName();
							if (typeName.startsWith("Elements<"))
							{
								String formName = typeName.substring("Elements<".length(), typeName.length() - 1);
								if (form.getName().equals(formName))
								{
									found = true;
								}
								else
								{
									Form frm = ServoyModelFinder.getServoyModel().getFlattenedSolution().getForm(formName);
									while (frm.getExtendsForm() != null)
									{
										frm = frm.getExtendsForm();
										if (form.getName().equals(frm.getName()))
										{
											found = true;
											break;
										}
									}
								}
							}
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
			FileMatch match = null;
			if (engine != null)
			{
				found = false;
				engine.select((IModuleSource)DLTKCore.createSourceModuleFrom(matchRequestor.getFile()), matchOffset + 1, matchOffset + 1);
				if (found)
				{
					match = super.createFileMatch(matchRequestor, matchOffset, lineElement);
				}
			}
			return match;
		}
	}
}
