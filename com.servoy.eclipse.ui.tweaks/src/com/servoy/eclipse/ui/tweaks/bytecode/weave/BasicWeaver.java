/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.tweaks.bytecode.weave;

import org.eclipse.core.runtime.Platform;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * Utility super class for any class weaver.
 * Performs common operations that most if not all weavers will need to do.
 *
 * @author acostescu
 */
public abstract class BasicWeaver
{

	public void weaveClass(WovenClass wovenClass)
	{
		byte[] byteCode = wovenClass.getBytes();
		ClassReader classReader = new ClassReader(byteCode);

		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);

		this.weaveClassInternal(classNode);

		ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_FRAMES)
		{
			@Override
			protected String getCommonSuperClass(String type1, String type2)
			{

				Class< ? > c, d;

				try
				{
					c = Platform.getBundle("org.eclipse.jface").loadClass(type1.replace('/', '.'));
					d = Platform.getBundle("org.eclipse.jface").loadClass(type2.replace('/', '.'));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e.toString());
				}
				if (c.isAssignableFrom(d))
				{
					return type1;
				}
				if (d.isAssignableFrom(c))
				{
					return type2;
				}
				if (c.isInterface() || d.isInterface())
				{
					return "java/lang/Object";
				}
				else
				{
					do
					{
						c = c.getSuperclass();
					}
					while (!c.isAssignableFrom(d));
					return c.getName().replace('.', '/');
				}

			}
		};
		classNode.accept(cw);

		wovenClass.setBytes(cw.toByteArray());
	}

	protected abstract void weaveClassInternal(ClassNode classNode);

	protected MethodNode getMethod(ClassNode classNode, String name, String descriptor)
	{
		for (Object o : classNode.methods)
		{
			MethodNode methodNode = (MethodNode)o;
			if (methodNode.name.equals(name) && (descriptor == null || descriptor.equals(methodNode.desc))) return methodNode;
		}

		return null;
	}


}
