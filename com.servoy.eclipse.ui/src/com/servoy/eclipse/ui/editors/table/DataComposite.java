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
package com.servoy.eclipse.ui.editors.table;

import net.sourceforge.sqlexplorer.dataset.DataSet;
import net.sourceforge.sqlexplorer.dbdetail.tab.AbstractDataSetTab;
import net.sourceforge.sqlexplorer.dbproduct.Alias;
import net.sourceforge.sqlexplorer.dbproduct.Session;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.servoy.j2db.persistence.Table;

/**
 * @author jcompagner
 * 
 */
public class DataComposite extends Composite
{
	private final Table table;
	private AbstractDataSetTab results;

	/**
	 * @param parent
	 * @param table
	 * @param style
	 */
	public DataComposite(Composite parent, Table table)
	{
		super(parent, SWT.None);
		this.table = table;
	}

	public void show()
	{
		if (getChildren().length == 0)
		{
			setLayout(new FillLayout(SWT.VERTICAL));
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			results = new AbstractDataSetTab()
			{

				@Override
				public String getLabelText()
				{
					return "select from " + table.getName(); //$NON-NLS-1$
				}

				@Override
				public String getStatusMessage()
				{
					return "select from " + table.getName(); //$NON-NLS-1$
				}

				@Override
				public DataSet getDataSet() throws Exception
				{
					String servername = table.getServerName();
					Alias alias = SQLExplorerPlugin.getDefault().getAliasManager().getAlias(servername);
					Session session = alias.getDefaultUser().createSession();
					try
					{
						return new DataSet(null, "select * from " + table.getSQLName(), null, session, 1000); //$NON-NLS-1$
					}
					finally
					{
						session.close();
					}
				}
			};
			results.fillComposite(this);
			layout();
		}
		else
		{
			results.refresh();
			layout();
		}
	}
}
