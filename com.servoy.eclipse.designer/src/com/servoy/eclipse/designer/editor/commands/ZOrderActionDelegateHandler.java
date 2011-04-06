/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.designer.editor.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;

import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.actions.ZOrderAction.ZOrderType;
import com.servoy.eclipse.designer.util.DesignerUtil;

/**
 * An action to change the z-ordering of selected objects.
 * 
 * @author rgansevles
 */
public abstract class ZOrderActionDelegateHandler extends DesignerSelectionActionDelegateHandler
{
	public ZOrderActionDelegateHandler(Object requestType)
	{
		super(requestType);
	}

	@Override
	protected Iterable<EditPart> getToRefresh(Iterable<EditPart> affected)
	{
		return DesignerUtil.getFormEditparts(affected);
	}

	public static class ToFront extends ZOrderActionDelegateHandler
	{
		public ToFront()
		{
			super(ZOrderType.Z_ORDER_BRING_TO_FRONT);
		}

		@Override
		protected Map<EditPart, Request> createRequests(List<EditPart> selected)
		{
			return ZOrderAction.createZOrderRequests(ZOrderType.Z_ORDER_BRING_TO_FRONT, selected);
		}
	}
	public static class ToBack extends ZOrderActionDelegateHandler
	{
		public ToBack()
		{
			super(ZOrderType.Z_ORDER_SEND_TO_BACK);
		}

		@Override
		protected Map<EditPart, Request> createRequests(List<EditPart> selected)
		{
			return ZOrderAction.createZOrderRequests(ZOrderType.Z_ORDER_SEND_TO_BACK, selected);
		}
	}

	public static class ToFrontOneStep extends ZOrderActionDelegateHandler
	{
		public ToFrontOneStep()
		{
			super(ZOrderType.Z_ORDER_BRING_TO_FRONT_ONE_STEP);
		}

		@Override
		protected Map<EditPart, Request> createRequests(List<EditPart> selected)
		{
			return ZOrderAction.createZOrderRequests(ZOrderType.Z_ORDER_BRING_TO_FRONT_ONE_STEP, selected);
		}
	}
	public static class ToBackOneStep extends ZOrderActionDelegateHandler
	{
		public ToBackOneStep()
		{
			super(ZOrderType.Z_ORDER_SEND_TO_BACK_ONE_STEP);
		}

		@Override
		protected Map<EditPart, Request> createRequests(List<EditPart> selected)
		{
			return ZOrderAction.createZOrderRequests(ZOrderType.Z_ORDER_SEND_TO_BACK_ONE_STEP, selected);
		}
	}
}
