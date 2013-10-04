/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.model.mobile.exporter;

/**
 * This class can append String values to a StringBuilder, but it also is able to track the current line number.
 * @author acostescu
 */
public class ScriptStringBuilder
{

	protected StringBuilder stringBuilder;
	protected boolean trackLines;
	protected long currentLineNumber = 1;

	public ScriptStringBuilder(boolean trackLines)
	{
		this.trackLines = trackLines;
		stringBuilder = new StringBuilder();
	}

	public ScriptStringBuilder append(String s)
	{
		stringBuilder.append(s);
		// count added lines
		if (trackLines) currentLineNumber += newLineSeparatorCount(s);
		return this;
	}

	public long getCurrentLineNumber()
	{
		if (!trackLines) throw new RuntimeException("Line numbers are not being tracked!"); //$NON-NLS-1$
		return currentLineNumber;
	}

	public static long newLineSeparatorCount(String s)
	{
		if (s == null || s.length() == 0) return 0;

		int size = s.length();
		long newLines = 0;

		for (int i = 0; i < size; i++)
		{
			char c = s.charAt(i);
			if (c == '\r')
			{
				newLines++;
				if (i + 1 < size && s.charAt(i + 1) == '\n') i++;
			}
			else if (c == '\n')
			{
				newLines++;
			}
		}
		return newLines;
	}

	@Override
	public String toString()
	{
		return stringBuilder.toString();
	}

}
