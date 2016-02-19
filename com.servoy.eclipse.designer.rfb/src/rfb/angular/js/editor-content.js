angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $document, $webSocket, $servoyInternal,$rootScope,$compile,$solutionSettings){
		$rootScope.createComponent = function(html, model, componentName) {
				var compScope = $scope.$new(false);
				if (!$scope.model)
				    $scope.model = {};
				$scope.model[model.componentName] = model;
				compScope.api = {};
				compScope.handlers = {};
				var el = $compile(html)(compScope);
				angular.element($document[0].body).append(el);
				return el;
		}
	$rootScope.highlight = false;
	$rootScope.showWireframe = false;
	$solutionSettings.enableAnchoring = false;
	$scope.solutionSettings = $solutionSettings;
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

	if (typeof(WebSocket) == 'undefined' || $webSocket.getURLParameter("replacewebsocket")=='true') {

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
	 var formName = $webSocket.getURLParameter("f");
	 var high = $webSocket.getURLParameter("highlight");
	 $rootScope.highlight = high;
	 $scope.getUrl = function() {
		 if ($webSocket.isConnected()) {
			 var url = $windowService.getFormUrl(formName);
			 // this main url is in design (the template must have special markers)
			 return url?url+"&design=true"+"&highlight="+$rootScope.highlight:null;
		 }
	 }
 }).factory("$editorContentService", function() {

	 return  {
		 refreshDecorators: function() {
			 renderDecorators();
		 },
		 refreshGhosts: function() {
			 renderGhosts();
		 },
		 updateForm: function(name, uuid, w, h) {
			 updateForm({name:name, uuid:uuid, w:w, h:h});
		 }
	 }
 }).factory("loadingIndicator",function() {
	//the loading indicator should not be shown in the editor
	return {
		showLoading: function() {},
		hideLoading: function() {}
	}
});
