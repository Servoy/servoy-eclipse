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


import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * Delegate label provider that adds the form context to the label.
 *
 * @author rgansevles
 *
 */
public class FormContextDelegateLabelProvider extends AbstractPersistContextDelegateLabelProvider
{
	public FormContextDelegateLabelProvider(AbstractPersistContextDelegateLabelProvider labelProvider)
	{
		this(labelProvider, labelProvider.getContext());
	}

	public FormContextDelegateLabelProvider(IPersistLabelProvider labelProvider, IPersist context)
	{
		super(labelProvider, context);
	}

	@Override
	public String getText(Object value)
	{
		String baseText = super.getText(value);
		if (!baseText.equalsIgnoreCase(Messages.LabelUnresolved) && getContext() != null && value != null)
		{
			IPersist persist = getPersist(value);
			if (persist != null)
			{
				Form parentForm = (Form)persist.getAncestor(IRepository.FORMS);
				Form contextForm = (Form)getContext().getAncestor(IRepository.FORMS);
				if (contextForm != null && parentForm != null && !parentForm.getUUID().equals(contextForm.getUUID()))
				{
					return baseText + " [" + parentForm.getName() + ']';
				}
			}
		}

		return baseText;
	}
}
