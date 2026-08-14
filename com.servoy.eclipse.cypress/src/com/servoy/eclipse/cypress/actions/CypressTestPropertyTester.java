package com.servoy.eclipse.cypress.actions;

import org.eclipse.core.expressions.PropertyTester;

public class CypressTestPropertyTester extends PropertyTester {
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof CypressFormTestTarget target)) {
			return false;
		}

		if ("isSingleFormTest".equals(property)) {
			return !target.isSolutionLevel();
		} else if ("isSolutionLevelTest".equals(property)) {
			return target.isSolutionLevel();
		}

		return false;
	}
}
