angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $webSocket, $servoyInternal){
	function getURLParameter(name) {
			return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	if (typeof(WebSocket) == 'undefined') {
		WebSocket = SwtWebSocket;
		 
		function SwtWebSocket(url)  
		{
			this.id = parent.window.addWebSocket(this);
			var me = this;
			function onopenCaller(){
				parent.window.SwtWebsocketBrowserFunction('open', url, me.id)
				me.onopen()
			}
			setTimeout(onopenCaller, 0);
		}
		
		SwtWebSocket.prototype.send = function(str)
		{
			parent.window.SwtWebsocketBrowserFunction('send', str, this.id)
		}
	}
	 $servoyInternal.connect();
	 var formName = getURLParameter("f");
	 $scope.getUrl = function() {
		 if ($webSocket.isConnected()) {
			 return $windowService.getFormUrl(formName);
		 }
	 }
 })