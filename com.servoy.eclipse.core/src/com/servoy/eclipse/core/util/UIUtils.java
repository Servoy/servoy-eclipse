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
package com.servoy.eclipse.core.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourceProjectChoiceDialog;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectSetupJob;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ScrollableDialog;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;

/**
 * Utility class that offers all kinds of utilities (UI related functionality).
 *
 * @author acostescu
 */
public class UIUtils
{
	public static final String ECLIPSE_DARK_THEME_ID = "org.eclipse.e4.ui.css.theme.e4_dark";
	public static final String SERVOY_DARK_THEME_ID = "com.servoy.eclipse.core.servoydarktheme";

	private static ImageDescriptor errorImageDescriptor;

	public static abstract class ExtendedInputDialog<T> extends InputDialog
	{
		public ExtendedInputDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialTextValue, IInputValidator validator)
		{
			super(parentShell, dialogTitle, dialogMessage, initialTextValue, validator);
		}

		public abstract T getExtendedValue();
	}
	public static class InputAndListDialog extends ExtendedInputDialog<String>
	{
		private final String listDescriptionText;
		private final String[] listOptions;
		private String listValue;
		protected ListViewer listViewer;

		public InputAndListDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialTextValue, IInputValidator validator,
			String[] listOptions, String listValue, String listDescriptionText)
		{
			super(parentShell, dialogTitle, dialogMessage, initialTextValue, validator);
			this.listOptions = listOptions;
			this.listValue = listValue;
			this.listDescriptionText = listDescriptionText;
			setBlockOnOpen(true);
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			if (listOptions == null)
			{
				// no list, just default behaviour
				return super.createDialogArea(parent);
			}

			Composite area = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 10;
			area.setLayout(layout);

			Label comboDescription = new Label(area, SWT.NONE);
			comboDescription.setText(listDescriptionText);

			listViewer = new ListViewer(area, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
			listViewer.setContentProvider(ArrayContentProvider.getInstance());
			listViewer.setLabelProvider(new LabelProvider());
			listViewer.setInput(listOptions);
			listViewer.setSelection(new StructuredSelection(listValue));
			listViewer.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					IStructuredSelection selection = (IStructuredSelection)listViewer.getSelection();
					if (selection.isEmpty())
					{
						listValue = null;
					}
					listValue = (String)selection.getFirstElement();
					validateInput();
				}
			});

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = 200;
			listViewer.getControl().setLayoutData(gd);

			addExtraComponents(area);

			layout = (GridLayout)((Composite)super.createDialogArea(area)).getLayout();
			layout.marginHeight = layout.marginWidth = 0;

			return area;
		}

		@Override
		public String getExtendedValue()
		{
			return listValue;
		}

		protected void addExtraComponents(Composite parent)
		{

		}

	}
	/**
	 * Input dialog with text field + combo box.
	 */
	public static class InputAndComboDialog extends ExtendedInputDialog<String>
	{

		private final String comboDescriptionText;
		private final String[] comboOptions;
		private int comboSelection;

		public InputAndComboDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialTextValue, IInputValidator validator,
			String[] comboOptions, String comboDescriptionText, int initialComboSelection)
		{
			super(parentShell, dialogTitle, dialogMessage, initialTextValue, validator);
			setBlockOnOpen(true);
			this.comboOptions = comboOptions;
			this.comboDescriptionText = comboDescriptionText;
			comboSelection = initialComboSelection;
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite area = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 10;
			area.setLayout(layout);

			Label comboDescription = new Label(area, SWT.NONE);
			comboDescription.setText(comboDescriptionText);

			final Combo combo = new Combo(area, SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(combo);

			combo.setItems(comboOptions);
			combo.select(comboSelection);
			GridData gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			combo.setLayoutData(gd);

			combo.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e)
				{
					comboSelection = combo.getSelectionIndex();
					validateInput();
				}
			});
			layout = (GridLayout)((Composite)super.createDialogArea(area)).getLayout();
			layout.marginHeight = layout.marginWidth = 0;

			return area;
		}

		@Override
		public String getExtendedValue()
		{
			if (comboSelection != -1)
			{
				return comboOptions[comboSelection];
			}
			return null;
		}

	}

	public static class InputAndCheckDialog extends ExtendedInputDialog<Boolean>
	{
		private final String checkButtonDescription;
		private boolean checked;

		public InputAndCheckDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialValue, IInputValidator validator,
			String checkButtonDescription, boolean initialCheck)
		{
			super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
			this.checkButtonDescription = checkButtonDescription;
			this.checked = initialCheck;
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite area = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 10;
			area.setLayout(layout);

			Control theDialogArea = super.createDialogArea(area);

			final Button checkButton = new Button(area, SWT.CHECK);
			GridData gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.verticalIndent = 5;
			checkButton.setLayoutData(gd);
			checkButton.setText(checkButtonDescription);
			checkButton.setSelection(checked);

			checkButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					checked = checkButton.getSelection();
				}
			});

			layout = (GridLayout)((Composite)theDialogArea).getLayout();
			layout.marginHeight = layout.marginWidth = 0;

			return area;
		}

		@Override
		public Boolean getExtendedValue()
		{
			return Boolean.valueOf(checked);
		}

	}

	/**
	 * Message dialog with check box.
	 */
	public static class MessageAndCheckBoxDialog extends MessageDialog
	{
		private final String checkBoxLabel;
		private boolean checked;

		/**
		 * Create a message dialog. Note that the dialog will have no visual representation (no widgets) until it is told to open.
		 * <p>
		 * The labels of the buttons to appear in the button bar are supplied in this constructor as an array. The <code>open</code> method will return the
		 * index of the label in this array corresponding to the button that was pressed to close the dialog. If the dialog was dismissed without pressing a
		 * button (ESC, etc.) then -1 is returned. Note that the <code>open</code> method blocks.
		 * </p>
		 *
		 * @param parentShell the parent shell
		 * @param dialogTitle the dialog title, or <code>null</code> if none
		 * @param dialogTitleImage the dialog title image, or <code>null</code> if none
		 * @param dialogMessage the dialog message
		 * @param checkBoxLabel the check box text.
		 * @param initialCheckBoxState initial selection state for the check box.
		 * @param dialogImageType one of the following values:
		 *            <ul>
		 *            <li><code>MessageDialog.NONE</code> for a dialog with no image</li>
		 *            <li><code>MessageDialog.ERROR</code> for a dialog with an error image</li>
		 *            <li><code>MessageDialog.INFORMATION</code> for a dialog with an information image</li>
		 *            <li><code>MessageDialog.QUESTION </code> for a dialog with a question image</li>
		 *            <li><code>MessageDialog.WARNING</code> for a dialog with a warning image</li>
		 *            </ul>
		 * @param dialogButtonLabels an array of labels for the buttons in the button bar
		 * @param defaultIndex the index in the button label array of the default button
		 */
		public MessageAndCheckBoxDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, String checkBoxLabel,
			boolean initialCheckBoxState, int dialogImageType, String[] dialogButtonLabels, int defaultIndex)
		{
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
			setBlockOnOpen(true);
			this.checkBoxLabel = checkBoxLabel;
			this.checked = initialCheckBoxState;
		}

		@Override
		protected void setShellStyle(int newShellStyle)
		{
			super.setShellStyle((newShellStyle & ~SWT.APPLICATION_MODAL) | SWT.PRIMARY_MODAL);
		}

		@Override
		protected Control createCustomArea(Composite parent)
		{
			Label l = new Label(parent, SWT.NONE);
			l.setBackground(new Color(null, 200, 200, 200));
			GridData gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalSpan = 2;
			gd.verticalIndent = 10;
			gd.heightHint = 1;
			l.setLayoutData(gd);

			final Button checkBox = new Button(parent, SWT.CHECK);
			checkBox.setText(checkBoxLabel);
			checkBox.setSelection(checked);
			gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalSpan = 2;
			gd.horizontalAlignment = SWT.RIGHT;
			checkBox.setLayoutData(gd);

//			l = new Label(parent, SWT.NONE);
//			l.setBackground(new Color(null, 200, 200, 200));
//			gd = new GridData();
//			gd.horizontalAlignment = GridData.FILL;
//			gd.grabExcessHorizontalSpace = true;
//			gd.horizontalSpan = 2;
//			gd.heightHint = 1;
//			l.setLayoutData(gd);

			checkBox.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e)
				{
					checked = checkBox.getSelection();
				}
			});

			return parent;
		}

		public boolean isChecked()
		{
			return checked;
		}

	}

	/**
	 * Class that is able to manage a question dialog with "Yes/Yes to all/No/No to all" options and it's state.
	 */
	public static class YesYesToAllNoNoToAllAsker
	{

		private static final int ASK = 0;
		private static final int ALL = 1;
		private static final int NONE = 2;
		private static final String[] buttons = new String[] { "Yes", "Yes to all", "No", "No to all" };

		private int state = ASK;
		private final Shell shell;
		private final String title;
		private String message;

		/**
		 * Creates a new instance that will use the given shell/title to open the dialog.
		 *
		 * @param shell the shell used to show the dialog.
		 * @param title the title of the dialog.
		 */
		public YesYesToAllNoNoToAllAsker(Shell shell, String title)
		{
			this.shell = shell;
			this.title = title;
		}

		/**
		 * Sets the dialog's message.
		 *
		 * @param message the dialog's message.
		 */
		public void setMessage(String message)
		{
			this.message = message;
		}

		/**
		 * Returns true if the user chose "Yes" or state is "Yes to all" and false otherwise.
		 *
		 * @return true if the user chose "Yes" or state is "Yes to all" and false otherwise.
		 */
		public boolean userSaidYes()
		{
			boolean yes = false;
			if (state == ASK)
			{
				ReturnValueRunnable runnable = new ReturnValueRunnable()
				{
					public void run()
					{
						final MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttons, 2);
						dialog.setBlockOnOpen(true);
						returnValue = new Integer(dialog.open());
					}
				};
				if (Display.getCurrent() != null)
				{
					runnable.run();
				}
				else
				{
					Display.getDefault().syncExec(runnable);
				}
				int choice = (Integer)runnable.getReturnValue();
				switch (choice)
				{
					case 0 :
						yes = true;
						break;
					case 1 :
						yes = true;
						state = ALL;
						break;
					case 3 :
						state = NONE;
						break;
				}
			}
			else if (state == ALL)
			{
				yes = true;
			}
			return yes;
		}
	}

	public static class CheckGroupDialog extends Dialog
	{

		private final String title;
		private final String[] options;
		private final String[] values;
		private Object[] selected;
		private CheckboxTableViewer checkboxTableViewer;
		private final String message;

		public CheckGroupDialog(Shell parentShell, String title, String message, String[] options, String[] values)
		{
			super(parentShell);
			setBlockOnOpen(true);
			this.title = title;
			this.options = options;
			this.values = values;
			this.message = message;
		}

		@Override
		protected void configureShell(Shell sh)
		{
			super.configureShell(sh);
			sh.setText(title);
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			GridLayout gridLayout = new GridLayout(1, false);
			gridLayout.marginHeight = gridLayout.marginWidth = 20;
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			Label label = new Label(composite, SWT.NONE);
			label.setText(message);

			Table table = new Table(composite, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			checkboxTableViewer = new CheckboxTableViewer(table);
			checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
			checkboxTableViewer.setInput(options);
			checkboxTableViewer.setCheckedElements(values);
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 4));
			return super.createDialogArea(parent);
		}

		@Override
		protected void okPressed()
		{
			selected = checkboxTableViewer.getCheckedElements();
			super.okPressed();
		}

		public Object[] getSelected()
		{
			return selected;
		}
	}


	/**
	 * grows automatically until screen height - taskbar , if the shell size is not set
	 * @param shell
	 * @param messageIcon the icon to display in the info description,  ex IMessageProvider.ERORR
	 * @param title
	 * @param description
	 * @param text
	 */
	public static void showScrollableDialog(final Shell shell, final int messageIcon, final String title, final String description, final String text)
	{
		runInUI(new Runnable()
		{
			public void run()
			{
				ScrollableDialog dialog = new ScrollableDialog(shell, messageIcon, title, description, text);
				//shell.setSize(400, 500);
				dialog.open();
			}
		}, true);
	}

	/**
	 * Creates a SWT image from the given swing icon + the SWT device. Take care to dispose the resources used by the returned Image object when it's no longer used.
	 *
	 * @param swingIcon the swing icon to convert.
	 * @param device the SWT device used in the Image constructor.
	 * @return the SWT image object created based on the swing icon. Remember to dispose it when it will no longer be used.
	 */
	public static Image getSWTImageFromSwingIcon(Icon swingIcon, Device device, int width, int height)
	{
		try
		{
			BufferedImage bufferedImage = new BufferedImage(width > 0 ? width : swingIcon.getIconWidth(), height > 0 ? height : swingIcon.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
			Graphics graphics = bufferedImage.getGraphics();
			if (width > 0 && width != swingIcon.getIconWidth() && height > 0 && height != swingIcon.getIconHeight() && graphics instanceof Graphics2D)
			{
				((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				((Graphics2D)graphics).scale(((double)width) / (double)swingIcon.getIconWidth(), ((double)height) / (double)swingIcon.getIconHeight());
			}
			swingIcon.paintIcon(null, graphics, 0, 0);

			PipedOutputStream outBytes = new PipedOutputStream();
			PipedInputStream inBytes = new PipedInputStream(outBytes);
			if (Debug.tracing()) Debug.trace("Trying to get a png in thread: " + Thread.currentThread().getName());
			ImageIO.write(bufferedImage, "PNG", outBytes);
			outBytes.flush();
			outBytes.close();
			if (Debug.tracing()) Debug.trace("Got a png in thread: " + Thread.currentThread().getName());
			return new Image(device, inBytes);
		}
		catch (Throwable e)
		{
			ServoyLog.logWarning("Cannot convert swing icon into SWT image in thread " + Thread.currentThread().getName(), e);
			return null;
		}
	}

	public static ImageDescriptor createImageDescriptorFromAwtImage(java.awt.Image image, final boolean transparent)
	{
		if (image == null) return null;

		final RenderedImage renderedImage;
		if (image instanceof RenderedImage)
		{
			renderedImage = (RenderedImage)image;
		}
		else
		{
			renderedImage = toBufferedImage(image);
		}

		return new ImageDescriptor()
		{
			@Override
			public ImageData getImageData()
			{
				try
				{
					int width = renderedImage.getWidth();
					int height = renderedImage.getHeight();

					int depth = 24;
					PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
					ImageData swtdata = new ImageData(width, height, depth, palette);
					Raster raster = renderedImage.getData();
					int numbands = raster.getNumBands();
					int[] awtdata = raster.getPixels(0, 0, width, height, new int[width * height * numbands]);
					int step = swtdata.depth / 8;

					byte[] data = swtdata.data;
					swtdata.transparentPixel = -1;
					int baseindex = 0;
					for (int y = 0; y < height; y++)
					{
						int idx = (0 + y) * swtdata.bytesPerLine + 0 * step;

						for (int x = 0; x < width; x++)
						{
							int pixel = x + y * width;
							baseindex = pixel * numbands;

							data[idx++] = (byte)awtdata[baseindex + 2];
							data[idx++] = (byte)awtdata[baseindex + 1];
							data[idx++] = (byte)awtdata[baseindex];
							if (numbands == 4 && transparent)
							{
								swtdata.setAlpha(x, y, awtdata[baseindex + 3]);
							}
						}
					}
					return swtdata;
				}
				catch (RuntimeException e)
				{
					ServoyLog.logError(e);
					return getErrorImageData();
				}
			}
		};
	}

	public static ImageData getErrorImageData()
	{
		if (errorImageDescriptor == null)
		{
			errorImageDescriptor = ImageDescriptor.createFromFile(IApplication.class, "images/error.gif");
		}
		return errorImageDescriptor.getImageData();
	}

	private static BufferedImage toBufferedImage(java.awt.Image image)
	{
		if (image instanceof BufferedImage)
		{
			return (BufferedImage)image;
		}

		// Ensure that all the pixels in the image are loaded
		Component mtComp = new Component()
		{
		};
		MediaTracker mt = new MediaTracker(mtComp);
		mt.addImage(image, 0);
		do
		{
			try
			{
				mt.waitForAll();
			}
			catch (InterruptedException ex)
			{
				// ignore
			}
		}
		while (!mt.checkAll());

		boolean hasAlpha = ImageLoader.imageHasAlpha(image, 30000); //workaround for case SVY-3024, import image loading hang up.
		BufferedImage bimage = null;
		try
		{
			bimage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(
				image.getWidth(null), image.getHeight(null), hasAlpha ? Transparency.BITMASK : Transparency.OPAQUE);
		}
		catch (HeadlessException e)
		{
		}
		if (bimage == null)
		{
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		}

		Graphics g = bimage.createGraphics(); // Paint the image onto the buffered
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimage;
	}

	/**
	 * Shows an option dialog showing the options in a combo.
	 *
	 * @param shell the shell.
	 * @param title the title.
	 * @param message the message.
	 * @param options the list of options.
	 * @return the index selected by the user of -1 if the user did not select an index.
	 */
	public static int showOptionDialog(Shell shell, String title, String message, final String[] options, int defaultOption)
	{
		OptionDialog dialog = new OptionDialog(shell, title, null, message, MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0, options, defaultOption);
		dialog.setBlockOnOpen(true);
		if (dialog.open() != Window.OK)
		{
			return -1;
		}
		return dialog.getSelectedOption();
	}

	public static String showEditableOptionDialog(Shell shell, String title, String message, final String[] options, int defaultOption)
	{
		OptionDialog dialog = new OptionDialog(shell, title, null, message, MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0, options, defaultOption,
			SWT.DROP_DOWN);
		dialog.setBlockOnOpen(true);
		if (dialog.open() != Window.OK)
		{
			return null;
		}
		return dialog.getSelectedText();
	}

	public static String showTextFieldDialog(Shell shell, String title, String message)
	{
		return showTextFieldDialog(shell, title, message, "");
	}

	public static String showTextFieldDialog(Shell shell, String title, String message, String initialValue)
	{
		return showTextFieldDialog(shell, title, message, initialValue, false);
	}

	public static String showTextFieldDialog(Shell shell, String title, String message, String initialValue, boolean isTextArea)
	{
		TextFieldDialog dialog = new TextFieldDialog(shell, title, null, message, MessageDialog.NONE, new String[] { "OK", "Cancel" }, initialValue,
			isTextArea);
		dialog.setBlockOnOpen(true);
		if (dialog.open() != Window.OK)
		{
			return null;
		}
		return dialog.getSelectedText();
	}


	public static boolean askConfirmation(final Shell shell, final String title, final String message)
	{
		final boolean[] ok = new boolean[1];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				ok[0] = MessageDialog.openConfirm(shell, title, message);
			}
		});
		return ok[0];
	}

	public static boolean askQuestion(final Shell shell, final String title, final String message)
	{
		final boolean[] ok = new boolean[1];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				ok[0] = MessageDialog.openQuestion(shell, title, message);
			}
		});
		return ok[0];
	}

	public static void showInformation(final Shell shell, final String title, final String message)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openInformation(shell, title, message);
			}
		});
	}

	public static String showPasswordDialog(final Shell shell, final String dialogTitle, final String dialogMessage, final String initialValue,
		final IInputValidator validator)
	{
		final String[] returnValue = new String[1];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				InputDialog dialog = new InputDialog(shell, dialogTitle, dialogMessage, initialValue, validator)
				{
					@Override
					protected Control createDialogArea(Composite parent)
					{
						Control c = super.createDialogArea(parent);
						getText().setEchoChar('*');

						return c;
					}
				};
				dialog.setBlockOnOpen(true);
				if (dialog.open() != Window.OK)
				{
					returnValue[0] = null;
					return;
				}
				returnValue[0] = dialog.getValue();
			}
		});
		return returnValue[0];
	}

	/**
	 * Runs runnable in SWT ui thread.
	 *
	 * @param r the runnable to run.
	 * @param forceSync if this is true and if not already on ui thread, run using sync exec. If this is false and not already running on the ui thread, it will
	 *            do asyncExec().
	 */
	public static void runInUI(Runnable r, boolean forceSync)
	{
		if (Display.getCurrent() != null)
		{
			r.run();
		}
		else
		{
			if (forceSync)
			{
				Display.getDefault().syncExec(r);
			}
			else
			{
				Display.getDefault().asyncExec(r);
			}
		}
	}

	public static <T> T runInUI(final Callable<T> c) throws Exception
	{
		if (Display.getCurrent() != null)
		{
			return c.call();
		}

		final Object[] ret = { null };
		final Exception[] ex = { null };
		Display.getDefault().syncExec(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ret[0] = c.call();
				}
				catch (Exception e)
				{
					ex[0] = e;
				}

			}
		});

		if (ex[0] != null) throw ex[0];
		return (T)ret[0];
	}

	/**
	 * Shows a warning dialog, but switches to UI thread if needed.
	 *
	 * @param title the title of the dialog.
	 * @param message the message in the dialog.
	 */
	public static void reportWarning(final String title, final String message)
	{
		runInUI(new Runnable()
		{
			public void run()
			{
				MessageDialog.openWarning(getActiveShell(), title, message);
			}
		}, false);
	}

	/**
	 * Shows a error dialog, but switches to UI thread if needed.
	 *
	 * @param title the title of the dialog.
	 * @param message the message in the dialog.
	 */
	public static void reportError(final String title, final String message)
	{
		runInUI(new Runnable()
		{
			public void run()
			{
				MessageDialog.openError(getActiveShell(), title, message);
			}
		}, false);
	}

	/**
	 * Tries to get the active shell. If this is a thread with an associated Display, then that display is used to get the active shell. If not,
	 * UIUtils.getActiveShell() is used, but called from the UI thread so as not to cause an exception.
	 *
	 * @return the found active shell (if any).
	 */
	public static Shell getActiveShell()
	{
		boolean mustSync = false;
		Display d = Display.getCurrent();
		if (d == null)
		{
			mustSync = true;
			d = Display.getDefault();
		}
		final Display disp = d;
		ReturnValueRunnable<Shell> r = new ReturnValueRunnable<Shell>()
		{
			public void run()
			{
				returnValue = disp.getActiveShell();
				if (returnValue == null)
				{
					IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					returnValue = (activeWindow != null) ? activeWindow.getShell() : null;
					if (returnValue == null)
					{
						Shell[] shells = disp.getShells();
						for (int i = shells.length; --i >= 0;)
						{
							if (shells[i].getParent() == null && shells[i].isVisible())
							{
								returnValue = shells[i];
								break;
							}
						}
						if (returnValue != null) while (returnValue.getParent() instanceof Shell && returnValue.getParent().isVisible())
						{
							returnValue = (Shell)returnValue.getParent();
						}
					}
				}
			}
		};
		if (mustSync)
		{
			// getActiveShell() throws exception if called from the wrong thread
			disp.syncExec(r);
			return r.getReturnValue();
		}
		else
		{
			r.run();
			return r.getReturnValue();
		}
	}

	/**
	 * WorkbenchImages.getImage(IWorkbenchGraphicConstants.IMG_LCL_RENDERED_VIEW_MENU) would give "discouraged access" warnings. So this is the code that creates the icon
	 * @return the image for drop-down menu in view toolbars.
	 */
	public static Image paintViewMenuImage()
	{
		Display d = Display.getCurrent();

		Image viewMenu = new Image(d, 11, 16);
		Image viewMenuMask = new Image(d, 11, 16);

		GC gc = new GC(viewMenu);
		GC maskgc = new GC(viewMenuMask);
		drawViewMenu(gc, maskgc);
		gc.dispose();
		maskgc.dispose();

		ImageData data = viewMenu.getImageData();
		data.transparentPixel = data.getPixel(0, 0);

		Image vm2 = new Image(d, viewMenu.getImageData(), viewMenuMask.getImageData());
		viewMenu.dispose();
		viewMenuMask.dispose();

		return vm2;
	}

	private static void drawViewMenu(GC gc, GC maskgc)
	{
		Display display = Display.getCurrent();

		gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		int[] shapeArray = new int[] { 1, 1, 10, 1, 6, 5, 5, 5 };
		gc.fillPolygon(shapeArray);
		gc.drawPolygon(shapeArray);

		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		Color white = display.getSystemColor(SWT.COLOR_WHITE);

		maskgc.setBackground(black);
		maskgc.fillRectangle(0, 0, 12, 16);

		maskgc.setBackground(white);
		maskgc.setForeground(white);
		maskgc.fillPolygon(shapeArray);
		maskgc.drawPolygon(shapeArray);
	}

	public static final int COMBO_VISIBLE_ITEM_COUNT = 10;

	public static void setDefaultVisibleItemCount(Combo combo)
	{
		combo.setVisibleItemCount(COMBO_VISIBLE_ITEM_COUNT);
	}

	public static boolean showChangeResourceProjectDlg(Shell parentShell, ServoyProject sp)
	{
		// show resource project choice dialog
		final ResourceProjectChoiceDialog dialog = new ResourceProjectChoiceDialog(parentShell,
			"Resources project for solution '" + sp.getProject().getName() + "'", sp.getResourcesProject(), false);

		if (dialog.open() == Window.OK)
		{
			IProject newResourcesProject;
			if (dialog.getResourceProjectData().getNewResourceProjectName() != null)
			{
				newResourcesProject = ServoyModel.getWorkspace().getRoot().getProject(dialog.getResourceProjectData().getNewResourceProjectName());
			}
			else
			{
				newResourcesProject = dialog.getResourceProjectData().getExistingResourceProject() != null
					? dialog.getResourceProjectData().getExistingResourceProject().getProject() : null;
			}
			// ok now associate the selected(create if necessary) resources project with the solution resources project
			WorkspaceJob job;
			// create new resource project if necessary and reference it from selected solution
			job = new ResourcesProjectSetupJob("Setting up resources project for solution '" + sp.getProject().getName() + "'", newResourcesProject, null,
				sp.getProject(), true);
			job.setRule(ServoyModel.getWorkspace().getRoot());
			job.setUser(true);
			job.schedule();
			return true;
		}

		return false;
	}

	/**
	 * Executes an invoke later on the AWT thread, but avoids (using a supplemental Job) InterruptedExceptions that could result when this is running on the SWT main thread because of
	 * a Display.syncExec being executed on another thread - that interrupts the main thread and throws exceptions that end like this:
	 *
	 * java.lang.InterruptedException
	 * 	at java.lang.Object.wait(Native Method)
	 * 	at java.lang.Object.wait(Object.java:503)
	 * 	at sun.awt.AWTAutoShutdown.activateBlockerThread(AWTAutoShutdown.java:337)
	 * 	at sun.awt.AWTAutoShutdown.notifyThreadBusy(AWTAutoShutdown.java:171)
	 * 	at java.awt.EventQueue.initDispatchThread(EventQueue.java:1025)
	 * 	at java.awt.EventQueue.postEventPrivate(EventQueue.java:250)
	 * 	at java.awt.EventQueue.postEvent(EventQueue.java:225)
	 * 	at java.awt.EventQueue.invokeLater(EventQueue.java:1192)
	 * 	at javax.swing.SwingUtilities.invokeLater(SwingUtilities.java:1287)
	 * 	(...)
	 *
	 * @param r the runnable to execute.
	 */
	public static void invokeLaterOnAWT(final Runnable r)
	{
		if (Display.getCurrent() == null)
		{
			SwingUtilities.invokeLater(r);
		}
		else
		{
			Job j = new Job("Call later on AWT")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					SwingUtilities.invokeLater(r);
					return Status.OK_STATUS;
				}
			};
			j.setSystem(true);
			j.setUser(false);
			j.schedule();
		}
	}

	public static boolean isDarkThemeSelected(boolean eclipseDarkTheme)
	{
		boolean IS_DARK_THEME = false;
		BundleContext ctx = Activator.getDefault().getBundle().getBundleContext();
		ServiceReference<IThemeManager> serviceReference = ctx.getServiceReference(IThemeManager.class);
		if (serviceReference != null)
		{
			IThemeManager manager = ctx.getService(serviceReference);
			if (manager != null)
			{
				Display d = Display.getDefault();
				IS_DARK_THEME = d.syncCall(() -> {
					Boolean retValue = Boolean.FALSE;
					IThemeEngine engine = manager.getEngineForDisplay(d);
					if (engine != null)
					{
						ITheme it = engine.getActiveTheme();
						if (it != null)
						{
							if (eclipseDarkTheme && ECLIPSE_DARK_THEME_ID.equals(it.getId()))
							{
								retValue = Boolean.TRUE;
							}
							if (!eclipseDarkTheme && isDarkTheme(it.getId()))
							{
								retValue = Boolean.TRUE;
							}
						}
					}
					return retValue;
				}).booleanValue();
			}
		}
		return IS_DARK_THEME;
	}

	public static boolean isDarkTheme(String id)
	{
		return ECLIPSE_DARK_THEME_ID.equals(id) || SERVOY_DARK_THEME_ID.equals(id);
	}

	public static String replaceLast(String string, String toReplace, String replacement)
	{
		int pos = string.lastIndexOf(toReplace);
		if (pos > -1)
		{
			return string.substring(0, pos) + replacement + string.substring(pos + toReplace.length());
		}
		else
		{
			return string;
		}
	}

}