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
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.ScriptProviderCellEditor;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodPropertyController.MethodPropertySource;
import com.servoy.eclipse.ui.property.MethodWithArguments.UnresolvedMethodWithArguments;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.SafeArrayList;

/**
 * Property controller for String properties that are script names, subproperties are instance arguments
 * 
 * @author rgansevles
 * 
 */
public class ScriptProviderPropertyController extends PropertyController<String, Object>
{
	public static final UnresolvedMethodWithArguments NONE = new UnresolvedMethodWithArguments(null);

	private final IPersist persist;
	private final IPersist context;
	private final Table table;

	public ScriptProviderPropertyController(String id, String displayName, Table table, IPersist persist, IPersist context)
	{
		super(id, displayName);
		this.table = table;
		this.context = context;
		this.persist = persist;
		setLabelProvider(new SolutionContextDelegateLabelProvider(new ScriptProviderCellEditor.ScriptDialog.ScriptDialogLabelProvider(persist, context, table,
			true), context));
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

				try
				{
					FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persist, context);
					int methodId = -1;

					// try global method
					String methodName = value.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX) ? value.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length()) : value;
					ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(methodName);
					if (scriptMethod != null)
					{
						methodId = scriptMethod.getID();
					}

					// try calc
					if (methodId == -1 && table != null)
					{
						Iterator<TableNode> tableNodes = flattenedSolution.getTableNodes(table);
						while (methodId == -1 && tableNodes.hasNext())
						{
							ScriptCalculation calc = AbstractBase.selectByName(tableNodes.next().getScriptCalculations().iterator(), value);
							if (calc != null)
							{
								// it is a ScriptCalculation
								methodId = calc.getID();
							}
						}
					}

					if (methodId != -1)
					{
						SafeArrayList<Object> args = null;
						if (persist instanceof AbstractBase)
						{
							List<Object> instanceArgs = ((AbstractBase)persist).getInstanceMethodArguments(id.toString());
							if (instanceArgs != null)
							{
								args = new SafeArrayList<Object>(instanceArgs);
							}
						}
						return new ComplexProperty<MethodWithArguments>(new MethodWithArguments(methodId, args))
						{
							@Override
							public IPropertySource getPropertySource()
							{
								return new MethodPropertySource(this, persist, context, table, getId().toString(), isReadOnly());
							}
						};
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
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
					MethodPropertyController.setInstancMethodArguments(persist, id, mwa.arguments);
					IScriptProvider scriptProvider = ModelUtils.getScriptMethod(persist, context, table, mwa.methodId);
					if (scriptProvider != null)
					{
						return scriptProvider.getParent() instanceof IRootObject ? ScriptVariable.GLOBAL_DOT_PREFIX + scriptProvider.getDisplayName()
							: scriptProvider.getDisplayName();
					}
				}
				return null;
			}
		};
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new ScriptProviderCellEditor(parent, table, persist, context, getId().toString(), isReadOnly());
	}
}
