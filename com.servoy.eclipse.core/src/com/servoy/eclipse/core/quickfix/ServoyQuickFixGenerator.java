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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixChangeInfoPkIntoUserRowIdent;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixCreateColumnInDB;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixCreateInfoForColumn;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixDeleteColumnFromDB;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixDeleteInfoForColumn;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixIgnoreColumnDifference;
import com.servoy.eclipse.core.quickfix.dbi.DBIQuickFixRenameInfoFromColumn;
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
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

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
		/**
		 * If you deal with a new marker type do not forget to also update the extention point.
		 */
		IMarkerResolution[] fixes = new IMarkerResolution[0];
		try
		{
			String type = marker.getType();
			if (type.equals(ServoyBuilder.MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE))
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
				String uuid = marker.getAttribute("Uuid", null);
				String solName = marker.getAttribute("SolutionName", null);
				IFile file = null;
				if (uuid != null && solName != null)
				{
					UUID id = UUID.fromString(uuid);
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
					IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
					Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, false);
					Path path = new Path(pathPair.getLeft() + pathPair.getRight());
					file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				}
				fixes = new IMarkerResolution[] { new OpenUsingEditor("com.servoy.eclipse.ui.editors.ValueListEditor", null, file) };
			}
			else if (type.equals(ServoyBuilder.INVALID_TABLE_NODE_PROBLEM) || type.equals(ServoyBuilder.ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE))
			{
				String name = marker.getAttribute("Name", null);
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				if (name != null && solName != null && uuid != null)
				{
					fixes = new IMarkerResolution[] { new DeleteOrphanPersistQuickFix(name, uuid, solName) };
				}
			}
			else if (type.equals(ServoyBuilder.DUPLICATE_UUID))
			{
				String uuid = marker.getAttribute("Uuid", null);
				String solName = marker.getAttribute("SolutionName", null);
				if (uuid != null && solName != null)
				{
					UUID id = UUID.fromString(uuid);
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);

					List<ServoyProject> projects = new ArrayList<ServoyProject>();
					projects.add(servoyProject);
					projects.addAll(Arrays.asList(ServoyBuilder.getSolutionModules(servoyProject)));
					List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();
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
			}
			else if (type.equals(ServoyBuilder.DUPLICATE_SIBLING_UUID))
			{
				String uuid = marker.getAttribute("Uuid", null);
				String solName = marker.getAttribute("SolutionName", null);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				if (servoyProject != null && uuid != null)
				{
					UUID id = UUID.fromString(uuid);

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
			}
			else if (type.equals(ServoyBuilder.MISSING_MODULES_MARKER_TYPE))
			{
				String moduleName = marker.getAttribute("moduleName", null);
				String solutionName = marker.getAttribute("solutionName", null);
				fixes = new IMarkerResolution[] { new MissingModuleQuickFix(solutionName, moduleName) };
			}
			else if (type.equals(ServoyBuilder.MISSING_SERVER))
			{
				String serverName = marker.getAttribute("missingServer", null);
				String uuid = marker.getAttribute("Uuid", null);
				String solName = marker.getAttribute("SolutionName", null);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				if (serverName != null && uuid != null && servoyProject != null && servoyProject.isSolutionLoaded())
				{
					UUID id = UUID.fromString(uuid);
					IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
					if (serverName != null)
					{
						fixes = new IMarkerResolution[] { new MissingServerQuickFix(serverName), new DeletePersistQuickFix(persist, servoyProject) };
					}
				}
				else fixes = new IMarkerResolution[0];
			}
			else if (type.equals(ServoyBuilder.BAD_STRUCTURE_MARKER_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				if (solName != null && uuid != null)
				{
					fixes = new IMarkerResolution[] { new DeleteOrphanPersistQuickFix("invalid element: " + uuid, uuid, solName) };
				}
			}
			else if (type.equals(ServoyBuilder.EVENT_METHOD_MARKER_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				String eventName = marker.getAttribute("EventName", null);
				if (solName != null && uuid != null && eventName != null)
				{
					fixes = new IMarkerResolution[] { new AddTemplateArgumentsQuickFix(uuid, solName, marker.getAttribute(IMarker.LINE_NUMBER, 0), eventName) };
				}
			}
			else if (type.equals(ServoyBuilder.WRONG_OVERRIDE_PARENT))
			{
				String solName = marker.getAttribute("SolutionName", null);
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				String uuid = marker.getAttribute("Uuid", null);
				if (servoyProject != null && uuid != null)
				{
					IPersist persist = AbstractRepository.searchPersist(servoyProject.getEditingSolution(), UUID.fromString(uuid));
					fixes = new IMarkerResolution[] { new MoveElementOverride(persist, servoyProject) };
				}
			}
			else if (type.equals(ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				if (solName != null && uuid != null)
				{
					fixes = new IMarkerResolution[] { new ModifyRelationNameQuickfix(uuid, solName, true), new ModifyRelationNameQuickfix(uuid, solName,
						false) };
				}
			}
			else if (type.equals(ServoyBuilder.OBSOLETE_ELEMENT) || type.equals(ServoyBuilder.FORM_DUPLICATE_PART_MARKER_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				if (solName != null && uuid != null)
				{
					UUID id = UUID.fromString(uuid);
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
					IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
					fixes = new IMarkerResolution[] { new DeletePersistQuickFix(persist, servoyProject) };
				}
			}
			else if (type.equals(ServoyBuilder.MISSING_STYLE))
			{
				boolean clearStyle = marker.getAttribute("clearStyle", false);
				String uuid = marker.getAttribute("Uuid", null);
				String styleClass = marker.getAttribute("styleClass", null);
				if (uuid != null && styleClass != null)
				{
					fixes = new IMarkerResolution[] { new ClearOrModifyToCaseSensitiveMissingStyleQuickFix(uuid, clearStyle, styleClass) };
				}
			}
			else if (type.equals(ServoyBuilder.DUPLICATE_NAME_MARKER_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				if (solName != null && uuid != null)
				{
					if (solName != null && uuid != null)
					{
						fixes = new IMarkerResolution[] { new RenamePersistQuickFix(uuid, solName) };
					}
				}
			}
			else if (type.equals(ServoyBuilder.LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE))
			{
				PersistIdentifier persistIdentifier = PersistIdentifier.fromJSONString(marker.getAttribute("persistIdentifier", null));
				String solName = marker.getAttribute("SolutionName", null);
				if (solName != null && persistIdentifier != null)
				{
					fixes = new IMarkerResolution[] { new ClearLabelForElementProperty(persistIdentifier, solName) };
				}
			}
			else if (type.equals(ServoyBuilder.MISPLACED_MODULES_MARKER_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				if (solName != null)
				{
					if (marker.getAttribute("SolutionType", -1) == SolutionMetaData.MOBILE)
					{
						fixes = new IMarkerResolution[] { new ChangeSolutionTypeQuickFix(solName,
							SolutionMetaData.MOBILE_MODULE), new ChangeSolutionTypeQuickFix(solName,
								SolutionMetaData.SOLUTION), new ChangeSolutionTypeQuickFix(solName, SolutionMetaData.MODULE) };
					}
					else
					{
						fixes = new IMarkerResolution[] { new ChangeSolutionTypeQuickFix(solName,
							SolutionMetaData.MOBILE), new ChangeSolutionTypeQuickFix(solName, SolutionMetaData.MOBILE_MODULE) };
					}
				}

			}
			else if (type.equals(ServoyBuilder.LINGERING_TABLE_FILES_TYPE))
			{
				String solName = marker.getAttribute("SolutionName", null);
				String uuid = marker.getAttribute("Uuid", null);
				if (solName != null && uuid != null)
				{
					UUID id = UUID.fromString(uuid);
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
					IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);
					fixes = new IMarkerResolution[] { new ClearLingeringTableFilesQuickFix(persist, servoyProject) };
				}
			}
			else if (type.equals(ServoyBuilder.INVALID_TABLE_NO_PRIMARY_KEY_TYPE))
			{
				String serverName = marker.getAttribute("serverName", null);
				String tableName = marker.getAttribute("tableName", null);
				if (serverName != null && tableName != null)
				{
					fixes = new IMarkerResolution[] { new InvalidTableBecauseNoPrimaryKeyQuickFix(serverName, tableName) };
				}
			}
			else if (type.equals(ServoyBuilder.MISSING_PROJECT_REFERENCE))
			{
				fixes = new IMarkerResolution[] { new OpenMissingProjectReferenceQuickFix(), new DeleteMissingProjectReferenceQuickFix() };
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
			possibleSecurityFixes = new SecurityQuickFix[] { RemoveAllButOneOfTheAccessMasksForElement.getInstance(), RemoveAccessMaskForMissingElement
				.getInstance(), RenameGroupWithInvalidNameInUserListAndAdd.getInstance(), RenameGroupWithInvalidName.getInstance(), RemoveGroupWithInvalidName
					.getInstance(), RenameSomeDuplicateUsers.getInstance(), RemoveSomeOfDuplicateUsers.getInstance(), ReplaceUUIDForOneOfUsersWithDuplicateUUID
						.getInstance(), RemoveSomeUsersWithDuplicateUUID.getInstance(), RemoveGroupWithInvalidNameFromUserList
							.getInstance(), DeclareGroupReferencedInPermissions.getInstance(), RemoveGroupReferencedInPermission
								.getInstance(), CreateUserFromGroupReferenceUUID.getInstance(), RemoveUserReferenceInGroup.getInstance(), CorrectInvalidUser
									.getInstance(), RemoveInvalidUser.getInstance(), DiscardExistingSecurityInfo.getInstance() };
		}
		List<IMarkerResolution> fixes = new ArrayList<IMarkerResolution>();
		fixes.add(new OpenUsingEditor(null, "Open file in text editor in order to manually fix the problem.", null));
		for (SecurityQuickFix possibleFix : possibleSecurityFixes)
		{
			if (possibleFix.canHandleMarker(marker))
			{
				fixes.add(possibleFix);
			}
		}

		return fixes.toArray(new IMarkerResolution[fixes.size()]);
	}

	private IMarkerResolution[] getDatabaseInformationQuickFixes(IMarker marker)
	{
		if (possibleTableFixes == null)
		{
			possibleTableFixes = new TableDifferenceQuickFix[] { DBIQuickFixRenameInfoFromColumn.getInstance(), DBIQuickFixCreateColumnInDB
				.getInstance(), DBIQuickFixDeleteInfoForColumn.getInstance(), DBIQuickFixCreateInfoForColumn.getInstance(), DBIQuickFixDeleteColumnFromDB
					.getInstance(), DBIQuickFixUpdateInfoFromColumn.getInstance(), DBIQuickFixUpdateColumnFromInfo
						.getInstance(), DBIQuickFixChangeInfoPkIntoUserRowIdent.getInstance(), DBIQuickFixIgnoreColumnDifference.getInstance() };
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
