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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarkdownOutputAnalyzer
{

	private static final Path duplicatesFile = Paths.get(System.getProperty("user.dir"), "duplicates.txt");
	private static final Path filesToDeleteFile = Paths.get(System.getProperty("user.dir"), "filesToDelete.txt");
	private static Path csvDuplicatesFile = Paths.get(System.getProperty("user.dir"), "duplicates.csv");

	public static void analyze() throws IOException
	{

		if (Files.exists(filesToDeleteFile))
		{
			Files.delete(filesToDeleteFile);
			while (Files.exists(filesToDeleteFile))
			{
				try
				{
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while waiting for file deletion", e);
				}
			}
		}

		Map<String, Integer> keyCounts = new HashMap<>();
		List<String> fileLines = Files.readAllLines(duplicatesFile, StandardCharsets.UTF_8);
		Set<String> uniqueLines = new HashSet<>(); // Track unique lines


		for (String line : fileLines)
		{
			if (line.isEmpty() || !line.contains("="))
			{
				continue;
			}

			if (uniqueLines.contains(line))
			{
				continue;
			}
			if (line.endsWith("README.md"))
			{
				continue;
			}

			String key = line.split("=", 2)[0].trim();

			keyCounts.put(key, keyCounts.getOrDefault(key, 0) + 1);
		}

		List<String> duplicates = new ArrayList<>();
		for (String line : fileLines)
		{
			if (line.isEmpty() || !line.contains("="))
			{
				continue;
			}
			if (line.endsWith("README.md"))
			{
				continue;
			}

			String key = line.split("=", 2)[0].trim();
			if (keyCounts.get(key) > 1)
			{
				duplicates.add(line);
			}
		}

		try (BufferedWriter writer = Files.newBufferedWriter(filesToDeleteFile, StandardCharsets.UTF_8))
		{

			duplicates.sort((line1, line2) -> {
				String key1 = line1.split("=", 2)[0].trim();
				String key2 = line2.split("=", 2)[0].trim();
				return key1.compareTo(key2);
			});

			writer.write("# DELETE: COMMENTS AFTER MANUAL PROCESSING\n");
			writer.write("# DELETE: KEEP THE LINES of which files you want to delete\n");
			writer.write("# DELETE: Rename the file to: filesToDelete_processed.txt\n");

			// Write each duplicate line
			for (String duplicate : duplicates)
			{
				writer.write(duplicate);
				writer.newLine();
			}
		}

		try (BufferedWriter csvWriter = Files.newBufferedWriter(csvDuplicatesFile, StandardCharsets.UTF_8))
		{
			// Write the CSV header
			csvWriter.write("Key,FileSize,RelativePath\n");

			for (String duplicate : duplicates)
			{
				// Extract the key and the full path
				String[] parts = duplicate.split("=", 2);
				String key = parts[0].trim();
				String fullPath = parts[1].trim();

				// Get the file size
				Path filePath = Paths.get(fullPath);
				long fileSize = Files.exists(filePath) ? Files.size(filePath) : 0;

				// Extract the relative path after "ng_generated/reference"
				String relativePath = "";
				int index = fullPath.indexOf("ng_generated/reference");
				if (index != -1)
				{
					relativePath = fullPath.substring(index + "ng_generated/reference".length())
						.replace("/", " ")
						.trim();
				}

				// Write the row to the CSV
				csvWriter.write(String.format("%s,%d,%s\n", key, fileSize, relativePath));
			}
		}

		// Count the number of keys with duplicates
		long duplicatedKeyCount = keyCounts.entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.count();

		// Display the count of duplicated keys
		System.out.println(duplicatedKeyCount + " duplicated keys have been found.");
		System.out.println("Analysis result: " + duplicatedKeyCount + " duplicate key found. Output has been written to: " + filesToDeleteFile.toString());
	}
}
