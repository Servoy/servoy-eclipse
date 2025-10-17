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
package com.servoy.eclipse.ui.node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jcompagner
 *
 */
public class SimpleDeveloperFeedback implements IDeveloperFeedback
{
	private final String code;
	private final String sample;
	private String tooltip;

	public SimpleDeveloperFeedback(String code, String sample, String tooltip)
	{
		this.code = code;
		this.sample = sample;
		this.tooltip = tooltip;

	}

	public String getCode()
	{
		return code;
	}

	public String getSample()
	{
		return sample;
	}

	private String prettyPrintToolTipText(String originalToolTipText)
	{
		if (originalToolTipText == null || (originalToolTipText != null && !originalToolTipText.contains("Array<"))) return originalToolTipText;

		String result = null;
		String ttt = originalToolTipText;

		Matcher m1 = Pattern.compile("<b>(.+?)</b>").matcher(ttt);
		if (m1.groupCount() > 0 && m1.find())
		{
			ttt = m1.group(1);
		}

		if (ttt != null)
		{
			Matcher m2 = Pattern.compile("\\(([^\\[]*)\\)").matcher(ttt);
			if (m2.groupCount() > 0 && m2.find())
			{
				String signatureMatch = m2.group(1);
				if (signatureMatch != null)
				{
					StringBuilder sb = new StringBuilder();
					sb.append("(");
					for (String p : signatureMatch.split(","))
					{
						String[] arr = p.split(":");
						sb.append(arr[0] + ":");
						String argType = arr[1];
						if (argType != null && argType.contains("Array"))
						{
							Matcher mm = Pattern.compile("\\<([^\\[]*)\\>").matcher(argType);
							if (mm.find())
							{
								String type = mm.group(1);
								argType = type + "[]";
							}
						}
						sb.append(argType + ",");
					}
					sb.deleteCharAt(sb.length() - 1);
					sb.append(")");

					result = m2.replaceFirst(sb.toString());
				}
			}
		}

		if (m1.groupCount() > 0)
		{
			result = m1.replaceFirst("<b>" + (result != null ? result : (ttt != null ? ttt : "")) + "</b>");
		}

		return (result != null ? result : originalToolTipText);
	}

	public String getToolTipText()
	{
		return prettyPrintToolTipText(tooltip);
	}

	public void setToolTipText(String toolTip)
	{
		this.tooltip = toolTip;
	}

}
