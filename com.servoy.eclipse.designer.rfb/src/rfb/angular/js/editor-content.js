angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $webSocket, $servoyInternal,$rootScope,$compile){
	 $rootScope.createComponent = function(html) {
		  	return $compile(html)($scope);
		  }
	 
	var realConsole = $window.console;
	$window.console = {
			log: function(msg) {
				if (typeof(consoleLog) != "undefined") {
					consoleLog("log",msg)
				}
				else if (realConsole) {
					realConsole.log(msg)
				}
				else alert(msg);
				
			},
			error: function(msg) {
				if (typeof(consoleLog) != "undefined") {
					consoleLog("error",msg)
				}
				else if (realConsole) {
					realConsole.error(msg)
				}				
				else alert(msg);
			}
	}
	function getURLParameter(name) {
			return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	
	if (typeof(WebSocket) == 'undefined' || getURLParameter("replacewebsocket")) {
		
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
			 var url = $windowService.getFormUrl(formName);
			 // this main url is in design (the template must have special markers)
			 return url?url+"&design=true":null;
		 }
	 }
 }).factory("$editorContentService", function() {
	 
	 return  {
		 refreshDecorators: function() {
			 renderDecorators();
		 }
	 }
 });