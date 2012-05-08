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

package com.servoy.eclipse.ui.editors.relation;

import com.servoy.j2db.persistence.LiteralDataprovider;

/**
 * @author jcompagner
 *
 */
public class RelationRow
{

	private String ci_from;
	private Integer operator;
	private String ci_to;

	/**
	 * @param ci_from
	 * @param operator
	 * @param ci_to
	 * @param object
	 */
	public RelationRow(String ci_from, Integer operator, String ci_to, Object object)
	{
		this.ci_from = ci_from;
		this.operator = operator;
		this.ci_to = ci_to;
	}

	/**
	 * @param dataProvidersIndex
	 */
	public void setCIFrom(String dataProvider)
	{
		ci_from = dataProvider;
	}

	/**
	 * @param dataProvidersIndex
	 */
	public void setCITo(String dataProvider)
	{
		ci_to = dataProvider;
	}

	/**
	 * @return
	 */
	public String getCIFrom()
	{
		if (ci_from != null && ci_from.startsWith(LiteralDataprovider.LITERAL_PREFIX))
		{
			return ci_from.substring(LiteralDataprovider.LITERAL_PREFIX.length());
		}
		return ci_from;
	}

	/**
	 * @return
	 */
	public String getCITo()
	{
		return ci_to;
	}

	/**
	 * @return
	 */
	public Integer getOperator()
	{
		return operator;
	}

	/**
	 * @param valueOf
	 */
	public void setOperator(Integer op)
	{
		operator = op;
	}

	public String getRawCIFrom()
	{
		return ci_from;
	}

}
