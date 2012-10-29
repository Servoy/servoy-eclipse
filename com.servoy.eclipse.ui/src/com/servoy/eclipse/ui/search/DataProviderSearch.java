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

package com.servoy.eclipse.ui.search;

import java.util.HashSet;
import java.util.Set;
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
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Debug;

/**
 * An {@link ISearchQuery} implementation for finding dataproviders (columns) in dbi, frm, rel, val, js files.
 * 
 * @author acostache
 */
public class DataProviderSearch extends DLTKSearchEngineSearch
{
	private final IColumn dataprovider;
	private FlattenedSolution flattenedSolution = null;
	private Set<String> relationReferences = null;
	private Set<String> valueListReferences = null;
	private Set<String> formReferences = null;
	private StringBuilder regexStrPattern = null;

	public DataProviderSearch(IColumn dataprovider)
	{
		this.dataprovider = dataprovider;
		flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();

		relationReferences = getRelationRefferences();
		valueListReferences = getValueListRefferences();
		formReferences = getFormRefferences();

		regexStrPattern = new StringBuilder();
		for (String rel : relationReferences)
		{
			regexStrPattern.append("(dataProviderID\\s*\\:\\s*" + "[\"']" + rel + "\\." + dataprovider.getDataProviderID() + ")|");
		}
		regexStrPattern.append("(dataProviderID\\s*\\:\\s*" + "[\"']" + dataprovider.getDataProviderID() + ")|");
		regexStrPattern.append("(\\b" + dataprovider.getDataProviderID() + "\\b)");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject == null) return Status.OK_STATUS;
		IResource[] scopes = getScopes(activeProject.getSolution());
		TextSearchRequestor collector = getResultCollector();

		//search servoy  resources
		FileTextSearchScope servoyResourceScope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.val", "*.frm", "*.rel" }, true);
		TextSearchEngine.create().search(servoyResourceScope, collector, Pattern.compile(regexStrPattern.toString()), monitor);

		//search js files
		//((DataProviderSearchCollector)collector).setEngine(DLTKLanguageManager.getSelectionEngine(JavaScriptNature.NATURE_ID));
		//FileTextSearchScope scriptScope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.js" }, true); //$NON-NLS-1$ 
		//TextSearchEngine.create().search(scriptScope, collector, Pattern.compile("\\b" + dataprovider.getName() + "\\b"), monitor); //$NON-NLS-1$ //$NON-NLS-2$

		return Status.OK_STATUS;
	}

	@Override
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		return new DataProviderSearchCollector(searchResult, dataprovider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to dataprovider '" + dataprovider.getName() + "'"; //$NON-NLS-1$//$NON-NLS-2$
	}

	private Set<String> getRelationRefferences()
	{
		Set<String> relRef = new HashSet<String>();
		if (flattenedSolution != null)
		{

			try
			{
				Table table = dataprovider.getTable();
				String dataproviderId = dataprovider.getDataProviderID();
				// collect in the 2 lists relations that contain dataproviderid as primary and relations that contain dataproviderID as foreign
				for (Relation rel : com.servoy.j2db.util.Utils.iterate(flattenedSolution.getSolution().getRelations(table, true, false)))
				{
					// collect primary
					// primary dataprovider is a column table
					if (table.getDataSource().equals(rel.getPrimaryDataSource()))
					{
						for (IDataProvider dataProvider : rel.getPrimaryDataProviders(flattenedSolution))
						{
							if (dataProvider.getDataProviderID().equals(dataproviderId))
							{
								relRef.add(rel.getName());
								break;
							}
						}
					}
					else
					{// primary dataprovider is a global variable
					}
				}
				for (Relation rel : com.servoy.j2db.util.Utils.iterate(flattenedSolution.getSolution().getRelations(table, false, false)))
				{
					for (IDataProvider dataProvider : rel.getForeignColumns())
					{
						if (dataProvider.getDataProviderID().equals(dataproviderId))
						{
							relRef.add(rel.getName());
							break;
						}
					}
				}

			}
			catch (RepositoryException e)
			{
				Debug.log("Exception while trying to get relations", e);
			}
		}
		return relRef;
	}

	/**
	 * @return Set<Form> a set of forms whose datasource is the dataprovider's table
	 */
	private Set<String> getFormRefferences()
	{
		Set<String> forms = new HashSet<String>();
		if (flattenedSolution != null)
		{
			try
			{
				for (Form frm : com.servoy.j2db.util.Utils.iterate(flattenedSolution.getSolution().getForms(dataprovider.getTable(), false)))
				{
					forms.add(frm.getName());
				}
			}
			catch (RepositoryException e)
			{
				Debug.log("Exception while trying to get Forms", e);
			}
		}
		return forms;
	}

	private Set<String> getValueListRefferences()
	{
		Set<String> valueLists = new HashSet<String>();
		if (flattenedSolution != null)
		{
			try
			{
				Table table = dataprovider.getTable();
				String dataproviderId = dataprovider.getDataProviderID();

				for (ValueList vl : com.servoy.j2db.util.Utils.iterate(flattenedSolution.getValueLists(false)))
				{
					//value list references Table
					if (table.getDataSource().equals(vl.getDataSource()))
					{
						for (String id : vl.getDataProviderIDs())
						{
							if (dataproviderId.equals(id))
							{
								valueLists.add(vl.getName());
								break;
							}
						}
					}
					//value list references a relation
					else if (vl.getRelationName() != null)
					{
						Relation[] relations = flattenedSolution.getRelationSequence(vl.getRelationName());
						if (relations != null)
						{
							if (table.getDataSource().contains(relations[relations.length - 1].getForeignDataSource()))
							{
								for (String id : vl.getDataProviderIDs())
								{
									if (dataproviderId.equals(id))
									{
										valueLists.add(vl.getName());
										break;
									}
								}
							}
						}
					}
				}

			}
			catch (RepositoryException e)
			{
				Debug.log("Exception while trying to get ValueLists", e);
			}


		}
		return valueLists;
	}

	private class DataProviderSearchCollector extends TextSearchResultCollector
	{
		private ISelectionEngine engine;
		private final IColumn dataprovider;
		private boolean found;

		public DataProviderSearchCollector(AbstractTextSearchResult result, IColumn dataprovider)
		{
			super(result);
			this.dataprovider = dataprovider;
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
						if (DataProviderSearchCollector.this.dataprovider.equals(((Element)object).getAttribute("servoy.RESOURCE"))) //$NON-NLS-1$
						{
							found = true;
						}
					}
				}
			});
		}

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
			else
			{// search in servoy resources
				String fileName = matchRequestor.getFile().getName();
				String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
				if (tokens.length > 1)
				{
					if (tokens[1].equals("rel"))
					{
						if (!DataProviderSearch.this.relationReferences.contains(tokens[0]))
						{
							return null;
						}
					}
					else if (tokens[1].equals("val"))
					{
						if (!DataProviderSearch.this.valueListReferences.contains(tokens[0]))
						{
							return null;
						}
					}
					else if (tokens[1].equals("frm"))
					{
						//if form's datasource does not reference dataprovider's table
						if (!DataProviderSearch.this.formReferences.contains(tokens[0]))
						{ //if matches the name of the dataprovider  (can't be this table )
							if (matchRequestor.getMatchLength() == dataprovider.getDataProviderID().length())
							{
								return null;
							}
						}
					}

				}
			}
			return match;
		}
	}

}
