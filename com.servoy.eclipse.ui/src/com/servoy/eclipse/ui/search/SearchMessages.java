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

package com.servoy.eclipse.ui.search;

import org.eclipse.osgi.util.NLS;

/**
 * NLS resources
 * 
 * @author jcompagner
 * @since 6.0
 */
public class SearchMessages extends NLS
{

	private static final String BUNDLE_NAME = "com.servoy.eclipse.ui.search.SearchMessages";//$NON-NLS-1$

	static
	{
		NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
	}

	private SearchMessages()
	{
		// Do not instantiate
	}

	public static String FileSearchPage_sort_name_label;
	public static String FileSearchPage_sort_path_label;
	public static String FileSearchPage_open_file_failed;
	public static String FileSearchPage_open_file_dialog_title;
	public static String FileSearchPage_sort_by_label;
	public static String FileSearchPage_limited_format_matches;
	public static String FileSearchPage_limited_format_files;
	public static String FileLabelProvider_removed_resource_label;
	public static String FileLabelProvider_count_format;
	public static String FileLabelProvider_line_number;

}
