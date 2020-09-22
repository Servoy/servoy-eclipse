/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ui.wizards;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Utility to use latest versions of packages (solutions, web packages) from wpm in New Solution Wizard
 *
 * @author gboros
 *
 */
public class NewSolutionWizardDefaultPackages
{
	public static final String PACKAGES[] = { "12grid", "aggrid", "bootstrapcomponents", "servoyextra", "fontawesome", "bootstrapextracomponents" };

	private static final String SOLUTIONS[][] = { { "svySearch", "1.2.3" }, { "svyProperties", "1.0.1" }, { "svySecurity", "1.4.1" }, { "svyUtils", "1.4.2" }, { "svyNavigation", "1.0.2" }, { "svyNavigationUX", "1.0.2" } };

	public static ArrayList<String> getSolutionsNames()
	{
		ArrayList<String> solutionNames = new ArrayList<String>(SOLUTIONS.length);
		for (String[] solutionInfo : SOLUTIONS)
		{
			solutionNames.add(solutionInfo[0]);
		}
		return solutionNames;
	}

	public static String getSolutionVersion(String solutionName)
	{
		String solutionVersion = "";
		for (String[] solutionInfo : SOLUTIONS)
		{
			if (solutionInfo[0].equals(solutionName))
			{
				solutionVersion = solutionInfo[1];
				break;
			}
		}
		return solutionVersion;
	}

	private static NewSolutionWizardDefaultPackages INSTANCE;

	public static NewSolutionWizardDefaultPackages getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new NewSolutionWizardDefaultPackages();
		}
		return INSTANCE;
	}

	// map with <name, version> of downloaded packages
	private final HashMap<String, String> downloadedPackages = new HashMap<String, String>();

	public void setup(List<JSONObject> packages)
	{
		File packagesFolder = new File(Activator.getDefault().getStateLocation().toFile(), "wizardpackages");
		if (!packagesFolder.exists()) packagesFolder.mkdir();
		for (File file : packagesFolder.listFiles())
		{
			if (file.isFile())
			{
				String fileNameVersion = file.getName();
				int sepIdx = fileNameVersion.indexOf('_');
				if (sepIdx != -1 && sepIdx < fileNameVersion.length() - 1)
				{
					String fileName = fileNameVersion.substring(0, sepIdx);
					String version = fileNameVersion.substring(sepIdx + 1);
					downloadedPackages.put(fileName, version);
				}
			}
		}

		ArrayList<String> allPackages = new ArrayList<String>();
		allPackages.addAll(Arrays.asList(PACKAGES));
		allPackages.addAll(getSolutionsNames());
		for (JSONObject p : packages)
		{
			String name = p.optString("name");
			if (name != null)
			{
				if (allPackages.indexOf(name) != -1)
				{
					JSONArray releases = p.optJSONArray("releases");
					if (releases != null && releases.length() > 0)
					{
						JSONObject latestRelease = releases.optJSONObject(0);
						if (latestRelease != null)
						{
							String version = latestRelease.optString("version");

							if (version != null && (!downloadedPackages.containsKey(name) ||
								(downloadedPackages.containsKey(name) && !version.equals(downloadedPackages.get(name)))))
							{
								String url = latestRelease.optString("url");

								if (url != null)
								{
									BufferedOutputStream out = null;
									BufferedInputStream in = null;
									try
									{
										File packageFile = new File(packagesFolder, name + "_" + version);

										URL urlObj = new URL(url);
										URLConnection conn = urlObj.openConnection();

										out = new BufferedOutputStream(new FileOutputStream(packageFile));
										in = new BufferedInputStream(conn.getInputStream());
										Utils.streamCopy(in, out);
										String oldVersion = downloadedPackages.put(name, version);

										File oldPackageFile = new File(packagesFolder, name + "_" + oldVersion);
										oldPackageFile.delete();
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
									finally
									{
										Utils.closeInputStream(in);
										Utils.closeOutputStream(out);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Get web or solution package
	 * @param name the name of the package
	 * @return a pair containing the version and the package input stream
	 */
	public Pair<String, InputStream> getPackage(String name)
	{
		try
		{
			if (downloadedPackages.containsKey(name))
			{
				File packagesFolder = new File(Activator.getDefault().getStateLocation().toFile(), "wizardpackages");
				File packageFile = new File(packagesFolder, name + "_" + downloadedPackages.get(name));
				if (packageFile.exists())
				{
					return new Pair<String, InputStream>(downloadedPackages.get(name), new FileInputStream(packageFile));
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

		if (Arrays.asList(PACKAGES).indexOf(name) != -1)
		{
			return new Pair<String, InputStream>("", NewServerWizard.class.getResourceAsStream("resources/packages/" + name + ".zip"));
		}
		else if (getSolutionsNames().indexOf(name) != -1)
		{
			String solutionVersion = getSolutionVersion(name);
			return new Pair<String, InputStream>(solutionVersion,
				NewServerWizard.class.getResourceAsStream("resources/solutions/" + name + "_" + solutionVersion + ".servoy"));
		}

		return null;
	}

	public Document getDatabaseInfo(String name)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			if (downloadedPackages.containsKey(name))
			{
				File packagesFolder = new File(Activator.getDefault().getStateLocation().toFile(), "wizardpackages");
				File packageFile = new File(packagesFolder, name + "_" + downloadedPackages.get(name));
				if (packageFile.exists())
				{
					try (ZipFile zip = new ZipFile(packageFile))
					{
						ZipEntry entry = zip.getEntry("export/database_info.xml");
						if (entry != null)
						{
							return builder.parse(zip.getInputStream(entry));
						}
					}
				}
			}
			else if (getSolutionsNames().indexOf(name) != -1)
			{
				try (ZipInputStream zis = new ZipInputStream(
					NewSolutionWizard.class.getResourceAsStream("resources/solutions/" + name + "_" + getSolutionVersion(name) + ".servoy")))
				{
					ZipEntry ze;
					while ((ze = zis.getNextEntry()) != null)
					{
						if ("export/database_info.xml".equals(ze.getName()))
						{
							Document doc = builder.parse(zis);
							zis.close();
							return doc;
						}
					}
					return null;

				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

		return null;
	}


}
