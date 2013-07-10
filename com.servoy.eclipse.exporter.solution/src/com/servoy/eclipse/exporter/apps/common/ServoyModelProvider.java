/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.exporter.apps.common;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.extensions.IServoyEnvironmentProvider;
import com.servoy.j2db.IServiceProvider;

public class ServoyModelProvider implements IServoyEnvironmentProvider
{

	private static ExportServoyModel model;

	public ExportServoyModel getServoyModel()
	{
		return getModel();
	}

	public static ExportServoyModel getModel()
	{
		if (model == null)
		{
			model = new ExportServoyModel();
		}
		return model;
	}

	public IServiceProvider getServiceProvider()
	{
		return Activator.getDefault().getDesignClient();
	}

}
