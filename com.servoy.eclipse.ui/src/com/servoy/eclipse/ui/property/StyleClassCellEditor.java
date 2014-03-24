/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.property;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Cell editor used for specifying multiple css style classes (e.g. btn btn-default)
 * Field assist example:
 * http://git.eclipse.org/c/platform/eclipse.platform.ui.git/tree/examples/org.eclipse.jface.snippets/Eclipse%20JFace%20Snippets/org/eclipse/jface/snippets/viewers/Snippet060TextCellEditorWithContentProposal.java
 *
 * @author emera
 */
public class StyleClassCellEditor extends TextCellEditor
{
	private ContentProposalAdapter contentProposalAdapter;
	private boolean popupOpen = false; // true, iff popup is currently open
	private int startPosition;
	private int endPosition;

	public StyleClassCellEditor(Composite parent, StyleClassesComboboxModel model)
	{
		super(parent);
		enableContentProposal(model);
	}

	private void enableContentProposal(StyleClassesComboboxModel model)
	{
		SimpleContentProposalProvider provider = new SimpleContentProposalProvider(model.getDisplayValues())
		{
			@Override
			public IContentProposal[] getProposals(String contents, int position)
			{
				//find proposals for last inserted style class prefix
				int lastIndexOfSpace = contents.substring(0, position).lastIndexOf(" ");
				if (contents.length() > 0 && lastIndexOfSpace > 0 && lastIndexOfSpace < contents.length() - 1)
				{
					setFiltering(true);
					IContentProposal[] proposals = super.getProposals(contents.substring(lastIndexOfSpace + 1), position);
					setFiltering(false);
					return proposals;
				}
				return super.getProposals(contents, position);
			}
		};
		contentProposalAdapter = new ContentProposalAdapter(text, new TextContentAdapter(), provider, null, null);
		contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
		contentProposalAdapter.addContentProposalListener(new IContentProposalListener2()
		{
			@Override
			public void proposalPopupClosed(ContentProposalAdapter adapter)
			{
				popupOpen = false;
				endPosition = adapter.getControlContentAdapter().getCursorPosition(adapter.getControl());
			}

			@Override
			public void proposalPopupOpened(ContentProposalAdapter adapter)
			{
				popupOpen = true;

				//find start position of last inserted style prefix
				int start = adapter.getControlContentAdapter().getCursorPosition(adapter.getControl());
				int lastIndexOfSpace = text.getText().substring(0, start).lastIndexOf(" ");
				startPosition = lastIndexOfSpace > 0 ? lastIndexOfSpace + 1 : 0;
			}
		});
		contentProposalAdapter.addContentProposalListener(new IContentProposalListener()
		{
			@Override
			public void proposalAccepted(IContentProposal proposal)
			{
				IControlContentAdapter contentAdapter = contentProposalAdapter.getControlContentAdapter();
				Control control = contentProposalAdapter.getControl();
				StringBuilder sb = new StringBuilder(contentAdapter.getControlContents(control));
				sb.insert(contentAdapter.getCursorPosition(control), proposal.getContent().substring(endPosition - startPosition));
				contentAdapter.setControlContents(control, sb.toString(), startPosition + proposal.getCursorPosition());
			}
		});
	}

	/**
	 * Return the {@link ContentProposalAdapter} of this cell editor.
	 *
	 * @return the {@link ContentProposalAdapter}
	 */
	public ContentProposalAdapter getContentProposalAdapter()
	{
		return contentProposalAdapter;
	}


	@Override
	protected void focusLost()
	{
		if (!popupOpen)
		{
			// Focus lost deactivates the cell editor.
			// This must not happen if focus lost was caused by activating
			// the completion proposal popup.
			super.focusLost();
		}
	}

	@Override
	protected boolean dependsOnExternalFocusListener()
	{
		// Always return false;
		// Otherwise, the ColumnViewerEditor will install an additional focus listener
		// that cancels cell editing on focus lost, even if focus gets lost due to
		// activation of the completion proposal popup. See also bug 58777.
		return false;
	}
}
