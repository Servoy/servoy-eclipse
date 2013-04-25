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
package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.CompositeResourceMapping;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.ui.actions.Openable;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.preferences.TeamPreferences;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.util.Pair;

public class SimpleUserNodeAdapterFactory implements IAdapterFactory
{

	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		if (adapterType == ResourceMapping.class)
		{
			ArrayList<SimpleResourceMapping> mappings = new ArrayList<SimpleResourceMapping>();

			SimpleUserNode simpleUserNode = (SimpleUserNode)adaptableObject;
			UserNodeType nodeType = simpleUserNode.getType();

			if (nodeType == UserNodeType.SOLUTION || nodeType == UserNodeType.SOLUTION_ITEM)
			{
				IProjectNature projectNature = (IProjectNature)simpleUserNode.getRealObject();

				if (projectNature != null)
				{
					mappings.add(new SimpleResourceMapping(projectNature.getProject()));

					// add resource / modules project(s) if set in preferences
					if (nodeType == UserNodeType.SOLUTION)
					{
						RepositoryProvider solutionRP = RepositoryProvider.getProvider(projectNature.getProject());

						if (solutionRP != null)
						{
							if (TeamPreferences.isAutomaticResourceSynch())
							{
								ServoyResourcesProject servoyResourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();

								if (servoyResourcesProject != null)
								{
									IProject resourceProject = servoyResourcesProject.getProject();
									RepositoryProvider rp = RepositoryProvider.getProvider(resourceProject);
									if (rp != null && rp.getID().equals(solutionRP.getID())) mappings.add(new SimpleResourceMapping(resourceProject));
								}
							}

							if (TeamPreferences.isAutomaticModulesSynch())
							{
								ServoyProject[] servoyModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProjectWithImportHooks();

								for (ServoyProject moduleProject : servoyModules)
								{
									// ignore main project that is part of the modules
									if (moduleProject.equals(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject())) continue;

									IProject moduleIProject = moduleProject.getProject();
									RepositoryProvider rp = RepositoryProvider.getProvider(moduleIProject);
									if (rp != null && rp.getID().equals(solutionRP.getID())) mappings.add(new SimpleResourceMapping(moduleProject.getProject()));
								}
							}
						}
					}
				}
			}
			else if (nodeType == UserNodeType.RESOURCES)
			{
				ServoyResourcesProject servoyResourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();

				if (servoyResourcesProject != null)
				{
					mappings.add(new SimpleResourceMapping(servoyResourcesProject.getProject()));
				}
			}
			else if (nodeType == UserNodeType.STYLES)
			{
				SimpleResourceMapping styles = getResourceProjectResourceMapping(StringResourceDeserializer.STYLES_DIR_NAME);
				if (styles != null) mappings.add(styles);
			}
			else if (nodeType == UserNodeType.TEMPLATES)
			{
				SimpleResourceMapping templates = getResourceProjectResourceMapping(StringResourceDeserializer.TEMPLATES_DIR_NAME);
				if (templates != null) mappings.add(templates);
			}
			else if (nodeType == UserNodeType.SERVERS)
			{
				SimpleResourceMapping servers = getResourceProjectResourceMapping(SolutionSerializer.DATASOURCES_DIR_NAME);
				if (servers != null) mappings.add(servers);
			}
			else if (nodeType == UserNodeType.USER_GROUP_SECURITY)
			{
				SimpleResourceMapping securities = getResourceProjectResourceMapping(WorkspaceUserManager.SECURITY_DIR);
				if (securities != null) mappings.add(securities);
			}
			else if (nodeType == UserNodeType.I18N_FILES)
			{
				SimpleResourceMapping i18n = getResourceProjectResourceMapping(EclipseMessages.MESSAGES_DIR);
				if (i18n != null) return i18n.getModelObject();
			}
			else if (nodeType == UserNodeType.FORM)
			{
				Form form = (Form)simpleUserNode.getRealObject();

				Pair<String, String> formFilePath = SolutionSerializer.getFilePath(form, true);
				String formScriptFile = SolutionSerializer.getScriptPath(form, true);

				// add the form file 
				mappings.add(new SimpleResourceMapping(ResourcesPlugin.getWorkspace().getRoot().getFile(
					new Path(formFilePath.getLeft() + formFilePath.getRight()))));
				if (formScriptFile != null)
				{
					IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(formScriptFile));
					if (file.exists()) mappings.add(new SimpleResourceMapping(file));
				}

			}
			else if (nodeType == UserNodeType.ALL_RELATIONS)
			{
				SimpleUserNode project = simpleUserNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping relations = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.RELATIONS_DIR);
					if (relations != null) mappings.add(relations);
				}
			}
			else if (nodeType == UserNodeType.VALUELISTS)
			{
				SimpleUserNode project = simpleUserNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping valuelists = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.VALUELISTS_DIR);
					if (valuelists != null) mappings.add(valuelists);
				}
			}
			else if (nodeType == UserNodeType.MEDIA)
			{
				SimpleUserNode project = simpleUserNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping media = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.MEDIAS_DIR);
					if (media != null) mappings.add(media);
				}
			}
			else if (nodeType == UserNodeType.FORMS)
			{
				SimpleUserNode project = simpleUserNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping forms = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.FORMS_DIR);
					if (forms != null) mappings.add(forms);
				}
			}

			if (mappings.size() > 0)
			{
				SimpleResourceMapping[] aSimpleResourceMapping = mappings.toArray(new SimpleResourceMapping[0]);

				CompositeResourceMapping compositeResourceMapping = new CompositeResourceMapping(ModelProvider.RESOURCE_MODEL_PROVIDER_ID,
					aSimpleResourceMapping[0].getModelObject(), aSimpleResourceMapping);

				return compositeResourceMapping;
			}
		}
		else if (adapterType == IResource.class)
		{
			IPersist persist = null;
			SimpleUserNode userNode = (SimpleUserNode)adaptableObject;
			UserNodeType type = userNode.getType();
			if (type == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE || type == UserNodeType.SOLUTION_ITEM || userNode.isEnabled())
			{
				Object realObject = userNode.getRealObject();
				if (realObject instanceof ServoyProject)
				{
					return ((ServoyProject)realObject).getProject();
				}
				// some nodes are stored in an object array
				if (realObject instanceof Object[] && ((Object[])realObject).length > 0 && ((Object[])realObject)[0] instanceof IPersist)
				{
					realObject = ((Object[])realObject)[0];
				}
				// some nodes are stored in a Pair
				else if (realObject instanceof Pair && ((Pair< ? , ? >)realObject).getLeft() instanceof IPersist)
				{
					realObject = ((Pair< ? , ? >)realObject).getLeft();
				}
				if (realObject instanceof IPersist && !(realObject instanceof Solution) && !(realObject instanceof Style) &&
					!(realObject instanceof StringResource)) // solution is shown under ServoyProject nodes
				{
					persist = (IPersist)realObject;
				}
			}

			if (persist != null)
			{
				if (type != UserNodeType.SOLUTION && type != UserNodeType.SOLUTION_ITEM && type != UserNodeType.RELATION && type != UserNodeType.FORM)
				{
					return null;
				}

				Pair<String, String> filePath = SolutionSerializer.getFilePath(persist, true);
				return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(filePath.getLeft() + filePath.getRight()));
			}

			if (type == UserNodeType.SERVERS)
			{
				SimpleResourceMapping servers = getResourceProjectResourceMapping(SolutionSerializer.DATASOURCES_DIR_NAME);
				if (servers != null) return servers.getModelObject();
			}
			else if (type == UserNodeType.STYLES)
			{
				SimpleResourceMapping styles = getResourceProjectResourceMapping(StringResourceDeserializer.STYLES_DIR_NAME);
				if (styles != null) return styles.getModelObject();
			}
			else if (type == UserNodeType.TEMPLATES)
			{
				SimpleResourceMapping templates = getResourceProjectResourceMapping(StringResourceDeserializer.TEMPLATES_DIR_NAME);
				if (templates != null) return templates.getModelObject();
			}
			else if (type == UserNodeType.USER_GROUP_SECURITY)
			{
				SimpleResourceMapping securities = getResourceProjectResourceMapping(WorkspaceUserManager.SECURITY_DIR);
				if (securities != null) return securities.getModelObject();
			}
			else if (type == UserNodeType.I18N_FILES)
			{
				SimpleResourceMapping i18n = getResourceProjectResourceMapping(EclipseMessages.MESSAGES_DIR);
				if (i18n != null) return i18n.getModelObject();
			}
			else if (type == UserNodeType.RESOURCES)
			{
				ServoyResourcesProject servoyResourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();

				if (servoyResourcesProject != null)
				{
					return servoyResourcesProject.getProject();
				}
			}
			else if (type == UserNodeType.ALL_RELATIONS)
			{
				SimpleUserNode project = userNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping relations = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.RELATIONS_DIR);
					if (relations != null) return relations.getModelObject();
				}
			}
			else if (type == UserNodeType.VALUELISTS)
			{
				SimpleUserNode project = userNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping valuelists = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.VALUELISTS_DIR);
					if (valuelists != null) return valuelists.getModelObject();
				}
			}
			else if (type == UserNodeType.MEDIA)
			{
				SimpleUserNode project = userNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping media = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.MEDIAS_DIR);
					if (media != null) return media.getModelObject();
				}
			}
			else if (type == UserNodeType.FORMS)
			{
				SimpleUserNode project = userNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					SimpleResourceMapping forms = getProjectResourceMapping(((IProjectNature)project.getRealObject()).getProject(),
						SolutionSerializer.FORMS_DIR);
					if (forms != null) return forms.getModelObject();
				}
			}
			else if (type == UserNodeType.GLOBALS_ITEM)
			{
				SimpleUserNode project = userNode.getAncestorOfType(IProjectNature.class);
				if (project != null)
				{
					// Pair<Solution, scopeName>
					Pair<Solution, String> pair = (Pair<Solution, String>)userNode.getRealObject();
					return (((IProjectNature)project.getRealObject()).getProject()).findMember(pair.getRight().toString() +
						SolutionSerializer.JS_FILE_EXTENSION);
				}
			}
		}
		else if (adapterType == Openable.class)
		{
			if (adaptableObject instanceof SimpleUserNode)
			{
				SimpleUserNode userNode = (SimpleUserNode)adaptableObject;
				switch (userNode.getType())
				{
					case SOLUTION :
						ServoyProject servoyProject = (ServoyProject)userNode.getRealObject();
						if (servoyProject != null)
						{
							return Openable.getOpenable(servoyProject.getSolution());
						}
						return null;
					case GLOBALS_ITEM :
					case GLOBAL_VARIABLES :
					case FORM :
					case FORM_CONTROLLER :
					case FORM_VARIABLES :
						return Openable.getOpenable(userNode.getRealObject());

					default :
				}
			}
		}
		else if (adapterType == IProject.class)
		{
			if (adaptableObject instanceof SimpleUserNode)
			{
				Object realObject = ((SimpleUserNode)adaptableObject).getRealObject();
				if (realObject instanceof ServoyProject)
				{
					return ((ServoyProject)realObject).getProject();
				}
			}
		}
		else if (adapterType == IWorkbenchAdapter.class)
		{
			if (adaptableObject instanceof SimpleUserNode)
			{
				IResource resource = (IResource)Platform.getAdapterManager().getAdapter(adaptableObject, IResource.class);
				if (resource != null) return Platform.getAdapterManager().getAdapter(resource, IWorkbenchAdapter.class);
			}
		}

		return null;
	}

	private SimpleResourceMapping getResourceProjectResourceMapping(String resourceDir)
	{
		ServoyResourcesProject servoyResourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();

		if (servoyResourcesProject != null)
		{
			IProject project = servoyResourcesProject.getProject();

			return getProjectResourceMapping(project, resourceDir);
		}

		return null;
	}

	private SimpleResourceMapping getProjectResourceMapping(IProject project, String resourceDir)
	{
		if (project.exists())
		{
			IFolder resourceDirFolder = (IFolder)project.findMember(resourceDir);
			if (resourceDirFolder != null) return new SimpleResourceMapping(resourceDirFolder);
		}

		return null;
	}

	public Class[] getAdapterList()
	{
		return new Class[] { ResourceMapping.class, IResource.class, Openable.class, IProject.class, IWorkbenchAdapter.class };
	}

}
