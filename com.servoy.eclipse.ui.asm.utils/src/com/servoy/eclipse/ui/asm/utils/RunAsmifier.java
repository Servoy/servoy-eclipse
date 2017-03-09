package com.servoy.eclipse.ui.asm.utils;

import org.objectweb.asm.util.ASMifier;

/**
 * The idea is that we can write plain java and then run this asmifier on it - and that will generate runtime class generating asm code that we can
 * use/integrate in our actual class weaver (so we don't have to write everything by hand).<br>
 *
 * For example we want a particular method, just run the asmifier on the class, take that block from the generated asm code
 * and use it in the weaver.
 *
 * @author acostescu
 */
public class RunAsmifier
{

	public static void main(String[] args)
	{
		try
		{
			ASMifier.main(new String[] { "org.eclipse.jface.resource.ImageDescriptor" });
//			ASMifier.main(new String[] { "org.eclipse.jface.resource.ImageDescriptor$OriginalCreateFromFile" });
//			ASMifier.main(new String[] { "org.eclipse.jface.resource.ImageDescriptor$OriginalCreateFromURL" });
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
