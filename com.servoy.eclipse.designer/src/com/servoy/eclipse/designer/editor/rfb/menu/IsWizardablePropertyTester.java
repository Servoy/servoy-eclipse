package com.servoy.eclipse.designer.editor.rfb.menu;

import org.eclipse.core.expressions.PropertyTester;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.WebComponent;

public class IsWizardablePropertyTester extends PropertyTester
{
	public IsWizardablePropertyTester()
	{
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
	{
		if (receiver instanceof PersistContext)
		{
			PersistContext persistContext = (PersistContext)receiver;
			if (persistContext.getPersist() instanceof WebComponent)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
					.getWebComponentSpecification(((WebComponent)persistContext.getPersist()).getTypeName());
				return spec.getAllPropertiesNames().stream()//
					.filter(prop -> spec.getProperty(prop) != null && "autoshow".equals(spec.getProperty(prop).getTag("wizard")))//
					.findAny().isPresent();
			}
		}
		return false;
	}
}
