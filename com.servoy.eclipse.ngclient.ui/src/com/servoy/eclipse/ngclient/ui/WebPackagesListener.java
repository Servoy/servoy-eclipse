/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.CssLib;
import org.sablo.specification.CssLibSet;
import org.sablo.specification.NG2Config;
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.Package.ZipPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.impl.ClientService;

import com.servoy.eclipse.model.ING2WarExportModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IEditorRefresh;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.SerialRule;
import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.utils.ZipUtils;
import com.servoy.j2db.util.DeletePathVisitor;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompager
 * @since 2021.06
 */
public class WebPackagesListener implements ILoadedNGPackagesListener
{
	// list of packages that are in node/projects folder
	private static final String[] defaultPackages = new String[] { "@servoy/servoydefault", "@servoy/dialogs", "@servoy/ngclientutils", "@servoy/window" };
	private static final Map<String, String> NG1MAPPING = new HashMap<>();
	static
	{
		Arrays.sort(defaultPackages);
		NG1MAPPING.put("@servoy/ngclientutils", "servoy_ng_only_services");
		NG1MAPPING.put("@servoy/dialogs", "servoydefaultservices");
		NG1MAPPING.put("@servoy/servoydefault", "servoydefault");
	}

	public static String generateAllComponentsModuleContent(Collection<PackageSpecification< ? >> packages)
	{
		StringBuilder allComponentsModule = new StringBuilder(256);
		allComponentsModule.append("import { NgModule } from '@angular/core';\n");
		packages.forEach(spec -> {
			if (spec.hasStandaloneComponents())
			{
				allComponentsModule.append("import { ");
				allComponentsModule.append(spec.getNg2Components());
				allComponentsModule.append(" } from '");
				allComponentsModule.append(spec.getNpmPackageName());
				allComponentsModule.append("';\n");
			}
			else if (spec.getNg2Module() != null)
			{
				allComponentsModule.append("import { ");
				allComponentsModule.append(spec.getNg2Module());
				allComponentsModule.append(" } from '");
				allComponentsModule.append(spec.getNpmPackageName());
				allComponentsModule.append("';\n");
			}
		});

		allComponentsModule.append("@NgModule({\n imports: [\n");
		packages.forEach(spec -> {
			if (spec.hasStandaloneComponents())
			{
				for (String component : spec.getNg2Components().split(","))
				{
					allComponentsModule.append(component.trim());
					allComponentsModule.append(",\n");
				}
			}
			else if (spec.getNg2Module() != null)
			{
				allComponentsModule.append(spec.getNg2Module());
				allComponentsModule.append(",\n");
			}
		});

		allComponentsModule.append(" ],\n exports: [\n");
		packages.forEach(spec -> {
			if (spec.hasStandaloneComponents())
			{
				for (String component : spec.getNg2Components().split(","))
				{
					allComponentsModule.append(component.trim());
					allComponentsModule.append(",\n");
				}
			}
			else if (spec.getNg2Module() != null)
			{
				allComponentsModule.append(spec.getNg2Module());
				allComponentsModule.append(",\n");
			}
		});
		allComponentsModule.append(" ]\n})\nexport class AllComponentsModule { }\n");
		return allComponentsModule.toString();
	}

	/**
	 * @author jcompanger
	 * @since 2021.06
	 */
	private static final class PackageCheckerJob extends Job
	{
		private final ITiNGExportModel warExportModel;
		private final File projectFolder;
		private final String whatToRun;
		// SVY-21502: the solution whose committed ng_web_packages/pnpm-build-approvals.json governs the pnpm build
		// scripts. For a Developer build this is null and the active solution project is used; for a WAR export it
		// is the exported solution name so the headless path can read the committed lists with no UI.
		private final String approvalsSolutionName;

		/**
		 * @param name
		 */
		private PackageCheckerJob(String name, File projectFolder, ITiNGExportModel warExportModel)
		{
			this(name, projectFolder, warExportModel, null);
		}

		private PackageCheckerJob(String name, File projectFolder, ITiNGExportModel warExportModel, String approvalsSolutionName)
		{
			super(name);
			this.warExportModel = warExportModel;
			this.projectFolder = projectFolder;
			this.approvalsSolutionName = approvalsSolutionName;
			String toRun = "build_debug_nowatch";
			if (warExportModel != null)
			{
				// skip the null values and the true value then it should just be "build"
				if (!Arrays.asList(null, "", "true").contains(warExportModel.exportNG2Mode()))
				{
					if ("sourcemaps".equals(warExportModel.exportNG2Mode()))
						toRun = "build_sourcemap";
					else if ("dev".equals(warExportModel.exportNG2Mode()))
						toRun = "build_debug_nowatch";
					else toRun = warExportModel.exportNG2Mode();
				}
				else
				{
					toRun = "build";
				}
			}
			else
			{
				if (Utils.isInTestingMode(null))
				{
					toRun = "build_sourcemap";
				}
			}
			whatToRun = toRun;

		}

		@Override
		public boolean belongsTo(Object family)
		{
			return CopySourceFolderAction.JOB_FAMILY.equals(family);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor)
		{
			if (Activator.isNodeExtractionAndTitaniumBuildDisabled()) return Status.CANCEL_STATUS;

			StringOutputStream console = Activator.getInstance().getConsole().outputStream();
			try
			{
				scheduled.incrementAndGet();
				long time = System.currentTimeMillis();
				writeConsole(console,
					"---- Starting Titanium NGClient solution/dependencies source check (" + DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now()) + ")");
				// modules and css of the components those are based on the Packages itself
				CssLibSet cssLibs = new CssLibSet();
				boolean hasFontAwesomePro = false;
				Set<String> packageToInstall = new HashSet<>();
				Set<String> assetsToAdd = new HashSet<>();
				Map<String, Pair<WebLayoutSpecification, String>> structureTagNames = new HashMap<>();
				// service are based just on all service specifications
				Map<WebObjectSpecification, IPackageReader> ng2Services = new TreeMap<>((spec1, spec2) -> spec1.getName().compareTo(spec2.getName()));
				SpecProviderState serviceProviderState = WebServiceSpecProvider.getSpecProviderState();
				WebObjectSpecification[] allServices = serviceProviderState.getAllWebObjectSpecifications();
				for (WebObjectSpecification webObjectSpecification : allServices)
				{
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					if (this.warExportModel != null && !this.warExportModel.getExportedPackagesExceptSablo().contains(webObjectSpecification.getPackageName()))
					{
						continue;
					}
					Set<CssLib> libs = webObjectSpecification.getNG2Config().getDependencies().getCssLibrary();
					if (libs != null)
					{
						cssLibs.addAll(libs);
						if ("@servoy/fontawesomepro".equals(webObjectSpecification.getNG2Config().getPackageName()))
						{
							hasFontAwesomePro = true;
						}
					}
					List<String> assets = webObjectSpecification.getNG2Config().getAssets().getAssetsList();
					if (assets != null)
					{
						assetsToAdd.addAll(assets);
					}
					if (webObjectSpecification.getNG2Config().getPackageName() != null)
					{
						if (isDefaultPackageEnabled(webObjectSpecification.getNG2Config().getPackageName()))
						{
							IPackageReader packageReader = serviceProviderState
								.getPackageReader(webObjectSpecification.getPackageName());
							ng2Services.put(webObjectSpecification, packageReader);
						}
					}
				}

				TreeMap<PackageSpecification< ? extends WebObjectSpecification>, IPackageReader> componentPackageSpecToReader = new TreeMap<>(
					(spec1, spec2) -> spec1.getPackageName().compareTo(spec2.getPackageName()));
				SpecProviderState specProviderState = WebComponentSpecProvider.getSpecProviderState();
				for (PackageSpecification<WebObjectSpecification> entry : specProviderState.getWebObjectSpecifications().values())
				{
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					if (this.warExportModel != null && !this.warExportModel.getExportedPackagesExceptSablo().contains(entry.getPackageName()))
					{
						continue;
					}

					String module = entry.getNg2Module();
					String packageName = entry.getNpmPackageName();
					if ((!Utils.stringIsEmpty(module) || entry.hasStandaloneComponents()) && !Utils.stringIsEmpty(packageName))
					{
						IPackageReader packageReader = specProviderState.getPackageReader(entry.getPackageName());
						componentPackageSpecToReader.put(entry, packageReader);
					}

					Set<CssLib> libs = entry.getNg2CssLibrary();
					if (libs != null)
					{
						cssLibs.addAll(libs);
					}
				}
				for (PackageSpecification<WebLayoutSpecification> entry : specProviderState.getLayoutSpecifications().values())
				{
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					if (this.warExportModel != null && !this.warExportModel.getExportedPackagesExceptSablo().contains(entry.getPackageName()))
					{
						continue;
					}

					String module = entry.getNg2Module();
					String packageName = entry.getNpmPackageName();
					if ((!Utils.stringIsEmpty(module) || entry.hasStandaloneComponents()) && !Utils.stringIsEmpty(packageName))
					{
						IPackageReader packageReader = specProviderState.getPackageReader(entry.getPackageName());
						componentPackageSpecToReader.put(entry, packageReader);
					}

					Set<CssLib> libs = entry.getNg2CssLibrary();
					if (libs != null)
					{
						cssLibs.addAll(libs);
					}

					Map<String, WebLayoutSpecification> specifications = entry.getSpecifications();
					specifications.values().forEach(spec -> {
						String tagName = null;
						PropertyDescription tagType = spec.getProperty("tagType");
						if (tagType != null && tagType.getDefaultValue() != null && !"div".equals(tagType.getDefaultValue()))
						{
							tagName = (String)tagType.getDefaultValue();
							structureTagNames.put(tagName, new Pair<WebLayoutSpecification, String>(spec, tagName));
						}
						List<String> directives = spec.getDirectives();
						if (directives.size() > 0)
						{
							if (tagName == null) tagName = "div";
							else structureTagNames.remove(tagName);
							structureTagNames.put(spec.getName(), new Pair<WebLayoutSpecification, String>(spec, tagName));
						}
					});
				}


				boolean sourceChanged = false;
				if (ng2Services.size() > 0 || componentPackageSpecToReader.size() > 0)
				{
					File distIndexFile = new File(projectFolder, "dist/app/browser/index.html");
					sourceChanged = !distIndexFile.exists();
					if (sourceChanged)
					{
						writeConsole(console, "- build will be triggered; no previously build-generated files detected...");
					}
					try
					{
						File packageJson = new File(projectFolder, "package.json");
						String json = FileUtils.readFileToString(packageJson, "UTF-8");
						JSONObject jsonObject = new JSONObject(json);
						JSONObject dependencies = jsonObject.optJSONObject("dependencies");
						ng2Services.entrySet().forEach(entry -> {
							WebObjectSpecification spec = entry.getKey();
							NG2Config ng2Config = spec.getNG2Config();
							String packageName = ng2Config.getPackageName();
							IPackageReader packageReader = entry.getValue();
							String entryPoint = ng2Config.getEntryPoint();
							String pck = checkPackage(dependencies, packageName, packageReader, entryPoint, console);
							if (pck != null)
							{
								writeConsole(console, "- need to install package " + pck);
								packageToInstall.add(pck);
							}
						});

						componentPackageSpecToReader.entrySet().forEach(entry -> {
							PackageSpecification< ? extends WebObjectSpecification> spec = entry.getKey();
							String packageName = spec.getNpmPackageName();
							IPackageReader packageReader = entry.getValue();
							String entryPoint = spec.getEntryPoint();
							String pck = checkPackage(dependencies, packageName, packageReader, entryPoint, console);
							if (pck != null)
							{
								packageToInstall.add(pck);
							}
						});
					}
					catch (IOException e)
					{
						writeErrorToConsoleAndLog(console, e, "Exception while checking what packages should be installed: ");
						// TODO should we return a WARNING status here as we do for other failures?
					}
				}

				// adjust the allservices.sevice.ts
				try
				{
					StringBuilder imports = new StringBuilder("// generated imports start\n");
					StringBuilder services = new StringBuilder("// generated services start\n");
					StringBuilder providers = new StringBuilder("// generated providers start\n");
					StringBuilder modules = new StringBuilder("// generated modules start\n");
					ng2Services.keySet().forEach(service -> {
						if (service.getNG2Config().getServiceName() != null)
						{
							if (this.warExportModel != null && !this.warExportModel.getAllExportedServicesWithoutSabloServices().contains(service.getName()))
							{
								return;
							}

							String moduleName = service.getNG2Config().getModuleName();
							// import the service
							imports.append("import { ");
							String serviceName = service.getNG2Config().getServiceName();
							imports.append(serviceName);
							// add it to the modules
							if (moduleName != null && modules.indexOf(moduleName + ',') == -1)
							{
								imports.append(',');
								imports.append(moduleName);
								modules.append(moduleName);
								modules.append(",\n");
							}
							imports.append(" } from '");
							imports.append(service.getNG2Config().getPackageName());
							imports.append("';\n");
							// add it to the service declarations as inject() field initializers
							services.append("private readonly ");
							services.append(ClientService.convertToJSName(service.getName()));
							services.append(" = inject(");
							services.append(serviceName);
							services.append(");\n");
							// add it to the providers (if it is not a module)
							if (!"aggridservice".equals(service.getPackageName()) && moduleName == null)
							{
								providers.append(serviceName);
								providers.append(",\n");
							}
						}
					});

					imports.append("// generated imports end");
					services.append("// generated services end");
					providers.append("// generated providers end");
					modules.append("// generated modules end");

					String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/allservices.service.ts"), "UTF-8");
					String old = content;
					content = replace(content, "// generated imports start", "// generated imports end", imports);
					content = replace(content, "// generated services start", "// generated services end", services);
					content = replace(content, "// generated providers start", "// generated providers end", providers);
					content = replace(content, "// generated modules start", "// generated modules end", modules);
					if (!old.equals(content))
					{
						sourceChanged = true;
						writeConsole(console, "- services ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/allservices.service.ts"), content, "UTF-8");
					}
				}
				catch (IOException e)
				{
					writeErrorToConsoleAndLog(console, e, "Exception while generating allservices.service.ts: ");
					// TODO should we return a WARNING status here as we do for other failures?
				}

				for (WebObjectSpecification spec : WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications())
				{
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					if (warExportModel == null || warExportModel.getAllExportedComponents().contains(spec.getName()))
					{
						Set<CssLib> libs = spec.getNG2Config().getDependencies().getCssLibrary();
						if (libs != null)
						{
							cssLibs.addAll(libs);
						}
						List<String> assets = spec.getNG2Config().getAssets().getAssetsList();
						if (assets != null)
						{
							assetsToAdd.addAll(assets);
						}
					}
				}

				ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
				Pair<StringBuilder, StringBuilder> componentTemplates = generator.generateHTMLTemplate(warExportModel);
				// SVY-21460: the stateless formtemplate copy also stamps data-svy-name/data-svy-id on the
				// component element itself; runtime and designer copies keep using componentTemplates.
				Pair<StringBuilder, StringBuilder> formTemplateComponentTemplates = generator.generateHTMLTemplate(warExportModel, true);
				try
				{
					// adjust component templates
					String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/form/form_component.component.ts"), "UTF-8");
					String editorContent = FileUtils.readFileToString(new File(projectFolder, "src/designer/designform_component.component.ts"), "UTF-8");
					String old = content;
					String oldEditor = editorContent;
					content = replace(content, "<!-- component template generate start -->", "<!-- component template generate end -->",
						componentTemplates.getLeft());
					content = replace(content, "// component viewchild template generate start", "// component viewchild template generate end",
						componentTemplates.getRight());
					editorContent = replace(editorContent, "<!-- component template generate start -->", "<!-- component template generate end -->",
						componentTemplates.getLeft());
					editorContent = replace(editorContent, "// component viewchild template generate start", "// component viewchild template generate end",
						componentTemplates.getRight());
					// add structure templates
					if (structureTagNames.size() > 0)
					{
						LayoutTemplates generateStructureTemplate = generateStructureTemplate(structureTagNames);
						content = replace(content, "<!-- structure template generate start -->", "<!-- structure template generate end -->",
							generateStructureTemplate.getFormComponentTemplate());
						content = replace(content, "// structure viewchild template generate start", "// structure viewchild template generate end",
							generateStructureTemplate.getViewChilds());

						editorContent = replace(editorContent, "<!-- structure template generate start -->", "<!-- structure template generate end -->",
							generateStructureTemplate.getFormComponentTemplate());
						editorContent = replace(editorContent, "// structure viewchild template generate start", "// structure viewchild template generate end",
							generateStructureTemplate.getViewChilds());

						String lfc = FileUtils.readFileToString(new File(projectFolder, "src/servoycore/listformcomponent/listformcomponent.ts"), "UTF-8");
						String oldLFC = lfc;
						lfc = replace(lfc, "<!-- structure template generate start -->", "<!-- structure template generate end -->",
							generateStructureTemplate.getLFFormComponentTemplate());
						lfc = replace(lfc, "// structure viewchild template generate start", "// structure viewchild template generate end",
							generateStructureTemplate.getViewChilds());
						if (!oldLFC.equals(lfc))
						{
							sourceChanged = true;
							writeConsole(console, "- LFC components ts file changed");
							FileUtils.writeStringToFile(new File(projectFolder, "src/servoycore/listformcomponent/listformcomponent.ts"), lfc, "UTF-8");
						}

					}
					if (!old.equals(content))
					{
						sourceChanged = true;
						writeConsole(console, "- components ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/form/form_component.component.ts"), content, "UTF-8");
					}

					if (!oldEditor.equals(editorContent))
					{
						sourceChanged = true;
						writeConsole(console, "- editor ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/designer/designform_component.component.ts"), editorContent, "UTF-8");
					}

					// SVY-21460: keep the stateless form-template copy's component/structure/viewchild
					// marker regions filled the same way as the runtime and designer form components
					File formTemplateFile = new File(projectFolder, "src/formtemplate/formtemplate_component.component.ts");
					if (formTemplateFile.exists())
					{
						String formTemplateContent = FileUtils.readFileToString(formTemplateFile, "UTF-8");
						String oldFormTemplate = formTemplateContent;
						formTemplateContent = replace(formTemplateContent, "<!-- component template generate start -->",
							"<!-- component template generate end -->", formTemplateComponentTemplates.getLeft());
						formTemplateContent = replace(formTemplateContent, "// component viewchild template generate start",
							"// component viewchild template generate end", formTemplateComponentTemplates.getRight());
						if (structureTagNames.size() > 0)
						{
							LayoutTemplates generateStructureTemplate = generateStructureTemplate(structureTagNames);
							formTemplateContent = replace(formTemplateContent, "<!-- structure template generate start -->",
								"<!-- structure template generate end -->", generateStructureTemplate.getFormComponentTemplate());
							formTemplateContent = replace(formTemplateContent, "// structure viewchild template generate start",
								"// structure viewchild template generate end", generateStructureTemplate.getViewChilds());
						}
						if (!oldFormTemplate.equals(formTemplateContent))
						{
							sourceChanged = true;
							writeConsole(console, "- form template ts file changed");
							FileUtils.writeStringToFile(formTemplateFile, formTemplateContent, "UTF-8");
						}
					}

				}
				catch (IOException e1)
				{
					writeErrorToConsoleAndLog(console, e1, "Exception while adjusting component templates: ");
					// TODO should we return a WARNING status here as we do for other failures?
				}

				try
				{


					// generate the all components.module.ts
					String current = generateAllComponentsModuleContent(componentPackageSpecToReader.keySet());
					String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/allcomponents.module.ts"), "UTF-8");

					if (!current.equals(content))
					{
						sourceChanged = true;
						writeConsole(console, "- component modules ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/allcomponents.module.ts"), current, "UTF-8");
					}
				}
				catch (IOException e1)
				{
					writeErrorToConsoleAndLog(console, e1, "Exception while generating allcomponents.module.ts: ");
					// TODO should we return a WARNING status here as we do for other failures?
				}

				try
				{
					/* component/services imports */
					if (cssLibs.size() > -0)
					{
						File angularJSON = new File(projectFolder, "angular.json");
						String angularJSONContents = FileUtils.readFileToString(angularJSON, "UTF8");
						JSONObject json = new JSONObject(angularJSONContents);
						JSONArray styles = json.getJSONObject("projects").getJSONObject("ngclient2").getJSONObject("architect").getJSONObject("build")
							.getJSONObject("options").getJSONArray("styles");
						boolean[] stylesChanged = new boolean[] { false };
						if (!hasFontAwesomePro && cssLibs.size() + 2 != styles.length())
						{
							stylesChanged[0] = true;
						}
						else
						{
							for (CssLib style : cssLibs)
							{
								boolean[] styleFound = new boolean[] { false };
								String styleUrl = style.getUrl().replace("~", "./node_modules/");
								styles.forEach(existingStyle -> {
									if (existingStyle.toString().equals(styleUrl))
									{
										styleFound[0] = true;
									}
								});
								if (!styleFound[0])
								{
									stylesChanged[0] = true;
									break;
								}
							}
						}
						if (stylesChanged[0])
						{
							// just write everything back to be sure the priority is respected
							writeConsole(console, "- styles source changed");
							sourceChanged = true;
							while (styles.length() > 0)
							{
								styles.remove(0);
							}
							if (!hasFontAwesomePro) styles.put("@fortawesome/fontawesome-free/css/all.css");
							for (CssLib style : cssLibs)
							{
								styles.put(style.getUrl().replace("~", "./node_modules/"));
							}
							styles.put("src/styles.css");
							FileUtils.write(angularJSON, json.toString(1), "UTF8", false);
						}
					}
				}
				catch (IOException e)
				{
					writeErrorToConsoleAndLog(console, e, "Exception while adjusting angular.json styles: ");
					// TODO should we return a WARNING status here as we do for other failures?
				}
				try
				{
					if (assetsToAdd.size() > 0)
					{
						File angularJSON = new File(projectFolder, "angular.json");
						String angularJSONContents = FileUtils.readFileToString(angularJSON, "UTF8");
						JSONObject json = new JSONObject(angularJSONContents);
						JSONArray assets = json.getJSONObject("projects").getJSONObject("ngclient2").getJSONObject("architect").getJSONObject("build")
							.getJSONObject("options").getJSONArray("assets");
						boolean[] assetsChanged = new boolean[] { false };
						assetsToAdd.forEach((asset) -> {
							boolean[] assetFound = new boolean[] { false };
							assets.forEach(existingAsset -> {
								if (existingAsset.toString().equals(asset))
								{
									assetFound[0] = true;
								}
							});
							if (!assetFound[0])
							{
								assetsChanged[0] = true;
								if (asset.startsWith("{") && asset.endsWith("}"))
								{
									try
									{
										JSONObject assetJSONObject = new JSONObject(asset);
										assets.put(assetJSONObject);
									}
									catch (JSONException ex)
									{
										writeErrorToConsoleAndLog(console, ex, "Can't add asset object to angular.json: " + asset + ": ");
										// TODO should we return a WARNING status here as we do for other failures?
									}
								}
								else
								{
									assets.put(asset);
								}
							}
						});
						if (assetsChanged[0])
						{
							writeConsole(console, "- assets source changed");
							sourceChanged = true;
							FileUtils.write(angularJSON, json.toString(1), "UTF8", false);
						}
					}
				}
				catch (IOException e)
				{
					writeErrorToConsoleAndLog(console, e, "Exception while checking assets: ");
					// TODO should we return a WARNING status here as we do for other failures?
				}
				// Only ever consider bundling zone.js on the developer-run build; a WAR export (warExportModel != null,
				// both from Developer and from the cloud) always stays zoneless regardless of the system property.
				// On the developer run: if -Dti.ng.includezonejs is set we honour it (including "false"); if it is not
				// set at all it defaults to true, so a developer run restores async patching by default and NG0100
				// ExpressionChangedAfterItHasBeenCheckedError warnings keep surfacing (debugging aid).
				if (warExportModel == null && "true".equals(System.getProperty("ti.ng.includezonejs", "true")))
				{
					// The app runs zoneless, so a fresh source copy has an empty src/polyfills.ts (no zone.js).
					// We add the zone.js import so async patching is restored.
					try
					{
						File polyfills = new File(projectFolder, "src/polyfills.ts");
						String polyfillsContents = polyfills.exists() ? FileUtils.readFileToString(polyfills, "UTF8") : "";
						if (!polyfillsContents.contains("import 'zone.js'"))
						{
							writeConsole(console, "- adding zone.js import to src/polyfills.ts (developer run, ti.ng.includezonejs not false)");
							sourceChanged = true;
							FileUtils.write(polyfills, "import 'zone.js';\r\n" + polyfillsContents, "UTF8", false);
						}
					}
					catch (IOException e)
					{
						writeErrorToConsoleAndLog(console, e, "Exception while adding zone.js to src/polyfills.ts: ");
						// TODO should we return a WARNING status here as we do for other failures?
					}
				}
				if (packageToInstall.size() > 0 || sourceChanged || !new File(projectFolder, "dist").exists() || cleanInstall.get())
				{
					if (warExportModel == null)
					{
						Display.getDefault().asyncExec(() -> {
							if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null &&
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null)
							{
								IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
								for (IEditorReference editorRef : editorRefs)
								{
									IEditorPart editor = editorRef.getEditor(false);
									if (editor instanceof IEditorRefresh)
									{
										((IEditorRefresh)editor).refresh();
									}
								}
							}
						});
					}
					// SVY-21502: in pnpm mode, before the first package-mutating pnpm command, generate the
					// solution-local pnpm-workspace.yaml from the KNOWN committed allow/deny lists so already-decided
					// build scripts run. The web-package tarballs installed below pull in NEW dependencies whose build
					// scripts can only be discovered after they are installed, so the install block runs inside a
					// strictDepBuilds-demotion window (so a brand-new pending script does not hard-fail), and
					// resolvePnpmBuildScriptApprovals (discover + prompt + rebuild) runs right after it.
					boolean pnpmMode = Activator.isPnpmMode();
					boolean[] installBlockSucceeded = new boolean[] { false };
					// SVY-21502 §3.10/§6: a headless WAR export that reaches an undecided name@version build script
					// must FAIL LOUD (not silently run or skip). resolvePnpmBuildScriptApprovals returns such a
					// failure status; it is captured here (it runs in the finally below) and returned after the block.
					IStatus[] approvalsStatus = new IStatus[] { Status.OK_STATUS };
					if (pnpmMode)
					{
						applyKnownPnpmBuildApprovals(this.projectFolder, this.warExportModel, this.approvalsSolutionName, console);
						RunPNPMCommand.beginBuildScriptDiscoveryDemotion();
					}
					try
					{
						// fist create the servoy/public dist if needed
						String location = Activator.getInstance().getBundle().getLocation();
						int fromSourceIndex = location.indexOf("file:/");
						if (fromSourceIndex > 0 || !new File(this.projectFolder, "dist-public").exists() || sourceChanged)
						{
							IRunNPMCommand npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder,
								Arrays.asList("run", "build_lib_debug_nowatch"));
							try
							{
								npmCommand.runCommand(monitor);
								if (npmCommand.getExitCode() != 0)
								{
									writeConsole(console,
										"\r\n" + "Unexpected EXIT_CODE calling build_lib_debug_nowatch: " + npmCommand.getExitCode() + "\r\n");
									return new Status(IStatus.WARNING, getClass(),
										"npm run build_lib_debug_nowatch EXIT_CODE was: " + npmCommand.getExitCode());
								}
							}
							catch (Exception e)
							{
								writeErrorToConsoleAndLog(console, e, "Exception while running 'npm run build_lib_debug_nowatch': ");
								return new Status(IStatus.WARNING, getClass(), "npm run build_lib_debug_nowatch failed: " + e.getMessage());
							}
						}

						// SVY-21502 §3.11: a WAR export / headless CI build installs FROZEN against the committed,
						// matched package.json + pnpm-lock.yaml pair (both seeded above). pnpm's --frozen-lockfile
						// compares the package.json specifier set against the lockfile, so the seeded package.json MUST
						// declare exactly the component tarballs the seeded lockfile pins. The committed pair is the
						// FULL component set; the WAR ships only the exported subset, so we first 'pnpm remove' the
						// components this war does not export (verified to not re-resolve/drift the remaining pins) so
						// package.json and the lockfile agree on the subset, then install frozen. We must NOT run an
						// unfrozen 'pnpm add <tarballs>' - that would re-resolve and rewrite the seeded lockfile,
						// defeating the version pin. A lockfile-vs-package.json mismatch then fails loud via the
						// non-zero exit code checked below. The Developer path stays NON-frozen (its install is what
						// updates and commits-back the full pair).
						boolean frozenWarInstall = Activator.isPnpmMode() && this.warExportModel != null;
						IRunNPMCommand npmCommand;
						if (!frozenWarInstall)
						{
							// Developer (and npm-mode) path: add the changed/new packages (unfrozen).
							// only execute this if a source is changed (should always happens the first time)
							// or if there are really packages to install.
							List<String> command = new ArrayList<>();
							command.add("install");
							packageToInstall.forEach(packageName -> command.add(packageName));
							// also add the public api: in pnpm mode a directory dep does not bring its transitive deps, so pack
							// dist-public into a tarball; in npm mode keep installing the directory as before.
							if (Activator.isPnpmMode())
							{
								File publicTarball = packPackage(new File(this.projectFolder, "dist-public"), "@servoy/public", console);
								if (publicTarball != null)
								{
									// SVY-21502: pass the tarball RELATIVE to the project folder (./.tarballs/<name>.tgz),
									// not an absolute path. pnpm records the 'add' argument verbatim as the lockfile
									// 'specifier:', so an absolute C:\... path would make the committed lockfile
									// machine- and OS-specific (it must work on a Linux WAR exporter). A relative path
									// makes the lockfile portable and identical across the dev and dist target folders.
									command.add(relativeTarballSpecifier(publicTarball));
								}
								else
								{
									writeConsole(console, "- WARNING: could not pack dist-public into a tarball; falling back to directory install");
									command.add("./dist-public/");
								}
							}
							else
							{
								command.add("./dist-public/");
							}
							npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder, command);
							try
							{
								npmCommand.runCommand(monitor);
								if (npmCommand.getExitCode() != 0)
								{
									writeConsole(console,
										"\r\n" + "Unexpected EXIT_CODE calling npm install [many packages that are needed]: " + npmCommand.getExitCode() +
											"\r\n");
									return new Status(IStatus.WARNING, getClass(),
										"npm install [many packages that are needed] EXIT_CODE was: " + npmCommand.getExitCode());
								}
							}
							catch (Exception e)
							{
								writeErrorToConsoleAndLog(console, e, "Exception while running 'npm install many, packages, that, we, need': ");
								return new Status(IStatus.WARNING, getClass(), "npm install many, packages, that, we, need failed: " + e.getMessage());
							}
						}
						else
						{
							// WAR/CI path (SVY-21502 §3.11): the frozen 'ci' below installs everything (registry + tarballs)
							// from the committed, matched package.json + lockfile pair; pack dist-public so its tarball exists
							// on disk for the lockfile's file: specifier to resolve, but do NOT run an unfrozen add. The
							// seeded pair is the FULL component set, so trim the components this war does not export (their
							// tarball was not packed into .tarballs) from package.json + lockfile via 'pnpm remove' - this does
							// not re-resolve the remaining pinned versions - so package.json and the lockfile agree on the
							// shipped subset and the following frozen 'ci' passes instead of failing ERR_PNPM_OUTDATED_LOCKFILE.
							packPackage(new File(this.projectFolder, "dist-public"), "@servoy/public", console);
							Status removeStatus = removeNonExportedComponents(monitor, console);
							if (removeStatus != null) return removeStatus;
							npmCommand = null;
						}
						if (cleanInstall.get() || frozenWarInstall)
						{
							cleanInstall.set(false);
							npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder, Arrays.asList("ci"));
							try
							{
								npmCommand.runCommand(monitor);
								if (npmCommand.getExitCode() != 0)
								{
									writeConsole(console,
										"\r\n" + "Unexpected EXIT_CODE calling npm ci: " + npmCommand.getExitCode() + "\r\n");
									return new Status(IStatus.WARNING, getClass(), "npm ci EXIT_CODE was: " + npmCommand.getExitCode());
								}
							}
							catch (Exception e)
							{
								writeErrorToConsoleAndLog(console, e, "Exception while running 'npm ci': ");
								return new Status(IStatus.WARNING, getClass(), "npm ci failed: " + e.getMessage());
							}
						}
						else
						{
							npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder, Arrays.asList("update"));
							try
							{
								npmCommand.runCommand(monitor);
								if (npmCommand.getExitCode() != 0)
								{
									writeConsole(console,
										"\r\n" + "Unexpected EXIT_CODE calling npm update: " + npmCommand.getExitCode() + "\r\n");
									return new Status(IStatus.WARNING, getClass(), "npm update EXIT_CODE was: " + npmCommand.getExitCode());
								}
							}
							catch (Exception e)
							{
								writeErrorToConsoleAndLog(console, e, "Exception while running 'npm update': ");
								return new Status(IStatus.WARNING, getClass(), "npm update failed: " + e.getMessage());
							}
						}
						// pnpm's global store + symlinked node_modules already deduplicate, so in pnpm mode neither the
						// 'dedup' verb nor the hand-written folder-deletion pass below runs.
						if (!Activator.isPnpmMode())
						{
							npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder, Arrays.asList("dedup"));
							try
							{
								npmCommand.runCommand(monitor);
								if (npmCommand.getExitCode() != 0)
								{
									writeConsole(console,
										"\r\n" + "Unexpected EXIT_CODE calling npm dedup: " + npmCommand.getExitCode() + "\r\n");
									return new Status(IStatus.WARNING, getClass(), "npm dedup EXIT_CODE was: " + npmCommand.getExitCode());
								}
							}
							catch (Exception e)
							{
								writeErrorToConsoleAndLog(console, e, "Exception while running 'npm dedup': ");
								return new Status(IStatus.WARNING, getClass(), "npm dedup failed: " + e.getMessage());
							}
							long dedupTime = System.currentTimeMillis();
							// after dedup we have to run our own dedup, but then compared to the root node_modules
							File projectNodeModules = new File(this.projectFolder, "node_modules");
							File rootNodeModules = new File(this.projectFolder.getParentFile(), "node_modules");

							File[] projectListing = projectNodeModules.listFiles();
							File[] rootListing = rootNodeModules.listFiles();

							BiFunction<File[], File[], SortedList<File>> filterFunction = (list1, list2) -> {
								Comparator<File> comparator = (file1, file2) -> file1.getName().compareToIgnoreCase(file2.getName());
								SortedList<File> result = new SortedList<File>(comparator, Arrays.asList(list1));
								result.retainAll(Arrays.asList(list2));
								return result;
							};

							if (projectListing != null && rootListing != null)
							{
								SortedList<File> mainDirs = filterFunction.apply(projectListing, rootListing);

								mainDirs.forEach(file -> {
									if (file.isDirectory())
									{
										if (new File(file, "package.json").exists())
										{
											// this is already the package (root of node modules, like rxjs)
											try
											{
												Files.walkFileTree(file.toPath(), DeletePathVisitor.INSTANCE);
											}
											catch (IOException e)
											{
												writeErrorToConsoleAndLog(console, e, "Exception while deleting node_modules: ");
											}
										}
										else
										{
											// sub dirs are the package, this is a group dir like angular/aggrid
											SortedList<File> nestedResult = filterFunction.apply(file.listFiles(),
												new File(rootNodeModules, file.getName()).listFiles());
											nestedResult.forEach(nested -> {
												if (nested.isDirectory() && new File(nested, "package.json").exists())
												{
													try
													{
														Files.walkFileTree(nested.toPath(), DeletePathVisitor.INSTANCE);
													}
													catch (IOException e)
													{
														writeErrorToConsoleAndLog(console, e, "Exception while deleting node_modules: ");
													}
												}
											});
										}

									}
								});
							}

							writeConsole(console,
								"Node NPM dedup time (root node_modules/solution node_modules): " +
									Math.round((System.currentTimeMillis() - dedupTime) / 1000) +
									" s.");
						}
						installBlockSucceeded[0] = true;
					}
					finally
					{
						// SVY-21502: close the install-only strictDepBuilds demotion window. When the installs
						// succeeded, node_modules is materialized, so discover the (newly-appeared) pending build
						// scripts, prompt the developer for the undecided ones (Developer UI only), persist, regenerate
						// the allowBuilds yaml and 'pnpm rebuild' so the approved scripts run - all BEFORE the Angular
						// build below, which then runs with the normal strictDepBuilds: true and the scripts already
						// built. If an install failed (early return), only the demotion window is closed.
						if (pnpmMode)
						{
							RunPNPMCommand.endBuildScriptDiscoveryDemotion();
							if (installBlockSucceeded[0])
							{
								approvalsStatus[0] = resolvePnpmBuildScriptApprovals(this.projectFolder, this.warExportModel,
									this.approvalsSolutionName, monitor, console);
							}
						}
					}

					// SVY-21502 §6: in a headless WAR export a non-OK approvals status (an undecided name@version was
					// reached) is a hard build failure - propagate it so the exporter surfaces the error and the
					// developer opens the solution, approves/denies the listed versions and commits. The Developer path
					// never returns a non-OK status here (it prompts instead), so this does not affect it.
					if (pnpmMode && approvalsStatus[0] != null && approvalsStatus[0].getSeverity() == IStatus.ERROR)
					{
						return approvalsStatus[0];
					}

					if (copyAngularLocales(this.projectFolder))
					{
						writeConsole(console, "- copying Angular locale files to locales/angular");
					}
					else
					{
						writeConsole(console, "- WARNING: Angular locale files not found or copy failed, skipping");
					}

					if (SOURCE_DEBUG)
					{
						writeConsole(console,
							"SOURCE DEBUG enabled (ti.ng.source.debug=true); skipping npm run build_debug_nowatch; you need to run it manually YOURSELF. (npm install did happen)");
					}
					else
					{
						IRunNPMCommand npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder, Arrays.asList("run", whatToRun));
						try
						{
							npmCommand.runCommand(monitor);
							if (npmCommand.getExitCode() != 0)
							{
								writeConsole(console,
									"\r\n" + "Unexpected EXIT_CODE calling npm " + whatToRun + ": " + npmCommand.getExitCode() + "\r\n");
								return new Status(IStatus.WARNING, getClass(), "npm run " + whatToRun + " EXIT_CODE was: " + npmCommand.getExitCode());
							}
						}
						catch (Exception e)
						{
							writeErrorToConsoleAndLog(console, e, "Exception while running 'npm run " + whatToRun + "': ");
							return new Status(IStatus.WARNING, getClass(), "npm run " + whatToRun + " failed: " + e.getMessage());
						}
					}
				}
				else writeConsole(console, "Skipping install / build as no changes were found.");
				writeConsole(console, "Total time to check/install Titanium NG solution/dependencies into target folder: " + projectFolder + " is " +
					Math.round((System.currentTimeMillis() - time) / 1000) + " s.\n");
				return Status.OK_STATUS;
			}
			finally
			{
				try
				{
					console.close();
				}
				catch (IOException e)
				{
				}
				// remove the schedules 1 and start + 1 from the scheduled.
				// if that is still higher then 0 we need to start another.
				if (scheduled.addAndGet(-2) <= 0)
				{
					// this can be a WAR export, then it was 1 not 2 when running.
					scheduled.set(0);
				}
				else
				{
					// first just set it now to 0 to start over.
					scheduled.set(0);
					checkPackages(false);
				}
			}
		}

		/*
		 * map<templateName , Pair<spec, tagName>>
		 */
		private LayoutTemplates generateStructureTemplate(Map<String, Pair<WebLayoutSpecification, String>> tags)
		{
			StringBuilder template = new StringBuilder();
			StringBuilder viewChild = new StringBuilder();
			StringBuilder templateLFC = new StringBuilder();

			template.append("<!-- structure template generate start -->\n");
			templateLFC.append("<!-- structure template generate start -->\n");
			viewChild.append("// structure viewchild template generate start\n");

			tags.forEach((templateName, pair) -> {
				template.append("<ng-template  #svyResponsive");
				template.append(templateName);
				template.append("  let-state=\"state\" >\n<");
				template.append(pair.getRight());
				pair.getLeft().getDirectives().forEach(directive -> template.append(' ').append(directive));
				template.append(
					" [svyContainerStyle]=\"state\" [svyContainerClasses]=\"state.classes\" [svyContainerAttributes]=\"state.attributes\"  class=\"svy-layoutcontainer\">\n");
				template.append(
					"@for (item of state.items; track item) {<ng-template [ngTemplateOutlet]=\"getTemplate(item)\" [ngTemplateOutletContext]=\"{ state:item, callback:this}\"></ng-template>}\n</");
				template.append(pair.getRight());
				template.append(">\n</ng-template>\n");

				templateLFC.append("<ng-template  #svyResponsive");
				templateLFC.append(templateName);
				templateLFC.append("  let-state=\"state\" let-row=\"row\" let-i=\"i\">\n<");
				templateLFC.append(pair.getRight());
				pair.getLeft().getDirectives().forEach(directive -> templateLFC.append(' ').append(directive));
				templateLFC.append(
					" [svyContainerStyle]=\"state\" [svyContainerClasses]=\"state.classes\" [svyContainerAttributes]=\"state.attributes\"  class=\"svy-layoutcontainer\">\n");
				templateLFC.append(
					"@for (item of state.items; track item) {<ng-template [ngTemplateOutlet]=\"getRowItemTemplate(item)\" [ngTemplateOutletContext]=\"{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}\"></ng-template>}\n</");
				templateLFC.append(pair.getRight());
				templateLFC.append(">\n</ng-template>\n");

				viewChild.append("readonly svyResponsive");
				viewChild.append(templateName);
				viewChild.append(" = viewChild<TemplateRef<any>>('svyResponsive");
				viewChild.append(templateName);
				viewChild.append("');\n");
			});
			template.append("<!-- structure template generate end -->");
			templateLFC.append("<!-- structure template generate end -->");
			viewChild.append("// structure viewchild template generate end");
			return new LayoutTemplates()
			{
				@Override
				public CharSequence getViewChilds()
				{
					return viewChild;
				}

				public CharSequence getLFFormComponentTemplate()
				{
					return templateLFC;
				}

				@Override
				public CharSequence getFormComponentTemplate()
				{
					return template;
				}
			};
		}

		private void writeConsole(StringOutputStream console, String message)
		{
			try
			{
				console.write(message + "\n");
			}
			catch (IOException e)
			{
			}
		}

		private void writeErrorToConsoleAndLog(StringOutputStream console, Exception e, String s)
		{
			Activator.getInstance().getLog().error(s, e);
			writeConsole(console, "\r\n" + s + e.getMessage() + "\r\n");
		}

		/**
		 * Resolves what to install for a Servoy web package that lives on disk as a directory (a default source
		 * project, a workspace {@link DirPackageReader} source package, or an extracted {@link ZipPackageReader}
		 * package).
		 * <p>
		 * In npm mode this returns the directory's canonical path, matching the historical behaviour where
		 * <code>npm install &lt;dir&gt;</code> also installs the directory's transitive dependencies.
		 * <p>
		 * In pnpm mode (SVY-21456) a directory dependency is NOT enough: pnpm symlinks a <code>file:</code>/
		 * <code>link:</code> directory dependency but never installs its transitive <code>dependencies</code>, so
		 * component libraries such as <code>@servoy/servoydefault</code> would be missing
		 * <code>@ng-bootstrap/ng-bootstrap</code>, <code>@eonasdan/tempus-dominus</code>, <code>@angular/cdk</code>
		 * etc. and the Angular build fails. Therefore in pnpm mode the directory is packed into a real
		 * <code>.tgz</code> tarball (via the bundled <code>pnpm pack</code>) which pnpm treats as a real package
		 * install and for which it DOES resolve the transitive dependencies. The tarball path is returned so it is
		 * installed via <code>pnpm add &lt;tarball&gt;</code>.
		 *
		 * @return the specifier to install (a directory path in npm mode, a tarball path in pnpm mode), or
		 *         <code>null</code> if packing failed in pnpm mode (the caller then keeps the previous behaviour).
		 */
		private String resolveInstallSpecifier(File packageDir, String packageName, StringOutputStream console)
		{
			if (!Activator.isPnpmMode())
			{
				try
				{
					return packageDir.getCanonicalPath();
				}
				catch (IOException e)
				{
					writeErrorToConsoleAndLog(console, e, "Exception while resolving package dir '" + packageName + "': ");
					return packageDir.getAbsolutePath();
				}
			}
			File tarball = packPackage(packageDir, packageName, console);
			if (tarball != null)
			{
				// SVY-21502: return the tarball RELATIVE to the project folder (./.tarballs/<name>.tgz). pnpm writes
				// the 'add' argument verbatim as the lockfile 'specifier:', so a relative path keeps the committed
				// lockfile portable (no absolute C:\... path that would break a Linux WAR exporter) and identical
				// across the dev and dist target folders.
				return relativeTarballSpecifier(tarball);
			}
			return null;
		}

		/**
		 * SVY-21502 §3.11: on the WAR/CI frozen path, removes from the seeded (FULL-set) <code>package.json</code> and
		 * <code>pnpm-lock.yaml</code> the <code>@servoy/*</code> component dependencies this WAR does NOT export, so the
		 * two files agree on the exported subset before the frozen <code>pnpm install --frozen-lockfile</code> runs
		 * (otherwise it fails <code>ERR_PNPM_OUTDATED_LOCKFILE</code>). A component is considered shipped if its tarball
		 * was packed into <code>.tarballs/</code> by the earlier {@link #checkPackage}/{@link #packPackage} pass (which
		 * already filters on {@code warExportModel.getExportedPackagesExceptSablo()}); any committed
		 * <code>@servoy/*</code> dependency whose tarball is absent is removed. A single <code>pnpm remove</code> drops
		 * exactly those dependencies from both files and does NOT re-resolve or drift the remaining pinned versions
		 * (verified). No-op when nothing needs removing. Returns a non-OK {@link Status} only if the remove command
		 * failed; {@code null} otherwise (the normal case).
		 */
		private Status removeNonExportedComponents(IProgressMonitor monitor, StringOutputStream console)
		{
			if (!Activator.isPnpmMode() || this.warExportModel == null) return null;
			try
			{
				File packageJson = new File(this.projectFolder, PnpmBuildScriptConfig.PACKAGE_JSON);
				if (!packageJson.exists()) return null;
				JSONObject json = new JSONObject(FileUtils.readFileToString(packageJson, "UTF-8"));
				JSONObject dependencies = json.optJSONObject("dependencies");
				if (dependencies == null) return null;
				File tarballsDir = new File(this.projectFolder, ".tarballs");
				List<String> toRemove = new ArrayList<>();
				for (String name : dependencies.keySet())
				{
					// only @servoy/* file: tarball component deps are managed as a packed-or-not subset here
					if (!name.startsWith("@servoy/")) continue;
					String specifier = dependencies.optString(name, "");
					if (!specifier.startsWith("file:")) continue;
					// the committed specifier is file:.tarballs/<tarball>.tgz; the component is shipped only if that
					// tarball was packed into .tarballs for this export
					String tarballName = specifier.substring(specifier.lastIndexOf('/') + 1);
					if (!new File(tarballsDir, tarballName).exists())
					{
						toRemove.add(name);
					}
				}
				if (toRemove.isEmpty()) return null;
				writeConsole(console, "- pnpm mode: removing " + toRemove.size() +
					" component(s) not exported by this WAR from package.json + lockfile: " + toRemove);
				List<String> command = new ArrayList<>();
				command.add("remove");
				command.addAll(toRemove);
				IRunNPMCommand removeCommand = Activator.getInstance().createNPMCommand(this.projectFolder, command);
				removeCommand.runCommand(monitor);
				if (removeCommand.getExitCode() != 0)
				{
					writeConsole(console, "\r\n" + "Unexpected EXIT_CODE calling pnpm remove: " + removeCommand.getExitCode() + "\r\n");
					return new Status(IStatus.WARNING, getClass(), "pnpm remove of non-exported components EXIT_CODE was: " +
						removeCommand.getExitCode());
				}
				return null;
			}
			catch (Exception e)
			{
				writeErrorToConsoleAndLog(console, e, "Exception while removing non-exported components on the WAR path: ");
				return new Status(IStatus.WARNING, getClass(), "removing non-exported components failed: " + e.getMessage());
			}
		}

		/**
		 * Packs the given package directory into a <code>.tgz</code> tarball under the project's
		 * <code>.tarballs/</code> folder using the bundled <code>pnpm pack</code>, and returns the resulting file.
		 * <p>
		 * The tarball name embeds a <em>content hash</em> of the package's files (SVY-21502):
		 * <code>&lt;sanitized-package-name&gt;-&lt;contentHash&gt;.tgz</code>. A content-unique name is REQUIRED: pnpm
		 * v11+ treats a <code>file:</code> tarball whose path is unchanged as "already up to date" and skips
		 * re-reading it on a plain <code>pnpm install</code>, so re-packing to a stable name would silently install
		 * stale package contents. A changing name changes the dependency specifier, which forces pnpm to re-resolve
		 * and install the new tarball. The hash is content-based (not mtime-based) so a fresh CI checkout of
		 * byte-identical content yields the SAME name - keeping a committed <code>pnpm-lock.yaml</code> valid across
		 * CI runs. Older tarballs of the same package are deleted so the folder does not grow.
		 * <p>
		 * Only the <em>caller</em> decides whether to re-pack at all (via the existing per-package change detection
		 * in {@link #checkPackage}); this method always produces a fresh tarball when invoked.
		 *
		 * @return the produced tarball file, or <code>null</code> on failure.
		 */
		private File packPackage(File packageDir, String packageName, StringOutputStream console)
		{
			try
			{
				File tarballsDir = new File(projectFolder, ".tarballs");
				if (!tarballsDir.exists()) tarballsDir.mkdirs();

				String sanitized = packageName.replace("@", "").replace('/', '-');
				String contentId = contentHash(packageDir);
				String tarballName = sanitized + "-" + contentId + ".tgz";
				File tarball = new File(tarballsDir, tarballName);

				// remove any previous tarballs of this package (stale timestamps) so the folder stays clean
				deleteStaleTarballs(tarballsDir, sanitized, tarballName);

				if (tarball.exists())
				{
					// already packed for this exact content timestamp; reuse it
					return tarball;
				}

				// 'pnpm pack --pack-destination <tarballsDir>' produces '<name>-<version>.tgz' in tarballsDir;
				// capture what appears and rename it to the content-unique tarballName.
				// NOTE: do NOT pass the shared job 'console' to the pack command via setOutputStream: the runner
				// closes the stream it is given in its finally block, which would close the shared console and swallow
				// all later output (including the final "Total time ..." line). Let the pack command use its own
				// console stream instead (as every other runner call in this job does).
				Set<String> before = listTgzNames(tarballsDir);
				IRunNPMCommand packCommand = Activator.getInstance().createNPMCommand(packageDir,
					Arrays.asList("pack", "--pack-destination", tarballsDir.getCanonicalPath()));
				packCommand.runCommand(new NullProgressMonitor());
				if (packCommand.getExitCode() != 0)
				{
					writeConsole(console, "- WARNING: 'pnpm pack' for " + packageName + " returned EXIT_CODE " + packCommand.getExitCode());
					return null;
				}
				File produced = findNewTgz(tarballsDir, before);
				if (produced == null)
				{
					writeConsole(console, "- WARNING: 'pnpm pack' for " + packageName + " produced no .tgz file");
					return null;
				}
				if (!produced.equals(tarball))
				{
					Files.move(produced.toPath(), tarball.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
				return tarball;
			}
			catch (Exception e)
			{
				writeErrorToConsoleAndLog(console, e, "Exception while packing package '" + packageName + "' into a tarball: ");
				return null;
			}
		}

		/**
		 * A content hash of the package directory used as the tarball name suffix (SVY-21502). The hash is derived
		 * from the package's file contents and their project-relative paths - NOT from file modification times. This
		 * is deliberate: a fresh CI/Jenkins checkout re-writes every file's mtime, so an mtime-based suffix would
		 * produce a different tarball name on every CI run even though the packed content is byte-identical, which
		 * would break a committed {@code pnpm-lock.yaml} (the {@code file:} specifier it pins would no longer match)
		 * and make a frozen WAR/CI install fail for no real change. A content hash is stable across checkouts: the
		 * tarball name only changes when the actual content changes (a Servoy update or a developer-made web-package
		 * change), which is exactly when a developer regenerates and commits the lockfile.
		 * <p>
		 * {@code node_modules} is excluded (it is not part of the packed tarball). A changed digest changes the
		 * dependency specifier and forces pnpm to re-resolve, keeping the "re-pack + re-install when content changed"
		 * behaviour the previous timestamp suffix provided.
		 */
		private String contentHash(File dir)
		{
			Path root = dir.toPath();
			try
			{
				List<File> files = FileUtils.listFiles(dir, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream()
					.filter(f -> !pathContainsNodeModules(root, f))
					.sorted(Comparator.comparing(f -> root.relativize(f.toPath()).toString().replace('\\', '/')))
					.collect(Collectors.toList());
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				for (File f : files)
				{
					String rel = root.relativize(f.toPath()).toString().replace('\\', '/');
					digest.update(rel.getBytes(StandardCharsets.UTF_8));
					digest.update((byte)0);
					digest.update(Files.readAllBytes(f.toPath()));
					digest.update((byte)0);
				}
				byte[] hash = digest.digest();
				StringBuilder sb = new StringBuilder();
				// a 12-hex-char prefix (48 bits) is plenty to distinguish package content revisions
				for (int i = 0; i < 6; i++)
					sb.append(String.format("%02x", Byte.valueOf(hash[i])));
				return sb.toString();
			}
			catch (Exception e)
			{
				writeErrorToConsoleAndLog(Activator.getInstance().getConsole().outputStream(), e,
					"Exception computing content hash for " + dir + "; falling back to a time-based suffix: ");
				// fall back rather than fail the build; a time-based suffix still works within a single workspace,
				// it only loses CI-checkout stability.
				Optional<File> newest = FileUtils.listFiles(dir, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream()
					.filter(f -> !f.getName().equals("node_modules"))
					.max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
				return "t" + (newest.isPresent() ? newest.get().lastModified() : dir.lastModified());
			}
		}

		private static boolean pathContainsNodeModules(Path root, File f)
		{
			Path rel = root.relativize(f.toPath());
			for (Path seg : rel)
			{
				if ("node_modules".equals(seg.toString())) return true;
			}
			return false;
		}

		private void deleteStaleTarballs(File tarballsDir, String sanitizedName, String keepName)
		{
			File[] files = tarballsDir.listFiles((d, name) -> name.startsWith(sanitizedName + "-") && name.endsWith(".tgz") && !name.equals(keepName));
			if (files != null)
			{
				for (File f : files)
					f.delete();
			}
		}

		/**
		 * Returns the tarball as a dependency specifier RELATIVE to the solution's pnpm project folder, using forward
		 * slashes, e.g. <code>./.tarballs/servoy-public-&lt;hash&gt;.tgz</code> (SVY-21502). The tarball always lives
		 * under <code>&lt;projectFolder&gt;/.tarballs/</code> and <code>pnpm add</code> runs in
		 * <code>projectFolder</code>, so this path is valid in both the dev and dist target folders. It is passed to
		 * <code>pnpm add</code>, which records it verbatim as the lockfile <code>specifier:</code> - a relative,
		 * forward-slashed value keeps the committed <code>pnpm-lock.yaml</code> portable across machines and OSes
		 * (a WAR export may run on Linux), whereas an absolute <code>C:\...</code> path would not.
		 */
		private String relativeTarballSpecifier(File tarball)
		{
			String rel = projectFolder.toPath().relativize(tarball.toPath()).toString().replace('\\', '/');
			// pnpm wants an explicit relative marker for a local path specifier
			return rel.startsWith(".") ? rel : "./" + rel;
		}

		private Set<String> listTgzNames(File dir)
		{
			Set<String> names = new HashSet<>();
			File[] files = dir.listFiles((d, name) -> name.endsWith(".tgz"));
			if (files != null)
			{
				for (File f : files)
					names.add(f.getName());
			}
			return names;
		}

		private File findNewTgz(File dir, Set<String> before)
		{
			File[] files = dir.listFiles((d, name) -> name.endsWith(".tgz"));
			if (files == null) return null;
			for (File f : files)
			{
				if (!before.contains(f.getName())) return f;
			}
			return null;
		}

		/**
		 * Whether the package is already installed with the given specifier. In npm mode this keeps the historical
		 * suffix check against the directory path. In pnpm mode the tarball name embeds a content hash, so this
		 * checks that the recorded specifier points at the tarball for the current content hash; if the content
		 * changed the hash (and thus the name) differs and the package is treated as not-yet-installed, forcing
		 * a re-pack + re-install.
		 */
		private boolean isAlreadyInstalled(String installedVersion, String dirSuffix, String packageName, File packageDir)
		{
			if (Activator.isPnpmMode())
			{
				if (installedVersion == null || packageDir == null) return false;
				String sanitized = packageName.replace("@", "").replace('/', '-');
				String currentTarballName = sanitized + "-" + contentHash(packageDir) + ".tgz";
				return installedVersion.endsWith(currentTarballName);
			}
			return installedVersion != null && installedVersion.endsWith(dirSuffix);
		}

		private String checkPackage(JSONObject dependencies, String packageName, IPackageReader packageReader, String entryPoint, StringOutputStream console)
		{
			String packageVersion = packageReader.getVersion();
			if (entryPoint != null)
			{
				if (Arrays.binarySearch(defaultPackages, packageName) >= 0)
				{
					// test if this internal package should be ignored:
					if (isDefaultPackageEnabled(packageName))
					{
						File packageFolder = new File(projectFolder, entryPoint);
						File tsConfig = new File(projectFolder, "tsconfig.json");
						try
						{
							String tsConfigContents = FileUtils.readFileToString(tsConfig, "UTF8");
							JSONObject json = new JSONObject(tsConfigContents);
							JSONObject paths = json.getJSONObject("compilerOptions").getJSONObject("paths");
							if (!paths.has(packageName))
							{
								JSONArray array = new JSONArray();
								array.put(new File(packageFolder, "src/public-api").getCanonicalPath());
								paths.put(packageName, array);
								FileUtils.write(tsConfig, json.toString(1), "UTF8", false);
							}
							String installedVersion = dependencies.optString(packageName);
							if (!isAlreadyInstalled(installedVersion, entryPoint, packageName, packageFolder))
							{
								return resolveInstallSpecifier(packageFolder, packageName, console);
							}
							return null;
						}
						catch (Exception e)
						{
							writeErrorToConsoleAndLog(console, e, "Exception while checking package '" + packageName + "': ");
							// TODO should we return a WARNING status here for the whole job as we do for other failures?
						}
					}
					else return null;
				}
				// its a file based service (something installed in the workspace as a source project)
				else if (packageReader instanceof DirPackageReader)
				{
					try
					{
						IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
						IContainer[] containers = root.findContainersForLocationURI(packageReader.getPackageURL().toURI());
						if (containers != null && containers.length > 0)
						{
							try
							{
								IProject project = null;
								if (containers.length == 1)
								{
									project = containers[0].getProject();
								}
								else
								{
									for (IContainer container : containers)
									{
										if (container instanceof IProject)
										{
											project = (IProject)container;
											break;
										}
									}
									if (project == null) project = containers[0].getProject();
								}
								IFile sourcePath = project.getFile(".sourcepath");
								if (sourcePath.exists())
								{
									File packagesFolder = new File(projectFolder, "packages");
									File packageFolder = new File(packagesFolder, packageName);

									// quickly check tsconfig.json to get rid of warnings
									File tsconfig = new File(packagesFolder, "tsconfig.json");
									if (!tsconfig.exists())
									{
										FileUtils.copyFile(new File(projectFolder, "tsconfig.json"), tsconfig);
									}

									JSONObject sourcePathJson = null;
									String sourcePathContents = FileUtils.readFileToString(sourcePath.getLocation().toFile(), "UTF8");
									sourcePathJson = new JSONObject(sourcePathContents);
									IFolder file = project.getFolder(sourcePathJson.getString("srcDir"));
									File apiFile = new File(packageFolder, sourcePathJson.getString("apiFile") + ".ts");

									String location = packageFolder.getCanonicalPath();
									File packageJson = new File(packageFolder, "package.json");
									boolean packageJsonChanged = !packageJson.exists();
									long timestamp = 0;
									if (!packageJsonChanged)
									{
										// this only works once at startup, after that the DirectorySync already pushed a new value before this check
										packageJsonChanged = file.getFile("package.json").getLocation().toFile().lastModified() -
											packageJson.lastModified() > 1000;

										if (!packageJsonChanged)
										{
											File timestampFile = new File(packageFolder, ".timestamp");
											if (timestampFile.exists())
											{
												try
												{
													timestamp = Long.parseLong(FileUtils.readFileToString(timestampFile, "UTF8"));
												}
												catch (Exception e)
												{
												}
											}
										}
										else
										{
											writeConsole(console, "- the source of the package.json in " + packageFolder + " was modified");
										}
									}
									else
									{
										writeConsole(console, "- package.json in " + packageFolder + " did not exist; installing this package...");
									}
									// check/copy the dist folder to the target packages location
									if (!WebPackagesListener.watchCreated.containsKey(packageFolder) || !packageFolder.exists() ||
										(apiFile != null && !apiFile.exists()))
									{
										DirectorySync directorySync = WebPackagesListener.watchCreated.get(packageFolder);
										if (directorySync != null) directorySync.destroy();
										if (packageFolder.exists())
										{
											try
											{
												Files.walkFileTree(packageFolder.toPath(), DeletePathVisitor.INSTANCE);
											}
											catch (IOException e)
											{
												writeErrorToConsoleAndLog(console, e,
													"Exception while deleting package folder " + packageFolder + " for replacement: ");
											}
										}

										File srcDir = file.getLocation().toFile();
										FileUtils.copyDirectory(srcDir, packageFolder, (File f) -> {
											// copy all except node_modules (in case an npm install was run in that package), we don't want to waste lots of space and time by copying it over at each developer restart or when this code hits...
											return !("node_modules".equals(f.getName()) && f.getParentFile() != null &&
												new File(f.getParentFile(), "package.json").exists());
										});
										writeConsole(console, "- updated target folder " + packageFolder + " from source package dir " + srcDir);
										WebPackagesListener.watchCreated.put(packageFolder, new DirectorySync(srcDir, packageFolder, null));

										Optional<File> srcMax = FileUtils.listFiles(packageFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream()
											.max((file1, file2) -> {
												long tm = file1.lastModified() - file2.lastModified();
												return tm < 0 ? -1 : tm > 0 ? 1 : 0;
											});

										long tm = srcMax.isPresent() ? srcMax.get().lastModified() : 0;

										if (tm != timestamp)
										{
											writeConsole(console, "- source Package Project changed (" + project.getName() + ") current timestamp: " +
												new Date(tm) + " compared to the stored timestamp " + new Date(timestamp));
											packageJsonChanged = true;
										}
										FileUtils.writeStringToFile(new File(packageFolder, ".timestamp"), Long.toString(tm), "UTF8");
									}
									// also add if this is a src thing to the ts config
									if (sourcePathJson != null)
									{
										File tsConfig = new File(projectFolder, "tsconfig.json");
										String tsConfigContents = FileUtils.readFileToString(tsConfig, "UTF8");
										JSONObject json = new JSONObject(tsConfigContents);
										JSONObject paths = json.getJSONObject("compilerOptions").getJSONObject("paths");
										if (!paths.has(packageName))
										{
											JSONArray array = new JSONArray();
											array.put(new File(packageFolder, sourcePathJson.getString("apiFile")).getCanonicalPath());
											paths.put(packageName, array);
											FileUtils.write(tsConfig, json.toString(1), "UTF8", false);
										}
									}

									String installedVersion = dependencies.optString(packageName);
									if (packageJsonChanged || !isAlreadyInstalled(installedVersion, "packages/" + packageName, packageName, new File(location)))
									{
										return resolveInstallSpecifier(new File(location), packageName, console);
									}
								}
								else
								{
									writeConsole(console, "\nSource Package " + packageName + " (project: " + project +
										") should have a .sourcepath json file in the project root having 2 properties: srcDir (pointing to the root souce dir) and apiFile (pointing to the public api file without extension in that source dir)!\n");
								}
								return null;
							}
							catch (IOException e)
							{
								writeErrorToConsoleAndLog(console, e, "Exception while checking package '" + packageName + "': ");
								// TODO should we return a WARNING status here for the whole job as we do for other failures?
							}
						}
					}
					catch (URISyntaxException e)
					{
						writeErrorToConsoleAndLog(console, e, "Exception while checking package '" + packageName + "': ");
						// TODO should we return a WARNING status here for the whole job as we do for other failures?
					}
				}
				else if (packageReader instanceof ZipPackageReader)
				{

					// this has an entry point in the zip, extract this package.
					File packagesFolder = new File(projectFolder, "packages");
					File packageFolder = new File(packagesFolder, packageName);

					// if we previously had a project reference and now we switched back to the zip we need to remove what was generated for it
					try
					{
						if (WebPackagesListener.watchCreated.containsKey(packageFolder))
						{
							DirectorySync directorySync = WebPackagesListener.watchCreated.remove(packageFolder);
							if (directorySync != null) directorySync.destroy();
							if (packageFolder.exists())
							{
								try
								{
									Files.walkFileTree(packageFolder.toPath(), DeletePathVisitor.INSTANCE);
								}
								catch (IOException e)
								{
									writeErrorToConsoleAndLog(console, e,
										"Exception while deleting package folder (of zip) " + packageFolder + " for replacement: ");
								}
							}

							File tsConfig = new File(projectFolder, "tsconfig.json");
							String tsConfigContents = FileUtils.readFileToString(tsConfig, "UTF8");
							JSONObject json = new JSONObject(tsConfigContents);
							JSONObject paths = json.getJSONObject("compilerOptions").getJSONObject("paths");
							if (paths.has(packageName))
							{
								if (paths.remove(packageName) != null)
								{
									FileUtils.write(tsConfig, json.toString(1), "UTF8", false);
								}
							}
						}
					}
					catch (IOException e)
					{
						writeErrorToConsoleAndLog(console, e, "Exception while checking package '" + packageName + "': ");
						// TODO should we return a WARNING status here for the whole job as we do for other failures?
					}

					boolean exists = packageFolder.exists();
					if (exists)
					{
						File entry = new File(packageFolder, ".timestamp");
						try
						{
							if (!entry.exists() || Long.parseLong(FileUtils.readFileToString(entry, "UTF8")) != packageReader.getResource().lastModified())
							{
								try
								{
									Files.walkFileTree(packageFolder.toPath(), DeletePathVisitor.INSTANCE);
								}
								catch (IOException e)
								{
									writeErrorToConsoleAndLog(console, e,
										"Exception while deleting package folder (zip - timestamp): " + packageFolder + " for replacement: ");
								}

								exists = false;
							}
						}
						catch (Exception e)
						{
							try
							{
								// this could happen if we deleted the war file but undeploy failed once
								Files.walkFileTree(packageFolder.toPath(), DeletePathVisitor.INSTANCE);
							}
							catch (IOException io)
							{
								writeErrorToConsoleAndLog(console, io,
									"Exception while deleting package folder (zip - timestamp - attempt2): " + packageFolder +
										" for replacement: ");
							}
							exists = false;
						}

					}

					try
					{
						if (!exists)
						{
							ZipUtils.extractZip(packageReader.getResource().toURI().toURL(), packageFolder);
							FileUtils.writeStringToFile(new File(packageFolder, ".timestamp"), Long.toString(packageReader.getResource().lastModified()),
								"UTF8");
						}
						File entry = new File(packageFolder, entryPoint);
						if (entry.exists())
						{
							String installedVersion = dependencies.optString(packageName);
							if (!exists || !isAlreadyInstalled(installedVersion, entryPoint, packageName, entry))
							{
								return resolveInstallSpecifier(entry, packageName, console);
							}
							return null;
						}
					}
					catch (IOException e)
					{
						writeErrorToConsoleAndLog(console, e, "Exception while checking package '" + packageName + "': ");
						// TODO should we return a WARNING status here for the whole job as we do for other failures?
					}
				}

				// check for prerelease (npm uses x.y.z-rc1 manifest: x.y.z.rc1)
				if (packageVersion != null)
				{
					String[] split = packageVersion.split("\\.");
					if (split.length == 4)
					{
						packageVersion = split[0] + '.' + split[1] + '.' + split[2] + '-' + split[3];
					}
					String installedVersion = dependencies.optString(packageName);
					if (!installedVersion.contains(packageVersion))
					{
						return packageName + '@' + packageVersion;
					}
				}
			}

			return null;
		}

		private String replace(String content, String start, String end, CharSequence toInsert)
		{
			int startIndex = content.indexOf(start);
			int endIndex = content.indexOf(end) + end.length();
			return content.substring(0, startIndex) + toInsert + content.substring(endIndex);
		}
	}

	private static boolean SOURCE_DEBUG = "true".equals(System.getProperty("ti.ng.source.debug", "false"));

	private static final AtomicBoolean ignore = new AtomicBoolean(false);

	private static final AtomicInteger scheduled = new AtomicInteger(0); // 0 == no jobs, 1 == job scheduled, 2  or 3 == job running, 3 == run again.
	private static final AtomicBoolean cleanInstall = new AtomicBoolean(false);

	private static final ConcurrentMap<File, DirectorySync> watchCreated = new ConcurrentHashMap<>();
	private static final SerialRule serialRule = SerialRule.getNewSerialRule();

	public WebPackagesListener()
	{
		if (WebServiceSpecProvider.isLoaded() && ServoyModelFinder.getServoyModel().getActiveProject() != null)
			createNodeFolderAndCheckPackages();
	}

	@Override
	public void ngPackagesChanged(CHANGE_REASON changeReason, boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
	{
		if (changeReason == CHANGE_REASON.ACTIVE_PROJECT_CHANGED && ServoyModelFinder.getServoyModel().getActiveProject() != null)
		{
			createNodeFolderAndCheckPackages();
		}
		else checkPackages(false);
	}

	protected void createNodeFolderAndCheckPackages()
	{
		// In some automated test runs, the node folder src copy and ng build are not needed.
		// Gate here (at scheduling) rather than only in run(), so the job is never
		// created and never appears in the progress view.
		if (Activator.isNodeExtractionAndTitaniumBuildDisabled()) return;

		final String activeProjectName = ServoyModelFinder.getServoyModel().getActiveProject().getProject().getName();
		Activator.getInstance().setActiveSolution(null);
		NodeFolderCreatorJob job = new NodeFolderCreatorJob(new File(Activator.getInstance().getMainTargetFolder(), activeProjectName), true, false);
		job.addJobChangeListener(new JobChangeAdapter()
		{
			@Override
			public void done(IJobChangeEvent event)
			{
				if (Utils.equalObjects(activeProjectName, ServoyModelFinder.getServoyModel().getActiveProject().getProject().getName()))
				{
					Activator.getInstance().setActiveSolution(ServoyModelFinder.getServoyModel().getActiveProject().getProject().getName());
					if (event.getResult().getSeverity() == IStatus.OK) checkPackages(false);
					else
					{
						// else npm install gave an error...
						handleTitaniumNGClientBuildFailure();
					}
				}
			}
		});
		job.setRule(serialRule);
		job.setUser(false);
		job.schedule();
	}

	public static boolean isBuildRunning()
	{
		return scheduled.get() > 1 || Job.getJobManager().find(CopySourceFolderAction.JOB_FAMILY).length > 0;
	}

	/**
	 * returns true if the given package is enabled, will return false only when the given package is a ng1 default package mapping
	 * and the preference says it is disabled
	 * @param packageName
	 * @return
	 */
	private static boolean isDefaultPackageEnabled(String packageName)
	{
		String ng1Name = NG1MAPPING.get(packageName);
		return ng1Name != null ? ServoyModelFinder.getServoyModel().getNGPackageManager().isDefaultPackageEnabled(ng1Name) : true;
	}

	/**
	 * if true then checkpackage will be blocked until it is called with false again
	 * if false then all call to checkpackage will run a job, this will also trigger a checkPackage itself to make sure that all the blocked calls will now be triggerd once
	 * @param ignore
	 */
	public static void setIgnore(boolean ignore)
	{
		WebPackagesListener.ignore.set(ignore);
		if (!ignore) checkPackages(false);
	}

	public static void setIgnoreAndCheck(boolean ignore, boolean check)
	{
		WebPackagesListener.ignore.set(ignore);
		if (!ignore && check) checkPackages(false);
	}

	public static void checkPackages(boolean ci)
	{
		if (ServoyModelFinder.getServoyModel().getActiveProject() == null || ignore.get() || Activator.getInstance().getSolutionProjectFolder() == null)
		{
			return;
		}
		Job[] jobs = Job.getJobManager().find(CopySourceFolderAction.JOB_FAMILY);
		if (jobs.length > 0 && scheduled.get() == 0)
		{
			// continue
		}
		else if (jobs.length > 0 && scheduled.get() != 2)
		{
			return;
		}
		if (ci) cleanInstall.set(ci);
		// only schedule 1 and a bit later to relax first the system
		if (scheduled.compareAndSet(0, 1))
		{
			Job job = new PackageCheckerJob("Checking/Installing Titanium NG Components and Services", Activator.getInstance().getSolutionProjectFolder(),
				null);
			job.setUser(false);
			job.schedule(5000);
			job.addJobChangeListener(IJobChangeListener.onDone(event -> {
				if (event.getResult().getSeverity() != IStatus.OK) handleTitaniumNGClientBuildFailure();
			}));
		}
		else if (scheduled.get() == 2)
		{
			// there is only 1 job and that one is running (not scheduled) then increase by one to make it run again
			scheduled.incrementAndGet();
		}
	}

	private static void handleTitaniumNGClientBuildFailure()
	{
		if (ModelUtils.isTestRunning()) return; // don't show dialogs when running junit tests

		Display display = Display.getDefault();

		// a timer just in case another titanium build is quickly triggered by another change;
		// give the new build a chance to start then; in that case we will want to ignore this failure
		display.asyncExec(() -> display.timerExec(2000, () -> {
			if (!CopySourceFolderAction.isTitaniumNGBuildRunning())
			{
				int usersChoice = MessageDialog.open(MessageDialog.WARNING, display.getActiveShell(),
					"Titanium NG Client build failed",
					"Details are available in the 'Titanium NG Build Console'.\nIt could be due an incorrect version or error in some service/component package that the solution uses, but there are multiple other possible causes.\n\nYou can either check what went wrong in the 'Titanium NG Build Console' or start a full/clean client build.",
					SWT.NONE, "Show 'Titanium NG Build Console'", "Retry; start a full/clean build");
				if (usersChoice <= 0)
				{
					ConsoleFactory.openTiNGBuildConsole();
				}
				else
				{
					CopySourceFolderAction.startTitaniumNGBuild(CopySourceFolderAction.CLEAN_BUILD);
				}
			} // else a new build is running; allow it to finish and see if it fails again
		}));
	}

	/**
	 *  exports the state location dir to the location given that should be a WAR layout
	 *  The index.html page will be put in WEB-INF/angular-index.html the rest in the root.
	 * @throws IOException
	 */
	public static void exportNG2ToWar(ING2WarExportModel model)
	{
		Activator activator = Activator.getInstance();
		activator.waitForNodeExtraction();

		try
		{
			File distributionSource = new File(activator.getMainTargetFolder(), model.getSolutionName() + "_dist");
			new NodeFolderCreatorJob(distributionSource, false, false).run(model.getProgressMonitor());
			// create the production build
			IStatus buildStatus = new PackageCheckerJob("production_build", distributionSource, model.getModel(), model.getSolutionName())
				.run(model.getProgressMonitor());
			// SVY-21502 §6: a headless WAR export fails loud when it reaches a build script for a name@version that is
			// not in the committed approvals (the PackageCheckerJob returns an ERROR status with the exact list). Do
			// not silently produce a WAR missing those builds; surface the error to the exporter.
			if (buildStatus != null && buildStatus.getSeverity() == IStatus.ERROR)
			{
				throw new RuntimeException(buildStatus.getMessage());
			}
			// copy the production build
			File distFolder = new File(distributionSource, "dist/app/browser");
			if (distFolder.exists())
			{
				FileUtils.copyDirectory(distFolder, model.getExportLocation(),
					(path) -> !(path.getName().equals("index.html") || path.getName().equals("favicon.ico")));

				FileUtils.copyFile(new File(distFolder, "index.html"), new File(model.getExportLocation(), "WEB-INF/angular-index.html"));
			}
			else
			{
				throw new RuntimeException("NGClient2 production resources not generated, see the log or the Titanium NGClient build console");
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error generating NGClient2 production resources", e);
		}
	}

	private static void writeConsole(StringOutputStream console, String message)
	{
		if (console == null) return;
		try
		{
			console.write(message + "\n");
		}
		catch (IOException e)
		{
			// ignore console write failures
		}
	}

	private static void writeErrorToConsoleAndLog(StringOutputStream console, Exception e, String s)
	{
		Activator.getInstance().getLog().error(s, e);
		writeConsole(console, "\r\n" + s + e.getMessage() + "\r\n");
	}

	/**
	 * SVY-21502: prepares the per-solution pnpm dependency build-script approvals before the first package-mutating
	 * pnpm command runs.
	 * <p>
	 * Ordered flow (Developer full build): apply the committed allowed/denied lists by generating the solution-local
	 * pnpm-workspace.yaml; run the bounded discovery query to find packages with (still) pending build scripts; if the
	 * pending set is non-empty AND a real UI is present (not headless/test, not WAR export), prompt the developer to
	 * allow or deny each pending package, persist the decisions into
	 * <code>ng_web_packages/pnpm-build-approvals.json</code> and regenerate the yaml.
	 * <p>
	 * In the headless WAR export (<code>warExportModel != null</code>) and in automated/test runs there is NO prompt:
	 * the committed lists are applied as-is and the build proceeds. Denied packages stay denied and the build proceeds
	 * regardless (SVY-21502 §3.6).
	 *
	 * @param projectFolder the solution's standalone pnpm project folder (<code>target/&lt;solution&gt;/</code>)
	 * @param warExportModel non-null for a WAR export (headless, never prompts), null for a Developer build
	 * @param approvalsSolutionName the solution name for a WAR export (else null -> active solution project)
	 * @param monitor progress monitor
	 * @param console the TiNG build console
	 */
	private static void applyKnownPnpmBuildApprovals(File projectFolder, ITiNGExportModel warExportModel, String approvalsSolutionName,
		StringOutputStream console)
	{
		IProject solutionProject = resolveApprovalsProject(warExportModel, approvalsSolutionName);
		// SVY-21502 §3.10: seed the generated target/<solution>/pnpm-lock.yaml from the committed
		// ng_web_packages/pnpm-lock.yaml (explicit step - NodeFolderCreatorJob's template copy skips lockfiles) so
		// both the Developer build and the frozen WAR/CI install start from the committed, pinned version set.
		PnpmBuildScriptConfig.seedLockfileFromCommitted(solutionProject, projectFolder, console);
		PnpmBuildApprovals approvals = PnpmBuildApprovals.fromWebPackagesFolder(solutionProject);
		// apply the KNOWN allowed/denied lists so the upcoming installs run the already-approved build scripts
		// (as an allowBuilds map, strictDepBuilds: true). Packages pulled in by the web-package tarballs that are
		// not yet decided are handled afterwards by resolvePnpmBuildScriptApprovals - the installs run inside a
		// RunPNPMCommand.beginBuildScriptDiscoveryDemotion() window so a brand-new pending script does not hard-fail.
		PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, console);
	}

	/**
	 * SVY-21502: after the package installs have materialized <code>node_modules</code> (inside a
	 * {@link RunPNPMCommand#beginBuildScriptDiscoveryDemotion()} window so they did not hard-fail), discover which
	 * <code>name@version</code> build scripts are still pending, prompt the developer to allow/deny the undecided ones
	 * (Developer UI only - never WAR/headless/test), persist the decisions, regenerate the <code>allowBuilds</code>
	 * yaml and run <code>pnpm rebuild</code> so the approved scripts actually run before the Angular build. On the
	 * Developer path a successful cycle also commits the resulting <code>pnpm-lock.yaml</code> back to the committed
	 * <code>ng_web_packages/pnpm-lock.yaml</code> (SVY-21502 §3.5d / §3.10).
	 * <p>
	 * Denied <code>name@version</code> entries stay denied and are not re-prompted; the build proceeds regardless
	 * (SVY-21502 §3.6). In a <b>headless WAR export</b> there is NO prompt: if any <code>name@version</code> is still
	 * pending after applying the committed lists this method returns an <b>ERROR</b> {@link IStatus} listing the exact
	 * pending entries (SVY-21502 §6 fail-loud - it never silently runs or silently skips). Automated/test runs with no
	 * UI simply log and proceed.
	 *
	 * @param projectFolder the solution's standalone pnpm project folder (node_modules already materialized)
	 * @param warExportModel non-null for a WAR export (headless, fails loud on drift), null for a Developer build
	 * @param approvalsSolutionName the solution name for a WAR export (else null -> active solution project)
	 * @param monitor progress monitor
	 * @param console the TiNG build console
	 * @return {@link Status#OK_STATUS} normally; an ERROR status (WAR/headless only) when an undecided
	 *         <code>name@version</code> build script was reached
	 */
	private static IStatus resolvePnpmBuildScriptApprovals(File projectFolder, ITiNGExportModel warExportModel, String approvalsSolutionName,
		IProgressMonitor monitor, StringOutputStream console)
	{
		IProject solutionProject = resolveApprovalsProject(warExportModel, approvalsSolutionName);
		PnpmBuildApprovals approvals = PnpmBuildApprovals.fromWebPackagesFolder(solutionProject);

		// discovery: which name@version entries still have pending (not-yet-decided) build scripts in the installed tree?
		Set<String> discovered = PnpmBuildScriptConfig.discoverPendingBuildScripts(projectFolder, monitor, console);
		Set<String> pending = approvals.pending(discovered);

		// gate on resolving the pending set - only when a real Developer UI is present (never WAR/headless/test)
		boolean uiPresent = warExportModel == null && !Activator.isNodeExtractionAndTitaniumBuildDisabled() && !ModelUtils.isTestRunning() &&
			PlatformUI.isWorkbenchRunning();
		boolean changed = false;
		if (warExportModel != null)
		{
			// SVY-21502 §6 + N1: a headless WAR/CI build must never silently ship unreviewed build scripts.
			// (a) If the install state could not be read (node_modules present but .modules.yaml missing), discovery
			// could not tell what was ignored - treat that as indeterminate and fail loud rather than trusting the
			// empty pending set.
			if (!PnpmBuildScriptConfig.isModulesStateReadable(projectFolder))
			{
				String msg = "pnpm build-script discovery could not read node_modules/.modules.yaml for the headless build, " +
					"so pending dependency build scripts cannot be determined. Refusing to produce a WAR that may silently skip " +
					"required native builds. Re-run a clean build in the Developer and commit ng_web_packages/pnpm-lock.yaml and " +
					"pnpm-build-approvals.json.";
				writeConsole(console, "- pnpm mode: ERROR - " + msg);
				return new Status(IStatus.ERROR, WebPackagesListener.class, msg);
			}
			// (b) A genuinely-undecided name@version was reached: fail loud with the exact list so the developer opens
			// the solution, approves/denies and commits. A denied version is NOT pending (it is in the denied list),
			// so this fires only for truly-undecided versions (e.g. the committed lockfile and approvals drifted).
			if (!pending.isEmpty())
			{
				String msg = "pnpm build-script approval drift: the headless build reached " + pending.size() +
					" dependency build script(s) that are not in the committed ng_web_packages/pnpm-build-approvals.json: " + pending +
					". Open the solution in the Developer, allow or deny each of these exact name@version entries, and commit " +
					"ng_web_packages/pnpm-build-approvals.json (and pnpm-lock.yaml). The headless export does not silently run or skip them.";
				writeConsole(console, "- pnpm mode: ERROR - " + msg);
				return new Status(IStatus.ERROR, WebPackagesListener.class, msg);
			}
		}
		if (!pending.isEmpty() && uiPresent && solutionProject != null)
		{
			boolean[] decided = new boolean[] { false };
			Display display = Display.getDefault();
			display.syncExec(() -> decided[0] = PnpmBuildApprovalPrompt.promptForPending(display.getActiveShell(), pending, approvals));
			if (decided[0])
			{
				try
				{
					approvals.saveToWebPackagesFolder(solutionProject);
					try
					{
						solutionProject.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).refreshLocal(IResource.DEPTH_ONE,
							new NullProgressMonitor());
					}
					catch (CoreException ce)
					{
						// refresh is best-effort; the file is already written to disk
						Activator.getInstance().getLog().error("Error refreshing ng_web_packages after writing pnpm build approvals", ce);
					}
				}
				catch (IOException e)
				{
					writeErrorToConsoleAndLog(console, e, "Exception while saving pnpm build approvals: ");
				}
				// regenerate the yaml from the now-updated committed lists
				PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, console);
				changed = true;
			}
		}
		else if (!pending.isEmpty())
		{
			// automated/test run with no UI (and not a WAR export): leave pending entries undecided and proceed.
			writeConsole(console, "- pnpm mode: " + pending.size() +
				" package(s) have pending build scripts but no UI is available to decide; build proceeds without them: " + pending);
		}

		// run the (now up to date) allowBuilds via 'pnpm rebuild' so approved scripts actually run - pnpm does not
		// retroactively run a script that was ignored on a previous install. Do this whenever something is allowed
		// (freshly decided now, or already-committed allowed packages that the demoted installs did not build).
		if (changed || !approvals.getAllowed().isEmpty())
		{
			PnpmBuildScriptConfig.rebuildApprovedBuildScripts(projectFolder, monitor, console);
		}

		// SVY-21502 §3.5d/§3.10: on the Developer path, commit the resulting pnpm-lock.yaml back to the committed
		// ng_web_packages/pnpm-lock.yaml so the developer's deliberate install is what gets committed. CI/WAR
		// (warExportModel != null) must NOT mutate committed sources, so it is skipped there.
		if (warExportModel == null)
		{
			PnpmBuildScriptConfig.commitLockfileBack(solutionProject, projectFolder, console);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Resolves the Eclipse project whose <code>ng_web_packages</code> folder holds the committed approvals file.
	 * For a WAR export the solution name is passed in; otherwise it is the active solution project.
	 *
	 * @return the solution project, or <code>null</code> if it cannot be resolved
	 */
	private static IProject resolveApprovalsProject(ITiNGExportModel warExportModel, String approvalsSolutionName)
	{
		try
		{
			if (warExportModel == null && approvalsSolutionName == null && ServoyModelFinder.getServoyModel().getActiveProject() != null)
			{
				return ServoyModelFinder.getServoyModel().getActiveProject().getProject();
			}
			if (approvalsSolutionName != null)
			{
				ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(approvalsSolutionName);
				if (project != null) return project.getProject();
			}
		}
		catch (RuntimeException e)
		{
			Activator.getInstance().getLog().error("Error resolving solution project for pnpm build approvals", e);
		}
		return null;
	}

	static boolean copyAngularLocales(File projectFolder)
	{
		File localesDest = new File(projectFolder, "locales/angular");
		File localesSource = new File(projectFolder.getParentFile(), "node_modules/@angular/common/locales");
		if (!localesSource.isDirectory())
		{
			localesSource = new File(projectFolder, "node_modules/@angular/common/locales");
		}
		if (localesSource.isDirectory())
		{
			localesDest.mkdirs();
			try
			{
				FileUtils.copyDirectory(localesSource, localesDest, (File f) -> f.isDirectory() || f.getName().endsWith(".js"));
			}
			catch (IOException e)
			{
				return false;
			}
			return true;
		}
		return false;
	}

	private interface LayoutTemplates
	{
		CharSequence getFormComponentTemplate();

		CharSequence getLFFormComponentTemplate();

		CharSequence getViewChilds();
	}
}
