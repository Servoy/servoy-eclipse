angular.module('editor', ['palette','toolbar','contextmenu','mouseselection',"dragselection",'inlineedit','decorators','webSocketModule','keyboardlayoutupdater','highlight']).factory("$pluginRegistry",function($rootScope) {
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
	SELECTION_CHANGED : "SELECTION_CHANGED",
	SELECTION_MOVED : "SELECTION_MOVED",
	INITIALIZED: "INITIALIZED",
	RELOAD_PALETTE: "RELOAD_PALETTE"
}).value("EDITOR_CONSTANTS", {
	PART_LABEL_WIDTH: 100,
	PART_LABEL_HEIGHT: 20,
	PART_TYPE_TITLE_HEADER: 1,
	PART_TYPE_HEADER: 2,
	PART_TYPE_BODY: 5,
	PART_TYPE_FOOTER: 8,
	GHOST_TYPE_CONFIGURATION: "config",
	GHOST_TYPE_COMPONENT: "comp",
	GHOST_TYPE_PART: "part",
	GHOST_TYPE_FORM: "form"
}).directive("editor", function($window, $pluginRegistry, $rootScope, EDITOR_EVENTS, EDITOR_CONSTANTS, $timeout, $editorService, $webSocket) {
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

			var formName =  $webSocket.getURLParameter("f");
			var formLayout =  $webSocket.getURLParameter("l");
			var formWidth = parseInt($webSocket.getURLParameter("w"), 10);
			var formHeight = parseInt($webSocket.getURLParameter("h"), 10);
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
			$scope.editorID = $element.attr('id');
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
				if ($scope.ghosts)
				{
					for (i = 0; i< $scope.ghosts.ghostContainers.length; i++) {
						for (j = 0; j< $scope.ghosts.ghostContainers[i].ghosts.length; j++){
							if ($scope.ghosts.ghostContainers[i].ghosts[j].uuid == uuid)
								return $scope.ghosts.ghostContainers[i].ghosts[j];
						}
					}
				}	
				return null;
			}
			//returns an array of objects for the specified container uuid
			$scope.getContainedGhosts = function (uuid) {
				if ($scope.ghosts)
				{
					for (i = 0; i< $scope.ghosts.ghostContainers.length; i++) {
						if ($scope.ghosts.ghostContainers[i].uuid == uuid)
							return $scope.ghosts.ghostContainers[i].ghosts;
					}
				}
				return null;
			}

			$scope.getGhostContainerStyle = function(ghostContainer) {
				if(ghostContainer.style == undefined) {
					//TODO refactor out this 20px addition
					return {left: "20px", top: "20px", width: $scope.contentStyle.width, height: $scope.contentStyle.height};
				}
				return ghostContainer.style;
			}
			
			$scope.getGhostStyle = function(ghost) {
				if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // parts
					return {background: "#d0d0d0", top: ghost.location.y + "px", right: "-" + (EDITOR_CONSTANTS.PART_LABEL_WIDTH - 6) +"px", width: (EDITOR_CONSTANTS.PART_LABEL_WIDTH - 6) + "px", height: EDITOR_CONSTANTS.PART_LABEL_HEIGHT + "px", textAlign: "center", whiteSpace: "nowrap", cursor: "s-resize"};
				}
				else if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) { // the form
					return {left: 0, top: 0, width: ghost.size.width + "px", height: ghost.size.height + "px", padding: "3px"};
				}
				var xOffset = 0;
				var yOffset = 0;
				
				//TODO refactor out this 20px addition
				if (!$scope.isContentSizeFull()) {
					xOffset += 20;
					yOffset += 20;
				}
				return {background: "#e4844a", opacity: 0.7, padding: "3px", left: ghost.location.x + xOffset, top: ghost.location.y + yOffset, width: ghost.size.width, height: ghost.size.height};
			}
			
			$scope.getGhostHRStyle = function(ghost) {
				if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // parts
					return {marginTop:"-4px", border:0, borderTop:"1px dashed #000", width:(parseInt($scope.contentStyle.width, 10) + EDITOR_CONSTANTS.PART_LABEL_WIDTH - 15) + "px", float:"right"};
				}
				else {
					return {display:"none"};
				}
			}

			$scope.rearrangeGhosts = function(ghosts) {
				var overflow = 0;
				for (var i = 0; i < ghosts.length ; i++)
				{
					var ghost = ghosts[i];
					var prevGhost = i > 0 ? ghosts[i-1] : undefined;
					if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION)
					{
						if ($('[svy-id='+ghost.uuid+']')[0])
						{
							var element = $('[svy-id='+ghost.uuid+']')[0];
							var width = element.scrollWidth;
							if (prevGhost != undefined && ghost.location.y == prevGhost.location.y)
							{
								ghost.location.x += overflow;
							}
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
				if(ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_PART)
				{
					$editorService.openContainedForm(ghost);
				}
			}
			
			$scope.getBeanModel = function(node)
			{
				if(node)
				{
					var name = node.getAttribute("name");
					return $scope.getFormState().model[name];
				}
				return null;
			}
			
			$scope.getBeanModelOrGhost = function(node)
			{
				if(node)
				{
					var name = node.getAttribute("name");
					if (name)
						return $scope.getFormState().model[name];
					else
						{
							var ghostObject = $scope.getGhost(node.getAttribute("svy-id"));
							if (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_COMPONENT)
								return ghostObject;
						}
				}
				return null;
			}

			$scope.updateGhostLocation = function(ghost, x, y) {
				if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // it is a part
					if(y <= ghost.min || (ghost.max != -1 && y >= ghost.max)) {
						// part is overlapping its neighbors
						return false;
					}
				}
				if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) {
					return false;
				}
				ghost.location.x = x;
				ghost.location.y = y;
				return true;
			}
			
			$scope.updateGhostLocationLimits = function(ghost) {
				ghost.min = ghost.partprev ? $scope.getGhost(ghost.partprev).location.y : 0;
				ghost.max = ghost.partnext ? $scope.getGhost(ghost.partnext).location.y : -1;
			}
			
			$scope.updateGhostSize = function(ghost, deltaWidth, deltaHeight) {
				if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) {
					ghost.size.width = ghost.size.width + deltaWidth;
					$scope.contentStyle.width = ghost.size.width + "px";
					var part = $scope.getLastPartGhost();
					if (part != null)
					{
						$scope.updateGhostLocationLimits(part);
						if ($scope.updateGhostLocation(part, part.location.x, part.location.y + deltaHeight))
						{
							ghost.size.height = ghost.size.height + deltaHeight;
							$scope.contentStyle.height = ghost.size.height + "px";	
						}
					}
				}
				else if(ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART || ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
					// nop
				}
				else {
					ghost.size.height = ghost.size.height + deltaHeight;
					ghost.size.width = ghost.size.width + deltaWidth;
				}
			}
			
			$scope.getLastPartGhost = function() {
				var part = null;
				for (i = 0; i< $scope.ghosts.ghostContainers.length; i++) {
					var container = $scope.ghosts.ghostContainers[i];
					for (j = 0; j < container.ghosts.length; j++)
					{
						if (container.ghosts[j].type == EDITOR_CONSTANTS.GHOST_TYPE_PART) part = container.ghosts[j];
					}
				}
				return part;
			}
			
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				for(var i = 0; i < selection.length; i++) 
				{
					var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
					if(ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART)) $scope.updateGhostLocationLimits(ghost);
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
							var partsStyle = result.parts;
							var formScope = $scope.getFormState().getScope();
							for (i = 0; i < partsStyle.length; i++) {
								formScope[partsStyle[i].name + 'Style'] = partsStyle[i].style 
							}
							editorContentRootScope.$apply();
						});						
					}, 0);	
				}
				else if(fieldLocation) {
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

			$scope.convertToAbsolutePoint = function(point){
				var frameRect = $element.find('.contentframe')[0].getBoundingClientRect()
				if (point.x && point.y)
				{
					point.x = point.x + frameRect.left;
					point.y = point.y + frameRect.top;
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
				removeInvalidSelectionEntries();
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

				removeInvalidSelectionEntries();
				if (dirty) {
					markDirty()
				}
			}

			function removeInvalidSelectionEntries() {
				if(selection.length > 1) {
					var validSelection = new Array();
					for (var i = 0; i < selection.length; i++) {
						var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
						if(ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART || ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM))
						{
							continue;
						}
						validSelection.push(selection[i])
					}
					selection = validSelection;
				}
			}
			
			$scope.isAbsoluteFormLayout = function() {
				return formLayout == "absolute";
			}

			$scope.getFormState = function() {
				var state = servoyInternal.initFormState(formName); // this is a normal direct get if no init config is given
				if (state) $scope.lastState = state;
				return $scope.lastState;
			}

			$scope.refreshEditorContent = function() {
				if (editorContentRootScope) {
					editorContentRootScope.$digest();
					$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_MOVED,selection)
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
			}
			$scope.setContentSizeFull = function() {
				$scope.contentStyle = {top: "0px", left: "0px",paddingRight: "80px", bottom: "0px"};
				delete $scope.contentStyle.width;
				delete $scope.contentStyle.height;
				delete $scope.contentStyle.h
				delete $scope.contentStyle.w
				$scope.glasspaneStyle = {};
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
			
			$scope.setGhosts = function(ghosts) {
				$scope.ghosts = ghosts;
				
				if($scope.ghosts.ghostContainers) {
					for (i = 0; i< $scope.ghosts.ghostContainers.length; i++) {
						for(j = 0; j < $scope.ghosts.ghostContainers[i].ghosts.length; j++) {
							if($scope.ghosts.ghostContainers[i].ghosts[j].type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
								var changedGhosts = $scope.ghosts.ghostContainers[i].ghosts;
								$timeout(function() { $scope.rearrangeGhosts(changedGhosts) }, 0);
								break;
							}
						}
					}
				}
				
				if ($scope.refocusGlasspane)
				{
					$scope.glasspane.focus();
					$scope.refocusGlasspane = false;
				}
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
						if(!$scope.isAbsoluteFormLayout()) {
							$scope.contentStyle.height = (sizes.height + 20)  +"px"
						}
						$scope.glasspaneStyle.height = (sizes.height + 20)  +"px"
					}
					if ($scope.isContentSizeFull()) {
						if (contentDiv.clientWidth < sizes.width && (!$scope.contentStyle.w || $scope.contentStyle.w + 20 < sizes.width || $scope.contentStyle.w - 20 > sizes.width)) {
							$scope.contentStyle.w = sizes.width
							if(!$scope.isAbsoluteFormLayout()) {
								$scope.contentStyle.width = (sizes.width + 20)  +"px"	
							}
							$scope.glasspaneStyle.width = (sizes.width + 20)  +"px"
						}
					}
				}
			}
			
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				$scope.setContentSizes();
			})

			$element.on('documentReady.content', function(event, contentDocument) {
				
				if (!$scope.editorInitialized)
					$pluginRegistry.registerEditor($scope);

				$scope.contentDocument = contentDocument;
				var htmlTag = $scope.contentDocument.getElementsByTagName("html")[0];
				var injector = $scope.contentWindow.angular.element(htmlTag).injector();
				editorContentRootScope = injector.get("$rootScope");
				servoyInternal = injector.get("$servoyInternal");
				$scope.glasspane.focus()
				$(function(){   
					$(document).keyup(function(objEvent) {					
						var fixedKeyEvent = $scope.getFixedKeyEvent(objEvent);
	
		                // 46 = delete
						if (fixedKeyEvent.keyCode == 46) {
							// send the DELETE key code to the server
							$editorService.keyPressed(objEvent);
							return false;
						}
						return true;
					});
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
		                return true;
					});
				});
				
				
				var promise = $editorService.getGhostComponents({"resetPosition":true});
				promise.then(function (result){
					$scope.setGhosts(result);
				});
				if (!$scope.editorInitialized) {
					$timeout(function() {					
						if($scope.isAbsoluteFormLayout()) {
							$scope.setContentSize(formWidth + "px", formHeight + "px");
						}
						else {
							$scope.setContentSizeFull();
						}
					},500);
				}
				
				$scope.editorInitialized = true;
			});
			
			$element.on('renderGhosts.content', function(event) {
				var promise = $editorService.getGhostComponents();//no parameter, then the ghosts are not repositioned
				promise.then(function (result){
					$scope.setGhosts(result);
				});
			});
			
			$element.on('renderDecorators.content', function(event) {
				// TODO this is now in a timeout to let the editor-content be able to reload the form.
				// could we have an event somewhere from the editor-content that the form is reloaded and ready?
				// maybe the form controllers code could call $evalAsync as last thing in its controller when it is in design.
				if (selection.length > 0) {
					var ghost = $scope.getGhost(selection[0].getAttribute("svy-id"));
					if(ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM)) {						
						$scope.setContentSizes();
					}
					else {
						var promise = $editorService.getGhostComponents();//no parameter, then the ghosts are not repositioned
						promise.then(function (result){
							$scope.setGhosts(result);
							$timeout(function() {
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
							}, 100);
						});
					}
				}
				else {
					$scope.setContentSizes();
				}
			});

			$element.on('updateForm.content', function(event, formInfo) {
				if(formName == formInfo.name) {
					$scope.setContentSize(formInfo.w + "px", formInfo.h + "px");
					var ghost = $scope.getGhost(formInfo.uuid);
					if(ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM)) {						
						ghost.size.width = formInfo.w;
						ghost.size.height = formInfo.h;
						
						if(selection.length > 0 && selection[0].getAttribute("svy-id") == formInfo.uuid) {
							$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED,selection)
						}
					}	
				}
			});			
			
			$editorService.registerEditor($scope);
			$editorService.connect().then(function() {
				var replacews = $webSocket.getURLParameter("replacewebsocket") ? "&replacewebsocket=true" : "";
				$scope.contentframe = "content/editor-content.html?id=%23" + $element.attr("id") + "&f=" +formName +"&s=" + $webSocket.getURLParameter("s") + replacews;
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

	function testWebsocket() {
		if (typeof(WebSocket) == 'undefined' || $webSocket.getURLParameter("replacewebsocket"))
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
		wsSession = $webSocket.connect('', [$webSocket.getURLParameter('editorid')])
		wsSession.onopen(function()
		{
			connected = true;
			if (deferred) deferred.resolve();
			deferred = null;
			$rootScope.$broadcast(EDITOR_EVENTS.INITIALIZED)
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
		
		moveResponsiveComponent: function(properties) {
			wsSession.callService('formeditor', 'moveComponent', properties, true)
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

		isInheritedForm: function() {
			return wsSession.callService('formeditor', 'isInheritedForm')
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
				var obj = {};
				var firstSize = null;
				for (var i=0;i<selection.length;i++)
				{
					var node = selection[i];
					var beanModel = editorScope.getBeanModel(node);
					if (beanModel)
					{
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
				}
				this.sendChanges(obj);
			}
		},

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
		
		setInlineEditMode: function(inlineEdit){
			wsSession.callService('formeditor', 'setInlineEditMode', {"inlineEdit":inlineEdit}, true)
		},
		
		reloadPalette: function() {
			$rootScope.$emit(EDITOR_EVENTS.RELOAD_PALETTE, "")
		},
		
		getShortcuts: function()
		{
			return wsSession.callService('formeditor', 'getShortcuts');
		},
		
		toggleHighlight: function() {
			if (editorScope.getEditorContentRootScope().highlight == undefined)
				editorScope.getEditorContentRootScope().highlight = true;
			else
				editorScope.getEditorContentRootScope().highlight = !editorScope.getEditorContentRootScope().highlight;
			
			editorScope.getEditorContentRootScope().$digest();
		}
		// add more service methods here
	}
});
