angular.module('editorContent',['servoyApp'])
 .controller("MainController", function($scope, $window, $timeout, $windowService, $webSocket,$sabloApplication, $servoyInternal,$rootScope,$compile,$solutionSettings, $editorContentService){
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
	 var formModelData = null;
	 var formUrl = null;
	 $scope.getUrl = function() {
		 if (formUrl) return formUrl;
		 if ($webSocket.isConnected()) {
			 if (formModelData == null) {
				 formModelData = {}
				 var promise = $sabloApplication.callService("$editor", "getData", {form:formName,solution:solutionName},false);
				 promise.then(function(data){
					 formModelData = JSON.parse(data);
					 $editorContentService.formData(formModelData);
					 formUrl = "solutions/" + solutionName + "/forms/" + formName + ".html?design=true&highlight="+$rootScope.highlight;
				 })
			 }
			 else {
				 // this main url is in design (the template must have special markers)
				 return formUrl;
			 }
		 }
	 };
	 

 }).controller("DesignForm",function($scope, $editorContentService, $rootScope) {
	
	 $scope.formStyle = {left:"0px",right:"0px",top:"0px",bottom:"0px"}
	 
	 $scope.removeComponent = function(name) {
		 delete model[name];
		 delete api[name];
		 delete handlers[name];
		 delete servoyApi[name];
		 delete layout[name];
	 } 
	 
	 $rootScope.getDesignFormControllerScope = function () {
	     return $scope;
	 };
	 
	 var formData = $editorContentService.formData();
	 // TODO should this be converted?
	 
	 var model = {}
	 var api = {}
	 var handlers = {}
	 var servoyApi = {}
	 var layout = {}
	 
	 $editorContentService.setLayoutData(layout);
	 
	 $scope.model = function(name) {
		 var ret = model[name];
		 if (!ret) {
			 ret = {}
			 if (formData.components[name]) ret = formData.components[name]; 
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
	 $scope.layout = function(name) {
		 var ret = layout[name];
		 if (!ret) {
			 ret = {}
			 if (formData.components[name]) {
				 ret.left = formData.components[name].location.x +"px"; 
				 ret.top = formData.components[name].location.y +"px";
				 ret.width = formData.components[name].size.width +"px"; 
				 ret.height = formData.components[name].size.height +"px";
				 ret.position = 'absolute'
			 }
			 layout[name] = ret;
		 }
		 return ret;
	 } 
 }).factory("$editorContentService", function($rootScope,$applicationService,$sabloApplication,$sabloConstants,$webSocket,$compile) {
	 var formData = null;
	 var layoutData = null
	 return  {
		 refreshDecorators: function() {
			 renderDecorators();
		 },
		 refreshGhosts: function() {
			 renderGhosts();
		 },
		 updateForm: function(name, uuid, w, h) {
			 updateForm({name:name, uuid:uuid, w:w, h:h});
		 },
		 formData: function(data) {
			 if (data) {
				 formData = data;
				 if (formData.solutionProperties) {
					 $applicationService.setStyleSheet(formData.solutionProperties.styleSheet);
				 }
			 }
			 else return formData;
		 },
		 setLayoutData: function(data) {
			layoutData = data; 
		 },
		 updateFormData: function(updates) {
			var data = JSON.parse(updates);
			if (data && (data.components || data.deleted)) {
				// TODO should it be converted??
				$rootScope.$apply(function() {
					for(var name in data.components) {
						var compData = formData.components[name];
						var newCompData = data.components[name];
						if (compData) {
							var modifyFunction = compData[$sabloConstants.modelChangeNotifier];
							for(var key in compData) {
								if (!newCompData[key]) {
									compData[key] = null;
									if (modifyFunction) modifyFunction(key,null)
								}
							}
							// copy it inside so that we update the data inside the model
							for(var key in newCompData) {
								compData[key] = newCompData[key];
								if (modifyFunction) modifyFunction(key,newCompData[key])
							}
						}
						else {
							formData.components[name] = newCompData;
							var highlight = $webSocket.getURLParameter("highlight");
							 var promise = $sabloApplication.callService("$editor", "getTemplate", {name:name,highlight:highlight},false);
							 promise.then(function(data){
								// append the template
								var json = JSON.parse(data);
								var parentId = json.parentId;
								if (!parentId) parentId = 'svyDesignForm';
								var parent = angular.element(document.getElementById(parentId));
								var tpl = $compile( json.template )( $rootScope.getDesignFormControllerScope() );
								parent.append(tpl)
							 })
							
						}
						var compLayout = layoutData[name];
						if (compLayout) {
							compLayout.left = newCompData.location.x +"px"; 
							compLayout.top = newCompData.location.y +"px";
							compLayout.width = newCompData.size.width +"px"; 
							compLayout.height = newCompData.size.height +"px";
						}
					}
					for(var index in data.deleted) {
						var toDelete = $('[svy-id="'+data.deleted[index]+'"]');
						var name = toDelete.attr("name");
						if (name) $rootScope.getDesignFormControllerScope().removeComponent(name);
						toDelete.remove();
						
					}
					renderDecorators();
				});
			}
			if (data && data.solutionProperties && formData.solutionProperties.styleSheet) {
				$applicationService.setStyleSheet(formData.solutionProperties.styleSheet);
	 		}
		 }
		 
	 }
 }).factory("loadingIndicator",function() {
	//the loading indicator should not be shown in the editor
	return {
		showLoading: function() {},
		hideLoading: function() {}
	}
});
