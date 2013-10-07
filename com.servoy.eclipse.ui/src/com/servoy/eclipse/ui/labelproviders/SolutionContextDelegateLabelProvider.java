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
package com.servoy.eclipse.ui.labelproviders;


import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;

/**
 * Delegate label provider that adds the solution context to the label.
 * 
 * @author rgansevles
 * 
 */
public class SolutionContextDelegateLabelProvider extends AbstractPersistContextDelegateLabelProvider
{
	private final boolean prefixSolutionName;

	public SolutionContextDelegateLabelProvider(AbstractPersistContextDelegateLabelProvider labelProvider)
	{
		this(labelProvider, labelProvider.getContext(), false);
	}

	public SolutionContextDelegateLabelProvider(AbstractPersistContextDelegateLabelProvider labelProvider, boolean prefixSolutionName)
	{
		this(labelProvider, labelProvider.getContext(), prefixSolutionName);
	}

	public SolutionContextDelegateLabelProvider(IPersistLabelProvider labelProvider, IPersist context)
	{
		this(labelProvider, context, false);
	}

	public SolutionContextDelegateLabelProvider(IPersistLabelProvider labelProvider, IPersist context, boolean prefixSolutionName)
	{
		super(labelProvider, context);
		this.prefixSolutionName = prefixSolutionName;
	}

	@Override
	public String getText(Object value)
	{
		String baseText = super.getText(value);
		if (value != null && getContext() != null)
		{
			IPersist persist = getPersist(value);
			if (persist != null)
			{
				Solution contextSolution = (Solution)getContext().getAncestor(IRepository.SOLUTIONS);
				Solution persistSolution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
				if (contextSolution != null && persistSolution != null && !contextSolution.getUUID().equals(persistSolution.getUUID()))
				{
					if (prefixSolutionName)
					{
						return persistSolution.getName() + '.' + baseText;
					}
					return baseText + " [" + persistSolution.getName() + ']'; //$NON-NLS-1$
				}
			}
		}
		return baseText;
	}

	@Override
	public StrikeoutLabelProvider newInstance()
	{
		return new SolutionContextDelegateLabelProvider((IPersistLabelProvider)getLabelProvider(), getContext(), prefixSolutionName);
	}
}
