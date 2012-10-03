/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.wizards.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.extension.ShowMessagesPage.UIMessage;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.extension.IProgress;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.DependencyPath;
import com.servoy.extension.dependency.DependencyResolver;
import com.servoy.extension.dependency.DisallowVersionReplacementFilter;
import com.servoy.extension.dependency.ExtensionNode;
import com.servoy.extension.dependency.IDependencyPathFilter;
import com.servoy.extension.dependency.MultipleCriteriaChooser;
import com.servoy.extension.dependency.OnlyFinalVersionsFilter;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.util.Debug;

/**
 * This page shows information on the extension that is about to be installed and allows altering the dependency resolve settings.
 * When next is pressed, it will do the actual dependency resolve (with progress) and depending on it's results choose the next page to show.
 * @author acostescu
 */
public class DependencyResolvingPage extends ReviewOperationPage
{

	protected InstallExtensionWizardOptions dialogOptions;

	protected Object longRunningLock = new Object();
	protected boolean longRunningOpInProgress1 = false;
	protected boolean pleaseStoplongRunningOp1 = false;
	protected boolean longRunningOpInProgress2 = false;
	private final boolean fromFile;

	/**
	 * Creates a new dependency resolving page.
	 * @param pageName see super.
	 * @param state the state of the install extension process. It contains the information needed by this page. It will also be filled with info retrieved from this page in order for the wizard to go forward.
	 * @param dialogOptions initial check-box states are held in this object; they will be updated.
	 * @param fromFile true if the extension to be installed is from a local .exp file. (in this case getting the icon/other description will be fast)
	 */
	public DependencyResolvingPage(String pageName, InstallExtensionState state, InstallExtensionWizardOptions dialogOptions, boolean fromFile)
	{
		super(pageName, "Please review extension information", state); //$NON-NLS-1$
		this.fromFile = fromFile;
		this.dialogOptions = dialogOptions;
	}

	@Override
	protected void addMoreContent(final Composite topLevel)
	{
		Label separator1 = new Label(topLevel, SWT.SEPARATOR | SWT.HORIZONTAL);
		ExpandBar advancedResolvingCollapser = new ExpandBar(topLevel, SWT.V_SCROLL);
		Label separator2 = new Label(topLevel, SWT.SEPARATOR | SWT.HORIZONTAL);

		advancedResolvingCollapser.setBackground(state.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		advancedResolvingCollapser.setForeground(state.display.getSystemColor(SWT.COLOR_DARK_BLUE));
		advancedResolvingCollapser.addExpandListener(new ExpandListener()
		{
			public void itemExpanded(ExpandEvent e)
			{
				relayout();
			}

			public void itemCollapsed(ExpandEvent e)
			{
				relayout();
			}

			private void relayout()
			{
				state.display.asyncExec(new Runnable()
				{
					public void run()
					{
						topLevel.layout(true, true);
					}
				});
			}
		});

		Composite advancedResolvingComposite = new Composite(advancedResolvingCollapser, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.marginBottom = 5;
		gridLayout.verticalSpacing = 5;
		advancedResolvingComposite.setLayout(gridLayout);

		// partial fix for a Linux (Ubuntu) bug (that I think has to do with ExpandBar's implementation)
		advancedResolvingComposite.addControlListener(new ControlAdapter()
		{
			@Override
			public void controlResized(ControlEvent e)
			{
				topLevel.layout(true, true);
			}
		});

		Label separator3 = new Label(advancedResolvingComposite, SWT.SEPARATOR | SWT.HORIZONTAL);

		final Button allowUpgrades = new Button(advancedResolvingComposite, SWT.CHECK);
		final Button restoreDefaults = new Button(advancedResolvingComposite, SWT.FLAT);
		final Button allowDowngrades = new Button(advancedResolvingComposite, SWT.CHECK);

		restoreDefaults.setText("Restore defaults"); //$NON-NLS-1$
		allowUpgrades.setText("allow dependency upgrades"); //$NON-NLS-1$
		allowDowngrades.setText("allow dependency downgrades"); //$NON-NLS-1$

		allowUpgrades.setSelection(dialogOptions.allowUpgrades);
		allowDowngrades.setSelection(dialogOptions.allowUpgrades && dialogOptions.allowDowngrades);
		allowDowngrades.setEnabled(dialogOptions.allowUpgrades);
		allowUpgrades.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				dialogOptions.allowUpgrades = allowUpgrades.getSelection();
				allowDowngrades.setSelection(dialogOptions.allowUpgrades && dialogOptions.allowDowngrades);
				allowDowngrades.setEnabled(dialogOptions.allowUpgrades);
			}
		});
		allowDowngrades.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				dialogOptions.allowDowngrades = allowDowngrades.getSelection();
			}
		});

		final Button onlyFinal = new Button(advancedResolvingComposite, SWT.CHECK);
		onlyFinal.setText("only use final (numeric) dependency versions"); //$NON-NLS-1$
		onlyFinal.setSelection(dialogOptions.allowOnlyFinalVersions);
		onlyFinal.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				dialogOptions.allowOnlyFinalVersions = onlyFinal.getSelection();
			}
		});

		final Button allowLibConflicts = new Button(advancedResolvingComposite, SWT.CHECK);
		allowLibConflicts.setText("allow library conflicts"); //$NON-NLS-1$
		allowLibConflicts.setSelection(dialogOptions.allowLibConflicts);
		allowLibConflicts.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				dialogOptions.allowLibConflicts = allowLibConflicts.getSelection();
			}
		});

		restoreDefaults.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				InstallExtensionWizardOptions defaults = new InstallExtensionWizardOptions();
				dialogOptions.allowUpgrades = defaults.allowUpgrades;
				dialogOptions.allowDowngrades = defaults.allowDowngrades;
				dialogOptions.allowLibConflicts = defaults.allowLibConflicts;
				dialogOptions.allowOnlyFinalVersions = defaults.allowOnlyFinalVersions;

				allowUpgrades.setSelection(dialogOptions.allowUpgrades);
				allowDowngrades.setSelection(dialogOptions.allowUpgrades && dialogOptions.allowDowngrades);
				allowDowngrades.setEnabled(dialogOptions.allowUpgrades);
				onlyFinal.setSelection(dialogOptions.allowOnlyFinalVersions);
				allowLibConflicts.setSelection(dialogOptions.allowLibConflicts);
			}
		});

		// layout advanced options composite
		separator3.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gd.verticalIndent = 5;
		gd.horizontalIndent = 30;
		allowUpgrades.setLayoutData(gd);
		gd = new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 4);
		gd.verticalIndent = 5;
		restoreDefaults.setLayoutData(gd);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd.horizontalIndent = 70;
		allowDowngrades.setLayoutData(gd);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd.horizontalIndent = 30;
		onlyFinal.setLayoutData(gd);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd.horizontalIndent = 30;
		allowLibConflicts.setLayoutData(gd);

		ExpandItem collapsableItem = new ExpandItem(advancedResolvingCollapser, SWT.NONE, 0);
		collapsableItem.setControl(advancedResolvingComposite);
		collapsableItem.setText("Advanced dependency resolve options"); //$NON-NLS-1$
		collapsableItem.setHeight(advancedResolvingComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		collapsableItem.setImage(Activator.getDefault().loadImageFromBundle("dependency.gif")); //$NON-NLS-1$

		separator1.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		advancedResolvingCollapser.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		separator2.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	}

	@Override
	protected void fillData()
	{
		// downloading the file & parsing it is long running; it's not necessary for dependency resolving, so it can be skipped by user,
		// but would give a nice overview if it's ran completely
		final IRunnableWithProgress toRun = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor)
			{
				synchronized (longRunningLock)
				{
					if (pleaseStoplongRunningOp1)
					{
						// user has already clicked "Next"; he is not interested in more info
						return;
					}
					if (longRunningOpInProgress1 == false)
					{
						longRunningOpInProgress1 = true;
					}
				}
				try
				{
					monitor.beginTask("Getting extension", 11); //$NON-NLS-1$

					monitor.subTask("acquiring extension name..."); //$NON-NLS-1$
					DependencyMetadata[] dmds = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(state.extensionID,
						state.version, state.version));
					if (dmds != null && dmds.length == 1)
					{
						final String name = dmds[0].extensionName;
						if (name != null)
						{
							// switch back to display thread to update UI
							state.display.asyncExec(new Runnable()
							{
								public void run()
								{
//									extensionNameText.setText(name);
//									topLevel.layout(true, true);
									setDescription(name);
									getWizard().getContainer().updateTitleBar();
								}
							});
						}
					}
					monitor.worked(1);

					synchronized (longRunningLock)
					{
						if (monitor.isCanceled() || longRunningOpInProgress1 == false)
						{
							// just stop, user wants to go forward, not interested in extension details
							monitor.done();
							return;
						}
					}

					monitor.subTask("acquiring package..."); //$NON-NLS-1$
					final SubProgressMonitor m = new SubProgressMonitor(monitor, 8);
					IProgress progress = new IProgress()
					{
						public void start(int totalWork)
						{
							m.beginTask("", totalWork); //$NON-NLS-1$
						}

						public void worked(int worked)
						{
							m.worked(worked);
						}

						public void setStatusMessage(String message)
						{
							m.subTask("acquiring package - " + message); //$NON-NLS-1$
						}

						public boolean shouldCancelOperation()
						{
							return m.isCanceled();
						}
					};

					File f = state.extensionProvider.getEXPFile(state.extensionID, state.version, progress);
					m.done();

					synchronized (longRunningLock)
					{
						if (monitor.isCanceled() || longRunningOpInProgress1 == false)
						{
							// just stop, user wants to go forward, not interested in extension details
							monitor.done();
							return;
						}
					}

					// no use checking for cancel in the future cause we are only dealing with HDD access from now on
					monitor.subTask("parsing information..."); //$NON-NLS-1$
					EXPParser parser = state.getOrCreateParser(f);
					final ExtensionConfiguration xml;
					if (parser != null)
					{
						xml = parser.parseWholeXML();
					}
					else
					{
						xml = null;
					}
					monitor.worked(1);

					monitor.subTask("getting extension icon and description..."); //$NON-NLS-1$
					if (xml != null && xml.getInfo() != null)
					{
						if (xml.getInfo().description != null || xml.getInfo().url != null)
						{
							state.display.asyncExec(new Runnable()
							{
								public void run()
								{
									if (xml.getInfo().description != null)
									{
										setExtensionDescription(xml.getInfo().description, true);
									}
									if (xml.getInfo().url != null)
									{
										setExtensionProductUrl("<a href=\"" + xml.getInfo().url + "\">" + xml.getInfo().url + "</a>", true); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
									}
								}
							});
						}

						if (xml.getInfo().iconPath != null)
						{
							try
							{
								Image image = ExtensionUtils.runOnEntry(f, xml.getInfo().iconPath, new EntryInputStreamRunner<Image>()
								{
									public Image runOnEntryInputStream(InputStream is) throws IOException
									{
										return new Image(state.display, is);
									}
								}).getRight();

								if (image != null)
								{
									state.allocatedImages.add(image);
									// switch back to display thread to update UI
									final Image img = image;
									state.display.asyncExec(new Runnable()
									{
										public void run()
										{
											setExtensionIcon(img, true);
										}
									});
								}
							}
							catch (IOException e)
							{
								// we can't get the image for some reason
								Debug.warn(e);
							}
						}
					}
					monitor.worked(1);
					monitor.done();

				}
				finally
				{
					synchronized (longRunningLock)
					{
						longRunningOpInProgress1 = false;
						longRunningLock.notifyAll();
					}
				}
			}
		};

		if (fromFile)
		{
			// it will be fast
			runWithProgress(false, toRun);
		}
		else
		{
			// it might take a while to get the icon and other info
			// run later because getWizard().getContainer().run does not guarantee it will not block, even if it's called with fork = true
			state.display.asyncExec(new Runnable()
			{
				public void run()
				{
					runWithProgress(true, toRun);
				}
			});
		}
	}

	protected void runWithProgress(boolean fork, IRunnableWithProgress toRun)
	{
		try
		{
			getWizard().getContainer().run(fork, true, toRun);
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public IWizardPage getNextPage()
	{
		// the user might click next before the .exp is downloaded to show the icon; in that case request stop and wait for that to finish
		synchronized (longRunningLock)
		{
			if (longRunningOpInProgress1)
			{
				longRunningOpInProgress1 = false; // request stop
				try
				{
					longRunningLock.wait();
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
			}
			else
			{
				pleaseStoplongRunningOp1 = true; // not started, tell it to never start
			}
			longRunningOpInProgress2 = true;
		}

		final String[] failMessage = new String[1]; // array [1] to be able to alter it in runnable
		final Message[][] warnings = new Message[1][]; // array [1] to be able to alter it in runnable
		state.chosenPath = null;

		// prepare & start dependency resolving

		// acquire already installed extensions
		if (state.installDir != null)
		{
			try
			{
				getWizard().getContainer().run(true, false, new IRunnableWithProgress()
				{
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
					{
						try
						{
							monitor.beginTask("Preparing to install", 10); //$NON-NLS-1$
							monitor.subTask("Checking installed extensions..."); //$NON-NLS-1$

							if (state.installedExtensionsProvider != null)
							{
								final DependencyResolver resolver = new DependencyResolver(state.extensionProvider);
								resolver.setInstalledExtensions(state.installedExtensionsProvider.getAllAvailableExtensions());
								resolver.setIgnoreLibConflicts(dialogOptions.allowLibConflicts);
								monitor.worked(4);

								monitor.subTask("Resolving dependencies..."); //$NON-NLS-1$
								resolver.resolveDependencies(state.extensionID, state.version);
								Message[] resolveWarnings = resolver.getMessages();
								if (resolveWarnings.length > 0) Debug.trace(Arrays.asList(resolveWarnings).toString());
								monitor.worked(4);

								List<DependencyPath> installPaths = resolver.getResults();
								if (installPaths != null && installPaths.size() > 0)
								{
									monitor.subTask("Filtering configurations..."); //$NON-NLS-1$
									int prevSize = installPaths.size();
									String s1 = null, s2 = null;
									IDependencyPathFilter filter;
									if (!dialogOptions.allowUpgrades || !dialogOptions.allowDowngrades)
									{
										filter = new DisallowVersionReplacementFilter(dialogOptions.allowUpgrades);
										filter.filterResolvePaths(installPaths);
										if (prevSize != installPaths.size()) s1 = filter.getFilterMessage();
									}
									if (dialogOptions.allowOnlyFinalVersions)
									{
										prevSize = installPaths.size();
										filter = new OnlyFinalVersionsFilter();
										filter.filterResolvePaths(installPaths);
										if (prevSize != installPaths.size()) s2 = filter.getFilterMessage();
									}
									monitor.worked(1);
									if (installPaths.size() > 0)
									{
										// dependency resolving successful
										monitor.subTask("Choosing best configuration..."); //$NON-NLS-1$
										MultipleCriteriaChooser chooser = new MultipleCriteriaChooser();
										state.chosenPath = chooser.pickResolvePath(installPaths); // we have a winner
										monitor.worked(1);
									}
									else
									{
										// a path was found, but dependency options denied it
										failMessage[0] = "Cannot resolve dependencies.\nTry changing dependency resolve options."; //$NON-NLS-1$
										int tmp = (s1 != null ? 1 : 0) + (s2 != null ? 1 : 0);
										warnings[0] = new Message[tmp];
										tmp = 0;
										if (s1 != null) warnings[0][tmp++] = new Message(s1, Message.INFO);
										if (s2 != null) warnings[0][tmp] = new Message(s2, Message.INFO);
									}
								}
								else
								{
									if (resolveWarnings.length == 1 && resolveWarnings[0].severity == Message.INFO)
									{
										// the extension is already installed probably; less alarming message needs to be shown
										failMessage[0] = resolveWarnings[0].message;
									}
									else
									{
										// no dependency path found; either because of dependency options (disallow lib conflicts) or because the dependency cannot be resolved
										failMessage[0] = "Cannot resolve dependencies. Potential reasons:\n(NOTE: the reasons below may not (all) be actual problems)"; //$NON-NLS-1$
									}
									warnings[0] = resolveWarnings;
								}
							}
							else
							{
								// should never happen
								failMessage[0] = "Cannot access installed extensions."; //$NON-NLS-1$
							}
							monitor.done();
						}
						finally
						{
							synchronized (longRunningLock)
							{
								longRunningOpInProgress2 = false;
								longRunningLock.notifyAll();
							}
						}
					}
				});
			}
			catch (InvocationTargetException e)
			{
				longRunningOpInProgress2 = false;
				ServoyLog.logError(e);
				failMessage[0] = e.getMessage();
			}
			catch (InterruptedException e)
			{
				longRunningOpInProgress2 = false;
				ServoyLog.logError(e);
				failMessage[0] = e.getMessage();
			}

			synchronized (longRunningLock)
			{
				if (longRunningOpInProgress2) try
				{
					longRunningLock.wait();
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
					failMessage[0] = e.getMessage();
				}
			}
		}
		else
		{
			// should never happen
			failMessage[0] = "Problem accessing install directory."; //$NON-NLS-1$
		}

		IWizardPage nextPage;
		// show correct next page based on 'failMessage' and 'warnings'
		Message[] exp1W = state.extensionProvider.getMessages();
		Message[] exp2W = state.installedExtensionsProvider.getMessages();
		if (failMessage[0] != null || exp2W.length > 0 || state.chosenPath == null)
		{
			if (failMessage[0] == null)
			{
				// new unhandled branch in above code? maybe it's because of installedExtensionsProvider warnings...
				Debug.warn("Chosen path should never be null without a fail message."); //$NON-NLS-1$
				failMessage[0] = "Dependency resolve failed."; //$NON-NLS-1$
			}
			// show problems page with failMessage as description and warnings as list (which could be null)
			List<Message> allWarnings = new ArrayList<Message>((warnings[0] != null ? warnings[0].length : 0) + exp1W.length + exp2W.length);
			if (warnings[0] != null) allWarnings.addAll(Arrays.asList(warnings[0]));
			allWarnings.addAll(Arrays.asList(exp2W));
			allWarnings.addAll(Arrays.asList(exp1W));

			Message[] messages;
			if (allWarnings.size() > 0)
			{
				messages = allWarnings.toArray(new Message[allWarnings.size()]);
			}
			else
			{
				messages = null;
			}
			nextPage = new ShowMessagesPage("DepWarnings", "Cannot install extension", failMessage[0], null, messages, true, null); //$NON-NLS-1$//$NON-NLS-2$
			nextPage.setWizard(getWizard());
		}
		else
		{
			// dependency resolving succeeded

			// prepare install page; afterwards, we might postpone it for after some dummy info/warning page
			nextPage = new ActualInstallPage("DoInstall", state); //$NON-NLS-1$
			nextPage.setWizard(getWizard());

			if (state.chosenPath.extensionPath.length > 1 ||
				(state.chosenPath.extensionPath.length == 1 && state.chosenPath.extensionPath[0].resolveType == ExtensionNode.DOWNGRADE_RESOLVE))
			{
				// more extensions are to be installed/replaced (or one is down-graded); tell the user
				UIMessage[] messages;
				Image addIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD);
				Image upgradeIcon = Activator.getDefault().loadImageFromBundle("upgrade.gif"); //$NON-NLS-1$
				Image downgradeIcon = Activator.getDefault().loadImageFromBundle("downgrade.gif"); //$NON-NLS-1$

				String[] header = new String[] { "", "From", "To", "Name", "Id" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
				messages = new UIMessage[state.chosenPath.extensionPath.length];
				for (int i = state.chosenPath.extensionPath.length - 1; i >= 0; i--)
				{
					ExtensionNode ext = state.chosenPath.extensionPath[i];
					DependencyMetadata[] newOne = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(ext.id, ext.version,
						ext.version));
					String name = ""; //$NON-NLS-1$
					if (newOne != null && newOne.length == 1)
					{
						name = newOne[0].extensionName;
					}

					Image icon;
					switch (ext.resolveType)
					{
						case ExtensionNode.DOWNGRADE_RESOLVE :
							icon = downgradeIcon;
							break;
						case ExtensionNode.UPGRADE_RESOLVE :
							icon = upgradeIcon;
							break;
						case ExtensionNode.SIMPLE_DEPENDENCY_RESOLVE :
						default :
							icon = addIcon;
					}
					messages[i] = new UIMessage(icon, new String[] { ext.installedVersion != null ? ext.installedVersion : "-", ext.version, name, ext.id }); //$NON-NLS-1$
				}

				nextPage = new ShowMessagesPage("DepReview", "Changes", "Review installation changes.", header, messages, true, nextPage); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				nextPage.setWizard(getWizard());
			} // else just use nextPage to install

			// check to see if extension provider has any warnings (for example connection failures for some of the required data)
			if (exp1W.length > 0)
			{
				// user should know about these; or should we just consider this step failed directly?
				nextPage = new ShowMessagesPage(
					"DepWarnings", "Some items require your attention", "However, you can continue with the install process.", null, exp1W, true, nextPage); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				nextPage.setWizard(getWizard());
			}
		}

		return nextPage;
	}
}
