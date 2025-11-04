/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.model.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.model.Activator;


public class ServoyLog
{
	/**
	 * Log the specified information.
	 *
	 * @param message a human-readable message,
	 * 		localized to the current locale
	 */
	public static void logInfo(String message)
	{
		log(IStatus.INFO, IStatus.OK, message, null);
	}

	/**
	 * Log the specified error.
	 *
	 * @param exception a low-level exception
	 */
	public static void logError(Throwable exception)
	{
		exception.printStackTrace();
		logError("Unexpected Exception", exception);
	}

	/**
	 * Log the specified error.
	 *
	 * @param message a human-readable message,
	 *       localized to the current locale
	 * @param exception a low-level exception,
	 *       or <code>null</code> if not applicable
	 */
	public static void logError(String message, Throwable exception)
	{
		log(IStatus.ERROR, IStatus.OK, message, exception);
	}

	/**
	 * Log the specified error.
	 *
	 * @param message a human-readable message,
	 *       localized to the current locale
	 * @param exception a low-level exception,
	 *       or <code>null</code> if not applicable
	 */
	public static void logError(String message)
	{
		log(IStatus.ERROR, IStatus.OK, message, null);
	}

	/**
	 * Log the specified warning.
	 *
	 * @param message a human-readable message,
	 *       localized to the current locale
	 * @param exception a low-level exception,
	 *       or <code>null</code> if not applicable
	 */
	public static void logWarning(String message, Throwable exception)
	{
		log(IStatus.WARNING, IStatus.OK, message, exception);
	}

	/**
	 * Log the specified information.
	 *
	 * @param severity the severity; one of
	 * 		<code>IStatus.OK</code>,
	 *		<code>IStatus.ERROR</code>,
	 *		<code>IStatus.INFO</code>,
	 *		or <code>IStatus.WARNING</code>
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param code the plug-in-specific status code, or <code>OK</code>
	 * @param message a human-readable message,
	 * 		localized to the current locale
	 * @param exception a low-level exception,
	 * 		or <code>null</code> if not applicable
	 */
	public static void log(int severity, int code, String message, Throwable exception)
	{

		log(createStatus(severity, code, message, exception));
	}

	/**
	 * Create a status object representing the specified information.
	 *
	 * @param severity the severity; one of
	 * 		<code>IStatus.OK</code>,
	 *		<code>IStatus.ERROR</code>,
	 *		<code>IStatus.INFO</code>,
	 *		or <code>IStatus.WARNING</code>
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param code the plug-in-specific status code, or <code>OK</code>
	 * @param message a human-readable message,
	 * 		localized to the current locale
	 * @param exception a low-level exception,
	 * 		or <code>null</code> if not applicable
	 * @return the status object (not <code>null</code>)
	 */
	public static IStatus createStatus(int severity, int code, String message, Throwable exception)
	{

		return new Status(severity, Activator.getDefault() != null ? Activator.getDefault().getBundle().getSymbolicName() : "com.servoy.eclipse.model", code,
			message, exception);
	}

	/**
	 * Log the given status.
	 *
	 * @param status the status to log
	 */
	public static void log(IStatus status)
	{
		if (Activator.getDefault() != null)
		{
			Activator.getDefault().getLog().log(status);
		}
		else
		{
			System.err.println("Cannot log message, plugin is disposed:" + status.getMessage());
		}
	}
}
