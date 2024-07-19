/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.property.PersistPropertySource.NullDefaultLabelProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;

/**
 * @author  emera
 */
public class VersionPropertyController extends PropertySetterController<String, String, PersistPropertySource>
{

	public VersionPropertyController(Object id, String displayName)
	{
		super(id, displayName, PersistPropertySource.NULL_STRING_CONVERTER, PersistPropertySource.NullDefaultLabelProvider.LABEL_NONE, null);
	}

	@Override
	public boolean isReadOnly()
	{
		return false;
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return NullDefaultLabelProvider.LABEL_NONE;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new CellEditor(parent, SWT.NONE)
		{
			private String value;
			private boolean setValue = true;

			@Override
			protected Control createControl(Composite parentComposite)
			{
				Text text = new Text(parentComposite, SWT.NONE);
				text.addModifyListener(event -> {
					if (!setValue) return;
					doSetValue(text.getText());
				});
				text.addKeyListener(new KeyListener()
				{
					@Override
					public void keyReleased(KeyEvent e)
					{
					}

					@Override
					public void keyPressed(KeyEvent e)
					{
						if (e.keyCode == SWT.CR)
						{
							doSetValue(text.getText());
						}
					}
				});
				return text;
			}

			@Override
			protected Object doGetValue()
			{
				return value;
			}

			@Override
			protected void doSetFocus()
			{
				setValue = false;
				((Text)getControl()).setText("-none-".equals(value) ? "" : value);
				((Text)getControl()).setSelection(((Text)getControl()).getText().length());
				setValue = true;
			}

			@Override
			protected void doSetValue(Object newValue)
			{
				this.value = "-none-".equals(((String)newValue).trim()) ? "" : ((String)newValue).trim();
				markDirty();
				fireApplyEditorValue();
			}
		};
	}

	@Override
	public void setProperty(PersistPropertySource propertySource, String value)
	{
		IPersist persist = propertySource.getPersist();
		if (persist instanceof Solution)
		{
			((Solution)persist).setVersion(value);
		}
	}

	@Override
	public String getProperty(PersistPropertySource propertySource)
	{
		IPersist persist = propertySource.getPersist();
		if (persist instanceof Solution)
		{
			return ((Solution)persist).getVersion();
		}
		return null;
	}
}