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

	/**
	 * @param tree
	 */
	public LinkWithEditorAction(TreeViewer tree, TableViewer list)
	{
		this.tree = tree;
		this.list = list;
		setText("Link with Editor");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("synced.gif"));//$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
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
		List<IPersist> persists = new ArrayList<IPersist>();
		ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
		ISelection selection = selectionProvider == null ? null : selectionProvider.getSelection();
		if (selection instanceof IStructuredSelection)
		{
			Iterator< ? > iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext())
			{
				Object obj = iterator.next();
				IPersist persist = (IPersist)Platform.getAdapterManager().getAdapter(obj, IPersist.class);
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
			IPersist persist = (IPersist)activeEditor.getAdapter(IPersist.class);
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
			IFile file = (IFile)activeEditor.getEditorInput().getAdapter(IFile.class);
			if (file != null)
			{
				// globals, scope or foundset
				if (file.getName().endsWith(".js")) //$NON-NLS-1$
				{
					IContainer parent = file.getParent();
					if (parent instanceof IProject)
					{
						String name = file.getName().substring(0, file.getName().indexOf('.'));
						// globals or scope
						PlatformSimpleUserNode solutionNode = ((SolutionExplorerTreeContentProvider)contentProvider).getSolutionNode(parent.getName());
						if (solutionNode.children == null)
						{
							// subtree is lazy loaded and currently oppened js file in editor might not be loaded in the Solex tree
							// load modules subtree
							((SolutionExplorerTreeContentProvider)contentProvider).getChildren(solutionNode);
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
				else if (file.getName().endsWith(".css")) //$NON-NLS-1$
				{
					PlatformSimpleUserNode styleNode = ((SolutionExplorerTreeContentProvider)contentProvider).getStylesNode();
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
			}
			else
			{
				ServerConfig config = (ServerConfig)activeEditor.getAdapter(ServerConfig.class);
				if (config == null)
				{
					Table table = (Table)activeEditor.getAdapter(Table.class);
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
			PlatformSimpleUserNode servers = ((SolutionExplorerTreeContentProvider)contentProvider).getServers();
			SimpleUserNode[] children = (SimpleUserNode[])((SolutionExplorerTreeContentProvider)contentProvider).getChildren(servers);
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
			setProperSelection(persists.get(0), UserNodeType.ALL_RELATIONS, contentProvider);
		}
		else if (persists.size() == 1 && persists.get(0) instanceof ValueList)
		{
			setProperSelection(persists.get(0), UserNodeType.VALUELISTS, contentProvider);
		}
		else if (files.size() > 0)
		{
			List<TreePath> paths = new ArrayList<TreePath>();
			for (Map.Entry<UUID, IFile> entry : files.entrySet())
			{
				TreePath path = ((SolutionExplorerTreeContentProvider)contentProvider).getTreePath(entry.getKey());
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
