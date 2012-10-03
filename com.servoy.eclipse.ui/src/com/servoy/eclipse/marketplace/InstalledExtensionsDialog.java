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
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.extension.ExtensionUIUtils;
import com.servoy.eclipse.core.extension.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.GrabExcessSpaceIn1ColumnTableListener;
import com.servoy.eclipse.ui.wizards.extension.InstallExtensionWizard;
import com.servoy.eclipse.ui.wizards.extension.UninstallExtensionWizard;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.extension.MarketPlaceExtensionProvider;
import com.servoy.extension.Message;
import com.servoy.extension.VersionStringUtils;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.EXPParserPool;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.StringComparator;

/**
 * Dialog that shows currently installed extensions and allows uninstall and checking for updates.
 * @author acostescu
 */
public class InstalledExtensionsDialog extends TrayDialog
{

	protected static final String INSTALLED_EXTENSIONS_SECTION = "InstalledExtensionsDialog"; //$NON-NLS-1$
	protected static final int UPDATE_CHECK_BUTTON_ID = 1017;
	protected static final int CI_UPDATE = 3;
	protected static final int CI_UNINSTALL = 4;
	protected static final String SPLIT_AT = "splitAt"; //$NON-NLS-1$
	protected static final String WIDTH = "shellWidth"; //$NON-NLS-1$
	protected static final String HEIGHT = "shellHeight"; //$NON-NLS-1$

	public static final int SERIAL_RULE_ID = 1017;

	public static WeakReference<InstalledExtensionsDialog> createdInstance;

	protected IDialogSettings dialogSettings;
	protected boolean automaticUpdateCheck = false;

	protected Object dataLock = new Object();
	protected InstalledWithPendingExtensionProvider installedProvider;
	protected Pair<DependencyMetadata, DependencyMetadata>[] extensions;
	protected EXPParserPool parserPool = new EXPParserPool();
	protected List<Image> allocatedImages = new ArrayList<Image>();
	protected SashForm split;

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

		if (dialogSettings.get(SPLIT_AT) == null) dialogSettings.put(SPLIT_AT, 85);

		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public void create()
	{
		super.create();
		if (dialogSettings.get(WIDTH) != null && dialogSettings.get(HEIGHT) != null)
		{
			try
			{
				getShell().setSize(dialogSettings.getInt(WIDTH), dialogSettings.getInt(HEIGHT));
			}
			catch (NumberFormatException e)
			{
				// wrong values; ignore
			}
		}

		getShell().setText("Installed Servoy Extensions"); //$NON-NLS-1$
		Image i1 = Activator.getDefault().loadImageFromBundle("extension16.png"); //$NON-NLS-1$
		Image i2 = Activator.getDefault().loadImageFromBundle("extension32.png"); //$NON-NLS-1$
		Image i3 = Activator.getDefault().loadImageFromBundle("extension64.png"); //$NON-NLS-1$
		Image i4 = Activator.getDefault().loadImageFromBundle("extension128.png"); //$NON-NLS-1$

		getShell().setImages(new Image[] { i1, i2, i3, i4 });

		if (automaticUpdateCheck)
		{
			automaticUpdateCheck = false;
			updateTableUI();
		}
		else refreshInstalledExtensions();
	}

	/**
	 * Meant to be called by "automatic update check at startup", called from within a job with a SerialRule using SERIAL_RULE_ID. 
	 */
	public boolean checkForUpdates(IProgressMonitor monitor)
	{
		this.automaticUpdateCheck = true;
		readInstalledExtensions();
		return checkForUpdatesInternal(monitor).getLeft().booleanValue();
	}

	protected Pair<Boolean, Message[]> checkForUpdatesInternal(IProgressMonitor monitor)
	{
		boolean foundUpdates = false;
		Message[] warnings = null;
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
			if (msgs.length > 0)
			{
				ServoyLog.logInfo("While checking for upgrades, problems were found: " + Arrays.asList(msgs).toString()); //$NON-NLS-1$
				warnings = msgs;
			}
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
		return new Pair<Boolean, Message[]>(Boolean.valueOf(foundUpdates), warnings);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite topLevel = (Composite)super.createDialogArea(parent);
		GridLayout gl = ((GridLayout)topLevel.getLayout());
		if (gl.marginTop == 0) gl.marginTop = gl.marginHeight;
		gl.marginHeight = 0;
		gl.marginBottom = 0;

		split = new SashForm(topLevel, SWT.VERTICAL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 350;
		gd.widthHint = 450;
		split.setLayoutData(gd);
		split.setLayout(new FillLayout());

		// ----------- Table area
		table = new Table(split, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		table.setLinesVisible(false);
		table.setHeaderVisible(true);

		TableColumn col = new TableColumn(table, SWT.CENTER); // icon
		col.setResizable(false);
		col = new TableColumn(table, SWT.NONE);
		col.setText("Extension name"); //$NON-NLS-1$
		col.setResizable(false);
		col = new TableColumn(table, SWT.CENTER);
		col.setResizable(false);
		col.setText("Version"); //$NON-NLS-1$
		col = new TableColumn(table, SWT.CENTER, CI_UPDATE); // upgrade button
		col.setResizable(false);
		col = new TableColumn(table, SWT.CENTER, CI_UNINSTALL); // remove button
		col.setResizable(false);

		grabExcessSpaceInColumnListener = new GrabExcessSpaceIn1ColumnTableListener(table, 1, new int[] { -1, -1, -1, 20, 20 });
		readInstalledExtensions(); // else they are already there

		table.addControlListener(grabExcessSpaceInColumnListener); // Name column grabs excess space

		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = table.getItem(pt);
				if (item != null && item.getBounds(CI_UPDATE).contains(pt))
				{
					onUpdate((Pair<DependencyMetadata, DependencyMetadata>)item.getData());
				}
				else if (item != null && item.getBounds(CI_UNINSTALL).contains(pt))
				{
					onUninstall((Pair<DependencyMetadata, DependencyMetadata>)item.getData());
				}
			}
		});

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
						if (whole.getInfo().description != null) descriptionText += whole.getInfo().description.replace("\r\n", "\n").replace("\n", System.getProperty("line.separator")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
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

		int saved = dialogSettings.getInt(SPLIT_AT);
		if (saved < 1 || saved > 99) saved = 85;
		split.setWeights(new int[] { saved, 100 - saved });
		return topLevel;
	}

	protected void onUninstall(Pair<DependencyMetadata, DependencyMetadata> data)
	{
		final DependencyMetadata dmd = data.getLeft();

		UIUtils.runInUI(new Runnable()
		{
			public void run()
			{
				UninstallExtensionWizard uninstallExtensionWizard = new UninstallExtensionWizard(dmd.id);
				uninstallExtensionWizard.init(PlatformUI.getWorkbench(), null);
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), uninstallExtensionWizard);
				dialog.open();
			}
		}, false);

		refreshInstalledExtensions();
	}

	protected void onUpdate(Pair<DependencyMetadata, DependencyMetadata> data)
	{
		DependencyMetadata dmd = data.getRight();
		// start upgrade
		if (dmd != null)
		{
			InstallExtensionWizard installExtensionWizard = new InstallExtensionWizard(dmd.id, dmd.version);
			installExtensionWizard.init(PlatformUI.getWorkbench(), null);
			WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), installExtensionWizard);
			dialog.open();
			refreshInstalledExtensions();
		}
	}

	protected synchronized void readInstalledExtensions()
	{
		synchronized (dataLock)
		{
			if (installedProvider == null)
			{
				File extDir = new File(new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory()).getParentFile(),
					ExtensionUtils.EXPFILES_FOLDER);
				if (!extDir.exists()) extDir.mkdirs();
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
			if (installedProvider != null)
			{
				parserPool.flushCache();
				installedProvider.flushCache();
				populateInstalledExtensions();
			}
		}

		updateTableUI();
	}

	protected void populateInstalledExtensions()
	{
		synchronized (dataLock)
		{
			int ml = installedProvider.getMessages().length;
			DependencyMetadata[] allInstalled = installedProvider.getAllAvailableExtensions();

			Arrays.sort(allInstalled, new Comparator<DependencyMetadata>()
			{
				public int compare(DependencyMetadata o1, DependencyMetadata o2)
				{
					return StringComparator.INSTANCE.compare(o1.extensionName, o2.extensionName);
				}
			});

			Message[] msgs = installedProvider.getMessages();
			if (msgs.length > ml) ServoyLog.logWarning(
				"When getting all items for installed extensions dialog, problems were encountered: " + Arrays.asList(msgs).toString(), null); //$NON-NLS-1$

			Pair<DependencyMetadata, DependencyMetadata>[] oldExtensions = extensions;
			extensions = new Pair[allInstalled.length];

			for (int i = allInstalled.length - 1; i >= 0; i--)
			{
				extensions[i] = new Pair<DependencyMetadata, DependencyMetadata>(allInstalled[i], null);
				if (oldExtensions != null) seeIfOldHasUpdate(extensions[i], oldExtensions);
			}
		}
	}

	protected void seeIfOldHasUpdate(Pair<DependencyMetadata, DependencyMetadata> item, Pair<DependencyMetadata, DependencyMetadata>[] oldExtensions)
	{
		for (Pair<DependencyMetadata, DependencyMetadata> oldPair : oldExtensions)
		{
			if (item.getLeft().id.equals(oldPair.getLeft().id) && oldPair.getRight() != null)
			{
				if (VersionStringUtils.compareVersions(item.getLeft().version, oldPair.getRight().version) < 0)
				{
					item.setRight(oldPair.getRight());
				}
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
				if (table == null || getShell().isDisposed()) return;

				Image upgradeIcon = Activator.getDefault().loadImageFromBundle("upgrade.gif"); //$NON-NLS-1$
				Image uninstallIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);

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

					synchronized (dataLock)
					{
						for (Pair<DependencyMetadata, DependencyMetadata> extension : extensions)
						{
							TableItem item = new TableItem(table, SWT.NONE);
							item.setData(extension);
							Image img = getInstalledExtensionIcon(extension.getLeft());
							if (img != null) item.setImage(0, img);
							item.setText(1, extension.getLeft().extensionName);
							item.setText(2, extension.getLeft().version);
							if (extension.getLeft().getServoyDependency() != null && !extension.getLeft().getServoyDependency().isCompatibleVersion())
							{
								item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
							}
							if (extension.getRight() != null)
							{
//								createButton(upgradeIcon, item, 3, extension.getRight()).addSelectionListener(upgradeListener);
								item.setImage(3, upgradeIcon);
							}
//							Button b = createButton(uninstallIcon, item, 4, extension.getLeft());
//							b.addSelectionListener(uninstallListener);
//							b.setEnabled(false);

							item.setImage(4, uninstallIcon);
						}
					}

					TableColumn[] columns = table.getColumns();
					for (TableColumn column : columns)
					{
						column.pack();
					}

					if (grabExcessSpaceInColumnListener != null) grabExcessSpaceInColumnListener.grabExcessSpaceInColumn();
				}
				finally
				{
					table.setVisible(true);
				}
			}

			// reverted to plain images instead of buttons, cause editors, buttons did not work well with column.pack on Linux
//			protected Button createButton(Image img, TableItem item, int col, DependencyMetadata dmd)
//			{
//				TableEditor editor = new TableEditor(table);
//				Button button = new Button(table, SWT.PUSH | SWT.FLAT);
//				button.setImage(img);
//				button.setData(dmd);
//				button.pack();
//				editor.minimumWidth = button.getSize().x;
//				editor.setEditor(button, item, col);
//				return button;
//			}

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

	public void simulateUpdateCheckButtonClick()
	{
		if (getButton(UPDATE_CHECK_BUTTON_ID).isEnabled()) buttonPressed(UPDATE_CHECK_BUTTON_ID);
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
					Pair<Boolean, Message[]> updFnd = null;
					try
					{
						updFnd = checkForUpdatesInternal(monitor);
					}
					finally
					{
						final Pair<Boolean, Message[]> updatesFound = updFnd;
						if (!getShell().isDisposed())
						{
							getShell().getDisplay().asyncExec(new Runnable()
							{
								public void run()
								{
									if (!getButton(UPDATE_CHECK_BUTTON_ID).isDisposed())
									{
										getButton(UPDATE_CHECK_BUTTON_ID).setEnabled(true);
										if (updatesFound == null)
										{
											MessageDialog.openError(getShell(), "Update check", //$NON-NLS-1$
												"Errors encountered while checking for updates.\nCheck logs for more information."); //$NON-NLS-1$
										}
										else if (updatesFound.getLeft().booleanValue()) updateTableUI();
										else if (updatesFound.getRight() == null) MessageDialog.openInformation(getShell(), "Update check", //$NON-NLS-1$
											"No new (and compatible) versions were found for installed extensions in the marketplace."); //$NON-NLS-1$
										else
										{
											MultiStatus details = ExtensionUIUtils.translateMessagesToStatus(
												"Could not find any updates for installed extensions.", updatesFound.getRight()); //$NON-NLS-1$
											ErrorDialog.openError(getShell(), "Update check", null, details); //$NON-NLS-1$
										}
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
	protected void handleShellCloseEvent()
	{
		dialogSettings.put(SPLIT_AT, (int)(split.getWeights()[0] * 100d / (split.getWeights()[0] + split.getWeights()[1])));
		dialogSettings.put(WIDTH, getShell().getSize().x);
		dialogSettings.put(HEIGHT, getShell().getSize().y);

		if (installedProvider != null)
		{
			cleanAllocatedImages();
			installedProvider.dispose();
			installedProvider = null;
		}
		super.handleShellCloseEvent();
	}

}
