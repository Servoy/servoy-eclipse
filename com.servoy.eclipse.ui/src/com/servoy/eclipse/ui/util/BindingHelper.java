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
package com.servoy.eclipse.ui.util;

import java.util.Iterator;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.IChangeListener;

/**
 * Utility class for data binding in editors.
 * 
 * @author jblok
 */

public class BindingHelper
{
	public static DataBindingContext dispose(DataBindingContext bindingContext)
	{
		if (bindingContext != null)
		{
			Object[] bindings = bindingContext.getBindings().toArray();
			if (bindings != null)
			{
				for (Object bind : bindings)
				{
					if (bind instanceof Binding)
					{
						Binding binding = (Binding)bind;
						binding.getTarget().dispose();
						if (binding.getModel() != null)
						{
							binding.getModel().dispose();
						}
					}
				}
			}
			bindingContext.dispose();
		}
		return null;
	}

	public static void addGlobalChangeListener(DataBindingContext bindingContext, IChangeListener listener)
	{
		if (bindingContext != null)
		{
			Iterator it = bindingContext.getBindings().iterator();
			while (it.hasNext())
			{
				Binding binding = (Binding)it.next();
				binding.getTarget().addChangeListener(listener);
			}
		}
	}
}
