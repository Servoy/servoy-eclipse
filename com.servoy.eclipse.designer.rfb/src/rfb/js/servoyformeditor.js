angular.module('servoyEditorApp', ['webSocketModule'])
.controller("editorController", function($scope) {
	
}).factory("$editorService", function($rootScope, $webSocket, $log) {

	var wsSession = $webSocket.connect('editor', getURLParameter('editorid'))
	
	var callback = {
		setSelection: function(sel) {
			wsSession.callService('formeditor', 'setSelection', {selection: sel}, true)
		}
	}
	
	wsSession.onopen = function()
	{
		$log.info('Info: WebSocket connection opened.');
		wsSession.callService('formeditor', 'getFormLayoutGrid').then(function(layoutGrid) {
    		setLayoutSrc(layoutGrid, callback)
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