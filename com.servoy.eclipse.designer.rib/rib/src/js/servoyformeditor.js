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

/*
 * Log to console using java callback
 */
if (typeof consoleLog != 'undefined') {
    window.console = {
	    log: function(msg) { consoleLog('log', msg) },
	    error: function(msg) { consoleLog('error', msg) },
	    info: function(msg) { consoleLog('info', msg) },
	    warn: function(msg) { consoleLog('warn', msg) },
	    trace: function(msg) { consoleLog('trace', msg) },
	    assert: function(msg) { consoleLog('assert', msg) }
    }
}

$.servoy = {

      _refreshing: false,
      
      editorid: undefined,
      websocket: undefined,
      connectedToWebsocket : function(editorid, websocket)
      {
      	$.servoy.editorid = editorid
      	$.servoy.websocket = websocket
      	
      	if (editorid) {
      		$.servoy.sendWebsocketMessage('') // register
      		websocket.onmessage = function(message) { $.servoy.handleMessage(message.data) }
      	}
	
		console.log('form design start')
		$.servoy.getReturnvalue(getFormDesign(), function(val) {
			console.log('save form design')
			requestFileSystem(window.TEMPORARY, 10,  function(filesystem) {
			    console.log('save design='+val)
			    console.log('save fs='+filesystem)
			    filesystem.saveServoyForm("form", val)
			    console.log('form design end')  	
	        })
		})
      },
      
      sendWebsocketMessage : function(message, defer)
      {
        if ($.servoy.websocket)
        {
        	if (defer)
        	{
				var deferred = $.Deferred()
				var msgid = $.servoy.getNextMessageId()
				$.servoy.deferredMessages[msgid] = deferred;
				$.servoy.websocket.send($.servoy.editorid+':#'+msgid+':' + message)
				return deferred.promise();
        	}
        	else
        	{
	          	$.servoy.websocket.send($.servoy.editorid+':' + message)
        	}
        }
        return null
      },
      
      getReturnvalue: function(o, func)
      {
      	if (o && o.then)
      	{
      		// a promise
      		$.when(o.then(func))
      	}
      	else
      	{
	      	// already a value, run later to mimic async behaviour
	      	/* setTimeout(function(){*/func.apply(this, [o])/*}, 0)*/
      	}
      },
      
      handleMessage : function(message)
      {
      	  // message either from websocket or from form editor java code
      	  
      	  if (message.substring(0, 1) == '#')
      	  {
      	  	// response message
      	  	var msgid = message.split(':').substring(1)
      	  	var deferred = $.servoy.deferredMessages[msgid];
      	  	if (deferred) {
      	  		deferred.resolve(message.substring(msgid.length+1))
      	  	}
      	  	return
      	  }
      	  
      	  var split = message.split(':')
      	  var func = split[0]
      	  if (func == 'refreshForm')
      	  {
      	  	$.servoy._refreshForm()
      	  }
      	  else if (func == 'refreshNode')
      	  {
      	  	$.servoy._refreshNode(split[1])
      	  }
      	  else if (func == 'selectNode')
      	  {
      		  $.servoy._selectNode(split[1])
      	  }
      	  else if (func == 'setPaletteItems')
      	  {
      		  $.servoy._setPaletteItems(split[1])
      	  }
      },
		
	  _refreshForm : function() // called from java to refresh on outside changes
	  {
	    if ($.servoy._refreshing) return
	    $.servoy._refreshing = true
	    
		var design = getFormDesign() // java browser function
		// refresh
		$.rib.updateDesignToJSON(ADM.getDesignRoot(), design);
		
		setTimeout("$.servoy._refreshing = false", 0);
		console.log('reload design end')
	},

	  _findNode : function(uuid) 
	  {
	 	var nodes = ADM.getDesignRoot().findNodesByProperty({type: 'string', name: 'id', value: uuid});
	 	if (nodes.length == 1)
	 	{
	 		return nodes[0].node
	 	}
	 	
	 	return null;
	},

	_refreshNode : function(uuid ) // called from java
	{
		console.log('refreshNode '+uuid)
		
		var node = $.servoy._findNode(uuid)
		if (!node)
		{
			console.log('refreshNode: node not found: '+uuid)
			return false;
		}
		var childJson = getChildJson(uuid)
		if (!childJson) 
		{
			console.log('refreshNode: json not found: '+uuid)
			return false;
		}
		
		var parsedObject
		try {
			parsedObject = $.parseJSON(childJson);
		} catch(e) {
			console.log('refreshNode: Invalid child design: '+childJson)
			return false;
		}
		if (parsedObject) {
			$.rib.updateSingleNode(node, parsedObject);
		}
		
		console.log('refreshNode done')
		return true
	},
	
	_selectNode : function(uuid ) // called from java
	{
		console.log('selectNode '+uuid)
		
		var node = $.servoy._findNode(uuid)
		if (node)
		{
			//node.suppressEvents(true)
			ADM.setSelected(node)
			//node.suppressEvents(false)
		}
		else {
			console.log('selectNode: node not found: '+uuid)
		}
		
		console.log('selectNode done')
	},

	_setPaletteItems : function(itemsString) // called from java
	{
		var items = $.parseJSON(itemsString);
		console.log('_setPaletteItems '+items)
		 $(':rib-paletteView').paletteView('option', "model", [ { "Palette Items": items }]);
		console.log('_setPaletteItems done')
	},
	
   handleModelUpdated: function(event, widget)
   {
     if ($.servoy._refreshing) return
      
   	  var newid = callbackModelUpdated(event.type, event.node.getType(), event.node.getParent().getProperty('id'), event.node.getZone(), event.node.getZoneIndex(), JSON.stringify(event.node._properties))
      if (event.type == 'nodeAdded' && newid)
      {
      	event.node.setProperty('id', newid)
      }
   }
}

requestFileSystem(window.TEMPORARY, 10,  function(filesystem)
{
    filesystem.saveServoyForm = function(formName, designString)
	{
		console.log('saveServoyForm '+formName+'-'+designString)
		// Fill with Servoy form data
		var projects = window.inMemFileSystem.root._getEntry('projects', true, false, true)
		var formDir = projects._getEntry(formName, true, false, true)
		
		var createPinfo = function() {
			formDir._getEntry('pInfo.json', true, false, false).createWriter(function (fileWriter) {
				var info = {
					name:formName,
					theme: 'Default',
					device: {
						name: 'Phones',
						screenHeight: 320,
						screenWidth: 480
					}
				}
				fileWriter.write(new Blob([JSON.stringify(info)]));
			})
		}
	
		formDir._getEntry('design.json', true, false, false).createWriter(function (fileWriter) {
			fileWriter.write(new Blob([designString]));
			createPinfo()
		})
	}
});

// Utility function to get the query parameters
// TODO use builtin from jquery
var QueryParameters = (function()
{
    var result = {};

    if (window.location.search)
    {
        // split up the query string and store in an associative array
        var params = window.location.search.slice(1).split("&");
        for (var i = 0; i < params.length; i++)
        {
            var tmp = params[i].split("=");
            result[tmp[0]] = unescape(tmp[1]);
        }
    }

    return result;
}());

if (QueryParameters.editorid && typeof(WebSocket) != 'undefined')
{
	console.log('connecting to form editor websocket')
	// TODO: take ws url from request parameters
	 var websocket = new WebSocket("ws://localhost:8080/messageproxy/wsproxy"); // export war from com.servoy.server.messageproxy to webapps/messageproxy.war
	 websocket.onopen = function () {
			console.log('Info: WebSocket connection opened.');
			$.servoy.connectedToWebsocket(QueryParameters.editorid, websocket)
	 }
}
else 
{
	// load using builtin functions
	$.servoy.connectedToWebsocket(null, null)
}
