/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
package com.servoy.eclipse.ui.editors.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes the UUID marker (<code>@type {UUID}</code>, or <code>@return {UUID}</code>/<code>@returns {UUID}</code> when reading) in the first JSDoc block of a
 * calculation declaration. This is a plain java helper, with no SWT or OSGi dependencies.
 *
 * @author servoy
 */
@SuppressWarnings("nls")
public final class CalculationUuidMarker
{
	private static final String TYPE_UUID_TAG = "@type {UUID}";

	// a tag line of the first JSDoc block (can also be on the same line as the block start) that declares a UUID type or return type
	private static final Pattern MARKER_LINE = Pattern.compile("^[ \\t]*(?:/\\*\\*|\\*)?[ \\t]*@(?:type|returns?)\\b[^\\r\\n]*\\{UUID\\}");
	// a whole line (with line terminator) that only exists in the block to hold the marker
	private static final Pattern MARKER_FULL_LINE = Pattern.compile("^[ \\t]*\\*[ \\t]*@(?:type|returns?)\\b[^\\r\\n]*\\{UUID\\}[^\\r\\n]*\\r?\\n$");
	// the block start line that also holds the marker
	private static final Pattern MARKER_OPENER_LINE = Pattern.compile("^([ \\t]*/\\*\\*)[ \\t]*@(?:type|returns?)\\b[^\\r\\n]*\\{UUID\\}[^\\r\\n]*(\\r?\\n)$");
	private static final Pattern BLANK_COMMENT_LINE = Pattern.compile("^[ \\t]*\\*[ \\t]*\\r?\\n$");
	private static final Pattern PREFIX = Pattern.compile("[ \\t]*\\*[ \\t]*");

	private CalculationUuidMarker()
	{
	}

	/**
	 * @return true if the first JSDoc block of the declaration has a <code>@type</code> or <code>@return</code> line containing <code>{UUID}</code>.
	 */
	public static boolean hasUuidMarker(String declaration)
	{
		if (declaration == null) return false;
		int start = blockStart(declaration);
		if (start == -1) return false;
		int commentEnd = declaration.indexOf("*/", start + 3);
		if (commentEnd == -1) return false;
		for (String line : declaration.substring(start, commentEnd).split("\\n", -1))
		{
			if (MARKER_LINE.matcher(line).find()) return true;
		}
		return false;
	}

	/**
	 * Adds (uuid == true) or removes (uuid == false) the UUID marker of the declaration.
	 *
	 * @return the new declaration, the same string if nothing had to change.
	 */
	public static String setUuidMarker(String declaration, boolean uuid)
	{
		if (declaration == null) declaration = "";
		return uuid ? addMarker(declaration) : removeMarker(declaration);
	}

	private static int blockStart(String declaration)
	{
		int start = 0;
		while (start < declaration.length() && Character.isWhitespace(declaration.charAt(start)))
		{
			start++;
		}
		return declaration.startsWith("/**", start) ? start : -1;
	}

	private static String lineDelimiter(String declaration)
	{
		return declaration.contains("\r\n") ? "\r\n" : "\n";
	}

	private static String addMarker(String declaration)
	{
		if (hasUuidMarker(declaration)) return declaration;
		String d = lineDelimiter(declaration);
		int start = blockStart(declaration);
		if (start == -1)
		{
			return "/**" + d + " * " + TYPE_UUID_TAG + d + " */" + d + declaration;
		}
		int commentEnd = declaration.indexOf("*/", start + 3);
		if (commentEnd == -1) return declaration;

		String block = declaration.substring(start, commentEnd);
		if (block.indexOf('\n') == -1)
		{
			// single line block
			String content = block.substring(3).trim();
			return declaration.substring(0, start) + "/**" + d + (content.isEmpty() ? "" : " * " + content + d) + " * " + TYPE_UUID_TAG + d + " */" +
				declaration.substring(commentEnd + 2);
		}

		int propertiesIndex = block.indexOf("@properties");
		if (propertiesIndex != -1)
		{
			int propertiesPos = start + propertiesIndex;
			int lineStart = declaration.lastIndexOf('\n', propertiesPos) + 1;
			// when @properties is on the same line as the block opener we cannot insert before that line, so fall through to the closing line
			if (lineStart > start)
			{
				String prefix = declaration.substring(lineStart, propertiesPos);
				if (!PREFIX.matcher(prefix).matches()) prefix = " * ";
				String blank = prefix.stripTrailing();
				return declaration.substring(0, lineStart) + prefix + TYPE_UUID_TAG + d + blank + d + declaration.substring(lineStart);
			}
		}

		int closeLineStart = declaration.lastIndexOf('\n', commentEnd) + 1;
		if (declaration.substring(closeLineStart, commentEnd).isBlank())
		{
			return declaration.substring(0, closeLineStart) + " * " + TYPE_UUID_TAG + d + declaration.substring(closeLineStart);
		}
		return declaration.substring(0, commentEnd) + d + " * " + TYPE_UUID_TAG + d + " " + declaration.substring(commentEnd);
	}

	private static String removeMarker(String declaration)
	{
		int start = blockStart(declaration);
		if (start == -1) return declaration;
		int commentEnd = declaration.indexOf("*/", start + 3);
		if (commentEnd == -1) return declaration;

		String block = declaration.substring(start, commentEnd);
		int lastNewLine = block.lastIndexOf('\n');
		if (lastNewLine == -1)
		{
			// single line block
			String newBlock = block.replaceAll("[ \\t]*@(?:type|returns?)\\b[^\\r\\n]*\\{UUID\\}[^\\r\\n]*", "");
			if (newBlock.equals(block)) return declaration;
			return declaration.substring(0, start) + newBlock.stripTrailing() + " " + declaration.substring(commentEnd);
		}

		String head = block.substring(0, lastNewLine + 1);
		String tail = block.substring(lastNewLine + 1);
		List<String> lines = new ArrayList<>(Arrays.asList(head.split("(?<=\n)")));
		boolean changed = false;
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i);
			Matcher opener = i == 0 ? MARKER_OPENER_LINE.matcher(line) : null;
			if (opener != null && opener.matches())
			{
				lines.set(i, opener.group(1) + opener.group(2));
				changed = true;
			}
			else if (MARKER_FULL_LINE.matcher(line).matches())
			{
				changed = true;
				lines.remove(i);
				boolean afterOpenerOrBlank = i > 0 && (i - 1 == 0 || BLANK_COMMENT_LINE.matcher(lines.get(i - 1)).matches());
				if (i < lines.size())
				{
					// avoid leaving a double blank line (or a blank line right after the block start)
					if (afterOpenerOrBlank && BLANK_COMMENT_LINE.matcher(lines.get(i)).matches())
					{
						lines.remove(i);
					}
				}
				else if (i - 1 > 0 && BLANK_COMMENT_LINE.matcher(lines.get(i - 1)).matches())
				{
					// the removed line was the last one before the block end, remove the blank line that preceded it
					lines.remove(i - 1);
					i--;
				}
				i--;
			}
		}
		if (!changed) return declaration;
		return declaration.substring(0, start) + String.join("", lines) + tail + declaration.substring(commentEnd);
	}
}
