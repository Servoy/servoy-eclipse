/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ngclient.ui.utils;

import java.util.Arrays;
import java.util.List;

/**
 * @author fbandrei
 *
 * In this class you can find constants that are used for the NG client project.
 *
 */
public class NGClientConstants
{
	public static final List<String> NG_BUILD_COMMAND = Arrays.asList("run-script", "build_debug");
	public static final String NPM_BUILD_JOB = "npmBuild";
	public static final String NPM_CONFIGURATION_TITLE_PROGRESS_BAR = "Running npm configurations ...";
	public static final List<String> NPM_INSTALL = Arrays.asList("install");
}
