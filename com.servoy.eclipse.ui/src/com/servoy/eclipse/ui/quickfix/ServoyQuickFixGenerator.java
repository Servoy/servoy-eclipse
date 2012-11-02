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
package com.servoy.eclipse.ui.quickfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.keyword.Ident;

/**
 * Class that gives the list of quick-fixes (available in the ui plugin) for Servoy markers.
 * 
 * @author acostescu
 */
public class ServoyQuickFixGenerator implements IMarkerResolutionGenerator
{

	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		try
		{
			String type = marker.getType();

			if (type.equals(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE))
			{
				return getDatabaseInformationQuickFixes(marker);
			}

			if (type.equals(ServoyBuilder.INVALID_SORT_OPTION))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				return new IMarkerResolution[] { new RemoveInvalidSortColumnsQuickFix(uuid, solName) };
			}

			if (type.equals(ServoyBuilder.DUPLICATE_SCOPE_NAME_MARKER_TYPE))
			{
				return new IMarkerResolution[] { new RenameScopeNameQuickFix(marker.getResource()) };
			}
			if (type.equals(ServoyBuilder.UNRESOLVED_RELATION_UUID))
			{
				String propertyName = (String)marker.getAttribute("PropertyName");
				String displayName = (String)marker.getAttribute("DisplayName");
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");

				return new IMarkerResolution[] { new ResolveUuidRelationNameQuickFix(solName, uuid, propertyName, displayName), new ClearPropertyQuickFix(
					solName, uuid, propertyName, displayName) };
			}

			// add dynamic resolutions below this line
			List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();

			if (type.equals(ServoyBuilder.DEPRECATED_PROPERTY_USAGE) || type.equals(ServoyBuilder.FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION))
			{
				String propertyName = (String)marker.getAttribute("PropertyName");
				String displayName = (String)marker.getAttribute("DisplayName");
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");

				resolutions.add(new ClearPropertyQuickFix(solName, uuid, propertyName, displayName));

				if (StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName().equals(propertyName))
				{
					resolutions.add(0, new CreateLoginSolutionQuickFix(solName));
					resolutions.add(new MarkSolutionAsWebclientOnlyQuickFix(solName));
				}
			}

			else if (type.equals(ServoyBuilder.INVALID_EVENT_METHOD) || type.equals(ServoyBuilder.INVALID_COMMAND_METHOD))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String eventName = (String)marker.getAttribute("EventName");
				String uuid = (String)marker.getAttribute("Uuid");
				String dataSource = (String)marker.getAttribute("DataSource");
				int contextTypeId = marker.getAttribute("ContextTypeId", -1);

				BaseSetPropertyQuickFix quickfix = type.equals(ServoyBuilder.INVALID_EVENT_METHOD) && contextTypeId == IRepository.FORMS
					? new SetPropertyQuickFix(solName, uuid, eventName, eventName, MethodWithArguments.METHOD_NONE) : new ClearPropertyQuickFix(solName, uuid,
						eventName, eventName);
				quickfix.setLabel("Clear property " + quickfix.getDisplayName());
				resolutions.add(quickfix);
				if (contextTypeId == IRepository.FORMS) resolutions.add(new CreateMethodReferenceQuickFix(uuid, solName, dataSource, eventName,
					IRepository.FORMS, "form"));
				resolutions.add(new CreateMethodReferenceQuickFix(uuid, solName, dataSource, eventName, IRepository.SOLUTIONS, "global"));
				if (dataSource != null) resolutions.add(new CreateMethodReferenceQuickFix(uuid, solName, dataSource, eventName, IRepository.TABLENODES,
					"entity"));
			}

			else if (type.equals(ServoyBuilder.INVALID_DATAPROVIDERID))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				String dataProviderID = (String)marker.getAttribute("DataProviderID");

				resolutions.add(new ClearPropertyQuickFix(solName, uuid, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName()));

				UUID id = UUID.fromString(uuid);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					if (persist instanceof ISupportDataProviderID)
					{
						IPersist parent = persist.getAncestor(IRepository.FORMS);
						if (parent != null)
						{
							Table table = ((Form)parent).getTable();
							if (table != null)
							{
								String columnName = ((ISupportDataProviderID)persist).getDataProviderID();
								if (table.getColumn(Ident.RESERVED_NAME_PREFIX + columnName) != null)
								{
									//resolutions.add(new RenameDataProviderIDQuickFix((ISupportDataProviderID)persist, columnName));
									resolutions.add(new RenamePropertyQuickFix(solName, uuid,
										StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
										StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), Ident.RESERVED_NAME_PREFIX + columnName));
								}
								else
								{
									// don't create column name when dpid is a global
									if (!ScopesUtils.isVariableScope(columnName)) resolutions.add(new CreateColumnReferenceQuickFix(uuid, solName));
									resolutions.add(new CreateVariableReferenceQuickFix(uuid, solName));
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}

			return resolutions.toArray(new IMarkerResolution[resolutions.size()]);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Can't get quick fixes for a marker", e);
		}
		return new IMarkerResolution[0];
	}

	private IMarkerResolution[] getDatabaseInformationQuickFixes(IMarker marker) throws CoreException
	{
		IMarkerResolution[] fixes = null;
		String serverName = null;
		serverName = (String)marker.getAttribute(TableDifference.ATTRIBUTE_SERVERNAME);
		if (serverName != null)
		{
			DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
			if (dmm != null)
			{
				String tableName = null;
				String columnName = null;
				tableName = (String)marker.getAttribute(TableDifference.ATTRIBUTE_TABLENAME);
				columnName = (String)marker.getAttribute(TableDifference.ATTRIBUTE_COLUMNNAME);

				TableDifference difference = dmm.getColumnDifference(serverName, tableName, columnName);
				if (difference != null)
				{
					if (difference.getType() == TableDifference.MISSING_DBI_FILE || difference.getType() == TableDifference.MISSING_TABLE)
					{
						fixes = new IMarkerResolution[] { DBIQuickFixShowSyncWizard.getInstance() };
						DBIQuickFixShowSyncWizard.getInstance().setCurrentMarker(marker);
					}
				}
				else
				{
					// it is possible that this happens because markers are updated async...
					ServoyLog.logWarning("Asking for .dbi quick fixes when the difference no longer exists", null);
				}
			}
			else
			{
				// it is possible that this happens because markers are updated async...
				ServoyLog.logWarning("Asking for .dbi quick fixes when the manager is null", null);
			}
		} // else JSON syntax problem probably - no quick fix
		if (fixes == null)
		{
			fixes = new IMarkerResolution[0];
		}
		return fixes;
	}

}