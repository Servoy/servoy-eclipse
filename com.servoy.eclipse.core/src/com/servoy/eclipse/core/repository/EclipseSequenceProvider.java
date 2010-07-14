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
package com.servoy.eclipse.core.repository;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.SQLGenerator;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnInfoSequence;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.ISequenceProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Sequence provider that always looks in the db when computing next sequence. Has no ties to the repository db. Only uses column info manager to update column
 * info & the column infos from the Column itself + SQL queries to compute next sequence.
 * 
 * @author acostescu
 */
public class EclipseSequenceProvider implements ISequenceProvider
{

	private final Map<IColumn, ColumnInfoSequence> columnInfoSeqCache;

	private final IColumnInfoManager columnInfoManager;

	/**
	 * Creates a new instance that uses the specified manager to save column info data.
	 * 
	 * @param columnInfoManager manager to save column info data.
	 */
	public EclipseSequenceProvider(IColumnInfoManager columnInfoManager)
	{
		this.columnInfoManager = columnInfoManager;
		columnInfoSeqCache = Collections.synchronizedMap(new HashMap<IColumn, ColumnInfoSequence>());
	}

	public synchronized Object getNextSequence(Column column, boolean update) throws RepositoryException
	{
		if (column == null) throw new RepositoryException("Invalid column"); //$NON-NLS-1$

		String preSequenceChars = null;
		String postSequenceChars = null;
		int seq_cache_size = 1;
		long nextSequence = 1;
		ColumnInfoSequence s = null;

		IDataServer dataServer = ApplicationServerSingleton.get().getDataServer();
		IDataSet rs = null;
		try
		{
			ColumnInfo ci = column.getColumnInfo();
			if (ci != null)
			{
				preSequenceChars = ci.getPreSequenceChars();
				postSequenceChars = ci.getPostSequenceChars();
				int sequenceStepSize = ci.getSequenceStepSize();

				if (ci.getAutoEnterType() != ColumnInfo.SEQUENCE_AUTO_ENTER)
				{
					columnInfoSeqCache.remove(column);
					throw new RepositoryException("Cannot get sequence for column without sequence settings"); //$NON-NLS-1$
				}

				IServerInternal tableServer = (IServerInternal)ServoyModel.getServerManager().getServer(column.getTable().getServerName(), false, true);
				QuerySelect select = SQLGenerator.createAggregateSelect(QueryAggregate.MAX, column.getTable(), column);

				QuerySet querySet = tableServer.getSQLQuerySet(select, null, 0, -1, false);
				String maxSeqSelect = querySet.getSelect().getSql();

				// in case one of the debug clients has a transaction started on this server, exectute the query inside that transaction
				// (otherwise the query will block)

				String clientId = ApplicationServerSingleton.get().getClientId();
				String tid = null;
				IServiceProvider client = J2DBGlobals.getServiceProvider();

				if (client != null)
				{
					// in case switchServer was called tableServer will be new_server because DataServerProxy was called
					String possibleMappedServerName = update && (client.getDataServer() instanceof DataServerProxy)
						? ((DataServerProxy)client.getDataServer()).getReverseMappedServerName(tableServer.getName()) : tableServer.getName();
					tid = client.getFoundSetManager().getTransactionID(possibleMappedServerName);
					clientId = client.getClientID();
				}

				rs = dataServer.performCustomQuery(clientId, column.getTable().getServerName(), column.getTable().getName(), tid, maxSeqSelect, null, 0, -1);
				String val = null;
				if (rs.getRowCount() == 1)
				{
					val = "" + rs.getRow(0)[0];//doing string lookup, can be other thing than long type in table
				}

				if (val != null)
				{
					String last = Utils.findLastNumber(val);
					if (last != null)
					{
						int index = val.indexOf(last);
						if (index > 0)
						{
							nextSequence = Utils.getAsLong(last) + sequenceStepSize;
						}
						else
						{
							nextSequence = Utils.getAsLong(val) + sequenceStepSize;
						}
					}
				}

				if (update)
				{
					s = columnInfoSeqCache.get(column);
					if (s != null)
					{
						long retval = s.getNextVal();
						if (retval == -1 || s.getSequenceStepSize() != sequenceStepSize)
						{
							columnInfoSeqCache.remove(column);
							if (s.getCreationTime() + 9000 > System.currentTimeMillis())//if empty within 9 seconds
							{
								seq_cache_size = (s.getTotalSeqSize() + 2) * 2; //double capacity for next time
							}
							columnInfoSeqCache.put(column, new ColumnInfoSequence(s.getLastReturnedVal() + 2 * sequenceStepSize, sequenceStepSize,
								seq_cache_size - 1, System.currentTimeMillis()));
							retval = s.getLastReturnedVal() + sequenceStepSize;
						}

						if (retval >= nextSequence)
						{
							nextSequence = retval;
						}
						else
						{
							columnInfoSeqCache.put(column, new ColumnInfoSequence(nextSequence + sequenceStepSize, sequenceStepSize, seq_cache_size - 1,
								System.currentTimeMillis()));
						}
					}
					else
					{
						columnInfoSeqCache.put(column, new ColumnInfoSequence(nextSequence + sequenceStepSize, sequenceStepSize, seq_cache_size - 1,
							System.currentTimeMillis()));
					}
				}
			}
			else
			{
				ServoyLog.logError("Cannot calculate next sequence because of null column info - " + column, null);
			}
		}
		catch (ServoyException e)
		{
			ServoyLog.logError(e);
			throw new RepositoryException("error getting next sequence for " + column, e); //$NON-NLS-1$
		}
		catch (RemoteException e)
		{
			ServoyLog.logError(e);
			throw new RepositoryException("error getting next sequence for " + column, e); //$NON-NLS-1$
		}


		//make return value
		boolean b1 = (preSequenceChars == null || preSequenceChars.trim().length() == 0);
		boolean b2 = (postSequenceChars == null || postSequenceChars.trim().length() == 0);

		if (b1 && b2)
		{
			return new Long(nextSequence);
		}
		else
		{
			StringBuffer sb = new StringBuffer();
			if (!b1)
			{
				sb.append(preSequenceChars);
			}
			sb.append(nextSequence);
			if (!b2)
			{
				sb.append(postSequenceChars);
			}
			return sb.toString();
		}
	}

	public Object syncSequence(Column column) throws RepositoryException
	{
		if (column == null) throw new RepositoryException("Can't sync on null column"); //$NON-NLS-1$

		IServerInternal tableServer = (IServerInternal)ServoyModel.getServerManager().getServer(column.getTable().getServerName(), false, true);
		Connection connection = null;
		PreparedStatement ps = null;
		try
		{
			String preSequenceChars = null;
			long newSequence = 0;
			String postSequenceChars = null;

			connection = tableServer.getUnmanagedConnection();

			QuerySelect select = SQLGenerator.createAggregateSelect(QueryAggregate.MAX, column.getTable(), column);
			QuerySet querySet = tableServer.getSQLQuerySet(select, null, 0, -1, false);
			String maxSelect = querySet.getSelect().getSql();


			ps = connection.prepareStatement(maxSelect);
			ResultSet rs = ps.executeQuery();
			String val = null;
			if (rs.next())
			{
				val = rs.getString(1);//doing string lookup, can be other thing than long type in table
			}
			rs.close();
			ps.close();
			ps = null;
			connection.close();
			connection = null;

			// Set maximum to 0 if no maximum value found, i.e., next value is 1.
			if (val == null)
			{
				val = "0"; //$NON-NLS-1$
			}

			String last = Utils.findLastNumber(val);
			if (last != null)
			{
				int index = val.indexOf(last);
				if (index > 0)
				{
					preSequenceChars = val.substring(0, index);
					newSequence = Utils.getAsLong(last) + 1;
					postSequenceChars = val.substring(index + last.length());
				}
				else
				{
					newSequence = Utils.getAsLong(val) + 1;
				}
			}

			if (newSequence > 0)
			{
				ColumnInfo columnInfo = column.getColumnInfo();
				if (columnInfo != null)
				{
					columnInfo.setPreSequenceChars(preSequenceChars);
					columnInfo.setNextSequence(newSequence);
					columnInfo.setPostSequenceChars(postSequenceChars);
					columnInfo.flagChanged();
					columnInfoManager.updateAllColumnInfo(column.getTable());
				}
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("error synchronizing sequence for " + column, e); //$NON-NLS-1$
		}
		finally
		{
			Utils.closeStatement(ps);
			Utils.closeConnection(connection);
		}
		return getNextSequence(column, false);
	}
}
