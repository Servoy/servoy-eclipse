/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.build.documentation.ai;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ai.shared.SharedStaticContent;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

/**
 * @author acostescu
 */
public class FineTuningDatasetGeneratorForJavaBasedApiAndWebPackages extends AbstractAIInfoGenerator
{

	private static final String SYSTEM_MESSAGE_WHEN_FINE_TUNNING = "{\"role\": \"system\", \"content\": \"Let's talk about Servoy, using up-to-date information from " +
		DATE_OF_REFERENCE_FORMATTED + ".\"}";

	private final FineTuningInfoKeeper fineTuningInfoKeeper;

	private FineTuningDatasetGeneratorForJavaBasedApiAndWebPackages()
	{
		super();

		fineTuningInfoKeeper = new FineTuningInfoKeeper();
	}

	/**
	 * It will generate fine tuning data.
	 *
	 * @param args 0 - uri to "servoydoc_jslib.xml"
	 *             1 - uri to "servoydoc.xml"
	 *             2 - uri to "servoydoc_design.xml"
	 *             3 - uri to the plugin dir of an installed application server (for java plugins)
	 *             4 - path to the text file that contains on each line one location of one component/service/layout (ng) package dir - to generate the info for
	 * @throws TemplateException
	 * @throws JSONException
	 */
	public static void main(String[] args) throws Exception
	{
		String jsLibURI = args[0];
		String servoyDocURI = args[1];
		String designDocURI = args[2];
		String pluginDirURI = args[3];
		String ngPackagesFileLocationsURI = args[4];

		FineTuningDatasetGeneratorForJavaBasedApiAndWebPackages fineTunningGenerator = new FineTuningDatasetGeneratorForJavaBasedApiAndWebPackages();
		fineTunningGenerator.generate(jsLibURI, servoyDocURI, designDocURI, pluginDirURI, ngPackagesFileLocationsURI,
			new FineTuningInfoFromNGPackagesGenerator(fineTunningGenerator.getFTLCfg(), fineTunningGenerator.fineTuningInfoKeeper,
				"fine_tuning_ng_package_template.md",
				"fine_tuning_ng_webobject_template.md",
				"fine_tuning_ng_webobject_method_template.md",
				"fine_tuning_ng_webobject_property_template.md",
				"fine_tuning_ng_webobject_type_template.md"),
			fineTunningGenerator); // this arg will be stored in "utils" in the root of the template model so that functions from it can be used in the template

		fineTunningGenerator.writeFineTuningFile();
		System.out.println("DONE.");
	}

	public String asJSONValue(Object content)
	{
		// it's actually "asJSONValueInsideAJSONString"
		String v = JSONObject.valueToString(content);
		return (v.startsWith("\"") && v.endsWith("\"") ? v.substring(1, v.length() - 1) : v);
	}

	public String getSystemMessage()
	{
		return SYSTEM_MESSAGE_WHEN_FINE_TUNNING;
	}

	private void writeFineTuningFile() throws FileNotFoundException, IOException
	{
		// write the generated texts to disk so they can be used afterwards by a tool from build/com.servoy.ai.tools project - to fine tune an OpenAI model
		Files.writeString(new File(SharedStaticContent.STORED_DOC_FINE_TUNING_ITEMS).toPath(), fineTuningInfoKeeper.getFullContentFromAllPasses(),
			StandardCharsets.UTF_8);
		System.out.println("Fine tuning entries were written to " + new File(SharedStaticContent.STORED_DOC_FINE_TUNING_ITEMS).getAbsolutePath());
	}

	/** It keeps generated info, and, as the web objects of a package get generated before the packge itself, it will swap the two in the end result, so that the order makes sense. */
	private static class InOrderInfoKeeper implements IInfoKeeper
	{

		private final StringBuilder webObjectsContent = new StringBuilder();
		// content for one of the fine tuning generating passes, that will be stored to disk/serialized; it can then be used for fine-tuning via tools in build/com.servoy.ai.tools project
		private final StringBuilder finalContent = new StringBuilder(1900 * 100);

		public void addInfoAboutWebObjectsInAPackage(String someContentOfAWebObjectInThePackage)
		{
			webObjectsContent.append(someContentOfAWebObjectInThePackage); // happens first, a bunch of times
		}

		public void addInfoAboutProcessedPackage(String packageInfo)
		{
			// happens second; but we do want this content to appear first
			registerNewInfo(packageInfo);
			registerNewInfo(webObjectsContent);
			webObjectsContent.setLength(0);
		}

		private void registerNewInfo(CharSequence newStringToEmbed)
		{
			if (newStringToEmbed.length() > 0)
				finalContent.append(finalContent.length() == 0 ? newStringToEmbed.subSequence(1, newStringToEmbed.length()) : newStringToEmbed); // gets rid of first \n so we don't have an empty new line at start of file
		}
	}

	private static class FineTuningInfoKeeper implements IInfoKeeper
	{

		private static final int CURRENT_NUMBER_OF_PASSES_IN_FINE_TUNING_TEMPLATE_MD_FILES = 3;

		InOrderInfoKeeper[] infoKeepersForEachPass = new InOrderInfoKeeper[3];
		private int pass;

		FineTuningInfoKeeper()
		{
			// we try to feed the same information to the fine-tuning process in multiple ways;
			// each of the .md templates has one section for each pass; however, the passes will be kept separately and
			// will be given to the training process sequentially by pass - even if the content for a service or component for example
			// is generated for all passes one after the other
			infoKeepersForEachPass[0] = new InOrderInfoKeeper();
			infoKeepersForEachPass[1] = new InOrderInfoKeeper();
			infoKeepersForEachPass[2] = new InOrderInfoKeeper();
		}

		@Override
		public void addInfoAboutWebObjectsInAPackage(String content)
		{
			infoKeepersForEachPass[pass].addInfoAboutWebObjectsInAPackage(content);
		}

		@Override
		public void addInfoAboutProcessedPackage(String content)
		{
			infoKeepersForEachPass[pass].addInfoAboutProcessedPackage(content);
		}

		public String getFullContentFromAllPasses()
		{
			return Arrays.stream(infoKeepersForEachPass).map((ik) -> ik.finalContent).collect(Collectors.joining("\n"));
		}

		public void forAllPasses(Consumer<Integer> toExecute)
		{
			for (int passNo = 0; passNo < CURRENT_NUMBER_OF_PASSES_IN_FINE_TUNING_TEMPLATE_MD_FILES; passNo++)
			{
				this.pass = passNo;
				toExecute.accept(Integer.valueOf(passNo + 1));
			}
		}

	}

	protected static class FineTuningInfoFromNGPackagesGenerator extends InfoFromNGPackagesGenerator
	{

		private final Template propertiesPrefixTemplate;
		private final Template propertiesPostfixTemplate;


		public FineTuningInfoFromNGPackagesGenerator(Configuration cfg, FineTuningInfoKeeper registerNewInfo, String packageTemplateFilename,
			String webObjectTemplateFilename, String methodTemplateFilename, String propertyTemplateFilename, String typeTemplateTemplateFilename)
			throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException
		{
			super(cfg, registerNewInfo, packageTemplateFilename, webObjectTemplateFilename, methodTemplateFilename, propertyTemplateFilename,
				typeTemplateTemplateFilename);
			this.propertiesPrefixTemplate = cfg.getTemplate("fine_tuning_ng_webobject_properties_prefix_template.md");
			this.propertiesPostfixTemplate = cfg.getTemplate("fine_tuning_ng_webobject_properties_postfix_template.md");
		}

		@Override
		public void generateComponentOrServiceInfo(Map<String, Object> root, File userDir, String displayName, String categoryName, boolean service,
			String deprecationString, String replacementInCaseOfDeprecation) throws TemplateException, IOException
		{
			FineTuningInfoKeeper ftk = ((FineTuningInfoKeeper)this.registerNewInfo);
			ftk.forAllPasses((pass) -> {
				root.put("pass", pass);
				try
				{
					super.generateComponentOrServiceInfo(root, userDir, displayName, categoryName, service, deprecationString, replacementInCaseOfDeprecation);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			});
		}

		@Override
		protected void generateProperties(Map<String, Object> root) throws TemplateException, IOException
		{
			// we insert all properties into one fine tuning chat training message
			StringWriter out = new StringWriter();
			propertiesPrefixTemplate.process(root, out);
			registerNewInfo.addInfoAboutWebObjectsInAPackage(out.toString());

			super.generateProperties(root);

			out = new StringWriter();
			propertiesPostfixTemplate.process(root, out);
			registerNewInfo.addInfoAboutWebObjectsInAPackage(out.toString());
		}

		@Override
		public void generateNGPackageInfo(String packageName, String packageDisplayName, String packageDescription, String packageType,
			Map<String, Object> root) throws TemplateException, IOException
		{
			((FineTuningInfoKeeper)this.registerNewInfo).forAllPasses((pass) -> {
				root.put("pass", pass);
				try
				{
					super.generateNGPackageInfo(packageName, packageDisplayName, packageDescription, packageType, root);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			});
		}

	}

}
