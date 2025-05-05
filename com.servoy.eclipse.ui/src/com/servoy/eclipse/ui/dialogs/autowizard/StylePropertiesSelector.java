/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

/**
 * @author  emera
 */
public class StylePropertiesSelector
{
	public TableViewer stylePropertiesViewer;
	private List<PropertyDescription> styleProperties;
	private PropertyWizardDialog wizard;

	public StylePropertiesSelector(PropertyWizardDialog wizard, final SashForm parent,
		List<PropertyDescription> styleProperties)
	{
		this.wizard = wizard;
		this.styleProperties = styleProperties;

		Object config = styleProperties.get(0).getTag("wizard");
		Object tag = config instanceof JSONObject ? ((JSONObject)config).optJSONArray("values") : config;
		if (tag instanceof JSONArray)
		{
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.marginRight = 5;
			Composite listParent = new Composite(parent, SWT.NONE);
			listParent.setLayout(layout);

			GridData gridData = new GridData();
			gridData.verticalAlignment = GridData.FILL;
			gridData.horizontalSpan = 1;
			gridData.verticalSpan = 1;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalAlignment = GridData.FILL;
			gridData.minimumWidth = 10;
			gridData.heightHint = 250;

			stylePropertiesViewer = new TableViewer(listParent);
			stylePropertiesViewer.getControl().setLayoutData(gridData);
			stylePropertiesViewer.setContentProvider(JSONContentProvider.INSTANCE);
			stylePropertiesViewer.setLabelProvider(new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					if (element instanceof JSONObject)
					{
						return ((JSONObject)element).getString("name");
					}
					return "";
				}

				@Override
				public Image getImage(Object element)
				{
					if (element instanceof JSONObject)
					{
						String cls = ((JSONObject)element).optString("cls", "");
						Image img = getFaImage(cls);
						return img;
					}
					return null;
				}
			});
			stylePropertiesViewer.setInput(tag);

			stylePropertiesViewer.addOpenListener(event -> moveStyleSelection());
			stylePropertiesViewer.addSelectionChangedListener(event -> moveStyleSelection());
		}
	}

	private void moveStyleSelection()
	{
		Object style = ((StructuredSelection)stylePropertiesViewer.getSelection()).getFirstElement();
		if (style instanceof JSONObject)
		{
			String styleClass = ((JSONObject)style).getString("cls");
			Map<String, Object> map = new HashMap<>();
			PropertyDescription propertyDescription = styleProperties.get(0);
			map.put(propertyDescription.getName(), styleClass);
			wizard.addNewRow(map);
		}
	}

	public static Image getFaImage(String cls)
	{
		String cls_ = cls.replace(" ", "-");
		return com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle("wizards/" + cls_ + ".png");
	}
}