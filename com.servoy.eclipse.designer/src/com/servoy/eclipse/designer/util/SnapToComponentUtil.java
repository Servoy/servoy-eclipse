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

import java.awt.Dimension;
import java.awt.Point;

import org.eclipse.core.runtime.Assert;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.util.PersistFinder;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportCSSPosition;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;

/**
 * Handlers used for setting the correct anchors for the snapped component.
 * @author emera
 */
public class SnapToComponentUtil
{
	public static CSSPosition cssPositionFromJSON(Form form, IPersist persist, JSONObject properties)
	{
		CSSPosition newPosition;
		JSONObject obj = properties.getJSONObject("cssPos");
		CSSPosition position = ((ISupportCSSPosition)persist).getCssPosition();
		if (position == null)
		{
			newPosition = new CSSPosition(properties.optString("y", "0"), "-1", "-1", properties.optString("x", "0"),
				properties.optString("w", "0"), properties.optString("h", "0"));
		}
		else
		{
			//adjust the location/size for the coordinate which was not snapped
			//for instance a component is only snapped on the left but we need to update the top property as well
			Point oldLocation = CSSPositionUtils.getLocation((ISupportCSSPosition)persist);
			java.awt.Dimension oldSize = CSSPositionUtils.getSize((ISupportCSSPosition)persist);
			newPosition = CSSPositionUtils.adjustCSSPosition((ISupportCSSPosition)persist,
				properties.optInt("x", oldLocation.x), properties.optInt("y", oldLocation.y),
				properties.optInt("width", oldSize.width),
				properties.optInt("height", oldSize.height), properties.optBoolean("move", false));
		}

		AbstractContainer componentParent = CSSPositionUtils.getParentContainer((ISupportSize)persist);
		if (position != null && (properties.has("width") || properties.has("height")))
		{
			//could be a resize
			checkResize(newPosition, properties, position, componentParent, "width");
			checkResize(newPosition, properties, position, componentParent, "height");
		}

		for (String property : obj.keySet())
		{
			JSONObject jsonObject = obj.getJSONObject(property);
			ISupportCSSPosition target = getTarget(form, jsonObject.optString("uuid"));
			ISupportCSSPosition targetContainer = getTarget(form, jsonObject.optString("container"));
			CSSPosition targetContainerCssPos = targetContainer != null ? targetContainer.getCssPosition() : null;
			AbstractContainer targetParent = target != null ? CSSPositionUtils.getParentContainer(target) : null;

			switch (property)
			{
				case "middleH" :
					snapToMiddle(newPosition, position, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), targetContainerCssPos,
						"left", "right", "width");
					break;

				case "middleV" :
					snapToMiddle(newPosition, position, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), targetContainerCssPos,
						"top", "bottom", "height");
					break;

				case "endWidth" :
				case "endHeight" :
					break;

				case "distX" :
					snapToDist(jsonObject, newPosition, position, componentParent.getSize(), form, "left", "right", "width");
					break;

				case "distY" :
					snapToDist(jsonObject, newPosition, position, componentParent.getSize(), form, "top", "bottom", "height");
					break;

				case "sameWidth" :
					snapToSameSize(newPosition, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), "left",
						"right", "width");
					break;

				case "sameHeight" :
					snapToSameSize(newPosition, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), "top",
						"bottom", "height");
					break;

				default :
					setCssValue(jsonObject, property, newPosition, position, target.getCssPosition(), componentParent.getSize(), targetParent.getSize(),
						targetContainerCssPos);
			}
		}

		if (obj.has("endWidth") || obj.has("endHeight"))
		{
			CSSValue startX = new CSSValue(properties.optInt("x", 0) + "px", componentParent.getSize().width, false);
			CSSValue startY = new CSSValue(properties.optInt("y", 0) + "px", componentParent.getSize().height, false);
			handleEndSize(newPosition, obj, componentParent, form, "endWidth", "left", "right", "width", startX);
			handleEndSize(newPosition, obj, componentParent, form, "endHeight", "top", "bottom", "height", startY);
		}

		return newPosition;
	}

	private static void checkResize(CSSPosition newPosition, JSONObject obj, CSSPosition position, AbstractContainer componentParent, String sizeProperty)
	{
		if (obj.has(sizeProperty) && getCssValue(position, sizeProperty, componentParent.getSize()).isSet())
		{
			CSSValue oldSize = getCssValue(position, sizeProperty, componentParent.getSize());
			CSSValue newSize = getCssValue(newPosition, sizeProperty, componentParent.getSize());
			if (oldSize.getAsPixels() != newSize.getAsPixels())
			{
				//resize
				CSSValue size = newSize;
				if (oldSize.getPercentage() > 0)
				{
					if (oldSize.getAsPixels() > newSize.getAsPixels())
					{
						CSSValue delta = oldSize.minus(newSize);
						size = oldSize.minus(delta);
					}
					else
					{
						CSSValue delta = newSize.minus(oldSize);
						size = oldSize.plus(delta);
					}
				}
				setCssValue(newPosition, sizeProperty, size);
			}
		}
	}

	public static void snapToSameSize(CSSPosition newPosition, java.awt.Dimension parentSize,
		CSSPosition targetPosition, java.awt.Dimension targetParentSize, String lowerProperty, String higherProperty, String sizeProperty)
	{
		CSSValue sizeValue = getCssValue(targetPosition, sizeProperty, targetParentSize);
		CSSValue lowerPropertyValue = getCssValue(targetPosition, lowerProperty, targetParentSize);
		if (sizeValue.isSet())
		{
			setCssValue(newPosition, sizeProperty, sizeValue);
			if (getCssValue(newPosition, lowerProperty, parentSize).isSet() && getCssValue(newPosition, higherProperty, parentSize).isSet())
			{
				//source component was anchored left-right or top-bottom, need to clear one of the properties
				//and make the anchoring the same as for the target
				setCssValue(newPosition, lowerPropertyValue.isSet() ? higherProperty : lowerProperty, CSSValue.NOT_SET);
			}
		}
		else
		{
			//target is anchored left-right or top-bottom
			CSSValue sourceLowerPropertyValue = getOrComputeValue(newPosition, lowerProperty, parentSize);
			sizeValue = computeDimension(lowerProperty, lowerPropertyValue, getCssValue(targetPosition, higherProperty, targetParentSize));
			CSSValue sourceHigherPropertyValue = sourceLowerPropertyValue.plus(sizeValue).toHigherProperty();
			setCssValue(newPosition, lowerProperty, sourceLowerPropertyValue);
			setCssValue(newPosition, higherProperty, sourceHigherPropertyValue);
			setCssValue(newPosition, sizeProperty, CSSValue.NOT_SET);
		}
	}


	private static void snapToDist(JSONObject obj, CSSPosition newPosition, CSSPosition oldPosition, java.awt.Dimension parentSize, Form form,
		String lowerProperty, String higherProperty, String sizeProperty)
	{
		JSONArray uuids = obj.getJSONArray("targets");
		JSONArray containers = obj.getJSONArray("containers");
		ISupportCSSPosition target1 = getTarget(form, uuids.getString(0));
		AbstractContainer parent1 = target1 != null ? CSSPositionUtils.getParentContainer(target1) : null;
		ISupportCSSPosition target2 = getTarget(form, uuids.getString(1));
		AbstractContainer parent2 = target1 != null ? CSSPositionUtils.getParentContainer(target2) : null;
		ISupportCSSPosition targetContainer1 = getTarget(form, containers.optString(0));
		CSSPosition targetContainerCssPos1 = targetContainer1 != null ? targetContainer1.getCssPosition() : null;
		ISupportCSSPosition targetContainer2 = getTarget(form, containers.optString(1));
		CSSPosition targetContainerCssPos2 = targetContainer2 != null ? targetContainer2.getCssPosition() : null;
		CSSPosition pos1 = target1.getCssPosition();
		CSSPosition pos2 = target2.getCssPosition();
		java.awt.Dimension parent1Size = parent1.getSize();
		java.awt.Dimension parent2Size = parent2.getSize();

		int pos = obj.optInt("pos", -1);
		snapToDist(newPosition, oldPosition, parentSize, lowerProperty, higherProperty, sizeProperty, pos1, pos2, parent1Size, parent2Size, pos,
			targetContainerCssPos1, targetContainerCssPos2);

	}

	public static void snapToDist(CSSPosition newPosition, CSSPosition oldPosition, java.awt.Dimension parentSize, String lowerProperty, String higherProperty,
		String sizeProperty, CSSPosition pos1, CSSPosition pos2, java.awt.Dimension parent1Size, java.awt.Dimension parent2Size,
		int pos, CSSPosition targetContainerCssPos1, CSSPosition targetContainerCssPos2)
	{
		CSSValue sizeValue = getOrComputeValue(newPosition, sizeProperty, parentSize);
		switch (pos)
		{
			case -1 :
			{
				//above the targets
				CSSValue dist = computeDist(lowerProperty, higherProperty, pos1, pos2, parent1Size, parent2Size, parentSize, targetContainerCssPos1,
					targetContainerCssPos2);
				CSSValue l1 = getOrComputeValue(pos1, lowerProperty, parent1Size);
				CSSValue higherPropertyValue = l1.minus(dist).toHigherProperty();

				//set the same anchoring as the closest target (first)
				if (getCssValue(pos1, lowerProperty, parent1Size).isSet())
				{
					CSSValue lowerPropertyValue = higherPropertyValue.minus(sizeValue);
					setCssValue(newPosition, lowerProperty, lowerPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					setCssValue(newPosition, higherProperty, higherPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
					}
				}
				break;
			}
			case 1 :
			{
				//below the targets
				CSSValue dist = computeDist(lowerProperty, higherProperty, pos1, pos2, parent1Size, parent2Size, parentSize, targetContainerCssPos1,
					targetContainerCssPos2);
				CSSValue h2 = getOrComputeValue(pos2, higherProperty, parent2Size);

				//set the same anchoring as the closest target (second)
				if (getCssValue(pos2, lowerProperty, parent2Size).isSet())
				{
					CSSValue lowerPropertyValue = h2.plus(dist);
					setCssValue(newPosition, lowerProperty, lowerPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					CSSValue higherPropertyValue = h2.plus(dist).plus(sizeValue).toHigherProperty();
					setCssValue(newPosition, higherProperty, higherPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
					}
				}
				break;
			}
			case 0 :
			{
				//between the targets
				CSSValue h1 = getOrComputeValue(pos1, higherProperty, parent1Size);
				CSSValue l2 = getOrComputeValue(pos2, lowerProperty, parent2Size);
				CSSValue size = getCssValue(newPosition, sizeProperty, parentSize);
				if (!size.isSet())
				{
					size = computeDimension(sizeProperty, getCssValue(oldPosition, lowerProperty, parentSize),
						getCssValue(oldPosition, higherProperty, parentSize));
				}
				CSSValue dist = l2.minus(h1).div(2).minus(size.div(2));

				if (getCssValue(pos1, lowerProperty, parent1Size).isSet() && getCssValue(pos2, lowerProperty, parent2Size).isSet())
				{
					CSSValue lowerPropertyValue = l2.minus(dist).minus(sizeValue);
					setCssValue(newPosition, lowerProperty, lowerPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
					}
				}
				else if (getCssValue(pos1, higherProperty, parent1Size).isSet() && getCssValue(pos2, higherProperty, parent2Size).isSet())
				{
					CSSValue higherPropertyValue = h1.plus(dist).minus(sizeValue);
					setCssValue(newPosition, higherProperty, higherPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					//what should happen if the targets don't have the same anchoring?
					setCssValue(newPosition, lowerProperty, h1.plus(dist));
					setCssValue(newPosition, higherProperty, l2.minus(dist).toHigherProperty());
					setCssValue(newPosition, sizeProperty, CSSValue.NOT_SET);
				}
			}
		}
	}


	private static CSSValue computeDist(String lowerProperty, String higherProperty, CSSPosition pos1, CSSPosition pos2, java.awt.Dimension parent1Size,
		java.awt.Dimension parent2Size, Dimension parentSize, CSSPosition targetContainerCssPos1, CSSPosition targetContainerCssPos2)
	{
		CSSValue h1 = getOrComputeValue(pos1, higherProperty, parent1Size, parentSize, targetContainerCssPos1);
		CSSValue l2 = getOrComputeValue(pos2, lowerProperty, parent2Size, parentSize, targetContainerCssPos2);
		CSSValue dist = l2.minus(h1);
		return dist;
	}


	private static void handleEndSize(CSSPosition newPosition, JSONObject obj, AbstractContainer componentParent, Form form, String key, String start,
		String end, String dimension, CSSValue startX)
	{
		if (obj.has(key))
		{
			JSONObject jsonObject = obj.getJSONObject(key);
			ISupportCSSPosition target = getTarget(form, jsonObject.optString("uuid"));
			AbstractContainer parent = CSSPositionUtils.getParentContainer(target);
			ISupportCSSPosition sibling = jsonObject.has(start)
				? getTarget(form, jsonObject.getString(start))
				: null;
			AbstractContainer siblingParent = CSSPositionUtils.getParentContainer(sibling);
			snapToEndSize(newPosition, componentParent.getSize(), target.getCssPosition(), parent.getSize(), sibling != null ? sibling.getCssPosition() : null,
				siblingParent != null ? siblingParent.getSize() : null, start, end, dimension, startX);
		}
	}

	public static void snapToEndSize(CSSPosition newPosition, java.awt.Dimension parentSize, CSSPosition targetCssPosition,
		java.awt.Dimension targetParentSize, CSSPosition siblingCssPosition,
		java.awt.Dimension siblingParentSize, String lowerProperty, String higherProperty, String sizeProperty, CSSValue start)
	{
		CSSValue higherPropertyValue = getCssValue(targetCssPosition, higherProperty, targetParentSize);
		CSSValue lowerPropertyValue = getCssValue(targetCssPosition, lowerProperty, targetParentSize);
		CSSValue sourceLowerPropertyValue = getCssValue(newPosition, lowerProperty, parentSize);
		if (siblingCssPosition != null && sourceLowerPropertyValue.isSet())
		{
			CSSValue siblingHigherPropertyValue = getOrComputeValue(siblingCssPosition, higherProperty, siblingParentSize);
			if (siblingHigherPropertyValue.getPercentage() > 0)
			{
				//adjust the sourceLowerPropertyValue if the sibling is using %
				CSSValue space = start.minus(siblingHigherPropertyValue);
				sourceLowerPropertyValue = siblingHigherPropertyValue.plus(space);
				setCssValue(newPosition, lowerProperty, sourceLowerPropertyValue);
			}
		}
		CSSValue sourceSizePropertyValue = getCssValue(newPosition, sizeProperty, parentSize);
		if (higherPropertyValue.isSet())
		{
			setCssValue(newPosition, higherProperty, higherPropertyValue);
			if (sourceLowerPropertyValue.isSet())
			{
				CSSValue size = higherPropertyValue.minus(sourceLowerPropertyValue);
				setCssValue(newPosition, sizeProperty, size);
			}
			else
			{
				CSSValue size = getCssValue(targetCssPosition, sizeProperty, targetParentSize);
				if (size.isSet())
				{
					setCssValue(newPosition, sizeProperty, size);
				}
			}
			setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
		}
		else
		{
			CSSValue computedHigherPropertyValue = lowerPropertyValue.plus(getCssValue(targetCssPosition, sizeProperty, targetParentSize)).toHigherProperty();
			if (!sourceSizePropertyValue.isSet())
			{
				setCssValue(newPosition, higherProperty, higherPropertyValue);
			}
			else
			{
				if (start != null && start.isSet())
				{
					if (lowerPropertyValue.getPercentage() > 0)
					{
						CSSValue siblingHigherPropertyValue = getCssValue(siblingCssPosition, higherProperty, siblingParentSize);
						if (!siblingHigherPropertyValue.isSet())
						{
							siblingHigherPropertyValue = getOrComputeValue(siblingCssPosition, lowerProperty, siblingParentSize)
								.plus(getOrComputeValue(siblingCssPosition, sizeProperty, siblingParentSize));
						}
						CSSValue space = start.minus(siblingHigherPropertyValue);
						sourceLowerPropertyValue = siblingHigherPropertyValue.plus(space);
						CSSValue size = computedHigherPropertyValue.minus(sourceLowerPropertyValue);

						setCssValue(newPosition, lowerProperty, sourceLowerPropertyValue);
						setCssValue(newPosition, sizeProperty, size);
					}
					else
					{
						CSSValue size = computedHigherPropertyValue.minus(start != null && start.isSet() ? start : sourceLowerPropertyValue);
						setCssValue(newPosition, lowerProperty, start);
						setCssValue(newPosition, sizeProperty, size);
					}
				}
				else
				{
					CSSValue size = computedHigherPropertyValue.minus(sourceLowerPropertyValue);
					setCssValue(newPosition, sizeProperty, size);
				}
				setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
			}
		}
		if (siblingCssPosition != null && sourceSizePropertyValue.isSet() && !sourceLowerPropertyValue.isSet())
		{
			CSSValue siblingHigherPropertyValue = getOrComputeValue(siblingCssPosition, higherProperty, siblingParentSize);
			if (siblingHigherPropertyValue.getPercentage() > 0)
			{
				//adjust the sourceSizePropertyValue if the sibling is using %
				CSSValue space = start.minus(siblingHigherPropertyValue);
				sourceLowerPropertyValue = siblingHigherPropertyValue.plus(space);
				CSSValue size = getCssValue(newPosition, higherProperty, parentSize).minus(sourceLowerPropertyValue);
				setCssValue(newPosition, sizeProperty, size);
			}
		}
	}


	public static void snapToMiddle(CSSPosition newPosition, CSSPosition oldPosition, java.awt.Dimension parentSize, CSSPosition midCssPosition,
		java.awt.Dimension targetParentSize, CSSPosition targetContainerCssPos, String lowerProperty, String higherProperty, String sizeProperty)
	{
		CSSValue mid = CSSValue.NOT_SET;
		CSSValue lowerPropertyValue = getCssValue(midCssPosition, lowerProperty, targetParentSize, targetContainerCssPos, parentSize);
		CSSValue higherPropertyValue = getCssValue(midCssPosition, higherProperty, targetParentSize, targetContainerCssPos, parentSize);
		CSSValue targetSizeValue = getCssValue(midCssPosition, sizeProperty, targetParentSize, targetContainerCssPos, parentSize);
		CSSValue sizeValue = getCssValue(oldPosition, sizeProperty, parentSize);
		if (sizeValue.isSet())
		{
			//maintain the size of the old css position
			setCssValue(newPosition, sizeProperty, sizeValue);
		}
		else if (oldPosition != null)
		{
			//the size is not set (component anchored left-right or top-bottom),
			//need to compute it based on the opposing properties and the parent container size
			sizeValue = getCssValue(oldPosition, higherProperty, parentSize).minus(getCssValue(oldPosition, lowerProperty, parentSize));
			setCssValue(newPosition, sizeProperty, sizeValue);
		}
		else
		{
			sizeValue = getCssValue(newPosition, sizeProperty, parentSize);
		}
		Assert.isTrue(sizeValue.isSet());

		boolean bothLowerHigherSet = lowerPropertyValue.isSet() && higherPropertyValue.isSet();
		if (targetSizeValue.isSet() && !bothLowerHigherSet)
		{
			if (lowerPropertyValue.isSet())
			{
				mid = lowerPropertyValue.plus(targetSizeValue.div(2));
				setCssValue(newPosition, lowerProperty, mid.minus(sizeValue.div(2)));
				setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
			}
			else if (higherPropertyValue.isSet())
			{
				mid = higherPropertyValue.minus(targetSizeValue.div(2));
				setCssValue(newPosition, higherProperty, mid.plus(sizeValue.div(2)).toHigherProperty());
				setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
			}
		}
		else
		{
			mid = lowerPropertyValue.plus(higherPropertyValue).div(2);
			setCssValue(newPosition, lowerProperty, mid.minus(sizeValue.div(2)));
			setCssValue(newPosition, higherProperty, mid.plus(sizeValue.div(2)).toHigherProperty());
			setCssValue(newPosition, sizeProperty, CSSValue.NOT_SET);
		}
	}

	private static CSSValue getOrComputeValue(CSSPosition pos, String property, java.awt.Dimension parentSize)
	{
		CSSValue value = getCssValue(pos, property, parentSize, null, null);
		if (!value.isSet())
		{
			value = computeValueBasedOnOppositeProperty(pos, property, parentSize, getCssValue(pos, getOppositeProperty(property), parentSize));
		}
		return value;
	}

	private static CSSValue getOrComputeValue(CSSPosition pos, String property, java.awt.Dimension parentSize, Dimension parentSize_,
		CSSPosition targetContainerCssPos)
	{
		CSSValue value = getCssValue(pos, property, parentSize, targetContainerCssPos, parentSize_);
		if (!value.isSet())
		{
			value = computeValueBasedOnOppositeProperty(pos, property, parentSize, getCssValue(pos, getOppositeProperty(property), parentSize));
		}
		return value;
	}

	private static CSSValue getCssValue(CSSPosition position, String property, java.awt.Dimension containerSize)
	{
		if (position == null) return CSSValue.NOT_SET;
		switch (property)
		{
			case "left" :
				return new CSSValue(position.left, containerSize.width, false);
			case "right" :
				return new CSSValue(position.right, containerSize.width, true);
			case "top" :
				return new CSSValue(position.top, containerSize.height, false);
			case "bottom" :
				return new CSSValue(position.bottom, containerSize.height, true);
			case "width" :
				return new CSSValue(position.width, containerSize.width, false);
			case "height" :
				return new CSSValue(position.height, containerSize.height, false);
			default :
				return CSSValue.NOT_SET;
		}
	}

	private static void setCssValue(CSSPosition position, String property, CSSValue val)
	{
		if (position == null) return;
		String value = val.toString();
		switch (property)
		{
			case "left" :
				position.left = value;
				return;
			case "right" :
				position.right = value;
				return;
			case "top" :
				position.top = value;
				return;
			case "bottom" :
				position.bottom = value;
				return;
			case "width" :
				position.width = value;
				return;
			case "height" :
				position.height = value;
				return;
		}
	}

	private static String getOppositeProperty(String property)
	{
		switch (property)
		{
			case "left" :
				return "right";
			case "right" :
				return "left";
			case "top" :
				return "bottom";
			case "bottom" :
				return "top";
			case "width" :
				return "height";
			case "height" :
				return "width";
			default :
				return null;
		}
	}

	private static String getSizeProperty(String property)
	{
		if ("left".equals(property) || "right".equals(property))
		{
			return "width";
		}
		else if ("top".equals(property) || "bottom".equals(property))
		{
			return "height";
		}
		return null;
	}

	/**
	 * Copy the anchoring from the target component.
	 *
	 * @param jsonObject contains the property name to copy from the target; for instance align the right edge with the right of left property of the target
	 * @param property the property to be set (one of top, bottom, left, right)
	 * @param newPosition the css position object on which the values should be set
	 * @param oldPosition the old css position object of the source component; null for new components
	 * @param targetPosition the css position object of the snap target
	 * @param containerSize the size of the source component parent
	 * @param targetContainerSize the size of the target component parent
	 * @param targetContainerCssPos on the form
	 */
	public static void setCssValue(JSONObject jsonObject, String property, CSSPosition newPosition, CSSPosition oldPosition, CSSPosition targetPosition,
		java.awt.Dimension containerSize, java.awt.Dimension targetContainerSize, CSSPosition targetContainerCssPos)
	{
		String sizeProperty = getSizeProperty(property);
		String oppositeProperty = getOppositeProperty(property);
		String targetProperty = jsonObject.optString("prop", property);
		String targetOppositeProperty = getOppositeProperty(oppositeProperty);
		CSSValue val = getCssValue(targetPosition, targetProperty, targetContainerSize, targetContainerCssPos, containerSize);
		if (val.isSet())
		{
			//if the property is set on the target, then we copy on the source component and clear the opposite property if the size property is set
			//when moving or resizing a component, if the property is set on the target, then we copy its value on the source component
			if (property.equals(targetProperty))
			{
				setCssValue(newPosition, property, val);
			}
			else
			{
				CSSValue computed = computeValueBasedOnOppositeTargetProperty(property, containerSize, val);
				if (getCssValue(targetPosition, property, targetContainerSize, targetContainerCssPos, containerSize).isSet())
				{
					//if the target has the same anchor just set the computed value
					setCssValue(newPosition, property, computed);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(newPosition, sizeProperty, containerSize).isSet())
					{
						setCssValue(newPosition, oppositeProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					//the target has the opposite property set, use the computed value and the size to obtain the opposite property value
					CSSValue oppositeComputed = computeValueBasedOnOppositeProperty(newPosition, oppositeProperty, containerSize, computed);
					setCssValue(newPosition, oppositeProperty, oppositeComputed);
					//clear the property value if the size property is set
					if (oldPosition == null || getCssValue(newPosition, sizeProperty, containerSize).isSet())
					{
						setCssValue(newPosition, property, CSSValue.NOT_SET);
					}
				}
			}
		}
		else if (getCssValue(targetPosition, oppositeProperty, targetContainerSize, targetContainerCssPos, containerSize).isSet() &&
			getCssValue(targetPosition, sizeProperty, targetContainerSize, targetContainerCssPos, containerSize).isSet())
		{
			//the property is not set on the target, need to compute it using the size and the value of the opposite property
			CSSValue oppositePropertyValue = getCssValue(targetPosition, oppositeProperty, targetContainerSize, targetContainerCssPos, containerSize);
			CSSValue computedPropertyValue = computeValueBasedOnOppositeProperty(targetPosition, property, targetContainerSize, oppositePropertyValue);

			//clear the property because the target component does also not have it and we want the same anchoring
			setCssValue(newPosition, property, CSSValue.NOT_SET);
			CSSValue dimension = getCssValue(newPosition, sizeProperty, containerSize);
//			if (dimension.isSet())
//			{
//				//maintain the size of the old css position
//				setCssValue(newPosition, sizeProperty, dimension);
//			}
//			else
			if (!dimension.isSet() && oldPosition != null)
			{
				//the size is not set (component anchored left-right or top-bottom),
				//need to compute it based on the opposing properties and the parent container size
				CSSValue computedDimension = computeDimension(property, getCssValue(oldPosition, property, containerSize),
					getCssValue(oldPosition, oppositeProperty, containerSize));
				setCssValue(newPosition, sizeProperty, computedDimension);
			}
			//compute the opposite property value for the source component using the size property, container size and the computed property value of the target
			CSSValue computedOppositePropertySourceComponent = computeValueBasedOnOppositeProperty(newPosition, oppositeProperty, containerSize,
				computedPropertyValue);
			setCssValue(newPosition, oppositeProperty, computedOppositePropertySourceComponent);
		}
		else if (getCssValue(targetPosition, targetOppositeProperty, targetContainerSize, targetContainerCssPos, containerSize).isSet() &&
			getCssValue(targetPosition, sizeProperty, targetContainerSize, targetContainerCssPos, containerSize).isSet())
		{
			CSSValue oppositePropertyValue = getCssValue(targetPosition, targetOppositeProperty, targetContainerSize);
			CSSValue computed = computeValueBasedOnOppositeProperty(targetPosition, targetProperty, targetContainerSize,
				oppositePropertyValue);

			if (property.equals(targetProperty))
			{
				//use the computed property value of the target
				setCssValue(newPosition, property, computed);
			}
			else
			{
				CSSValue computedPropertyValue = computeValueBasedOnOppositeTargetProperty(property, containerSize, computed);
				setCssValue(newPosition, property, computedPropertyValue);
			}
		}
	}

	public static CSSValue getCssValue(CSSPosition targetPosition, String targetProperty, Dimension targetContainerSize, CSSPosition targetContainerCssPos,
		Dimension containerSize)
	{
		CSSValue val = getCssValue(targetPosition, targetProperty, targetContainerSize);
		if (val.isSet() && targetContainerCssPos != null)
		{
			switch (targetProperty)
			{
				case "left" :
				{
					CSSValue left = getCssValue(targetContainerCssPos, targetProperty, containerSize);
					return val.plus(left);
				}
				case "right" :
				{
					// TODO impl
					break;
				}
				case "top" :
				{
					CSSValue top = getCssValue(targetContainerCssPos, targetProperty, containerSize);
					return val.plus(top);
				}
				case "bottom" :
				{
					// TODO impl
				}
				//TODO width, height?
			}
		}
		return val;
	}

	private static CSSValue computeValueBasedOnOppositeProperty(CSSPosition position, String property, java.awt.Dimension containerSize,
		CSSValue oppositePropertyValue)
	{
		switch (property)
		{
			case "left" :
			{
				CSSValue width = getCssValue(position, "width", containerSize);
				return oppositePropertyValue.minus(width);
			}
			case "right" :
			{
				CSSValue width = getCssValue(position, "width", containerSize);
				return oppositePropertyValue.plus(width).toHigherProperty();
			}
			case "top" :
			{
				CSSValue height = getCssValue(position, "height", containerSize);
				return oppositePropertyValue.minus(height);
			}
			case "bottom" :
			{
				CSSValue height = getCssValue(position, "height", containerSize);
				return oppositePropertyValue.plus(height).toHigherProperty();
			}
		}
		return CSSValue.NOT_SET;
	}

	private static CSSValue computeValueBasedOnOppositeTargetProperty(String property, java.awt.Dimension containerSize,
		CSSValue oppositePropertyValue)
	{
		CSSValue result = CSSValue.NOT_SET;
		boolean isPercentage = oppositePropertyValue.isPercentage();
		switch (property)
		{
			case "left" :
				result = new CSSValue(isPercentage ? 100 - oppositePropertyValue.getPercentage() : 0,
					isPercentage ? 0 : oppositePropertyValue.getAsPixels());
				result.setParentContainerSize(containerSize.width);
				break;
			case "right" :
				String val = oppositePropertyValue.isPercentage() ? 100 - oppositePropertyValue.getPercentage() + "%"
					: containerSize.width - oppositePropertyValue.getAsPixels() + "px";
				result = new CSSValue(val, containerSize.width, true);
				break;
			case "top" :
				result = new CSSValue(oppositePropertyValue.isPercentage() ? 100 - oppositePropertyValue.getPercentage() : 0,
					isPercentage ? 0 : oppositePropertyValue.getAsPixels());
				result.setParentContainerSize(containerSize.height);
				break;
			case "bottom" :
				String bottom = oppositePropertyValue.isPercentage() ? 100 - oppositePropertyValue.getPercentage() + "%"
					: containerSize.height - oppositePropertyValue.getAsPixels() + "px";
				result = new CSSValue(bottom, containerSize.height, true);
				break;
		}
		return result;
	}


	private static CSSValue computeDimension(String property, CSSValue value, CSSValue oppositePropertyValue)
	{
		switch (property)
		{
			case "left" :
			case "top" :
				return oppositePropertyValue.minus(value);
			case "right" :
			case "bottom" :
				return value.minus(oppositePropertyValue);
		}
		return CSSValue.NOT_SET;
	}

	private static ISupportCSSPosition getTarget(Form form, String persistIdentifierAsString)
	{
		IPersist persist = PersistFinder.INSTANCE.searchForPersist(form, PersistIdentifier.fromJSONString(persistIdentifierAsString));
		if (persist instanceof ISupportCSSPosition)
		{
			return (ISupportCSSPosition)persist;
		}
		else if (persist instanceof WebFormComponentChildType)
		{
			return ((WebFormComponentChildType)persist).getElement();
		}
		return null;
	}
}
