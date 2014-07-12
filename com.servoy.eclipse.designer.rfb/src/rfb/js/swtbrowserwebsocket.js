
// Install fake webSocket in case browser is running in SWT and browser implementation does not support WebSocket
if (typeof(WebSocket) == 'undefined' && typeof(SwtWebsocketBrowserFunction) != 'undefined')
{
	WebSocket = SwtWebSocket

	var $currentSwtWebsocket = null

	function SwtWebSocket(url)  
	{
		if ($currentSwtWebsocket != null)
		{
			SwtWebsocketBrowserFunction('close')
		}
		$currentSwtWebsocket = this
		
		setTimeout(function(){
			SwtWebsocketBrowserFunction('open', url)
			$currentSwtWebsocket.onopen()
		}, 0);
	}

	SwtWebSocket.prototype.send = function(str)
	{
		SwtWebsocketBrowserFunction('send', str)
	}

	function SwtWebsocketClient(command, arg1, arg2)
	{
		if (command == 'receive')
		{
			$currentSwtWebsocket.onmessage({data: arg1})
		}
		else if (command == 'close')
		{
			$currentSwtWebsocket.onclose({code: arg1, reason: arg2})
		}
		else if (command == 'error')
		{
			$currentSwtWebsocket.onerror(arg1)
		}
	}
}
