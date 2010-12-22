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

import java.io.File;
import java.rmi.RemoteException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
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
		if (activeEditor != null && contentProvider instanceof SolutionExplorerTreeContentProvider)
		{
			UUID uuid = null;
			IPersist persist = (IPersist)activeEditor.getAdapter(IPersist.class);
			IFile file = null;
			if (persist != null)
			{
				uuid = persist.getUUID();
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			}
			else
			{
				file = (IFile)activeEditor.getEditorInput().getAdapter(IFile.class);
				if (file != null)
				{
					File f = file.getRawLocation().toFile();
					File workspace = file.getWorkspace().getRoot().getLocation().toFile();
					File parentFile = SolutionSerializer.getParentFile(workspace, f);
					if (parentFile != null)
					{
						uuid = SolutionDeserializer.getUUID(parentFile);
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
					String serverName = null;
					Table table = null;
					ServerConfig config = (ServerConfig)activeEditor.getAdapter(ServerConfig.class);
					if (config == null)
					{
						table = (Table)activeEditor.getAdapter(Table.class);
						if (table != null)
						{
							serverName = table.getServerName();
						}
					}
					else
					{
						serverName = config.getServerName();
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
										if (table != null)
										{
											Object[] elements = ((IStructuredContentProvider)list.getContentProvider()).getElements(list.getInput());
											if (elements != null)
											{
												for (Object element : elements)
												{
													Object realObject = ((SimpleUserNode)element).getRealObject();
													if (realObject instanceof TableWrapper && ((TableWrapper)realObject).getTableName().equals(table.getName()))
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
				}
			}
			if (uuid != null)
			{
				TreePath path = ((SolutionExplorerTreeContentProvider)contentProvider).getTreePath(uuid);
				if (path != null)
				{
					TreePath realPath = path;
					tree.expandToLevel(path.getLastSegment(), 1);
					if (path.getLastSegment() instanceof SimpleUserNode)
					{
						SimpleUserNode node = (SimpleUserNode)path.getLastSegment();
						if (node.children != null && node.children.length > 0)
						{
							path = path.createChildPath(node.children[node.children.length - 1]);
						}
					}
					tree.setSelection(new TreeSelection(path), true);
					if (realPath != path)
					{
						tree.setSelection(new TreeSelection(realPath), true);
					}
				}
				else if (persist instanceof ValueList)
				{
					Solution solution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
					if (solution != null)
					{
						PlatformSimpleUserNode solutionNode = ((SolutionExplorerTreeContentProvider)contentProvider).getSolutionNode(solution.getName());
						if (solutionNode != null && solutionNode.children != null)
						{
							for (SimpleUserNode child : solutionNode.children)
							{
								if (child.getType() == UserNodeType.VALUELISTS)
								{
									tree.setSelection(new StructuredSelection(child), true);
									Object[] elements = ((IStructuredContentProvider)list.getContentProvider()).getElements(list.getInput());
									if (elements != null)
									{
										for (Object element : elements)
										{
											Object realObject = ((SimpleUserNode)element).getRealObject();
											if (realObject instanceof IPersist && ((IPersist)realObject).getUUID().equals(uuid))
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
				IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IPageLayout.ID_RES_NAV);
				if (part instanceof ISetSelectionTarget && file != null)
				{
					((ISetSelectionTarget)part).selectReveal(new StructuredSelection(file));
				}
			}
		}
	}

}
