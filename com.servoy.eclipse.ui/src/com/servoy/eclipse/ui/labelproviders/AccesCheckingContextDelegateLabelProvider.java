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


import org.eclipse.osgi.util.NLS;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * Delegate label provider that adds a marker to the text when the value should not be accessible from the context..
 *
 * @author rgansevles
 *
 */
public class AccesCheckingContextDelegateLabelProvider extends AbstractPersistContextDelegateLabelProvider
{
	public AccesCheckingContextDelegateLabelProvider(AbstractPersistContextDelegateLabelProvider labelProvider)
	{
		this(labelProvider, labelProvider.getContext());
	}

	public AccesCheckingContextDelegateLabelProvider(IPersistLabelProvider labelProvider, IPersist context)
	{
		super(labelProvider, context);
	}

	@Override
	public String getText(Object value)
	{
		String baseText = super.getText(value);
		if (!baseText.equalsIgnoreCase(Messages.LabelUnresolved) && value != null && getContext() != null)
		{
			IPersist persist = getPersist(value);
			if (persist instanceof ScriptMethod && persist.getParent() != getContext() && ((ScriptMethod)persist).isPrivate())
			{
				return NLS.bind(Messages.LabelInvalidAccessPrivate, baseText);
			}
		}
		return baseText;
	}
}
