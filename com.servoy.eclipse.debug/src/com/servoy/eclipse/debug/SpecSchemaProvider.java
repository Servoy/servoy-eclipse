package com.servoy.eclipse.debug;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;

import com.servoy.eclipse.model.util.ServoyLog;

import sj.jsonschemavalidation.ISchemaProvider;

public class SpecSchemaProvider implements ISchemaProvider
{
	private final String specSchema;

	public SpecSchemaProvider()
	{
		specSchema = readSpecSchema();
	}

	protected String readSpecSchema()
	{
		StringBuilder sb = new StringBuilder();
		char[] chars = new char[2048];
		try (InputStream is = getClass().getResourceAsStream("spec.schema"); InputStreamReader reader = new InputStreamReader(is))
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
			ServoyLog.logError(e);
		}
		return sb.toString();
	}

	@Override
	public String getSchemaFor(IFile file)
	{
		if (file.getName().toLowerCase().endsWith(".spec"))
		{
//			FOR DEBUG uncomment the following line
//			return readSpecSchema();
			return specSchema;
		}
		return null;
	}
}
