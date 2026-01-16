/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.debug.script;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReturnTypeParser
{

	/**
	 * Represents a parsed return type along with an optional description.
	 */
	public static record ParsedReturnType(
		String fullType, // e.g. "Promise<JSDataSet>"
		String baseType, // e.g. "Promise"
		List<String> genericArgs, // e.g. ["JSDataSet", "Result<Number>"]
		String description // e.g. "The promise that will receive the result."
	)
	{
		@Override
		public String toString()
		{
			return "ParsedReturnType[fullType=%s, baseType=%s, genericArgs=%s, description=%s]"
				.formatted(fullType, baseType, genericArgs, description);
		}
	}

	/**
	 * Parses a return type declaration with optional description.
	 * Examples it accepts:
	 *   "{Promise<JSDataSet>} The promise that will receive the result."
	 *   "{Object}"
	 *   "Promise<JSRecord>"
	 *   "{Map<String, List<Number>>} Some complex type"
	 *   "{void}"
	 *   "{Array<JSRecord>}"
	 *   "{JSFoundSet} Foundset of related records"
	 *   "{Promise<JSDataSet>}The promise..." (no space)
	 *   "Promise<JSDataSet> - The result arrives asynchronously."
	 *
	 * Notes:
	 *  - HTML entities &lt; &gt; are normalized.
	 *  - Outer braces around the type are optional.
	 *  - Description is anything after the type. If type is braced, description starts after the closing brace.
	 */
	public static ParsedReturnType parse(String input)
	{
		if (input == null || input.trim().isEmpty())
		{
			return new ParsedReturnType("void", "void", Collections.emptyList(), "");
		}

		String s = normalizeEntities(input.trim());

		String typePart;
		String description = "";

		// If input starts with a brace, we try to extract the balanced {...} at the start
		if (s.startsWith("{"))
		{
			int closing = findMatchingBraceEnd(s, 0, '{', '}');
			if (closing > 0)
			{
				// Extract inside braces as type
				typePart = s.substring(1, closing).trim();
				// Everything after the closing brace is description
				description = s.substring(closing + 1).trim();
			}
			else
			{
				// Malformed braces; fallback: try to parse up to the first '}' if any
				int idx = s.indexOf('}');
				if (idx > 0)
				{
					typePart = s.substring(1, idx).trim();
					description = s.substring(idx + 1).trim();
				}
				else
				{
					// No closing brace, assume no description
					typePart = s.substring(1).trim();
				}
			}
		}
		else
		{
			// No initial brace: split type from description heuristically
			// Prefer splitting on known delimiters like ' - ' or first '. ' after a type-looking token.
			// First, try to find a safe boundary (space) after a type expression that may include generics.
			int boundary = findTypeDescriptionBoundary(s);
			if (boundary >= 0)
			{
				typePart = s.substring(0, boundary).trim();
				description = s.substring(boundary).trim();
				// Trim leading separators from the description
				description = trimLeadingSeparators(description);
			}
			else
			{
				typePart = s.trim();
				description = "";
			}
		}

		// Normalize typePart by removing accidental outer braces again if present
		typePart = stripOuterBraces(typePart);

		if (typePart.isEmpty())
		{
			return new ParsedReturnType("void", "void", Collections.emptyList(), description);
		}

		// Parse base type and generic arguments (depth-aware, supports nesting)
		String fullType = typePart;
		String baseType = extractBaseType(typePart);
		List<String> genericArgs = extractGenericArguments(typePart);

		return new ParsedReturnType(fullType, baseType, Collections.unmodifiableList(genericArgs), description);
	}

	// ---------- Helpers ----------

	private static String normalizeEntities(String s)
	{
		return s.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&amp;lt;", "<")
			.replace("&amp;gt;", ">");
	}

	private static String stripOuterBraces(String type)
	{
		String t = type.trim();
		if (t.startsWith("{") && t.endsWith("}") && t.length() >= 2)
		{
			return t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	/**
	 * Find the matching closing brace for the first character at 'start'.
	 * Returns index of matching char or -1 if not found / malformed.
	 */
	private static int findMatchingBraceEnd(String s, int start, char open, char close)
	{
		if (start < 0 || start >= s.length() || s.charAt(start) != open) return -1;
		int depth = 0;
		for (int i = start; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == open) depth++;
			else if (c == close)
			{
				depth--;
				if (depth == 0) return i;
			}
		}
		return -1;
	}

	/**
	 * Attempts to find a reasonable boundary between a (possibly generic) type and a description
	 * in strings that don't start with '{' and aren't already split.
	 *
	 * Heuristics:
	 *  - If a " - " sequence exists after a type-looking segment, split there.
	 *  - Otherwise, try to split after the generic block or the first space after a balanced generic.
	 */
	private static int findTypeDescriptionBoundary(String s)
	{
		// If there's an explicit " - ", use that
		int dashSep = s.indexOf(" - ");
		if (dashSep > 0)
		{
			return dashSep;
		}

		// If there are generics, try to end after the matching '>' for the first '<'
		int lt = s.indexOf('<');
		if (lt >= 0)
		{
			int gt = findMatchingAngleEnd(s, lt);
			if (gt > lt && gt + 1 < s.length())
			{
				// If the char after generics is a space or punctuation, split there
				int pos = gt + 1;
				// Skip trailing punctuation matching a type expression (like [] or ?)
				while (pos < s.length() && isTypeTrailerChar(s.charAt(pos)))
					pos++;
				// Skip one space if present
				if (pos < s.length() && Character.isWhitespace(s.charAt(pos)))
				{
					while (pos < s.length() && Character.isWhitespace(s.charAt(pos)))
						pos++;
					return pos;
				}
				// Otherwise, if there is a space later, we can split at the first space after gt
				int spaceAfter = s.indexOf(' ', gt + 1);
				if (spaceAfter > gt)
				{
					return spaceAfter + 1;
				}
			}
		}

		// No generics: split at first space (if any)
		int firstSpace = s.indexOf(' ');
		if (firstSpace > 0)
		{
			return firstSpace + 1;
		}
		return -1;
	}

	/** Matches nested <...> pairs. */
	private static int findMatchingAngleEnd(String s, int startLt)
	{
		int depth = 0;
		for (int i = startLt; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '<') depth++;
			else if (c == '>')
			{
				depth--;
				if (depth == 0) return i;
			}
		}
		return -1;
	}

	private static boolean isTypeTrailerChar(char c)
	{
		return c == '[' || c == ']' || c == '?' || c == '!';
	}

	private static String trimLeadingSeparators(String s)
	{
		String t = s.trim();
		// Remove leading hyphen or colon separators commonly used in docs
		if (t.startsWith("-")) t = t.substring(1).trim();
		if (t.startsWith(":")) t = t.substring(1).trim();
		return t;
	}

	private static String extractBaseType(String typePart)
	{
		String t = typePart.trim();
		// Base type is the identifier before the first '<', or entire string if no generics
		int lt = t.indexOf('<');
		String before = (lt >= 0) ? t.substring(0, lt) : t;
		// Also handle array-like suffixes (e.g., "Number[]") by trimming them from base type only
		before = before.trim();
		// Remove any trailing array / nullable markers from baseType, keep them in fullType
		while (!before.isEmpty() && (before.endsWith("[]") || before.endsWith("?") || before.endsWith("!")))
		{
			if (before.endsWith("[]")) before = before.substring(0, before.length() - 2);
			else before = before.substring(0, before.length() - 1);
			before = before.trim();
		}
		return before.isEmpty() ? t : before;
	}

	/**
	 * Extracts top-level generic arguments from a type like:
	 *   "Map<String, List<Number>>" -> ["String", "List<Number>"]
	 *   "Promise<JSDataSet>" -> ["JSDataSet"]
	 *   "Either<Result<Number>, Error>" -> ["Result<Number>", "Error"]
	 * Returns empty list if there are no generics.
	 */
	private static List<String> extractGenericArguments(String typePart)
	{
		List<String> result = new ArrayList<>();
		String t = typePart.trim();

		int lt = t.indexOf('<');
		if (lt < 0) return result;

		int gt = findMatchingAngleEnd(t, lt);
		if (gt < 0) return result; // malformed

		String inside = t.substring(lt + 1, gt).trim();
		if (inside.isEmpty()) return result;

		// Split by commas at depth 0 within the <...> block
		List<String> args = splitByCommaDepthAware(inside, '<', '>');
		for (String arg : args)
		{
			String cleaned = arg.trim();
			if (!cleaned.isEmpty()) result.add(cleaned);
		}
		return result;
	}

	/**
	 * Depth-aware splitter on commas with nested delimiters.
	 */
	private static List<String> splitByCommaDepthAware(String s, char open, char close)
	{
		List<String> parts = new ArrayList<>();
		int depth = 0;
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == open)
			{
				depth++;
				current.append(c);
			}
			else if (c == close)
			{
				depth = Math.max(0, depth - 1);
				current.append(c);
			}
			else if (c == ',' && depth == 0)
			{
				parts.add(current.toString());
				current.setLength(0);
			}
			else
			{
				current.append(c);
			}
		}
		if (current.length() > 0) parts.add(current.toString());
		return parts;
	}

	public static void main(String[] args)
	{
		String[] samples = new String[] { "{Map<String, String[]>} A map of parameter", "{Promise<Array<plugins.http.Response>>} The promise object tha", "{Promise<plugins.http.Response>} A Promise that resolves with a", "{Promise&lt;JSDataSet&gt;} The promise that will receive the result.", "{Object}", "Promise<JSRecord>", "{Map<String, List<Number>>} Some complex type", "{void}", "{Array<JSRecord>}", "{JSFoundSet} Foundset of related records", "{Either<Result<Number>, Error>} When computation can fail", "Promise<JSDataSet> - The result arrives asynchronously.", "{Promise<JSDataSet[]>} Returns an array within a promise", "{Option<Result<Map<String,Number>>>} Deeply nested example"
		};

		for (String s : samples)
		{
			ParsedReturnType p = parse(s);
			System.out.println("--- INPUT ---");
			System.out.println(s);
			System.out.println("--- PARSED ---");
			System.out.println("fullType   = " + p.fullType());
			System.out.println("baseType   = " + p.baseType());
			System.out.println("genericArgs= " + p.genericArgs());
			System.out.println("description= " + p.description());
			System.out.println();
		}
	}
}