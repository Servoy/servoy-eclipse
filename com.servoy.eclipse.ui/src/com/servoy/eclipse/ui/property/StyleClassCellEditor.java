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

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Cell editor used for specifying multiple css style classes (e.g. btn btn-default)
 * Field assist example:
 * http://git.eclipse.org/c/platform/eclipse.platform.ui.git/tree/examples/org.eclipse.jface.snippets/Eclipse%20JFace%20Snippets/org/eclipse/jface/snippets/viewers/Snippet060TextCellEditorWithContentProposal.java
 *
 * @author emera
 */
public class StyleClassCellEditor extends TextCellEditor
{
	private ModifiedContentProposalAdapter contentProposalAdapter;
	private boolean popupOpen = false; // true, iff popup is currently open
	private final boolean multiSelect;

	public StyleClassCellEditor(Composite parent, StyleClassesComboboxModel model, boolean multiSelect)
	{
		super(parent);
		this.multiSelect = multiSelect;
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
				if (contents.length() > 0 && lastIndexOfSpace < contents.length() - 1)
				{
					setFiltering(true);
					IContentProposal[] proposals = super.getProposals(contents.substring(lastIndexOfSpace + 1), position);
					setFiltering(false);
					return proposals;
				}
				return super.getProposals(contents, position);
			}
		};
		contentProposalAdapter = new ModifiedContentProposalAdapter(text, new TextContentAdapter(), provider, null, null);
		contentProposalAdapter.addContentProposalListener(new IContentProposalListener2()
		{
			@Override
			public void proposalPopupClosed(ContentProposalAdapter adapter)
			{
				popupOpen = false;
			}

			@Override
			public void proposalPopupOpened(ContentProposalAdapter adapter)
			{
				popupOpen = true;
			}
		});
		if (multiSelect)
		{
			contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
			contentProposalAdapter.addContentProposalListener(new IContentProposalListener()
			{
				@Override
				public void proposalAccepted(IContentProposal proposal)
				{
					IControlContentAdapter contentAdapter = contentProposalAdapter.getControlContentAdapter();
					Control control = contentProposalAdapter.getControl();
					String contents = contentAdapter.getControlContents(control);
					int cursorPosition = contentAdapter.getCursorPosition(control);
					int startPosition = contents.lastIndexOf(" ", cursorPosition) + 1;
					contents = contents.substring(0, startPosition) + proposal.getContent();
					// set/reset auto activation chars to prevent popup from opening again
					contentProposalAdapter.setAutoActivationCharacters(new char[0]);
					contentAdapter.setControlContents(control, contents, startPosition + proposal.getContent().length());
					contentProposalAdapter.setAutoActivationCharacters(null);
				}
			});
		}
		else
		{
			contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		}
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
		return false;
	}

	@Override
	protected Object doGetValue()
	{
		String value = (String)super.doGetValue();
		return value.replace("DEFAULT", "");
	}

	@Override
	public void activate()
	{
		super.activate();
		Display.getCurrent().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				contentProposalAdapter.openProposalPopup();
			}
		});

	}

	private static class ModifiedContentProposalAdapter extends ContentProposalAdapter
	{

		/**
		 * @param control
		 * @param controlContentAdapter
		 * @param proposalProvider
		 * @param keyStroke
		 * @param autoActivationCharacters
		 */
		public ModifiedContentProposalAdapter(Control control, IControlContentAdapter controlContentAdapter, IContentProposalProvider proposalProvider,
			KeyStroke keyStroke, char[] autoActivationCharacters)
		{
			super(control, controlContentAdapter, proposalProvider, keyStroke, autoActivationCharacters);
		}

		@Override
		public void openProposalPopup()
		{
			super.openProposalPopup();
		}

	}
}
