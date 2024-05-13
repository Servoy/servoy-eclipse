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
	private boolean isHigherProperty;

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

	public CSSValue(String value, int parentSize, boolean isHigherProperty)
	{
		super();
		parseCSSValue(value);
		this.parentSize = parentSize;
		this.isHigherProperty = isHigherProperty; //right or bottom
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

	public int getAsPixels()
	{
		if (isHigherProperty && percentage == 100)
		{
			return parentSize + pixels;
		}
		int percentageInPixels = percentage * parentSize / 100;
		return isHigherProperty ? parentSize - pixels - percentageInPixels : pixels + percentageInPixels;
	}

	public CSSValue div(int scalar)
	{
		CSSValue val = new CSSValue(percentage / scalar, pixels / scalar);
		val.parentSize = parentSize;
		val.isHigherProperty = isHigherProperty;
		return val;
	}

	public CSSValue minus(CSSValue val)
	{
		int px = getAsPixels() - val.getAsPixels();
		int percentageDiff = 0;
		if (parentSize == val.parentSize && (percentage > 0 || val.percentage > 0))
		{
			percentageDiff = Math.max((isHigherProperty && percentage != 100 ? (100 - percentage) : percentage) - val.percentage, 0);
			if (percentageDiff > 0)
			{
				px -= Math.round(percentageDiff * parentSize / 100);
			}
			else
			{
				percentageDiff = isHigherProperty && percentage != 100 ? (100 - percentage) : percentage;
				px -= Math.round(percentageDiff * parentSize / 100);
			}
		}
		CSSValue res = new CSSValue(percentageDiff, px);
		res.parentSize = parentSize;
		res.isHigherProperty = false;
		return res;
	}


	public CSSValue plus(CSSValue val)
	{
		int o1_percentage = isHigherProperty ? 100 - percentage : percentage;
		int o1_pixels = isHigherProperty && percentage == 0 ? parentSize - pixels : pixels;

		int o2_percentage = val.isHigherProperty ? 100 - val.percentage : val.percentage;
		int o2_pixels = val.isHigherProperty && val.percentage == 0 ? val.parentSize - val.pixels : val.pixels;

		CSSValue res = new CSSValue(o1_percentage + o2_percentage, o1_pixels + o2_pixels);
		res.parentSize = parentSize;
		res.isHigherProperty = false;
		return res;
	}

	public CSSValue toHigherProperty()
	{
		Assert.isTrue(parentSize > 0);

		CSSValue res;
		if (this.percentage > 0)
		{
			res = new CSSValue(100 - this.percentage, (-1) * this.pixels);
		}
		else
		{
			res = new CSSValue(this.percentage, parentSize - this.pixels);
		}
		res.parentSize = parentSize;
		res.isHigherProperty = true;
		return res;
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
