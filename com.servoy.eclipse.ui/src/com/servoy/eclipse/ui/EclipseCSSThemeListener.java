/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui;

import java.io.InputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.Preferences;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ServoyMessageDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.MessageAndCheckBoxDialog;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.tweaks.IconPreferences;

/**
 * @author emera
 */
public class EclipseCSSThemeListener
{
	private static final String ECLIPSE_CSS_SWT_THEME = "org.eclipse.e4.ui.css.swt.theme";
	private static final String SCRIPT_EDITOR_PLUGIN_ID = "org.eclipse.dltk.javascript.ui";
	private static final String CSS_EDITOR_PLUGIN_ID = "org.eclipse.wst.css.ui";
	private static final String JSON_EDITOR_PLUGIN_ID = "org.sweetlemonade.eclipse.json";
	private static final String XML_EDITOR_PLUGIN_ID = "org.eclipse.wst.xml.ui";
	private static final String HTML_EDITOR_PLUGIN_ID = "org.eclipse.wst.html.ui";
	private static final String SQL_EDITOR_PLUGIN_ID = "net.sourceforge.sqlexplorer";

	private static EclipseCSSThemeListener instance;
	private IPreferenceChangeListener themeChangedListener;

	EclipseCSSThemeListener()
	{
	}

	public static EclipseCSSThemeListener getInstance()
	{
		if (instance == null)
		{
			instance = new EclipseCSSThemeListener();
		}
		return instance;
	}

	public void initThemeListener()
	{
		final BundleContext ctx = Activator.getDefault().getBundle().getBundleContext();
		final ServiceReference<IThemeManager> serviceReference = ctx.getServiceReference(IThemeManager.class);
		if (serviceReference != null)
		{
			final IThemeManager manager = ctx.getService(serviceReference);
			if (manager != null)
			{
				final Display d = Display.getDefault();
				final IThemeEngine engine = manager.getEngineForDisplay(d);
				if (engine != null)
				{
					final ITheme it = engine.getActiveTheme();
					if (it != null)
					{
						String label = it.getLabel();
						if (UIUtils.isDarkTheme(it.getId()) && !IconPreferences.getInstance().getUseDarkThemeIcons() ||
							!UIUtils.isDarkTheme(it.getId()) && IconPreferences.getInstance().getUseDarkThemeIcons())
						{
							IconPreferences.getInstance().setUseDarkThemeIcons(UIUtils.isDarkTheme(it.getId()));
							IconPreferences.getInstance().save(true);
							ServoyModelManager.getServoyModelManager().getServoyModel()
								.addDoneListener(() -> {
									if (checkOverwriteThemePreferences(UIUtils.isDarkTheme(it.getId())))
									{
										MessageAndCheckBoxDialog dialog = new MessageAndCheckBoxDialog(UIUtils.getActiveShell(),
											label + " theme was detected", null,
											"It is strongly recommended to restart the developer for the " + label +
												" theme preferences to be applied. Would you like to restart now?",
											"Setup script editor preferences for the selected theme (might overwrite exising values).", true,
											MessageDialog.QUESTION,
											new String[] { "Yes", "No" }, 0);
										dialog.open();
										if (dialog.isChecked())
										{
											setThemePreferences(it.getId());
										}
										if (dialog.getReturnCode() == 0)
										{
											PlatformUI.getWorkbench().restart();
										}
									}
									else
									{
										setThemePreferences(it.getId()); //if nothing is overwritten, just import preferences
										if (ServoyMessageDialog.openQuestion(UIUtils.getActiveShell(),
											label + " theme was detected",
											"It is strongly recommended to restart the developer for the " + label +
												" theme preferences to be applied. Would you like to restart now?"))
										{
											PlatformUI.getWorkbench().restart();
										}
									}
								});
						}
					}
					else if (IconPreferences.getInstance().getUseDarkThemeIcons())
					{
						IconPreferences.getInstance().setUseDarkThemeIcons(false);
						IconPreferences.getInstance().save(true);
						ServoyModelManager.getServoyModelManager().getServoyModel()
							.addDoneListener(() -> {
								if (ServoyMessageDialog.openQuestion(UIUtils.getActiveShell(),
									"Theming is disabled",
									"It is strongly recommended to restart the developer for the theming preferences to be applied. Would you like to restart now?"))
								{
									PlatformUI.getWorkbench().restart();
								}
							});
					}
				}
			}
		}

		listenForThemeChanges();
	}

	private void listenForThemeChanges()
	{
		themeChangedListener = (PreferenceChangeEvent event) -> {
			if (event.getKey().equals("themeid"))
			{
				String themeid = (String)event.getNewValue();
				IconPreferences iconPreferences = IconPreferences.getInstance();
				iconPreferences.setUseDarkThemeIcons(UIUtils.isDarkTheme(themeid));
				iconPreferences.save();
				if (checkOverwriteThemePreferences(UIUtils.isDarkTheme(themeid)))
				{
					Display.getDefault().asyncExec(() -> {
						if (org.eclipse.jface.dialogs.MessageDialog.openQuestion(UIUtils.getActiveShell(),
							"Import theme preferences",
							"Would you like to apply script editor preferences for the selected theme? It might overwrite exising values."))
						{
							setThemePreferences((String)event.getNewValue());
						}
					});
				}
				else
				{
					setThemePreferences((String)event.getNewValue());
				}
			}
		};
		InstanceScope.INSTANCE.getNode(ECLIPSE_CSS_SWT_THEME).addPreferenceChangeListener(themeChangedListener);
	}

	private void setThemePreferences(final String themeID)
	{
		try (InputStream is = getClass().getResourceAsStream("dark_editor_preferences.epf"))
		{
			IPreferencesService preferencesService = Platform.getPreferencesService();
			IExportedPreferences prefs = preferencesService
				.readPreferences(is);
			if (UIUtils.isDarkTheme(themeID))
			{
				IStatus status = preferencesService.applyPreferences(prefs);
				if (!status.isOK())
				{
					org.eclipse.jface.dialogs.MessageDialog.openWarning(UIUtils.getActiveShell(), "Could not import theme preferences",
						"Please check the Servoy wiki and import the theme preferences manually.\n" +
							status.getMessage());
				}
			}
			else
			{
				resetPreferencesToDefault(SCRIPT_EDITOR_PLUGIN_ID, prefs);
				resetPreferencesToDefault(CSS_EDITOR_PLUGIN_ID, prefs);
				resetPreferencesToDefault(JSON_EDITOR_PLUGIN_ID, prefs);
				resetPreferencesToDefault(XML_EDITOR_PLUGIN_ID, prefs);
				resetPreferencesToDefault(HTML_EDITOR_PLUGIN_ID, prefs);
				resetPreferencesToDefault(SQL_EDITOR_PLUGIN_ID, prefs);
			}
		}
		catch (Exception e)
		{
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
	}

	private void resetPreferencesToDefault(String pluginID, IExportedPreferences prefs)
	{
		try
		{
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(pluginID);
			for (String pref : prefs.node("instance").node(pluginID).keys())
			{
				preferences.remove(pref);
			}
			preferences.flush();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private boolean checkOverwriteThemePreferences(boolean dark)
	{
		try (InputStream is = getClass().getResourceAsStream("dark_editor_preferences.epf"))
		{
			IPreferencesService preferencesService = Platform.getPreferencesService();
			IExportedPreferences prefs = preferencesService.readPreferences(is);
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(SCRIPT_EDITOR_PLUGIN_ID);
			Preferences scriptPreferences = prefs.node("instance").node(SCRIPT_EDITOR_PLUGIN_ID);
			for (String pref : scriptPreferences.keys())
			{
				if (dark)
				{
					//we want to switch to dark but we have a non-default value for one of the script editor prefs
					if (preferences.get(pref, null) != null && !preferences.get(pref, null).equals(scriptPreferences.get(pref, null)))
					{
						return true;
					}
				}
				else
				{
					//we want to switch to light but we have a different value than the value which is in the dark theme prefs file
					String darkVal = scriptPreferences.get(pref, "");
					String actualVal = preferences.get(pref, "");
					if (!darkVal.equals(actualVal)) return true;
				}
			}
		}
		catch (Exception e)
		{
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
		return false;
	}

	public void removeListener()
	{
		if (themeChangedListener != null) InstanceScope.INSTANCE.getNode(ECLIPSE_CSS_SWT_THEME).removePreferenceChangeListener(themeChangedListener);
	}


}
