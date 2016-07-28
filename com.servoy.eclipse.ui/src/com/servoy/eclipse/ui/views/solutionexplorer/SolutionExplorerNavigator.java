/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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
import java.util.Collections;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;

/**
 * This class offers ways to navigate in the tree or list of the given solution explorer view.
 *
 * @author acostescu
 */
public class SolutionExplorerNavigator
{

	private final SolutionExplorerView solutionExplorerView;
	private IRefreshListener<SimpleUserNode> treeRefreshListener;

	/**
	 * Creates a new navigator for the given solution explorer view instance.
	 * @param solutionExplorerView the view.
	 */
	protected SolutionExplorerNavigator(SolutionExplorerView solutionExplorerView)
	{
		this.solutionExplorerView = solutionExplorerView;
	}

	/**
	 * Calculates and returns the full path of names for the given node. That means a list with that names of all parents (up to tree root) including the node itself.
	 *
	 * @param node the node to get the path for. If null, an empty list will be returned.
	 * @return the full path of names.
	 */
	public static ArrayList<String> getNamePathInTree(SimpleUserNode node)
	{
		ArrayList<String> namePath = new ArrayList<>();
		if (node == null) return namePath;

		SimpleUserNode n = node;
		do
		{
			namePath.add(n.getName());
			n = n.parent;
		}
		while (n != null);
		namePath.remove(namePath.size() - 1); // remove the invisible root node of SolEx tree content provider as it would break the path

		Collections.reverse(namePath);
		return namePath;
	}

	/**
	 * Same as {@link #getNamePathInTree(SimpleUserNode)} but it adds to the end of the full path the given subpath.<br/>
	 * If node is null and subpath is null, it will return an empty array.
	 */
	protected static String[] getNamePathInTree(SimpleUserNode node, String[] subPath)
	{
		ArrayList<String> namePath = getNamePathInTree(node);
		if (subPath != null)
		{
			for (String name : subPath)
				namePath.add(name);
		}

		return namePath.toArray(new String[namePath.size()]);
	}

	/**
	 * Expands and optionally selects a node in the solution explorer view's tree. Either startNode or subPath must point to something. You must be runnning in SWT event thread to call this method.
	 *
	 * @param startNode the node to start at; if null it will start at root level.
	 * @param subPath an array of node names representing a path below the given startNode to expand; if null only the startNode will be expanded; elements in subPath must not be null.
	 * @param select if true, the last node in the sub-path will also be selected (or if subpath is null then startNode will be selected)
	 *
	 * @return true if the target element was revealed and selected. False otherwise. Even if it returns false, it will reveal (but not select) the most inner element found using the given criteria.
	 */
	public boolean reveaInTree(SimpleUserNode startNode, String[] subPath, boolean select)
	{
		return revealInTree(getNamePathInTree(startNode, subPath), select);
	}

	/**
	 * Same as {@link #reveaInTree(SimpleUserNode, String[], boolean)} but the target node is specified by a full String name path (so names of nodes from the root of the tree up to the target node).
	 *
	 * @param fullNamePath see {@link #reveaInTree(SimpleUserNode, String[], boolean)}.
	 * @param select see {@link #reveaInTree(SimpleUserNode, String[], boolean)}.
	 * @return see {@link #reveaInTree(SimpleUserNode, String[], boolean)}
	 */
	public boolean revealInTree(String[] fullNamePath, boolean select)
	{
		if (fullNamePath == null || fullNamePath.length == 0)
		{
			ServoyLog.logError(new IllegalArgumentException("Cannot reveal 'empty path' in Solex Tree."));
			return false;
		}

		final TreeViewer treeViewer = solutionExplorerView.getTreeViewer();
		SolutionExplorerTreeContentProvider treeContentProvider = solutionExplorerView.getTreeContentProvider();

		boolean foundTargetNode = false;

		Object[] children = treeContentProvider.getElements(treeViewer.getInput());

		SimpleUserNode endNode = null;
		int idxInFullPath = 0;
		boolean foundChild;

		while (children != null && children.length > 0 && idxInFullPath < fullNamePath.length)
		{
			foundChild = false;
			for (int i = 0; i < children.length && !foundChild; i++)
				if (children[i] instanceof SimpleUserNode && fullNamePath[idxInFullPath].equals(((SimpleUserNode)children[i]).getName()))
				{
					endNode = (SimpleUserNode)children[i];
					foundChild = true;
				}

			if (foundChild)
			{
				idxInFullPath++;
				if (treeContentProvider.hasChildren(endNode))
				{
					treeViewer.setExpandedState(endNode, true);

					// if this node is in the process of being refreshed (so it has children but they are not yet cached) it's fine, just don't reveal it now;
					// we called setExpandedState(..., true) already so next time it tries to reveal this path it will go one step further
					//children = treeContentProvider.getChildren(endNode);
					children = endNode.children;
				}
				else children = null;
			}
			else children = null;
		}

		if (endNode != null && fullNamePath.length == idxInFullPath)
		{
			foundTargetNode = true;
			final SimpleUserNode fEndNode = endNode;
			// without the async Exec the node is not yet really expanded in the UI (although we did call expand) or something - it won't select
			Display.getCurrent().asyncExec(new Runnable()
			{

				@Override
				public void run()
				{
					treeViewer.setSelection(new StructuredSelection(fEndNode), true);
				}

			});
		}

		return foundTargetNode;
	}

	/**
	 * Similar to {@link #reveaInTree(SimpleUserNode, String[], boolean)}. But if the node is not available right away it will listen to tree changes
	 * and it will reveal the target node later - when it becomes available. The next time this method is called, any previous not-yet-revealed target is discarded.<br/><br/>
	 *
	 * It will reveal (but not select) the most inner element found using the given criteria even if the target node is not found (but will continue to watch for tree change until it is found).
	 *
	 * @param startNode the node to start at; if null it will start at root level.
	 * @param subPath an array of node names representing a path below the given startNode to expand; if null only the startNode will be expanded; elements in subPath must not be null.
	 * @param select if true, the last node in the sub-path will also be selected (or if subpath is null then startNode will be selected)
	 */
	public void revealWhenAvailable(SimpleUserNode startNode, String[] subPath, final boolean select)
	{
		final String[] fullNamePath = getNamePathInTree(startNode, subPath);
		solutionExplorerView.getSite().getShell().getDisplay().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (treeRefreshListener != null)
				{
					solutionExplorerView.removeTreeRefreshListener(treeRefreshListener);
					treeRefreshListener = null;
				}

				// try to reveal right away
				if (!revealInTree(fullNamePath, select))
				{
					// try to reveal later when tree gets refreshed
					treeRefreshListener = new IRefreshListener<SimpleUserNode>()
					{

						@Override
						public void treeNodeRefreshed(RefreshEvent<SimpleUserNode> refreshEvent)
						{
							if (revealInTree(fullNamePath, select))
							{
								solutionExplorerView.removeTreeRefreshListener(treeRefreshListener);
								treeRefreshListener = null;
							}
						}
					};
					solutionExplorerView.addTreeRefreshListener(treeRefreshListener);
				}
			}
		});
	}

}
