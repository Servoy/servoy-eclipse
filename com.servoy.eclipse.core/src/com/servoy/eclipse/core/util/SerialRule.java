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
package com.servoy.eclipse.core.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Rule allows only one job to run at a time
 */
public class SerialRule implements ISchedulingRule
{
	public static final SerialRule INSTANCE = new SerialRule(0);
	private final int id;

	/**
	 * Serial rules with the same id conflict. INSTANCE has id 0.
	 */
	public SerialRule(int id)
	{
		this.id = id;
	}

	public boolean contains(ISchedulingRule rule)
	{
		return (rule == this) || (rule instanceof IResource);
	}

	public boolean isConflicting(ISchedulingRule rule)
	{
		return rule instanceof SerialRule && ((SerialRule)rule).id == id;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof SerialRule && ((SerialRule)obj).id == id;
	}

}
