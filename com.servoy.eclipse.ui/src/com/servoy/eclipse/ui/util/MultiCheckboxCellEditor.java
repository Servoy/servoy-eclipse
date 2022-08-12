package com.servoy.eclipse.ui.util;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.editors.MultiCheckSelectionCombo;

/**
 * A cell editor that presents a list of options as checkboxes. The cell editor's
 * value is the zero-based list of indexes of the checked items.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MultiCheckboxCellEditor extends CellEditor
{
	/**
	 * The list is dropped down when the activation is done through the mouse
	 */
	public static final int DROP_DOWN_ON_MOUSE_ACTIVATION = 1;

	/**
	 * The list is dropped down when the activation is done through the keyboard
	 */
	public static final int DROP_DOWN_ON_KEY_ACTIVATION = 1 << 1;

	/**
	 * The list is dropped down when the activation is done without
	 * ui-interaction
	 */
	public static final int DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION = 1 << 2;

	/**
	 * The list is dropped down when the activation is done by traversing from
	 * cell to cell
	 */
	public static final int DROP_DOWN_ON_TRAVERSE_ACTIVATION = 1 << 3;

	private int activationStyle = DROP_DOWN_ON_MOUSE_ACTIVATION;

	/**
	 * The list of items to present in the combo box.
	 */
	private String[] items;

	/**
	 * The zero-based index of the selected item.
	 */
	int[] selectionIndexes;

	/**
	 * The custom combo box control.
	 */
	MultiCheckSelectionCombo multiCheckSelectionCombo;

	/**
	 * Default ComboBoxCellEditor style
	 */
	private static final int defaultStyle = SWT.NONE;

	private static final int[] NO_SELECTION = new int[0];

	/**
	 * Creates a new cell editor with no control and no items.
	 * Initially, the cell editor has no cell validator.
	 *
	 * @see CellEditor#setStyle
	 * @see CellEditor#create
	 * @see ComboBoxCellEditor#setItems
	 * @see CellEditor#dispose
	 */
	public MultiCheckboxCellEditor()
	{
		setStyle(defaultStyle);
	}

	/**
	 * Creates a new cell editor with a combo containing the given list of
	 * choices and parented under the given control. The cell editor value is
	 * an empty array of indexes. Initially, the cell editor has
	 * no cell validator.
	 *
	 * @param parent
	 *            the parent control
	 * @param items
	 *            the list of strings for the combo box
	 */
	public MultiCheckboxCellEditor(Composite parent, String[] items)
	{
		this(parent, items, defaultStyle);
	}

	/**
	 * Creates a new cell editor with a combo containing the given list of
	 * choices and parented under the given control. The cell editor value is
	 * an empty array of indexes. Initially, the cell editor has
	 * no cell validator.
	 *
	 *
	 * @param parent
	 *            the parent control
	 * @param items
	 *            the list of strings for the combo box
	 * @param style
	 *            the style bits
	 */
	public MultiCheckboxCellEditor(Composite parent, String[] items, int style)
	{
		super(parent, style);
		setItems(items);
	}

	/**
	 * Returns the list of choices for the checkboxes
	 *
	 * @return the list of choices for the checkboxes
	 */
	public String[] getItems()
	{
		return this.items;
	}

	/**
	 * Sets the list of choices for the checkboxes
	 *
	 * @param items
	 *            the list of choices for the checkboxes
	 */
	public void setItems(String[] items)
	{
		Assert.isNotNull(items);
		this.items = items;
		populateComboBoxItems();
	}

	/**
	 * @param defaultText the defaultText to set
	 */
	public void setDefaultText(String defaultText)
	{
		multiCheckSelectionCombo.setDefaultText(defaultText);
	}

	@Override
	protected Control createControl(Composite parent)
	{
		multiCheckSelectionCombo = new MultiCheckSelectionCombo(parent, getStyle());
		// pack shell when combo selection display changes and resizes
		multiCheckSelectionCombo.addModifyListener(e -> parent.pack());

		multiCheckSelectionCombo.setFont(parent.getFont());

		populateComboBoxItems();

		multiCheckSelectionCombo.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				keyReleaseOccured(e);
			}
		});

		multiCheckSelectionCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetDefaultSelected(SelectionEvent event)
			{
				applyEditorValueAndDeactivate();
			}

			@Override
			public void widgetSelected(SelectionEvent event)
			{
				selectionIndexes = multiCheckSelectionCombo.getSelectionIndices();
			}
		});

		multiCheckSelectionCombo.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN)
			{
				e.doit = false;
			}
		});

		multiCheckSelectionCombo.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				MultiCheckboxCellEditor.this.focusLost();
			}
		});

		return multiCheckSelectionCombo;
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method returns the zero-based index
	 * of the current selection.
	 *
	 * @return the zero-based index of the current selection wrapped as an
	 *         <code>Integer</code>
	 */
	@Override
	protected Object doGetValue()
	{
		return selectionIndexes;
	}

	@Override
	protected void doSetFocus()
	{
		multiCheckSelectionCombo.setFocus();
	}

	@Override
	public LayoutData getLayoutData()
	{
		LayoutData layoutData = super.getLayoutData();
		if ((multiCheckSelectionCombo == null) || multiCheckSelectionCombo.isDisposed())
		{
			layoutData.minimumWidth = 60;
		}
		else
		{
			// make the comboBox 10 characters wide
			GC gc = new GC(multiCheckSelectionCombo);
			layoutData.minimumWidth = (int)((gc.getFontMetrics().getAverageCharacterWidth() * 10) + 10);
			gc.dispose();
		}
		return layoutData;
	}

	@Override
	protected void doSetValue(Object value)
	{
		Assert.isTrue(multiCheckSelectionCombo != null && (value instanceof int[]));
		selectionIndexes = (int[])value;
		multiCheckSelectionCombo.deselectAll();
		multiCheckSelectionCombo.select(selectionIndexes);
	}

	/**
	 * Updates the list of choices for the combo box for the current control.
	 */
	private void populateComboBoxItems()
	{
		if (multiCheckSelectionCombo != null && items != null)
		{
			multiCheckSelectionCombo.removeAll();
			for (int i = 0; i < items.length; i++)
			{
				multiCheckSelectionCombo.add(items[i], i);
			}

			setValueValid(true);
			selectionIndexes = NO_SELECTION;
		}
	}

	/**
	 * Applies the currently selected value and deactivates the cell editor
	 */
	void applyEditorValueAndDeactivate()
	{
		// must set the selection before getting value
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid)
		{
			setErrorMessage(MessageFormat.format(getErrorMessage(), newValue));
		}

		fireApplyEditorValue();
		deactivate();
	}

	@Override
	protected void focusLost()
	{
		if (isActivated())
		{
			applyEditorValueAndDeactivate();
		}
	}

	@Override
	protected void keyReleaseOccured(KeyEvent keyEvent)
	{
		if (keyEvent.character == '\u001b')
		{ // Escape character
			fireCancelEditor();
		}
		else if (keyEvent.character == '\t')
		{ // tab key
			applyEditorValueAndDeactivate();
		}
	}

	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent)
	{
		if (activationStyle != SWT.NONE)
		{
			boolean dropDown = false;
			if ((activationEvent.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION ||
				activationEvent.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) &&
				(activationStyle & DROP_DOWN_ON_MOUSE_ACTIVATION) != 0)
			{
				dropDown = true;
			}
			else if (activationEvent.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && (activationStyle & DROP_DOWN_ON_KEY_ACTIVATION) != 0)
			{
				dropDown = true;
			}
			else if (activationEvent.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC &&
				(activationStyle & DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION) != 0)
			{
				dropDown = true;
			}
			else if (activationEvent.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL && (activationStyle & DROP_DOWN_ON_TRAVERSE_ACTIVATION) != 0)
			{
				dropDown = true;
			}

			if (dropDown)
			{
				getControl().getDisplay().asyncExec(() -> ((MultiCheckSelectionCombo)getControl()).showDropdown());
			}
		}
	}

	/**
	 * This method allows to control how the combo reacts when activated
	 *
	 * @param activationStyle
	 *            the style used
	 */
	public void setActivationStyle(int activationStyle)
	{
		this.activationStyle = activationStyle;
	}
}
