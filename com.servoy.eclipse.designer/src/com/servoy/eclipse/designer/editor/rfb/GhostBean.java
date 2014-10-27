/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;


/**
 * @author user
 *
 */
public class GhostBean extends Bean
{
	private final Bean parentBean;
	private final String jsonKey;
	private final String typeName;
	private final boolean isArray;
	private int index;

	/**
	 * @param newBean
	 * @param b
	 * @param parent
	 * @param element_id
	 * @param uuid
	 */
	public GhostBean(Bean parentBean, String jsonKey, String typeName, int index, boolean isArray, boolean newBean)
	{
		//we just tell the GhostBean that it has a parent, we do not tell the parent that it contains a GhostBean
		super(parentBean.getParent(), 0, UUID.randomUUID());
		this.parentBean = parentBean;
		this.jsonKey = jsonKey;
		this.typeName = typeName;
		this.isArray = isArray;
		this.index = index;
		try
		{
			if (parentBean.getBeanXML() == null)
			{
				if (isArray) parentBean.setBeanXML("{ \"" + jsonKey + "\":[]}");
				else parentBean.setBeanXML("{ \"" + jsonKey + "\":{}}");
			}
			JSONObject entireModel = new JSONObject(parentBean.getBeanXML());
			if (isArray)
			{
				if (!entireModel.has(jsonKey)) entireModel.put(jsonKey, new JSONArray());
				JSONArray childElements = entireModel.getJSONArray(jsonKey);
				if (newBean)
				{
					this.index = childElements.length();
					childElements.put(new JSONObject());
				}
				setBeanXML(childElements.getString(this.index));
				parentBean.setBeanXML(entireModel.toString());
			}
			else if (!newBean && parentBean.getBeanXML() != null)
			{
				if (entireModel.has(jsonKey))
				{
					setBeanXML(new JSONObject(parentBean.getBeanXML()).getString(jsonKey));
				}
			}
			else
			{
				parentBean.setBeanXML(new JSONObject(parentBean.getBeanXML()).put(jsonKey, new JSONObject()).toString());
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	public Bean getParentBean()
	{
		return parentBean;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.Bean#setBeanXML(java.lang.String)
	 */
	@Override
	public void setBeanXML(String arg)
	{
		super.setBeanXML(arg);
		String beanXML = getBeanXML();
		try
		{
			if (isArray)
			{
				JSONObject entireModel = new JSONObject(parentBean.getBeanXML());
				JSONArray jsonArray = entireModel.getJSONArray(jsonKey);
				jsonArray.put(index, new JSONObject(beanXML));
				parentBean.setBeanXML(entireModel.toString());
			}
			else
			{
				JSONObject entireModel = new JSONObject(parentBean.getBeanXML());
				entireModel.put(jsonKey, beanXML);
				parentBean.setBeanXML(entireModel.toString());
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	/**
	 * @return
	 */
	public String getUUIDString()
	{
		String addIndex = "";
		if (index >= 0) addIndex = "[" + index + "]";
		return parentBean.getUUID() + "_" + jsonKey + addIndex + "_" + typeName;
	}

	public String getTypeName()
	{
		return typeName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractBase#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof GhostBean)
		{
			return ((GhostBean)obj).getUUIDString().equals(this.getUUIDString());
		}
		return super.equals(obj);
	}
}
