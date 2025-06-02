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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.servoy.eclipse.core.util.UIUtils;
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

	public static final String SVYGEN_TEMPLATES = "svyGenCore";

	private static final String SOLUTIONS[][] = { { "svySearch", "2022.3.0" }, { "svyProperties", "1.0.1" }, { "svySecurity", "1.6.0" }, { "svyUtils", "2022.3.0" }, { "svyNavigation", "2022.3.0" }, { "svyNavigationUX", "2022.3.0" }, { SVYGEN_TEMPLATES, "1.0.0" } };

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

	public void setup(List<JSONObject> packages) throws IOException
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
									File dataFile = Utils.downloadUrlPackage(url);

									BufferedOutputStream out = null;
									BufferedInputStream in = new BufferedInputStream(new FileInputStream(dataFile));
									try
									{
										File packageFile = new File(packagesFolder, name + "_" + version);

										out = new BufferedOutputStream(new FileOutputStream(packageFile));
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
	//public Pair<String, InputStream> getPackage(String name)
	public Pair<String, File> getPackage(String name)
	{
		try
		{
			if (downloadedPackages.containsKey(name))
			{
				File packagesFolder = new File(Activator.getDefault().getStateLocation().toFile(), "wizardpackages");
				File packageFile = new File(packagesFolder, name + "_" + downloadedPackages.get(name));
				if (packageFile.exists())
				{
					return new Pair<String, File>(downloadedPackages.get(name), packageFile);
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

		try
		{
			if (Arrays.asList(PACKAGES).indexOf(name) != -1)
			{
				File dataFile = Utils.downloadUrlPackage(NewServerWizard.class.getResource("resources/packages/" + name + ".zip").toString());
				return new Pair<String, File>("", dataFile);
			}
			else if (getSolutionsNames().indexOf(name) != -1)
			{
				String solutionVersion = getSolutionVersion(name);
				File dataFile = Utils
					.downloadUrlPackage(NewServerWizard.class.getResource("resources/solutions/" + name + "_" + solutionVersion + ".servoy").toString());
				return new Pair<String, File>(solutionVersion, dataFile);
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Error", e.getMessage());
				}
			});
		}
		return null;
	}

	public Document getDatabaseInfo(String name)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
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
