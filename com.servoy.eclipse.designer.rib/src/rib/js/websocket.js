$.ws = {
	websocket : undefined,
	nextMessageId : 1,
	deferredMessages : {},
	connectedHandlers : [],

	_getNextMessageId : function() {
		return $.ws.nextMessageId++
	},

	_sendWebsocketMessage : function(message, defer) {
		if ($.ws.websocket) {
			if (defer) {
				var deferred = $.Deferred()
				var msgid = $.ws._getNextMessageId()
				$.ws.deferredMessages[msgid] = deferred;
				$.ws.websocket.send('<' + msgid + ':' + message)
				return deferred.promise();
			} else {
				$.ws.websocket.send(message)
			}
		}

		return null
	},

	_handleMessage : function(message) {

		console.log('received message: ' + message)
		if (message.substring(0, 1) == '>') {
			// response message
			var msgid = message.split(':')[0].substring(1)
			var deferred = $.ws.deferredMessages[msgid];
			if (deferred) {
				delete $.ws.deferredMessages[msgid]
				deferred.resolve(message.substring(msgid.length + 2))
			}
		}
	},

	_connectToDeveloper : function() {
		console.log('connectToDeveloper start')

		if (typeof (WebSocket) == 'undefined') {
			console.log('connectToDeveloper: no WebSocket support')
			return false;
		}

		var editorid = getURLParameter("editorid")
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

			console.log('connectedToDeveloper start')
			$.ws.websocket = websocket
			$.ws.websocket.onmessage = function(message) {
				$.ws._handleMessage(message.data)
			}

			// inform handlers we are connected
			for ( var i in $.ws.connectedHandlers) {
				$.ws.connectedHandlers[i]()
			}
			$.ws.connectedHandlers = null
		}
	},

	// public api

	/**
	 * Register for when connected to developer
	 */
	onConnectedToDeveloper : function(handler) {
		if ($.ws.connectedHandlers) {
			$.ws.connectedHandlers.push(handler)
		} else {
			handler()
		}
	},

	/**
	 * Send message to editor, optional response handler for the reply
	 */
	sendMessage : function(message, responseHandler) {
		if (responseHandler) {
			$.when($.ws._sendWebsocketMessage(message, true)).then(
					responseHandler)
		} else {
			$.ws._sendWebsocketMessage(message, false)
		}
	}

}

$(document).ready(function() {
	$.ws._connectToDeveloper();
});
