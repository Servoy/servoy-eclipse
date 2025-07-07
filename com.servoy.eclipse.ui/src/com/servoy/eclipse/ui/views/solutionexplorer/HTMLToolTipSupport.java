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

package com.servoy.eclipse.ui.views.solutionexplorer;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.dltk.javascript.ui.scriptdoc.JavaDoc2HTMLTextReader;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.tweaks.IconPreferences;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.util.Utils;

public class HTMLToolTipSupport extends ColumnViewerToolTipSupport
{

	protected HTMLToolTipSupport(ColumnViewer viewer, int style, boolean manualActivation)
	{
		super(viewer, style, manualActivation);
		setHideOnMouseDown(false);
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent)
	{
		setShift(new Point(1, 1));
		// we can't just use SWT.DEFAULT below to determine preferred height unfortunately (don't think Browser.compute... can detect that - from what I tested)
		// so, instead, we give these 4 sizing hints
		return createBrowserTooltipContentArea(this, getText(event), parent, true, 600, 450, 150, 50);
	}

	public static Composite createBrowserTooltipContentArea(ToolTip toolTip, String toolTipText, Composite parent, boolean hideIfMouseOnRightSide,
		int preferredWidthIfMultipleLines, int preferredWidthIfSingleLine, int preferredHeightIfMultipleLines, int preferredHeightIfSingleLine)
	{
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout l = new GridLayout(1, false);
		l.horizontalSpacing = 0;
		l.marginWidth = 0;
		l.marginHeight = 0;
		l.verticalSpacing = 0;

		comp.setLayout(l);
		final Browser browser = new Browser(comp, SWT.BORDER);
		browser.addLocationListener(new LocationAdapter()
		{
			@Override
			public void changing(LocationEvent event)
			{
				if (event.location != null && (event.location.startsWith("http:") || event.location.startsWith("https:")))
				{
					event.doit = false;
					try
					{
						EditorUtil.openURL(PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser(), event.location);
					}
					catch (Exception e)
					{
						ServoyLog.logInfo("Could not launch browser for location : " + event.location);
					}
					toolTip.hide();
				}
			}
		});
		browser.setJavascriptEnabled(false);
		if (hideIfMouseOnRightSide) browser.addListener(SWT.MouseMove, new Listener()
		{

			@Override
			public void handleEvent(Event e)
			{
				Rectangle rect = browser.getBounds();
				rect.x += 3;
				rect.y += 3;
				rect.width -= 6;
				rect.height -= 6;
				if (!rect.contains(e.x, e.y))
				{
					// if we are on the right side exit
					rect = browser.getBounds();
					rect.x += rect.width / 2;
					if (rect.contains(e.x, e.y))
					{
						toolTip.hide();
					}
				}
			}

		});
		String text = toolTipText;
		try (JavaDoc2HTMLTextReader reader = new JavaDoc2HTMLTextReader(new StringReader(text)))
		{
			text = reader.getString();
		}
		catch (IOException e)
		{
			ServoyLog.logInfo("Could not convert tooltip text to HTML: " + text);
		}
		Font f = JFaceResources.getFont(JFaceResources.DEFAULT_FONT);
		float systemDPI = Utils.isLinuxOS() ? 96f : 72f;
		int pxHeight = Math.round(f.getFontData()[0].getHeight() * Display.getDefault().getDPI().y / systemDPI);


		String bgColor = "#fffff6";
		String fgColor = null;
		if (IconPreferences.getInstance().getUseDarkThemeIcons())
		{
			Color darkBGColor = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
				.get("org.eclipse.ui.workbench.DARK_BACKGROUND");
			RGB backgroundColorRGB = darkBGColor != null ? darkBGColor.getRGB() : new RGB(31, 31, 31);
			bgColor = "rgb(" + backgroundColorRGB.red + "," + backgroundColorRGB.green + "," + backgroundColorRGB.blue + ")";
			Color darkFGColor = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
				.get("org.eclipse.ui.workbench.DARK_FOREGROUND");
			RGB foregroundColorRGB = darkFGColor != null ? darkFGColor.getRGB() : new RGB(204, 204, 204);
			fgColor = "rgb(" + foregroundColorRGB.red + "," + foregroundColorRGB.green + "," + foregroundColorRGB.blue + ")";
		}

		browser.setText("<html><body style='background-color:" + bgColor + ";" + (fgColor != null ? "color:" + fgColor + ";" : "") + "font-family:\"" +
			f.getFontData()[0].getName() + "\";font-size:" + pxHeight +
			"px;font-weight:normal'>" + text + "</body></html>");
		GridData data = (text.contains("<br>") || text.contains("<br/>") || text.contains("\n") || text.contains("<li>") || text.contains("<p>"))
			? new GridData(preferredWidthIfMultipleLines, preferredHeightIfMultipleLines)
			: new GridData(preferredWidthIfSingleLine, preferredHeightIfSingleLine);
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		browser.setLayoutData(data);

		return comp;
	}

	public static final void enableFor(ColumnViewer viewer, int style)
	{
		new HTMLToolTipSupport(viewer, style, false);
	}
}