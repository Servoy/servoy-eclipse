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

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.CompositeResourceMapping;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.internal.core.SourceMethod;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.actions.Openable;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ScriptMethod;
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
			else if (nodeType == UserNodeType.COMPONENTS_FROM_RESOURCES)
			{
				SimpleResourceMapping components = getResourceProjectResourceMapping(SolutionSerializer.COMPONENTS_DIR_NAME);
				if (components != null) mappings.add(components);
			}
			else if (nodeType == UserNodeType.SERVICES_FROM_RESOURCES)
			{
				SimpleResourceMapping services = getResourceProjectResourceMapping(SolutionSerializer.SERVICES_DIR_NAME);
				if (services != null) mappings.add(services);
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
			else if (nodeType == UserNodeType.SERVER && simpleUserNode.getRealObject() instanceof IServerInternal)
			{
				IServerInternal server = (IServerInternal)simpleUserNode.getRealObject();
				SimpleResourceMapping serverResourceMapping = getResourceProjectResourceMapping(
					SolutionSerializer.DATASOURCES_DIR_NAME + IPath.SEPARATOR + server.getName());
				if (serverResourceMapping != null) mappings.add(serverResourceMapping);
			}
			else if (nodeType == UserNodeType.TABLE && simpleUserNode.getRealObject() instanceof TableWrapper)
			{
				TableWrapper table = (TableWrapper)simpleUserNode.getRealObject();
				SimpleResourceMapping tableResourceMapping = getResourceProjectResourceMapping(SolutionSerializer.DATASOURCES_DIR_NAME + IPath.SEPARATOR +
					table.getServerName() + IPath.SEPARATOR + table.getTableName() + DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT);
				if (tableResourceMapping != null) mappings.add(tableResourceMapping);
			}
			else if (nodeType == UserNodeType.USER_GROUP_SECURITY)
			{
				SimpleResourceMapping securities = getResourceProjectResourceMapping(DataModelManager.SECURITY_DIRECTORY);
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
				mappings.add(
					new SimpleResourceMapping(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(formFilePath.getLeft() + formFilePath.getRight()))));
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
			else if (nodeType == UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE || nodeType == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE ||
				nodeType == UserNodeType.COMPONENTS_PROJECT_PACKAGE || nodeType == UserNodeType.SERVICES_NONPROJECT_PACKAGE ||
				nodeType == UserNodeType.SERVICES_PROJECT_PACKAGE || nodeType == UserNodeType.LAYOUT_NONPROJECT_PACKAGE ||
				nodeType == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				mappings.add(new SimpleResourceMapping(SolutionExplorerTreeContentProvider.getResource((IPackageReader)simpleUserNode.getRealObject())));
			}
			else if (nodeType == UserNodeType.ALL_WEB_PACKAGE_PROJECTS)
			{
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject iProject : projects)
				{
					try
					{
						if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
						{
							mappings.add(new SimpleResourceMapping(iProject));
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			else if (nodeType == UserNodeType.COMPONENT || nodeType == UserNodeType.LAYOUT || nodeType == UserNodeType.SERVICE)
			{
				WebObjectSpecification spec = (WebObjectSpecification)simpleUserNode.getRealObject();
				if ("file".equals(spec.getSpecURL().getProtocol()))
				{
					try
					{
						IFile specFile = ResourcesUtils.findFileWithShortestPathForLocationURI(spec.getSpecURL().toURI());
						if (specFile != null)
						{
							mappings.add(new SimpleResourceMapping(specFile.getParent())); // here we assume all files in a component are nicely placed in the parent dir. of the spec file; for other path usages inside web packages this won't work well
						}
					}
					catch (URISyntaxException e)
					{
						ServoyLog.logError(e);
					}
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
			else if (type == UserNodeType.SERVER && userNode.getRealObject() instanceof IServerInternal)
			{
				IServerInternal server = (IServerInternal)userNode.getRealObject();
				SimpleResourceMapping serverResourceMapping = getResourceProjectResourceMapping(
					SolutionSerializer.DATASOURCES_DIR_NAME + IPath.SEPARATOR + server.getName());
				if (serverResourceMapping != null) return serverResourceMapping.getModelObject();
			}
			else if (type == UserNodeType.TABLE && userNode.getRealObject() instanceof TableWrapper)
			{
				TableWrapper table = (TableWrapper)userNode.getRealObject();
				SimpleResourceMapping tableResourceMapping = getResourceProjectResourceMapping(SolutionSerializer.DATASOURCES_DIR_NAME + IPath.SEPARATOR +
					table.getServerName() + IPath.SEPARATOR + table.getTableName() + DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT);
				if (tableResourceMapping != null) return tableResourceMapping.getModelObject();
			}
			else if (type == UserNodeType.STYLES)
			{
				SimpleResourceMapping styles = getResourceProjectResourceMapping(StringResourceDeserializer.STYLES_DIR_NAME);
				if (styles != null) return styles.getModelObject();
			}
			else if (type == UserNodeType.COMPONENTS_FROM_RESOURCES)
			{
				SimpleResourceMapping components = getResourceProjectResourceMapping(SolutionSerializer.COMPONENTS_DIR_NAME);
				if (components != null) return components.getModelObject();
			}
			else if (type == UserNodeType.SERVICES_FROM_RESOURCES)
			{
				SimpleResourceMapping services = getResourceProjectResourceMapping(SolutionSerializer.SERVICES_DIR_NAME);
				if (services != null) return services.getModelObject();
			}
			else if (type == UserNodeType.TEMPLATES)
			{
				SimpleResourceMapping templates = getResourceProjectResourceMapping(StringResourceDeserializer.TEMPLATES_DIR_NAME);
				if (templates != null) return templates.getModelObject();
			}
			else if (type == UserNodeType.USER_GROUP_SECURITY)
			{
				SimpleResourceMapping securities = getResourceProjectResourceMapping(DataModelManager.SECURITY_DIRECTORY);
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
					return (((IProjectNature)project.getRealObject()).getProject()).findMember(
						pair.getRight().toString() + SolutionSerializer.JS_FILE_EXTENSION);
				}
			}
			else if (type == UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE || type == UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE ||
				type == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE || type == UserNodeType.COMPONENTS_PROJECT_PACKAGE ||
				type == UserNodeType.SERVICES_NONPROJECT_PACKAGE || type == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				type == UserNodeType.LAYOUT_NONPROJECT_PACKAGE || type == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				Object realObject = userNode.getRealObject();
				if (realObject instanceof IResource) return realObject;
				return SolutionExplorerTreeContentProvider.getResource((IPackageReader)realObject);
			}
			else if (type == UserNodeType.COMPONENT || type == UserNodeType.LAYOUT || type == UserNodeType.SERVICE)
			{
				WebObjectSpecification spec = (WebObjectSpecification)userNode.getRealObject();
				if ("file".equals(spec.getSpecURL().getProtocol()))
				{
					try
					{
						IFile specFile = ResourcesUtils.findFileWithShortestPathForLocationURI(spec.getSpecURL().toURI());
						if (specFile != null)
						{
							return specFile.getParent(); // here we assume all files in a component are nicely placed in the parent dir. of the spec file; for other path usages inside web packages this won't work well
						}
					}
					catch (URISyntaxException e)
					{
						ServoyLog.logError(e);
					}
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
					case FORM_METHOD :
					case FORM_ELEMENTS_ITEM :
						return Openable.getOpenable(userNode.getRealObject());
					default :
				}
			}
			if (adaptableObject instanceof ScriptMethod || adaptableObject instanceof SourceMethod)
			{
				return Openable.getOpenable(adaptableObject);
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
				else if (realObject instanceof IProject)
				{
					return realObject;
				}
			}
		}
		else if (adapterType == IWorkbenchAdapter.class)
		{
			if (adaptableObject instanceof SimpleUserNode)
			{
				IResource resource = Platform.getAdapterManager().getAdapter(adaptableObject, IResource.class);
				if (resource != null) return Platform.getAdapterManager().getAdapter(resource, IWorkbenchAdapter.class);
			}
		}

		return null;
	}

	private SimpleResourceMapping getResourceProjectResourceMapping(String resource)
	{
		ServoyResourcesProject servoyResourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();

		if (servoyResourcesProject != null)
		{
			IProject project = servoyResourcesProject.getProject();

			return getProjectResourceMapping(project, resource);
		}

		return null;
	}

	private SimpleResourceMapping getProjectResourceMapping(IProject project, String resource)
	{
		if (project.exists())
		{
			IResource res = project.findMember(resource);
			if (res != null) return new SimpleResourceMapping(res);
		}

		return null;
	}

	public Class[] getAdapterList()
	{
		return new Class[] { ResourceMapping.class, IResource.class, Openable.class, IProject.class, IWorkbenchAdapter.class };
	}

}
