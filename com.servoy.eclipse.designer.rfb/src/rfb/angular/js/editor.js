angular.module('editor', ['palette','toolbar','contextmenu','mouseselection',"dragselection",'decorators','webSocketModule','keyboardlayoutupdater']).factory("$pluginRegistry",function($rootScope) {
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
}).value("EDITOR_CONSTANTS", {
	PART_LABEL_WIDTH: 80,
	PART_LABEL_HEIGHT: 20,
	PART_PERSIST_TYPE: 19,
	PART_TYPE_TITLE_HEADER: 1,
	PART_TYPE_HEADER: 2,
	PART_TYPE_BODY: 5,
	PART_TYPE_FOOTER: 8
}).directive("editor", function( $window, $pluginRegistry,$rootScope,EDITOR_EVENTS,EDITOR_CONSTANTS,$timeout,$editorService){
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
			var servoyInternal = null;
			var fieldLocation = null;
			
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
					$($element).on(eventType, null, eventCallback);
				}
				else if (target == "CONTENT_AREA")
				{
					$($element.find('.content-area')[0]).on(eventType, null, eventCallback)
				}
				else if (target == "PALETTE")
				{
					$($element.find('.palette')[0]).on(eventType, null, eventCallback)
				}
				else if (target == "CONTENTFRAME_OVERLAY") {
					$($scope.glasspane).on(eventType, null, eventCallback)
				}
				return eventCallback;
			}
			
			
			
			//returns the ghost object with the specified uuid
			$scope.getGhost = function (uuid) {
				for (i = 0; i< $scope.ghosts.ghostContainers.length; i++) {
					for (j = 0; j< $scope.ghosts.ghostContainers[i].ghosts.length; j++){
						if ($scope.ghosts.ghostContainers[i].ghosts[j].uuid == uuid)
							return $scope.ghosts.ghostContainers[i].ghosts[j];
					}
				}
				return null;
			}
			//returns an array of objects for the specified container uuid
			$scope.getContainedGhosts = function (uuid) {
				for (i = 0; i< $scope.ghosts.ghostContainers.length; i++) {
					if ($scope.ghosts.ghostContainers[i].uuid == uuid)
						return $scope.ghosts.ghostContainers[i].ghosts;
				}
				return null;
			}

			$scope.getGhostContainerStyle = function(ghostContainer) {
				if(ghostContainer.style == undefined) {
					if($scope.isContentSizeFull()) {
						return {left: "0px", top: "0px", right: EDITOR_CONSTANTS.PART_LABEL_WIDTH + "px", bottom: "0px"};
					} else {
						return {left: "20px", top: "20px", width: $scope.contentStyle.width, height: $scope.contentStyle.height};
					}
				}
				return ghostContainer.style;
			}
			
			$scope.getGhostStyle = function(ghost) {
				if(ghost.type == EDITOR_CONSTANTS.PART_PERSIST_TYPE) { // parts
					var partStyle = {right: "-" + (EDITOR_CONSTANTS.PART_LABEL_WIDTH - 6) +"px", width: (EDITOR_CONSTANTS.PART_LABEL_WIDTH - 6) + "px", height: EDITOR_CONSTANTS.PART_LABEL_HEIGHT + "px", textAlign: "center", whiteSpace: "nowrap"};
					switch(ghost.parttype) {
						case EDITOR_CONSTANTS.PART_TYPE_TITLE_HEADER:
						case EDITOR_CONSTANTS.PART_TYPE_HEADER:
							partStyle.top = (ghost.location.y - EDITOR_CONSTANTS.PART_LABEL_HEIGHT) + "px";
							break;
						case EDITOR_CONSTANTS.PART_TYPE_BODY:
							partStyle.bottom = ghost.partnext ? ($scope.getGhost(ghost.partnext).location.y - ghost.location.y) + "px" : "0px";	
							break;
						case EDITOR_CONSTANTS.PART_TYPE_FOOTER:
							partStyle.bottom = "0px";
						
					}
					return partStyle;
				}
				return {background: "#e4844a", padding: "3px", left: ghost.location.x, top: ghost.location.y, width: ghost.size.width, height: ghost.size.height};
			}
			
			$scope.rearrangeGhosts = function(ghosts) {
				var overflow = 0;
				for (var i = 0; i < ghosts.length ; i++)
				{
					var ghost = ghosts[i];
					if (ghost.type != EDITOR_CONSTANTS.PART_PERSIST_TYPE)
					{
						if ($('[svy-id='+ghost.uuid+']')[0])
						{
							var element = $('[svy-id='+ghost.uuid+']')[0];
							var width = element.scrollWidth;
							ghost.location.x = ghost.location.x + overflow;
							if (width > ghost.size.width){
								overflow += width - ghost.size.width;
								ghost.size.width = width;
							}
						}
					}
				}
				return true;
			}
			
			$scope.openContainedForm = function(ghost){
				if(ghost.type != EDITOR_CONSTANTS.PART_PERSIST_TYPE)
				{
					$editorService.openContainedForm(ghost);
				}
			}
			
			$scope.updateGhostLocation = function(ghost, x, y) {
				if(ghost.type == EDITOR_CONSTANTS.PART_PERSIST_TYPE) { // it is a part

					if(ghost.max == -1 || y <= ghost.min || y >= ghost.max) {
						// part is overlapping its neighbors or it is last part (ghost.max == -1)
						return;
					}
				}
				
				ghost.location.x = x;
				ghost.location.y = y;
			}
			
			$scope.updateGhostLocationLimits = function(ghost) {
				ghost.min = ghost.partprev ? $scope.getGhost(ghost.partprev).location.y : ($scope.isContentSizeFull() ? 0 : EDITOR_CONSTANTS.PART_LABEL_HEIGHT);
				ghost.max = ghost.partnext ? $scope.getGhost(ghost.partnext).location.y : -1;
			}
			
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				for(var i = 0; i < selection.length; i++) 
				{
					var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
					if(ghost && (ghost.type == EDITOR_CONSTANTS.PART_PERSIST_TYPE)) $scope.updateGhostLocationLimits(ghost);
				}
			})

			function getMousePosition(event) {
				var xMouse = -1;
				var yMouse = -1;
				if (event.pageX || event.pageY) {
					xMouse = event.pageX;
					yMouse = event.pageY;
				}
				else if (event.clientX || event.clientY) {
					xMouse = event.clientX;
					yMouse = event.clientY;			
				}
				
				return {x: xMouse, y: yMouse};
			}
			
			$scope.getFixedKeyEvent = function(event) {
				var keyCode, isCtrl, isShift, isAlt;

	            if(window.event) {	//IE
	            	keyCode = window.event.keyCode;
	            	isCtrl = window.event.ctrlKey ? true : false;
	            	isShift = window.event.shiftKey ? true : false;
	            	isAlt =  window.event.altKey ? true : false;
	            }
	            else {	// firefox
	            	keyCode = event.which;
	            	isCtrl = event.ctrlKey ? true : false;
	            	isShift = event.shiftKey ? true : false;
	            	isAlt =  event.altKey ? true : false;		                        
	            }
	            
	            return {keyCode:keyCode, isCtrl:isCtrl, isShift:isShift, isAlt:isAlt};
			}
			
			$scope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", function(event) {
				fieldLocation = getMousePosition(event);
			});
			
			$scope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", function(event) {
				var selection = $scope.getSelection();
				
				var isPartSelected = false;
				
				for(var i = 0; i < selection.length; i++) 
				{
					var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
					if(ghost && (ghost.type == 19)) {
						isPartSelected = true;
						break;
					}
				}				
				
				if(isPartSelected) {
					$timeout(function() {
						var promise = $editorService.getPartsStyles();
						promise.then(function (result){
							var formScope = $scope.getFormState().getScope();
							for (i = 0; i < result.length; i++) {
								formScope[result[i].name + 'Style'] = result[i].style 
							}
							editorContentRootScope.$digest();
						});						
					}, 0);	
				}
				else {
					var currentMouseLocation = getMousePosition(event);
					if(fieldLocation.x == currentMouseLocation.x && fieldLocation.y == currentMouseLocation.y) {
						$editorService.updateFieldPositioner($scope.convertToContentPoint(fieldLocation));
					}						
				}
			});
			
			
			$scope.unregisterDOMEvent = function(eventType, target,callback) {
				if (target == "FORM") {
					$($scope.contentDocument).off(eventType,null,callback)
				} else if (target == "EDITOR") {
					$($element).off(eventType,null,callback);
				}
				else if (target == "CONTENT_AREA")
				{
					$($element.find('.content-area')[0]).off(eventType,null,callback);
				}
				else if (target == "CONTENTFRAME_OVERLAY")
				{
					$($scope.glasspane).off(eventType, null, callback)
				}
				else if (target == "PALETTE")
				{
					$($element.find('.palette')[0]).off(eventType,null,callback);
				}
			}

			$scope.convertToContentPoint = function(point){
				var frameRect = $element.find('.contentframe')[0].getBoundingClientRect()
				if (point.x && point.y)
				{
					point.x = point.x - frameRect.left;
					point.y = point.y - frameRect.top;
				} else if (point.top && point.left)
				{
					point.left = point.left - frameRect.left;
					point.top = point.top - frameRect.top;
				}
				return point
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

			$scope.isAbsoluteFormLayout = function() {
				return formLayout == "absolute";
			}

			$scope.getFormState = function() {
				return servoyInternal.initFormState(formName); // this is a normal direct get if no init config is given
			}

			$scope.refreshEditorContent = function() {
				if (editorContentRootScope) {
					editorContentRootScope.$digest();
					$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
				}
			}

			$scope.getEditorContentRootScope = function() {
				return editorContentRootScope;
			}

			$scope.contentStyle = {top: "0px", left: "0px", paddingRight: "80px", bottom: "0px"};
			$scope.glasspaneStyle = {};

			$scope.setContentSize = function(width, height) {
				$scope.contentStyle.width = width;
				$scope.contentStyle.height = height;
				delete $scope.contentStyle.top;
				delete $scope.contentStyle.left;
				delete $scope.contentStyle.paddingRight;
				delete $scope.contentStyle.bottom;
				delete $scope.contentStyle.h
				delete $scope.contentStyle.w
				$scope.glasspaneStyle = {};
				$timeout(function() {
					$scope.setContentSizes();
				},100);
			}
			$scope.setContentSizeFull = function() {
				$scope.contentStyle = {top: "0px", left: "0px",paddingRight: "80px", bottom: "0px"};
				delete $scope.contentStyle.width;
				delete $scope.contentStyle.height;
				delete $scope.contentStyle.h
				delete $scope.contentStyle.w
				$scope.glasspaneStyle = {};
				$timeout(function() {
					$scope.setContentSizes();
				},100);
			},
			$scope.getContentSize = function() {
				return {width: $scope.contentStyle.width, height: $scope.contentStyle.height};
			}
			$scope.isContentSizeFull = function() {
				return ($scope.contentStyle.paddingRight == "80px") && ($scope.contentStyle.bottom == "0px");
			}
			
			$scope.setCursorStyle = function(cursor) {
				$scope.glasspane.style.cursor = cursor;
			}
			
			function getScrollSizes(x) {
				var height = 0;
				var width = 0;
				for (var i =0;i<x.length;i++) {
					if (x[i].scrollHeight > height) {
						height = x[i].scrollHeight;
					}
					if (x[i].scrollWidth > width) {
						width = x[i].scrollWidth;
					}
					var childHeights = getScrollSizes($(x[i]).children())
					if (childHeights.height > height) {
						height = childHeights.height;
					}
					if (childHeights.width > width) {
						width = childHeights.width;
					}
				}
				return {height:height,width:width}
			}

			$scope.setContentSizes = function() {
				var sizes = getScrollSizes($scope.contentDocument.querySelectorAll(".sfcontent"));
				if (sizes.height > 0 && sizes.width > 0) {
					var contentDiv = $element.find('.content-area')[0];
					if (contentDiv.clientHeight < sizes.height && (!$scope.contentStyle.h || $scope.contentStyle.h + 20 < sizes.height || $scope.contentStyle.h - 20 > sizes.height)) {
						$scope.contentStyle.h = sizes.height
						$scope.contentStyle.height = (sizes.height + 20)  +"px"
						$scope.glasspaneStyle.height = (sizes.height + 20)  +"px"
					}
					if ($scope.isContentSizeFull()) {
						if (contentDiv.clientWidth < sizes.width && (!$scope.contentStyle.w || $scope.contentStyle.w + 20 < sizes.width || $scope.contentStyle.w - 20 > sizes.width)) {
							$scope.contentStyle.w = sizes.width
							$scope.contentStyle.width = (sizes.width + 20)  +"px"
							$scope.glasspaneStyle.width = (sizes.width + 20)  +"px"
						}
					}
				}
			}
			
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				$scope.setContentSizes();
			})

			$element.on('documentReady.content', function(event, contentDocument) {
				$scope.contentDocument = contentDocument;
				$pluginRegistry.registerEditor($scope);

				var htmlTag = $scope.contentDocument.getElementsByTagName("html")[0];
				var injector = $scope.contentWindow.angular.element(htmlTag).injector();
				editorContentRootScope = injector.get("$rootScope");
				servoyInternal = injector.get("$servoyInternal");
				$scope.glasspane.focus()
				$(function(){   
					$(document).keydown(function(objEvent) {					
						var fixedKeyEvent = $scope.getFixedKeyEvent(objEvent);
	
		                if(fixedKeyEvent.isCtrl)
		                {
		                	var k = String.fromCharCode(fixedKeyEvent.keyCode).toLowerCase();
		                    if ('a' == k || 's' == k )
		                    {
		                    	if(fixedKeyEvent.isShift && 's' == k) {
									// send the CTRL+SHIFT+S (save all) key code to the server
		                    		$editorService.keyPressed(objEvent);
		                    	}                         
			                   return false;
		                    }
		                }
		                // 46 = delete
						if (fixedKeyEvent.keyCode == 46) {
							// send the DELETE key code to the server
							$editorService.keyPressed(objEvent);
							return false;
						}
						return true;
					});
				}); 
				var promise = $editorService.getGhostComponents({"resetPosition":true});
				promise.then(function (result){
					$scope.ghosts = result;
				});
				$timeout(function() {
					$scope.setContentSizes();
				},500);
			});


			$element.on('renderDecorators.content', function(event) {
				// TODO this is now in a timeout to let the editor-content be able to reload the form.
				// could we have an event somewhere from the editor-content that the form is reloaded and ready?
				// maybe the form controllers code could call $evalAsync as last thing in its controller when it is in design.
				if (selection.length > 0) {
					$timeout(function() {
						var promise = $editorService.getGhostComponents();//no parameter, then the ghosts are not repositioned
						promise.then(function (result){
							$scope.ghosts = result;
						});
						var nodes = Array.prototype.slice.call($scope.contentDocument.querySelectorAll("[svy-id]"));
						var ghosts = Array.prototype.slice.call($scope.glasspane.querySelectorAll("[svy-id]"));
						nodes = nodes.concat(ghosts);
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
				else {
					$scope.setContentSizes();
				}
			});

			$editorService.registerEditor($scope);
			var promise = $editorService.connect();
			promise.then(function() {
				var replacews = $editorService.getURLParameter("replacewebsocket") ? "&replacewebsocket=true" : "";
				$scope.contentframe = "content/editor-content.html?id=%23" + $element.attr("id") + "&f=" +formName +"&s=" + $editorService.getURLParameter("s") + replacews;
			})
		},
		templateUrl: 'templates/editor.html',
		replace: true
	};

}).factory("$editorService", function($rootScope, $webSocket, $log, $q,$window, EDITOR_EVENTS, $rootScope,$timeout) {
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
	var wsSession = null;
	var connected = false;
	var deferred = null;

	function getURLParameter(name) {
		return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec($window.location.search)||[,""])[1].replace(/\+/g, '%20'))||null
	}
	function testWebsocket() {
		if (typeof(WebSocket) == 'undefined' || getURLParameter("replacewebsocket"))
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
		wsSession = $webSocket.connect('', [getURLParameter('editorid')])
		wsSession.onopen(function()
		{
			connected = true;
			if (deferred) deferred.resolve();
			deferred = null;
		});
	}

	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		var sel = []
		for(var i=0;i<selection.length;i++) {
			sel[sel.length] = selection[i].getAttribute("svy-id");
		}
		wsSession.callService('formeditor', 'setSelection', {selection: sel}, true)
	})
	var editorScope; //todo this should become a array if we want to support multiply editors on 1 html page.
	return {
		registerEditor: function(scope) {
			editorScope = scope;
		},
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

		keyPressed: function(event) {
			wsSession.callService('formeditor', 'keyPressed', {ctrl:event.ctrlKey,shift:event.shiftKey,alt:event.altKey,keyCode:event.keyCode}, true)
		},

		sendChanges: function(properties) {
			wsSession.callService('formeditor', 'setProperties', properties, true)
		},

		createComponent: function(component) {
			wsSession.callService('formeditor', 'createComponent', component, true)
		},

		getGhostComponents: function(node) {
			return wsSession.callService('formeditor', 'getGhostComponents', node, false)
		},

		getPartsStyles: function() {
			return wsSession.callService('formeditor', 'getPartsStyles', null, false)
		},

		createComponents: function(components) {
			wsSession.callService('formeditor', 'createComponents', components, true)
		},
		
		openElementWizard: function(elementType) {
			wsSession.callService('formeditor', 'openElementWizard', {elementType: elementType}, true)
		},
		
		updateFieldPositioner: function(location) {
			wsSession.callService('formeditor', 'updateFieldPositioner', {location: location}, true)
		},

		executeAction: function(action,params)
		{
			wsSession.callService('formeditor', action, params, true)
		},
		
		sameSize : function(width)
		{
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1)
			{
				var formState = editorScope.getFormState();
				var obj = {};
				var firstSize = null;
				for (var i=0;i<selection.length;i++)
				{
					var node = selection[i];
					var name = node.getAttribute("name");
					var beanModel = formState.model[name];
					if (firstSize == null)
					{
						firstSize = beanModel.size;
					}
					else
					{
						var newSize;
						if (width)
						{
							newSize = {width:firstSize.width,height:beanModel.size.height};
						}
						else
						{
							newSize = {width:beanModel.size.width,height:firstSize.height};
						}
						obj[node.getAttribute("svy-id")] = newSize;
					}
				}
				this.sendChanges(obj);
			}
		},
		getURLParameter: getURLParameter,

		updateSelection: function(ids) {
			$timeout(function(){
				var prevSelection = editorScope.getSelection();
				var changed = false;
				var selection = [];
				if (ids && ids.length > 0) {
					var nodes = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[svy-id]"));
					var ghosts = Array.prototype.slice.call(editorScope.glasspane.querySelectorAll("[svy-id]"));
					nodes = nodes.concat(ghosts);
					for(var i=0;i<nodes.length;i++) {
						var id = nodes[i].getAttribute("svy-id");
						if (ids.indexOf(id) != -1) {
							selection.push(nodes[i]);
							changed = changed || prevSelection.indexOf(nodes[i]) == -1;
							if (selection.length == ids.length) break;
						}
					}
				}
				else if (prevSelection.length > 0) {
					changed = true;
				}
				if (changed) editorScope.setSelection(selection);
			},400);
		},
		
		openContainedForm: function(ghost) {
			wsSession.callService('formeditor', 'openContainedForm', {"uuid":ghost.uuid}, true)
		},
		// add more service methods here
	}
});
