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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;


/**
 * Content provider for solution names.
 *
 * @author rgansevles
 *
 */
public class SolutionContentProvider extends FlatTreeContentProvider
{
	public static final SolutionContentProvider SOLUTION_CONTENT_PROVIDER = new SolutionContentProvider();

	@Override
	public Object[] getElements(Object inputElement)
	{

		if (inputElement instanceof SolutionListOptions)
		{
			SolutionListOptions options = (SolutionListOptions)inputElement;

			List<String> solutionNames = new ArrayList<String>();
			if (options.includeNone)
			{
				solutionNames.add("");
			}
			RootObjectMetaData[] solutionMetaDatas;
			try
			{
				solutionMetaDatas = ApplicationServerRegistry.get().getDeveloperRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
				for (RootObjectMetaData element : solutionMetaDatas)
				{
					if ((((SolutionMetaData)element).getSolutionType() & options.solutionTypeFilter) != 0)
					{
						solutionNames.add(element.getName());
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}

			return solutionNames.toArray();
		}

		return super.getElements(inputElement);
	}

	public static class SolutionListOptions
	{
		public final int solutionTypeFilter;
		private final boolean includeNone;

		public SolutionListOptions(int solutionTypeFilter, boolean includeNone)
		{
			this.solutionTypeFilter = solutionTypeFilter;
			this.includeNone = includeNone;
		}
	}

}
