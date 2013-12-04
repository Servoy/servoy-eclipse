/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

"use strict";

/**
 * Replacements for functions that are defined in java, for debugging in browser
 */

window.callbackSelectionChanged = window.callbackSelectionChanged || function(props)
{
	// send message to editor via websocket
	$.servoy.sendWebsocketMessage('callbackSelectionChanged:' +props)
}

window.callbackModelUpdated = window.callbackModelUpdated || function(props)
{
}

window.callbackEditingStateChanged = window.callbackEditingStateChanged || function(b)
{
	// send message to editor via websocket
	$.servoy.sendWebsocketMessage('callbackEditingStateChanged:' +b)
}

window.getFormDesign = window.getFormDesign || function()
{
	return $.servoy.sendWebsocketMessage('getFormDesign:', true)
	// full form
	//return '{"children":[{"children":[{"properties":{"id":"header_C8D07AEE-075E-4958-9242-7C21C791FB27","position":"fixed","servoydataprovider":" ","text":"Title","theme":"d"},"type":"Header","zone":"top"},{"children":[{"properties":{"id":"04445FF0-D115-49DC-ACFA-81452B9ACC3C","name":"bean_148"},"type":"Bean"},{"properties":{"id":"FDE372AF-F1FF-48B5-86E3-763FA779786F","label":"Title","labelsize":1,"servoydataprovider":" ","servoytitledataprovider":" ","text":"Text"},"type":"Label"},{"properties":{"id":"C05E0737-AE62-49B8-8928-13D3D47AD10F","name":"bean_69"},"type":"Bean"},{"properties":{"id":"18FC6A63-3DE6-4FC2-84AD-7C7C27E9276D","label":"Title","servoydataprovider":" ","servoytitledataprovider":" "},"type":"Calendar"}],"properties":{"id":"content_BBE40C3D-044C-4D52-90E6-F14849DD83DA"},"type":"Content","zone":"content"},{"properties":{"id":"footer_4334FF38-6271-4180-866B-B10B00C54793","position":"fixed","text":" ","theme":"b"},"type":"Footer","zone":"bottom"}],"properties":{"id":"BBE40C3D-044C-4D52-90E6-F14849DD83DA","theme":"d"},"type":"Page"}],"type":"Design"}'
}
