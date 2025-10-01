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
package com.servoy.eclipse.ui.property;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.editors.ScriptProviderCellEditor;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodPropertyController.MethodPropertySource;
import com.servoy.eclipse.ui.property.MethodWithArguments.UnresolvedMethodWithArguments;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Property controller for String properties that are script names, subproperties are instance arguments
 *
 * @author rgansevles
 *
 */
public class ScriptProviderPropertyController extends PropertyController<String, Object>
{
	public static final UnresolvedMethodWithArguments NONE = new UnresolvedMethodWithArguments(null);

	private final PersistContext persistContext;
	private final ITable table;

	public ScriptProviderPropertyController(String id, String displayName, ITable table, PersistContext persistContext)
	{
		super(id, displayName);
		this.table = table;
		this.persistContext = persistContext;
		setLabelProvider(new SolutionContextDelegateLabelProvider(
			new ScriptProviderCellEditor.ScriptDialog.ScriptDialogLabelProvider(persistContext, table, true), persistContext.getContext()));
		setSupportsReadonly(true);
	}

	@Override
	protected IPropertyConverter<String, Object> createConverter()
	{
		return new IPropertyConverter<String, Object>()
		{
			public Object convertProperty(Object id, String value)
			{
				if (value == null || value.length() == 0) return NONE;

				FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());

				// try global method
				IPersist method = flattenedSolution.getScriptMethod(null, value);

				if (method == null && table != null)
				{
					// try calc or foundset method
					Iterator<TableNode> tableNodes = flattenedSolution.getTableNodes(table);
					while (method == null && tableNodes.hasNext())
					{
						method = AbstractBase.selectByName(tableNodes.next().getAllObjects(), value);
					}
				}

				IPersist persist = persistContext.getPersist();
				if (method != null)
				{
					SafeArrayList<Object> args = null;
					if (persist instanceof AbstractBase)
					{
						List<Object> instanceArgs = ((AbstractBase)persist).getFlattenedMethodArguments(id.toString());
						if (instanceArgs != null)
						{
							args = new SafeArrayList<Object>(instanceArgs);
						}
					}
					return new ComplexProperty<MethodWithArguments>(MethodWithArguments.create(method, args))
					{
						@Override
						public IPropertySource getPropertySource()
						{
							return new MethodPropertySource(this, persistContext, table, getId().toString(), isReadOnly());
						}
					};
				}

				return new UnresolvedMethodWithArguments(value);
			}

			public String convertValue(Object id, Object value)
			{
				if (value instanceof UnresolvedMethodWithArguments)
				{
					return ((UnresolvedMethodWithArguments)value).unresolvedValue;
				}

				MethodWithArguments mwa = null;
				if (value instanceof ComplexProperty)
				{
					mwa = ((ComplexProperty<MethodWithArguments>)value).getValue();
				}
				else if (value instanceof MethodWithArguments)
				{
					mwa = (MethodWithArguments)value;
				}
				if (mwa != null)
				{
					MethodPropertyController.setMethodArguments(persistContext.getPersist(), id, mwa.paramNames, mwa.arguments);
					IScriptProvider scriptProvider = ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(), mwa.table,
						mwa.methodUUID);
					if (scriptProvider != null)
					{
						return scriptProvider.getParent() instanceof IRootObject ? ScopesUtils.getScopeString(scriptProvider) : scriptProvider.getDisplayName();
					}
				}
				return null;
			}
		};
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new ScriptProviderCellEditor(parent, table, persistContext, getId().toString(), isReadOnly());
	}
}
