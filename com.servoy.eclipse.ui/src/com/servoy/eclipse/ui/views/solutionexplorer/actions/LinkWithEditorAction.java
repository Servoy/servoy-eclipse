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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
public class LinkWithEditorAction extends Action
{

	private final TreeViewer tree;
	private final TableViewer list;

	public LinkWithEditorAction(TreeViewer tree, TableViewer list)
	{
		this.tree = tree;
		this.list = list;
		setText("Link with Editor");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("link_to_editor.png"));
	}

	@Override
	public void run()
	{
		IContentProvider contentProvider = tree.getContentProvider();
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor == null || !(contentProvider instanceof SolutionExplorerTreeContentProvider))
		{
			return;
		}

		// find the persists from the selection
		showInSolex(contentProvider, null, activeEditor, activeEditor);
	}

	public void showInSolex(IContentProvider treeContentProvider, ISelection selectionP, IAdaptable editorOrAdaptable, IEditorPart editor)
	{
		ISelection selection;
		if (selectionP == null && editor != null)
		{
			// get selection from editor if a selection was not provided
			ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
			selection = selectionProvider == null ? null : selectionProvider.getSelection();
		}
		else selection = selectionP;

		List<IPersist> persists = new ArrayList<IPersist>();
		if (selection instanceof IStructuredSelection)
		{
			Iterator< ? > iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext())
			{
				Object obj = iterator.next();
				IPersist persist = Platform.getAdapterManager().getAdapter(obj, IPersist.class);
				if (persist instanceof BaseComponent && ((BaseComponent)persist).getName() != null)
				{
					persists.add(persist);
				}
			}
		}
		String serverName = null;
		String tableName = null;

		if (persists.size() == 0)
		{
			// none found, go via the editor
			IPersist persist = (editorOrAdaptable != null ? editorOrAdaptable.getAdapter(IPersist.class) : null);
			if (persist instanceof TableNode)
			{
				serverName = ((TableNode)persist).getServerName();
				tableName = ((TableNode)persist).getTableName();
			}
			else if (persist != null)
			{
				persists.add(persist);
			}
		}

		Map<UUID, IFile> files = new HashMap<UUID, IFile>();

		if (persists.size() > 0)
		{
			for (IPersist persist : persists)
			{
				UUID uuid = persist.getUUID();
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (file != null)
				{
					files.put(uuid, file);
				}
			}
		}
		else if (serverName == null)
		{
			IFile file = (editor != null ? editor.getEditorInput().getAdapter(IFile.class) : null);
			if (file != null)
			{
				// globals, scope or foundset
				if (file.getName().endsWith(".js"))
				{
					IContainer parent = file.getParent();
					if (parent instanceof IProject)
					{
						String name = file.getName().substring(0, file.getName().indexOf('.'));
						// globals or scope
						PlatformSimpleUserNode solutionNode = ((SolutionExplorerTreeContentProvider)treeContentProvider).getSolutionNode(parent.getName());
						if (solutionNode.children == null)
						{
							// subtree is lazy loaded and currently oppened js file in editor might not be loaded in the Solex tree
							// load modules subtree
							((SolutionExplorerTreeContentProvider)treeContentProvider).getChildren(solutionNode);
						}
						for (SimpleUserNode node : solutionNode.children)
						{
							if (node.getName().equals(Messages.TreeStrings_Scopes))
							{

								for (SimpleUserNode scopeChild : node.children)
								{
									if (scopeChild.getName().equals(name))
									{
										tree.setSelection(new StructuredSelection(new Object[] { scopeChild }), true);
										break;
									}
								}
								break;
							}
						}
					}
				}
				else if (file.getName().endsWith(".css"))
				{
					PlatformSimpleUserNode styleNode = ((SolutionExplorerTreeContentProvider)treeContentProvider).getStylesNode();
					tree.setSelection(new StructuredSelection(styleNode), true);
					Object[] elements = ((IStructuredContentProvider)list.getContentProvider()).getElements(list.getInput());
					if (elements != null)
					{
						String styleName = file.getName().substring(0, file.getName().length() - 4);
						for (Object element : elements)
						{
							Object realObject = ((SimpleUserNode)element).getRealObject();
							if (realObject instanceof Style && ((Style)realObject).getName().equals(styleName))
							{
								list.setSelection(new StructuredSelection(element), true);
								break;
							}
						}
					}
				}
				if (file.getName().endsWith(".spec") || file.getName().endsWith(".js") || file.getName().endsWith(".html") ||
					file.getName().endsWith(".json") || file.getName().endsWith(".css"))
				{
					if (file.getParent() != null && file.getParent().getParent() != null)
					{
						setProperWebObjectSelection(file, (SolutionExplorerTreeContentProvider)treeContentProvider);
					}
				}
				if ("MANIFEST.MF".equals(file.getName()) && file.getParent() != null && file.getParent().getParent() != null)
				{
					setProperWebObjectSelection(file, (SolutionExplorerTreeContentProvider)treeContentProvider);
				}
			}
			else
			{
				ServerConfig config = editorOrAdaptable.getAdapter(ServerConfig.class);
				if (config == null)
				{
					Table table = editorOrAdaptable.getAdapter(Table.class);
					if (table != null)
					{
						serverName = table.getServerName();
						tableName = table.getName();
					}
				}
				else
				{
					serverName = config.getServerName();
				}
			}
		}
		if (serverName != null)
		{
			PlatformSimpleUserNode servers = ((SolutionExplorerTreeContentProvider)treeContentProvider).getServers();
			SimpleUserNode[] children = (SimpleUserNode[])((SolutionExplorerTreeContentProvider)treeContentProvider).getChildren(servers);
			if (children != null)
			{
				for (SimpleUserNode child : children)
				{
					try
					{
						if (serverName.equals(((IServer)child.getRealObject()).getName()))
						{
							tree.setSelection(new TreeSelection(new TreePath(new Object[] { servers, child })), true);
							if (tableName != null)
							{
								Object[] elements = ((IStructuredContentProvider)list.getContentProvider()).getElements(list.getInput());
								if (elements != null)
								{
									for (Object element : elements)
									{
										Object realObject = ((SimpleUserNode)element).getRealObject();
										if (realObject instanceof TableWrapper && ((TableWrapper)realObject).getTableName().equals(tableName))
										{
											list.setSelection(new StructuredSelection(element), true);
											break;
										}
									}
								}

							}
							break;
						}
					}
					catch (RemoteException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		if (persists.size() == 1 && persists.get(0) instanceof Relation)
		{
			setProperSelection(persists.get(0), UserNodeType.ALL_RELATIONS, treeContentProvider);
		}
		else if (persists.size() == 1 && persists.get(0) instanceof ValueList)
		{
			setProperSelection(persists.get(0), UserNodeType.VALUELISTS, treeContentProvider);
		}
		else if (persists.size() == 1 && persists.get(0) instanceof Media)
		{
			setProperSelection(persists.get(0), UserNodeType.MEDIA, treeContentProvider);
		}
		else if (files.size() > 0)
		{
			List<TreePath> paths = new ArrayList<TreePath>();
			for (Map.Entry<UUID, IFile> entry : files.entrySet())
			{
				TreePath path = ((SolutionExplorerTreeContentProvider)treeContentProvider).getTreePath(entry.getKey());
				if (path != null)
				{
					tree.expandToLevel(path, 1);
					paths.add(path);
				}
				else
				{
					ServoyLog.log(IStatus.WARNING, IStatus.OK, "Could not find tree path for : " + entry, null);
				}
			}
			tree.setSelection(new TreeSelection(paths.toArray(new TreePath[paths.size()])), true);
		}

		if (files.size() > 0)
		{
			IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IPageLayout.ID_RES_NAV);
			if (part instanceof ISetSelectionTarget)
			{
				((ISetSelectionTarget)part).selectReveal(new StructuredSelection(files.values().toArray()));
			}
		}
	}

	private void setProperWebObjectSelection(IFile file, SolutionExplorerTreeContentProvider treeContentProvider)
	{
		String packageName = file.getParent().getParent().getName();
		String webObjectName = file.getName().endsWith(".MF") ? null : file.getParent().getName();

		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		Solution activeSolution = activeProject != null ? activeProject.getSolution() : null;

		boolean selected = false;
		if (activeSolution != null)
		{
			PlatformSimpleUserNode solutionNode = treeContentProvider.getSolutionNode(activeSolution.getName());
			for (Object obj : treeContentProvider.getChildren(solutionNode))
			{
				if (obj instanceof PlatformSimpleUserNode &&
					((PlatformSimpleUserNode)obj).getRealType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES)
				{
					selected = selectWebObjectNode(treeContentProvider, packageName, webObjectName, obj, file);
					break;
				}
			}
		}
		if (!selected)
		{
			PlatformSimpleUserNode allPackages = treeContentProvider.getAllWebPackagesNode();
			selectWebObjectNode(treeContentProvider, packageName, webObjectName, allPackages, file);
		}

	}

	private boolean selectWebObjectNode(SolutionExplorerTreeContentProvider treeContentProvider, String packageName, String webObjectName, Object obj,
		IFile file)
	{
		for (Object o : treeContentProvider.getChildren(obj))
		{
			if (o instanceof PlatformSimpleUserNode)
			{
				PlatformSimpleUserNode packageNode = (PlatformSimpleUserNode)o;
				if (packageNode.getName().equals(packageName) ||
					packageNode.getRealObject() instanceof IPackageReader && ((IPackageReader)packageNode.getRealObject()).getPackageName().equals(packageName))
				{
					if (webObjectName == null)
					{
						tree.setSelection(new StructuredSelection(packageNode), true);
						return true;
					}
					for (Object ob : treeContentProvider.getChildren(packageNode))
					{
						if (ob instanceof PlatformSimpleUserNode)
						{
							PlatformSimpleUserNode webObjectNode = (PlatformSimpleUserNode)ob;
							if (webObjectNode.getRealObject() instanceof WebObjectSpecification)
							{
								WebObjectSpecification spec = ((WebObjectSpecification)webObjectNode.getRealObject());
								String[] n = spec.getName().split("-");
								if (n.length == 2 && webObjectName.equals(n[1]))
								{
									tree.setSelection(new StructuredSelection(webObjectNode), true);
									Object[] elements = ((IStructuredContentProvider)list.getContentProvider()).getElements(list.getInput());
									if (elements != null)
									{
										for (int i = 0; i < elements.length; i++)
										{
											Object element = elements[i];
											if (((SimpleUserNode)element).getName().equals(file.getName()))
											{
												list.getTable().setSelection(new int[] { i });
												list.getTable().showSelection();
												break;
											}
										}
									}
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private void setProperSelection(IPersist persist, UserNodeType persistType, IContentProvider contentProvider)
	{
		Solution solution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
		if (solution != null)
		{
			PlatformSimpleUserNode solutionNode = ((SolutionExplorerTreeContentProvider)contentProvider).getSolutionNode(solution.getName());
			if (solutionNode != null && solutionNode.children != null)
			{
				for (SimpleUserNode child : solutionNode.children)
				{
					if (child.getType() == persistType)
					{
						tree.setSelection(new StructuredSelection(child), true);
						Object[] elements = ((IStructuredContentProvider)list.getContentProvider()).getElements(list.getInput());
						if (elements != null)
						{
							for (Object element : elements)
							{
								Object realObject = ((SimpleUserNode)element).getRealObject();
								if (realObject instanceof IPersist && ((IPersist)realObject).getUUID().equals(persist.getUUID()))
								{
									list.setSelection(new StructuredSelection(element), true);
									break;
								}
							}
						}
						break;
					}
				}
			}
		}
	}

}
