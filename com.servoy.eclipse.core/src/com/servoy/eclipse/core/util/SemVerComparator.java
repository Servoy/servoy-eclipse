/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.core.util;

import java.util.Arrays;

/**
 * Used to compare web package versions.
 * A version can have a qualifier after the first '-' (for example 2026.9.1-pre). When the numeric parts are equal,
 * a version without a qualifier is greater than one with a qualifier, so 2026.9.1-pre is lower than 2026.9.1.
 * @author emera
 */
public class SemVerComparator
{
	public static int compare(String v1, String v2)
	{
		int v1Dash = v1.indexOf('-');
		int v2Dash = v2.indexOf('-');
		String v1Qualifier = v1Dash >= 0 ? v1.substring(v1Dash + 1) : null;
		String v2Qualifier = v2Dash >= 0 ? v2.substring(v2Dash + 1) : null;

		int cv = compareNumbers(v1Dash >= 0 ? v1.substring(0, v1Dash) : v1, v2Dash >= 0 ? v2.substring(0, v2Dash) : v2);
		if (cv != 0) return cv;

		if (v1Qualifier == null && v2Qualifier == null) return 0;
		if (v1Qualifier == null) return 1;
		if (v2Qualifier == null) return -1;
		return v1Qualifier.compareTo(v2Qualifier);
	}

	private static int compareNumbers(String v1, String v2)
	{
		String[] v1Split = v1.split("\\.");
		String[] v2Split = v2.split("\\.");

		// make the splits to be the same size, filling missing part with "0"
		int v1SplitLength = v1Split.length, v2SplitLength = v2Split.length;
		int maxVersionTags = Math.max(v1SplitLength, v2SplitLength);
		if (v1SplitLength < maxVersionTags)
		{
			v1Split = Arrays.copyOf(v1Split, maxVersionTags);
			Arrays.fill(v1Split, v1SplitLength, maxVersionTags, "0");
		}
		else if (v2SplitLength < maxVersionTags)
		{
			v2Split = Arrays.copyOf(v2Split, maxVersionTags);
			Arrays.fill(v2Split, v2SplitLength, maxVersionTags, "0");
		}

		int cv = 0;
		for (int i = 0; i < v1Split.length; i++)
		{

			try
			{
				int v1Nr = Integer.parseInt(v1Split[i]);
				int v2Nr = Integer.parseInt(v2Split[i]);
				cv = v1Nr - v2Nr;
			}
			catch (NumberFormatException ex)
			{
				cv = v1Split[i].compareTo(v2Split[i]);
			}
			if (cv != 0) break;
		}

		return cv;
	}
}
