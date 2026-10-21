package com.servoy.eclipse.model.tests.integrationpde.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.util.ModelUtils;

class ModelUtilsTestModeTest
{
	@Test
	@DisplayName("isTestRunning returns true when PDE JUnit runtime bundle is present")
	void isTestRunningReturnsTrueInPDETestEnvironment()
	{
		assertTrue(ModelUtils.isTestRunning(),
			"isTestRunning() should return true when running as a PDE JUnit plugin test");
	}
}
