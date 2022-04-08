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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.StyleClassPropertyType;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ServoyStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.TitleStringPropertyType;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 * @since 2022.06
 *
 */
public class PropertyWizardDialog extends Dialog
{
	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private final ITable table;
	private final DataProviderOptions dataproviderOptions;
	private final IDialogSettings settings;
	private final Collection<PropertyDescription> wizardProperties;
	private final PropertyDescription property;
	private final List<Map<String, Object>> input;
	private DataproviderPropertiesSelector dataprovidersSelector;
	private AutoWizardPropertiesComposite tableComposite;
	private StylePropertiesSelector stylePropertiesSelector;

	PropertyWizardDialog(Shell parentShell, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final IDialogSettings settings, PropertyDescription property, Collection<PropertyDescription> wizardProperties,
		List<Map<String, Object>> input)
	{
		super(parentShell);
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.dataproviderOptions = dataproviderOptions;
		this.settings = settings;
		this.property = property;
		this.wizardProperties = wizardProperties;
		this.input = input;
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Property configurator for " + property.getName());

		Composite area = (Composite)super.createDialogArea(parent);
		// first check what the main thing must be (dataproviders, forms, relations?)

		//TODO move to the builder
		List<PropertyDescription> dataproviderProperties = wizardProperties.stream()
			.filter(prop -> FoundsetLinkedPropertyType.class.isAssignableFrom(prop.getType().getClass()) ||
				DataproviderPropertyType.class.isAssignableFrom(prop.getType().getClass()))
			.sorted((desc1, desc2) -> Integer.parseInt((String)desc1.getTag("wizard")) - Integer.parseInt((String)desc2.getTag("wizard")))
			.collect(Collectors.toList());
		// should be only 1 (or only 1 with values)
		List<PropertyDescription> styleProperties = filterProperties(StyleClassPropertyType.class);
		List<PropertyDescription> i18nProperties = filterProperties(TitleStringPropertyType.class);
		List<PropertyDescription> stringProperties = filterProperties(ServoyStringPropertyType.class);

		Composite c = new Composite(area, SWT.NONE);
		c.setLayout(new FillLayout());
		SashForm form = new SashForm(c, SWT.HORIZONTAL);
		SashForm form2 = new SashForm(form, SWT.VERTICAL);

		if (dataproviderProperties.size() > 0)
		{
			dataprovidersSelector = new DataproviderPropertiesSelector(this, form2, persistContext, dataproviderProperties, flattenedSolution,
				dataproviderOptions, table, settings, getShell());
		}

		if (styleProperties.size() > 0) // should really be always 1 size..
		{
			stylePropertiesSelector = new StylePropertiesSelector(this, form2, styleProperties);
		}

		if (dataprovidersSelector != null && stylePropertiesSelector.stylePropertiesViewer != null)
			form2.setWeights(70, 30);

		tableComposite = new AutoWizardPropertiesComposite(form, persistContext, flattenedSolution, table,
			dataproviderProperties, styleProperties, i18nProperties, stringProperties, input);
		return area;
	}


	private List<PropertyDescription> filterProperties(Class< ? > cls)
	{
		return wizardProperties.stream()
			.filter(prop -> cls.isAssignableFrom(prop.getType().getClass()))
			.collect(Collectors.toList());
	}

	public List<Map<String, Object>> getResult()
	{
		if (tableComposite != null)
		{
			return tableComposite.getResult();
		}
		return Collections.emptyList();
	}

	void setTreeInput(List<Pair<String, Map<String, Object>>> list)
	{
		tableComposite.setInput(list);

	}

	void addNewRow(String id, Map<String, Object> row)
	{
		tableComposite.addNewRow(new Pair<String, Map<String, Object>>(id, row));
	}

	public List<Pair<String, Map<String, Object>>> getInput()
	{
		return tableComposite.getInput();
	}
}
