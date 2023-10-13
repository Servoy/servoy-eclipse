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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.servoy.eclipse.core.ai.shared.PineconeItem;
import com.servoy.eclipse.core.ai.shared.SharedStaticContent;

import freemarker.template.TemplateException;

/**
 * @author acostescu
 */
public class FullPineconeEmbeddingsGenerator extends AbstractAIInfoGenerator
{

	private final PineconeInfoKeeper pineconeInfoKeeper;

	private FullPineconeEmbeddingsGenerator()
	{
		super();

		pineconeInfoKeeper = new PineconeInfoKeeper();
	}

	private class PineconeInfoKeeper implements IInfoKeeper
	{

		int[] id = new int[] { 1 };
		// 1 list that will be stored to disk/serialized; it can then be used for upsert/etc. via tools in build/com.servoy.ai.tools project
		private final List<PineconeItem> pineconeItemsToUpsert = new ArrayList<>();

		@Override
		public void addInfoAboutWebObjectsInAPackage(String newStringToEmbed)
		{
			this.registerNewEmbedding(newStringToEmbed);
		}

		@Override
		public void addInfoAboutProcessedPackage(String newStringToEmbed)
		{
			this.registerNewEmbedding(newStringToEmbed);
		}

		private void registerNewEmbedding(String newStringToEmbed)
		{
			// We could add other metadata as needed (versions of Servoy, versions of packages, don't know what exactly would be useful in the future to filter the similarity checks)
			pineconeItemsToUpsert.add(new PineconeItem(id[0]++, newStringToEmbed, null));
		}

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

		FullPineconeEmbeddingsGenerator pineconeEmbeddingsGenerator = new FullPineconeEmbeddingsGenerator();
		pineconeEmbeddingsGenerator.generate(jsLibURI, servoyDocURI, designDocURI, pluginDirURI, ngPackagesFileLocationsURI,
			new InfoFromNGPackagesGenerator(pineconeEmbeddingsGenerator.getFTLCfg(), pineconeEmbeddingsGenerator.pineconeInfoKeeper,
				"pinecone_ng_package_template.md",
				"pinecone_ng_webobject_template.md",
				"pinecone_ng_webobject_method_template.md",
				"pinecone_ng_webobject_property_template.md",
				"pinecone_ng_webobject_type_template.md"),
			null);

		pineconeEmbeddingsGenerator.writePineconeItemsToFile();
		System.out.println("DONE.");
	}

	private void writePineconeItemsToFile() throws FileNotFoundException, IOException
	{
		// write the generated texts to disk so they can be used afterwards by a tool from build/com.servoy.ai.tools project - to upsert them to pinecone
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SharedStaticContent.STORED_ALL_UNEMBEDDED_PINECONE_ITEMS)))
		{
			out.writeObject(pineconeInfoKeeper.pineconeItemsToUpsert);
		}

		StringBuffer b = new StringBuffer(2000 * 200);
		for (PineconeItem item : pineconeInfoKeeper.pineconeItemsToUpsert)
		{
			b.append(item.getText()).append("\n-------------------\n");
		}

		Files.writeString(new File(SharedStaticContent.STORED_ALL_UNEMBEDDED_PINECONE_ITEMS + ".txt").toPath(), b, StandardCharsets.UTF_8);

		System.out.println("Doc items were written to " + new File(SharedStaticContent.STORED_ALL_UNEMBEDDED_PINECONE_ITEMS).getAbsolutePath());
		System.out.println(
			"Doc items (human friendly) were written to " + new File(SharedStaticContent.STORED_ALL_UNEMBEDDED_PINECONE_ITEMS).getAbsolutePath() + ".txt");
	}

}
