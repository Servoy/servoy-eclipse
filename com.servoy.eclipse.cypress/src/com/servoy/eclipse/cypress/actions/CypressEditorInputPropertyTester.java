package com.servoy.eclipse.cypress.actions;

import org.eclipse.core.expressions.PropertyTester;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.model.util.ServoyLog;

public class CypressEditorInputPropertyTester extends PropertyTester {
	private final CypressTestDiscoveryService discoveryService = new CypressTestDiscoveryService();

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof PersistEditorInput persistInput)) {
			return false;
		}

		if ("cypressTestExists".equals(property)) {
			String name = persistInput.getName();
			boolean result = name != null && discoveryService.hasTest(name);
			if (!result) {
				ServoyLog.logInfo("CypressEditorInputPropertyTester: name='" + name + "' result=" + result);
			}
			return result;
		}

		return false;
	}
}
