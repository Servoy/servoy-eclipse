package com.servoy.eclipse.ui.views.solutionexplorer;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager.ContainerPackageReader;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.UserNodeType;

public class WebPackageDecorator implements ILightweightLabelDecorator
{

	@Override
	public void addListener(ILabelProviderListener listener)
	{
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public boolean isLabelProperty(Object element, String property)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener)
	{

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
	public void decorate(Object element, IDecoration decoration)
	{
		if (element instanceof PlatformSimpleUserNode)
		{
			PlatformSimpleUserNode platformSimpleUserNode = (PlatformSimpleUserNode)element;
			ImageDescriptor imgd = null;
			if (platformSimpleUserNode.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE ||
				platformSimpleUserNode.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE)
			{
				imgd = Activator.loadImageDescriptorFromBundle("bean_decorator.png");
			}
			if (platformSimpleUserNode.getType() == UserNodeType.SERVICES_NONPROJECT_PACKAGE ||
				platformSimpleUserNode.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE)
			{
				imgd = Activator.loadImageDescriptorFromBundle("service_decorator.png");
			}
			if (platformSimpleUserNode.getType() == UserNodeType.LAYOUT_NONPROJECT_PACKAGE ||
				platformSimpleUserNode.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				imgd = Activator.loadImageDescriptorFromBundle("layout_decorator.png");
			}
			if (platformSimpleUserNode.getRealType() == UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE)
			{
				Object realObject = platformSimpleUserNode.getRealObject();
				if (realObject instanceof IProject)
				{
					imgd = resolveWebPackageImage((IProject)realObject);
				}
			}
			if (imgd != null)
			{
				decoration.addOverlay(imgd, IDecoration.BOTTOM_RIGHT);
			}
		}
	}

	private ImageDescriptor resolveWebPackageImage(IProject iProject)
	{
		String imageFile = null;

		String packageType = WebComponentSpecProvider.getInstance().getPackageType(iProject.getName());
		if (IPackageReader.WEB_COMPONENT.equals(packageType)) imageFile = "bean_decorator.png";
		else if (IPackageReader.WEB_LAYOUT.equals(packageType)) imageFile = "layout_decorator.png";
		else if (IPackageReader.WEB_SERVICE.equals(WebServiceSpecProvider.getInstance().getPackageType(iProject.getName())))
			imageFile = "service_decorator.png";
		else
		{
			//now we have to read the package type from the manifest
			imageFile = "bean_decorator.png";
			if (iProject.getFile(new Path("META-INF/MANIFEST.MF")).exists())
			{
				String notReferencesProjectPackageType = new ContainerPackageReader(new File(iProject.getLocationURI()), iProject).getPackageType();
				if (Package.IPackageReader.WEB_SERVICE.equals(notReferencesProjectPackageType)) imageFile = "service_decorator.png";
				else if (Package.IPackageReader.WEB_LAYOUT.equals(notReferencesProjectPackageType)) imageFile = "layout_decorator.png";
			}

		}
		return Activator.loadImageDescriptorFromBundle(imageFile);
	}

}
