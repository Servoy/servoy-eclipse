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
package com.servoy.eclipse.dnd;

import java.awt.Dimension;

import org.eclipse.core.runtime.IAdapterFactory;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.DatabaseUtils;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.dnd.FormElementDragData.DataProviderDragData;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.util.ImageLoader;

/**
 * Factory for adapters for the IDragData.
 */
public class DragDataAdapterFactory implements IAdapterFactory
{
	private static Class[] ADAPTERS = new Class[] { IDragData.class };

	public Class[] getAdapterList()
	{
		return ADAPTERS;
	}

	public Object getAdapter(Object obj, Class key)
	{
		if (key == IDragData.class)
		{
			if (obj instanceof ScriptVariable)
			{
				ScriptVariable sv = (ScriptVariable)obj;
				if (sv.getParent() instanceof Form)
				{
					Form form = (Form)sv.getParent();
					return new DataProviderDragData(form.getTableName(), form.getServerName(), sv.getDataProviderID(), form.getTableName(), null);
				}
				return new DataProviderDragData(null, null, sv.getDataProviderID(), null, null);
			}

			if (obj instanceof IPersist && !(obj instanceof Solution) && !(obj instanceof Relation))
			{
				IPersist persist = (IPersist)obj;
				int width = 80, height = 20;
				Dimension size = null;
				if (persist instanceof Template)
				{
					size = ElementFactory.getTemplateBoundsize(new TemplateElementHolder((Template)persist));
				}
				else if (persist instanceof Form)
				{
					size = ElementFactory.calculateFormSize(Activator.getDefault().getDesignClient(), (Form)persist);
				}
				else if (persist instanceof Media)
				{
					size = ImageLoader.getSize(((Media)persist).getMediaData());
				}
				if (size != null && size.width > 0 && size.height > 0)
				{
					width = size.width;
					height = size.height;
				}
				return new PersistDragData(persist.getRootObject().getName(), persist.getUUID(), persist.getTypeID(), width, height, -1);
			}

			if (obj instanceof TemplateElementHolder)
			{
				TemplateElementHolder templateHolder = (TemplateElementHolder)obj;
				int width = 80, height = 20;
				Dimension size = ElementFactory.getTemplateBoundsize(templateHolder);
				if (size != null)
				{
					width = size.width;
					height = size.height;
				}
				return new PersistDragData(templateHolder.template.getRootObject().getName(), templateHolder.template.getUUID(),
					templateHolder.template.getTypeID(), width, height, templateHolder.element);
			}

			if (obj instanceof IDataProvider)
			{
				ColumnWrapper cw = ((IDataProvider)obj).getColumnWrapper();
				IColumn column = cw.getColumn();
				String primaryTableName = null;
				String relationName = null;
				try
				{
					if (cw.getRelations() == null)
					{
						primaryTableName = column.getTable().getName();
					}
					else
					{
						primaryTableName = cw.getRelations()[0].getPrimaryTableName();
						relationName = DatabaseUtils.getRelationsString(cw.getRelations());
					}

					return new DataProviderDragData(column.getTable().getName(), column.getTable().getServerName(), column.getDataProviderID(),
						primaryTableName, relationName);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		return null;
	}
}
