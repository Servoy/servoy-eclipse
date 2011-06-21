/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.search;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.ibm.icu.text.MessageFormat;

/**
 * StyledLabel provider for search results based on files.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class FileLabelProvider extends LabelProvider implements IStyledLabelProvider
{

	public static final int SHOW_LABEL = 1;
	public static final int SHOW_LABEL_PATH = 2;
	public static final int SHOW_PATH_LABEL = 3;

	private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

	private static final String fgEllipses = " ... "; //$NON-NLS-1$

	private final WorkbenchLabelProvider fLabelProvider;
	private final AbstractTextSearchViewPage fPage;
	private final Comparator<FileMatch> fMatchComparator;

	private final Image fLineMatchImage;

	private int fOrder;

	public FileLabelProvider(AbstractTextSearchViewPage page, int orderFlag)
	{
		fLabelProvider = new WorkbenchLabelProvider();
		fOrder = orderFlag;
		fPage = page;
		fLineMatchImage = SearchPluginImages.get(SearchPluginImages.IMG_OBJ_TEXT_SEARCH_LINE);
		fMatchComparator = new Comparator<FileMatch>()
		{
			public int compare(FileMatch o1, FileMatch o2)
			{
				return o1.getOriginalOffset() - o2.getOriginalOffset();
			}
		};
	}

	public void setOrder(int orderFlag)
	{
		fOrder = orderFlag;
	}

	public int getOrder()
	{
		return fOrder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object object)
	{
		return getStyledText(object).getString();
	}

	public StyledString getStyledText(Object element)
	{
		if (element instanceof LineElement) return getLineElementLabel((LineElement)element);

		if (!(element instanceof IResource)) return new StyledString();

		IResource resource = (IResource)element;
		if (!resource.exists()) new StyledString(SearchMessages.FileLabelProvider_removed_resource_label);

		String name = BasicElementLabels.getResourceName(resource);
		if (fOrder == SHOW_LABEL)
		{
			return getColoredLabelWithCounts(resource, new StyledString(name));
		}

		String pathString = BasicElementLabels.getPathLabel(resource.getParent().getFullPath(), false);
		if (fOrder == SHOW_LABEL_PATH)
		{
			StyledString str = new StyledString(name);
			String decorated = MessageFormat.format(fgSeparatorFormat, new String[] { str.getString(), pathString });

			StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
			return getColoredLabelWithCounts(resource, str);
		}

		StyledString str = new StyledString(MessageFormat.format(fgSeparatorFormat, new String[] { pathString, name }));
		return getColoredLabelWithCounts(resource, str);
	}

	private StyledString getLineElementLabel(LineElement lineElement)
	{
		int lineNumber = lineElement.getLine();
		String lineNumberString = MessageFormat.format(SearchMessages.FileLabelProvider_line_number, new Object[] { new Integer(lineNumber) });

		StyledString str = new StyledString(lineNumberString, StyledString.QUALIFIER_STYLER);

		FileMatch[] matches = lineElement.getMatches(fPage.getInput());
		Arrays.sort(matches, fMatchComparator);

		String content = lineElement.getContents();

		int pos = evaluateLineStart(matches, content, lineElement.getOffset());

		int length = content.length();

		int charsToCut = getCharsToCut(length, matches); // number of characters to leave away if the line is too long
		for (int i = 0; i < matches.length; i++)
		{
			FileMatch match = matches[i];
			if (match.isPossibleMatch())
			{
				str.append(" (none exact match) "); //$NON-NLS-1$
			}
			int start = Math.max(match.getOriginalOffset() - lineElement.getOffset(), 0);
			// append gap between last match and the new one
			if (pos < start)
			{
				if (charsToCut > 0)
				{
					charsToCut = appendShortenedGap(content, pos, start, charsToCut, i == 0, str);
				}
				else
				{
					str.append(content.substring(pos, start));
				}
			}
			// append match
			int end = Math.min(match.getOriginalOffset() + match.getOriginalLength() - lineElement.getOffset(), lineElement.getLength());
			str.append(content.substring(start, end), DecoratingFileSearchLabelProvider.HIGHLIGHT_STYLE);
			pos = end;
		}
		// append rest of the line
		if (charsToCut > 0)
		{
			appendShortenedGap(content, pos, length, charsToCut, false, str);
		}
		else
		{
			str.append(content.substring(pos));
		}
		return str;
	}

	private static final int MIN_MATCH_CONTEXT = 10; // minimal number of characters shown after and before a match

	private int appendShortenedGap(String content, int start, int end, int charsToCut, boolean isFirst, StyledString str)
	{
		int gapLength = end - start;
		if (!isFirst)
		{
			gapLength -= MIN_MATCH_CONTEXT;
		}
		if (end < content.length())
		{
			gapLength -= MIN_MATCH_CONTEXT;
		}
		if (gapLength < MIN_MATCH_CONTEXT)
		{ // don't cut, gap is too small
			str.append(content.substring(start, end));
			return charsToCut;
		}

		int context = MIN_MATCH_CONTEXT;
		if (gapLength > charsToCut)
		{
			context += gapLength - charsToCut;
		}

		if (!isFirst)
		{
			str.append(content.substring(start, start + context)); // give all extra context to the right side of a match
			context = MIN_MATCH_CONTEXT;
		}

		str.append(fgEllipses, StyledString.QUALIFIER_STYLER);

		if (end < content.length())
		{
			str.append(content.substring(end - context, end));
		}
		return charsToCut - gapLength + fgEllipses.length();
	}


	private int getCharsToCut(int contentLength, Match[] matches)
	{
		if (contentLength <= 256 || !"win32".equals(SWT.getPlatform()) || matches.length == 0) { //$NON-NLS-1$
			return 0; // no shortening required
		}
		// XXX: workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=38519
		return contentLength - 256 + Math.max(matches.length * fgEllipses.length(), 100);
	}

	private int evaluateLineStart(Match[] matches, String lineContent, int lineOffset)
	{
		int max = lineContent.length();
		if (matches.length > 0)
		{
			FileMatch match = (FileMatch)matches[0];
			max = match.getOriginalOffset() - lineOffset;
			if (max < 0)
			{
				return 0;
			}
		}
		for (int i = 0; i < max; i++)
		{
			char ch = lineContent.charAt(i);
			if (!Character.isWhitespace(ch) || ch == '\n' || ch == '\r')
			{
				return i;
			}
		}
		return max;
	}

	private StyledString getColoredLabelWithCounts(Object element, StyledString coloredName)
	{
		AbstractTextSearchResult result = fPage.getInput();
		if (result == null) return coloredName;

		int matchCount = result.getMatchCount(element);
		if (matchCount <= 1) return coloredName;

		String countInfo = MessageFormat.format(SearchMessages.FileLabelProvider_count_format, new Object[] { new Integer(matchCount) });
		coloredName.append(' ').append(countInfo, StyledString.COUNTER_STYLER);
		return coloredName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element)
	{
		if (element instanceof LineElement)
		{
			return fLineMatchImage;
		}
		if (!(element instanceof IResource)) return null;

		IResource resource = (IResource)element;
		Image image = fLabelProvider.getImage(resource);
		return image;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();
		fLabelProvider.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property)
	{
		return fLabelProvider.isLabelProperty(element, property);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener)
	{
		super.removeListener(listener);
		fLabelProvider.removeListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener)
	{
		super.addListener(listener);
		fLabelProvider.addListener(listener);
	}

}
