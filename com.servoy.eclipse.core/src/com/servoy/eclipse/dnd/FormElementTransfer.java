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
package com.servoy.eclipse.dnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import com.servoy.eclipse.model.util.ServoyLog;


/**
 * @author jcompagner
 * 
 */
public class FormElementTransfer extends ByteArrayTransfer
{

	/**
	 * Singleton instance.
	 */
	private static final FormElementTransfer fInstance = new FormElementTransfer();

	// Create a unique ID to make sure that different Eclipse
	// applications use different "types" of <code>FormElementTransfer</code>
	private static final String TYPE_NAME = "formelement-transfer-format:" + System.currentTimeMillis() + ":" + fInstance.hashCode();

	private static final int TYPEID = registerType(TYPE_NAME);

	private FormElementTransfer()
	{
	}

	/**
	 * Returns the singleton instance.
	 * 
	 * @return the singleton instance
	 */
	public static FormElementTransfer getInstance()
	{
		return fInstance;
	}

	/**
	 * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
	 */
	@Override
	protected int[] getTypeIds()
	{
		return new int[] { TYPEID };
	}

	/**
	 * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
	 */
	@Override
	protected String[] getTypeNames()
	{
		return new String[] { TYPE_NAME };
	}

	/**
	 * @see org.eclipse.swt.dnd.ByteArrayTransfer#javaToNative(java.lang.Object, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	protected void javaToNative(Object object, TransferData transferData)
	{
		ByteArrayOutputStream bao = new ByteArrayOutputStream(256);
		try
		{
			boolean ok = true;

			if (object instanceof Object[] && ((Object[])object).length > 0)
			{
				ObjectOutputStream oos = new ObjectOutputStream(bao);
				oos.writeObject(FormElementDragData.DND_PREFIX);
				oos.writeInt(((Object[])object).length);

				for (Object o : ((Object[])object))
				{
					if (o instanceof FormElementDragData)
					{
						oos.writeObject(o);
					}
					else
					{
						ok = false;
						break;
					}
				}
				oos.close();
			}
			else
			{
				ok = false;
			}
			if (ok)
			{
				super.javaToNative(bao.toByteArray(), transferData);
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * @see org.eclipse.swt.dnd.ByteArrayTransfer#nativeToJava(org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	protected Object nativeToJava(TransferData transferData)
	{
		byte[] array = (byte[])super.nativeToJava(transferData);

		if (array == null)
		{
			return null;
		}

		ArrayList<Object> result = new ArrayList<Object>();
		ByteArrayInputStream bais = new ByteArrayInputStream(array);
		ObjectInputStream ois = null;
		try
		{
			ois = new ObjectInputStream(bais);
			if (!FormElementDragData.DND_PREFIX.equals(ois.readObject()))
			{
				return null;
			}
			int length = ois.readInt();
			Object o;
			for (int i = 0; i < length; i++)
			{
				o = ois.readObject();
				if (!(o instanceof FormElementDragData))
				{
					return null;
				}
				result.add(o);
			}
			return result.size() > 0 ? result.toArray(new Object[result.size()]) : null;
		}
		catch (IOException ex)
		{
			ServoyLog.logError(ex);
		}
		catch (ClassNotFoundException e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			try
			{
				if (ois != null) ois.close();
			}
			catch (IOException e)
			{
			}
		}
		return null;
	}
}
