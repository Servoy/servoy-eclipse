/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.model.war.exporter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;

import org.apache.wicket.util.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;
import org.json.JSONObject;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.impl.ClientService;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Base class for the war export model used in the developer and the one used in command line export.
 * @author emera
 */
public abstract class AbstractWarExportModel implements IWarExportModel
{

	private final Set<String> usedComponents;
	private final Set<String> usedServices;
	protected final Map<String, License> licenses;

	protected SpecProviderState componentsSpecProviderState;
	protected SpecProviderState servicesSpecProviderState;
	private final boolean isNgExport;
	private String userHome;
	private boolean isOverwriteDeployedDBServerProperties = true;
	private boolean isOverwriteDeployedServoyProperties;

	public AbstractWarExportModel(boolean isNGExport)
	{
		usedComponents = new TreeSet<String>();
		usedServices = new TreeSet<String>();
		licenses = new HashMap<String, License>();

		this.isNgExport = isNGExport;

		if (isNGExport)
		{
			this.componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
			this.servicesSpecProviderState = WebServiceSpecProvider.getSpecProviderState();
		}
	}

	public static class License
	{
		private String companyKey;
		private String code;
		private String numberOfLicenses;

		public License(String company, String code, String numLicenses)
		{
			this.companyKey = company;
			this.code = code;
			this.numberOfLicenses = numLicenses;
		}

		public String getCompanyKey()
		{
			return companyKey;
		}

		public void setCompanyKey(String companyKey)
		{
			this.companyKey = companyKey;
		}

		public String getCode()
		{
			return code;
		}

		public void setCode(String code)
		{
			this.code = code;
		}

		public String getNumberOfLicenses()
		{
			return numberOfLicenses;
		}

		public void setNumberOfLicenses(String numberOfLicenses)
		{
			this.numberOfLicenses = numberOfLicenses;
		}


		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof License)
			{
				License l = (License)obj;
				return code.equals(l.code) && companyKey.equals(l.companyKey) && numberOfLicenses == l.numberOfLicenses;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return code.hashCode();
		}

	}

	public boolean containsLicense(String code)
	{
		return licenses.containsKey(code);
	}

	private void findUsedComponents(ISupportChilds parent)
	{
		Iterator<IPersist> persists = parent.getAllObjects();
		while (persists.hasNext())
		{
			IPersist persist = persists.next();
			if (persist instanceof IFormElement)
			{
				usedComponents.add(FormTemplateGenerator.getComponentTypeName((IFormElement)persist));
			}
			if (persist instanceof ISupportChilds)
			{
				findUsedComponents((ISupportChilds)persist);
			}
		}
	}

	public void extractUsedComponentsAndServices(String scriptPath)
	{
		IFile scriptFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(scriptPath));
		if (scriptFile.exists())
		{
			try
			{
				InputStream is = scriptFile.getContents();
				final String source = IOUtils.toString(is);
				is.close();
				if (source != null)
				{
					JavaScriptParser parser = new JavaScriptParser();
					Script script = parser.parse(source, null);
					script.visitAll(new AbstractNavigationVisitor<ASTNode>()
					{

						/*
						 * (non-Javadoc)
						 *
						 * @see org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor#visitCallExpression(org.eclipse.dltk.javascript.ast.CallExpression)
						 */
						@Override
						public ASTNode visitCallExpression(CallExpression node)
						{
							if (node.getExpression().getChilds().size() > 0)
							{
								ASTNode astNode = node.getExpression().getChilds().get(0);
								String expr = source.substring(astNode.sourceStart(), astNode.sourceEnd());
								if (expr.startsWith("plugins."))
								{
									String[] parts = expr.split("\\.");
									if (parts.length > 1)
									{
										WebObjectSpecification serviceSpec = ClientService.getServiceDefinitionFromScriptingName(parts[1]);
										if (serviceSpec != null) usedServices.add(serviceSpec.getName());
									}
								}
								else if (expr.contains("newWebComponent"))
								{
									if (node.getArguments().size() > 1)
									{
										ASTNode arg = node.getArguments().get(1);
										String componentName = source.substring(arg.sourceStart(), arg.sourceEnd());
										if (componentName.startsWith("\"") || componentName.startsWith("'"))
										{
											componentName = componentName.replaceAll("'|\"", "");
											if (WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(componentName) != null)
											{
												usedComponents.add(componentName);
											}
										}
									}
								}
							}
							return super.visitCallExpression(node);
						}
					});
				}
			}
			catch (CoreException e)
			{
				Debug.error(e);
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getUsedComponents()
	 */
	@Override
	public Set<String> getUsedComponents()
	{
		return usedComponents;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getUsedServices()
	 */
	@Override
	public Set<String> getUsedServices()
	{
		return usedServices;
	}

	@Override
	public String[] getModulesToExport()
	{
		ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
		String[] toExport = new String[modules.length];
		for (int i = 0; i < modules.length; i++)
		{
			toExport[i] = modules[i].getSolution().getName();
		}
		return toExport;
	}

	@Override
	public boolean isProtectWithPassword()
	{
		return false;
	}

	@Override
	public String getPassword()
	{
		return null;
	}

	@Override
	public boolean isExportReferencedModules()
	{
		return true;
	}

	@Override
	public boolean isExportReferencedWebPackages()
	{
		return false;
	}

	public List<Pair<String, List<File>>> getModulesWebPackages()
	{
		return null;
	}

	@Override
	public boolean useImportSettings()
	{
		return false;
	}

	@Override
	public JSONObject getImportSettings()
	{
		return null;
	}

	protected void search()
	{
		if (!isNGExport()) return;
		FlattenedSolution solution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		Iterator<Form> forms = solution.getForms(false);
		while (forms.hasNext())
		{
			Form form = forms.next();
			findUsedComponents(form);
			extractUsedComponentsAndServices(SolutionSerializer.getRelativePath(form, false) + form.getName() + SolutionSerializer.JS_FILE_EXTENSION);
			if (form.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
			{
				usedComponents.add("servoycore-navigator");
				usedComponents.add("servoycore-slider");
			}
		}

		for (Pair<String, IRootObject> scope : solution.getAllScopes())
		{
			extractUsedComponentsAndServices(
				SolutionSerializer.getRelativePath(scope.getRight(), false) + scope.getLeft() + SolutionSerializer.JS_FILE_EXTENSION);
		}

		//these are always required
		usedComponents.add("servoycore-errorbean");
		usedComponents.add("servoycore-portal");
		usedServices.addAll(servicesSpecProviderState.getWebObjectSpecifications().get("servoyservices").getSpecifications().keySet());
	}

	@Override
	public Collection<License> getLicenses()
	{
		return licenses.values();
	}

	public void addLicense(License license)
	{
		licenses.put(license.code, license);
	}

	public String decryptPassword(Cipher desCipher, String password)
	{
		String result = "";
		if (password.startsWith(IWarExportModel.enc_prefix))
		{
			try
			{
				String val_val = password.substring(IWarExportModel.enc_prefix.length());
				byte[] array_val = Utils.decodeBASE64(val_val);
				result = new String(desCipher.doFinal(array_val));
			}
			catch (Exception e)
			{
				Debug.error("Could not decrypt property");
			}
		}
		else if (!"".equals(password))
		{
			result = new String(Utils.decodeBASE64(password));
		}
		return result;
	}

	public boolean isNGExport()
	{
		return isNgExport;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#setUserHome(java.lang.String)
	 */
	@Override
	public void setUserHome(String userHome)
	{
		this.userHome = userHome;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#getUserHome()
	 */
	@Override
	public String getUserHome()
	{
		return userHome;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#isOverwriteDeployedDBServerProperties()
	 */
	@Override
	public boolean isOverwriteDeployedDBServerProperties()
	{
		return isOverwriteDeployedDBServerProperties;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#isOverwriteDeployedServoyProperties()
	 */
	@Override
	public boolean isOverwriteDeployedServoyProperties()
	{
		return isOverwriteDeployedServoyProperties;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#setOverwriteDeployedDBServerProperties(boolean)
	 */
	@Override
	public void setOverwriteDeployedDBServerProperties(boolean isOverwriteDeployedDBServerProperties)
	{
		this.isOverwriteDeployedDBServerProperties = isOverwriteDeployedDBServerProperties;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.war.exporter.IWarExportModel#setOverwriteDeployedServoyProperties(boolean)
	 */
	@Override
	public void setOverwriteDeployedServoyProperties(boolean isOverwriteDeployedServoyProperties)
	{
		this.isOverwriteDeployedServoyProperties = isOverwriteDeployedServoyProperties;
	}


	public String checkWebXML()
	{
		String message = null;
		if (getWebXMLFileName() != null)
		{
			File f = new File(getWebXMLFileName());
			if (!f.exists())
			{
				message = "Specified web.xml  file doesn't exist.";
			}
			else if (f.isDirectory())
			{
				message = "Specified web.xml  file is a folder.";
			}
			else
			{
				String content = Utils.getTXTFileContent(f, Charset.forName("UTF8"));
				if (content == null || content.trim().length() == 0)
				{
					message = "Specified web.xml file has no content";
				}
				else
				{
					final String VERSION_STRING = "servoy web.xml version:";
					int index = content.indexOf(VERSION_STRING);
					if (index == -1)
					{
						message = "Specified web.xml file is not a valid servoy web,xml file (doesn't contain he servoy version comment)";
					}
					else
					{
						int index2 = content.indexOf("-->", index);
						int version = Utils.getAsInteger(content.substring(index + VERSION_STRING.length(), index2).trim(), 0);

						String currentWebXml = Utils.getTXTFileContent(WarExporter.class.getResourceAsStream("resources/web.xml"), Charset.forName("UTF8"),
							true);
						int currentWebXmlIndex = currentWebXml.indexOf(VERSION_STRING);
						int currentWebXmlIndex2 = currentWebXml.indexOf("-->", currentWebXmlIndex);
						int currentWebXmlVersion = Utils.getAsInteger(
							currentWebXml.substring(currentWebXmlIndex + VERSION_STRING.length(), currentWebXmlIndex2).trim(), 0);
						if (version != currentWebXmlVersion)
						{
							message = "Specified web.xml file is has a different version (" + version + ") then what is current shipped in servoy (" +
								currentWebXmlVersion + ") please regenerate the web.xml first";
						}
					}
				}
			}
		}
		return message;
	}
}
