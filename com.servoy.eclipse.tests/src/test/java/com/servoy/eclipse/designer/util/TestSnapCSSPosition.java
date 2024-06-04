/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.designer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;

/**
 * Unit tests for css position when snapping to a component edge.
 * @author emera
 */
public class TestSnapCSSPosition
{
	@Test
	public void testSnapToRight1() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: right }");

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "345", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the computed value", "340", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToRight2() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: right }");

		CSSPosition old = new CSSPosition("250", "460", "-1", "80", "-1", "30"); //component is anchored left-right, no width
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "340", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the computed value", "340", newPosition.left);
		assertEquals("css pos width should be set to the computed value", "100", newPosition.width);
	}

	@Test
	public void testSnapToRight3() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: left }"); //align the right side of the component to the left of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "160", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the left value copied from the target", "380", newPosition.right);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToLeft1() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: right }"); //align the left side of the component to the right of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "440", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the right value copied from the target", "440", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToLeft2() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: right }"); //align the left side of the component to the right of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "440", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "200", "-1", "-1", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the right value copied from the target", "440", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToTop1() throws Exception
	{
		String property = "top";
		JSONObject json = new JSONObject("{prop: top }");

		CSSPosition old = new CSSPosition("-1", "250", "150", "-1", "100", "30"); //component is anchored right, bottom
		CSSPosition newPosition = new CSSPosition("150", "280", "160", "236", "100", "30");
		CSSPosition targetPosition = new CSSPosition("-1", "80", "260", "-1", "180", "70"); //top property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos bottom should be set to the computed value", "300", newPosition.bottom);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}

	@Test
	public void testSnapToBottom1() throws Exception
	{
		String property = "bottom";
		JSONObject json = new JSONObject("{prop: bottom }");

		CSSPosition old = new CSSPosition("150", "250", "-1", "-1", "100", "30"); //component is anchored right, top
		CSSPosition newPosition = new CSSPosition("160", "280", "-1", "-1", "100", "30");
		CSSPosition targetPosition = new CSSPosition("260", "80", "-1", "-1", "180", "70"); //bottom property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos bottom should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos top should be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos top should be set to the computed value", "300", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}

	@Test
	public void testSnapToLeftBottom() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: left }");

		CSSPosition old = null; //the component is new
		CSSPosition newPosition = new CSSPosition("239", "-1", "-1", "74", "180", "30");
		CSSPosition targetPosition = new CSSPosition("110", "386", "-1", "-1", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		property = "bottom";
		json = new JSONObject("{prop: bottom }");
		targetPosition = new CSSPosition("269", "160", "-1", "340", "0", "30");
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the right value copied from the target", "386", newPosition.right);

		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos top should be set to the computed value", "269", newPosition.top);
		assertEquals("css pos height should be set", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToRight_Percentages1() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: right }");

		CSSPosition old = null; //component is new
		CSSPosition newPosition = new CSSPosition("136", "-1", "-1", "432", "80", "30");
		CSSPosition targetPosition = new CSSPosition("65", "-1", "-1", "30%", "320", "30"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the computed value", "calc(30% + 240px)", newPosition.left);
		assertEquals("css pos width should not be changed", "80", newPosition.width);
	}
	
	@Test
	public void testSnapToRight_Percentages2() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: right }");

		CSSPosition old = new CSSPosition("300", "50%", "-1", "200", "-1", "30");//width not set
		CSSPosition newPosition = new CSSPosition("136", "20%", "-1", "392", "-1", "30");
		CSSPosition targetPosition = new CSSPosition("65", "-1", "-1", "30%", "320", "30"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the computed value", "calc(30% + 200px)", newPosition.left);
		assertEquals("css pos width should be changed", "calc(50% - 200px)", newPosition.width);
	}
	
	@Test
	public void testSnapToRight_Percentages3() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: left }");

		CSSPosition old = null; //component is new
		CSSPosition newPosition = new CSSPosition("136", "-1", "-1", "112", "80", "30");
		CSSPosition targetPosition = new CSSPosition("65", "-1", "-1", "30%", "320", "30"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the left value computed from the target", "70%", newPosition.right);
		assertEquals("css pos width should not be changed", "80", newPosition.width);
	}
	
	@Test
	public void testSnapToLeft_Percentages1() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: left }");

		CSSPosition old = null; //component is new
		CSSPosition newPosition = new CSSPosition("136", "-1", "-1", "192", "80", "30");
		CSSPosition targetPosition = new CSSPosition("65", "20%", "-1", "-1", "320", "30"); //left property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the computed value", "calc(20% + 240px)", newPosition.right);
		assertEquals("css pos width should be set", "80", newPosition.width);
	}
	
	@Test
	public void testSnapToLeft_Percentages2() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: right }"); //align the left side of the component to the right of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "192", "100", "30");
		CSSPosition targetPosition = new CSSPosition("65", "-1", "-1", "30%", "50%", "30"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the right value copied from the target", "80%", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}
	
	@Test
	public void testSnapToMiddleHorizontally() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("170", "-1", "-1", "312", "80", "30");
		CSSPosition targetPosition = new CSSPosition("65", "20%", "-1", "-1", "320", "30"); //left property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.snapToMiddle(newPosition, old, containerSize, targetPosition, targetContainerSize, "left", "right", "width");

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the right value computed from the target", "calc(20% + 120px)", newPosition.right);
		assertEquals("css pos width should not be changed", "80", newPosition.width);
	}
	
	@Test
	public void testSnapToMiddleHorizontally2() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("170", "-1", "-1", "312", "80", "30");
		CSSPosition targetPosition = new CSSPosition("65", "20%", "-1", "30%", "-1", "30"); //anchored left-right

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.snapToMiddle(newPosition, old, containerSize, targetPosition, targetContainerSize, "left", "right", "width");

		assertFalse("css pos width should NOT be set", CSSPositionUtils.isSet(newPosition.width));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));

		assertEquals("css pos right should be set to the right value computed from the target", "calc(45% - 40px)", newPosition.right);
		assertEquals("css pos left should be set to the left value computed from the target", "calc(55% - 40px)", newPosition.left);
	}
	
	@Test
	public void testSnapToMiddleVertically() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("170", "-1", "-1", "312", "80", "30");
		CSSPosition targetPosition = new CSSPosition("-1", "-1", "20%", "240", "100", "240"); //top property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.snapToMiddle(newPosition, old, containerSize, targetPosition, targetContainerSize, "top", "bottom", "height");

		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos right should be set to the bottom value computed from the target", "calc(20% + 105px)", newPosition.bottom);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToEqualDist1() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("295", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition1 = new CSSPosition("83", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition2 = new CSSPosition("189", "-1", "-1", "53", "140", "30");
		int pos = 1; //below

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToDist(newPosition, old, containerSize, "top", "bottom", "height", targetPosition1, targetPosition2, containerSize, containerSize, pos);

		assertTrue("css pos top should be set", CSSPositionUtils.isSet(newPosition.top));
		assertFalse("css pos bottom should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("295", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToEqualDist2() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("83", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition1 = new CSSPosition("189", "-1", "-1", "53", "140", "30"); //top, left
		CSSPosition targetPosition2 = new CSSPosition("295", "-1", "-1", "53", "140", "30");//top, left
		int pos = -1; //above

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToDist(newPosition, old, containerSize, "top", "bottom", "height", targetPosition1, targetPosition2, containerSize, containerSize, pos);

		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.top));
		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("83", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToEqualDist_Percentage1() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("208", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition1 = new CSSPosition("10%", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition2 = new CSSPosition("calc(10% + 80px)", "-1", "-1", "53", "140", "30");
		int pos = 1; //below

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToDist(newPosition, old, containerSize, "top", "bottom", "height", targetPosition1, targetPosition2, containerSize, containerSize, pos);

		assertTrue("css pos top should be set", CSSPositionUtils.isSet(newPosition.top));
		assertFalse("css pos bottom should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("calc(10% + 160px)", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToEqualDist_Percentage2() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("48", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition1 = new CSSPosition("calc(10% + 80px)", "-1", "-1", "53", "140", "30"); //top, left
		CSSPosition targetPosition2 = new CSSPosition("calc(10% + 160px)", "-1", "-1", "53", "140", "30");//top, left
		int pos = -1; //above

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToDist(newPosition, old, containerSize, "top", "bottom", "height", targetPosition1, targetPosition2, containerSize, containerSize, pos);

		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.top));
		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("10%", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToEqualDist_Percentage3() throws Exception
	{
		CSSPosition old = null; // new component
		CSSPosition newPosition = new CSSPosition("128", "-1", "-1", "53", "140", "30");
		CSSPosition targetPosition1 = new CSSPosition("10%", "-1", "-1", "53", "140", "30"); //top, left
		CSSPosition targetPosition2 = new CSSPosition("calc(10% + 160px)", "-1", "-1", "53", "140", "30");//top, left
		int pos = 0; //between the targets

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToDist(newPosition, old, containerSize, "top", "bottom", "height", targetPosition1, targetPosition2, containerSize, containerSize, pos);

		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.top));
		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("calc(10% + 80px)", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}
	
	@Test
	public void testSnapToEndWidth_Percentage1() throws Exception
	{
		CSSPosition newPosition = new CSSPosition("157", "calc(60% - 256px)", "-1", "-1", "256", "30");
		CSSPosition targetPosition = new CSSPosition("65", "20%", "-1", "-1", "40%", "30"); //top, right

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToEndSize(newPosition, containerSize, targetPosition, containerSize, "left", "right", "width");

		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("20%", newPosition.right);
		assertEquals("css pos width should be the same as the target width", "40%", newPosition.width);
	}
	
	@Test
	public void testSnapToEndWidth_Percentage2() throws Exception
	{
		CSSPosition newPosition = new CSSPosition("146", "-1", "-1", "20%", "256", "30");
		CSSPosition targetPosition = new CSSPosition("65", "-1", "-1", "20%", "40%", "30"); //top, left

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToEndSize(newPosition, containerSize, targetPosition, containerSize, "left", "right", "width");

		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("20%", newPosition.left);
		assertEquals("css pos width should be the same as the target width", "40%", newPosition.width);
	}

	@Test
	public void testSnapToSameWidth_Percentage1() throws Exception
	{
		CSSPosition newPosition = new CSSPosition("157", "-1", "-1", "27%", "256", "30");
		CSSPosition targetPosition = new CSSPosition("65", "-1", "-1", "20%", "40%", "30"); //left & width

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToSameSize(newPosition, containerSize, targetPosition, containerSize, "left", "right", "width");

		assertTrue("css pos left should not be changed", CSSPositionUtils.isSet(newPosition.left));
		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("27%", newPosition.left);
		assertEquals("css pos width should be the same as the target width", "40%", newPosition.width);
	}
	
	@Test
	public void testSnapToSameWidth_Percentage2() throws Exception
	{
		CSSPosition newPosition = new CSSPosition("157", "-1", "-1", "27%", "256", "30");
		CSSPosition targetPosition = new CSSPosition("65", "40%", "-1", "20%", "-1", "30"); //left-right

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480); // use the same container size
		DesignerUtil.snapToSameSize(newPosition, containerSize, targetPosition, containerSize, "left", "right", "width");

		assertTrue("css pos left should not be changed", CSSPositionUtils.isSet(newPosition.left));
		assertFalse("css pos width should NOT be set", CSSPositionUtils.isSet(newPosition.width));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));

		assertEquals("27%", newPosition.left);
		assertEquals("css pos right should be computed from the target and left values", "33%", newPosition.right);
	}
}