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
	if ($.servoy.websocket) return $.servoy.sendWebsocketMessage('getFormDesign:', true)
	// full form
	// take it from MobileVisualFormEditorHtmlDesignPage.getFormDesign
	return '{"children":[{"children":[{"children":[{"properties":{"id":"4058B46E-4997-43C2-9302-7A7085046006","right":true,"servoydataprovider":" ","text":"save","theme":"b"},"type":"Button","zone":"right"},{"properties":{"id":"7DCF0011-76D1-4480-B9B9-9F01592C74EF","right":false,"servoydataprovider":" ","text":"back","theme":"b"},"type":"Button","zone":"left"}],"properties":{"id":"header_7C97454A-656A-44B5-A42B-46247F36DBBA","position":"default","servoydataprovider":" ","text":"Title","theme":"b"},"type":"Header","zone":"top"},{"children":[{"properties":{"id":"C59065E5-0951-445D-AA98-073803618ED0","label":"Name","servoydataprovider":"name","servoytitledataprovider":" "},"type":"TextInput"},{"properties":{"id":"D85C052C-27AB-4C32-A1A9-A31ED5CFBD78","label":"Title","servoydataprovider":"mobile1_to_mobile2.sometext","servoytitledataprovider":" "},"type":"TextInput"},{"properties":{"id":"5B9D0F3E-EEC8-43D9-8F38-97EDA774CF83","label":"Title","servoydataprovider":"mydate","servoytitledataprovider":" "},"type":"Calendar"},{"children":[{"properties":{"checked":"checked","id":"child1_EB8666BD-5C60-449A-8DE3-3440DE79860F","label":"one","servoydataprovider":"mynumber"},"type":"Checkbox"},{"properties":{"id":"child2_EB8666BD-5C60-449A-8DE3-3440DE79860F","label":"two"},"type":"Checkbox"},{"properties":{"id":"child3_EB8666BD-5C60-449A-8DE3-3440DE79860F","label":"three"},"type":"Checkbox"}],"properties":{"id":"EB8666BD-5C60-449A-8DE3-3440DE79860F","label":"Title","servoydataprovider":"mynumber","servoytitledataprovider":" "},"type":"CheckboxGroup"},{"children":[{"properties":{"checked":"checked","id":"child1_EA8DE25D-CF12-4667-B56A-C5E46BD5B1BD","label":"one","servoydataprovider":"mynumber"},"type":"RadioButton"},{"properties":{"id":"child2_EA8DE25D-CF12-4667-B56A-C5E46BD5B1BD","label":"two"},"type":"RadioButton"},{"properties":{"id":"child3_EA8DE25D-CF12-4667-B56A-C5E46BD5B1BD","label":"three"},"type":"RadioButton"}],"properties":{"id":"EA8DE25D-CF12-4667-B56A-C5E46BD5B1BD","label":"Title","orientation":"vertical","servoydataprovider":"mynumber","servoytitledataprovider":" "},"type":"RadioGroup"},{"properties":{"id":"89103C0D-AF44-4810-B122-5C30E2D3D726","label":"Title","servoydataprovider":"name","servoytitledataprovider":" "},"type":"TextInput"},{"properties":{"id":"8C50832A-EAE6-4890-87AB-2D2C82E9C1F4","label":"Title","servoydataprovider":"name","servoytitledataprovider":" "},"type":"TextInput"}],"properties":{"id":"content_0642505B-0960-4BB4-B324-1631645898F2"},"type":"Content","zone":"content"},{"properties":{"id":"footer_22F64C4C-C935-41D4-87E1-5689348AF16C","position":"fixed","text":" ","theme":"b"},"type":"Footer","zone":"bottom"}],"properties":{"id":"0642505B-0960-4BB4-B324-1631645898F2","theme":"d"},"type":"Page"}],"type":"Design"}'
}
