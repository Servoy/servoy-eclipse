/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

/*******************************************************************************
 * Copyright (c) 2015 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package com.servoy.eclipse.notification.mylyn;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;

import com.servoy.eclipse.model.util.ServoyLog;

public class E4ThemeColor
{
	public static RGB getRGBFromCssString(String cssValue) {
		try {
			if (cssValue.startsWith("rgb(")) { //$NON-NLS-1$
				String rest = cssValue.substring(4, cssValue.length());
				int idx = rest.indexOf("rgb("); //$NON-NLS-1$
				if (idx != -1) {
					rest = rest.substring(idx + 4, rest.length());
				}
				idx = rest.indexOf(")"); //$NON-NLS-1$
				if (idx != -1) {
					rest = rest.substring(0, idx);
				}
				String[] rgbValues = rest.split(","); //$NON-NLS-1$
				if (rgbValues.length == 3) {
					return new RGB(Integer.parseInt(rgbValues[0].trim()), Integer.parseInt(rgbValues[1].trim()),
							Integer.parseInt(rgbValues[2].trim()));
				}
			} else if (cssValue.startsWith("#")) { //$NON-NLS-1$
				String rest = cssValue.substring(1, cssValue.length());
				int idx = rest.indexOf("#"); //$NON-NLS-1$
				if (idx != -1) {
					rest = rest.substring(idx + 1, rest.length());
				}
				if (rest.length() > 5) {
					return new RGB(Integer.parseInt(rest.substring(0, 2), 16),
							Integer.parseInt(rest.substring(2, 4), 16), Integer.parseInt(rest.substring(4, 6), 16));
				}
			}
			throw new E4CssParseException("RGB", cssValue); //$NON-NLS-1$
		} catch (NumberFormatException | E4CssParseException e) {
			ServoyLog.logError(e);
			return null;
		}
	}	
	
	public static String getCssValueFromTheme(Display display, String value) {
		// use reflection so that this can build against Eclipse 3.x
		BundleContext context = FrameworkUtil.getBundle(GradientColors.class).getBundleContext();
		try {
			Object reference = MethodUtils.invokeMethod(context, "getServiceReference", //$NON-NLS-1$
					"org.eclipse.e4.ui.css.swt.theme.IThemeManager"); //$NON-NLS-1$
			if (reference != null) {
				Object iThemeManager = MethodUtils.invokeMethod(context, "getService", reference); //$NON-NLS-1$
				if (iThemeManager != null) {
					Object themeEngine = MethodUtils.invokeMethod(iThemeManager, "getEngineForDisplay", display); //$NON-NLS-1$
					if (themeEngine != null) {
						CSSStyleDeclaration shellStyle = getStyleDeclaration(themeEngine, display);
						if (shellStyle != null) {
							CSSValue cssValue = shellStyle.getPropertyCSSValue(value);
							if (cssValue != null) {
								return cssValue.getCssText();
							}
						}
					}
				}
			}
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			ServoyLog.logError(e);
			return null;
		}

		return null;
	}	
	
	private static CSSStyleDeclaration getStyleDeclaration(Object themeEngine, Display display)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Shell shell = display.getActiveShell();
		CSSStyleDeclaration shellStyle = null;
		if (shell != null) {
			shellStyle = retrieveStyleFromShell(themeEngine, shell);
		} else {
			for (Shell input : display.getShells()) {
				shellStyle = retrieveStyleFromShell(themeEngine, input);
				if (shellStyle != null) {
					break;
				}
			}
		}
		return shellStyle;
	}
	
	private static CSSStyleDeclaration retrieveStyleFromShell(Object themeEngine, Shell shell)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Object shellStyle = MethodUtils.invokeMethod(themeEngine, "getStyle", shell); //$NON-NLS-1$
		if (shellStyle instanceof CSSStyleDeclaration) {
			return (CSSStyleDeclaration) shellStyle;
		}
		return null;
	}	

}
