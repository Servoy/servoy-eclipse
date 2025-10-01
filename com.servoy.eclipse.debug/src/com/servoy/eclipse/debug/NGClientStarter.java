/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.debug;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.RunNPMCommand;
import com.servoy.eclipse.ngclient.ui.utils.NGClientConstants;

/**
 * @author fbandrei
 *
 * This interface is used for running a progress bar before starting the client.
 *
 */
public interface NGClientStarter
{
	/**
	 * Start a progress bar when the client is started before the build is started or during the build.
	 * The NPM install is also caught (it is executed before the build - if executed).
	 * 0 build jobs means that the build (in watch mode) was not started
	 *
	 * @param monitor
	 */
	default void runProgressBarAndNGClient(IProgressMonitor monitor)
	{
		// *** the code in comments BELOW has nothing to do with the actual run client method; skip it

		// this commented out code is useful for adjusting native-references.xml of org.eclipse.dltk.javascript.core/resources
		// which is the documentation place for native javascript things (String, Number etc. for code completion; if you want
		// to generate the content to put in a method's description in that xml based on the JSDoc of classes that we have
		// in package com.servoy.j2db.documentation.scripting.docs (for example com.servoy.j2db.documentation.scripting.docs.String)
		// (make sure you copy the generated servoydoc_jslib.xml from jenkins update_zip to /com.servoy.eclipse.core/src/com/servoy/eclipse/core/doc/servoydoc_jslib.xml)
		// uncomment the lines below, and when running NGClient you will see these in the console:

		// IN CASE OF OVERLOADED METHODS (same name, different/optional args) do use the one with the most args in generating the doc for native-references.xml
		// AND ADD [] to the optional arguments (so '@param String [myOptionalArg]' instead of '@param String myOptionalArg') - StringEscapeUtils.escapeXml10 does not do that (as it's meant for having separate descriptions for each overload, but native-references.xml only supports one version of the method)
//		System.out.println("\npadStart(int, String): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("padStart", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[] { int.class, com.servoy.j2db.documentation.scripting.docs.String.class }))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));
//		System.out.println("\npadEnd(int, String): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("padEnd", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[] { int.class, com.servoy.j2db.documentation.scripting.docs.String.class }))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));
//		System.out.println("\nfromCharCode(Number...): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("fromCharCode", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[] { com.servoy.j2db.documentation.scripting.docs.Number[].class }))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));
//		System.out.println("\nfromCodePoint(Number...): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("fromCodePoint", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[] { com.servoy.j2db.documentation.scripting.docs.Number[].class }))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));
//		System.out.println("\ncodePointAt(Number): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("codePointAt", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[] { com.servoy.j2db.documentation.scripting.docs.Number.class }))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));
//		System.out.println("\ntrimStart(): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("trimStart", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[0]))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));
//		System.out.println("\ntrimEnd(): " + StringEscapeUtils.escapeXml10(
//			TypeCreator.getDoc("trimStart", com.servoy.j2db.documentation.scripting.docs.String.class,
//				new Class[0]))
//			.replace("\r", "&#13;").replace("\n", "&#10;"));


		// *** the code ABOVE has nothing to do with the actual run client method


//		Job[] buildJobs = Job.getJobManager().find(NGClientConstants.NPM_BUILD_JOB);
		if ( /* buildJobs.length == 0 || */ RunNPMCommand.isNGBuildRunning())
		{
			Display.getDefault().asyncExec(() -> {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(UIUtils.getActiveShell());
				try
				{
					dialog.run(true, false, new IRunnableWithProgress()
					{
						@Override
						public void run(IProgressMonitor monitorNpmBuild) throws InvocationTargetException, InterruptedException
						{
							monitorNpmBuild.beginTask(NGClientConstants.NPM_CONFIGURATION_TITLE_PROGRESS_BAR, IProgressMonitor.UNKNOWN);
							while (/* Job.getJobManager().find(NGClientConstants.NPM_BUILD_JOB).length == 0 || */RunNPMCommand.isNGBuildRunning())
							{
								// verify the NG build job at every second
								// the progress bar is completed if the command takes more than the given workload
								Thread.sleep(1000);
								monitorNpmBuild.worked(1);
							}
							monitorNpmBuild.done();
							startNGClient(monitor);
						}
					});
				}
				catch (InvocationTargetException | InterruptedException e)
				{
					ServoyLog.logError(e);
				}
			});
		}
		else // Start the client directly, no progress bar needed.
		{
			startNGClient(monitor);
		}
	}

	/**
	 * This method starts the NG client (web or desktop).
	 *
	 * @param monitor
	 */
	void startNGClient(IProgressMonitor monitor);
}
