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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;


/**
 * 
 * tabSeq property from the Property view of serclipse was read only. This cell editor
 * allows the "tab sequence" tab of the design editor to be opened when the button near
 * the tabSeq label is pressed
 * 
 * @author alorincz
 *
 */
public class TabSeqDialogCellEditor extends DialogCellEditor
{
	/**
	 * @param parent
	 * @param labelProvider
	 * @param valueEditor
	 * @param readOnly
	 * @param style
	 */
	public TabSeqDialogCellEditor(Composite parent, ILabelProvider labelProvider, IValueEditor valueEditor, boolean readOnly, int style)
	{
		super(parent, labelProvider, valueEditor, readOnly, style);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow)
	{
		return null;
	}

	public static class TabSeqDialogValueEditor implements IValueEditor<Object>
	{
		private final Form form;

		public TabSeqDialogValueEditor(Form form)
		{
			this.form = form;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.ui.editors.IValueEditor#canEdit(java.lang.Object)
		 */
		public boolean canEdit(Object value)
		{
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.ui.editors.IValueEditor#openEditor(java.lang.Object)
		 */
		public void openEditor(Object value)
		{
			if (form == null) return;
			IEditorPart editorRef = EditorUtil.openFormDesignEditor(form);
			if (editorRef instanceof ITabbedEditor)
			{
				((ITabbedEditor)editorRef).changeActiveTab("Tab sequence");
			}
		}

	}
}
