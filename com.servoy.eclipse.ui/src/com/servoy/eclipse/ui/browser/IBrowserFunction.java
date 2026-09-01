/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ui.browser;

/**
 * Callback invoked from JavaScript running in an {@link IBrowser}.
 * Registered via {@link IBrowser#addBrowserFunction(String, IBrowserFunction)}
 * and backed by the browser-specific {@code BrowserFunction} implementation
 * (SWT or Chromium).
 *
 * @author jcompagner
 */
@FunctionalInterface
public interface IBrowserFunction
{
	/**
	 * Called when the JavaScript function with the registered name is invoked.
	 *
	 * @param arguments the arguments passed from JavaScript
	 * @return the value to return to JavaScript, or <code>null</code>
	 */
	public Object function(Object[] arguments);
}
