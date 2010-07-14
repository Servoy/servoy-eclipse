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
package com.servoy.eclipse.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.servoy.eclipse.ui.util.ContentProposal;

/**
 * ContentProposalProvider that creates proposals based on the elements created by a tree content provider, the elements are formatted using a label provider.
 * 
 * @author rgansevles
 *
 */
public class ContentLabelProviderProposalProvider implements IContentProposalProvider
{
	private final Object input;
	private final ITreeContentProvider contentProvider;
	private final ILabelProvider labelProvider;

	public ContentLabelProviderProposalProvider(Object input, ITreeContentProvider contentProvider, ILabelProvider labelProvider)
	{
		this.input = input;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
	}

	public IContentProposal[] getProposals(String contents, int position)
	{
		List<ContentProposal> proposals = getProposalList(contents, true);
		return proposals.toArray(new IContentProposal[proposals.size()]);
	}

	protected List<ContentProposal> getProposalList(String contents, boolean includePartialMatches)
	{
		List<ContentProposal> proposals = new ArrayList<ContentProposal>();
		Object[] elements = contentProvider.getElements(input);
		if (elements != null)
		{
			for (Object element : elements)
			{
				addProposals(proposals, contents, includePartialMatches, element);
			}
		}
		return proposals;
	}

	protected void addProposals(List<ContentProposal> proposals, String contents, boolean includePartialMatches, Object element)
	{
		String text = labelProvider.getText(element);
		if (text != null)
		{
			if (text.startsWith(contents))
			{
				if (includePartialMatches || text.equals(contents))
				{
					proposals.add(new ContentProposal(text, text.length(), null, null, element));
				}
			}
			else if (contents.startsWith(text))
			{
				Object[] children = contentProvider.getChildren(element);
				if (children != null)
				{
					for (Object child : children)
					{
						addProposals(proposals, contents, includePartialMatches, child);
					}
				}
			}
		}
	}

	/**
	 * Get the input that matches the contents according to the content provider and the label provider.
	 * @param contents
	 * @return
	 */
	public Object determineValue(String contents)
	{
		for (ContentProposal proposal : getProposalList(contents, false))
		{
			if (contents.equals(proposal.getContent()))
			{
				return proposal.getInput();
			}
		}
		return null;
	}
}
