/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.build.documentation.DocumentationManager;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;

/**
 * @author marianvid
 *
 */
public class ReferencesTracker
{
	private static ReferencesTracker instance;

	private static final Pattern LINK_PATTERN = Pattern.compile(
		"<a\\s+[^>]*href=\\\"([^\\\"]+\\.md(?:#[^\\\"\\s]*)?)\\\"",
		Pattern.CASE_INSENSITIVE);

	// Static counters for tracking link stats
	private static int totalLinksFound = 0;
	private static int totalLinksResolved = 0;
	private static int totalLinksUnresolved = 0;

	// Index for quick summary lookups: file name or full path -> list of summary paths
	private static final Map<String, List<String>> summaryIndex = new HashMap<>();
	// Raw summary paths for fallback matching
	private static Set<String> rawSummaryPaths = new HashSet<>();

	// Collected references and unresolved entries
	private final Map<String, List<Map<String, String>>> resolvedRefs = new LinkedHashMap<>();
	private final List<String> unresolvedEntries = new ArrayList<>();

	public static ReferencesTracker getInstance()
	{
		if (instance == null)
		{
			instance = new ReferencesTracker();
		}
		return instance;
	}

	/**
	 * Initialize lookup index and reset all collected data.
	 */
	public static void init(Set<String> summary)
	{
		// capture raw summary paths
		rawSummaryPaths.clear();
		rawSummaryPaths.addAll(summary);
		// build index
		summaryIndex.clear();
		for (String path : summary)
		{
			String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
			summaryIndex.computeIfAbsent(name, k -> new ArrayList<>()).add(path);
			summaryIndex.computeIfAbsent(path, k -> new ArrayList<>()).add(path);
		}
		// reset counters and previous data
		totalLinksFound = totalLinksResolved = totalLinksUnresolved = 0;
		ReferencesTracker instance = getInstance();
		instance.resolvedRefs.clear();
		instance.unresolvedEntries.clear();
	}

	/**
	 * After all trackReferences calls, write the collected references to JSON.
	 */
	public void writeResults(String outputPath)
	{
		File file = new File(outputPath);
		try (FileWriter writer = new FileWriter(file, false))
		{
			JSONArray outputArray = new JSONArray();
			for (Map.Entry<String, List<Map<String, String>>> entry : resolvedRefs.entrySet())
			{
				JSONObject obj = new JSONObject();
				obj.put("key", entry.getKey());
				obj.put("links", entry.getValue());
				outputArray.put(obj);
			}
			writer.write(outputArray.toString(2));
			writer.write(System.lineSeparator());
			// summary
			System.out.println("References Tracker Summary: total links found = " + totalLinksFound + ", resolved = " + totalLinksResolved + ", unresolved = " +
				totalLinksUnresolved);
			if (!unresolvedEntries.isEmpty())
			{
				System.out.println("\u001B[92mUnresolved references:\u001B[0m");
				unresolvedEntries.forEach(entry -> System.out.println("\u001B[92m" + entry + "\u001B[0m"));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void trackReferences(DocumentationManager manager, Set<String> summary, Map<String, String> referenceTypes, boolean ngOnly,
		Map<String, List<String>> computedReturnTypes)
	{
		if (summaryIndex.isEmpty())
		{
			init(summary);
		}
		SortedMap<String, IObjectDocumentation> objects = manager.getObjects();
		for (Entry<String, IObjectDocumentation> entry : objects.entrySet())
		{
			String fullName = entry.getKey();
			IObjectDocumentation value = entry.getValue();

			String description = value.getDescription(value.getClientSupport());
			if (value.isDeprecated() ||
				(value.getFunctions().isEmpty() && (description == null || description.trim().isEmpty()) && computedReturnTypes.isEmpty()))
				continue;
			if (ngOnly && !(value.getClientSupport() == null ? ClientSupport.Default : value.getClientSupport()).hasSupport(ClientSupport.ng))
				continue;

			try
			{
				processDescriptionLinks(fullName, description);
				for (IFunctionDocumentation function : value.getFunctions())
				{
					String funcDesc = function.getDescription(function.getClientSupport());
					if (funcDesc != null && funcDesc.contains("<a href"))
					{
						processDescriptionLinks(fullName + "->" + function.getMainName(), funcDesc);
					}
				}
			}
			catch (RuntimeException e)
			{
				System.err.println("Error processing references for " + fullName + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void processDescriptionLinks(String key, String html)
	{
		if (html == null) return;
		Matcher matcher = LINK_PATTERN.matcher(html);
		while (matcher.find())
		{
			totalLinksFound++;
			String href = matcher.group(1);
			if (href.startsWith("http://") || href.startsWith("https://")) continue;

			String fragment = "";
			int hashIdx = href.indexOf('#');
			String pathPart = hashIdx >= 0 ? href.substring(0, hashIdx) : href;
			if (hashIdx >= 0) fragment = href.substring(hashIdx);

			String fileName = pathPart.substring(pathPart.lastIndexOf('/') + 1);
			List<String> matches = summaryIndex.getOrDefault(fileName, Collections.emptyList());
			String matchedPath = null;
			if (matches.size() == 1)
			{
				totalLinksResolved++;
				matchedPath = matches.get(0);
			}
			else
			{
				// fallback: strip ../ or ./ and try with summaryIndex
				String strippedPath = pathPart;
				while (strippedPath.startsWith("../") || strippedPath.startsWith("./"))
				{
					strippedPath = strippedPath.substring(strippedPath.indexOf('/') + 1);
				}
				// Improved fallback: match against rawSummaryPaths for exact or endsWith
				final String finalPath = strippedPath;
				List<String> fallbackMatches = rawSummaryPaths.stream()
					.filter(p -> p.equals(finalPath) || p.endsWith("/" + finalPath))
					.collect(Collectors.toList());
				if (fallbackMatches.size() == 1)
				{
					totalLinksResolved++;
					matchedPath = fallbackMatches.get(0);
				}
				else
				{
					totalLinksUnresolved++;
					unresolvedEntries.add(key + " -> " + href + " (matches: " + matches.size() + ", fallback: " + fallbackMatches.size() + ")");
					continue;
				}
			}

			String base;
			if (matchedPath.endsWith("/README.md"))
				base = matchedPath.substring(0, matchedPath.length() - "/README.md".length());
			else if (matchedPath.endsWith(".md"))
				base = matchedPath.substring(0, matchedPath.length() - ".md".length());
			else
				base = matchedPath;

			String resolvedUrl = "https://docs.servoy.com/" + base + fragment;

			Map<String, String> info = new HashMap<>();
			info.put("originalHref", href);
			info.put("resolvedUrl", resolvedUrl);

			resolvedRefs.computeIfAbsent(key, k -> new ArrayList<>()).add(info);
		}
	}

	public static int getTotalLinksFound()
	{
		return totalLinksFound;
	}

	public static int getTotalLinksResolved()
	{
		return totalLinksResolved;
	}

	public static int getTotalLinksUnresolved()
	{
		return totalLinksUnresolved;
	}
}
