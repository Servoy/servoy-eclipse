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

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import com.servoy.eclipse.core.util.CoreUtils;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.MethodWithArguments.UnresolvedMethodWithArguments;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;

public class MethodLabelProvider extends LabelProvider implements IFontProvider, IPersistLabelProvider
{
	private final IPersist persist;
	private final IPersist context;
	private final boolean showPrefix;
	private final boolean showNoneForDefault;

	public MethodLabelProvider(IPersist persist, IPersist context, boolean showPrefix, boolean showNoneForDefault)
	{
		this.showPrefix = showPrefix;
		this.showNoneForDefault = showNoneForDefault;
		if (persist == null) throw new NullPointerException();
		this.persist = persist;
		this.context = context;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null) return ""; //$NON-NLS-1$
		MethodWithArguments mwa;
		if (value instanceof ComplexProperty)
		{
			mwa = ((ComplexProperty<MethodWithArguments>)value).getValue();
		}
		else
		{
			mwa = (MethodWithArguments)value;
		}
		return getMethodText(mwa, persist, context, null, showPrefix, showNoneForDefault);
	}

	public static String getMethodText(MethodWithArguments mwa, IPersist persist, IPersist context, Table table, boolean showPrefix, boolean showNoneForDefault)
	{
		if (MethodDialog.METHOD_DEFAULT.equals(mwa))
		{
			return showNoneForDefault ? Messages.LabelNone : Messages.LabelDefault;
		}

		if (MethodDialog.METHOD_NONE.equals(mwa))
		{
			return Messages.LabelNone;
		}

		if (mwa instanceof UnresolvedMethodWithArguments)
		{
			return UnresolvedValue.getUnresolvedMessage(((UnresolvedMethodWithArguments)mwa).unresolvedValue);
		}

		IScriptProvider sm = CoreUtils.getScriptMethod(persist, context, table, mwa.methodId);
		if (sm == null)
		{
			return Messages.LabelUnresolved;
		}

		StringBuffer sb = new StringBuffer();
		if (showPrefix && sm.getParent() instanceof Solution)
		{
			// global method
			sb.append(ScriptVariable.GLOBAL_DOT_PREFIX);
		}
		sb.append(sm.getName());

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

	public Font getFont(Object value)
	{
		if (MethodDialog.METHOD_DEFAULT.equals(value) || MethodDialog.METHOD_NONE.equals(value))
		{
			return FontResource.getDefaultFont(SWT.BOLD, -1);
		}
		return FontResource.getDefaultFont(SWT.NONE, 0);
	}

	public IPersist getPersist(Object value)
	{
		MethodWithArguments mwa;
		if (value instanceof ComplexProperty)
		{
			mwa = ((ComplexProperty<MethodWithArguments>)value).getValue();
		}
		else
		{
			mwa = (MethodWithArguments)value;
		}
		return CoreUtils.getScriptMethod(persist, context, null, mwa.methodId);
	}
}
