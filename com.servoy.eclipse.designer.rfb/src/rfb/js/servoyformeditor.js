angular.module('servoyEditorApp', ['webSocketModule'])
.controller("editorController", function($scope) {
	
}).factory("$editorService", function($rootScope, $webSocket, $log) {
	
	var wsSession = $webSocket.connect('editor', 'rfbtest') // TODO: use uuid of EditorWebsocketSession instance
	wsSession.onopen = function()
	{
		$log.info('Info: WebSocket connection opened.');
		wsSession.callService('formeditor', 'getFormLayoutGrid').then(function(layoutGrid) {
    		setLayoutSrc(layoutGrid)
    	})
	}
	
	return {
		// add service methods here
		somemethod: function(somearg) {
			return "did something"
		}
	}
})
.run(function($editorService) {
	// triggers connect to server
})