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
package com.servoy.eclipse.jsunit.runner;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Test class that will start a Servoy test client, import the solutions specified by system properties {@link #TEST_TARGET} and {@link #TEST_TARGET_FILE_EXTENSION}, run the jsUnit tests in them and then
 * shut down the test client.<br> 
 * The Servoy properties file that will be used by the client can be specified by system property {@link #TEST_PROPERTIES_FILE}.
 */
public class ServoyJSUnitTestRunner
{
	/**
	 * Key for the system property that gives the path to the properties file that should be used (either absolute or relative to the working dir). If not specified,
	 * the properties will be loaded from "<working dir>/<default properties file name obtained through usual means>".
	 */
	public static final String TEST_PROPERTIES_FILE = "servoy.test.property-file";
	/**
	 * Key for the system property that points to the solutions that should be tested. It must point to a folder containing exported solutions or to an exported solution file.
	 * The name of the export files must be the same as the solution it contains that must be tested. If this is not specified, the default value is "<working dir>/../../exportedSolutions/".
	 */
	public static final String TEST_TARGET = "servoy.test.target-exports";
	/**
	 * Key for the system property that specifies the extension of export files. ".servoy" is used by default, if the property is not specified.
	 */
	public static final String TEST_TARGET_FILE_EXTENSION = "servoy.test.target-file-ext";

	public static final String DEFAULT_PATH_TO_EXPORTED_TEST_SOLUTIONS = System.getProperty("user.dir") + "/../../exportedSolutions/";
	public static final String DEFAULT_EXT = ".servoy";

	private static final Log log = LogFactory.getLog("com.servoy.j2db.SolutionJSTestSuite");

	public static void main(String[] args)
	{
		junit.swingui.TestRunner.run(ServoyJSUnitTestRunner.class);
	}

	public static Test suite()
	{
		return null; // Temporary commented out, FIXME
//		TestSuite suite = new TestSuite("All test solutions"); //$NON-NLS-1$
//
//		String testTarget = System.getProperty(TEST_TARGET, DEFAULT_PATH_TO_EXPORTED_TEST_SOLUTIONS);
//		File exportedSolutions = new File(testTarget);
//
//		File[] exportFiles = null;
//		if (exportedSolutions.exists())
//		{
//			if (exportedSolutions.isDirectory())
//			{
//				exportFiles = exportedSolutions.listFiles();
//			}
//			else
//			{
//				exportFiles = new File[] { exportedSolutions };
//			}
//		}
//		if (exportFiles != null)
//		{
//			TestProcess application = null;
//			Exception initException = null;
//			String ext = System.getProperty(TEST_TARGET_FILE_EXTENSION, DEFAULT_EXT);
//			for (File f : exportFiles)
//			{
//				if (f.getName().endsWith(ext) && f.isFile())
//				{
//					if (application == null)
//					{
//						// see if the servoy.properties and solution exports are specified (through system properties)
//						String servoyPropertiesFile = System.getProperty(TEST_PROPERTIES_FILE);
//						if (servoyPropertiesFile == null)
//						{
//							servoyPropertiesFile = Settings.FILE_NAME; // use normally found property file name from working dir
//						}
//
//						// create the test client that will be used for testing
//						try
//						{
//							application = TestProcess.instance();
//							int i = 300; // max 30 sec of waiting for stuff to get initialized
//							while (ApplicationServerSingleton.get() == null && i > 0)
//							{
//								i--;
//								Thread.sleep(100);
//							}
//							if (ApplicationServerSingleton.get() == null)
//							{
//								initException = new Exception("Cannot properly initialize test client.");
//							}
//						}
//						catch (IOException e1)
//						{
//							e1.printStackTrace();
//							initException = e1;
//						}
//						catch (InterruptedException e2)
//						{
//							e2.printStackTrace();
//							initException = e2;
//						}
//					}
//					log.info("Creating test suite for solution: " + f.getName());
//					SolutionJSTestSuite tmp = new SolutionJSTestSuite(f.getName().substring(0, f.getName().length() - ext.length()), f, application);
//					if (initException != null) tmp.setInitException(initException);
//					tmp.importSolutionAndCreateSuite(); // this is only needed so that the whole test suite structure is created at this time, not lazy (when UI that reports unit test results wants to show results in a tree-like structure, not flattened)
//					suite.addTest(tmp);
//					if (initException != null) break;
//				}
//			}
//
//			final TestProcess app = application;
//			TestSetup wrapper = new TestSetup(suite)
//			{
//				// Gently bring down Servoy after all tests have been run
//				@Override
//				protected void tearDown()
//				{
//					log.info("All solution tests were performed.");
//
//					if (app != null)
//					{
//						log.info("Shutting down test client.");
//						try
//						{
//							app.shutDown(false);
//							Thread.sleep(10000);
//						}
//						catch (InterruptedException e)
//						{
//							log.error(e);
//						}
//						app.shutDown(true);
//						log.info("Test client shut down was called.");
//					}
//				}
//			};
//
//			return wrapper;
//		}
//		else
//		{
//			throw new RuntimeException("Cannot find exported test solution(s) in: " + exportedSolutions.getAbsolutePath() +
//				". Define a 'servoy.test.target-exports' system property that points to a exported solution or a folder containing exported solutions to test.");
//		}
	}
}