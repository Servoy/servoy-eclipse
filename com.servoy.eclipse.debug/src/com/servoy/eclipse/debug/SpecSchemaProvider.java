package com.servoy.eclipse.debug;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;

import sj.jsonschemavalidation.ISchemaProvider;

public class SpecSchemaProvider implements ISchemaProvider
{
	private final String specSchema;

	public SpecSchemaProvider()
	{
		InputStream is = getClass().getResourceAsStream("spec.schema");
		InputStreamReader reader = new InputStreamReader(is);
		StringBuilder sb = new StringBuilder();
		char[] chars = new char[2048];
		try
		{
			int read = reader.read(chars);
			while (read != -1)
			{
				sb.append(chars, 0, read);
				read = reader.read(chars);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		specSchema = sb.toString();
	}

	@Override
	public String getSchemaFor(IFile file)
	{
		if (file.getName().toLowerCase().endsWith(".spec"))
		{
			InputStream is = getClass().getResourceAsStream("spec.schema");
			InputStreamReader reader = new InputStreamReader(is);
			StringBuilder sb = new StringBuilder();
			char[] chars = new char[2048];
			try
			{
				int read = reader.read(chars);
				while (read != -1)
				{
					sb.append(chars, 0, read);
					read = reader.read(chars);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return sb.toString();
//			return specSchema;
		}
		return null;
	}
}
