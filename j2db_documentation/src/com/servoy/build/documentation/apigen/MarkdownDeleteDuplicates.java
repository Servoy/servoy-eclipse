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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MarkdownDeleteDuplicates
{

	private static final String FILES_TO_DELETE = "filesToDelete_processed.txt";
	private static int count;

	public static void deleteDuplicates() throws IOException
	{
		Path filesToDeletePath = Paths.get(System.getProperty("user.dir"), FILES_TO_DELETE);

		// Check if the file exists
		if (!Files.exists(filesToDeletePath))
		{
			System.err.println(System.getenv("user.dir") + File.pathSeparator + "filesToDelete_processed.txt does not exist. Exiting...!");
			return;
		}

		List<String> lines = Files.readAllLines(filesToDeletePath);

		// Check for comments in the file
		boolean hasComments = lines.stream().anyMatch(line -> line.startsWith("# DELETE:"));
		if (hasComments)
		{
			System.err.println("Error: The filesToDelete file still contains comments. Please review and remove them before proceeding.");
			return;
		}

		// Process each line and delete the files
		System.out.println("Deleting files...");
		for (String line : lines)
		{
			line = line.trim();
			if (line.isEmpty()) continue; // Skip empty lines

			// Split key and path
			String[] keyValue = line.split("=", 2);
			if (keyValue.length != 2)
			{
				System.err.println("Invalid entry in filesToDelete: " + line);
				continue;
			}

			String key = keyValue[0].trim();
			String path = keyValue[1].trim();

			// Remove trailing comma if present
			if (path.endsWith(","))
			{
				path = path.substring(0, path.length() - 1).trim();
			}

			// Attempt to delete the file
			Path filePath = Paths.get(path);
			try
			{
				Files.deleteIfExists(filePath);
				System.out.println("Deleted: " + key + ": " + filePath);
				count++;
			}
			catch (IOException e)
			{
				System.err.println("Failed to delete: " + filePath + " - " + e.getMessage());
			}
		}
		System.out.println("File deletion process completed: " + count + "files deleted");
	}
}
