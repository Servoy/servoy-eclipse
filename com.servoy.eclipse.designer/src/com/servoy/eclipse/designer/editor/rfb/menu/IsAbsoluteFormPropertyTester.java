package com.servoy.eclipse.designer.editor.rfb.menu;

import org.eclipse.core.expressions.PropertyTester;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;

public class IsAbsoluteFormPropertyTester extends PropertyTester
{

	public IsAbsoluteFormPropertyTester()
	{
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
	{
		BaseVisualFormEditor activeEditor = DesignerUtil.getActiveEditor();
		if (activeEditor != null) return !activeEditor.getForm().isResponsiveLayout();
		return false;
	}

}
