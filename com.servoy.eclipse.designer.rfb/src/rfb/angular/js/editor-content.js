angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $webSocket, $servoyInternal,$rootScope,$compile,$solutionSettings){
	 $rootScope.createComponent = function(html,model) {
			 var compScope = $scope.$new(true);
			 compScope.model = model;
			 compScope.api = {};
			 compScope.handlers = {};
			 var el = $compile(html)(compScope);
			 $('body').append(el); 
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
	 var solutionName = $webSocket.getURLParameter("s");
	 var high = $webSocket.getURLParameter("highlight");
	 $rootScope.highlight = high;
	 $scope.getUrl = function() {
		 if ($webSocket.isConnected()) {
			 var url = "solutions/" + solutionName + "/forms/" + formName + ".html"; //$windowService.getFormUrl(formName);
			 // this main url is in design (the template must have special markers)
			 return url?url+"?design=true"+"&highlight="+$rootScope.highlight:null;
		 }
	 }
 }).controller("DesignForm",function($scope){
	
	 var model = {}
	 var api = {}
	 var handlers = {}
	 var servoyApi = {}
	 
	 $scope.model = function(name) {
		 var ret = model[name];
		 if (!ret) {
			 ret = {}
			 model[name] = ret;
		 }
		 return ret;
	 } 
	 $scope.api = function(name) {
		 var ret = api[name];
		 if (!ret) {
			 ret = {}
			 api[name] = ret;
		 }
		 return ret;
	 } 
	 $scope.handlers = function(name) {
		 var ret = handlers[name];
		 if (!ret) {
			 ret = {}
			 handlers[name] = ret;
		 }
		 return ret;
	 } 
	 $scope.servoyApi = function(name) {
		 var ret = servoyApi[name];
		 if (!ret) {
			 ret = {}
			 servoyApi[name] = ret;
		 }
		 return ret;
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
