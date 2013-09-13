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

import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.FormatDialog.IFormatTextContainer;
import com.servoy.j2db.util.FormatParser.ParsedFormat;

/**
 * @author jcompagner
 *
 */
public class FormatDateContainer extends Composite implements IFormatTextContainer
{
	private class TableLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public Image getColumnImage(Object element, int columnIndex)
		{
			return null;
		}

		public String getColumnText(Object element, int columnIndex)
		{
			return ((Object[])element)[columnIndex].toString();
		}
	}

	private final Table table;
	private final Text placeholder;
	private final Button useMask;
	private final Combo displayFormat;
	private final Combo editFormat;

	private Point displayCaret = null;
	private Point editCaret = null;


	/**
	 * @param parent
	 * @param style
	 */
	@SuppressWarnings("nls")
	public FormatDateContainer(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new GridLayout(2, false));

		boolean mobile = ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile();

		Label lblDisplayFormat = new Label(this, SWT.NONE);
		lblDisplayFormat.setText("Display Format");

		displayFormat = new Combo(this, SWT.NONE);
		GridData gridData_1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gridData_1.widthHint = 351;
		displayFormat.setLayoutData(gridData_1);
		String[] formatItems = new String[] { "dd-MM-yyyy", "dd-MM-yyyy HH:mm", "MM/dd/yyyy", "MM/dd/yyyy hh:mm aa", "dd.MM.yyyy" };
		displayFormat.setItems(formatItems);
		displayFormat.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				displayCaret = displayFormat.getSelection();
				editCaret = null;
			}
		});
		displayFormat.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				displayCaret = displayFormat.getSelection();
				editCaret = null;
			}
		});

		Label lblEditFormat = new Label(this, SWT.NONE);
		lblEditFormat.setText("Edit Format");

		editFormat = new Combo(this, SWT.NONE);
		editFormat.setEnabled(!mobile);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gridData.widthHint = 411;
		editFormat.setLayoutData(gridData);
		editFormat.setItems(formatItems);
		editFormat.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				editCaret = editFormat.getSelection();
				displayCaret = null;
			}
		});
		editFormat.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				editCaret = editFormat.getSelection();
				displayCaret = null;
			}
		});
		new Label(this, SWT.NONE);

		useMask = new Button(this, SWT.CHECK);
		useMask.setEnabled(!mobile);
		useMask.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				editFormat.setEnabled(!useMask.getSelection());
				placeholder.setEnabled(useMask.getSelection());
			}
		});
		useMask.setText("Use as mask (the exact format is used, 1 symbol for 1 character)");

		Label lblPlaceholder = new Label(this, SWT.NONE);
		lblPlaceholder.setText("Placeholder");

		placeholder = new Text(this, SWT.BORDER);
		placeholder.setEnabled(!mobile);
		GridData gridData_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gridData_2.widthHint = 29;
		placeholder.setLayoutData(gridData_2);
		placeholder.setTextLimit(1);
		placeholder.setEnabled(false);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		Label lblLegenda = new Label(this, SWT.NONE);
		lblLegenda.setText("Legend");
		new Label(this, SWT.NONE);

		TableViewer tableViewer = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		table.setHeaderVisible(true);

		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnSymbol = tableViewerColumn.getColumn();
		tblclmnSymbol.setWidth(90);
		tblclmnSymbol.setText("Character");
		tableViewerColumn.getColumn().setText("Character");

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnPresentation = tableViewerColumn_1.getColumn();
		tblclmnPresentation.setWidth(90);
		tblclmnPresentation.setText("Presentation");

		TableViewerColumn tableViewerColumn_2_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnMeaning = tableViewerColumn_2_1.getColumn();
		tblclmnMeaning.setWidth(400);
		tblclmnMeaning.setText("Description");

		if (mobile)
		{
			Label mobileClientFormatWarning = new Label(this, SWT.NONE);
			mobileClientFormatWarning.setForeground(Activator.getDefault().getSharedTextColors().getColor(new RGB(255, 102, 51)));
			mobileClientFormatWarning.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
			mobileClientFormatWarning.setText("A format for a Calendar field is not used to format the data when it is mapped on a native date field.\r\nIts only used to determine of a \"date\", \"time\" or \"datetime\" should be used.");
		}

		tableViewer.setLabelProvider(new TableLabelProvider());
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				if (event.getSelection() instanceof IStructuredSelection)
				{
					IStructuredSelection selection = (IStructuredSelection)event.getSelection();
					String[] selectedRow = (String[])selection.getFirstElement();

					String txt = displayFormat.getText();
					Combo selected = displayFormat;
					Point caret = displayCaret;
					if (editCaret != null)
					{
						txt = editFormat.getText();
						selected = editFormat;
						caret = editCaret;
					}
					if (caret == null)
					{
						txt += selectedRow[0];
					}
					else
					{
						txt = txt.substring(0, caret.x) + selectedRow[0] + txt.substring(caret.y);
						caret.x++;
						caret.y = caret.x;
					}
					selected.setText(txt);
				}
			}
		});
		tableViewer.setInput(new String[][] { { "G", "Text", "Era designator" }, { "y", "Number", "Year" }, { "M", "Number/Text", "Month in year" }, { "w", "Number", "Week in year" }, { "W", "Number", "Week in month" }, { "D", "Number", "Day in year" }, { "d", "Number", "Day in month" }, { "F", "Number", "Day of week in month" }, { "E", "Text", "Day in week" }, { "a", "Text", "Am/Pm marker" }, { "H", "Number", "Hour in day (0-23)" }, { "k", "Number", "Hour in day (1-24)" }, { "K", "Number", "Hour in am/pm (0-11)" }, { "h", "Number", "Hour in am/pm (1-12)" }, { "m", "Number", "Minute in hour" }, { "s", "Number", "Second in minute" }, { "S", "Number", "Millisecond" }, { "z", "Text", "Time zone" }, { "Z", "Text", "RFC 822 Time zone" }, });
	}

	/**
	 * @see com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer#getFormat()
	 */
	@SuppressWarnings("nls")
	public ParsedFormat getParsedFormat()
	{
		String dispformat = displayFormat.getText();
		String editOrPlaceholder = null;
		if (dispformat.length() > 0)
		{
			if (useMask.getSelection())
			{
				new SimpleDateFormat(dispformat); // test
				if (placeholder.getText().length() > 0) editOrPlaceholder = placeholder.getText().substring(0, 1);
			}
			else if (editFormat.getText().length() > 0)
			{
				new SimpleDateFormat(editFormat.getText()); // test
				editOrPlaceholder = editFormat.getText();
			}
		}

		return new ParsedFormat(false, false, false, false, useMask.getSelection(), editOrPlaceholder, displayFormat.getText(), null, null, null, null);
	}

	/**
	 * @see com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer#setFormat(java.lang.String)
	 */
	@SuppressWarnings("nls")
	public void setParsedFormat(ParsedFormat parsedFormat)
	{
		boolean mobile = ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile();
		displayFormat.setText("");
		editFormat.setText("");
		placeholder.setText("");
		useMask.setSelection(false);
		placeholder.setEnabled(false);
		editFormat.setEnabled(!mobile);
		if (parsedFormat != null)
		{
			displayFormat.setText(parsedFormat.getDisplayFormat() != null ? parsedFormat.getDisplayFormat() : "");
			if (parsedFormat.isMask())
			{
				useMask.setSelection(!mobile);
				placeholder.setEnabled(!mobile);
				editFormat.setEnabled(false);
				if (parsedFormat.getPlaceHolderCharacter() != 0) placeholder.setText(Character.toString(parsedFormat.getPlaceHolderCharacter()));
			}
			else if (parsedFormat.hasEditFormat()) editFormat.setText(parsedFormat.getEditFormat());
		}
	}
}
