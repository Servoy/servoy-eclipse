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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.editors.FormatDialog.IFormatTextContainer;
import com.servoy.j2db.util.FormatParser.ParsedFormat;

/**
 * @author jcompagner
 *
 */
public class FormatTextContainer extends Composite implements IFormatTextContainer
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
	private final Combo displayFormat;
	private final Text placeHolder;
	private final Button useRaw;
	private final Button lowerCase;
	private final Button upperCase;
	private final Button numberInput;
	private final ModifyListener textListener;
	protected boolean ignoreTextChanges;
	private final Button displayFormatRadio;
	private Point caret = null;
	private final Text allowedCharacters;

	/**
	 * @param parent
	 * @param style
	 */
	@SuppressWarnings("nls")
	public FormatTextContainer(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new GridLayout(2, false));

		textListener = new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				if (!ignoreTextChanges)
				{
					upperCase.setSelection(false);
					lowerCase.setSelection(false);
					numberInput.setSelection(false);
					displayFormatRadio.setSelection(true);
					placeHolder.setEnabled(true);
					useRaw.setEnabled(true);
				}
			}
		};
		ignoreTextChanges = true;

		upperCase = new Button(this, SWT.RADIO);
		upperCase.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (upperCase.getSelection())
				{
					placeHolder.setEnabled(false);
					useRaw.setEnabled(false);
				}
			}
		});
		upperCase.setText("All Uppercase");
		new Label(this, SWT.NONE);

		lowerCase = new Button(this, SWT.RADIO);
		lowerCase.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (lowerCase.getSelection())
				{
					placeHolder.setEnabled(false);
					useRaw.setEnabled(false);
				}
			}
		});
		lowerCase.setText("All Lowercase");
		new Label(this, SWT.NONE);

		numberInput = new Button(this, SWT.RADIO);
		numberInput.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (numberInput.getSelection())
				{
					placeHolder.setEnabled(false);
					useRaw.setEnabled(false);
				}
			}
		});
		numberInput.setText("Only Numbers");
		new Label(this, SWT.NONE);

		displayFormatRadio = new Button(this, SWT.RADIO);
		displayFormatRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				placeHolder.setEnabled(true);
				useRaw.setEnabled(true);
			}

		});
		displayFormatRadio.setText("Display Format");

		displayFormat = new Combo(this, SWT.NONE);
		displayFormat.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				caret = displayFormat.getSelection();
			}
		});
		displayFormat.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				caret = displayFormat.getSelection();
			}
		});
		displayFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		displayFormat.setItems(new String[] { "####UU", "###-#######" });
		displayFormat.addModifyListener(textListener);

		Label lblEditFormat = new Label(this, SWT.NONE);
		lblEditFormat.setText("PlaceHolder");

		placeHolder = new Text(this, SWT.BORDER);
		placeHolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		placeHolder.addModifyListener(textListener);

		Label lblAllowedCharFormat = new Label(this, SWT.NONE);
		lblAllowedCharFormat.setText("Allowed Characters for '*'");

		allowedCharacters = new Text(this, SWT.BORDER);
		allowedCharacters.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		allowedCharacters.addModifyListener(textListener);
		new Label(this, SWT.NONE);

		useRaw = new Button(this, SWT.CHECK);
		useRaw.setText("Raw value (Literals are not in the real value)");
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
		tblclmnMeaning.setWidth(300);
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
					displayFormat.setText(txt);
				}
			}
		});
		tableViewer.setInput(new String[][] { { "#", "Any valid number (0-9)" }, { "'", "Escape character, used to escape any of the special formatting characters" }, { "U", "Any alpha character. All lowercase letters are mapped to upper case (A-Z)" }, { "L", "Any alpha character. All upper case letters are mapped to lower case (a-z)" }, { "A", "Any alpha character or number (a-z,A-Z,0-9)" }, { "?", "Any alpha character (a-z,A-Z)" }, { "*", "Anything" }, { "H", "Any hex character, lowercase will be converted to uppercase (0-9,A-F)" } });
		ignoreTextChanges = false;
	}

	/**
	 * @see com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer#getFormat()
	 */
	@SuppressWarnings("nls")
	public ParsedFormat getParsedFormat()
	{
		return new ParsedFormat(upperCase.getSelection(), lowerCase.getSelection(), numberInput.getSelection(), useRaw.getSelection(), false,
			placeHolder.getText(), displayFormat.getText(), null, null, null, allowedCharacters.getText());
	}

	/**
	 * @see com.servoy.eclipse.ui.editors.FormatCellEditor.IFormatTextContainer#setFormat(java.lang.String)
	 */
	public void setParsedFormat(ParsedFormat parsedFormat)
	{
		clearText();
		upperCase.setSelection(false);
		lowerCase.setSelection(false);
		displayFormatRadio.setSelection(true);
		placeHolder.setEnabled(true);
		useRaw.setEnabled(true);
		if (parsedFormat != null)
		{
			upperCase.setSelection(parsedFormat.isAllUpperCase());
			lowerCase.setSelection(parsedFormat.isAllLowerCase());
			numberInput.setSelection(parsedFormat.isNumberValidator());
			if (parsedFormat.isAllUpperCase() || parsedFormat.isAllLowerCase() || parsedFormat.isNumberValidator())
			{
				displayFormatRadio.setSelection(false);
				placeHolder.setEnabled(false);
				useRaw.setEnabled(false);
			}
			else
			{
				displayFormat.setText(parsedFormat.getDisplayFormat() != null ? parsedFormat.getDisplayFormat() : "");
				allowedCharacters.setText(parsedFormat.getAllowedCharacters() != null ? parsedFormat.getAllowedCharacters() : "");
				useRaw.setSelection(parsedFormat.isRaw());
				if (parsedFormat.getPlaceHolderString() != null)
				{
					placeHolder.setText(parsedFormat.getPlaceHolderString());
				}
				else if (parsedFormat.getPlaceHolderCharacter() != 0)
				{
					placeHolder.setText(Character.toString(parsedFormat.getPlaceHolderCharacter()));
				}
			}
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("nls")
	private void clearText()
	{
		ignoreTextChanges = true;
		placeHolder.setText("");
		displayFormat.setText("");
		allowedCharacters.setText("");
		caret = null;
		useRaw.setSelection(false);
		ignoreTextChanges = false;
	}
}
