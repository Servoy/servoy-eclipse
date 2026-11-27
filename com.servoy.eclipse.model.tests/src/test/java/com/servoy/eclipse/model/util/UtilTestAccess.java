/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.model.util;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IBasicWebObject;

/**
 * Test-side bridge to package-private members of com.servoy.eclipse.model.util, so plainjunit tests (which live in
 * com.servoy.eclipse.model.tests.plainjunit.*) need no reflection. This bundle is a fragment of com.servoy.eclipse.model, so this class
 * shares the package and sees its package-private members.
 */
public final class UtilTestAccess
{
	private UtilTestAccess()
	{
	}

	/**
	 * A form component child that is not resolved against its form component (no property description, no flattened element): it works on
	 * the parent's json only.
	 */
	public static WebFormComponentChildType unresolvedFormComponentChild(IBasicWebObject parent, String[] fcPropAndCompPath, FlattenedSolution fs)
	{
		return new WebFormComponentChildType(parent, fcPropAndCompPath, fs, false);
	}
}
