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

package com.servoy.eclipse.marketplace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.GrabExcessSpaceIn1ColumnTableListener;
import com.servoy.eclipse.ui.wizards.extension.InstallExtensionWizard;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.EXPParserPool;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.extension.MarketPlaceExtensionProvider;
import com.servoy.extension.Message;
import com.servoy.extension.VersionStringUtils;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Pair;

/**
 * Dialog that shows currently installed extensions and allows uninstall and checking for updates.
 * @author acostescu
 */
public class InstalledExtensionsDialog extends TrayDialog
{

	protected static final String INSTALLED_EXTENSIONS_SECTION = "InstalledExtensionsDialog"; //$NON-NLS-1$
	protected static final int UPDATE_CHECK_BUTTON_ID = 1017;
	public static final int SERIAL_RULE_ID = 1017;

	public static WeakReference<InstalledExtensionsDialog> createdInstance;

	protected IDialogSettings dialogSettings;
	protected boolean automaticUpdateCheck = false;

	protected Object dataLock = new Object();
	protected InstalledWithPendingExtensionProvider installedProvider;
	protected Pair<DependencyMetadata, DependencyMetadata>[] extensions;
	protected EXPParserPool parserPool = new EXPParserPool();
	protected List<Image> allocatedImages = new ArrayList<Image>();

	protected Table table;

	protected GrabExcessSpaceIn1ColumnTableListener grabExcessSpaceInColumnListener;

	/**
	 * Only at most one instance of this dialog should exist. You should not have for example one started by automatic update check
	 * and one started at the same time by the user Help action.
	 */
	public static synchronized InstalledExtensionsDialog getOrCreateInstance(Shell shell)
	{
		InstalledExtensionsDialog ref;
		if (createdInstance != null)
		{
			ref = createdInstance.get();
			if (ref != null)
			{
				return ref;
			}
		}
		ref = new InstalledExtensionsDialog(shell);
		createdInstance = new WeakReference<InstalledExtensionsDialog>(ref);
		return ref;
	}

	/**
	 * Creates a dialog that is able to show installed extensions, start uninstall or check for updates.
	 * Shows installed extensions when open.
	 * @param shell parent shell.
	 */
	private InstalledExtensionsDialog(Shell shell)
	{
		super(shell);

		// dialog Settings will be used to store/retrieve ignored updates (by "automatic update check at startup")
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(INSTALLED_EXTENSIONS_SECTION);
		if (section == null)
		{
			section = workbenchSettings.addNewSection(INSTALLED_EXTENSIONS_SECTION);
		}
		dialogSettings = section;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public void create()
	{
		super.create();
		getShell().setText("Installed Servoy Extensions"); //$NON-NLS-1$
		Image i1 = Activator.getDefault().loadImageFromBundle("extension16.png"); //$NON-NLS-1$
		Image i2 = Activator.getDefault().loadImageFromBundle("extension32.png"); //$NON-NLS-1$
		Image i3 = Activator.getDefault().loadImageFromBundle("extension64.png"); //$NON-NLS-1$
		Image i4 = Activator.getDefault().loadImageFromBundle("extension128.png"); //$NON-NLS-1$

		getShell().setImages(new Image[] { i1, i2, i3, i4 });
	}

	/**
	 * Meant to be called by "automatic update check at startup", called from within a job with a SerialRule using SERIAL_RULE_ID. 
	 */
	public boolean checkForUpdates(IProgressMonitor monitor)
	{
		this.automaticUpdateCheck = true;
		readInstalledExtensions();
		return checkForUpdatesInternal(monitor);
	}

	protected boolean checkForUpdatesInternal(IProgressMonitor monitor)
	{
		boolean foundUpdates = false;
		DependencyMetadata installedDmds[];
		DependencyMetadata availableUpdates[];
		MarketPlaceExtensionProvider marketPlaceProvider;
		synchronized (dataLock)
		{
			monitor.beginTask("Checking for available Servoy Extension upgrades...", extensions.length * 10 + 2); //$NON-NLS-1$
			installedDmds = new DependencyMetadata[extensions.length];
			availableUpdates = new DependencyMetadata[extensions.length];
			for (int i = extensions.length - 1; i >= 0; i--)
			{
				installedDmds[i] = extensions[i].getLeft();
			}
			File installDir = new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory()).getParentFile();
			marketPlaceProvider = new MarketPlaceExtensionProvider(installDir); // always create it in order not to use cached data in subsequent calls
		}
		monitor.worked(1);

		try
		{
			for (int i = installedDmds.length - 1; i >= 0; i--)
			{
				String[] versions = marketPlaceProvider.getAvailableVersions(installedDmds[i].id);
				DependencyMetadata bestVersion = installedDmds[i];
				for (String v : versions)
				{
					if (monitor.isCanceled())
					{
						foundUpdates = false;
						break;
					}
					if (VersionStringUtils.compareVersions(v, bestVersion.version) > 0)
					{
						DependencyMetadata[] vdmds = marketPlaceProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(installedDmds[i].id, v, v));
						if (vdmds != null &&
							vdmds.length == 1 &&
							(vdmds[0].getServoyDependency() == null || VersionStringUtils.belongsToInterval(VersionStringUtils.getCurrentServoyVersion(),
								vdmds[0].getServoyDependency().minVersion, vdmds[0].getServoyDependency().maxVersion)))
						{
							bestVersion = vdmds[0];
						}
					}
				}

				if (monitor.isCanceled())
				{
					foundUpdates = false;
					break;
				}

				if (bestVersion != installedDmds[i]) // could use == as well here
				{
					availableUpdates[i] = bestVersion;
					foundUpdates = true;
				}
			}
		}
		finally
		{
			Message[] msgs = marketPlaceProvider.getMessages();
			if (msgs.length > 0) ServoyLog.logInfo("While checking for upgrades, problems were found: " + Arrays.asList(msgs).toString()); //$NON-NLS-1$
			marketPlaceProvider.dispose();
		}

		if (foundUpdates)
		{
			synchronized (dataLock)
			{
				for (int i = availableUpdates.length - 1; i >= 0; i--)
				{
					if (availableUpdates[i] != null)
					{
						// find this installed extension, because installed extensions might have changed while we were checking...
						// (as this was run as a job, meanwhile the user might have already uninstalled/updated some installed extensions)
						for (Pair<DependencyMetadata, DependencyMetadata> pair : extensions)
						{
							if (pair.getLeft() == installedDmds[i])
							{
								pair.setRight(availableUpdates[i]);
							}
						}
					}
				}
			}
		}
		monitor.worked(1);
		monitor.done();
		return foundUpdates;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite topLevel = (Composite)super.createDialogArea(parent);
		GridLayout gl = ((GridLayout)topLevel.getLayout());
		if (gl.marginTop == 0) gl.marginTop = gl.marginHeight;
		gl.marginHeight = 0;
		gl.marginBottom = 0;

		SashForm split = new SashForm(topLevel, SWT.VERTICAL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 350;
		gd.widthHint = 450;
		split.setLayoutData(gd);
		split.setLayout(new FillLayout());

		// ----------- Table area
		table = new Table(split, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		table.setLinesVisible(false);
		table.setHeaderVisible(true);

		TableColumn col = new TableColumn(table, SWT.NONE); // icon
		col = new TableColumn(table, SWT.NONE);
		col.setText("Extension name"); //$NON-NLS-1$
		col = new TableColumn(table, SWT.NONE);
		col.setText("Version"); //$NON-NLS-1$
		col = new TableColumn(table, SWT.NONE); // upgrade button
		col = new TableColumn(table, SWT.NONE); // remove button

		grabExcessSpaceInColumnListener = new GrabExcessSpaceIn1ColumnTableListener(table, 1);
		readInstalledExtensions(); // else they are already there

		table.addControlListener(grabExcessSpaceInColumnListener); // Name column grabs excess space

		// ----------- Description area
		final ScrolledComposite scroller = new ScrolledComposite(split, SWT.V_SCROLL | SWT.BORDER);
		scroller.setExpandHorizontal(true);
		scroller.setExpandVertical(true);
		scroller.setAlwaysShowScrollBars(false);
//		scroller.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));

		final Text description = new Text(scroller, SWT.WRAP | SWT.READ_ONLY);
//		description.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		scroller.setContent(description);
		table.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
				synchronized (dataLock)
				{
					DependencyMetadata dmd = extensions[table.getSelectionIndex()].getLeft();
					EXPParser parser = parserPool.getOrCreateParser(installedProvider.getEXPFile(dmd.id, dmd.version, null));
					int ml = parser.getMessages().length;
					ExtensionConfiguration whole = parser.parseWholeXML();

					Message[] msgs = parser.getMessages();
					if (msgs.length > ml) ServoyLog.logWarning(
						"When preparing to show description in installed extensions dialog, problems were encountered: " + Arrays.asList(msgs).toString(), null); //$NON-NLS-1$

					String descriptionText = "Extension ID: " + dmd.id + System.getProperty("line.separator"); //$NON-NLS-1$//$NON-NLS-2$
					if (whole.getInfo() != null)
					{
						if (whole.getInfo().description != null) descriptionText += whole.getInfo().description;
					}

					description.setText(descriptionText);
					Rectangle r = scroller.getClientArea();
					scroller.setMinSize(description.computeSize(r.width, SWT.DEFAULT));
				}
			}
		});
		scroller.addControlListener(new ControlAdapter()
		{
			@Override
			public void controlResized(ControlEvent e)
			{
				Rectangle r = scroller.getClientArea();
				scroller.setMinSize(description.computeSize(r.width, SWT.DEFAULT));
			}
		});

		split.setWeights(new int[] { 85, 15 });
		return topLevel;
	}

	protected synchronized void readInstalledExtensions()
	{
		synchronized (dataLock)
		{
			if (installedProvider == null)
			{
				File extDir = new File(new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory()).getParentFile(),
					ExtensionUtils.EXPFILES_FOLDER);
				installedProvider = new InstalledWithPendingExtensionProvider(extDir, parserPool);
				populateInstalledExtensions();
			}
		}
		updateTableUI();
	}

	protected void refreshInstalledExtensions()
	{
		synchronized (dataLock)
		{
			parserPool.flushCache();
			installedProvider.flushCache();
			populateInstalledExtensions();
		}

		updateTableUI();
	}

	protected void populateInstalledExtensions()
	{
		synchronized (dataLock)
		{
			int ml = installedProvider.getMessages().length;
			DependencyMetadata[] allInstalled = installedProvider.getAllAvailableExtensions();

			Message[] msgs = installedProvider.getMessages();
			if (msgs.length > ml) ServoyLog.logWarning(
				"When gettint all items for installed extensions dialog, problems were encountered: " + Arrays.asList(msgs).toString(), null); //$NON-NLS-1$

			extensions = new Pair[allInstalled.length];

			for (int i = allInstalled.length - 1; i >= 0; i--)
			{
				extensions[i] = new Pair<DependencyMetadata, DependencyMetadata>(allInstalled[i], null);
			}
		}
	}

	protected synchronized void updateTableUI()
	{
		if (table == null) return;
		UIUtils.runInUI(new Runnable()
		{
			public void run()
			{
				if (getShell().isDisposed()) return;

				Image upgradeIcon = Activator.getDefault().loadImageFromBundle("upgrade.gif"); //$NON-NLS-1$
				Image uninstallIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE);

				table.setVisible(false);
				try
				{
					table.clearAll();
					table.removeAll();

					// the table editors/buttons will not get removed by above calls (only table items); we have to remove them manually
					for (Control c : table.getChildren())
					{
						if (c.getData() instanceof DependencyMetadata) c.dispose(); // for buttons 
					}
					cleanAllocatedImages();

					SelectionListener upgradeListener = new SelectionListener()
					{
						public void widgetSelected(SelectionEvent e)
						{
							widgetDefaultSelected(e);
						}

						public void widgetDefaultSelected(SelectionEvent e)
						{
							DependencyMetadata dmd = (DependencyMetadata)e.widget.getData();
							// start upgrade
							InstallExtensionWizard installExtensionWizard = new InstallExtensionWizard(dmd.id, dmd.version);
							installExtensionWizard.init(PlatformUI.getWorkbench(), null);
							WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), installExtensionWizard);
							dialog.open();
						}
					};

					SelectionListener uninstallListener = new SelectionListener()
					{
						public void widgetSelected(SelectionEvent e)
						{
							widgetDefaultSelected(e);
						}

						public void widgetDefaultSelected(SelectionEvent e)
						{
							DependencyMetadata dmd = (DependencyMetadata)e.widget.getData();
							// start uninstall process
							// TODO
							MessageDialog.openInformation(getShell(), "Feature unavailable", "Uninstall will be available in upcoming Servoy versions."); //$NON-NLS-1$ //$NON-NLS-2$
						}
					};

					synchronized (dataLock)
					{
						for (Pair<DependencyMetadata, DependencyMetadata> extension : extensions)
						{
							TableItem item = new TableItem(table, SWT.NONE);
							Image img = getInstalledExtensionIcon(extension.getLeft());
							if (img != null) item.setImage(0, img);
							item.setText(1, extension.getLeft().extensionName);
							item.setText(2, extension.getLeft().version);
							if (extension.getRight() != null)
							{
								createButton(upgradeIcon, item, 3, extension.getRight()).addSelectionListener(upgradeListener);
//							item.setImage(3, upgradeIcon);
							}
							createButton(uninstallIcon, item, 4, extension.getLeft()).addSelectionListener(uninstallListener);
//						item.setImage(4, uninstallIcon);
						}
					}

					TableColumn[] columns = table.getColumns();
					for (TableColumn column : columns)
					{
						column.pack();
					}
				}
				finally
				{
					table.setVisible(true);
				}

				if (grabExcessSpaceInColumnListener != null) grabExcessSpaceInColumnListener.grabExcessSpaceInColumn();
			}

			protected Button createButton(Image img, TableItem item, int col, DependencyMetadata dmd)
			{
				TableEditor editor = new TableEditor(table);
				Button button = new Button(table, SWT.PUSH | SWT.FLAT);
				button.setImage(img);
				button.setData(dmd);
				button.pack();
				editor.minimumWidth = button.getSize().x;
				editor.setEditor(button, item, col);
				return button;
			}

		}, false);
	}

	protected Image getInstalledExtensionIcon(DependencyMetadata dmd)
	{
		Image image = null;

		File f = installedProvider.getEXPFile(dmd.id, dmd.version, null);
		EXPParser parser = parserPool.getOrCreateParser(f);
		int nm = parser.getMessages().length;
		ExtensionConfiguration xml = parser.parseWholeXML();
		Message[] msgs = parser.getMessages();
		if (msgs.length > nm) ServoyLog.logInfo("While getting icon in installed dialog, problems were found: " + Arrays.asList(msgs).toString()); //$NON-NLS-1$

		if (xml != null && xml.getInfo() != null)
		{
			if (xml.getInfo().iconPath != null)
			{
				try
				{
					image = ExtensionUtils.runOnEntry(f, xml.getInfo().iconPath, new EntryInputStreamRunner<Image>()
					{
						public Image runOnEntryInputStream(InputStream is) throws IOException
						{
							Image img = new Image(getShell().getDisplay(), is);
							Image resized = resizeImage(img, 16, 16); // make it 16x16 if it's not already and disposes of old image
							return resized;
						}
					}).getRight();

					if (image != null)
					{
						allocatedImages.add(image);
					}
				}
				catch (IOException e)
				{
					// we can't get the image for some reason
					ServoyLog.logWarning("Cannot get extension icon for installed extension", e); //$NON-NLS-1$
				}
			}
		}
		return image;
	}

	protected Image resizeImage(Image i, int width, int height)
	{
		Image newImg;
		if (i.getBounds().width != width || i.getBounds().height != height)
		{
			newImg = new Image(getShell().getDisplay(), width, height);
			GC gc = new GC(newImg);
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(i, 0, 0, i.getBounds().width, i.getBounds().height, 0, 0, width, height);
			gc.dispose();
			i.dispose();
		}
		else
		{
			newImg = i;
		}
		return newImg;
	}

	protected void cleanAllocatedImages()
	{
		for (Image i : allocatedImages)
		{
			try
			{
				i.dispose();
			}
			catch (Throwable e)
			{
				ServoyLog.logWarning("Cannot deallocated img.; installed ext. dialog", e);
				// nothing to do here
			}
		}
		allocatedImages.clear();
	}

	@Override
	public boolean isHelpAvailable()
	{
		return false;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createButton(parent, UPDATE_CHECK_BUTTON_ID, "Check for updates", true); //$NON-NLS-1$
	}

	@Override
	protected void buttonPressed(int buttonId)
	{
		super.buttonPressed(buttonId);

		if (buttonId == UPDATE_CHECK_BUTTON_ID)
		{
			getButton(UPDATE_CHECK_BUTTON_ID).setEnabled(false);
			Job job = new Job("Checking for Servoy Extension upgrades") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					final boolean updatesFound[] = new boolean[] { false };
					try
					{
						updatesFound[0] = checkForUpdatesInternal(monitor);
					}
					finally
					{
						if (!getShell().isDisposed())
						{
							getShell().getDisplay().asyncExec(new Runnable()
							{
								public void run()
								{
									if (!getButton(UPDATE_CHECK_BUTTON_ID).isDisposed())
									{
										getButton(UPDATE_CHECK_BUTTON_ID).setEnabled(true);
										if (updatesFound[0]) updateTableUI();
										else MessageDialog.openInformation(getShell(), "Check for upgrades", //$NON-NLS-1$
											"No new compatible versions found for installed extensions."); //$NON-NLS-1$
									}
								}
							});
						}
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(false);
			job.setUser(true);
			job.setRule(new SerialRule(SERIAL_RULE_ID));
			job.schedule();
		}
	}

	@Override
	public boolean close()
	{
		boolean b = super.close();
		if (b && installedProvider != null)
		{
			cleanAllocatedImages();
			installedProvider.dispose();
		}
		return b;
	}

}
