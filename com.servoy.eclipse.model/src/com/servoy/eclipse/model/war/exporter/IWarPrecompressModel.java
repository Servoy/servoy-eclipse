package com.servoy.eclipse.model.war.exporter;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Describes a precompression run over an assembled (temporary) WAR directory.
 * Implemented by the WAR exporter and consumed by the bundle that has access to Node.js.
 */
public interface IWarPrecompressModel
{
	/** The assembled WAR directory (not yet zipped). */
	File getWarDirectory();

	/** Whether to create .br (brotli) siblings. */
	boolean isBrotli();

	/** Whether to create .gz (gzip) siblings. */
	boolean isGzip();

	/** The progress monitor, used for cancelling the running process. */
	IProgressMonitor getProgressMonitor();
}
