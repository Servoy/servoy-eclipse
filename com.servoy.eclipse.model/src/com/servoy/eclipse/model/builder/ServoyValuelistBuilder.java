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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.builder.ServoyBuilder.Problem;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public class ServoyValuelistBuilder
{
	public static void addValuelistMarkers(IProject project, IFile file)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		FlattenedSolution fs = servoyModel.getFlattenedSolution();
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());

		ServoyBuilder.checkPersistDuplicateName();
		ServoyBuilder.checkPersistDuplicateUUID();


		String valuelistName = file.getName().substring(0, file.getName().length() - SolutionSerializer.VALUELIST_FILE_EXTENSION.length());
		ValueList valuelist = fs.getValueList(valuelistName);
		addValuelistMarkers(servoyProject, valuelist, fs);
	}

	public static void addValuelistMarkers(ServoyProject servoyProject, ValueList valuelist, FlattenedSolution fs)
	{
		if (valuelist != null)
		{
			deleteMarkers(valuelist);
			ServoyBuilder.addMarkers(ServoyBuilderUtils.getPersistResource(valuelist),
				ServoyValuelistBuilder.checkValuelist(valuelist, ServoyBuilder.getPersistFlattenedSolution(valuelist, fs),
					ApplicationServerRegistry.get().getServerManager(), false),
				valuelist);

			List<Form> forms = BuilderDependencies.getInstance().getValuelistDependencies(valuelist);
			if (forms != null)
			{
				Set<UUID> methodsParsed = new HashSet<UUID>();
				Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
				for (Form form : forms)
				{
					ServoyFormBuilder.deleteMarkers(form);
					ServoyFormBuilder.addFormMarkers(servoyProject, form, methodsParsed, formsAbstractChecked);
				}
			}
		}
	}

	public static void deleteMarkers(ValueList valuelist)
	{
		IResource markerResource = ServoyBuilderUtils.getPersistResource(valuelist);
		try
		{
			markerResource.deleteMarkers(ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_ELEMENT_USAGE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.HIDDEN_TABLE_STILL_IN_USE, true, IResource.DEPTH_INFINITE);
			markerResource.deleteMarkers(ServoyBuilder.INVALID_SORT_OPTION, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

	}

	/**
	 * Checks the state of a valueList, and is able to automatically correct a few problems - but with the possibility of loosing info.
	 *
	 * @param vl the valueList to be checked.
	 * @param flattenedSolution the flattened solution used to get relations if needed.
	 * @param sm the server manager to use.
	 * @param fixIfPossible if this is true, it will try to fix a few problems, but be careful - some invalid info in the valueList will probably be lost.
	 * @return a list of problems. Each element in the list represents a problem, and is a 3 object array like [ (int)severity, (String)problemMessage,
	 *         (String)fixDescription ]. "fixDescription" is the description of the fix that would be applied if fixIfPossible is true; null if no fix would be
	 *         applied.
	 */
	public static List<Problem> checkValuelist(ValueList vl, FlattenedSolution flattenedSolution, IServerManagerInternal sm, boolean fixIfPossible)
	{
		List<Problem> problems = new ArrayList<Problem>();
		try
		{
			if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES)
			{
				if (vl.getCustomValues() != null)
				{
					String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_DB_WITH_CUSTOM_VALUES.getLeft(),
						ServoyBuilder.VALUELIST_DB_WITH_CUSTOM_VALUES.getRight().name(), vl);
					if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
					{
						// this is not a custom valuelist
						ServoyMarker marker = MarkerMessages.ValuelistDBWithCustomValues.fill(vl.getName());
						problems.add(new Problem(marker.getType(),
							ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_DB_WITH_CUSTOM_VALUES.getRight()),
							marker.getText(), marker.getFix()));
					}
					if (fixIfPossible) vl.setCustomValues(null);
				}
				String dataSource = null;
				ITable table = null;
				if (vl.getRelationName() != null)
				{
					// vl. based on relation; make sure table name/server name are not specified
					if (vl.getTableName() != null || vl.getServerName() != null)
					{
						String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_RELATION_WITH_DATASOURCE.getLeft(),
							ServoyBuilder.VALUELIST_RELATION_WITH_DATASOURCE.getRight().name(),
							vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistRelationWithDatasource.fill(vl.getName());
							problems.add(new Problem(mk.getType(),
								ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_RELATION_WITH_DATASOURCE.getRight()),
								mk.getText(), mk.getFix()));
						}
						if (fixIfPossible) vl.setDataSource(null);
					}
					String[] parts = vl.getRelationName().split("\\.");
					for (String relName : parts)
					{
						Relation relation = flattenedSolution.getRelation(relName);
						if (relation == null)
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
								ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistRelationNotFound.fill(vl.getName(), relName);
								problems.add(
									new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
							}
						}
						else
						{
							BuilderDependencies.getInstance().addDependency(vl, relation);
							dataSource = relation.getForeignDataSource();
						}
					}
					if (dataSource != null)
					{
						// check if the relations match up (check foreign/primary tables)
						if (flattenedSolution.getRelationSequence(vl.getRelationName()) == null)
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_RELATION_SEQUENCE_INCONSISTENT.getLeft(),
								ServoyBuilder.VALUELIST_RELATION_SEQUENCE_INCONSISTENT.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistRelationSequenceInconsistent.fill(vl.getName(), vl.getRelationName());
								problems.add(new Problem(mk.getType(),
									ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_RELATION_SEQUENCE_INCONSISTENT.getRight()),
									mk.getText()));
							}
						}
					}
				}
				else if (vl.getDataSource() != null)
				{
					// this is table based...
					dataSource = vl.getDataSource();
				}
				else
				{
					String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_DB_NOT_TABLE_OR_RELATION.getLeft(),
						ServoyBuilder.VALUELIST_DB_NOT_TABLE_OR_RELATION.getRight().name(), vl);
					if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
					{
						ServoyMarker mk = MarkerMessages.ValuelistDBNotTableOrRelation.fill(vl.getName());
						problems.add(
							new Problem(mk.getType(),
								ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_DB_NOT_TABLE_OR_RELATION.getRight()),
								mk.getText()));
					}
				}
				if (dataSource != null)
				{
					String inmemDataSourceName = DataSourceUtils.getInmemDataSourceName(dataSource);
					if (inmemDataSourceName != null)
					{
					}
					else
					{
						String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
						if (stn == null || (stn != null && (stn.length == 0 || (stn.length > 0 && stn[0] == null))))
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_DB_MALFORMED_TABLE_DEFINITION.getLeft(),
								ServoyBuilder.VALUELIST_DB_MALFORMED_TABLE_DEFINITION.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBMalformedTableDefinition.fill(vl.getName(), dataSource);
								problems.add(new Problem(mk.getType(),
									ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_DB_MALFORMED_TABLE_DEFINITION.getRight()),
									mk.getText()));
							}
						}
						else
						{
							IServerInternal server = (IServerInternal)sm.getServer(stn[0]);
							if (server != null)
							{
								if (!server.getName().equals(stn[0]))
								{
									String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_DB_SERVER_DUPLICATE.getLeft(),
										ServoyBuilder.VALUELIST_DB_SERVER_DUPLICATE.getRight().name(), vl);
									if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
									{
										ServoyMarker mk = MarkerMessages.ValuelistDBServerDuplicate.fill(vl.getName(), stn[0]);
										problems.add(new Problem(mk.getType(),
											ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_DB_SERVER_DUPLICATE.getRight()),
											mk.getText()));
									}
								}
								table = server.getTable(stn[1]);
								if (table == null)
								{
									String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
										ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
									if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
									{
										ServoyMarker mk = MarkerMessages.ValuelistDBTableNotAccessible.fill(vl.getName(), stn[1]);
										problems.add(new Problem(mk.getType(),
											ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
											mk.getText()));
									}
								}
								else if (table.isMarkedAsHiddenInDeveloper())
								{
									String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.INVALID_TABLE_REFERENCE.getLeft(),
										ServoyBuilder.INVALID_TABLE_REFERENCE.getRight().name(), vl);
									if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
									{
										ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), "valuelist ", vl.getName());
										problems.add(
											new Problem(mk.getType(),
												ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.INVALID_TABLE_REFERENCE.getRight()),
												IMarker.PRIORITY_LOW, mk.getText(), null));
									}
								}
								else if (table.getRowIdentColumnsCount() == 0 && !(table instanceof MemTable))
								{
									String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_DB_TABLE_NO_PK.getLeft(),
										ServoyBuilder.VALUELIST_DB_TABLE_NO_PK.getRight().name(), vl);
									if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
									{
										ServoyMarker mk = MarkerMessages.ValuelistDBTableNoPk.fill(vl.getName(), stn[1]);
										problems.add(new Problem(mk.getType(),
											ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_DB_TABLE_NO_PK.getRight()),
											IMarker.PRIORITY_LOW, mk.getText(), null));
									}
								}
							} // server not found is reported elsewhere
						}
					}
				}
				if (table != null)
				{
					if (vl.getDataProviderID1() != null && !"".equals(vl.getDataProviderID1()))
					{
						Column column = table.getColumn(vl.getDataProviderID1());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table) == null)
							{
								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
									ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID1(), table.getName());
									problems.add(
										new Problem(mk.getType(),
											ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
											mk.getText()));
								}
							}
							else if (flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table).isDeprecated())
							{
								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
									ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
										flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table).getName(), "valuelist " + vl.getName(),
										"Related value");
									problems.add(new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()),
										mk.getText()));
								}
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
								ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID1(), table.getName());
								problems.add(
									new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
							}
						}
					}
					if (vl.getDataProviderID2() != null && !vl.getDataProviderID2().equals(vl.getDataProviderID1()) && !"".equals(vl.getDataProviderID2()))
					{
						Column column = table.getColumn(vl.getDataProviderID2());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table) == null)
							{
								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
									ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID2(), table.getName());
									problems.add(
										new Problem(mk.getType(),
											ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
											mk.getText()));
								}
							}
							else if (flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table).isDeprecated())
							{
								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
									ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
										flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table).getName(), "valuelist " + vl.getName(),
										"Related value");
									problems.add(new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()),
										mk.getText()));
								}
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
								ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID2(), table.getName());
								problems.add(
									new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
							}
						}
					}
					if (vl.getDataProviderID3() != null && !vl.getDataProviderID3().equals(vl.getDataProviderID1()) &&
						!vl.getDataProviderID3().equals(vl.getDataProviderID2()) && !"".equals(vl.getDataProviderID3()))
					{
						Column column = table.getColumn(vl.getDataProviderID3());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table) == null)
							{
								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
									ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID3(), table.getName());
									problems.add(
										new Problem(mk.getType(),
											ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
											mk.getText()));
								}
							}
							else if (flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table).isDeprecated())
							{
								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
									ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
										flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table).getName(), "valuelist " + vl.getName(),
										"Related value");
									problems.add(new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()),
										mk.getText()));
								}
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
								ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID3(), table.getName());
								problems.add(
									new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
							}
						}
					}
					if (vl.getUseTableFilter() && vl.getValueListType() == IValueListConstants.DATABASE_VALUES &&
						vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
					{
						Column column = table.getColumn(DBValueList.NAME_COLUMN);
						if (column == null)
						{
							String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
								ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), DBValueList.NAME_COLUMN, table.getName());
								problems.add(
									new Problem(mk.getType(),
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
							}
						}
					}

					if (vl.getSortOptions() != null)
					{
						List<Problem> sortProblems = ServoyBuilder.checkSortOptions(table, vl.getSortOptions(), vl, flattenedSolution);
						if (sortProblems != null)
						{
							problems.addAll(sortProblems);
						}
					}
				}
			}
			else if (vl.getValueListType() == IValueListConstants.CUSTOM_VALUES || vl.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
			{
				// custom value list; make sure it does not specify table/server/relation
				if (vl.getTableName() != null || vl.getServerName() != null || vl.getRelationName() != null)
				{
					String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_CUSTOM_VALUES_WITH_DB_INFO.getLeft(),
						ServoyBuilder.VALUELIST_CUSTOM_VALUES_WITH_DB_INFO.getRight().name(),
						vl);
					if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
					{
						ServoyMarker marker = MarkerMessages.ValuelistCustomValuesWithDBInfo.fill(vl.getName());
						problems.add(new Problem(marker.getType(),
							ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_CUSTOM_VALUES_WITH_DB_INFO.getRight()),
							marker.getText(), marker.getFix()));
					}
					if (fixIfPossible)
					{
						vl.setDataSource(null);
						vl.setRelationName(null);
					}
				}
				if (vl.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
				{
					ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(vl.getCustomValues());
					if (scriptMethod == null)
					{
						String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
							ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistGlobalMethodNotFound.fill(vl.getName());
							problems.add(new Problem(mk.getType(),
								ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()), mk.getText()));
						}
					}
					else if (scriptMethod.getParent() != vl.getParent() && scriptMethod.isPrivate())
					{
						String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getLeft(),
							ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistGlobalMethodNotAccessible.fill(vl.getName());
							problems.add(new Problem(mk.getType(),
								ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND.getRight()), mk.getText()));
						}
					}
					else if (scriptMethod.isDeprecated())
					{
						String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
							ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
								"valuelist " + vl.getName(), "Global method");
							problems.add(new Problem(mk.getType(),
								ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()),
								mk.getText()));
						}
					}
					if (scriptMethod != null)
					{
						BuilderDependencies.getInstance().addDependency(scriptMethod.getScopeName(), vl);
					}
				}
				if (vl.getValueListType() == IValueListConstants.CUSTOM_VALUES)
				{
					String values = vl.getCustomValues();
					boolean invalidValues = false;
					if (values != null && values.contains("|"))
					{
						StringTokenizer tk = new StringTokenizer(values.trim(), "\r\n");
						while (tk.hasMoreTokens())
						{
							String line = tk.nextToken();
							if (!line.contains("|"))
							{
								invalidValues = true;
								break;
							}
						}
					}
					if (invalidValues)
					{
						String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_INVALID_CUSTOM_VALUES.getLeft(),
							ServoyBuilder.VALUELIST_INVALID_CUSTOM_VALUES.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistInvalidCustomValues.fill(vl.getName());
							problems.add(
								new Problem(mk.getType(),
									ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_INVALID_CUSTOM_VALUES.getRight()),
									mk.getText()));
						}
					}
				}
			}
			else
			{
				String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_TYPE_UNKNOWN.getLeft(),
					ServoyBuilder.VALUELIST_TYPE_UNKNOWN.getRight().name(), vl);
				if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
				{
					ServoyMarker mk = MarkerMessages.ValuelistTypeUnknown.fill(vl.getName(), vl.getValueListType());
					problems.add(new Problem(mk.getType(), ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_TYPE_UNKNOWN.getRight()),
						mk.getText()));
				}
				if (fixIfPossible) vl.setValueListType(IValueListConstants.CUSTOM_VALUES);
			}
		}
		catch (Exception ex)
		{
			ServoyBuilder.exceptionCount++;
			if (ServoyBuilder.exceptionCount < ServoyBuilder.MAX_EXCEPTIONS) ServoyLog.logError(ex);
			String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.VALUELIST_GENERIC_ERROR.getLeft(),
				ServoyBuilder.VALUELIST_GENERIC_ERROR.getRight().name(), vl);
			if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
			{
				ServoyMarker mk;
				if (ex.getMessage() != null) mk = MarkerMessages.ValuelistGenericErrorWithDetails.fill(vl.getName(), ex.getMessage());
				else mk = MarkerMessages.ValuelistGenericError.fill(vl.getName());
				problems.add(new Problem(mk.getType(), ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.VALUELIST_GENERIC_ERROR.getRight()),
					mk.getText()));
			}
		}
		return problems;
	}
}
