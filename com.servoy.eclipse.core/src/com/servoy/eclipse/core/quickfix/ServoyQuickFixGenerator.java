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
package com.servoy.eclipse.core.quickfix;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.builder.AddTemplateArgumentsQuickFix;
import com.servoy.eclipse.core.builder.ChangeResourcesProjectQuickFix;
import com.servoy.eclipse.core.builder.ClearEventMethod;
import com.servoy.eclipse.core.builder.ClearMissingStyleQuickFix;
import com.servoy.eclipse.core.builder.DeleteOrphanPersistQuickFix;
import com.servoy.eclipse.core.builder.DeletePersistQuickFix;
import com.servoy.eclipse.core.builder.DuplicateSiblingUuidQuickFix;
import com.servoy.eclipse.core.builder.DuplicateUuidQuickFix;
import com.servoy.eclipse.core.builder.MissingModuleQuickFix;
import com.servoy.eclipse.core.builder.MissingServerQuickFix;
import com.servoy.eclipse.core.builder.ModifyRelationNameQuickfix;
import com.servoy.eclipse.core.builder.MultipleResourcesMarkerQuickFix;
import com.servoy.eclipse.core.builder.NoResourcesMarkerQuickFix;
import com.servoy.eclipse.core.builder.OpenUsingEditor;
import com.servoy.eclipse.core.builder.RenamePersistQuickFix;
import com.servoy.eclipse.core.builder.ServoyBuilder;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixChangeInfoPkIntoUserRowIdent;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixCreateColumnInDB;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixCreateInfoForColumn;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixDeleteColumnFromDB;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixDeleteInfoForColumn;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixUpdateColumnFromInfo;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixUpdateInfoFromColumn;
import com.servoy.eclipse.core.quickfix.dbi.TableDifferenceQuickFix;
import com.servoy.eclipse.core.quickfix.security.CorrectInvalidUser;
import com.servoy.eclipse.core.quickfix.security.CreateUserFromGroupReferenceUUID;
import com.servoy.eclipse.core.quickfix.security.DeclareGroupReferencedInPermissions;
import com.servoy.eclipse.core.quickfix.security.DiscardExistingSecurityInfo;
import com.servoy.eclipse.core.quickfix.security.RemoveAccessMaskForMissingElement;
import com.servoy.eclipse.core.quickfix.security.RemoveAllButOneOfTheAccessMasksForElement;
import com.servoy.eclipse.core.quickfix.security.RemoveGroupReferencedInPermission;
import com.servoy.eclipse.core.quickfix.security.RemoveGroupWithInvalidName;
import com.servoy.eclipse.core.quickfix.security.RemoveGroupWithInvalidNameFromUserList;
import com.servoy.eclipse.core.quickfix.security.RemoveInvalidUser;
import com.servoy.eclipse.core.quickfix.security.RemoveSomeOfDuplicateUsers;
import com.servoy.eclipse.core.quickfix.security.RemoveSomeUsersWithDuplicateUUID;
import com.servoy.eclipse.core.quickfix.security.RemoveUserReferenceInGroup;
import com.servoy.eclipse.core.quickfix.security.RenameGroupWithInvalidName;
import com.servoy.eclipse.core.quickfix.security.RenameGroupWithInvalidNameInUserListAndAdd;
import com.servoy.eclipse.core.quickfix.security.RenameSomeDuplicateUsers;
import com.servoy.eclipse.core.quickfix.security.ReplaceUUIDForOneOfUsersWithDuplicateUUID;
import com.servoy.eclipse.core.quickfix.security.SecurityQuickFix;
import com.servoy.eclipse.core.repository.SolutionDeserializer;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.core.repository.DataModelManager.TableDifference;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Class that gives the list of quick-fixes (available in the core plugin) for Servoy markers.
 * 
 * @author acostescu
 */
public class ServoyQuickFixGenerator implements IMarkerResolutionGenerator
{

	private static TableDifferenceQuickFix[] possibleTableFixes = null;
	private static SecurityQuickFix[] possibleSecurityFixes = null;

	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		IMarkerResolution[] fixes = new IMarkerResolution[0];
		try
		{
			String type = marker.getType();
			if (type.equals(ServoyBuilder.NO_RESOURCES_PROJECTS_MARKER_TYPE))
			{
				fixes = new IMarkerResolution[] { new NoResourcesMarkerQuickFix(), new ChangeResourcesProjectQuickFix() };
			}
			else if (type.equals(ServoyBuilder.MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE))
			{
				fixes = new IMarkerResolution[] { new MultipleResourcesMarkerQuickFix(), new NoResourcesMarkerQuickFix(), new ChangeResourcesProjectQuickFix() };
			}
			else if (type.equals(ServoyBuilder.USER_SECURITY_MARKER_TYPE))
			{
				fixes = getSecurityQuickFixes(marker);
			}
			else if (type.equals(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE))
			{
				fixes = getDatabaseInformationQuickFixes(marker);
			}
			else if (type.equals(ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE))
			{
				fixes = new IMarkerResolution[] { new OpenUsingEditor("com.servoy.eclipse.ui.editors.ValueListEditor", null) };
			}
			else if (type.equals(ServoyBuilder.INVALID_TABLE_NODE_PROBLEM))
			{
				String name = null;
				String solName = null;
				String uuid = null;
				name = (String)marker.getAttribute("Name");
				solName = (String)marker.getAttribute("SolutionName");
				uuid = (String)marker.getAttribute("Uuid");
				fixes = new IMarkerResolution[] { new DeleteOrphanPersistQuickFix(name, uuid, solName) };
			}
			else if (type.equals(ServoyBuilder.DUPLICATE_UUID))
			{
				final String uuid = (String)marker.getAttribute("Uuid");
				String solName = (String)marker.getAttribute("SolutionName");
				final UUID id = UUID.fromString(uuid);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				List<ServoyProject> projects = new ArrayList<ServoyProject>();
				projects.add(servoyProject);
				projects.addAll(Arrays.asList(ServoyBuilder.getSolutionModules(servoyProject)));
				final List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();
				for (final ServoyProject project : projects)
				{
					project.getSolution().acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o.getUUID().equals(id))
							{
								resolutions.add(new DuplicateUuidQuickFix(o, uuid, project.getSolution().getName()));
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
				fixes = resolutions.toArray(new IMarkerResolution[resolutions.size()]);
			}
			else if (type.equals(ServoyBuilder.DUPLICATE_SIBLING_UUID))
			{
				String uuid = (String)marker.getAttribute("Uuid");
				String solName = (String)marker.getAttribute("SolutionName");
				final UUID id = UUID.fromString(uuid);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, false);
				Path path = new Path(pathPair.getLeft());
				IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				java.io.File file = new File(folder.getLocation().toOSString());
				File[] files = file.listFiles(new FileFilter()
				{
					public boolean accept(File pathname)
					{
						return pathname.isFile() && SolutionSerializer.isJSONFile(pathname.getName());
					}
				});
				List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();
				for (File f : files)
				{
					UUID newUUID = SolutionDeserializer.getUUID(f);
					if (newUUID != null && newUUID.equals(id))
					{
						IFile fileForLocation = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
							Path.fromPortableString(f.getAbsolutePath().replace('\\', '/')));
						resolutions.add(new DuplicateSiblingUuidQuickFix(fileForLocation.getFullPath().toString()));
					}
				}
				fixes = resolutions.toArray(new IMarkerResolution[resolutions.size()]);
			}
			else if (type.equals(ServoyBuilder.MISSING_MODULES_MARKER_TYPE))
			{
				String moduleName = (String)marker.getAttribute("moduleName");
				String solutionName = (String)marker.getAttribute("solutionName");
				fixes = new IMarkerResolution[] { new MissingModuleQuickFix(solutionName, moduleName) };
			}
			else if (type.equals(ServoyBuilder.MISSING_SERVER))
			{
				String serverName = (String)marker.getAttribute("missingServer");
				String uuid = (String)marker.getAttribute("Uuid");
				final UUID id = UUID.fromString(uuid);
				String solName = (String)marker.getAttribute("SolutionName");
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
				if (serverName != null) fixes = new IMarkerResolution[] { new MissingServerQuickFix(serverName), new DeletePersistQuickFix(persist,
					servoyProject) };
				else fixes = new IMarkerResolution[0];
			}
			else if (type.equals(ServoyBuilder.BAD_STRUCTURE_MARKER_TYPE))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				fixes = new IMarkerResolution[] { new DeleteOrphanPersistQuickFix("invalid element: " + uuid, uuid, solName) };
			}
			else if (type.equals(ServoyBuilder.EVENT_METHOD_MARKER_TYPE))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				String eventName = (String)marker.getAttribute("EventName");
				fixes = new IMarkerResolution[] { new AddTemplateArgumentsQuickFix(uuid, solName, Utils.getAsInteger(marker.getAttribute(IMarker.LINE_NUMBER),
					false), eventName) };
			}
			else if (type.equals(ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				fixes = new IMarkerResolution[] { new ModifyRelationNameQuickfix(uuid, solName, true), new ModifyRelationNameQuickfix(uuid, solName, false) };
			}
			else if (type.equals(ServoyBuilder.INVALID_EVENT_METHOD))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String eventName = (String)marker.getAttribute("EventName");
				String uuid = (String)marker.getAttribute("Uuid");
				fixes = new IMarkerResolution[] { new ClearEventMethod(uuid, solName, eventName) };
			}
			else if (type.equals(ServoyBuilder.MISSING_STYLE))
			{
				boolean clearStyle = marker.getAttribute("clearStyle", false);
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				fixes = new IMarkerResolution[] { new ClearMissingStyleQuickFix(uuid, solName, clearStyle) };
			}
			else if (type.equals(ServoyBuilder.DUPLICATE_NAME_MARKER_TYPE))
			{
				String solName = (String)marker.getAttribute("SolutionName");
				String uuid = (String)marker.getAttribute("Uuid");
				if (solName != null && uuid != null)
				{
					fixes = new IMarkerResolution[] { new RenamePersistQuickFix(uuid, solName) };
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Can't get quick fixes for a marker", e);
			fixes = new IMarkerResolution[0];
		}
		return fixes;
	}

	private IMarkerResolution[] getSecurityQuickFixes(IMarker marker)
	{
		if (possibleSecurityFixes == null)
		{
			new DiscardExistingSecurityInfo();
			possibleSecurityFixes = new SecurityQuickFix[] { RemoveAllButOneOfTheAccessMasksForElement.getInstance(), RemoveAccessMaskForMissingElement.getInstance(), RenameGroupWithInvalidNameInUserListAndAdd.getInstance(), RenameGroupWithInvalidName.getInstance(), RemoveGroupWithInvalidName.getInstance(), RenameSomeDuplicateUsers.getInstance(), RemoveSomeOfDuplicateUsers.getInstance(), ReplaceUUIDForOneOfUsersWithDuplicateUUID.getInstance(), RemoveSomeUsersWithDuplicateUUID.getInstance(), RemoveGroupWithInvalidNameFromUserList.getInstance(), DeclareGroupReferencedInPermissions.getInstance(), RemoveGroupReferencedInPermission.getInstance(), CreateUserFromGroupReferenceUUID.getInstance(), RemoveUserReferenceInGroup.getInstance(), CorrectInvalidUser.getInstance(), RemoveInvalidUser.getInstance(), DiscardExistingSecurityInfo.getInstance() };
		}
		List<IMarkerResolution> fixes = new ArrayList<IMarkerResolution>();
		fixes.add(new OpenUsingEditor(null, "Open file in text editor in order to manually fix the problem."));
		for (SecurityQuickFix possibleFix : possibleSecurityFixes)
		{
			if (possibleFix.canHandleMarker(marker))
			{
				fixes.add(possibleFix);
			}
		}

		return fixes.toArray(new IMarkerResolution[fixes.size()]);
	}

	private IMarkerResolution[] getDatabaseInformationQuickFixes(IMarker marker) throws CoreException
	{
		if (possibleTableFixes == null)
		{
			possibleTableFixes = new TableDifferenceQuickFix[] { DBIQuickFixCreateColumnInDB.getInstance(), DBIQuickFixDeleteInfoForColumn.getInstance(), DBIQuickFixCreateInfoForColumn.getInstance(), DBIQuickFixDeleteColumnFromDB.getInstance(), DBIQuickFixUpdateInfoFromColumn.getInstance(), DBIQuickFixUpdateColumnFromInfo.getInstance(), DBIQuickFixChangeInfoPkIntoUserRowIdent.getInstance() };
		}
		List<IMarkerResolution> fixes = new ArrayList<IMarkerResolution>();
		TableDifference difference = TableDifferenceQuickFix.getTableDifference(marker);
		for (TableDifferenceQuickFix possibleFix : possibleTableFixes)
		{
			if (possibleFix.canHandleDifference(difference))
			{
				fixes.add(possibleFix);
				possibleFix.setCurrentMarker(marker);
			}
		}

		return fixes.toArray(new IMarkerResolution[fixes.size()]);
	}

}
