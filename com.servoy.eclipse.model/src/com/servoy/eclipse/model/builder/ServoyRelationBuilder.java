/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.model.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public class ServoyRelationBuilder
{
	public static boolean addRelationMarkers(IProject project, IFile file)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());
		FlattenedSolution fs = ServoyBuilderUtils.getReferenceFlattenedSolution(servoyProject.getSolution());
		String relationName = file.getName().substring(0, file.getName().length() - SolutionSerializer.RELATION_FILE_EXTENSION.length());
		Relation relation = fs.getRelation(relationName);
		if (relation == null)
		{
			return false;
		}

		ServoyBuilder.checkPersistDuplicateName();
		ServoyBuilder.checkPersistDuplicateUUID();

		return addRelationMarkers(servoyProject, relation);
	}

	public static boolean addRelationMarkers(ServoyProject servoyProject, Relation relation)
	{
		deleteMarkers(relation);
		checkRelation(relation);

		List<IPersist> dependencies = BuilderDependencies.getInstance().getRelationDependencies(relation);
		if (dependencies != null)
		{
			Set<UUID> methodsParsed = new HashSet<UUID>();
			Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
			for (IPersist persist : dependencies)
			{
				if (persist instanceof Form)
				{
					ServoyFormBuilder.deleteMarkers((Form)persist);
					ServoyFormBuilder.addFormMarkers(servoyProject, (Form)persist, methodsParsed, formsAbstractChecked);
				}
				if (persist instanceof ValueList)
				{
					ServoyValuelistBuilder.deleteMarkers((ValueList)persist);
					ServoyValuelistBuilder.addValuelistMarkers(servoyProject, (ValueList)persist, ServoyModelFinder.getServoyModel().getFlattenedSolution());
				}
			}
		}
		return true;
	}

	public static void deleteMarkers(Relation relation)
	{
		IResource markerResource = ServoyBuilderUtils.getPersistResource(relation);
		try
		{
			markerResource.deleteMarkers(ServoyBuilder.PROJECT_RELATION_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_ELEMENT_USAGE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.HIDDEN_TABLE_STILL_IN_USE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.INVALID_SORT_OPTION, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

	}

	public static void checkRelation(Relation element)
	{
		ServoyMarker mk = null;
		IResource markerResource = ServoyBuilderUtils.getPersistResource(element);
		IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
		String primaryServerName = null;
		String primaryTableName = null;
		String foreignServerName = null;
		String foreignTableName = null;

		String[] snt = DataSourceUtils.getDBServernameTablename(element.getPrimaryDataSource());
		if (snt != null)
		{
			primaryServerName = snt[0];
			primaryTableName = snt[1];
		}
		else
		{
			primaryTableName = DataSourceUtils.getInmemDataSourceName(element.getPrimaryDataSource());
			if (primaryTableName != null)
			{
				primaryServerName = DataSourceUtils.INMEM_DATASOURCE;
			}
			else if ((primaryServerName = DataSourceUtils.getViewDataSourceName(element.getForeignDataSource())) != null)
			{
				primaryServerName = DataSourceUtils.VIEW_DATASOURCE;
			}
			else return; // just skip this relation, unknown datasource
		}

		snt = DataSourceUtils.getDBServernameTablename(element.getForeignDataSource());
		if (snt != null)
		{
			foreignServerName = snt[0];
			foreignTableName = snt[1];
		}
		else
		{
			foreignTableName = DataSourceUtils.getInmemDataSourceName(element.getForeignDataSource());
			if (foreignTableName != null)
			{
				foreignServerName = DataSourceUtils.INMEM_DATASOURCE;
			}
			else if ((foreignTableName = DataSourceUtils.getViewDataSourceName(element.getForeignDataSource())) != null)
			{
				foreignServerName = DataSourceUtils.VIEW_DATASOURCE;
			}
			else return; // just skip this relation, unknown datasource
		}

		FlattenedSolution relationFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(element,
			ServoyModelFinder.getServoyModel().getFlattenedSolution());
		element.setValid(true);//if is reload
		try
		{
			IServerInternal pserver = dsm.getServer(element.getPrimaryDataSource());
			if (pserver == null)
			{
				mk = MarkerMessages.RelationPrimaryServerWithProblems.fill(element.getName(), primaryServerName);
				element.setValid(false);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_PRIMARY_SERVER_WITH_PROBLEMS,
					IMarker.PRIORITY_NORMAL, null, element);
				return;
			}
			else
			{
				if (!pserver.getName().equals(primaryServerName))
				{
					mk = MarkerMessages.RelationPrimaryServerDuplicate.fill(element.getName(), primaryServerName);
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_SERVER_DUPLICATE, IMarker.PRIORITY_NORMAL,
						null, element);
				}
			}
			ITable ptable = pserver.getTable(primaryTableName);
			boolean usingHiddenTableInPrimary = false;
			if (ptable == null)
			{
				mk = MarkerMessages.RelationPrimaryTableNotFound.fill(element.getName(), primaryTableName, primaryServerName);
				element.setValid(false);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_TABLE_NOT_FOUND, IMarker.PRIORITY_NORMAL, null,
					element);
				return;
			}
			else
			{
				if (ptable.isMarkedAsHiddenInDeveloper())
				{
					usingHiddenTableInPrimary = true;
					mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ptable.getDataSource(), "relation ", element.getName());
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW, null,
						element);
				}
				if (ptable.getRowIdentColumnsCount() == 0 && !(ptable instanceof MemTable))
				{
					mk = MarkerMessages.RelationPrimaryTableWithoutPK.fill(element.getName(), primaryTableName, primaryServerName);
					element.setValid(false);
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_TABLE_WITHOUT_PK, IMarker.PRIORITY_NORMAL,
						null, element);
					return;
				}
			}

			IServerInternal fserver = dsm.getServer(element.getForeignDataSource());
			if (fserver == null)
			{
				mk = MarkerMessages.RelationForeignServerWithProblems.fill(element.getName(), foreignServerName);
				element.setValid(false);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_FOREIGN_SERVER_WITH_PROBLEMS,
					IMarker.PRIORITY_NORMAL, null, element);
				return;
			}
			else if (!fserver.getName().equals(foreignServerName))
			{
				mk = MarkerMessages.RelationForeignServerDuplicate.fill(element.getName(), foreignServerName);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_SERVER_DUPLICATE, IMarker.PRIORITY_NORMAL, null,
					element);
			}

			ITable ftable = fserver.getTable(foreignTableName);
			if (ftable == null)
			{
				mk = MarkerMessages.RelationForeignTableNotFound.fill(element.getName(), foreignTableName, foreignServerName);
				element.setValid(false);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_TABLE_NOT_FOUND, IMarker.PRIORITY_NORMAL, null,
					element);
				return;
			}
			else
			{
				if (!usingHiddenTableInPrimary && ftable.isMarkedAsHiddenInDeveloper())
				{
					mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ftable.getDataSource(), "relation ", element.getName());
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW, null,
						element);
				}
				if (ftable.getRowIdentColumnsCount() == 0 && !(ftable instanceof MemTable))
				{
					mk = MarkerMessages.RelationForeignTableWithoutPK.fill(element.getName(), foreignTableName, foreignServerName);
					element.setValid(false);
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_TABLE_WITHOUT_PK, IMarker.PRIORITY_NORMAL,
						null, element);
					return;
				}
			}
			if (!element.isParentRef() && element.getItemCount() == 0)
			{
				mk = MarkerMessages.RelationEmpty.fill(element.getName());
				element.setValid(false);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_EMPTY, IMarker.PRIORITY_NORMAL, null, element);
				return;
			}
			if (element.getInitialSort() != null)
			{
				ServoyBuilder.addMarkers(markerResource, ServoyBuilder.checkSortOptions(ftable, element.getInitialSort(), element, relationFlattenedSolution),
					element);
			}
			Iterator<RelationItem> items = element.getObjects(IRepository.RELATION_ITEMS);
			boolean errorsFound = false;
			while (items.hasNext())
			{
				RelationItem item = items.next();
				String primaryDataProvider = item.getPrimaryDataProviderID();
				String foreignColumn = item.getForeignColumnName();
				IDataProvider dataProvider = null;
				IDataProvider column = null;
				if (primaryDataProvider == null || "".equals(primaryDataProvider))
				{
					mk = MarkerMessages.RelationItemNoPrimaryDataprovider.fill(element.getName());
					errorsFound = true;
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
						IMarker.PRIORITY_NORMAL, null,
						element);
				}
				else if (!primaryDataProvider.startsWith(LiteralDataprovider.LITERAL_PREFIX))
				{
					if (ScopesUtils.isVariableScope(primaryDataProvider))
					{
						dataProvider = relationFlattenedSolution.getGlobalDataProvider(primaryDataProvider, true);
						if (dataProvider != null && dataProvider instanceof ScriptVariable && ((ScriptVariable)dataProvider).isDeprecated())
						{
							mk = MarkerMessages.ElementUsingDeprecatedVariable.fill(((ScriptVariable)dataProvider).getName(),
								"relation " + element.getName(), "primary key");
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
								IMarker.PRIORITY_NORMAL,
								null, element);
						}
						if (dataProvider instanceof ScriptVariable)
							BuilderDependencies.getInstance().addDependency(((ScriptVariable)dataProvider).getScopeName(), element);
					}
					else
					{
						dataProvider = relationFlattenedSolution.getDataProviderForTable(ptable, primaryDataProvider);
					}
					if (dataProvider == null)
					{
						boolean isHiddenEnumProperty = ScopesUtils.isVariableScope(primaryDataProvider) &&
							primaryDataProvider.split("\\.").length > 3;
						if (!isHiddenEnumProperty)
						{
							mk = MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill(element.getName(), primaryDataProvider);
							errorsFound = true;
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
								IMarker.PRIORITY_NORMAL, null,
								element);
						}
					}
					else
					{
						if (dataProvider instanceof ScriptCalculation)
						{
							ScriptCalculation calc = (ScriptCalculation)dataProvider;
							if (calc.isDeprecated())
							{
								mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(calc.getName(), "relation " + element.getName(),
									"primary key");
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
									IMarker.PRIORITY_NORMAL,
									null, element);
							}
						}
					}
				}
				if (foreignColumn == null || "".equals(foreignColumn))
				{
					mk = MarkerMessages.RelationItemNoForeignDataprovider.fill(element.getName());
					errorsFound = true;
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
						IMarker.PRIORITY_NORMAL, null,
						element);
				}
				else
				{
					column = relationFlattenedSolution.getDataProviderForTable(ftable, foreignColumn);
					if (column == null)
					{
						mk = MarkerMessages.RelationItemForeignDataproviderNotFound.fill(element.getName(), foreignColumn);
						errorsFound = true;
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
							IMarker.PRIORITY_NORMAL, null,
							element);
					}
				}
				if (dataProvider != null && column != null && dataProvider instanceof Column && column instanceof Column &&
					((Column)dataProvider).getColumnInfo() != null && ((Column)column).getColumnInfo() != null)
				{
					boolean primaryColumnUuidFlag = ((Column)dataProvider).getColumnInfo().hasFlag(IBaseColumn.UUID_COLUMN);
					boolean foreignColumnUuidFlag = ((Column)column).getColumnInfo().hasFlag(IBaseColumn.UUID_COLUMN);
					if ((primaryColumnUuidFlag && !foreignColumnUuidFlag) || (!primaryColumnUuidFlag && foreignColumnUuidFlag))
					{
						if (!(((Column)dataProvider).getTable() instanceof MemTable) && !(((Column)column).getTable() instanceof MemTable))
						{
							// for memtable flag cannot be set, ignore then
							mk = MarkerMessages.RelationItemUUIDProblem.fill(element.getName(), primaryDataProvider, foreignColumn);
							errorsFound = true;
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_UUID_PROBLEM,
								IMarker.PRIORITY_NORMAL, null, element);
						}
					}
					if (((Column)dataProvider).getColumnInfo().isExcluded())
					{
						mk = MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill(element.getName(), primaryDataProvider);
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
							IMarker.PRIORITY_NORMAL, null,
							element);
					}
					if (((Column)column).getColumnInfo().isExcluded())
					{
						mk = MarkerMessages.RelationItemForeignDataproviderNotFound.fill(element.getName(), foreignColumn);
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
							IMarker.PRIORITY_NORMAL, null,
							element);
					}
				}
			}
			if (errorsFound)
			{
				element.setValid(false);
				return;
			}
			String typeMismatchWarning = element.checkKeyTypes(relationFlattenedSolution);
			if (typeMismatchWarning != null)
			{
				mk = MarkerMessages.RelationItemTypeProblem.fill(element.getName(), typeMismatchWarning);
				ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
					ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM, IMarker.PRIORITY_LOW,
					null, element);
			}
		}
		catch (Exception ex)
		{
			ServoyBuilder.exceptionCount++;
			if (ServoyBuilder.exceptionCount < ServoyBuilder.MAX_EXCEPTIONS) ServoyLog.logError(ex);
			element.setValid(false);
			if (ex.getMessage() != null) mk = MarkerMessages.RelationGenericErrorWithDetails.fill(element.getName(), ex.getMessage());
			else mk = MarkerMessages.RelationGenericError.fill(element.getName());
			ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.RELATION_GENERIC_ERROR, IMarker.PRIORITY_NORMAL, null,
				element);
		}
	}
}
