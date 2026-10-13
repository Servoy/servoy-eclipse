package com.servoy.eclipse.ui.tests.plainjunit.dialogs;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
	LoginTokenResponseTest.class,
	ServoyLoginDialogCloudStateTest.class,
	ServoyLoginDialogDoLoginBehaviorTest.class,
	ServoyLoginDialogGetLoginTokenTest.class
})
public class AllLoginDialogTests
{
}
