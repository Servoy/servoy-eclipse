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

package com.servoy.eclipse.ui.quickfix.jsexternalize;

import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemIdentifierFactory;

/**
 * Problem identifier factory for non-externalized strings problem
 * @author gboros
 */
public class JSFileExternalizeProblemFactory implements IProblemIdentifierFactory
{

	/*
	 * @see org.eclipse.dltk.compiler.problem.IProblemIdentifierFactory#valueOf(java.lang.String)
	 */
	public IProblemIdentifier valueOf(String localName) throws IllegalArgumentException
	{
		return JSFileExternalizeProblem.valueOf(localName);
	}

	/*
	 * @see org.eclipse.dltk.compiler.problem.IProblemIdentifierFactory#values()
	 */
	public IProblemIdentifier[] values()
	{
		return JSFileExternalizeProblem.values();
	}
}
