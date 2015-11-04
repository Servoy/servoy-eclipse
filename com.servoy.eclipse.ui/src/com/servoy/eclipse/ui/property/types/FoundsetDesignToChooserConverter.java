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

package com.servoy.eclipse.ui.property.types;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.FormFoundsetEntryContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.property.DatasourcePropertyConverter;
import com.servoy.eclipse.ui.property.RelationNameConverter;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyTypeConfig;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * As the foundset main property editor actually just chooses the foundset, not also the dataproviders we leave it to this class
 * to convert json design values to what the chooser needs and the other way around.
 *
 * @author acostescu
 */
public class FoundsetDesignToChooserConverter
{

	protected final RelationNameConverter relationNameConverter;
	protected final DatasourcePropertyConverter datasourcePropertyConverter;

	public FoundsetDesignToChooserConverter(FlattenedSolution flattenedSolution)
	{
		relationNameConverter = new RelationNameConverter(flattenedSolution);
		datasourcePropertyConverter = new DatasourcePropertyConverter();
	}

	public Object convertJSONValueToChooserValue(Object value)
	{
		Object valueForChooser;

		if (value instanceof JSONObject)
		{
			String selectorString = ((JSONObject)value).optString(FoundsetPropertyType.FOUNDSET_SELECTOR, null);
			if (selectorString == null) valueForChooser = null;
			else if ("".equals(selectorString)) valueForChooser = FormFoundsetEntryContentProvider.FORM_FOUNDSET;
			else
			{
				// either a datasource or a relation
				valueForChooser = datasourcePropertyConverter.convertProperty(null, selectorString);
				if (valueForChooser == null) valueForChooser = relationNameConverter.convertProperty(null, selectorString);
			}
		}
		else valueForChooser = value;
		return valueForChooser;
	}

	public JSONObject convertFromChooserValueToJSONValue(Object value, JSONObject oldValue)
	{
		JSONObject jsonValue;

		if (value == null || value instanceof JSONObject) jsonValue = (JSONObject)value;
		else
		{
			if (value == CombinedTreeContentProvider.NONE) jsonValue = null;
			else
			{
				String oldFoundsetSelector, newFoundsetSelector, oldLoadAllRecordsForSeparate;
				if (oldValue != null)
				{
					oldFoundsetSelector = oldValue.optString(FoundsetPropertyType.FOUNDSET_SELECTOR, null);
					oldLoadAllRecordsForSeparate = oldValue.optString(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE, null);
				}
				else
				{
					oldFoundsetSelector = null;
					oldLoadAllRecordsForSeparate = null;
				}

				if (value instanceof RelationsWrapper) newFoundsetSelector = relationNameConverter.convertValue(null, value);
				else if (value instanceof TableWrapper)
				{
					newFoundsetSelector = datasourcePropertyConverter.convertValue(null, (TableWrapper)value);
				}
				else if (value == FormFoundsetEntryContentProvider.FORM_FOUNDSET) newFoundsetSelector = "";
				else if (value instanceof UnresolvedValue) newFoundsetSelector = null;
				else
				{
					ServoyLog.logError("Foundset property type picker returned a wrong value (" + value + ")?", new RuntimeException("Stack trace:"));
					newFoundsetSelector = null;
				}

				if (newFoundsetSelector != null)
				{
					// set new design value with new selector; keep dataproviders only if selector is the same as before... // TODO optimise this to keep dataproviders list if both before and after selectors point to the same table, even if one if a relation or form foundset for example and the other is a separate foundset
					jsonValue = new ServoyJSONObject(false, false);
					try
					{
						jsonValue.put(FoundsetPropertyType.FOUNDSET_SELECTOR, newFoundsetSelector);
						if (value instanceof TableWrapper)
						{
							if (oldLoadAllRecordsForSeparate == null) jsonValue.put(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE, true); // by default load all records initially for separate foundsets
							else jsonValue.put(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE, oldLoadAllRecordsForSeparate);
						}

						if (oldValue != null)
						{
							if (oldValue.has(FoundsetPropertyTypeConfig.DATAPROVIDERS) && Utils.equalObjects(oldFoundsetSelector, newFoundsetSelector)) jsonValue.put(
								FoundsetPropertyTypeConfig.DATAPROVIDERS, oldValue.get(FoundsetPropertyTypeConfig.DATAPROVIDERS));
						}
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
						jsonValue = oldValue; // if an error happened. don't change anything
					}
				}
				else jsonValue = oldValue; // if we don't know what the user selected, don't change anything (this shouldn't happen)
			}
		}
		return jsonValue;
	}

}
