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

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IAutomaticImportWPMPackages;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
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

				return new IMarkerResolution[] { new ResolveUuidRelationNameQuickFix(solName, uuid, propertyName,
					displayName), new ClearPropertyQuickFix(solName, uuid, propertyName, displayName) };
			}
			if (type.equals(ServoyBuilder.MISSING_PROPERTY_FROM_SPEC))
			{
				return new IMarkerResolution[] { new RemoveJSONPropertyQuickFix(marker) };
			}
			if (type.equals(ServoyBuilder.MISSING_SPEC))
			{
				final String packageName = (String)marker.getAttribute("packageName");
				return new IMarkerResolution[] { new IMarkerResolution()
				{

					@Override
					public void run(IMarker marker)
					{
						List<IAutomaticImportWPMPackages> defaultImports = ModelUtils.getExtensions(IAutomaticImportWPMPackages.EXTENSION_ID);
						if (defaultImports != null && defaultImports.size() > 0)
						{
							defaultImports.get(0).importPackage(packageName);
						}
					}

					@Override
					public String getLabel()
					{
						return "Automatic import of '" + packageName + "' from Servoy Package Manager";
					}
				}, new IMarkerResolution()
				{

					@Override
					public void run(IMarker marker)
					{
						EditorUtil.openWebPackageManager();
					}

					@Override
					public String getLabel()
					{
						return "Open Servoy Package Manager";
					}
				} };
			}

			if (type.equals(ServoyBuilder.DEPRECATED_SPEC))
			{
				final String replacement = (String)marker.getAttribute("replacement");
				if (replacement != null)
				{
					String solName = (String)marker.getAttribute("solutionName");
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
					if (servoyProject.isSolutionLoaded())
					{
						return new IMarkerResolution[] { new DeprecatedSpecQuickFix(marker) };
					}
				}
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
				}
			}

			else if (type.equals(ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE))
			{
				String propertyName = (String)marker.getAttribute("PropertyName");
				String displayName = (String)marker.getAttribute("DisplayName");
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");

				if (StaticContentSpecLoader.PROPERTY_FIRSTFORMID.getPropertyName().equals(propertyName))
				{
					resolutions.add(new ClearPropertyQuickFix(solName, uuid, propertyName, displayName));
				}
			}

			else if (type.equals(ServoyBuilder.INVALID_EVENT_METHOD) || type.equals(ServoyBuilder.INVALID_COMMAND_METHOD))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String eventName = (String)marker.getAttribute("EventName");
				String uuid = (String)marker.getAttribute("Uuid");
				String dataSource = (String)marker.getAttribute("DataSource");
				int contextTypeId = marker.getAttribute("ContextTypeId", -1);

				BaseSetPropertyQuickFix quickfix = new ClearPropertyQuickFix(solName, uuid, eventName, eventName);
				quickfix.setLabel("Clear property " + quickfix.getDisplayName());
				resolutions.add(quickfix);
				if (contextTypeId == IRepository.FORMS)
					resolutions.add(new CreateMethodReferenceQuickFix(uuid, solName, dataSource, eventName, IRepository.FORMS, "form"));
				resolutions.add(new CreateMethodReferenceQuickFix(uuid, solName, dataSource, eventName, IRepository.SOLUTIONS, "global"));
				if (dataSource != null)
					resolutions.add(new CreateMethodReferenceQuickFix(uuid, solName, dataSource, eventName, IRepository.TABLENODES, "entity"));
			}

			else if (type.equals(ServoyBuilder.INVALID_DATAPROVIDERID))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				String dataProviderID = (String)marker.getAttribute("DataProviderID");

				resolutions.add(new ClearPropertyQuickFix(solName, uuid, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName()));

				UUID id = UUID.fromString(uuid);
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getServoyProject(solName);
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					if (persist instanceof ISupportDataProviderID)
					{
						IPersist parent = persist.getAncestor(IRepository.FORMS);
						if (parent != null)
						{
							ITable table = servoyModel.getDataSourceManager().getDataSource(((Form)parent).getDataSource());
							if (table != null)
							{
								String columnName = ((ISupportDataProviderID)persist).getDataProviderID();
								if (table.getColumn(Ident.RESERVED_NAME_PREFIX + columnName) != null)
								{
									//resolutions.add(new RenameDataProviderIDQuickFix((ISupportDataProviderID)persist, columnName));
									resolutions.add(new RenamePropertyQuickFix(solName, uuid, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
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
			else if (type.equals(ServoyBuilder.DUPLICATE_MEM_TABLE_TYPE))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				final UUID id = UUID.fromString(uuid);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
				resolutions.add(new RenameMemTableQuickFix(persist, servoyProject));
				resolutions.add(new DeleteMemTableQuickFix(persist, servoyProject));
			}
			else if (type.equals(ServoyBuilder.SUPERFORM_PROBLEM_TYPE))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				final UUID id = UUID.fromString(uuid);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				Form form = (Form)servoyProject.getEditingPersist(id);
				Form extendsForm = form.getExtendsForm();
				resolutions.add(new ChangeSuperFormQuickFix(form, servoyProject));
				if (form != null && extendsForm != null)
				{
					if (form.getUseCssPosition() && !extendsForm.getUseCssPosition() && !extendsForm.isResponsiveLayout())
					{
						resolutions.add(new ConvertToCSSPositionLayout(extendsForm, servoyProject));
					}
					else if (extendsForm.getUseCssPosition() && !form.getUseCssPosition() && !form.isResponsiveLayout())
					{
						resolutions.add(new ConvertToCSSPositionLayout(form, servoyProject));
					}
				}
			}
			else if (type.equals(BaseNGPackageManager.SPEC_READ_MARKER))
			{
				resolutions.add(new SpecReadMarkerQuickFix(marker.getResource()));
			}
			else if (type.equals(ServoyBuilder.METHOD_OVERRIDE))
			{
				resolutions.add(new MethodOverrideProblemQuickFix());
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