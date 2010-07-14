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
package com.servoy.eclipse.core.builder;


/**
 * Objects of this type are able to hold information related to entities that have some problems. For example you could hold JSONException objects (E) for File
 * objects (T).
 * 
 * @author acostescu
 * 
 * @param <T> the type of the entities that have problems.
 * @param <E> the type of the problems they have.
 */
public interface ErrorKeeper<T, E>
{

	/**
	 * Registers the given error to the badObject.
	 * 
	 * @param badObject the object that has the error.
	 * @param error the error.
	 */
	void addError(T badObject, E error);

	/**
	 * Removes the given object from the list of objects with errors.
	 * 
	 * @param badObject the object that is no longer bad.
	 */
	void removeError(T badObject);

}
