/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.model.extensions;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.javascript.ast.Script;

import com.servoy.eclipse.model.Activator;

/**
 *
 * @author emera
 */
public interface ICalculationTypeInferencerProvider
{
	static final String EXTENSION_ID = Activator.PLUGIN_ID + ".typeInferencer";

	public ICalculationTypeInferencer parse(Script script, final IFile resource);
}
