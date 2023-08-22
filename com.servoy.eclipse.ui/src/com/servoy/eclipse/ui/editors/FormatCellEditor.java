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
import org.json.JSONObject;
import org.sablo.specification.IYieldingType;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IPropertyType;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
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
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			FlattenedSolution flattenedSolution = servoyModel.getFlattenedSolution();
			for (String propertyName : formatForPropertyNames)
			{
				PropertyDescription pd = null;
				if (persist instanceof WebCustomType wct)
				{
					PropertyDescription customTypePD = wct.getPropertyDescription();
					pd = customTypePD.getProperty(propertyName);
					IBasicWebObject parent = wct.getParent();
					while (pd == null)
					{
						if (parent instanceof WebCustomType p)
						{
							customTypePD = p.getPropertyDescription();
							pd = customTypePD.getProperty(propertyName);
							parent = p.getParent();
						}
						else
						{
							String webComponentClassName = parent.getTypeName();
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponentClassName);
							pd = spec.getProperty(propertyName);
							break;
						}
					}
				}
				else if (persist instanceof IBasicWebObject p)
				{
					String webComponentClassName = p.getTypeName();
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponentClassName);
					pd = spec.getProperty(propertyName);

				}
				else if (persist instanceof IFormElement)
				{
					WebObjectSpecification spec = FormTemplateGenerator.getWebObjectSpecification((IFormElement)persist);
					pd = spec.getProperty(propertyName);
				}
				if (pd != null)
				{
					IPropertyType propertyType = pd.getType();
					if (propertyType instanceof IYieldingType)
					{
						propertyType = ((IYieldingType)propertyType).getPossibleYieldType();
					}
					if (propertyType instanceof DataproviderPropertyType)
					{
						String dataProviderID = (String)((AbstractBase)persist).getProperty(propertyName);
						if (dataProviderID == null && persist instanceof IBasicWebObject)
						{
							dataProviderID = ((IBasicWebObject)persist).getProperty(propertyName) != null
								? (String)((IBasicWebObject)persist).getProperty(propertyName) : null;
						}
						if (dataProviderID != null)
						{
							Object config = pd.getConfig();
							// if it is a dataprovider type. look if it is foundset linked
							if (config instanceof FoundsetLinkedConfig && ((FoundsetLinkedConfig)config).getForFoundsetName() != null)
							{
								Object json = ((AbstractBase)persist.getParent()).getProperty(((FoundsetLinkedConfig)config).getForFoundsetName());
								if (json instanceof JSONObject)
								{
									// get the foundset selector and try to resolve the table
									String fs = ((JSONObject)json).optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
									ITable table = servoyModel.getDataSourceManager().getDataSource(fs);
									if (table == null)
									{
										// table not found is the foundset selector a relation.
										Relation[] relations = flattenedSolution.getRelationSequence(fs);
										if (relations != null && relations.length > 0)
										{
											table = servoyModel.getDataSourceManager().getDataSource(
												relations[relations.length - 1].getForeignDataSource());
										}
									}
									try
									{
										IDataProvider dataProvider = flattenedSolution.getDataProviderForTable(table, dataProviderID);
										// the dataprovider is found through the table, returns for this the ComponentFormat, if not fall through through the forms
										// dataprovider lookup below
										if (dataProvider != null)
										{
											type = dataProvider.getDataProviderType();
											break;
										}
									}
									catch (RepositoryException e)
									{
										Debug.error(e);
									}
								}
							}
							Form form = (Form)persist.getAncestor(IRepository.FORMS);
							if (form != null)
							{
								form = flattenedSolution.getFlattenedForm(form);
								IDataProviderLookup dataproviderLookup = flattenedSolution.getDataproviderLookup(
									Activator.getDefault().getDesignClient().getFoundSetManager(), form);
								ComponentFormat componentFormat = ComponentFormat.getComponentFormat(formatString, dataProviderID, dataproviderLookup,
									Activator.getDefault().getDesignClient());
								type = componentFormat.dpType;
							}
							break;
						}
					}
					else if (propertyType instanceof ValueListPropertyType)
					{
						ValueList vl = null;
						if (persist instanceof WebCustomType)
						{
							Object property = ((WebCustomType)persist).getProperty(propertyName);
							if (property != null)
							{
								vl = (ValueList)flattenedSolution.searchPersist(property.toString());
							}
						}
						else
						{
							int valuelistID = Utils.getAsInteger(((AbstractBase)persist).getProperty(propertyName));
							if (valuelistID > 0)
							{
								vl = flattenedSolution.getValueList(valuelistID);
							}
						}

						if (vl != null)
						{
							if ((vl.getValueListType() == IValueListConstants.CUSTOM_VALUES ||
								vl.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES))
							{
								int displayValueType = vl.getDisplayValueType();
								if (displayValueType != 0)
								{
									type = displayValueType;
									break;
								}
							}

							IDataProvider dataProvider = null;
							ITable table = null;
							try
							{
								if (vl.getRelationName() != null)
								{
									Relation[] relations = flattenedSolution.getRelationSequence(vl.getRelationName());
									table = servoyModel.getDataSourceManager().getDataSource(relations[relations.length - 1].getForeignDataSource());
								}
								else
								{
									table = servoyModel.getDataSourceManager().getDataSource(vl.getDataSource());
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
										dataProvider = flattenedSolution.getDataProviderForTable(table, dp);
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
									if (dataProvider != null)
									{
										type = dataProvider.getDataProviderType();
										break;
									}
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
