/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.servoy.eclipse.cypress.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the optional <code>viewportWidths</code> parameter shared by the Cypress spec generators (SVY-21619). The value is a comma-separated list of
 * positive pixel widths (e.g. <code>"375,1024"</code>); the result is an ordered, deduped list of ints preserving the first occurrence of each width.
 * An invalid token (non-numeric, zero or negative) is reported by throwing {@link IllegalArgumentException} naming the offending token, which the tool
 * methods catch and turn into an <code>Error:</code> string.
 */
public final class ViewportWidths
{
	private ViewportWidths()
	{
	}

	/**
	 * Turns a comma-separated widths string into an ordered, deduped list of positive ints. A null or blank input yields an empty list (behave as
	 * today). A blank token between commas is skipped. A token that is not a positive integer throws {@link IllegalArgumentException}.
	 *
	 * @param csv the comma-separated widths, may be null/blank
	 * @return an ordered list of distinct positive widths, never null
	 * @throws IllegalArgumentException if a non-blank token is not a positive integer
	 */
	public static List<Integer> parse(String csv)
	{
		if (csv == null || csv.isBlank())
		{
			return new ArrayList<>();
		}
		Set<Integer> ordered = new LinkedHashSet<>();
		for (String rawToken : csv.split(","))
		{
			String token = rawToken.trim();
			if (token.isEmpty())
			{
				continue;
			}
			int width;
			try
			{
				width = Integer.parseInt(token);
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Invalid viewportWidths token: '" + token + "' (must be a positive integer)");
			}
			if (width <= 0)
			{
				throw new IllegalArgumentException("Invalid viewportWidths token: '" + token + "' (must be a positive integer)");
			}
			ordered.add(Integer.valueOf(width));
		}
		return new ArrayList<>(ordered);
	}
}
