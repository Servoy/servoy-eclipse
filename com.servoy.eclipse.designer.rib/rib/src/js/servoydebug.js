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
	return '{"children":[{"children":[{"children":[{"properties":{"id":"FA2BB2FB-BF4F-4301-87D5-3C6D4EC3E571","servoydataprovider":"xlabel_text","text":" ","theme":"b"},"type":"Button"},{"properties":{"id":"D3C97751-C49E-425A-A946-4940C726E656","servoydataprovider":" ","text":"b","theme":"b"},"type":"Button"},{"properties":{"id":"E26FDAEB-F3A3-44E2-B697-30ED31AEDCD7","servoydataprovider":" ","text":"c","theme":"b"},"type":"Button"},{"properties":{"id":"2EF0EDD7-6546-4752-A786-C7E8DC80A6EB","servoydataprovider":" ","text":"d","theme":"b"},"type":"Button"},{"properties":{"id":"35F0A0F5-61DC-4F95-B116-AA42F6ED2128","servoydataprovider":" ","text":"e","theme":"b"},"type":"Button"},{"properties":{"id":"D4929900-8961-4ADD-89C0-5273F67FE0E7","servoydataprovider":" ","text":"f","theme":"b"},"type":"Button"},{"properties":{"id":"66C688C7-331F-42A1-83F0-D4ADC08BCD15","servoydataprovider":" ","text":"g","theme":"b"},"type":"Button"},{"properties":{"id":"0140EEAC-03A5-4817-9246-D35D44E029A0","servoydataprovider":" ","text":"h","theme":"b"},"type":"Button"},{"properties":{"id":"E4D2EA33-4419-40F2-80F9-D0A7024F6AA9","servoydataprovider":" ","text":"i","theme":"b"},"type":"Button"},{"properties":{"id":"E6E3AB97-5735-422A-AF83-D28821452CA5","servoydataprovider":" ","text":"j","theme":"b"},"type":"Button"},{"properties":{"id":"863E3176-CAA9-40E0-8BF8-1065D04BCD0C","servoydataprovider":" ","text":"k","theme":"b"},"type":"Button"},{"properties":{"id":"89D96D70-3285-44F8-AE93-75A930B81BB1","servoydataprovider":" ","text":"l","theme":"b"},"type":"Button"},{"properties":{"id":"68491B08-F6BD-426A-907E-5AE2DA588C5D","servoydataprovider":" ","text":"m","theme":"b"},"type":"Button"},{"properties":{"id":"1E17DEAB-9C40-47D4-B50F-AEEC04C8E49F","servoydataprovider":" ","text":"n","theme":"b"},"type":"Button"},{"properties":{"id":"BFB18E38-F52C-4AC7-B4B1-80ECF89A287E","servoydataprovider":" ","text":"o","theme":"b"},"type":"Button"},{"properties":{"id":"665A4438-F8CE-46F0-A8F7-A094AC7CD8E2","servoydataprovider":" ","text":"p","theme":"b"},"type":"Button"},{"properties":{"id":"63885D9D-3695-4229-909D-0574DAA3E2BB","servoydataprovider":" ","text":"q","theme":"b"},"type":"Button"},{"properties":{"id":"81CCBF17-E641-41E2-B972-ED2D667D9772","servoydataprovider":" ","text":"r","theme":"b"},"type":"Button"},{"properties":{"id":"3FCB9CFE-CB3E-4A0A-A696-A2DA7C34B3F4","servoydataprovider":" ","text":"s","theme":"b"},"type":"Button"},{"properties":{"id":"79A3A14C-4275-401D-9F34-891EFDCD8E2A","servoydataprovider":" ","text":"t","theme":"b"},"type":"Button"},{"properties":{"id":"068CFA64-1017-432D-9695-47F7945FDACF","servoydataprovider":" ","text":"u","theme":"b"},"type":"Button"},{"properties":{"id":"5EAC0D04-5863-4A4D-A443-ACE9622285E2","servoydataprovider":" ","text":"v","theme":"b"},"type":"Button"},{"children":[{"properties":{"checked":"checked","id":"child1_3F03FEAD-DD2B-4EE0-9044-5FE39B511520","label":"One","servoydataprovider":" "},"type":"RadioButton"},{"properties":{"id":"child2_3F03FEAD-DD2B-4EE0-9044-5FE39B511520","label":"Two"},"type":"RadioButton"},{"properties":{"id":"child3_3F03FEAD-DD2B-4EE0-9044-5FE39B511520","label":"Three"},"type":"RadioButton"}],"properties":{"id":"3F03FEAD-DD2B-4EE0-9044-5FE39B511520","label":"Title","orientation":"vertical","servoydataprovider":" ","servoytitledataprovider":" "},"type":"RadioGroup"},{"properties":{"id":"5113F9C9-4207-44F0-A2CA-8F1B10404E5E","servoydataprovider":" ","text":"w","theme":"b"},"type":"Button"},{"children":[{"properties":{"checked":"checked","id":"child1_E06B4A8C-0EAE-4E7A-88D9-BC29D3C582D6","label":"One","servoydataprovider":" "},"type":"RadioButton"},{"properties":{"id":"child2_E06B4A8C-0EAE-4E7A-88D9-BC29D3C582D6","label":"Two"},"type":"RadioButton"},{"properties":{"id":"child3_E06B4A8C-0EAE-4E7A-88D9-BC29D3C582D6","label":"Three"},"type":"RadioButton"}],"properties":{"id":"E06B4A8C-0EAE-4E7A-88D9-BC29D3C582D6","label":"Title","orientation":"vertical","servoydataprovider":" ","servoytitledataprovider":" "},"type":"RadioGroup"},{"children":[{"properties":{"checked":"checked","id":"child1_1C6BC61B-58C7-4EC9-AAAD-5EE5E6F5E7B6","label":"One","servoydataprovider":" "},"type":"RadioButton"},{"properties":{"id":"child2_1C6BC61B-58C7-4EC9-AAAD-5EE5E6F5E7B6","label":"Two"},"type":"RadioButton"},{"properties":{"id":"child3_1C6BC61B-58C7-4EC9-AAAD-5EE5E6F5E7B6","label":"Three"},"type":"RadioButton"}],"properties":{"id":"1C6BC61B-58C7-4EC9-AAAD-5EE5E6F5E7B6","label":"Title","orientation":"vertical","servoydataprovider":" ","servoytitledataprovider":" "},"type":"RadioGroup"},{"properties":{"id":"45864C00-2105-4755-B387-683FD7134AE6","servoydataprovider":" ","text":"bbbbb","theme":"b"},"type":"Button"},{"properties":{"id":"75771726-DB78-4623-B70F-53FB731BCC34","label":"cc","servoydataprovider":" ","servoytitledataprovider":" "},"type":"TextInput"},{"properties":{"id":"A724091E-CAA8-46C7-BC60-464933E3E79E","label":"combo","servoydataprovider":" ","servoytitledataprovider":" "},"type":"TextInput"},{"children":[{"properties":{"checked":"checked","id":"child1_9FF14BA8-4F30-4682-82E5-C97BF027EBC5","label":"One","servoydataprovider":" "},"type":"RadioButton"},{"properties":{"id":"child2_9FF14BA8-4F30-4682-82E5-C97BF027EBC5","label":"Two"},"type":"RadioButton"},{"properties":{"id":"child3_9FF14BA8-4F30-4682-82E5-C97BF027EBC5","label":"Three"},"type":"RadioButton"}],"properties":{"id":"9FF14BA8-4F30-4682-82E5-C97BF027EBC5","label":"fsdfsd","orientation":"vertical","servoydataprovider":" ","servoytitledataprovider":" "},"type":"RadioGroup"}],"properties":{"id":"content_6C7E1BC9-F3BA-4F8E-B5C5-5A1D6B6A42CC"},"type":"Content","zone":"content"},{"children":[{"properties":{"id":"BA234C11-110E-4152-AD7E-317A7F706FB3","servoydataprovider":" ","text":"button","theme":"b"},"type":"Button"},{"properties":{"id":"DC8D4AB0-569C-4274-8774-47A2E512DC9E","servoydataprovider":" ","text":"1","theme":"b"},"type":"Button"}],"properties":{"id":"footer_B4B38219-F6CE-44DD-A410-1B4CC1C9BB6B","position":"fixed","text":" ","theme":"b"},"type":"Footer","zone":"bottom"}],"properties":{"id":"6C7E1BC9-F3BA-4F8E-B5C5-5A1D6B6A42CC","theme":"d"},"type":"Page"}],"type":"Design"}'
}

window.getChildJson = window.getChildJson || function(uuid)
{
	if ($.servoy.websocket) return $.servoy.sendWebsocketMessage('getChildJson:'+uuid, true)
	// take it from MobileVisualFormEditorHtmlDesignPage.getChildJson
	return '{"properties":{"id":"A724091E-CAA8-46C7-BC60-464933E3E79E","label":"combo","options":{"children":[]},"servoydataprovider":" ","servoytitledataprovider":" "},"type":"SelectMenu"}'
}
