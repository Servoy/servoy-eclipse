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
package com.servoy.eclipse.ui.labelproviders;

import java.util.List;

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.MethodWithArguments.UnresolvedMethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.IDeprecationProvider;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportDeprecatedAnnotation;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Label provider for methods.
 *
 * @author rgansevles
 */

public class MethodLabelProvider extends LabelProvider implements IFontProvider, IPersistLabelProvider, IDeprecationProvider
{
	private final PersistContext persistContext;
	private final boolean showPrefix;
	private final boolean showNoneForDefault;
	private final boolean defaultAsText;

	public MethodLabelProvider(PersistContext persistContext, boolean showPrefix, boolean showNoneForDefault)
	{
		this(persistContext, showPrefix, showNoneForDefault, false);
	}

	public MethodLabelProvider(PersistContext persistContext, boolean showPrefix, boolean showNoneForDefault, boolean defaultAsText)
	{
		this.showPrefix = showPrefix;
		this.showNoneForDefault = showNoneForDefault;
		this.defaultAsText = defaultAsText;
		if (persistContext == null) throw new NullPointerException();
		this.persistContext = persistContext;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null) return Messages.LabelNone;
		Object val = value;
		if (val instanceof ComplexProperty)
		{
			val = ((ComplexProperty)val).getValue();
		}
		if (val instanceof MethodWithArguments)
		{
			return getMethodText((MethodWithArguments)val, persistContext, showPrefix, showNoneForDefault, defaultAsText);
		}
		return val.toString();
	}

	public static String getMethodText(MethodWithArguments mwa, PersistContext persistContext, boolean showPrefix, boolean showNoneForDefault,
		boolean defaultAsText)
	{
		if (MethodWithArguments.METHOD_DEFAULT.equals(mwa))
		{
			return showNoneForDefault ? Messages.LabelNone : defaultAsText ? Messages.LabelDefaultAsText : Messages.LabelDefault;
		}

		if (MethodWithArguments.METHOD_NONE.equals(mwa))
		{
			return Messages.LabelNone;
		}

		if (mwa instanceof UnresolvedMethodWithArguments)
		{
			return UnresolvedValue.getUnresolvedMessage(((UnresolvedMethodWithArguments)mwa).unresolvedValue);
		}

		IScriptProvider sm = getScriptProvider(mwa, persistContext);
		if (sm == null)
		{
			return Messages.LabelUnresolved;
		}
		else
		{
			if (persistContext.getContext() instanceof Form && sm.getParent() instanceof Form)
			{
				List<Form> hierarchy = ServoyModelFinder.getServoyModel().getFlattenedSolution().getFormHierarchy((Form)persistContext.getContext());
				if (!hierarchy.contains(sm.getParent()))
				{
					return Messages.LabelUnresolved;
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		if (showPrefix && sm.getParent() instanceof Solution)
		{
			// global method
			sb.append(ScopesUtils.getScopeString(sm));
		}
		else
		{
			sb.append(sm.getName());
		}

		if (sm instanceof AbstractBase)
		{
			MethodArgument[] formalArguments = ((AbstractBase)sm).getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
			if (formalArguments != null && formalArguments.length > 0)
			{
				sb.append('(');
				for (int i = 0; i < formalArguments.length; i++)
				{
					if (i > 0) sb.append(',');
					sb.append(formalArguments[i].getName());
				}
				sb.append(')');
			}
		}

		return sb.toString();
	}

	public static IScriptProvider getScriptProvider(MethodWithArguments mwa, PersistContext persistContext)
	{
		ITable table = mwa.table;
		if (table == null)
		{
			Form form = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
			if (form != null)
			{
				table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(form.getDataSource());
			}
		}
		return ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(), table, mwa.methodUUID);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.util.IDeprecationProvider#isDeprecated(java.lang.Object)
	 */
	@Override
	public Boolean isDeprecated(Object element)
	{
		Object val = element;
		if (val instanceof ComplexProperty)
		{
			val = ((ComplexProperty)val).getValue();
		}
		if (val instanceof MethodWithArguments)
		{
			IScriptProvider sm = getScriptProvider((MethodWithArguments)val, persistContext);
			return Boolean.valueOf(sm instanceof ISupportDeprecatedAnnotation && ((ISupportDeprecatedAnnotation)sm).isDeprecated());
		}

		return null;
	}

	public Font getFont(Object value)
	{
		if (MethodWithArguments.METHOD_DEFAULT.equals(value) || MethodWithArguments.METHOD_NONE.equals(value))
		{
			return FontResource.getDefaultFont(SWT.BOLD, -1);
		}
		return FontResource.getDefaultFont(SWT.NONE, 0);
	}

	public IPersist getPersist(Object value)
	{
		Object val = value;
		if (val instanceof ComplexProperty)
		{
			val = ((ComplexProperty)val).getValue();
		}
		if (val instanceof IPersist)
		{
			return (IPersist)val;
		}
		if (val instanceof MethodWithArguments)
		{
			return ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(), ((MethodWithArguments)val).table,
				((MethodWithArguments)val).methodUUID);
		}
		return null;
	}
}
