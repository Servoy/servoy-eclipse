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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.servoy.eclipse.core.util.ReturnValueSnippet;

/**
 * Base class for wrapping around another cell editor (the 'base' cell editor).
 * Useful when a thrid party editor needs to be reused but with some aspects slightly altered.
 *
 * @author acostescu
 */
public abstract class ProxyCellEditor<T, CT> extends CellEditor
{

	protected CellEditor baseCellEditor;

	private ICellEditorListener cellEditorListener;
	private Listener hideL1sT3n3r;
	private IPropertyChangeListener enablementListener;
	private ReturnValueSnippet<CellEditor, Composite> baseCellEditorCreator;

	/**
	 * When using this constructor please make sure to call either ({@link #setBaseCellEditor(CellEditor)} or {@link #setBaseCellEditorCreator(ReturnValueSnippet)})
	 * before {@link #create(Composite)} or {@link #createControl(Composite)} are called.
	 */
	public ProxyCellEditor()
	{
		init();
	}

	public ProxyCellEditor(CellEditor baseCellEditor)
	{
		this.baseCellEditor = baseCellEditor;
		init();
	}

	/**
	 * In case you must use a cell editor that gets it's parent and calls create in it's own constructor, then we need a getter for the editor instead of a direct reference.
	 * This way the editor is created only when {@link #createControl(Composite)} is called on this instance.
	 */
	public ProxyCellEditor(ReturnValueSnippet<CellEditor, Composite> baseCellEditorCreator)
	{
		this.baseCellEditorCreator = baseCellEditorCreator;
		init();
	}

	public void setBaseCellEditor(CellEditor baseCellEditor)
	{
		this.baseCellEditor = baseCellEditor;
	}

	public void setBaseCellEditorCreator(ReturnValueSnippet<CellEditor, Composite> baseCellEditorCreator)
	{
		this.baseCellEditorCreator = baseCellEditorCreator;
	}

	protected void init()
	{
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
		baseCellEditor.addListener(cellEditorListener);
		baseCellEditor.addPropertyChangeListener(enablementListener);
	}

	@Override
	protected Control createControl(final Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NONE);
		FillLayout fillLayout = new FillLayout();
		composite.setLayout(fillLayout);

		if (baseCellEditor != null) baseCellEditor.create(composite);
		else baseCellEditor = baseCellEditorCreator.run(composite);

		addChildEditorListeners();

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

		baseCellEditor.getControl().addListener(SWT.Hide, hideL1sT3n3r);

		return composite;
	}

	@Override
	protected Object doGetValue()
	{
		return baseCellEditor.getValue();
	}

	@Override
	protected void doSetValue(Object v)
	{
		baseCellEditor.setValue(v);
	}

	@Override
	protected void doSetFocus()
	{
		baseCellEditor.setFocus();
	}

	@Override
	public void activate()
	{
		baseCellEditor.activate();
		setChildControlsVisibility(true);
	}

	protected void setChildControlsVisibility(boolean visible)
	{
		baseCellEditor.getControl().setVisible(visible);
	}

	@Override
	public boolean isDirty()
	{
		return baseCellEditor.isDirty();
	}

	@Override
	public void dispose()
	{
		super.dispose();

		if (!baseCellEditor.getControl().isDisposed()) baseCellEditor.getControl().removeListener(SWT.Hide, hideL1sT3n3r);
		baseCellEditor.removeListener(cellEditorListener);
		baseCellEditor.removePropertyChangeListener(enablementListener);
	}

	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent)
	{
		super.activate(activationEvent);
		baseCellEditor.activate();
	}

	@Override
	public void deactivate()
	{
		baseCellEditor.deactivate();
		super.deactivate();
	}

	@Override
	public String getErrorMessage()
	{
		return baseCellEditor.getErrorMessage();
	}

	// TODO here we define more methods with default void impl. - that sub-cell-editors might define
	// some might need nicer impl. then these fast ones below...
	@Override
	public boolean isCopyEnabled()
	{
		return baseCellEditor.isCopyEnabled();
	}

	@Override
	public boolean isCutEnabled()
	{
		return baseCellEditor.isCutEnabled();
	}

	@Override
	public boolean isDeleteEnabled()
	{
		return baseCellEditor.isDeleteEnabled();
	}

	@Override
	public boolean isFindEnabled()
	{
		return baseCellEditor.isFindEnabled();
	}

	@Override
	public boolean isPasteEnabled()
	{
		return baseCellEditor.isPasteEnabled();
	}

	@Override
	public boolean isRedoEnabled()
	{
		return baseCellEditor.isRedoEnabled();
	}

	@Override
	public boolean isSelectAllEnabled()
	{
		return baseCellEditor.isSelectAllEnabled();
	}

	@Override
	public boolean isUndoEnabled()
	{
		return baseCellEditor.isUndoEnabled();
	}

	@Override
	public void performCopy()
	{
		baseCellEditor.performCopy();
	}

	@Override
	public void performCut()
	{
		baseCellEditor.performCut();
	}

	@Override
	public void performDelete()
	{
		baseCellEditor.performDelete();
	}

	@Override
	public void performFind()
	{
		baseCellEditor.performFind();
	}

	@Override
	public void performPaste()
	{
		baseCellEditor.performPaste();
	}

	@Override
	public void performRedo()
	{
		baseCellEditor.performRedo();
	}

	@Override
	public void performSelectAll()
	{
		baseCellEditor.performSelectAll();
	}

	@Override
	public void performUndo()
	{
		baseCellEditor.performUndo();
	}

}
