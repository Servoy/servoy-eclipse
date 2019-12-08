package com.servoy.eclipse.ui.asm.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

import com.servoy.eclipse.ui.tweaks.bytecode.weave.ImageDescriptorWeaver;

public class WeaveClassWithASMCode
{

	public static void main(String[] args)
	{
		WovenClass wovenClass = new WovenClass()
		{

			@Override
			public byte[] getBytes()
			{
				try (BufferedInputStream in = new BufferedInputStream(ImageDescriptor.class.getResource("ImageDescriptor.class").openStream()))
				{
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					int x;
					byte[] buffer = new byte[4096];
					try
					{
						while ((x = in.read(buffer)) >= 0)
						{
							bytes.write(buffer, 0, x);
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					return bytes.toByteArray();
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return new byte[0];
				}
			}


			@Override
			public void setBytes(byte[] newBytes)
			{
				try (BufferedOutputStream bufferedOut = new BufferedOutputStream(
					new FileOutputStream("dumpedClasses/org/eclipse/jface/resource/ImageDescriptor.class")))
				{
					bufferedOut.write(newBytes);
					System.out.println("Altered/weaved class file was written to dumpedClasses/org/eclipse/jface/resource/ImageDescriptor.class");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			@Override
			public boolean isWeavingComplete()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public int getState()
			{
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public ProtectionDomain getProtectionDomain()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getDynamicImports()
			{
				return new ArrayList<String>();
			}

			@Override
			public Class< ? > getDefinedClass()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getClassName()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public BundleWiring getBundleWiring()
			{
				// TODO Auto-generated method stub
				return null;
			}
		};

		new ImageDescriptorWeaver().weaveClass(wovenClass);
	}

}
