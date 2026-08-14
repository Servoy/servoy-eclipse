package com.servoy.eclipse.cypress.actions;

import java.util.List;

public interface CypressFormTestTarget {
	String getFormName();

	boolean isSolutionLevel();

	List<String> getTestFormNames();
}
