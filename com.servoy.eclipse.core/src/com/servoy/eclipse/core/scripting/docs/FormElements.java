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
package com.servoy.eclipse.core.scripting.docs;

import com.servoy.j2db.documentation.ServoyDocumented;

@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "elements", scriptingName = "elements")
public class FormElements
{
	/**
	 * Get the names of all elements of the form, as an array.
	 * 
	 * @sample
	 * for (var i=0; i<%%prefix%%elements.allnames.length; i++)
	 * {
	 * 	var name = %%prefix%%elements.allnames[i];
	 * 	var elem = %%prefix%%elements[name];
	 * 	application.output(name + ": " + elem.getDataProviderID());	
	 * }
	 * 
	 * @special
	 */
	public Array allnames;

	/**
	 * Get the number of elements of the form.
	 * 
	 * @sample
	 * for (var i=0; i<%%prefix%%elements.length; i++)
	 * {
	 * 	var elem = %%prefix%%elements[i];
	 * 	application.output(elem.getName() + ": " + elem.getDataProviderID());
	 * }
	 */
	public int length;

	/**
	 * Get an element of the form by its name.
	 * 
	 * @sampleas allnames
	 */
	public String index_name;

	/**
	 * Get an element of the form by its index.
	 * 
	 * @sampleas length
	 */
	public Number index_index;
}
