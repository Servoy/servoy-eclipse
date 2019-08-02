package com.servoy.build.documentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class DocumentationLogger
{
	private static DocumentationLogger instance = null;

	public static DocumentationLogger getInstance()
	{
		if (instance == null) instance = new DocumentationLogger();
		return instance;
	}

	private PrintStream out;

	private DocumentationLogger()
	{
		String fname = "docu.log";
		try
		{
			File rnd = File.createTempFile("docu", ".log", new File("."));
			fname = rnd.getAbsolutePath();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		System.out.println("Documentation log proposed name is '" + fname + "'.");
		File f = new File(fname);
		try
		{
			out = new PrintStream(f.getAbsolutePath());
			System.out.println("Documentation log written to '" + f.getAbsolutePath() + "'.");
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Documentation log written to stdout.");
			out = System.out;
			e.printStackTrace();
		}
	}

	public PrintStream getOut()
	{
		return out;
	}
}
