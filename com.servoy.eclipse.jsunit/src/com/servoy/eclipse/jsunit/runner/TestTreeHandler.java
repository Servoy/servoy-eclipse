/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Class that is able to manipulate the TestSuite hierarchy of a given suite.
 * 
 * @author acostescu
 */
public class TestTreeHandler
{

	public final static Object NEXT_CHILD_GROUP = null;

	protected String[] sourceTestTree;
	protected TestSuite rootSuite;


	public TestTreeHandler(String[] sourceTestTree, TestSuite rootSuite)
	{
		this.sourceTestTree = sourceTestTree;
		this.rootSuite = rootSuite;
	}

	/**
	 * Creates a dummy test suite hierarchy (with names only) based on the given flattened {@link #sourceTestTree} in {@link #rootSuite}.
	 */
	public void createDummyTestTree()
	{
		// we must create a hierarchy of JUnit test cases/test suites to match the JSUnit ones that are now in the tree;
		// this is only needed for the presentation or running tests/available tests done by the JUnit Eclipse plugin,
		// not for the actual run of the tests

		// first find out which one is a test case and which one is a test suite
		boolean[] isTestSuite = new boolean[sourceTestTree.length];
		int currentParent = 0;
		boolean hasChildren = true; // always mark this as test suite
		for (int i = 2; i < sourceTestTree.length; i++)
		{
			String currentElement = sourceTestTree[i];
			if (currentElement == NEXT_CHILD_GROUP)
			{
				isTestSuite[currentParent++] = hasChildren;
				while ((currentParent < sourceTestTree.length) && (sourceTestTree[currentParent] == NEXT_CHILD_GROUP))
				{
					currentParent++;
				}
				hasChildren = false;
			}
			else
			{
				hasChildren = true;
			}
		}

		// create & link test cases/test suites
		TestSuite[] suites = new TestSuite[sourceTestTree.length];
		suites[0] = rootSuite;
		if (sourceTestTree.length > 0)
		{
			rootSuite.setName(sourceTestTree[0]);
		}
		currentParent = 0;
		for (int i = 2; i < sourceTestTree.length; i++)
		{
			String currentElement = sourceTestTree[i];
			if (currentElement == NEXT_CHILD_GROUP)
			{
				currentParent++;
				while ((currentParent < sourceTestTree.length) && (sourceTestTree[currentParent] == NEXT_CHILD_GROUP))
				{
					currentParent++;
				}
			}
			else
			{
				Test currentTest = null;
				if (isTestSuite[i])
				{
					currentTest = new TestSuite(currentElement);
					suites[i] = (TestSuite)currentTest;
				}
				else
				{
					currentTest = new DummyTestCase(currentElement);
				}
				suites[currentParent].addTest(currentTest);
			}
		}
	}

	/**
	 * Fills the given list with the sequencial execution order of Tests in {@link #rootSuite}.
	 * @param list
	 */
	public void fillTestListSequencialOrder(List<Test> list)
	{
		fillTestListSequencialOrderInternal(rootSuite, list);
	}

	protected void fillTestListSequencialOrderInternal(TestSuite suite, List<Test> list)
	{
		Enumeration<Test> children = suite.tests();
		while (children.hasMoreElements())
		{
			Test child = children.nextElement();
			list.add(child);
			if (child instanceof TestSuite)
			{
				fillTestListSequencialOrderInternal((TestSuite)child, list);
			}
		}
	}

}
