/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.javascript.typeinfo.DefaultMetaType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

/**
 * @author jcompagner
 *
 */
public class ServoyDynamicMetaType extends DefaultMetaType
{
	public static final ServoyDynamicMetaType META_TYPE = new ServoyDynamicMetaType();

	private ServoyDynamicMetaType()
	{
	}

	@Override
	public IRType toRType(IRTypeDeclaration declaration)
	{
		return new ServoyDynamicRuntimeType(declaration);
	}

	@Override
	public IRType toRType(ITypeSystem typeSystem, Type type)
	{
		// a bit hard coded to get the correct type of the record type of this foundset by checking the selected record method.
		if (type.getName().startsWith("JSFoundSet") && type.findDirectMember("getSelectedRecord") != null)
		{
			Type recordType = type.findDirectMember("getSelectedRecord").getDirectType();
			if (recordType != null)
			{
				return new ServoyDynamicArrayRuntimeType(typeSystem, type, recordType.toRType(typeSystem));
			}
		}
		// for a dataset we need to look at the record type that a dataset can have and use that one.
		if (type.getName().startsWith("JSDataSet<"))
		{
			String fullTypeName = type.getName();
			String recordType = fullTypeName.substring("JSDataSet<".length(), fullTypeName.length() - 1);
			Type t = TypeCreator.getRecordType(recordType);
			t.setName(recordType);
			return new ServoyDynamicArrayRuntimeType(typeSystem, type, t.toRType(typeSystem));
		}
		return new ServoyDynamicRuntimeType(typeSystem, type);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.javascript.typeinfo.MetaType#getId()
	 */
	public String getId()
	{
		return "ServoyDynamicType";
	}
}
