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
package com.servoy.eclipse.ui.editors;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.dataprocessing.IUIConverter;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;

/**
 * Dialog for format options, includes options to configure UI converters
 *
 * @author jcompagner, rgansevles
 *
 */
public class FormatDialog extends Dialog
{
	private ParsedFormat parsedFormat;

	private Composite uiConverterContainer = null;

	private Composite formatContainer = null;
	private IFormatTextContainer formatComposite = null;

	private final int dataproviderType;
	private Integer convertedType;

	private Button useConvertersCheckbutton;

	private ConvertersComposite<IUIConverter> uiConvertersComposite;

	/**
	 * @param parent
	 * @param formatProperty
	 * @param objectType
	 */
	public FormatDialog(Shell parent, String formatProperty, int objectType)
	{
		super(parent);
		this.parsedFormat = FormatParser.parseFormatProperty(formatProperty);
		this.dataproviderType = objectType;
	}

	/**
	 * @param value
	 */
	public String getFormatString()
	{
		return parsedFormat.getFormatString();
	}

	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);
		shell.setText("Edit format property");
	}

	@Override
	protected Control createContents(Composite parent)
	{
		Control content = super.createContents(parent);
		getShell().pack();

		// macOS layout is wrong on very first rendering
		// simulate a click on the checkbox to force layout update
		if (SWT.getPlatform().equals("cocoa"))
		{
			parent.getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					if (useConvertersCheckbutton != null && !useConvertersCheckbutton.isDisposed())
					{
						boolean originalSelection = useConvertersCheckbutton.getSelection();
						useConvertersCheckbutton.setSelection(!originalSelection);
						useConvertersCheckbutton.notifyListeners(SWT.Selection, null);
						useConvertersCheckbutton.setSelection(originalSelection);
						useConvertersCheckbutton.notifyListeners(SWT.Selection, null);
					}
				}
			});
		}

		return content;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		// create components
		Composite composite = (Composite)super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		uiConverterContainer = new Composite(composite, SWT.BORDER);
		uiConverterContainer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

		useConvertersCheckbutton = new Button(uiConverterContainer, SWT.CHECK);
		useConvertersCheckbutton.setText("Use a UI converter");

		useConvertersCheckbutton.setEnabled(!ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile());

		formatContainer = new Composite(composite, SWT.NONE);
		formatContainer.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		formatContainer.setLayout(new GridLayout());

		uiConvertersComposite = new ConvertersComposite<IUIConverter>(uiConverterContainer, SWT.NONE)
		{
			@Override
			protected int[] getSupportedTypes(IUIConverter converter)
			{
				return converter.getSupportedDataproviderTypes();
			}
		};

		// layout components
		GroupLayout gl_uiConverterContainer = new GroupLayout(uiConverterContainer);
		gl_uiConverterContainer.setHorizontalGroup(gl_uiConverterContainer.createParallelGroup(GroupLayout.LEADING).add(
			gl_uiConverterContainer.createSequentialGroup().add(
				gl_uiConverterContainer.createParallelGroup(GroupLayout.LEADING).add(
					gl_uiConverterContainer.createSequentialGroup().addContainerGap().add(useConvertersCheckbutton, GroupLayout.PREFERRED_SIZE, 169,
						GroupLayout.PREFERRED_SIZE))
					.add(uiConvertersComposite))
				.addContainerGap()));
		gl_uiConverterContainer.setVerticalGroup(gl_uiConverterContainer.createParallelGroup(GroupLayout.LEADING).add(
			gl_uiConverterContainer.createSequentialGroup().add(useConvertersCheckbutton, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(uiConvertersComposite, GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)));

		uiConverterContainer.setLayout(gl_uiConverterContainer);

		// set data
		useConvertersCheckbutton.setSelection(parsedFormat.getUIConverterName() != null);
		uiConvertersComposite.setConverters(dataproviderType, parsedFormat.getUIConverterName(),
			ApplicationServerRegistry.get().getPluginManager().getUIConverterManager().getConverters().values());

		setPropertiesForSelectedConverter();
		recreateFormatTextContainer();
		layoutForUIConverters();

		// add listeners
		useConvertersCheckbutton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				handleUseConvertersChanged();
				layoutForUIConverters();
				recreateFormatTextContainer();
				((Composite)getContents()).layout(true, true);
				getShell().pack();
			}
		});

		uiConvertersComposite.addConverterChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				handleConverterChanged();
				setPropertiesForSelectedConverter();
				recreateFormatTextContainer();
				((Composite)getContents()).layout(true, true);
				getShell().pack();
			}
		});

		uiConvertersComposite.addPropertyChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				handleConverterPropertyChanged();
				recreateFormatTextContainer();
				((Composite)getContents()).layout(true, true);
			}
		});

		return composite;
	}

	protected void handleUseConvertersChanged()
	{
		if (!useConvertersCheckbutton.getSelection())
		{
			parsedFormat = parsedFormat.getCopy(null, null);
		}
	}

	protected void handleConverterChanged()
	{
		if (useConvertersCheckbutton.getSelection())
		{
			IUIConverter conv = uiConvertersComposite.getSelectedConverter();
			parsedFormat = parsedFormat.getCopy(conv == null ? null : conv.getName(), null);
		}
	}

	protected void handleConverterPropertyChanged()
	{
		if (useConvertersCheckbutton.getSelection())
		{
			parsedFormat = parsedFormat.getCopy(parsedFormat.getUIConverterName(), uiConvertersComposite.getProperties());
		}
	}

	protected void layoutForUIConverters()
	{
		boolean useConverters = useConvertersCheckbutton.getSelection();
		uiConvertersComposite.setVisible(useConverters);
		((GridData)uiConverterContainer.getLayoutData()).heightHint = useConverters ? -1 : 50;
	}

	protected void recreateFormatTextContainer()
	{
		int newConvertedType;
		if (convertedType == null)
		{
			newConvertedType = calculateConvertedType();
		}
		else if (convertedType.intValue() == (newConvertedType = calculateConvertedType()))
		{
			// type unchanged
			return;
		}

		convertedType = Integer.valueOf(newConvertedType);

		// recreate format container
		if (formatComposite != null)
		{
			((Widget)formatComposite).dispose();
			formatComposite = null;
		}

		String title;
		switch (newConvertedType)
		{
			case IColumnTypes.DATETIME :

				formatComposite = new FormatDateContainer(formatContainer, SWT.NONE);
				title = "Edit date format property";
				break;

			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :

				formatComposite = new FormatIntegerContainer(formatContainer, SWT.NONE);
				title = "Edit integer/number format property";
				break;

			default :
				formatComposite = new FormatTextContainer(formatContainer, SWT.NONE);
				title = "Edit text format property";
		}

		((Composite)formatComposite).setLayoutData(new GridData(GridData.FILL_BOTH));

		formatComposite.setParsedFormat(parsedFormat);

		getShell().setText(title);
	}

	protected int calculateConvertedType()
	{
		if (useConvertersCheckbutton.getSelection())
		{
			IUIConverter conv = uiConvertersComposite.getSelectedConverter();
			if (conv != null)
			{
				int uiType = conv.getToObjectType(uiConvertersComposite.getProperties());
				if (uiType != Integer.MAX_VALUE)
				{
					return Column.mapToDefaultType(uiType);
				}
			}
		}

		return dataproviderType;
	}

	/**
	 *
	 */
	protected void setPropertiesForSelectedConverter()
	{
		uiConvertersComposite.setProperties(parsedFormat.getUIConverterProperties());
	}

	@Override
	protected void okPressed()
	{
		try
		{
			parsedFormat = formatComposite.getParsedFormat().getCopy(parsedFormat.getUIConverterName(), parsedFormat.getUIConverterProperties());
			super.okPressed();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
			UIUtils.reportError("Unexpected error while parsing format", ex.getMessage());
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return EditorUtil.getDialogSettings("formatDialog");
	}

	public static interface IFormatTextContainer
	{
		ParsedFormat getParsedFormat();

		void setParsedFormat(ParsedFormat parsedFormat);
	}

}