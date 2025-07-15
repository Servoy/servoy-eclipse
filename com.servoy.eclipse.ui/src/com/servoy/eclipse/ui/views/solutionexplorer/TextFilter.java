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

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.servoy.j2db.documentation.ClientSupport;

/**
 * Filter for viewers, used on top of Solution Explorer Tree.
 *
 * @author jblok
 */

public class TextFilter extends ViewerFilter
{
	private final Set<Object> matchingNodes = new HashSet<Object>();
	private final Set<Object> parentsOfMatchingNodes = new HashSet<Object>(); // the nodes that are in here (parentsOfMatchingNodes) will not be in childrenOfMatchingNodes
	private final Set<Object> childrenOfMatchingNodes = new HashSet<Object>();
	private final ILabelProvider labelProvider;
	private IProgressMonitor monitor;

	@Override
	public Object[] filter(Viewer viewer, Object parent, Object[] elements)
	{
		if (fPattern.trim().length() == 0)
		{
			return elements;
		}
		else
		{
			if (parent == viewer.getInput())
			{
				filterNodeChildren(viewer, parent, elements, matchingNodes.contains(parent) || childrenOfMatchingNodes.contains(parent));
			}
			return super.filter(viewer, parent, elements);
		}
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object node)
	{
		if (fPattern.trim().length() == 0) return true;
		return (matchingNodes.contains(node) || parentsOfMatchingNodes.contains(node) || childrenOfMatchingNodes.contains(node)) &&
			ClientSupportViewerFilter.isNodeAllowedInClient(clientType, node);
	}

	/**
	 * Filter the parent's subtree (elements are the direct children of parent) and puts them into 3 categories: matching nodes, parents of matching nodes and
	 * children of matching nodes.
	 *
	 * @param viewer the viewer being filtered
	 * @param parent the parent node
	 * @param elements the parent's direct children
	 * @param ancestorsMatched2
	 * @return true if any of the nodes of the sub-tree matched; false otherwise.
	 */
	private boolean filterNodeChildren(Viewer viewer, Object parent, Object[] elements, boolean ancestorsMatched)
	{
		int size = elements.length;
		boolean retval = false;
		for (int i = 0; i < size; ++i)
		{
			if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
			Object element = elements[i];
			if (filterNodeSubtree(viewer, element, ancestorsMatched))
			{
				retval = true;
			}
			else if (ancestorsMatched)
			{
				if (ClientSupportViewerFilter.isNodeAllowedInClient(clientType, element))
				{
					childrenOfMatchingNodes.add(element);
				}
			}
		}
		return retval;
	}

	private boolean filterNodeSubtree(Viewer viewer, Object element, boolean ancestorsMatched)
	{
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();

		boolean match = matchInList(element) || match(labelProvider.getText(element));

		if (match)
		{
			addMatchingNode(element);
		}
		// check and categorize children
		if (viewer instanceof StructuredViewer)
		{
			StructuredViewer v = (StructuredViewer)viewer;
			IContentProvider cp = v.getContentProvider();
			if (cp instanceof ITreeContentProvider)
			{
				ITreeContentProvider scp = (ITreeContentProvider)cp;
				Object[] children = scp.getChildren(element);
				if (children != null)
				{
					boolean foundMatchingChildren = filterNodeChildren(viewer, element, children, match | ancestorsMatched);
					if (!match && foundMatchingChildren)
					{
						// this means that some of the children of this element matched - so if this
						// node itself does not match - store it in parentsOfMatchingNodes
						if (ClientSupportViewerFilter.isNodeAllowedInClient(clientType, element))
						{
							parentsOfMatchingNodes.add(element);
							match = true;
						}
					}
				}
			}
		}
		return match;
	}

	private boolean matchInList(Object tn)
	{
		// if there are nodes in the outline list (supplementalContentProvider) - corresponding to this
		// item of the tree - that match, we must return true for this element
		boolean matched = false;
		if (supplementalContentProvider != null)
		{
			Object[] supplementalNodes = supplementalContentProvider.getElements(tn);
			for (Object node : supplementalNodes)
			{
				if (match(labelProvider.getText(node)))
				{
					if (ClientSupportViewerFilter.isNodeAllowedInClient(clientType, node))
					{
						matchingNodes.add(node); // for text decoration in list
						matched = true;
					}
				}
			}
		}
		return matched;
	}

	private void addMatchingNode(Object tn)
	{
		if (firstMatchingNode == null)
		{
			firstMatchingNode = tn;
		}
		matchingNodes.add(tn);
	}

	/**
	 * A string pattern matcher, supporting ?*? and ??? wildcards.
	 */
	protected String fPattern = "";

	protected int fLength; // pattern length

	protected boolean fIgnoreWildCards;

	protected boolean fIgnoreCase;

	protected ClientSupport clientType;

	protected boolean fHasLeadingStar;

	protected boolean fHasTrailingStar;

	protected String fSegments[]; //the given pattern is split into * separated segments

	/* boundary value beyond which we don't need to search in the text */
	protected int fBound = 0;

	private IStructuredContentProvider supplementalContentProvider;

	private Object firstMatchingNode;

	protected static final char fSingleWildCard = '\u0000';

	public static class Position
	{
		int start; //inclusive

		int end; //exclusive

		public Position(int start, int end)
		{
			this.start = start;
			this.end = end;
		}

		public int getStart()
		{
			return start;
		}

		public int getEnd()
		{
			return end;
		}
	}

	/**
	 * StringMatcher constructor. Method setText() takes in a String object that is a simple pattern which may contain '*' for 0 and many characters and '?' for
	 * exactly one character.
	 *
	 * Literal '*' and '?' characters must be escaped in the pattern e.g., "\*" means literal "*", etc.
	 *
	 * Escaping any other character (including the escape character itself), just results in that character in the pattern. e.g., "\a" means "a" and "\\" means
	 * "\"
	 *
	 * If invoking the StringMatcher with string literals in Java, don't forget escape characters are represented by "\\".
	 *
	 * @param ignoreCase if true, case is ignored
	 * @param ignoreWildCards if true, wild cards and their escape sequences are ignored (everything is taken literally).
	 */
	public TextFilter(ILabelProvider labelProvider, boolean ignoreCase, boolean ignoreWildCards)
	{
		this.labelProvider = labelProvider;
		fIgnoreCase = ignoreCase;
		fIgnoreWildCards = ignoreWildCards;
		this.clientType = ClientSupport.Default;
	}

	/**
	 * Registers a supplemental content provider to this filter. When filtering is applied to an element, this content-provider is used to get more content for
	 * that element - content that will be searched too. If the filter is true for any content returned by this content provider, it will be true for the
	 * element as well.
	 *
	 * @param contentProvider the supplemental content provider.
	 */
	public void setSupplementalContentProvider(IStructuredContentProvider contentProvider)
	{
		this.supplementalContentProvider = contentProvider;
	}

	public void setText(String pattern)
	{
		if (pattern == null)
		{
			throw new IllegalArgumentException();
		}
		fPattern = pattern;
		fLength = pattern.length();
//        if (fLength == 0)
		firstMatchingNode = null;
		matchingNodes.clear();
		parentsOfMatchingNodes.clear();
		childrenOfMatchingNodes.clear();

		fHasLeadingStar = false;
		fHasTrailingStar = false;
		fBound = 0;
		fSegments = null;

		if (fIgnoreWildCards)
		{
			parseNoWildCards();
		}
		else
		{
			parseWildCards();
		}
	}

	public void setProgressMonitor(IProgressMonitor monitor)
	{
		this.monitor = monitor;
	}

	/**
	 * Returns the current filter String (pattern).
	 *
	 * @return the current filter String (pattern).
	 */
	public String getText()
	{
		return fPattern;
	}

	/**
	 * Returns the first node that matched the last pattern.
	 *
	 * @return the first node that matched the last pattern.
	 */
	public Object getFirstMatchingNode()
	{
		return firstMatchingNode;
	}

	/**
	 * Returns all nodes that match directly the last given pattern.
	 *
	 * @return all nodes that match directly the last given pattern.
	 */
	public Set<Object> getMatchingNodes()
	{
		return matchingNodes;
	}

	/**
	 * Returns all nodes that have matching children in their subtree, but are not matching them selves...
	 *
	 * @return all nodes that have matching children in their subtree, but are not matching them selves...
	 */
	public Set<Object> getParentsOfMatchingNodes()
	{
		return parentsOfMatchingNodes;
	}

	/**
	 * Find the first occurrence of the pattern between <code>start</code)(inclusive)
	 * and <code>end</code>(exclusive).
	 * @param <code>text</code>, the String object to search in
	 * @param <code>start</code>, the starting index of the search range, inclusive
	 * @param <code>end</code>, the ending index of the search range, exclusive
	 * @return an <code>StringMatcher.Position</code> object that keeps the starting
	 * (inclusive) and ending positions (exclusive) of the first occurrence of the
	 * pattern in the specified range of the text; return null if not found or subtext
	 * is empty (start==end). A pair of zeros is returned if pattern is empty string
	 * Note that for pattern like "*abc*" with leading and trailing stars, position of "abc"
	 * is returned. For a pattern like"*??*" in text "abcdf", (1,3) is returned
	public StringMatcher.Position find(String text, int start, int end) {
	    if (text == null) {
			throw new IllegalArgumentException();
		}

	    int tlen = text.length();
	    if (start < 0) {
			start = 0;
		}
	    if (end > tlen) {
			end = tlen;
		}
	    if (end < 0 || start >= end) {
			return null;
		}
	    if (fLength == 0) {
			return new Position(start, start);
		}
	    if (fIgnoreWildCards) {
	        int x = posIn(text, start, end);
	        if (x < 0) {
				return null;
			}
	        return new Position(x, x + fLength);
	    }

	    int segCount = fSegments.length;
	    if (segCount == 0) {
			return new Position(start, end);
		}

	    int curPos = start;
	    int matchStart = -1;
	    int i;
	    for (i = 0; i < segCount && curPos < end; ++i) {
	        String current = fSegments[i];
	        int nextMatch = regExpPosIn(text, curPos, end, current);
	        if (nextMatch < 0) {
				return null;
			}
	        if (i == 0) {
				matchStart = nextMatch;
			}
	        curPos = nextMatch + current.length();
	    }
	    if (i < segCount) {
			return null;
		}
	    return new Position(matchStart, curPos);
	}
	 */
	/**
	 * match the given <code>text</code> with the pattern
	 *
	 * @return true if matched eitherwise false
	 * @param <code>text</code>, a String object
	 */
	private boolean match(String text)
	{
		return text != null ? match(text, 0, text.length()) : false;
	}

	/**
	 * Given the starting (inclusive) and the ending (exclusive) poisitions in the <code>text</code>, determine if the given substring matches with aPattern
	 *
	 * @return true if the specified portion of the text matches the pattern
	 * @param String <code>text</code>, a String object that contains the substring to match
	 * @param int <code>start<code> marks the starting position (inclusive) of the substring
	 * @param int <code>end<code> marks the ending index (exclusive) of the substring
	 */
	private boolean match(String text, int start, int end)
	{
		if (null == text)
		{
			throw new IllegalArgumentException();
		}

		if (start > end)
		{
			return false;
		}

		if (fIgnoreWildCards)
		{
			return (end - start == fLength) && fPattern.regionMatches(fIgnoreCase, 0, text, start, fLength);
		}
		int segCount = fSegments == null ? 0 : fSegments.length;
		if (segCount == 0 && (fHasLeadingStar || fHasTrailingStar))
		{
			return true;
		}
		if (start == end)
		{
			return fLength == 0;
		}
		if (fLength == 0)
		{
			return start == end;
		}

		int tlen = text.length();
		if (start < 0)
		{
			start = 0;
		}
		if (end > tlen)
		{
			end = tlen;
		}

		int tCurPos = start;
		int bound = end - fBound;
		if (bound < 0)
		{
			return false;
		}
		int i = 0;
		String current = fSegments[i];

		if (current == null)
		{
			return false;
		}
		int segLength = current.length();

		/* process first segment */
		if (!fHasLeadingStar)
		{
			if (!regExpRegionMatches(text, start, current, 0, segLength))
			{
				return false;
			}
			else
			{
				++i;
				tCurPos = tCurPos + segLength;
			}
		}
		if ((fSegments.length == 1) && (!fHasLeadingStar) && (!fHasTrailingStar))
		{
			// only one segment to match, no wildcards specified
			return tCurPos == end;
		}
		/* process middle segments */
		for (; i < segCount && tCurPos <= bound; ++i)
		{
			current = fSegments[i];
			int currentMatch;
			int k = current.indexOf(fSingleWildCard);
			if (k < 0)
			{
				currentMatch = textPosIn(text, tCurPos, end, current);
				if (currentMatch < 0)
				{
					return false;
				}
			}
			else
			{
				currentMatch = regExpPosIn(text, tCurPos, end, current);
				if (currentMatch < 0)
				{
					return false;
				}
			}
			tCurPos = currentMatch + current.length();
		}

		/* process final segment */
		if (!fHasTrailingStar && tCurPos != end)
		{
			int clen = current.length();
			return regExpRegionMatches(text, end - clen, current, 0, clen);
		}
		return i == segCount;
	}

	/**
	 * This method parses the given pattern into segments seperated by wildcard '*' characters. Since wildcards are not being used in this case, the pattern
	 * consists of a single segment.
	 */
	private void parseNoWildCards()
	{
		fSegments = new String[1];
		fSegments[0] = fPattern;
		fBound = fLength;
	}

	/**
	 * Parses the given pattern into segments seperated by wildcard '*' characters.
	 *
	 * @param p, a String object that is a simple regular expression with '*' and/or '?'
	 */
	private void parseWildCards()
	{
		fHasLeadingStar = (fPattern.startsWith("*"));

		if (fPattern.endsWith("*"))
		{
			/* make sure it's not an escaped wildcard */
			if (fLength > 1 && fPattern.charAt(fLength - 2) != '\\')
			{
				fHasTrailingStar = true;
			}
		}

		Vector temp = new Vector();

		int pos = 0;
		StringBuffer buf = new StringBuffer();
		while (pos < fLength)
		{
			char c = fPattern.charAt(pos++);
			switch (c)
			{
				case '\\' :
					if (pos >= fLength)
					{
						buf.append(c);
					}
					else
					{
						char next = fPattern.charAt(pos++);
						/* if it's an escape sequence */
						if (next == '*' || next == '?' || next == '\\')
						{
							buf.append(next);
						}
						else
						{
							/* not an escape sequence, just insert literally */
							buf.append(c);
							buf.append(next);
						}
					}
					break;
				case '*' :
					if (buf.length() > 0)
					{
						/* new segment */
						temp.addElement(buf.toString());
						fBound += buf.length();
						buf.setLength(0);
					}
					break;
				case '?' :
					/* append special character representing single match wildcard */
					buf.append(fSingleWildCard);
					break;
				default :
					buf.append(c);
			}
		}

		/* add last buffer to segment list */
		if (buf.length() > 0)
		{
			temp.addElement(buf.toString());
			fBound += buf.length();
		}

		fSegments = new String[temp.size()];
		temp.copyInto(fSegments);
	}

	/**
	 * @param <code>text</code>, a string which contains no wildcard
	 * @param <code>start</code>, the starting index in the text for search, inclusive
	 * @param <code>end</code>, the stopping point of search, exclusive
	 * @return the starting index in the text of the pattern , or -1 if not found
	 */
	protected int posIn(String text, int start, int end)
	{//no wild card in pattern
		int max = end - fLength;

		if (!fIgnoreCase)
		{
			int i = text.indexOf(fPattern, start);
			if (i == -1 || i > max)
			{
				return -1;
			}
			return i;
		}

		for (int i = start; i <= max; ++i)
		{
			if (text.regionMatches(true, i, fPattern, 0, fLength))
			{
				return i;
			}
		}

		return -1;
	}

	/**
	 * @param <code>text</code>, a simple regular expression that may only contain '?'(s)
	 * @param <code>start</code>, the starting index in the text for search, inclusive
	 * @param <code>end</code>, the stopping point of search, exclusive
	 * @param <code>p</code>, a simple regular expression that may contains '?'
	 * @param <code>caseIgnored</code>, wether the pattern is not casesensitive
	 * @return the starting index in the text of the pattern , or -1 if not found
	 */
	protected int regExpPosIn(String text, int start, int end, String p)
	{
		int plen = p.length();

		int max = end - plen;
		for (int i = start; i <= max; ++i)
		{
			if (regExpRegionMatches(text, i, p, 0, plen))
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 *
	 * @return boolean
	 * @param <code>text</code>, a String to match
	 * @param <code>start</code>, int that indicates the starting index of match, inclusive
	 * @param <code>end</code> int that indicates the ending index of match, exclusive
	 * @param <code>p</code>, String, String, a simple regular expression that may contain '?'
	 * @param <code>ignoreCase</code>, boolean indicating wether code>p</code> is case sensitive
	 */
	protected boolean regExpRegionMatches(String text, int tStart, String p, int pStart, int plen)
	{
		while (plen-- > 0)
		{
			char tchar = text.charAt(tStart++);
			char pchar = p.charAt(pStart++);

			/* process wild cards */
			if (!fIgnoreWildCards)
			{
				/* skip single wild cards */
				if (pchar == fSingleWildCard)
				{
					continue;
				}
			}
			if (pchar == tchar)
			{
				continue;
			}
			if (fIgnoreCase)
			{
				if (Character.toUpperCase(tchar) == Character.toUpperCase(pchar))
				{
					continue;
				}
				// comparing after converting to upper case doesn't handle all cases;
				// also compare after converting to lower case
				if (Character.toLowerCase(tchar) == Character.toLowerCase(pchar))
				{
					continue;
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * @param <code>text</code>, the string to match
	 * @param <code>start</code>, the starting index in the text for search, inclusive
	 * @param <code>end</code>, the stopping point of search, exclusive
	 * @param code>p</code>, a string that has no wildcard
	 * @param <code>ignoreCase</code>, boolean indicating wether code>p</code> is case sensitive
	 * @return the starting index in the text of the pattern , or -1 if not found
	 */
	protected int textPosIn(String text, int start, int end, String p)
	{

		int plen = p.length();
		int max = end - plen;

		if (!fIgnoreCase)
		{
			int i = text.indexOf(p, start);
			if (i == -1 || i > max)
			{
				return -1;
			}
			return i;
		}

		for (int i = 0; i <= max; ++i)
		{
			if (text.regionMatches(true, i, p, 0, plen))
			{
				return i;
			}
		}

		return -1;
	}

	/**
	 * @param clientType the clientType to set
	 */
	public void setClientType(ClientSupport clientType)
	{
		this.clientType = clientType;
	}
}
