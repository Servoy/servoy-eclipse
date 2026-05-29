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

package com.servoy.eclipse.ui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Caches the GenAI team spend information fetched from the genai.servoy-cloud.eu user info endpoint.
 * The cache has a TTL of 5 minutes. Refreshes happen on a daemon background thread so the UI thread
 * is never blocked.
 *
 * @author jcompagner
 */
public class GenaiUsageCache
{
	/** Threshold percentage above which the "upgrade?" prompt is shown. */
	private static final long UPGRADE_THRESHOLD_PERCENT = 90;

	/** Cache TTL in milliseconds (5 minutes). */
	private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

	/** The GenAI user info endpoint. */
	private static final String USER_INFO_URL = "https://genai.servoy-cloud.eu/user/info"; //$NON-NLS-1$

	/** Cached spend information; null when no data has been fetched or the cache has been cleared. */
	private static final AtomicReference<SpendInfo> cached = new AtomicReference<>(null);

	/** Timestamp (System.currentTimeMillis()) of the last successful fetch; 0 when cache is empty. */
	private static final AtomicLong fetchedAt = new AtomicLong(0);

	/** Tracks whether a background refresh is already in progress to avoid duplicate fetches. */
	private static volatile boolean refreshing = false;

	private GenaiUsageCache()
	{
		// utility class
	}

	/**
	 * Immutable record holding a team's spend and budget values.
	 */
	public record SpendInfo(double spend, double maxBudget)
	{
	}

	/**
	 * Returns the most recently cached {@link SpendInfo}, or {@code null} if the cache is empty.
	 * This method never blocks.
	 *
	 * @return the cached value, or {@code null}
	 */
	public static SpendInfo getCached()
	{
		return cached.get();
	}

	/**
	 * If the cache is absent or older than 5 minutes, starts a daemon background thread that
	 * fetches {@code /user/info} and updates the cache. Returns immediately (non-blocking).
	 * Does nothing when the {@code GENAI_API_KEY} system property is blank or absent.
	 */
	public static void refreshIfStale()
	{
		String apiKey = System.getProperty("GENAI_API_KEY"); //$NON-NLS-1$
		if (apiKey == null || apiKey.isBlank()) return;

		long now = System.currentTimeMillis();
		long lastFetch = fetchedAt.get();
		boolean stale = (lastFetch == 0) || (now - lastFetch > CACHE_TTL_MS);
		if (!stale || refreshing) return;

		refreshing = true;
		Thread t = new Thread(() -> {
			try
			{
				fetchAndUpdate(apiKey);
			}
			finally
			{
				refreshing = false;
			}
		}, "GenaiUsageCache-refresh"); //$NON-NLS-1$
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Clears the cache. Should be called after logout so that the next popup shows "loading..."
	 * until a fresh fetch completes.
	 */
	public static void clear()
	{
		cached.set(null);
		fetchedAt.set(0);
	}

	/**
	 * Formats a {@link SpendInfo} as a human-readable percentage string.
	 * Returns {@code "spend: X%"} or {@code "spend: X% (upgrade?)"} when spend exceeds 90 %.
	 *
	 * @param info the spend info to format; must not be {@code null}
	 * @return formatted string
	 */
	public static String formatSpend(SpendInfo info)
	{
		long percent = Math.round(info.spend() / info.maxBudget() * 100);
		if (percent > UPGRADE_THRESHOLD_PERCENT)
		{
			return "AI spend: " + percent + "% (upgrade?)"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "AI spend: " + percent + "%"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	// ---- private helpers ----

	private static void fetchAndUpdate(String apiKey)
	{
		try
		{
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();

			HttpRequest request = HttpRequest.newBuilder(URI.create(USER_INFO_URL))
				.header("Authorization", "Bearer " + apiKey) //$NON-NLS-1$ //$NON-NLS-2$
				.header("Accept", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
				.timeout(Duration.ofSeconds(5))
				.GET()
				.build();

			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

			if (response.statusCode() == 200)
			{
				JSONObject json = new JSONObject(response.body());
				JSONArray teams = json.getJSONArray("teams"); //$NON-NLS-1$
				JSONObject team = teams.getJSONObject(0);
				double spend = team.getDouble("spend"); //$NON-NLS-1$
				double maxBudget = team.getDouble("max_budget"); //$NON-NLS-1$
				cached.set(new SpendInfo(spend, maxBudget));
				fetchedAt.set(System.currentTimeMillis());
			}
			else
			{
				ServoyLog.logError(
					"GenaiUsageCache: non-200 response from user/info endpoint: " + response.statusCode(), null); //$NON-NLS-1$
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}
}
