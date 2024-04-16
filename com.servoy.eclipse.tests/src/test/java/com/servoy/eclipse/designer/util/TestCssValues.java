package com.servoy.eclipse.designer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestCssValues {
	
	@Test
	public void testParseCSSValueNotSet() throws Exception
	{
		String value = "-1";
		CSSValue css = new CSSValue(value);
		
		assertEquals(-1, css.getPixels());
		assertEquals(0, css.getPercentage());
		assertEquals(value, css.toString());
		assertFalse(css.isSet());
	}
	
	@Test
	public void testParseCSSValuePixels() throws Exception
	{
		String value = "120";
		CSSValue css = new CSSValue(value);
		
		assertEquals(120, css.getPixels());
		assertEquals(0, css.getPercentage());
		assertEquals(value, css.toString());
		assertTrue(css.isPx());
	}
	
	@Test
	public void testParseCSSValuePercentage() throws Exception
	{
		String value = "50%";
		CSSValue css = new CSSValue(value);
		
		assertEquals(0, css.getPixels());
		assertEquals(50, css.getPercentage());
		assertEquals(value, css.toString());
		assertFalse(css.isPx());
	}
	
	@Test
	public void testParseCSSValueCalc() throws Exception
	{
		String value = "calc(50% + 10px)";
		CSSValue css = new CSSValue(value);
		
		assertEquals(10, css.getPixels());
		assertEquals(50, css.getPercentage());
		assertEquals(value, css.toString());
		assertFalse(css.isPx());
	}
	
	@Test
	public void testParseCSSValueCalcMinus() throws Exception
	{
		String value = "calc(100% - 80px)";
		CSSValue css = new CSSValue(value);
		
		assertEquals(-80, css.getPixels());
		assertEquals(100, css.getPercentage());
		assertEquals(value, css.toString());
		assertFalse(css.isPx());
	}
}
