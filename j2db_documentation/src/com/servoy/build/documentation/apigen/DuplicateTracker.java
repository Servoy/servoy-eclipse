/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.build.documentation.apigen;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

public class DuplicateTracker
{

	private static final DuplicateTracker instance = new DuplicateTracker();
	private Path duplicatesFile;
	private final ReentrantLock lock = new ReentrantLock();
	private final String filterIn = "ng_generated";

	private DuplicateTracker()
	{
	}

	public static DuplicateTracker getInstance()
	{
		return instance;
	}

	protected void init()
	{
		// Initialize the duplicates.txt file in the user directory
		this.duplicatesFile = Paths.get(System.getProperty("user.dir"), "duplicates.txt");
		try (FileWriter writer = new FileWriter(duplicatesFile.toFile(), StandardCharsets.UTF_8, false))
		{
			// Initialize file (clear any existing content)
			writer.write("");
		}
		catch (IOException e)
		{
			System.err.println("Failed to initialize duplicates.txt: " + e.getMessage());
		}
	}


	public void trackFile(String key, String path)
	{
		if (!path.contains(filterIn)) return;
		lock.lock();
		try
		{
			try (FileWriter writer = new FileWriter(duplicatesFile.toFile(), StandardCharsets.UTF_8, true)) //append mode
			{
				writer.write(key + "=" + path + "\n");

			}
		}
		catch (IOException e)
		{
			System.err.println("Failed to write to duplicates.txt: " + e.getMessage());
		}
		finally
		{
			lock.unlock();
		}
	}
}
