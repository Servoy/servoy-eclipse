/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.designer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;

import com.servoy.j2db.persistence.CSSPositionUtils;

/**
 * The value of a CSS position property. Can be px, % or calc.
 * @author emera
 */
public class CSSValue
{
	private static Pattern calcPattern = Pattern.compile("calc\\((\\d+)%(?:\\s+([-+])\\s+(\\d+)px)\\)");
	public static final CSSValue NOT_SET = new CSSValue("-1");

	private int percentage;
	private int pixels;
	private int parentSize;

	public CSSValue(String value)
	{
		super();
		parseCSSValue(value);
	}

	public CSSValue(int percentage, int pixels)
	{
		super();
		this.percentage = percentage;
		this.pixels = pixels;
	}

	public CSSValue(String value, int parentSize)
	{
		super();
		parseCSSValue(value);
		this.parentSize = parentSize;
	}

	private void parseCSSValue(String value)
	{
		if (value == null || !CSSPositionUtils.isSet(value))
		{
			pixels = -1;
			return;
		}
		if (value.startsWith("calc("))
		{
			Matcher calcMatcher = calcPattern.matcher(value);

			if (calcMatcher.matches())
			{
				percentage = Integer.parseInt(calcMatcher.group(1));
				int op = calcMatcher.group(2).equals("+") ? 1 : -1;
				pixels = op * Integer.parseInt(calcMatcher.group(3));
			}
			return;
		}

		if (value.endsWith("%"))
		{
			percentage = Integer.parseInt(value.replace("%", ""));
		}
		else
		{
			pixels = Integer.parseInt(value.replace("px", ""));
		}
	}

	public int getPercentage()
	{
		return percentage;
	}

	public int getPixels()
	{
		return pixels;
	}

	public boolean isSet()
	{
		return pixels != -1;
	}

	public boolean isPx()
	{
		return percentage == 0 && pixels != -1;
	}

	public boolean isPercentage()
	{
		return percentage != 0 && pixels == 0;
	}

	public int getAsPixels(int containerSize)
	{
		if (parentSize > 0 && percentage == 0) return containerSize - pixels;
		return Math.round(percentage * containerSize / 100) + pixels;
	}

	public CSSValue div(int scalar)
	{
		return new CSSValue(percentage / scalar, pixels / scalar);
	}

	public CSSValue minus(CSSValue val)
	{
		//assert that the current object is the higher property value
		Assert.isTrue(this.parentSize > 0);
		Assert.isTrue(val.parentSize == 0);

		int px = pixels;
		if (parentSize > 0 && percentage == 0)
		{
			px = val.percentage == 0 ? parentSize - pixels : (-1) * pixels;
		}

		return new CSSValue((percentage != 0 ? percentage : 100) - val.percentage, px - val.pixels);
	}


	public CSSValue plus(CSSValue val)
	{
		int o1_percentage = parentSize > 0 ? 100 - percentage : percentage;
		int o1_pixels = parentSize > 0 && percentage == 0 ? parentSize - pixels : pixels;

		int o2_percentage = val.parentSize > 0 ? 100 - val.percentage : val.percentage;
		int o2_pixels = val.parentSize > 0 && val.percentage == 0 ? val.parentSize - val.pixels : val.pixels;

		return new CSSValue(o1_percentage + o2_percentage, o1_pixels + o2_pixels);
	}

	@Override
	public String toString()
	{
		if (percentage != 0 && pixels != 0)
		{
			return "calc(" + percentage + "% " + (pixels > 0 ? "+ " : "- ") + Math.abs(pixels) + "px)";
		}
		if (percentage != 0)
		{
			return percentage + "%";
		}
		return pixels + "";
	}
}
