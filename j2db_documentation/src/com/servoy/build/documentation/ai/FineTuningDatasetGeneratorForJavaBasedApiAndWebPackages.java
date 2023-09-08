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
import java.util.Map;
import java.util.function.Consumer;

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

	private static final String SYSTEM_MESSAGE_WHEN_FINE_TUNNING = "{\"role\": \"system\", \"content\": \"You are a Servoy developer's assistant that likes to help only based on what it knows. All assumptions you make about things you do not know - when reasoning - you will present up-front in short sentences that start with 'Assuming '.\"},";

	// 1 list that will be stored to disk/serialized; it can then be used for upsert/etc. via tools in build/com.servoy.ai.tools project
	private final StringBuffer fineTuningJSONEntries = new StringBuffer(1900 * 100);
	private final Consumer<String> registerNewInfo;

	private FineTuningDatasetGeneratorForJavaBasedApiAndWebPackages()
	{
		super();

		registerNewInfo = (newStringToEmbed) -> {
			fineTuningJSONEntries.append(fineTuningJSONEntries.length() == 0 ? newStringToEmbed.substring(1) : newStringToEmbed); // gets rid of first \n so we don't have an empty new line at start of file
		};
	}

	/**
	 * It will generate and upsert all the needed pinecone embeddings related to Servoy (at least for APIs).
	 * These are things that are injected afterwards based on a similarity search into the chat prompt for the GPT-x LLM model to have more info when generating an answer.
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
			new FineTuningInfoFromNGPackagesGenerator(fineTunningGenerator.getFTLCfg(), fineTunningGenerator.registerNewInfo,
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
		Files.writeString(new File(SharedStaticContent.STORED_DOC_FINE_TUNING_ITEMS).toPath(), fineTuningJSONEntries, StandardCharsets.UTF_8);
		System.out.println("Fine tuning entries were written to " + new File(SharedStaticContent.STORED_DOC_FINE_TUNING_ITEMS).getAbsolutePath());
	}

	protected static class FineTuningInfoFromNGPackagesGenerator extends InfoFromNGPackagesGenerator
	{

		private final Template propertiesPrefixTemplate;


		public FineTuningInfoFromNGPackagesGenerator(Configuration cfg, Consumer<String> registerNewInfo, String packageTemplateFilename,
			String webObjectTemplateFilename, String methodTemplateFilename, String propertyTemplateFilename, String typeTemplateTemplateFilename)
			throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException
		{
			super(cfg, registerNewInfo, packageTemplateFilename, webObjectTemplateFilename, methodTemplateFilename, propertyTemplateFilename,
				typeTemplateTemplateFilename);
			this.propertiesPrefixTemplate = cfg.getTemplate("fine_tuning_ng_webobject_properties_prefix_template.md");
		}


		@Override
		protected void generateProperties(Map<String, Object> root) throws TemplateException, IOException
		{
			// we insert all properties into one fine tuning chat training message
			StringWriter out = new StringWriter();
			propertiesPrefixTemplate.process(root, out);

			registerNewInfo.accept(out.toString());

			super.generateProperties(root);
			registerNewInfo.accept("]}");
		}
	}

}
