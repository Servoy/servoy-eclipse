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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.core.elements.IPlaceDataProviderConfiguration;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Pair;

/**
 * Command to place a field in the form designer.
 *
 * @author rgansevles
 */

public class FormPlaceFieldCommand extends BaseFormPlaceElementCommand
{
	private final IFieldPositioner fieldPositioner;
	private final IPersist formContext;
	private final IPlaceDataProviderConfiguration config;

	/**
	 * @param application
	 * @param form
	 * @param form2
	 * @param fieldPositioner2
	 * @param object
	 * @param form3
	 * @param dataFieldRequest
	 */
	public FormPlaceFieldCommand(IApplication application, ISupportChilds parent, Object requestType, Map<Object, Object> extendedData, IPersist formContext,
		IFieldPositioner fieldPositioner, Point defaultLocation, org.eclipse.draw2d.geometry.Dimension size, IPersist context,
		IPlaceDataProviderConfiguration config)
	{
		super(application, parent, config.getDataProvidersConfig(), requestType, extendedData, fieldPositioner, defaultLocation, size, context);
		this.formContext = formContext;
		this.fieldPositioner = fieldPositioner;
		this.config = config;
	}

	@Override
	protected IPersist[] placeElements(Point location) throws RepositoryException
	{
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null &&
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() instanceof BaseVisualFormEditor)
		{
			BaseVisualFormEditor editor = (BaseVisualFormEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			editor.setRenderGhosts(true);
		}
		if (parent instanceof ISupportFormElements)
		{
			List<Pair<IDataProvider, Object>> lst = null;
			if (object instanceof List< ? >)
			{
				lst = (List<Pair<IDataProvider, Object>>)object;
			}
			else if (object instanceof Object[])
			{
				lst = new ArrayList<>();
				for (Object dp : (Object[])object)
				{
					lst.add(new Pair<IDataProvider, Object>((IDataProvider)dp, null));
				}
			}
			setLabel("place field(s)");
			IPersist[] elements = ElementFactory.createFields((ISupportFormElements)parent, config, fieldPositioner, location);
			if (parent instanceof Portal)
			{
				// if all elements are from 1 relation, correct the portal
				Iterator<Field> fields = ((Portal)parent).getFields();
				String relationName = null;
				boolean same = true;
				while (same && fields.hasNext())
				{
					Field field = fields.next();
					if (field.getDataProviderID() != null && field.getDataProviderID().indexOf('.') > 0)
					{
						String relName = field.getDataProviderID().substring(0, field.getDataProviderID().lastIndexOf('.'));
						if (relationName == null)
						{
							relationName = relName;
						}
						else
						{
							same = relationName.equals(relName);
						}
					}
				}
				if (same && relationName != null)
				{
					// don't set the relation name directly, use PersistPropertySource in case the portal is from a superform.
					PersistPropertySource.createPersistPropertySource(parent, formContext, false).setPersistPropertyValue(
						StaticContentSpecLoader.PROPERTY_RELATIONNAME.getPropertyName(), relationName);
				}
			}
			return elements;
		}
		return null;
	}
}
