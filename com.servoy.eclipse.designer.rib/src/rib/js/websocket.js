$.editor = {
	websocket : undefined,
	nextMessageId : 1,
	deferredMessages : {},

	getNextMessageId : function() {
		return $.editor.nextMessageId++
	},

	sendWebsocketMessage : function(message, defer) {
		if ($.editor.websocket) {
			if (defer) {
				var deferred = $.Deferred()
				var msgid = $.editor.getNextMessageId()
				$.editor.deferredMessages[msgid] = deferred;
				$.editor.websocket.send(message)
				return deferred.promise();
			} else {
				$.editor.websocket.send(message)
			}
		}

		return null
	},
	
	handleMessage : function(message) {
		
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
		
		console.log('received message: '+message)
	}
}

function connectedToDeveloper(websocket) {
	console.log('connectedToDeveloper start')
	$.editor.websocket = websocket
	$.editor.websocket.onmessage = function(message) { $.editor.handleMessage(message.data) }

	$.editor.sendWebsocketMessage('hi')
}

function connectToDeveloper() {
	console.log('connectToDeveloper start')

	if (typeof (WebSocket) == 'undefined') {
		console.log('connectToDeveloper: no WebSocket support')
		return false;
	}

	var editorid = getParameterByName("editorid")
	if (!editorid) {
		console.log('connectToDeveloper: no editorid in url')
		return false;
	}

	var uri
	if (window.location.protocol === "https:") {
		uri = "wss:";
	} else {
		uri = "ws:";
	}
	uri += "//" + window.location.host + "/formeditor/" + editorid;

	console.log('connectToDeveloper: connecting to ' + uri)

	var websocket = new WebSocket(uri);
	websocket.onopen = function() {
		connectedToDeveloper(websocket)
	}
}

function getParameterByName(name) {
	var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
	return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
}

$(document).ready(function() {
	connectToDeveloper();
});
