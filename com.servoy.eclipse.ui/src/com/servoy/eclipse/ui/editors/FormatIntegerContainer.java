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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.ibm.icu.text.DecimalFormat;
import com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer;
import com.servoy.j2db.util.FormatParser;

/**
 * @author jcompagner
 *
 */
public class FormatIntegerContainer extends Composite implements IFormatTextContainer
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
	private final Combo editFormat;
	private final Combo displayFormat;

	private Point displayCaret = null;
	private Point editCaret = null;

	/**
	 * @param parent
	 * @param style
	 */
	@SuppressWarnings("nls")
	public FormatIntegerContainer(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label lblDisplayFormat = new Label(this, SWT.NONE);
		lblDisplayFormat.setText("Display Format");

		displayFormat = new Combo(this, SWT.NONE);
		displayFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		displayFormat.setItems(new String[] { "#.00", "#,###", "\u00A4#.00" });
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
		editFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		editFormat.setItems(new String[] { "#.00", "#,###", "\u00A4#.00" });
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

		TableViewerColumn tableViewerColumn_2_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnMeaning = tableViewerColumn_2_1.getColumn();
		tblclmnMeaning.setWidth(340);
		tblclmnMeaning.setText("Description");

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
		tableViewer.setInput(new String[][] { { "0", "Digit" }, { "#", "Digit, zero shows as basent" }, { ".", "Decimal separator or monetary decimal separator" }, { "-", "Minus sign" }, { ",", "Grouping separator" }, { "E", "Separates mantissa and exponent in scientific notation." }, { "%", "Multiply by 100 and show as percentage (prefix or suffix)" }, { "\u2030", "Multiply by 1000 and show as per mille value (prefix or suffix)" }, { "\u00A4", "Currency sign, replaced by currency symbol. (prefix or suffix)" } });
		new Label(this, SWT.NONE);
	}

	/**
	 * @see com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer#getFormat()
	 */
	public String getFormat()
	{
		String format = displayFormat.getText();
		// test it
		new DecimalFormat(format);
		if (format.length() > 0 && editFormat.getText().length() > 0)
		{
			// test it
			new DecimalFormat(editFormat.getText());
			format = format + "|" + editFormat.getText(); //$NON-NLS-1$
		}
		return format;

	}

	/**
	 * @see com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer#setFormat(java.lang.String)
	 */
	@SuppressWarnings("nls")
	public void setFormat(String format)
	{
		displayFormat.setText("");
		editFormat.setText("");

		if (format != null)
		{
			FormatParser fp = new FormatParser(format);

			if (fp.hasEditFormat())
			{
				displayFormat.setText(fp.getDisplayFormat());
				editFormat.setText(fp.getEditFormat());
			}
			else
			{
				displayFormat.setText(fp.getDisplayFormat());
				editFormat.setText("");
			}
		}
	}
}
