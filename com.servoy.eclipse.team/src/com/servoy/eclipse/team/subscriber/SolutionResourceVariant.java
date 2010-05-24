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
package com.servoy.eclipse.team.subscriber;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.CachedResourceVariant;

import com.servoy.eclipse.team.Activator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class SolutionResourceVariant extends CachedResourceVariant
{
	private final File variantFile;
	private byte[] bytes;

	public SolutionResourceVariant(File file)
	{
		this.variantFile = file;
	}

	public SolutionResourceVariant(File file, byte[] bytes)
	{
		this.variantFile = file;
		this.bytes = bytes;
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException
	{
		setContents(getContents(), monitor);
	}

	@Override
	protected String getCacheId()
	{
		return Activator.PLUGIN_ID;
	}

	@Override
	protected String getCachePath()
	{
		return variantFile.getAbsolutePath();
	}

	public byte[] asBytes()
	{
		if (bytes == null)
		{
			if (isContainer())
			{
				bytes = getName().getBytes();
			}
			else
			{
				ByteArrayOutputStream bos = null;
				BufferedInputStream bis = null;
				byte[] buffer = new byte[1024];
				int len;
				try
				{
					bos = new ByteArrayOutputStream();
					bis = new BufferedInputStream(getContents());

					while ((len = bis.read(buffer)) != -1)
					{
						bos.write(buffer, 0, len);
					}
					bos.flush();
					bytes = bos.toByteArray();
				}
				catch (Exception ex)
				{
					Debug.error("solution variant asBytes", ex);
				}
				finally
				{
					if (bis != null)
					{
						try
						{
							bis.close();
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
					if (bos != null)
					{
						try
						{
							bos.close();
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
				}
			}

			if (bytes != null) bytes = getSynchBytes(bytes);
		}

		return bytes;
	}

	public static byte[] getSynchBytes(byte[] content)
	{
		byte[] synchBytes = null;

		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(content);

			// cannot have '/' in the bytes
			String base64EncHash = Utils.encodeBASE64(hash);
			base64EncHash = base64EncHash.replace('/', '_');

			synchBytes = base64EncHash.getBytes();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}

		return synchBytes;
	}

	public String getContentIdentifier()
	{
		return "";
	}

	public String getName()
	{
		return variantFile.getName();
	}

	public boolean isContainer()
	{
		return variantFile.isDirectory();
	}

	/**
	 * Return the files contained by the file of this resource variant.
	 * 
	 * @return the files contained by the file of this resource variant.
	 */
	public SolutionResourceVariant[] members()
	{
		if (isContainer())
		{
			File[] members = variantFile.listFiles();
			SolutionResourceVariant[] result = new SolutionResourceVariant[members.length];
			for (int i = 0; i < members.length; i++)
			{
				result[i] = new SolutionResourceVariant(members[i]);
			}

			return result;
		}

		return new SolutionResourceVariant[0];
	}

	/**
	 * @return
	 */
	public InputStream getContents() throws TeamException
	{
		try
		{
			InputStream is = variantFile.exists() ? new FileInputStream(variantFile) : new ByteArrayInputStream("".getBytes());

			return new BufferedInputStream(is);
		}
		catch (FileNotFoundException ex)
		{
			throw new TeamException("Failed to fetch contents for " + variantFile, ex);
		}
	}
}
