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
	return '{"children":[{"children":[{"children":[{"properties":{"id":"4F43D357-C1AE-45E2-9039-27D1BA3F1148","right":false,"servoydataprovider":" ","text":"button","theme":"e"},"type":"Button","zone":"left"}],"properties":{"id":"header_F4866B4F-C88F-43D6-A059-321BF6E85DF0","position":"fixed","servoydataprovider":" ","text":"Title","theme":"b"},"type":"Header","zone":"top"},{"children":[{"children":[{"properties":{"checked":"checked","id":"child1_ABAC064C-F265-400B-B6E5-E74A5A63FAFF","labelItem":"item1","servoydataprovider":" ","theme":"a"},"type":"RadioButton"},{"properties":{"id":"child2_ABAC064C-F265-400B-B6E5-E74A5A63FAFF","labelItem":"item2","theme":"a"},"type":"RadioButton"},{"properties":{"id":"child3_ABAC064C-F265-400B-B6E5-E74A5A63FAFF","labelItem":"item3","theme":"a"},"type":"RadioButton"}],"properties":{"id":"ABAC064C-F265-400B-B6E5-E74A5A63FAFF","label":"ttt","orientation":"vertical","servoydataprovider":" ","servoytitledataprovider":" ","theme":"a","visibleelement":"visibleElement"},"type":"RadioGroup"},{"children":[{"properties":{"checked":"checked","id":"child1_58197F8C-9F3A-4FE0-8765-F3C30F5A1AF9","labelItem":"One","servoydataprovider":" "},"type":"RadioButton"},{"properties":{"id":"child2_58197F8C-9F3A-4FE0-8765-F3C30F5A1AF9","labelItem":"Two"},"type":"RadioButton"},{"properties":{"id":"child3_58197F8C-9F3A-4FE0-8765-F3C30F5A1AF9","labelItem":"Three"},"type":"RadioButton"}],"properties":{"id":"58197F8C-9F3A-4FE0-8765-F3C30F5A1AF9","label":"Title123","orientation":"vertical","servoydataprovider":" ","servoytitledataprovider":" ","visibleelement":"visibleElement"},"type":"RadioGroup"},{"children":[{"properties":{"checked":"checked","id":"child1_8D38CA72-3ECD-425F-A324-C6E542DFFC94","labelItem":"aa1","servoydataprovider":" ","theme":"a"},"type":"Checkbox"},{"properties":{"id":"child2_8D38CA72-3ECD-425F-A324-C6E542DFFC94","labelItem":"aa2","theme":"a"},"type":"Checkbox"}],"properties":{"id":"8D38CA72-3ECD-425F-A324-C6E542DFFC94","label":"vvv","servoydataprovider":" ","servoytitledataprovider":" ","theme":"a","visibleelement":"visibleElement"},"type":"CheckboxGroup"},{"properties":{"id":"594F574D-CB8B-4BBD-82E8-E58BCCED47A4","label":"Title","servoydataprovider":" ","servoytitledataprovider":" ","text":"Text","titlevisible":"visibleElement","visibleelement":"visibleElement"},"type":"Label"}],"properties":{"id":"content_4E950767-024E-47EF-B894-5CFEAC970C5C"},"type":"Content","zone":"content"},{"children":[{"properties":{"id":"A3D6CDC6-BCAC-4617-ACE2-AE1629120B7A","servoydataprovider":" ","text":"aaa","theme":"b"},"type":"Button"}],"properties":{"id":"footer_C21F21C6-87E2-42FD-971C-6C5A0416A939","position":"fixed","text":" ","theme":"b"},"type":"Footer","zone":"bottom"}],"properties":{"id":"4E950767-024E-47EF-B894-5CFEAC970C5C","theme":"d"},"type":"Page"}],"type":"Design"}'
}

window.getPaletteItems = window.getPaletteItems || function()
{
	if ($.servoy.websocket) return $.servoy.sendWebsocketMessage('getPaletteItems:', true)
	// take it from MobileVisualFormEditorHtmlDesignPage.getPaletteItems
	return '["Header","Footer","Button","TextInput","PasswordField","TextArea","Calendar","Bean","SelectMenu","RadioGroup","SingleCheckbox","CheckboxGroup","InsetList","Label"]'
}

window.getChildJson = window.getChildJson || function(uuid)
{
	if ($.servoy.websocket) return $.servoy.sendWebsocketMessage('getChildJson:'+uuid, true)
	// take it from MobileVisualFormEditorHtmlDesignPage.getChildJson
	return '{"properties":{"id":"A724091E-CAA8-46C7-BC60-464933E3E79E","label":"combo","options":{"children":[]},"servoydataprovider":" ","servoytitledataprovider":" "},"type":"SelectMenu"}'
}
