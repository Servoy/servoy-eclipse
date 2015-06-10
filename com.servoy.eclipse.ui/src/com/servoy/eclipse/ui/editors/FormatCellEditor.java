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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class FormatCellEditor extends TextDialogCellEditor
{

	private final IPersist persist;
	private final String[] formatForPropertyNames;

	/**
	 * @param parent
	 * @param persist
	 */
	public FormatCellEditor(Composite parent, IPersist persist, String[] formatForPropertyNames)
	{
		super(parent, SWT.NONE, null);
		this.persist = persist;
		this.formatForPropertyNames = formatForPropertyNames;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		String formatString = (String)getValue();

		int type = IColumnTypes.TEXT;
		if (formatForPropertyNames != null && formatForPropertyNames.length > 0)
		{
			String webComponentClassName = FormTemplateGenerator.getComponentTypeName((IFormElement)persist);

			WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponentClassName);
			if (spec != null)
			{
				FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
				for (String propertyName : formatForPropertyNames)
				{
					PropertyDescription pd = spec.getProperty(propertyName);
					if (pd != null)
					{
						if (pd.getType() instanceof DataproviderPropertyType)
						{
							String dataProviderID = (String)((AbstractBase)persist).getProperty(propertyName);
							if (dataProviderID != null)
							{
								Form form = (Form)persist.getAncestor(IRepository.FORMS);
								if (form != null)
								{
									IDataProviderLookup dataproviderLookup = flattenedSolution.getDataproviderLookup(null, form);
									ComponentFormat componentFormat = ComponentFormat.getComponentFormat(formatString, dataProviderID, dataproviderLookup,
										Activator.getDefault().getDesignClient());
									type = componentFormat.dpType;
								}
								break;
							}
						}
						else if (pd.getType() instanceof ValueListPropertyType)
						{
							int valuelistID = Utils.getAsInteger(((AbstractBase)persist).getProperty(propertyName));
							if (valuelistID > 0)
							{
								ValueList vl = flattenedSolution.getValueList(valuelistID);
								if (vl != null)
								{
									IDataProvider dataProvider = null;
									ITable table = null;
									try
									{
										if (vl.getRelationName() != null)
										{
											Relation[] relations = flattenedSolution.getRelationSequence(vl.getRelationName());
											table = relations[relations.length - 1].getForeignTable();
										}
										else
										{
											table = vl.getTable();
										}
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
									if (table != null)
									{
										String dp = null;
										int showDataProviders = vl.getShowDataProviders();
										if (showDataProviders == 1)
										{
											dp = vl.getDataProviderID1();
										}
										else if (showDataProviders == 2)
										{
											dp = vl.getDataProviderID2();
										}
										else if (showDataProviders == 4)
										{
											dp = vl.getDataProviderID3();
										}

										if (dp != null)
										{
											try
											{
												dataProvider = flattenedSolution.getDataProviderForTable((Table)table, dp);
											}
											catch (Exception ex)
											{
												ServoyLog.logError(ex);
											}
											if (dataProvider != null)
											{
												type = dataProvider.getDataProviderType();
											}
										}
									}
									break;
								}
							}
						}
					}
				}
			}
		}

		FormatDialog dialog = new FormatDialog(cellEditorWindow.getShell(), formatString, type);
		dialog.open();
		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return TextDialogCellEditor.CANCELVALUE;
		}
		return dialog.getFormatString();
	}
}
