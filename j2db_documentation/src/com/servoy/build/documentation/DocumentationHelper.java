package com.servoy.build.documentation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.server.shared.IClientInformation;
import com.servoy.j2db.util.ServoyException;

public class DocumentationHelper
{
	public static final String DOCS_PACKAGE = "com.servoy.j2db.documentation.scripting.docs.";

	private static Map<String, String> returnedTypesEquiv = fillReturnedTypesMapping();

	public static Method findRealMethod(Class< ? > realClass, String name)
	{
		for (Method m : realClass.getMethods())
			if (m.getName().equals("js_" + name) || m.getName().equals("jsFunction_" + name) || m.getName().equals("jsConstructor" + name)) return m;
		for (Method m : realClass.getDeclaredMethods())
			if (m.getName().equals("js_" + name) || m.getName().equals("jsFunction_" + name) || m.getName().equals("jsConstructor" + name)) return m;
		return null;
	}

	public static Field findRealField(Class< ? > realClass, String name)
	{
		for (Field f : realClass.getFields())
			if (f.getName().equals(name)) return f;
		for (Field f : realClass.getDeclaredFields())
			if (f.getName().equals(name)) return f;
		return null;
	}

	public static String getCorrectedQualifiedName(Class< ? > clazz)
	{
		String qname = clazz.getCanonicalName();
		Class< ? > ec = clazz;
		while (ec.getEnclosingClass() != null)
		{
			qname = qname.replace(ec.getEnclosingClass().getSimpleName() + ".", ec.getEnclosingClass().getSimpleName() + "$");
			ec = ec.getEnclosingClass();
		}
		return qname;
	}

	public static Map<String, String> fillReturnedTypesMapping()
	{
		Map<String, String> rtEquiv = new HashMap<String, String>();
		rtEquiv.put(Color.class.getCanonicalName(), DOCS_PACKAGE + "String");
		rtEquiv.put(Dimension.class.getCanonicalName(), Dimension.class.getSimpleName());
		rtEquiv.put(Exception.class.getCanonicalName(), ServoyException.class.getCanonicalName());
		rtEquiv.put(IFoundSetInternal.class.getCanonicalName(), FoundSet.class.getCanonicalName());
		rtEquiv.put(FoundSet.class.getCanonicalName(), FoundSet.class.getCanonicalName());
		rtEquiv.put(IRecordInternal.class.getCanonicalName(), Record.class.getCanonicalName());
		rtEquiv.put(IRecordInternal[].class.getCanonicalName(), Record[].class.getCanonicalName());
		rtEquiv.put("Object", DOCS_PACKAGE + "Object");
		rtEquiv.put(Object.class.getCanonicalName(), DOCS_PACKAGE + "Object");
		rtEquiv.put(Object[].class.getCanonicalName(), DOCS_PACKAGE + "Object[]");
		rtEquiv.put(Object[][].class.getCanonicalName(), DOCS_PACKAGE + "Object[][]");
		rtEquiv.put(String.class.getCanonicalName(), DOCS_PACKAGE + "String");
		rtEquiv.put(String[].class.getCanonicalName(), DOCS_PACKAGE + "String[]");
		rtEquiv.put(Boolean.class.getCanonicalName(), DOCS_PACKAGE + "Boolean");
		rtEquiv.put(boolean.class.getCanonicalName(), DOCS_PACKAGE + "Boolean");
		rtEquiv.put(byte[].class.getCanonicalName(), byte[].class.getSimpleName());
		rtEquiv.put("Number", DOCS_PACKAGE + "Number");
		rtEquiv.put(Number.class.getCanonicalName(), DOCS_PACKAGE + "Number");
		rtEquiv.put(double.class.getCanonicalName(), DOCS_PACKAGE + "Number");
		rtEquiv.put(float.class.getCanonicalName(), DOCS_PACKAGE + "Number");
		rtEquiv.put(int.class.getCanonicalName(), DOCS_PACKAGE + "Number");
		rtEquiv.put(int[].class.getCanonicalName(), DOCS_PACKAGE + "Number[]");
		rtEquiv.put(long.class.getCanonicalName(), DOCS_PACKAGE + "Number");
		rtEquiv.put(long[].class.getCanonicalName(), DOCS_PACKAGE + "Number[]");
		rtEquiv.put(void.class.getCanonicalName(), void.class.getSimpleName());
		rtEquiv.put(Point.class.getCanonicalName(), Point.class.getSimpleName());
		rtEquiv.put(Insets.class.getCanonicalName(), Insets.class.getSimpleName());
		rtEquiv.put(NativeArray.class.getCanonicalName(), DOCS_PACKAGE + "Array");
		rtEquiv.put(NativeObject.class.getCanonicalName(), DOCS_PACKAGE + "Object");
		rtEquiv.put(Date.class.getCanonicalName(), DOCS_PACKAGE + "Date");
		rtEquiv.put(IClientInformation.class.getCanonicalName(), "com.servoy.extensions.plugins.maintenance.JSClientInformation");
		rtEquiv.put(IClientInformation[].class.getCanonicalName(), "com.servoy.extensions.plugins.maintenance.JSClientInformation[]");
		rtEquiv.put(com.servoy.j2db.FormController.JSForm.class.getCanonicalName(), getCorrectedQualifiedName(com.servoy.j2db.FormController.JSForm.class));
		rtEquiv.put("com.servoy.extensions.plugins.window.menu.AbstractMenuItem", "com.servoy.extensions.plugins.window.menu.MenuItem");
		rtEquiv.put("com.servoy.extensions.plugins.window.menu.JSMenuItem", "com.servoy.extensions.plugins.window.menu.MenuItem");
		return rtEquiv;
	}

	public static String removeArraySuffix(String type)
	{
		if (type != null)
		{
			String t = type;
			while (t.endsWith("[]"))
				t = t.substring(0, t.length() - 2);
			return t;
		}
		else
		{
			return null;
		}
	}

	public static String mapReturnedType(String returnedType, boolean annotated, String clazzName)
	{
		String resultStr = null;
		if (returnedType != null)
		{
			resultStr = returnedTypesEquiv.get(returnedType);
			if (resultStr == null)
			{
				String suffix = "";
				if (returnedType.endsWith("[]"))
				{
					String original = returnedType;
					returnedType = returnedType.substring(0, returnedType.indexOf('['));
					suffix = original.substring(returnedType.length());
				}
				if (annotated)
				{
					resultStr = returnedType + suffix;
				}
				else
				{
					System.out.println("Unpublished return type " + returnedType + " in " + clazzName + "."); //;
					resultStr = returnedTypesEquiv.get(Object.class.getCanonicalName()) + suffix;
				}
			}
		}
		return resultStr;
	}
}
