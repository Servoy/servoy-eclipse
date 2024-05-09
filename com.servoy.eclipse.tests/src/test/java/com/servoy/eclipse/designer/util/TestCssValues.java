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
	
	@Test
	public void testAddition() throws Exception
	{
		String l = "calc(30% - 10px)";
		String w = "calc(30% + 10px)";
		CSSValue left = new CSSValue(l, 640, false);
		CSSValue width = new CSSValue(w, 640, false);
		CSSValue right = left.plus(width).toHigherProperty();
		
		assertEquals(0, right.getPixels());
		assertEquals(40, right.getPercentage());
		assertEquals("40%", right.toString());
		assertFalse(right.isPx());
		assertTrue(right.isPercentage());
	}
	
	@Test
	public void testSubtraction() throws Exception
	{
		String r = "calc(100% - 80px)";
		String w = "calc(30% + 10px)";
		CSSValue right = new CSSValue(r, 640, true);
		CSSValue width = new CSSValue(w, 640, false);
		CSSValue left = right.minus(width);
		
		assertEquals(560, right.getAsPixels());
		assertEquals(202, width.getAsPixels());
		int res = right.getAsPixels() - width.getAsPixels();
		
		CSSValue expected = new CSSValue("calc(70% - 90px)", 640, false);
		assertEquals(res, expected.getAsPixels());
				
		assertEquals(-90, left.getPixels());
		assertEquals(70, left.getPercentage());
		assertEquals(res, left.getAsPixels());
		assertEquals(expected.toString(), left.toString());
		assertFalse(left.isPx());
	}
	
	@Test
	public void testSubtraction2() throws Exception
	{
		String r = "calc(20% - 80px)";
		String w = "calc(30% + 10px)";
		CSSValue right = new CSSValue(r, 640, true);
		CSSValue width = new CSSValue(w, 640, false);
		CSSValue left = right.minus(width);
		
		assertEquals(592, right.getAsPixels());
		assertEquals(202, width.getAsPixels());
		int res = right.getAsPixels() - width.getAsPixels();
		
		CSSValue expected = new CSSValue("calc(50% + 70px)", 640, false);
		assertEquals(res, expected.getAsPixels());
				
		assertEquals(70, left.getPixels());
		assertEquals(50, left.getPercentage());
		assertEquals(res, left.getAsPixels());
		assertEquals(expected.toString(), left.toString());
		assertFalse(left.isPx());
	}
	
	@Test
	public void testSubtraction3() throws Exception
	{
		String r = "calc(20% - 80px)";
		String l = "calc(50% - 90px)";
		CSSValue right = new CSSValue(r, 640, true);
		CSSValue left = new CSSValue(l, 640, false);
		
		assertEquals(592, right.getAsPixels());
		assertEquals(230, left.getAsPixels());
		
		CSSValue width = right.minus(left);
		int res = right.getAsPixels() - left.getAsPixels();
		
		CSSValue expected = new CSSValue("calc(30% + 170px)", 640, false);
		assertEquals(res, expected.getAsPixels());
				
		assertEquals(170, width.getPixels());
		assertEquals(30, width.getPercentage());
		assertEquals(res, width.getAsPixels());
		assertEquals(expected.toString(), width.toString());
	}
	
	@Test
	public void testSubtraction4() throws Exception
	{
		String r = "80px";
		String w = "calc(30% + 10px)";
		CSSValue right = new CSSValue(r, 640, true);
		CSSValue width = new CSSValue(w, 640, false);
		CSSValue left = right.minus(width);
		int res = right.getAsPixels() - width.getAsPixels();
		
		CSSValue expected = new CSSValue("calc(70% - 90px)", 640, false);
		assertEquals(res, expected.getAsPixels());
				
		assertEquals(-90, left.getPixels());
		assertEquals(70, left.getPercentage());
		assertEquals(res, left.getAsPixels());
		assertEquals(expected.toString(), left.toString());
		assertFalse(left.isPx());
	}
	
	@Test
	public void testSubtraction5() throws Exception
	{
		String r = "80px";
		String w = "30%";
		CSSValue right = new CSSValue(r, 640, true);
		CSSValue width = new CSSValue(w, 640, false);
		CSSValue left = right.minus(width);
		int res = right.getAsPixels() - width.getAsPixels();
		
		CSSValue expected = new CSSValue("calc(70% - 80px)", 640, false);
		assertEquals(res, expected.getAsPixels());
				
		assertEquals(-80, left.getPixels());
		assertEquals(70, left.getPercentage());
		assertEquals(res, left.getAsPixels());
		assertEquals(expected.toString(), left.toString());
		assertFalse(left.isPx());
	}
	
	@Test
	public void testSubtraction6() throws Exception
	{
		String r = "20%";
		String w = "160";
		CSSValue right = new CSSValue(r, 640, true);
		CSSValue width = new CSSValue(w, 640, false);
		CSSValue left = right.minus(width);
		int res = right.getAsPixels() - width.getAsPixels(); //TODO check
		assertEquals(512, right.getAsPixels());
		assertEquals(160, width.getAsPixels());
		
		CSSValue expected = new CSSValue("calc(80% - 160px)", 640, false);
		assertEquals(expected.toString(), left.toString());
		assertEquals(res, expected.getAsPixels());
				
		assertEquals(-160, left.getPixels());
		assertEquals(80, left.getPercentage());
		assertEquals(res, left.getAsPixels());
		assertFalse(left.isPx());
	}
	
	@Test
	public void testDivision() throws Exception
	{
		String value = "calc(100% - 80px)";
		CSSValue op = new CSSValue(value);
		CSSValue css = op.div(2);
		
		assertEquals(-40, css.getPixels());
		assertEquals(50, css.getPercentage());
		assertEquals("calc(50% - 40px)", css.toString());
		assertFalse(css.isPx());
	}
}
