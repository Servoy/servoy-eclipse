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
package com.servoy.eclipse.designer.property;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseFormGraphicalRootEditPart;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.editors.IDialogDirectCellEditor;
import com.servoy.eclipse.ui.editors.TextDialogCellEditor;

/**
 * Edit properties in form editor, cell editor is same one as used in the properties view.
 *
 * @author rgansevles
 */

public class PropertyDirectEditManager extends DirectEditManager
{
	protected Font scaledFont;
	protected final IPropertyDescriptor propertyDescriptor;
	protected final CellEditorLocator cellEditorLocator;

	public PropertyDirectEditManager(GraphicalEditPart source, CellEditorLocator locator, Object propertyId)
	{
		super(source, null, locator);
		this.cellEditorLocator = locator;
		this.propertyDescriptor = getPropertyDescriptor(source, propertyId);
	}

	@Override
	protected DirectEditRequest createDirectEditRequest()
	{
		DirectEditRequest req = super.createDirectEditRequest();
		req.setDirectEditFeature(propertyDescriptor);
		return req;
	}

	/**
	 * @see org.eclipse.gef.tools.DirectEditManager#bringDown()
	 */
	@Override
	protected void bringDown()
	{
		// This method might be re-entered when super.bringDown() is called.
		Font disposeFont = scaledFont;
		scaledFont = null;
		super.bringDown();
		if (disposeFont != null) disposeFont.dispose();

		BaseFormGraphicalRootEditPart root = (BaseFormGraphicalRootEditPart)getEditPart().getRoot();
		BaseVisualFormEditor formEditor = root.getEditorPart();
		formEditor.activateEditorContext();
	}

	@Override
	protected CellEditor createCellEditorOn(Composite composite)
	{
		if (propertyDescriptor == null)
		{
			return null;
		}
		return propertyDescriptor.createPropertyEditor(composite);
	}

	@Override
	protected boolean isDirty()
	{
		if (super.isDirty())
		{
			return true;
		}

		CellEditor cellEditor = getCellEditor();
		return cellEditor != null && cellEditor.isDirty();
	}

	@Override
	protected void initCellEditor()
	{
		CellEditor cellEditor = getCellEditor();
		if (cellEditor != null)
		{
			Object initialValue = getPropertyValue(propertyDescriptor);
			cellEditor.setValue(initialValue);
			Control editor = cellEditor.getControl();
			IFigure figure = getEditPart().getFigure();
			scaledFont = figure.getFont();
			FontData data = scaledFont.getFontData()[0];
			Dimension fontSize = new Dimension(0, data.getHeight());
			getEditPart().getFigure().translateToAbsolute(fontSize);
			data.setHeight(fontSize.height);
			scaledFont = new Font(null, data);

			editor.setFont(scaledFont);
		}
	}

	/**
	 * Set the error text. This should be set to null when the current value is valid, otherwise it should be set to a error string
	 */
	protected void setErrorText()
	{
		((BaseFormGraphicalRootEditPart)getEditPart().getRoot()).getEditorPart().getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(
			getCellEditor().isValueValid() ? null : getCellEditor().getErrorMessage());
	}


	/**
	 * Override show to enable immediate showing of dialog direct editors.
	 */
	@Override
	public void show()
	{
		if (getCellEditor() != null) return;
		Composite composite = (Composite)getEditPart().getViewer().getControl();
		setCellEditor(createCellEditorOn(composite));
		if (getCellEditor() == null) return;
		initCellEditor();
		if (getCellEditor() instanceof IDialogDirectCellEditor)
		{
			// do not show the button, just open the dialog
			((IDialogDirectCellEditor)getCellEditor()).editValue(composite);
		}
		else
		{
			BaseFormGraphicalRootEditPart root = (BaseFormGraphicalRootEditPart)getEditPart().getRoot();
			BaseVisualFormEditor formEditor = root.getEditorPart();
			formEditor.deactivateEditorContext();

			getCellEditor().activate();
			if (getCellEditor() instanceof TextDialogCellEditor)
			{
				((TextDialogCellEditor)getCellEditor()).setAlwaysLooseFocus(true);
			}
			cellEditorLocator.relocate(getCellEditor());
			getCellEditor().getControl().setVisible(true);
			getCellEditor().setFocus();
			showFeedback();
		}
	}

	@Override
	public void showFeedback()
	{
		setErrorText();
		super.showFeedback();
	}

	@Override
	protected void commit()
	{
		if (getCellEditor().isValueValid())
		{
			super.commit();
		}
		else
		{
			// do not commit an invalid value, show a dialog instead
			final String errorMessage = getCellEditor().getErrorMessage() == null ? "Invalid value" : getCellEditor().getErrorMessage();
			try
			{
				eraseFeedback();
			}
			finally
			{
				bringDown();
			}
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Could not set property value", errorMessage);
				}
			});
		}
	}

	/**
	 * Get the property value.
	 * <p>
	 * Gets the property value of the property specified by the property. It should be as the editable property value because it will go straight into the
	 * appropriate cell editor.
	 *
	 * @param property
	 * @return a String value
	 */
	protected Object getPropertyValue(IPropertyDescriptor property)
	{
		// retrieve the property's value from the model
		Object value = null;
		IPropertySource ps = getEditPart().getAdapter(IPropertySource.class);
		if (ps.isPropertySet(property.getId()))
		{
			value = ps.getPropertyValue(property.getId());
			if (value instanceof IPropertySource)
			{
				value = ((IPropertySource)value).getEditableValue();
			}
		}
		return value;
	}

	/**
	 * Get the property descriptor for a property id.
	 */
	public static IPropertyDescriptor getPropertyDescriptor(EditPart editPart, Object propertyId)
	{
		IPropertySource ps = editPart.getAdapter(IPropertySource.class);
		if (ps != null)
		{
			for (IPropertyDescriptor pd : ps.getPropertyDescriptors())
			{
				if (pd.getId().equals(propertyId))
				{
					return pd;
				}
			}
		}
		return null;
	}

	public static class PropertyCellEditorLocator implements CellEditorLocator
	{
		private final GraphicalEditPart editPart;

		public PropertyCellEditorLocator(GraphicalEditPart editPart)
		{
			this.editPart = editPart;
		}

		public void relocate(CellEditor cellEditor)
		{
			Control control = cellEditor.getControl();
			Point sel = null;
			if (control instanceof Text)
			{
				sel = ((Text)control).getSelection();
			}
			Point pref = control.computeSize(-1, -1);
			Point textSize = pref;
			// look for a text component, determine its size
			if (control instanceof Composite)
			{
				Control[] children = ((Composite)control).getChildren();
				for (Control child : children)
				{
					if (child instanceof Text)
					{
						textSize = child.computeSize(-1, -1);
						break;
					}
				}
			}
			IFigure figure = editPart.getFigure();
			Rectangle rect = figure.getBounds().getCopy();
			figure.translateToAbsolute(rect);
			// if there is a text-inner control, make the text control as large as the figure
			control.setBounds(rect.x, rect.y, Math.max(rect.width + pref.x - textSize.x, pref.x + 1), Math.max(rect.height + pref.y - textSize.y, pref.y + 1));
			control.setBackground(ColorConstants.white);
			if (control instanceof Text)
			{
				((Text)control).setSelection(0);
				((Text)control).setSelection(sel);
			}
		}
	}
}
