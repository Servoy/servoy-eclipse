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

package com.servoy.eclipse.ui.tweaks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.tweaks.bytecode.weave.ImageDescriptorWeaver;
import com.servoy.j2db.util.Pair;

/**
 * This class contains the mappings between old images locations and new image locations that are used to replace icons in non Servoy plug-ins.
 * It is done via byte-code weaving on ImageDescriptor. See {@link ImageDescriptorWeaver}.
 *
 * @author acostescu
 */
public class ImageReplacementMapper
{
	public static final String DEFAULT_ICONS_PATH = "icons";
	public static final String DARK_ICONS_PATH = "darkicons";

	// just logging interceptable images stuff
	private static boolean LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS = false;

	private static Set<String> interceptableUrls = null;
	private static Set<Pair<Class< ? >, String>> interceptableFiles = null;

	// replacements loaded from extension points
	private final static Map<URL, AlternateImageLocation> urlReplacements = new HashMap<>();
	private final static Map<Pair<String, String>, AlternateImageLocation> classAndFileNameReplacements = new HashMap<>();

	private static interface AlternateImageLocation
	{
		ImageDescriptor createAlternateImage(Method urlCreatorMethod, Method classAndFileNameCreatorMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
	}

	private static class AlternateURLImageLocation implements AlternateImageLocation
	{

		private final URL url;

		public AlternateURLImageLocation(URL url)
		{
			this.url = url;
		}

		@Override
		public ImageDescriptor createAlternateImage(Method urlCreatorMethod, Method classAndFileNameCreatorMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
		{
			return (ImageDescriptor)urlCreatorMethod.invoke(null, url);
		}

	}

	private static class AlternateClassAndFileNameImageLocation implements AlternateImageLocation
	{

		private final Class< ? > relativeToClass;
		private final String fileName;

		public AlternateClassAndFileNameImageLocation(Class< ? > relativeToClass, String fileName)
		{
			this.relativeToClass = relativeToClass;
			this.fileName = fileName;
		}

		@Override
		public ImageDescriptor createAlternateImage(Method urlCreatorMethod, Method classAndFileNameCreatorMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
		{
			return (ImageDescriptor)classAndFileNameCreatorMethod.invoke(null, relativeToClass, fileName);
		}

	}

	private static void fill()
	{
		if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
		{
			interceptableUrls = new HashSet<>(128);
			interceptableFiles = new HashSet<>(32);
		}
		try
		{
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/new_wiz.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/new.png"));
// TODO			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/etool16/importdir_wiz.png"),
//				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/open.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/etool16/save_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/save.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/dtool16/save_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/save-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/etool16/saveas_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/save-as.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/dtool16/saveas_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/save-as-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/etool16/saveall_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/save-all.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/dtool16/saveall_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/save-all-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/pin_editor.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/pin_editor.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/print_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/print.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/import_wiz.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/import.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/export_wiz.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/export.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/$nl$/icons/full/elcl16/refresh_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/refresh.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/home_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/home.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dlcl16/home_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/home-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/$nl$/icons/full/etool16/search.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/dtool16/search_src.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/etool16/search_src.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/etool16/prev_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/back-to-annotation.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/dtool16/prev_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/back-to-annotation-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/etool16/next_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/forward-to-annotation.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/dtool16/next_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/forward-to-annotation-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/backward_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/backward_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dlcl16/backward_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/backward_nav-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/forward_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/forward_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dlcl16/forward_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/forward_nav-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/etool16/last_edit_pos.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/back-to-last-edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/dtool16/last_edit_pos.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/back-to-last-edit-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.externaltools/$nl$/icons/full/obj16/external_tools.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/launch_last_tool.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/skip_brkp.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/skip_breakpoints.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/undo_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/undo.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/undo_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/undo.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/redo_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/redo.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/redo_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/redo.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/cut_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/cut_edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/cut_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/cut_edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/copy_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/copy_edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/copy_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/copy_edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/paste_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/paste_edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/paste_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/paste_edit.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/delete_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/delete.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dtool16/delete_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/delete.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/etool16/next_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/next_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/etool16/prev_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/prev_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/$nl$/icons/full/elcl16/tsearch_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search_file.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/etool16/build_exec.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/build_all.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/dtool16/build_exec.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/build_all.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/obj16/brkp_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/breakpoint.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/obj16/readwrite_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/watchpoint.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/help_contents.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/help_contents.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/etool16/help_search.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/help_search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.equinox.p2.ui.sdk/icons/obj/iu_update_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/check_for_updates.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.equinox.p2.ui.sdk/icons/obj/iu_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/install_new_software.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.equinox.p2.ui.sdk/icons/obj/profile_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/installation_details.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/delete_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/delete.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dlcl16/delete_edit.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/delete.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/obj16/prj_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/project.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/eview16/debug_persp.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/debug.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/eview16/debug_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/debug.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/eview16/debug_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/debug.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide.application/$nl$/icons/full/eview16/resource_persp.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/resource.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/eview16/new_persp.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/open_perspective.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/eview16/tasks_tsk.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/tasks.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/eview16/tasks_tsk.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/tasks.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/eview16/problems_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/problems.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/eview16/problems_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/problems.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/eview16/problems_view_warning.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/problems.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/eview16/problems_view_error.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/problems_view_error.png"));
//TODO			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/elcl16/filter_ps.png"),
//				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/filters.png"));
//			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/dlcl16/filter_ps.png"),
//				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/filters-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/eview16/bkmrk_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/bookmarks.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/eview16/bkmrk_nav.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/bookmarks.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/$nl$/icons/full/eview16/searchres.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/eview16/searchres.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/cview16/console_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/$nl$/icons/full/cview16/console_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/elcl16/pin.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/clcl16/pin.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/dlcl16/pin.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/$nl$/icons/full/eview16/synch_synch.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/team_synchronizing.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/eview16/synch_synch.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/team_synchronizing.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/$nl$/icons/full/elcl16/synch_participants.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/team_synchronizing.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/elcl16/refresh.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/refresh.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/eview16/history_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/history.png"));
// TODO			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/dlcl16/history_nav.png"),
//				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/history_nav.png"));
//			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/dlcl16/history_nav.png"),
//				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/history_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/elcl16/pin.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/elcl16/synced.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/link_to_editor.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.team.ui/icons/full/dlcl16/pin.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/icons/full/eview16/outline_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/outline.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/$nl$/icons/full/eview16/outline_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/outline.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/icons/full/eview16/prop_ps.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/properties.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/$nl$/icons/full/eview16/prop_ps.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/properties.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/$nl$/icons/full/eview16/pview.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/progress.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.ide/icons/full/eview16/pview.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/progress.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/$nl$/icons/full/elcl16/filter_ps.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/show_advanced_properties.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/$nl$/icons/full/elcl16/tree_mode.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/show_categories.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/$nl$/icons/full/elcl16/defaults_ps.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/restore_default_value.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.views/$nl$/icons/full/dlcl16/defaults_ps.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/restore_default_value-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/cview16/console_view.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/display_selected_console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/elcl16/new_con.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/open_console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/clcl16/clear_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/clear_console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/dlcl16/clear_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/clear_console-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/clcl16/lock_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/scroll_lock.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/dlcl16/lock_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/scroll_lock.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/clcl16/wordwrap.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/word_wrap.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/dlcl16/wordwrap.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/word_wrap.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.workbench.texteditor/$nl$/icons/full/etool16/wordwrap.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/word_wrap.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.workbench.texteditor/$nl$/icons/full/dtool16/wordwrap.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/word_wrap.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/rem_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/rem_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/rem_all_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem_all.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/rem_all_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem_all-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/rem_all_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem_all.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/rem_all_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem_all-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.javascript.ui/icons/obj16/sourceEditor.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/js.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/etool16/mark_occurrences.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/mark_occurrences.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/dtool16/mark_occurrences.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/mark_occurrences-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/$nl$/icons/full/etool16/mark_occurrences.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/mark_occurrences.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/$nl$/icons/full/dtool16/mark_occurrences.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/mark_occurrences-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.workbench.texteditor/$nl$/icons/full/etool16/block_selection_mode.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/block_selection_mode.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.workbench.texteditor/$nl$/icons/full/dtool16/block_selection_mode.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/block_selection_mode-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.testing/$nl$/icons/full/eview16/testing.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/script_unit_test.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.testing/icons/full/eview16/testing.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/script_unit_test.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/resume_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/resume.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/resume_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/resume-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/resume_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/resume.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/resume_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/resume-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/suspend_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/suspend.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/suspend_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/suspend-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/suspend_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/suspend.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/suspend_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/suspend-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/stepinto_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_into.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/stepinto_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_into-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/stepinto_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_into.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/stepinto_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_into-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/stepover_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_over.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/stepover_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_over-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/stepover_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_over.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/stepover_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_over-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/stepreturn_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_return.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/stepreturn_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_return-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/stepreturn_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_return.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/stepreturn_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/step_return-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/stepbystep_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/use_step_filters.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/stepbystep_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/use_step_filters-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/stepbystep_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/use_step_filters.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/stepbystep_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/use_step_filters-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/com.servoy.eclipse.debug/icons/stop.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/stop.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/terminate_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/terminate.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/terminate_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/terminate-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/terminate_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/terminate.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/terminate_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/terminate-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/disconnect_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/disconnect.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/disconnect_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/disconnect-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/disconnect_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/disconnect.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/disconnect_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/disconnect-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/drop_to_frame.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/drop_to_frame.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/drop_to_frame.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/drop_to_frame-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/elcl16/drop_to_frame.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/drop_to_frame.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/dlcl16/drop_to_frame.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/drop_to_frame-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/eview16/variable_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/variable_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/eview16/variable_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/variable_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/eview16/breakpoint_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/breakpoint_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/eview16/breakpoint_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/breakpoint_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/eview16/watchlist_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/expressions.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/$nl$/icons/full/eview16/watchlist_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/expressions.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.debug.ui/icons/full/eview16/debug_console.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/interactive_console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.debug.ui/$nl$/icons/full/eview16/debug_console.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/interactive_console.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/tnames_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/show_type_names.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/tnames_co.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/show_type_names-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/var_cntnt_prvdr.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/show_logical_structure.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/var_cntnt_prvdr.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/show_logical_structure-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/elcl16/collapseall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/collapseall.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.debug.ui/icons/full/dlcl16/collapseall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/collapseall-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/com.servoy.eclipse.profiler/icons/viewicon.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/profiler_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/elcl16/trash.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/trash.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/dlcl16/trash.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/trash.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/pin_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/pin_view.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/pin_view.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/stop.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/terminate-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/stop.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/terminate.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/search_remall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem_all-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/search_remall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem_all.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/search_rem.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/search_rem.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/rem.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/search_next.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/next_nav-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/search_next.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/next_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/search_prev.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/prev_nav-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/search_prev.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/prev_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/expandall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/expandall-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/expandall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/expandall.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/collapseall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/collapseall-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/collapseall.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/collapseall.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/refresh.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/refresh-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/refresh.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/refresh.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/tsearch_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search_file.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/$nl$/icons/full/elcl16/tsearch_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search_file.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/dlcl16/search_history.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search_file-disabled.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/elcl16/search_history.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/icons/search_file.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.search/icons/full/obj16/line_match.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/forward_nav.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/methpub_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/method_public.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/methpro_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/method_protected.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/methpri_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/method_private.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/methdef_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/method_default.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/field_default_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/variable_default.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/field_private_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/variable_private.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/obj16/field_public_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/variable_public.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/dlcl16/alphab_sort_co.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/sort.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/elcl16/alphab_sort_co.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/sort.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/elcl16/filter_fields.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/hide_variables.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/elcl16/filter_methods.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/hide_method.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.ui/icons/full/elcl16/filter_classes.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/hide_classes.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.jdt.ui/$nl$/icons/full/etool16/opentype.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/open_type.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.dltk.javascript.ui/icons/obj16/jsearch_obj.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/javascript_search.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui/icons/full/obj16/fldr_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/project.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/$nl$/icons/full/obj16/file_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/file_obj.png"));
			urlReplacements.put(new URL("platform:/plugin/org.eclipse.ui.editors/icons/full/obj16/file_obj.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/file_obj.png"));
			urlReplacements.put(new URL("platform:/plugin/com.servoy.eclipse.designer/icons/designer.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui/{0}/designer.png"));
			urlReplacements.put(new URL("platform:/plugin/com.servoy.eclipse.ui/icons/portal.png"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui/{0}/portal.png"));
		}
		catch (Exception e)
		{
			System.err.println("Url replacements parsing going wrong: " + e.getMessage());
			e.printStackTrace();
		}
		try
		{
			//   some toolbar actions in the old editor
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignbottom.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignbottom.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/aligncenter.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/aligncenter.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignleft.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignleft.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignmid.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignmid.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignright.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignright.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/aligntop.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/aligntop.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignbottom_d.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignbottom-disabled.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/aligncenter_d.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/aligncenter-disabled.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignleft_d.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignleft-disabled.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignmid_d.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignmid-disabled.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/alignright_d.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/alignright-disabled.png"));
			classAndFileNameReplacements.put(new Pair<>("org.eclipse.gef.internal.InternalImages", "icons/aligntop_d.gif"),
				formatUrl("platform:/plugin/com.servoy.eclipse.ui.tweaks/{0}/aligntop-disabled.png"));
		}
		catch (Exception e)
		{
			System.err.println("ClassAndFileName Url replacements parsing going wrong: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static AlternateURLImageLocation formatUrl(String alternateURL) throws MalformedURLException
	{
		return new AlternateURLImageLocation(new URL(MessageFormat.format(alternateURL, new Object[] { getIconsPath() })));
	}

	/**
	 * Creates and returns alternate image descriptors (if needed) from files.
	 * If there is no alternate image configured for this location, it just returns the original one.
	 *
	 * @param originalCreateFromFile the original implementation of {@link ImageDescriptor#createFromFile(Class, String)}.
	 *
	 * @throws InvocationTargetException if an attempt to call the original {@link ImageDescriptor#createFromFile(Class, String)} failed.
	 * @throws IllegalArgumentException if an attempt to call the original {@link ImageDescriptor#createFromFile(Class, String)} failed.
	 * @throws IllegalAccessException if an attempt to call the original {@link ImageDescriptor#createFromFile(Class, String)} failed.
	 *
	 * @return see description above.
	 */
	public static ImageDescriptor getFileBasedImageReplacement(Class< ? > classLocation, String fileName, final Method originalCreateFromURL,
		final Method originalCreateFromFile) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if (PlatformUI.isWorkbenchRunning())
		{
			if (urlReplacements.size() == 0) fill();
			AlternateImageLocation replacement = classAndFileNameReplacements.get(new Pair<>(classLocation.getName(), fileName));

			if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
			{
				if (interceptableFiles.add(new Pair<Class< ? >, String>(classLocation, fileName)))
				{
					System.out.println(
						(replacement == null ? "(ORIGINAL)" : "(REPLACED)") + " FileBasedImageReplacer: (" + classLocation + ", " + fileName + ")");
				}
			}

			if (replacement != null) return replacement.createAlternateImage(originalCreateFromURL, originalCreateFromFile);
		}
		else if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
		{
			System.out.println("skipped the url " + fileName + " because workbench is not running yet");
		}
		return (ImageDescriptor)originalCreateFromFile.invoke(null, new Object[] { classLocation, fileName });
	}

	/**
	 * Creates and returns alternate image descriptors (if needed) from URLs.
	 * If there is no alternate image configured for this URL, it just returns the original one.
	 *
	 * @param originalCreateFromURL the original implementation of {@link ImageDescriptor#createFromURL(URL)}.
	 *
	 * @throws InvocationTargetException if an attempt to call the original {@link ImageDescriptor#createFromURL(URL)} failed.
	 * @throws IllegalArgumentException if an attempt to call the original {@link ImageDescriptor#createFromURL(URL)} failed.
	 * @throws IllegalAccessException if an attempt to call the original {@link ImageDescriptor#createFromURL(URL)} failed.
	 *
	 * @return see description above.
	 * @throws IOException
	 */
	public static ImageDescriptor getUrlBasedImageReplacement(URL url, final Method originalCreateFromURL, final Method originalCreateFromFile)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException
	{
		if (PlatformUI.isWorkbenchRunning())
		{
			if (urlReplacements.size() == 0) fill();
			URL stableUrl = url;
			if (String.valueOf(url).startsWith("bundleentry://"))
			{
				URI uri = URI.createURI(String.valueOf(url));
				long bundleId = Long.parseLong(uri.host().split("\\.")[0]);//the first number after bundleentry:// is the bundle id
				String name = Activator.getDefault().getBundle().getBundleContext().getBundle(bundleId).getSymbolicName();
				if (name != null) stableUrl = new URL(URI.createPlatformPluginURI(name + uri.path(), true).toString());
			}

			AlternateImageLocation replacement = urlReplacements.get(stableUrl);

			if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
			{
				if (interceptableUrls.add(String.valueOf(stableUrl)))
				{
					System.out.println((replacement == null ? "(ORIGINAL)" : "(REPLACED)") + " URLBasedImageReplacer: (" + stableUrl + ")");
				}
			}

			if (replacement != null) return replacement.createAlternateImage(originalCreateFromURL, originalCreateFromFile);
		}
		else if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
		{
			System.out.println("skipped the url " + url + " because workbench is not running yet");
		}
		return (ImageDescriptor)originalCreateFromURL.invoke(null, new Object[] { url });
	}


	private static String getIconsPath()
	{
		return IconPreferences.getInstance().getUseDarkThemeIcons() ? DARK_ICONS_PATH : DEFAULT_ICONS_PATH;
	}

}
