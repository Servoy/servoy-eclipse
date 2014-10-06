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

package com.servoy.eclipse.debug.scriptingconsole;

import org.eclipse.osgi.util.NLS;

/**
 * @author jcompagner
 *
 */
public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.servoy.eclipse.debug.scriptingconsole.messages";
	public static String ScriptingConsole_activeClients;
	public static String ScriptingConsole_headlessClientName;
	public static String ScriptingConsole_smartClientName;
	public static String ScriptingConsole_testClientsJobName;
	public static String ScriptingConsole_webClientName;
	public static String ScriptingConsole_ngClientName;
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}
