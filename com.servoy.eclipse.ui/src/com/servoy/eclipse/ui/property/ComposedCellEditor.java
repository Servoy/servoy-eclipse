/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.property;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.servoy.eclipse.core.util.ReturnValueSnippet;

/**
 * Cell editor useful for visually aggregating horizontally 2 cell editors.
 * Depending on the value of {@link #cellEditor1IsMain} one or the other cell editor has priority when setting values, getting focus, ...
 *
 * @author acostescu
 */
public class ComposedCellEditor extends CellEditor
{

	protected CellEditor cellEditor1;
	protected CellEditor cellEditor2;
	protected boolean cellEditor1IsMain;
	protected Object oldValue;
	protected boolean childNestedCellEditor;

	private ICellEditorListener cellEditorListener;
	private Listener hideL1sT3n3r;
	private IPropertyChangeListener enablementListener;
	private int freeSpaceInPixels;
	private ReturnValueSnippet<CellEditor, Composite> cellEditorCreator1;
	private ReturnValueSnippet<CellEditor, Composite> cellEditorCreator2;

	/**
	 * When using this constructor please make sure to call either ({@link #setCellEditor1(CellEditor)} or {@link #setCellEditorCreator1(ReturnValueSnippet)})
	 * and ({@link #setCellEditor2(CellEditor)} or {@link #setCellEditorCreator2(ReturnValueSnippet)}) before {@link #create(Composite)} or {@link #createControl(Composite)} are called.
	 *
	 * @param childNestedCellEditor this is a workaround for a bug in GridLayout (minimumWidth of GridData is ignored, and we rely
	 * on widthHint to change minimumWidth); set this to false if ComposedCellEditor is the main cell editor that is shown in properties view and
	 * set it to true if you have nested ComposedCellEditors for the child composed cell editors.
	 * @param freeSpaceInPixels empty space between the two sub-cell-editors; usually 0 would be fine.
	 */
	public ComposedCellEditor(boolean cellEditor1IsMain, boolean childNestedCellEditor, int freeSpaceInPixels)
	{
		init(cellEditor1IsMain, childNestedCellEditor, freeSpaceInPixels);
	}

	/**
	 * @param childNestedCellEditor this is a workaround for a bug in GridLayout (minimumWidth of GridData is ignored, and we rely
	 * on widthHint to change minimumWidth); set this to false if ComposedCellEditor is the main cell editor that is shown in properties view and
	 * set it to true if you have nested ComposedCellEditors for the child composed cell editors.
	 * @param freeSpaceInPixels empty space between the two sub-cell-editors; usually 0 would be fine.
	 */
	public ComposedCellEditor(CellEditor cellEditor1, CellEditor cellEditor2, boolean cellEditor1IsMain, boolean childNestedCellEditor, int freeSpaceInPixels)
	{
		this.cellEditor1 = cellEditor1;
		this.cellEditor2 = cellEditor2;

		init(cellEditor1IsMain, childNestedCellEditor, freeSpaceInPixels);
	}

	/**
	 * In case you must use a cell editor that gets it's parent and calls create in it's own constructor, then we need getters for the editors instead of direct references.
	 * This way the editors are created only when {@link #createControl(Composite)} is called on this instance.
	 *
	 * @param childNestedCellEditor this is a workaround for a bug in GridLayout (minimumWidth of GridData is ignored, and we rely
	 * on widthHint to change minimumWidth); set this to false if ComposedCellEditor is the main cell editor that is shown in properties view and
	 * set it to true if you have nested ComposedCellEditors for the child composed cell editors.
	 * @param freeSpaceInPixels empty space between the two sub-cell-editors; usually 0 would be fine.
	 */
	public ComposedCellEditor(ReturnValueSnippet<CellEditor, Composite> cellEditorCreator1, ReturnValueSnippet<CellEditor, Composite> cellEditorCreator2,
		boolean cellEditor1IsMain, boolean childNestedCellEditor, int freeSpaceInPixels)
	{
		this.cellEditorCreator1 = cellEditorCreator1;
		this.cellEditorCreator2 = cellEditorCreator2;
		init(cellEditor1IsMain, childNestedCellEditor, freeSpaceInPixels);
	}

	/**
	 * @param cellEditor1 the cellEditor1 to set
	 */
	public void setCellEditor1(CellEditor cellEditor1)
	{
		this.cellEditor1 = cellEditor1;
	}

	/**
	 * @param cellEditor2 the cellEditor2 to set
	 */
	public void setCellEditor2(CellEditor cellEditor2)
	{
		this.cellEditor2 = cellEditor2;
	}

	/**
	 * @param cellEditorCreator1 the cellEditorCreator1 to set
	 */
	public void setCellEditorCreator1(ReturnValueSnippet<CellEditor, Composite> cellEditorCreator1)
	{
		this.cellEditorCreator1 = cellEditorCreator1;
	}

	/**
	 * @param cellEditorCreator2 the cellEditorCreator2 to set
	 */
	public void setCellEditorCreator2(ReturnValueSnippet<CellEditor, Composite> cellEditorCreator2)
	{
		this.cellEditorCreator2 = cellEditorCreator2;
	}

	protected void init(boolean cellEditor1IsMain, boolean childNestedCellEditor, int freeSpaceInPixels)
	{
		this.cellEditor1IsMain = cellEditor1IsMain;
		this.childNestedCellEditor = childNestedCellEditor;
		this.freeSpaceInPixels = freeSpaceInPixels;

		// TODO this listener might need to be a bit smarter
		cellEditorListener = new ICellEditorListener()
		{

			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState)
			{
				fireEditorValueChanged(oldValidState, newValidState);
			}

			@Override
			public void cancelEditor()
			{
				fireCancelEditor();
			}

			@Override
			public void applyEditorValue()
			{
				fireApplyEditorValue();
			}

		};


		// TODO this listener might need to be a bit smarter
		enablementListener = new IPropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				fireEnablementChanged(event.getProperty());
			}
		};
	}

	protected void addChildEditorListeners()
	{
		cellEditor1.addListener(cellEditorListener);
		cellEditor2.addListener(cellEditorListener);
		cellEditor1.addPropertyChangeListener(enablementListener);
		cellEditor2.addPropertyChangeListener(enablementListener);
	}

	@Override
	protected Control createControl(final Composite parent)
	{
		Composite composite = (childNestedCellEditor ? new Composite(parent, SWT.NONE) : new MarkerComposite(parent, SWT.NONE));
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = freeSpaceInPixels;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
		gridLayout.numColumns = 2;

		Composite cellEditor1ControlParent = new Composite(composite, SWT.NONE);
		cellEditor1ControlParent.setLayout(new FillLayout());
		if (cellEditor1 != null) cellEditor1.create(cellEditor1ControlParent);
		else cellEditor1 = cellEditorCreator1.run(cellEditor1ControlParent);

		Composite cellEditor2ControlParent = new Composite(composite, SWT.NONE);
		cellEditor2ControlParent.setLayout(new FillLayout());
		if (cellEditor2 != null) cellEditor2.create(cellEditor2ControlParent);
		else cellEditor2 = cellEditorCreator2.run(cellEditor2ControlParent);

		addChildEditorListeners();

		GridData gdCellEditor1 = new GridData();
		gdCellEditor1.horizontalAlignment = SWT.FILL;
//		gdCellEditor1.minimumWidth = 0; // this doesn't seem to work for text cell editor as editor1; setting widthHint instead... (but this workaround has the side effect that preferred size is then 0 as well, so nested ComposedCellEditors might not show the 'middle' components anymore as preferred is 0px), that is why the boolean on next line is used
		if (!childNestedCellEditor) gdCellEditor1.widthHint = 0; // see comment above
		gdCellEditor1.verticalAlignment = SWT.FILL;
		gdCellEditor1.grabExcessHorizontalSpace = true;
		gdCellEditor1.grabExcessVerticalSpace = true;
		cellEditor1ControlParent.setLayoutData(gdCellEditor1);

		GridData gdCellEditor2 = new GridData();
		gdCellEditor2.horizontalAlignment = SWT.FILL;
		gdCellEditor2.grabExcessHorizontalSpace = false;
		gdCellEditor2.grabExcessVerticalSpace = true;
		gdCellEditor2.verticalAlignment = SWT.FILL;
		cellEditor2ControlParent.setLayoutData(gdCellEditor2);

		setValueValid(true);

		setChildControlsVisibility(true);
		composite.pack();
		setChildControlsVisibility(false); // CellEditor auto-sets these to invisible after creating them. So we toggle them just a bit before packing.

		// when a cell editor deactivates itself (unfortunately we don't have an event for that) it will hide it's control; in that case we should hide
		// the other cell editor as well so as not to end up with only one of the two cell editors being visible
		hideL1sT3n3r = new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				deactivate();
			}
		};

		cellEditor1.getControl().addListener(SWT.Hide, hideL1sT3n3r);
		cellEditor2.getControl().addListener(SWT.Hide, hideL1sT3n3r);

		return composite;
	}

	@Override
	protected Object doGetValue()
	{
		Object v1 = cellEditor1.getValue();
		Object v2 = cellEditor2.getValue();

		if (v1 == v2)
		{
			oldValue = v1;
		}
		else if (oldValue == v1)
		{
			oldValue = v2;
			cellEditor1.setValue(oldValue);
		}
		else if (oldValue == v2)
		{
			oldValue = v1;
			cellEditor2.setValue(oldValue);
		}
		else
		{
			if (cellEditor1IsMain)
			{
				oldValue = v1;
				cellEditor2.setValue(oldValue);
			}
			else
			{
				oldValue = v2;
				cellEditor1.setValue(oldValue);
			}
		}

		return oldValue;
	}

	@Override
	protected void doSetFocus()
	{
		if (cellEditor1IsMain) cellEditor1.setFocus();
		else cellEditor2.setFocus();
	}

	@Override
	protected void doSetValue(Object val)
	{
		oldValue = val;
		cellEditor1.setValue(val);
		cellEditor2.setValue(val);
	}

	@Override
	public boolean isDirty()
	{
		return cellEditor1.isDirty() || cellEditor2.isDirty();
	}

	@Override
	public void activate()
	{
		cellEditor1.activate();
		cellEditor2.activate();

		setChildControlsVisibility(true);
	}

	protected void setChildControlsVisibility(boolean visible)
	{
		cellEditor1.getControl().setVisible(visible);
		cellEditor2.getControl().setVisible(visible);
	}

	@Override
	public void dispose()
	{
		super.dispose();

		if (!cellEditor1.getControl().isDisposed()) cellEditor1.getControl().removeListener(SWT.Hide, hideL1sT3n3r);
		if (!cellEditor2.getControl().isDisposed()) cellEditor2.getControl().removeListener(SWT.Hide, hideL1sT3n3r);

		cellEditor1.removeListener(cellEditorListener);
		cellEditor2.removeListener(cellEditorListener);

		cellEditor1.removePropertyChangeListener(enablementListener);
		cellEditor2.removePropertyChangeListener(enablementListener);
	}

	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent)
	{
		super.activate(activationEvent);
		if (cellEditor1IsMain)
		{
			cellEditor2.activate();
			cellEditor1.activate();
		}
		else
		{
			cellEditor2.activate();
			cellEditor1.activate();
		}
	}

	@Override
	public void deactivate()
	{
		cellEditor1.deactivate();
		cellEditor2.deactivate();
		super.deactivate();
	}

	@Override
	public String getErrorMessage()
	{
		String e;
		if (cellEditor1IsMain) e = cellEditor1.getErrorMessage();
		else e = cellEditor2.getErrorMessage();

		if (e == null)
		{
			if (cellEditor1IsMain) e = cellEditor2.getErrorMessage();
			else e = cellEditor1.getErrorMessage();
		}
		return e;
	}

	// TODO here we define more methods with default void impl. - that sub-cell-editors might define
	// some might need nicer impl. then these fast ones below...
	@Override
	public boolean isCopyEnabled()
	{
		return cellEditor1.isCopyEnabled() || cellEditor2.isCopyEnabled();
	}

	@Override
	public boolean isCutEnabled()
	{
		return cellEditor1.isCutEnabled() || cellEditor2.isCutEnabled();
	}

	@Override
	public boolean isDeleteEnabled()
	{
		return cellEditor1.isDeleteEnabled() || cellEditor2.isDeleteEnabled();
	}

	@Override
	public boolean isFindEnabled()
	{
		return cellEditor1.isFindEnabled() || cellEditor2.isFindEnabled();
	}

	@Override
	public boolean isPasteEnabled()
	{
		return cellEditor1.isPasteEnabled() || cellEditor2.isPasteEnabled();
	}

	@Override
	public boolean isRedoEnabled()
	{
		return cellEditor1.isRedoEnabled() || cellEditor2.isRedoEnabled();
	}

	@Override
	public boolean isSelectAllEnabled()
	{
		return cellEditor1.isSelectAllEnabled() || cellEditor2.isSelectAllEnabled();
	}

	@Override
	public boolean isUndoEnabled()
	{
		return cellEditor1.isUndoEnabled() || cellEditor2.isUndoEnabled();
	}

	@Override
	public void performCopy()
	{
		cellEditor1.performCopy();
		cellEditor2.performCopy();
	}

	@Override
	public void performCut()
	{
		cellEditor1.performCut();
		cellEditor2.performCut();
	}

	@Override
	public void performDelete()
	{
		cellEditor1.performDelete();
		cellEditor2.performDelete();
	}

	@Override
	public void performFind()
	{
		cellEditor1.performFind();
		cellEditor2.performFind();
	}

	@Override
	public void performPaste()
	{
		cellEditor1.performPaste();
		cellEditor2.performPaste();
	}

	@Override
	public void performRedo()
	{
		cellEditor1.performRedo();
		cellEditor2.performRedo();
	}

	@Override
	public void performSelectAll()
	{
		cellEditor1.performSelectAll();
		cellEditor2.performSelectAll();
	}

	@Override
	public void performUndo()
	{
		cellEditor1.performUndo();
		cellEditor2.performUndo();
	}

	public static boolean isRootComposedCellEditor(Composite c)
	{
		return c instanceof MarkerComposite;
	}

	protected static class MarkerComposite extends Composite
	{

		public MarkerComposite(Composite parent, int style)
		{
			super(parent, style);
		}

	}

}
