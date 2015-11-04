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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.eclipse.ui.labelproviders.CombinedTreeLabelProvider;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.j2db.util.Utils;

/**
 * Combine 2 or more tree content providers into 1 - optionally adding a parent node for each content provider (for more visual structure).
 *
 * @author rgansevles
 * @author acostescu
 */
public class CombinedTreeContentProvider implements ITreeContentProvider, IKeywordChecker, ISearchKeyAdapter
{

	public static final Object NONE = new Object()
	{
		@Override
		public String toString()
		{
			return "CombinedTreeContentProvider.NONE";
		}
	};

	private final ITreeContentProvider[] contentProviders;
	private ArrayList<GroupingNode> groupingNodes;
	private final Object lastInput = null;
	private final int id;

	/**
	 * @param id as {@link CombinedTreeContentProvider}s can be nested, they need to know which node belongs to which level (especially {@link CombinedTreeLabelProvider}s need that). So this is a random (unique in the tree) id that should be the same for corresponding content providers and label providers.
	 */
	public CombinedTreeContentProvider(int id, ITreeContentProvider[] contentProviders)
	{
		this.contentProviders = contentProviders;
		this.id = id;
		if (contentProviders == null) throw new IllegalArgumentException("contentProviders must not be null");
	}

	public Object[] getElements(Object inputElement)
	{
		boolean includeNone = false;
		Object[] inputElements = null;

		if (!Utils.equalObjects(inputElement, lastInput))
		{
			groupingNodes = null;
			if (inputElement instanceof CombinedTreeOptions)
			{
				CombinedTreeOptions input = (CombinedTreeOptions)inputElement;
				includeNone = input.includeNone;

				if (input.groupingNodesTexts != null && contentProviders.length != input.groupingNodesTexts.length) throw new IllegalArgumentException(
					"contentProviders must have the same size as groupingNodes (if available): (" + contentProviders.length + "," +
						input.groupingNodesTexts.length + ")");

				if (input.groupingNodesTexts != null)
				{
					groupingNodes = new ArrayList<GroupingNode>(input.groupingNodesTexts.length);
					for (int i = 0; i < input.groupingNodesTexts.length; i++)
					{
						GroupingNode groupingNode = new GroupingNode(input.groupingNodesTexts[i], i, input.inputElements[i], id);
						groupingNode.children = contentProviders[i].getElements(groupingNode.inputElement);
						groupingNodes.add(groupingNode);
					}
				}
				else
				{
					inputElements = input.inputElements;
				}
			}
		}

		ArrayList<Object> rootNodes;
		if (groupingNodes != null)
		{
			// one parent node for each content provider (nice grouping of contents)
			if (includeNone)
			{
				rootNodes = new ArrayList<>(groupingNodes.size() + 1);
				rootNodes.add(NONE);
			}
			else
			{
				rootNodes = new ArrayList<>(groupingNodes.size());
			}
			rootNodes.addAll(groupingNodes);
		}
		else
		{
			// just flat merged contents
			rootNodes = new ArrayList<>();
			if (includeNone)
			{
				rootNodes.add(NONE);
			}
			for (int i = 0; i < contentProviders.length; i++)
			{
				rootNodes.addAll(Arrays.asList(contentProviders[i].getElements((inputElements != null && inputElements.length > i) ? inputElements[i] : null)));
			}
		}
		return rootNodes.toArray();
	}

	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof GroupingNode && ((GroupingNode)parentElement).id == id)
		{
			GroupingNode groupingNode = (GroupingNode)parentElement;
//			groupingNode.children = contentProviders[groupingNode.idx].getElements(groupingNode.inputElement);
			return groupingNode.children;
		}
		else
		{
			ArrayList<Object> childNodes = new ArrayList<>();
			for (ITreeContentProvider contentProvider : contentProviders)
			{
				Object[] children = contentProvider.getChildren(parentElement);
				if (children != null) childNodes.addAll(Arrays.asList(children));
			}
			return childNodes.toArray();
		}
	}

	public Object getParent(Object element)
	{
		if (element != NONE && !(element instanceof GroupingNode && ((GroupingNode)element).id == id))
		{
			for (ITreeContentProvider contentProvider : contentProviders)
			{
				Object parent = contentProvider.getParent(element);
				if (parent != null) return parent;
			}

			// see if it's the child of a grouping node
			if (groupingNodes != null)
			{
				for (int i = 0; i < groupingNodes.size(); i++)
				{
					GroupingNode groupingNode = groupingNodes.get(i);
					if (groupingNode.children == null) groupingNode.children = contentProviders[i].getElements(groupingNode.inputElement); // initial selection of nested things can search for parents even for non-yet created direct child values of grouping nodes; so create those
					if (groupingNode.children != null)
					{
						for (Object childOfNode : groupingNode.children)
						{
							if (Utils.equalObjects(childOfNode, element)) return groupingNode;
						}
					}
				}
			}
		}
		return null;
	}

	public boolean hasChildren(Object element)
	{
		if (element instanceof GroupingNode && ((GroupingNode)element).id == id) return true;
		if (element == NONE) return false;


		for (ITreeContentProvider contentProvider : contentProviders)
		{
			if (contentProvider.hasChildren(element)) return true;
		}
		return false;
	}

	public void dispose()
	{
		for (ITreeContentProvider contentProvider : contentProviders)
		{
			contentProvider.dispose();
		}
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		Object[] newInputs = null;
		Object[] oldInputs = null;
		if (newInput instanceof CombinedTreeOptions)
		{
			CombinedTreeOptions newOpts = (CombinedTreeOptions)newInput;
			newInputs = newOpts.inputElements;
		}
		if (oldInput instanceof CombinedTreeOptions)
		{
			CombinedTreeOptions oldOpts = (CombinedTreeOptions)oldInput;
			oldInputs = oldOpts.inputElements;
		}

		for (int i = 0; i < contentProviders.length; i++)
		{
			contentProviders[i].inputChanged(viewer, oldInputs != null && oldInputs.length > i ? oldInputs[i] : null, newInputs != null && newInputs.length > i
				? newInputs[i] : null);
		}
	}

	public boolean isKeyword(Object element)
	{
		if ((element instanceof GroupingNode && ((GroupingNode)element).id == id) || element == NONE) return false;

		for (ITreeContentProvider contentProvider : contentProviders)
		{
			if (contentProvider instanceof IKeywordChecker && ((IKeywordChecker)contentProvider).isKeyword(element)) return true;
		}
		return false;
	}

	public Object getSearchKey(Object element)
	{
		if ((element instanceof GroupingNode && ((GroupingNode)element).id == id) || element == NONE) return null;

		for (ITreeContentProvider contentProvider : contentProviders)
		{
			if (contentProvider instanceof ISearchKeyAdapter)
			{
				Object searchKey = ((ISearchKeyAdapter)contentProvider).getSearchKey(element);
				if (searchKey != null) return searchKey;
			}
		}
		return null;
	}

	public static class GroupingNode
	{
		public final String groupingNodeText;
		public Object inputElement;
		public Object[] children;
		public final int idx;
		public final int id;

		public GroupingNode(String groupingNodeText, int idx, Object inputElement, int id)
		{
			this.groupingNodeText = groupingNodeText;
			this.idx = idx;
			this.inputElement = inputElement;
			this.id = id;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((groupingNodeText == null) ? 0 : groupingNodeText.hashCode());
			result = prime * result + id;
			result = prime * result + idx;
			result = prime * result + ((inputElement == null) ? 0 : inputElement.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			GroupingNode other = (GroupingNode)obj;
			if (groupingNodeText == null)
			{
				if (other.groupingNodeText != null) return false;
			}
			else if (!groupingNodeText.equals(other.groupingNodeText)) return false;
			if (id != other.id) return false;
			if (idx != other.idx) return false;
			if (inputElement == null)
			{
				if (other.inputElement != null) return false;
			}
			else if (!inputElement.equals(other.inputElement)) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "GroupingNode [groupingNodeText=" + groupingNodeText + ", idx=" + idx + ", id=" + id + "]";
		}

	}

	public static class CombinedTreeOptions
	{
		public final String[] groupingNodesTexts;
		public final Object[] inputElements;
		public final boolean includeNone;

		/**
		 * @param groupingNodesTexts if the tree should show separate parent nodes for each content provider, you should set this with the corresponding text of each node.
		 * If you want the content to be merged side-by-side just use null for this arg.
		 */
		public CombinedTreeOptions(String[] groupingNodesTexts, boolean includeNone, Object[] inputElements)
		{
			this.groupingNodesTexts = groupingNodesTexts;
			this.includeNone = includeNone;
			this.inputElements = inputElements;
		}
	}

}
