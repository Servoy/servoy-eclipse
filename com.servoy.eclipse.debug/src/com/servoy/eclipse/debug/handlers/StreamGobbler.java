package com.servoy.eclipse.debug.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * A utility class to read and consume a process's input stream in a separate thread.
 * This is crucial for preventing the child process from hanging due to full pipe buffers.
 */
public class StreamGobbler implements Runnable
{
	private final InputStream inputStream;
	private final Consumer<String> consumer;
	private final String prefix;

	/**
	 * Creates a StreamGobbler instance.
	 *
	 * @param inputStream The stream (stdout or stderr) to read from.
	 * @param consumer The function to execute for each line read (e.g., System.out::println).
	 * @param prefix A string prefix (e.g., "[STDOUT]" or "[STDERR]") for clear identification in the console.
	 */
	public StreamGobbler(InputStream inputStream, Consumer<String> consumer, String prefix)
	{
		this.inputStream = inputStream;
		this.consumer = consumer;
		this.prefix = prefix;
	}

	@Override
	public void run()
	{
		// Use a BufferedReader to read the stream line by line
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				// Apply the consumer (e.g., print to System.out/System.err) with the prefix
				consumer.accept(prefix + " " + line);
			}
		}
		catch (Exception e)
		{
			// Log the error but don't stop the main application
			ServoyLog.logError("Error when reading the output or error stream", e);
		}
	}
}
