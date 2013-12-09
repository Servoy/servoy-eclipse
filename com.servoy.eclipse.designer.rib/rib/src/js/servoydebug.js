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
	return '{"children":[{"children":[{"children":[{"properties":{"id":"5BEE5E5D-6EA4-4FFC-9CDA-CD6BF76203D9","right":true,"servoydataprovider":" ","text":"Products","theme":"b"},"type":"Button","zone":"right"},{"properties":{"icon":"refresh","id":"6C21CA78-C3F1-41F4-ACBA-0F56474E5954","right":false,"servoydataprovider":" ","text":"Sync","theme":"b"},"type":"Button","zone":"left"}],"properties":{"id":"header_FB8E97F2-B86F-45DC-AB83-4BAE74A1A86E","position":"default","servoydataprovider":" ","text":"AccountManagerz","theme":"b"},"type":"Header","zone":"top"},{"children":[{"properties":{"countbubble":" ","headertext":"Companies","headertheme":"b","id":"2F72DC24-6501-4664-AFE9-CF1CD71261C8","servoydataprovider":"accountmanager_to_companies.company_name","servoysubtextdataprovider":"accountmanager_to_companies.company_description","servoytitledataprovider":" ","subtext":" ","text":" "},"type":"InsetList"},{"properties":{"id":"EBF08648-8084-491A-B577-9631504334C6","servoydataprovider":" ","text":"button","theme":"b"},"type":"Button"},{"properties":{"id":"EB4E2EA7-95EB-4A9B-90FA-64C15796BC98","label":"Title","servoydataprovider":" ","servoytitledataprovider":" "},"type":"TextInput"},{"properties":{"id":"9FA22BC4-FF1D-49FD-9B5A-A063B49BB95D","label":"Title","servoydataprovider":" ","servoytitledataprovider":" "},"type":"SingleCheckbox"}],"properties":{"id":"content_901391C1-0DDB-45BF-BF64-62F4B28B033E"},"type":"Content","zone":"content"}],"properties":{"id":"901391C1-0DDB-45BF-BF64-62F4B28B033E","theme":"d"},"type":"Page"}],"type":"Design"}'
}
