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
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleSheet;
import org.eclipse.wst.css.core.internal.util.RegionIterator;
import org.eclipse.wst.css.core.text.ICSSPartitions;
import org.eclipse.wst.css.ui.StructuredTextViewerConfigurationCSS;
import org.eclipse.wst.css.ui.internal.contentassist.CSSContentAssistProcessor;
import org.eclipse.wst.css.ui.internal.image.CSSImageHelper;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;

import com.servoy.j2db.component.ComponentFactory;

/**
 * This class is able to filter the part listener events and provide support for ActiveEditorListeners.
 * 
 * @author acostescu
 */
public class ActiveEditorTracker implements IPartListener
{

	private IEditorPart currentActiveEditor;
	private final ArrayList<ActiveEditorListener> listeners;

	/**
	 * Creates and initializes (the currently active editor) a new ActiveEditorTracker.
	 */
	public ActiveEditorTracker(IEditorPart currentActiveEditor)
	{
		listeners = new ArrayList<ActiveEditorListener>();
		this.currentActiveEditor = currentActiveEditor;
	}

	/**
	 * Returns the currently active editor (as calculated by this tracker).
	 * 
	 * @return the currently active editor (as calculated by this tracker).
	 */
	public IEditorPart getActiveEditor()
	{
		return currentActiveEditor;
	}

	public void addActiveEditorListener(ActiveEditorListener l)
	{
		if (!listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	public void removeActiveEditorListener(ActiveEditorListener l)
	{
		listeners.remove(l);
	}

	public void partActivated(IWorkbenchPart part)
	{
		if (part instanceof IEditorPart && currentActiveEditor != part)
		{
			currentActiveEditor = (IEditorPart)part;
			fireActiveEditorChanged(currentActiveEditor);
		}
	}

	private void fireActiveEditorChanged(IEditorPart part)
	{
		for (ActiveEditorListener l : listeners)
		{
			l.activeEditorChanged(part);
		}
	}

	public void partBroughtToTop(IWorkbenchPart part)
	{
	}

	public void partClosed(IWorkbenchPart part)
	{
		if (part instanceof IEditorPart && currentActiveEditor == part)
		{
			currentActiveEditor = null;
			fireActiveEditorChanged(currentActiveEditor);
		}
	}

	public void partDeactivated(IWorkbenchPart part)
	{
	}

	public void partOpened(IWorkbenchPart part)
	{
		if (part instanceof StructuredTextEditor)
		{
			((StructuredTextEditor)part).getTextViewer().configure(new StructuredTextViewerConfigurationCSS()
			{
				@Override
				protected IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer, String partitionType)
				{
					IContentAssistProcessor[] processors = super.getContentAssistProcessors(sourceViewer, partitionType);

					if (partitionType == ICSSPartitions.STYLE)
					{
						return new IContentAssistProcessor[] { new CSSContentAssistProcessor()
						{
							@Override
							public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset)
							{
								IndexedRegion indexedNode = ContentAssistUtils.getNodeAt(viewer, offset);
								if (indexedNode == null || indexedNode instanceof ICSSStyleSheet ||
									(indexedNode instanceof ICSSStyleRule && isBeforeBracket(viewer, offset)))
								{
									List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
									String text = "";
									int replacementOffset = 0;
									Image image = CSSImageHelper.getInstance().getImage(CSSImageType.SELECTOR_TAG);
									String delimiter = ((IStructuredDocument)viewer.getDocument()).getLineDelimiter();
									String braces = delimiter + "{" + delimiter + "\t" + delimiter + "}";
									int cursorOffset = delimiter.length() + 1;
									IStructuredDocumentRegion documentRegion = ((IStructuredDocument)viewer.getDocument()).getRegionAtCharacterOffset(offset);
									if (documentRegion != null)
									{
										ITextRegion region = documentRegion.getRegionAtCharacterOffset(offset);
										if (region == null)
										{
											region = documentRegion.getFirstRegion();
										}
										int textOffset = (offset > 0 && !isSpecialDelimiterRegion(region)) ? (offset - 1) : offset;
										ITextRegion targetRegion = documentRegion.getRegionAtCharacterOffset(textOffset);
										if (targetRegion == null)
										{
											targetRegion = documentRegion.getFirstRegion();
										}
										if (targetRegion != null) text = documentRegion.getText(targetRegion).trim();
										replacementOffset = (offset - text.length()) > 0 ? (offset - text.length()) : 0;
									}
									for (String lookup : ComponentFactory.LOOKUP_NAMES)
									{
										if (!"".equals(text) && !lookup.toLowerCase().startsWith(text.toLowerCase())) continue;
										String toReplace = lookup + braces;
										proposals.add(new CompletionProposal(toReplace, replacementOffset, text.length(), toReplace.length() - cursorOffset,
											image, lookup, null, null));
									}
									return proposals.toArray(new ICompletionProposal[] { });
								}
								return super.computeCompletionProposals(viewer, offset);
							}
						} };
					}
					return processors;
				}
			});
		}
	}

	private boolean isBeforeBracket(ITextViewer viewer, int offset)
	{
		RegionIterator iRegion = new RegionIterator((IStructuredDocument)viewer.getDocument(), offset);
		while (iRegion.hasNext())
		{
			ITextRegion region = iRegion.next();
			if (region.getType() == CSSRegionContexts.CSS_SELECTOR_ELEMENT_NAME || region.getType() == CSSRegionContexts.CSS_SELECTOR_COMBINATOR)
			{
				return true;
			}
			if (region.getType() == CSSRegionContexts.CSS_RBRACE)
			{
				return false;
			}
		}

		return false;
	}

	private boolean isSpecialDelimiterRegion(ITextRegion region)
	{
		if (region == null) return true;
		String type = region.getType();
		return (type == CSSRegionContexts.CSS_LBRACE || type == CSSRegionContexts.CSS_RBRACE || type == CSSRegionContexts.CSS_DELIMITER ||
			type == CSSRegionContexts.CSS_DECLARATION_SEPARATOR || type == CSSRegionContexts.CSS_DECLARATION_DELIMITER ||
			type == CSSRegionContexts.CSS_DECLARATION_VALUE_OPERATOR || type == CSSRegionContexts.CSS_S);
	}
}
