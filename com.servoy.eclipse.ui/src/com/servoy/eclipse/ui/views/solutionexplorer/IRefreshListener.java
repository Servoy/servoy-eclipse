/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer;

/**
 * Classes that are interested in knowing when a SWT tree/list/... refresh is called (probably due to an underlying content change) should implement this interface.
 * Notifications are normally received on the SWT event thread.
 *
 * @author acostescu
 */
public interface IRefreshListener<T>
{

	interface IRefreshEvent<T>
	{
		/**
		 * Returns the node that was be refreshed; if null, then the whole content was refreshed.
		 * @return the node.
		 */
		T getNode();
	}

	class RefreshEvent<T> implements IRefreshEvent<T>
	{
		private final T node;

		public RefreshEvent(T node)
		{
			this.node = node;
		}

		@Override
		public T getNode()
		{
			return node;
		}
	}

	/**
	 * Notifies that a refresh has happened.
	 * @param refreshEvent describes what kind of refresh has happeed.
	 */
	void treeNodeRefreshed(RefreshEvent<T> refreshEvent);

}
