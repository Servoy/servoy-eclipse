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

package com.servoy.eclipse.exporter.mobile.launch.test;

import com.servoy.eclipse.jsunit.mobile.SuiteBridge;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public interface IMobileTestLaunchConstants
{
	public static final String LAUNCH_TEST_CONFIGURATION_TYPE_ID = "com.servoy.eclipse.mobile.test.launch";

	public static final String CLOSE_BROWSER_WHEN_DONE = "close_when_done";
	public static final String DEFAULT_CLOSE_BROWSER_WHEN_DONE = "true";
	public static final String CLIENT_CONNECT_TIMEOUT = "client_connect_timeout";
	public static final String DEFAULT_CLIENT_CONNECT_TIMEOUT = String.valueOf(SuiteBridge.DEFAULT_TEST_TREE_WAIT_TIMEOUT / 1000);
	public static final String AUTO_GENERATED = "auto_generated_#"; // launch configs that are generated automatically based on TestTarget from SolEx.
	// are are limited in number; running different methods each time should not result in too many launch configs;
	// the contents of this attribute is an int; the higher the int value, the more recent that config launch was used

}
