angular.module('editor', ['palette','toolbar','mouseselection',"dragselection",'decorators','webSocketModule']).factory("$pluginRegistry",function($rootScope) {
	var plugins = [];

	return {
		registerEditor: function(editorScope) {
			for(var i=0;i<plugins.length;i++) {
				plugins[i](editorScope);
			}
		},
		
		registerPlugin: function(plugin) {
			plugins[plugins.length] = plugin;
		},
	}
}).value("EDITOR_EVENTS", {
    SELECTION_CHANGED : "SELECTION_CHANGED"
}).directive("editor", function( $window, $pluginRegistry,$rootScope,EDITOR_EVENTS, $timeout,$editorService){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {},
	      link: function($scope, $element, $attrs) {
			var timeout;
			var delta = {
				addedNodes: [],
				removedNodes: []
			}
			var selection = [];
			function markDirty() {
				if (timeout) {
					clearTimeout(timeout)
				}
				timeout = $timeout(fireSelectionChanged, 1)
			}
			
			var formName =  $editorService.getURLParameter("f");
			var formLayout =  $editorService.getURLParameter("l");
			var editorContentRootScope = null;
			var formState = null;
			
			function fireSelectionChanged(){
				//Reference to editor should be gotten from Editor instance somehow
				//instance.fire(Editor.EVENT_TYPES.SELECTION_CHANGED, delta)
				$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
				delta.addedNodes.length = delta.removedNodes.length = 0
				timeout = null
			}
			

			$scope.contentWindow = $element.find('.contentframe')[0].contentWindow;
			$scope.glasspane = $element.find('.contentframe-overlay')[0];
			$scope.contentDocument = null;
			$scope.registerDOMEvent = function(eventType, target,callback) {
				var eventCallback = callback.bind(this);
				if (target == "FORM") {
					$($scope.contentDocument).on(eventType, null, eventCallback)
				} else if (target == "EDITOR") {
					console.log("registering dom event: " + eventType)
					// $(doc) is the document of the editor (or a div)
					//	$(doc).on(eventType, context, callback.bind(this))
				}
				else if (target == "CONTENT_AREA")
				{
					$($element.find('.content-area')[0]).on(eventType, null, eventCallback)
				}
				else if (target == "CONTENTFRAME_OVERLAY") {
					$($scope.glasspane).on(eventType, null, callback.bind(this))
				}
				return eventCallback;
			}
			
			$scope.moveGlasspaneAbove = function (){
				$scope.glasspane.style.zIndex = "1";
			} 
			$scope.moveGlasspaneBelow = function (){
				$scope.glasspane.style.zIndex = "0";
			} 
			
			$scope.unregisterDOMEvent = function(eventType, target,callback) {
				if (target == "FORM") {
					$($scope.contentDocument).off(eventType,null,callback)
				} else if (target == "EDITOR") {
					console.log("unregistering dom event: " + eventType)
				}
				else if (target == "CONTENT_AREA")
				{
					$($element.find('.content-area')[0]).off(eventType,null,callback);
				}
			}
			$scope.getSelection = function() {
				//Returning a copy so selection can't be changed my modifying the selection array
				return selection.slice(0)
			}
		
			$scope.extendSelection = function(nodes) {
				var ar = Array.isArray(nodes) ? nodes : [nodes]
				var dirty = false
				
				for (var i = 0; i < ar.length; i++) {
					if (selection.indexOf(ar[i]) === -1) {
						dirty = true
						delta.addedNodes.push(ar[i])
						selection.push(ar[i])
					}
				}
				if (dirty) {
					markDirty()
				}
			}
		
			$scope.reduceSelection = function(nodes) {
				var ar = Array.isArray(nodes) ? nodes : [nodes]
				var dirty = false
				for (var i = 0; i < ar.length; i++) {
					var idx = selection.indexOf(ar[i])
					if (idx !== -1) {
						dirty = true
						delta.removedNodes.push(ar[i])
						selection.splice(idx,1)
					}
				}
				if (dirty) {
					markDirty()
				}
			}
			$scope.setSelection = function(node) {
				var ar = Array.isArray(node) ? node : node ? [node] : []
				var dirty = ar.length||selection.length
				Array.prototype.push.apply(delta.removedNodes, selection)
				selection.length = 0
				
				Array.prototype.push.apply(delta.addedNodes, ar)
				Array.prototype.push.apply(selection, ar)
				
				if (dirty) {
					markDirty()
				}
			}
			
			$scope.getFormLayout = function() {
				return formLayout;
			}
			
			$scope.getFormState = function() {
				return formState;
			}
			
			$scope.refreshEditorContent = function() {
				if (editorContentRootScope) {
					editorContentRootScope.$digest();
					$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
				}
			}
			
			$scope.contentStyle = {width: "100%", height: "100%"};
			
			$scope.setContentSize = function(width, height) {
				$scope.contentStyle.width = width;
				$scope.contentStyle.height = height;
			}
			$scope.getContentSize = function() {
				return {width: $scope.contentStyle.width, height: $scope.contentStyle.height};
			}
			$scope.isContentSizeFull = function() {
				var size = $scope.getContentSize();
				return (size.width == "100%") && (size.height == "100%");
			}
			
			$element.on('documentReady.content', function(event, contentDocument) {
				$scope.contentDocument = contentDocument;
				$pluginRegistry.registerEditor($scope);
				
				var htmlTag = $scope.contentDocument.getElementsByTagName("html")[0];
				var injector = $scope.contentWindow.angular.element(htmlTag).injector();
				editorContentRootScope = injector.get("$rootScope");
				var servoyInternal = injector.get("$servoyInternal");
				var promise = servoyInternal.getFormState(formName);
				promise.then(function(state){
					formState = state;
				})
			});
			

			$element.on('renderDecorators.content', function(event) {
				// TODO this is now in a timeout to let the editor-content be able to reload the form.
				// could we have an event somewhere from the editor-content that the form is reloaded and ready?
				// maybe the form controllers code could call $evalAsync as last thing in its controller when it is in design.
				if (selection.length > 0) {
					$timeout(function() {
						var nodes = $scope.contentDocument.querySelectorAll("[svy-id]")
						var matchedElements = []
						for (var i = 0; i < nodes.length; i++) {
							var element = nodes[i]
							for(var s=0;s<selection.length;s++) {
								if (selection[s].getAttribute("svy-id") == element.getAttribute("svy-id")){
									matchedElements.push(element);
									break;
								}
							}
						}
						selection = matchedElements;
						$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
					},100)
				}
			});
			
			
			var promise = $editorService.connect();
			promise.then(function() {
				$scope.contentframe = "editor-content.html?endpoint=designclient&id=%23" + $element.attr("id") + "&f=" +formName +"&s=" + $editorService.getURLParameter("s");
			})
	      },
	      templateUrl: 'templates/editor.html',
	      replace: true
	    };
	
}).factory("$editorService", function($rootScope, $webSocket, $log, $q,$window, EDITOR_EVENTS, $rootScope) {
	
	if (typeof(console) == "undefined") {
		$window.console = {
				log: function(msg) {
					if (typeof(consoleLog) != "undefined") {
						consoleLog("log",msg)
					}
					else alert(msg);
					
				},
				error: function(msg) {
					if (typeof(consoleLog) != "undefined") {
						consoleLog("error",msg)
					}
					else alert(msg);
				}
		}
	}
	var wsSession = null;
	var connected = false;
	var deferred = null;
	
	function getURLParameter(name) {
		return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	function testWebsocket() {
		if (typeof(WebSocket) == 'undefined')
		{
			if (typeof(SwtWebsocketBrowserFunction) != 'undefined') 
			{
				WebSocket = SwtWebSocket

				var $currentSwtWebsockets = [];
				
				$window.addWebSocket = function(socket) {
					var id = $currentSwtWebsockets.length;
					$currentSwtWebsockets[id] = socket;
					return id;
				}

				function SwtWebSocket(url)  
				{
					var me = this;
					me.id = $currentSwtWebsockets.length;
					$currentSwtWebsockets[me.id] = me;
					setTimeout(function(){
						SwtWebsocketBrowserFunction('open', url, me.id)
						me.onopen()
					}, 0);
				}

				SwtWebSocket.prototype.send = function(str)
				{
					SwtWebsocketBrowserFunction('send', str, this.id)
				}

				function SwtWebsocketClient(command, arg1, arg2, id)
				{
					if (command == 'receive')
					{
						$currentSwtWebsockets[id].onmessage({data: arg1})
					}
					else if (command == 'close')
					{
						$currentSwtWebsockets[parseInt(id)].onclose({code: arg1, reason: arg2})
						$currentSwtWebsockets[parseInt(id)] = null;
					}
					else if (command == 'error')
					{
						$currentSwtWebsockets[parseInt(id)].onerror(arg1)
					}
				}
				$window.SwtWebsocketClient = SwtWebsocketClient;
			} 
			else 
			{
				$timeout(testWebsocket,100);
				return;
			}
		}
		wsSession = $webSocket.connect('editor', getURLParameter('editorid'))
		wsSession.onopen = function()
		{
			connected = true;
			if (deferred) deferred.resolve();
			deferred = null;
			
		}
	}
	
	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		var sel = []
		for(var i=0;i<selection.length;i++) {
			sel[sel.length] = selection[i].getAttribute("svy-id");
		}
		wsSession.callService('formeditor', 'setSelection', {selection: sel}, true)
	})
	
	return {
		connect: function() {
			if (deferred) return deferred.promise;
			deferred = $q.defer();
			var promise = deferred.promise;
			if(!connected) testWebsocket();
			else {
				deferred.resolve();
				deferred = null;
			}
			return promise;
		},
		
		sendChanges: function(properties) {
			wsSession.callService('formeditor', 'setProperties', properties, true)
		},
		
		getURLParameter: getURLParameter,
		
		updateSelection: function(ids) {
			// TODO
		}
	
	// add more service methods here
	}
});
