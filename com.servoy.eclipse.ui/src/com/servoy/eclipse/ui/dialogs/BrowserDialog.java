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

package com.servoy.eclipse.ui.dialogs;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.progress.IProgressService;
import org.json.JSONObject;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IMainConceptsPageAction;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.browser.BrowserFactory;
import com.servoy.eclipse.ui.browser.IBrowser;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.eclipse.ui.util.IAutomaticImportWPMPackages;
import com.servoy.eclipse.ui.views.TutorialView;
import com.servoy.eclipse.ui.wizards.ImportSolutionWizard;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 * @since 2020.03
 *
 */
public class BrowserDialog extends Dialog
{

	private String url;
	private IBrowser browser;
	private Shell shell;
	private final boolean showSkipNextTime;
	private static final int MIN_WIDTH = 900;
	private static final int MIN_HEIGHT = 600;

	/**
	 * @param parentShell
	 */
	public BrowserDialog(Shell parentShell, String url, boolean modal, boolean showSkipNextTime)
	{
		this(parentShell, url, modal, showSkipNextTime, false);
	}

	/**
	 * @param parentShell
	 */
	public BrowserDialog(Shell parentShell, String url, boolean modal, boolean showSkipNextTime, boolean resize)
	{
		super(parentShell, (resize ? SWT.RESIZE : SWT.NONE) | (modal ? SWT.PRIMARY_MODAL : SWT.MODELESS));
		this.url = url;
		this.showSkipNextTime = showSkipNextTime;
	}


	public Object open()
	{
		Shell parent = getParent();

		while (parent.getParent() instanceof Shell)
		{
			parent = (Shell)parent.getParent();
		}

		Rectangle size = parent.getBounds();
		int newWidth = (int)(size.width / 1.5) < MIN_WIDTH ? MIN_WIDTH : (int)(size.width / 1.5);
		int newHeight = (int)(size.height / 1.4) < MIN_HEIGHT ? MIN_HEIGHT : (int)(size.height / 1.4);
		Dimension newSize = new Dimension(newWidth, newHeight);

		int locationX, locationY;
		locationX = (size.width < newWidth ? size.width : size.width - newWidth) / 2 + size.x;
		locationY = (size.height < newHeight ? size.height : size.height - newHeight) / 2 + size.y;

		return this.open(new Point(locationX, locationY), newSize);
	}

	public Object open(Dimension size)
	{
		Shell parent = getParent();

		while (parent.getParent() instanceof Shell)
		{
			parent = (Shell)parent.getParent();
		}

		Rectangle parentSize = parent.getBounds();

		int locationX, locationY;
		locationX = (parentSize.width < size.width ? parentSize.width : parentSize.width - size.width) / 2 + parentSize.x;
		locationY = (parentSize.height < size.height ? parentSize.height : parentSize.height - size.height) / 2 + parentSize.y;

		return this.open(new Point(locationX, locationY), size);
	}

	public Object open(Point location, Dimension size)
	{
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | getStyle());
		shell.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		if (showSkipNextTime)
		{
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 1;
			shell.setLayout(gridLayout);
		}
		else
		{
			shell.setLayout(new FillLayout());
		}
		final Button[] showNextTime = new Button[1];

		browser = BrowserFactory.createBrowser(shell);
		LocationListener locationListener = new LocationListener()
		{
			@Override
			public void changing(LocationEvent event)
			{
				String loc = event.location;
				if (loc == null) return;
				if (loc.equals(url)) return;

				IntroURLParser parser = new IntroURLParser(loc);
				if (parser.hasIntroUrl())
				{
					// stop URL first.
					event.doit = false;
					// execute the action embedded in the IntroURL
					final IntroURL introURL = parser.getIntroURL();
					if (IntroURL.RUN_ACTION.equals(introURL.getAction()))
					{
						String pluginId = introURL.getParameter(IntroURL.KEY_PLUGIN_ID);
						String className = introURL.getParameter(IntroURL.KEY_CLASS);

						final Object actionObject = ModelLoaderUtil.createClassInstance(pluginId, className);

						if (actionObject instanceof IMainConceptsPageAction)
						{
							Display display = Display.getCurrent();
							BusyIndicator.showWhile(display, new Runnable()
							{
								public void run()
								{
									((IMainConceptsPageAction)actionObject).runAction(introURL);
								}
							});
							if (!shell.isDisposed()) shell.close();
							return;
						}
					}

					String importSample = introURL.getParameter("importSample");
					final String[] showTutorial = new String[] { null };
					if (importSample != null)
					{
						try (InputStream is = new URL(importSample.startsWith("https://") ? importSample
							: "https://" + importSample).openStream())
						{
							IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
							if (importSample.endsWith(".json"))
							{
								String content = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
								if (content != null && content.startsWith("{"))
								{
									JSONObject obj = new JSONObject(content);
									String solutionName = obj.optString("name", "");
									if (!"".equals(solutionName))
									{
										boolean install = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName) == null;
										boolean shouldOverwrite = !install ? askOverwriteSolution() : false;
										if (install || shouldOverwrite)
										{
											if (!isValidServerPresent()) return;
											if (!shell.isDisposed()) shell.close();
											progressService.run(true, false, (IProgressMonitor monitor) -> {
												monitor.beginTask("Installing solution " + solutionName, 1);
												List<IAutomaticImportWPMPackages> defaultImports = ModelUtils
													.getExtensions(IAutomaticImportWPMPackages.EXTENSION_ID);
												if (defaultImports != null && defaultImports.size() > 0)
												{
													defaultImports.get(0).importPackage(obj, null);
												}
												monitor.worked(1);
												monitor.done();
											});
										}
									}
									else
									{
										UIUtils.reportError("Cannot install sample",
											"An error occured when trying to install the sample. Please try again later");
									}
								}
							}
							else
							{
								String[] urlParts = importSample.split("/");
								if (urlParts.length >= 1)
								{
									final String solutionName = urlParts[urlParts.length - 1].substring(0, urlParts[urlParts.length - 1].indexOf("."));
									boolean install = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName) == null;
									boolean shouldOverwrite = !install ? askOverwriteSolution() : false;
									if (install || shouldOverwrite)
									{
										if (!isValidServerPresent()) return;

										if (!shell.isDisposed()) shell.close();

										final File importSolutionFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(),
											solutionName + ".servoy");
										if (importSolutionFile.exists())
										{
											importSolutionFile.delete();
										}
										try (FileOutputStream fos = new FileOutputStream(importSolutionFile))
										{
											Utils.streamCopy(is, fos);
										}

										//TODO import packages if (importPackagesRunnable != null) progressService.run(true, false, importPackagesRunnable);
										progressService.run(true, false, (IProgressMonitor monitor) -> {
											ImportSolutionWizard importSolutionWizard = new ImportSolutionWizard();
											importSolutionWizard.setSolutionFilePath(importSolutionFile.getAbsolutePath());
											importSolutionWizard.setAllowSolutionFilePathSelection(false);
											importSolutionWizard.init(PlatformUI.getWorkbench(), null);
											importSolutionWizard.setReportImportFail(true);
											importSolutionWizard.setSkipModulesImport(false);
											importSolutionWizard.setAllowDataModelChanges(true);
											importSolutionWizard.setImportSampleData(true);
											importSolutionWizard.shouldAllowSQLKeywords(true);
											importSolutionWizard.shouldCreateMissingServer(true);
											importSolutionWizard.setOverwriteModule(shouldOverwrite);

											ServoyResourcesProject project = ServoyModelManager.getServoyModelManager().getServoyModel()
												.getActiveResourcesProject();
											String resourceProjectName = project == null ? getNewResourceProjectName() : null;

											importSolutionWizard.doImport(importSolutionFile, resourceProjectName, project, false, false, true, null, null,
												monitor, false, false, null);
											if (importSolutionWizard.isMissingServer() != null)
											{
												showTutorial[0] = introURL.getParameter("createDBConn");
											}

											try
											{
												importSolutionFile.delete();
											}
											catch (RuntimeException e)
											{
												Debug.error(e);
											}
										});
									}
									else
									{
										if (!shell.isDisposed()) shell.close();
										progressService.run(true, false, (IProgressMonitor monitor) -> {
											ServoyModelManager.getServoyModelManager()
												.getServoyModel()
												.setActiveProject(ServoyModelManager.getServoyModelManager()
													.getServoyModel()
													.getServoyProject(solutionName), true);
										});
									}
									ServoyModelManager.getServoyModelManager()
										.getServoyModel()
										.addActiveProjectListener(new IActiveProjectListener()
										{

											@Override
											public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
											{
												return true;
											}

											@Override
											public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
											{
											}

											@Override
											public void activeProjectChanged(ServoyProject activeProject)
											{
												Display.getDefault().asyncExec(() -> {
													if (introURL.getParameter("showTinyTutorial") != null)
													{
														showTinyTutorial(introURL);
														if (!shell.isDisposed()) shell.close();
													}
												});
												ServoyModelManager.getServoyModelManager()
													.getServoyModel()
													.removeActiveProjectListener(this);
											}
										});
								}
							}
						}
						catch (Exception e)
						{
							Debug.error(e);
						}
					}
					if (showTutorial[0] != null)
					{
						showTinyTutorial(showTutorial[0]);
						return;
					}
					else if (introURL.getParameter("showTinyTutorial") != null)
					{
						showTinyTutorial(introURL);
						if (!shell.isDisposed()) shell.close();
						return;
					}
					if (introURL.getParameter("maximize") != null)
					{
						if (showNextTime != null && showNextTime[0] != null)
						{
							showNextTime[0].setVisible(false);
						}
						Rectangle bounds = parent.getBounds();
						browser.setSize(bounds.width, bounds.height);
						shell.setBounds(bounds);
						shell.layout(true, true);
						return;
					}

					if (introURL.getParameter("normalize") != null)
					{
						Rectangle size = getParent().getBounds();
						Rectangle bounds = new Rectangle((size.width - (int)(size.width / 1.5)) / 2 + size.x,
							(size.height - (int)(size.height / 1.4)) / 2 + size.y, (int)(size.width / 1.5),
							(int)(size.height / 1.4));
						browser.setSize(bounds.width, bounds.height);
						shell.setBounds(bounds);
						shell.layout(true, true);
						return;
					}

					try
					{
						introURL.execute();
					}
					catch (Exception e)
					{
						Debug.error(e);
					}

				}
			}

			protected boolean isValidServerPresent()
			{
				if (Arrays.stream(ApplicationServerRegistry.get().getServerManager().getServerConfigs())
					.filter(
						s -> s.isEnabled() &&
							ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()) != null &&
							((IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()))
								.isValid())
					.count() == 0)
				{
					// no valid servers
					UIUtils.reportError("No valid server",
						"There is no valid server defined in Servoy Developer, you must define servers / install PostgreSQL before importing the sample solution.");
					return false;
				}
				return true;
			}

			protected boolean askOverwriteSolution()
			{
				boolean[] overwrite = new boolean[] { false };
				Display.getDefault().syncExec(() -> {

					overwrite[0] = UIUtils.askConfirmation(UIUtils.getActiveShell(),
						"Sample already exists in the workspace",
						"Do you want to fully overwrite the installed sample again?");
				});
				return overwrite[0];
			}

			private String getNewResourceProjectName()
			{
				String newResourceProjectName = "resources";
				int counter = 1;
				while (ServoyModel.getWorkspace().getRoot().getProject(newResourceProjectName).exists())
				{
					newResourceProjectName = "resources" + counter++;
				}
				return newResourceProjectName;
			}

			protected void showTinyTutorial(final String tutorialUrl)
			{
				try
				{
					TutorialView view = (TutorialView)PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView(TutorialView.PART_ID);
					view.openTutorial(tutorialUrl.startsWith("https://") ? tutorialUrl : "https://" + tutorialUrl);
				}
				catch (PartInitException e)
				{
					Debug.error(e);
				}
			}

			protected void showTinyTutorial(final IntroURL introURL)
			{
				try
				{
					TutorialView view = (TutorialView)PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView(TutorialView.PART_ID);
					view.openTutorial(introURL.getParameter("showTinyTutorial").startsWith("https://") ? introURL.getParameter("showTinyTutorial")
						: "https://" + introURL.getParameter("showTinyTutorial"));
				}
				catch (PartInitException e)
				{
					Debug.error(e);
				}
			}

			@Override
			public void changed(LocationEvent event)
			{
			}
		};
		browser.addLocationListener(locationListener);
		browser.setUrl(url);
		browser.setSize(size.width, size.height);
		if (showSkipNextTime)
		{
			browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			showNextTime[0] = new Button(shell, SWT.CHECK);
			showNextTime[0].setText("Do not show this dialog anymore");
			showNextTime[0].setSelection(!Utils.getAsBoolean(Settings.getInstance().getProperty(StartupPreferences.STARTUP_SHOW_START_PAGE, "true")));
			showNextTime[0].addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					Settings.getInstance().setProperty(StartupPreferences.STARTUP_SHOW_START_PAGE,
						new Boolean(!showNextTime[0].getSelection()).toString());
				}
			});
			showNextTime[0].setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}
		shell.setLocation(location);
		// in chromium i have to set size, else it shows very small
		if (Util.isMac() || Util.isLinux() || browser.isChromium())
		{
			Rectangle rect = shell.computeTrim(location.x, location.y, size.width, size.height);
			shell.setSize(rect.width, rect.height);
		}
		else
		{
			shell.pack();
		}
		shell.open();
		if (getStyle() == SWT.APPLICATION_MODAL)
		{
			Display display = parent.getDisplay();
			while (!shell.isDisposed())
			{
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		return null;
	}

	public boolean isDisposed()
	{
		return shell == null || shell.isDisposed();
	}


	/**
	 * @param optString
	 */
	public void setUrl(String url)
	{
		this.url = url;
		browser.setUrl(url);
	}

	public void setLocationAndSize(Point location, Dimension size)
	{
		browser.setSize(size.width, size.height);
		shell.setLocation(location);
		shell.setSize(size.width, size.height);
	}
}
