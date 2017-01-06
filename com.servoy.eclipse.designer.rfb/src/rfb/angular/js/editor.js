angular.module('editor', ['mc.resizer', 'palette', 'toolbar', 'contextmenu', 'mouseselection', "dragselection",
	'inlineedit', 'decorators', 'webSocketModule', 'keyboardlayoutupdater', 'highlight'
]).factory("$pluginRegistry", function() {
	var plugins = [];

	return {
		registerEditor: function(editorScope) {
			for (var i = 0; i < plugins.length; i++) {
				plugins[i](editorScope);
			}
		},

		registerPlugin: function(plugin) {
			plugins[plugins.length] = plugin;
		}
	}
}).factory("$allowedChildren", function() {
	var allowedChildren = {};	
	return {
		get: function(layoutName) {
			return allowedChildren[layoutName];
		},
		set: function(map)
		{
			allowedChildren = map;
		}
	}
}).value("EDITOR_EVENTS", {
	SELECTION_CHANGED: "SELECTION_CHANGED",
	SELECTION_MOVED: "SELECTION_MOVED",
	INITIALIZED: "INITIALIZED",
	RENDER_DECORATORS: "RENDER_DECORATORS",
	HIDE_DECORATORS: "HIDE_DECORATORS"
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
	GHOST_TYPE_FORM: "form",
	GHOST_TYPE_INVISIBLE: "invisible",
	GHOST_TYPE_GROUP: "group"
}).directive("editor", function($window, $pluginRegistry, $rootScope, EDITOR_EVENTS, EDITOR_CONSTANTS, $timeout,
	$editorService, $webSocket, $q, $interval,$allowedChildren,$document) {
	return {
		restrict: 'E',
		transclude: true,
		scope: {},
		link: function($scope, $element) {
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

			 $document.bind('selectstart', function(event) {
				 if (!$editorService.isInlineEditMode())
				 {
					 return false
				 }	 
		      })
		          
			var formName = $webSocket.getURLParameter("f");
			var formLayout = $webSocket.getURLParameter("l");
			var formWidth = parseInt($webSocket.getURLParameter("w"), 10);
			var formHeight = parseInt($webSocket.getURLParameter("h"), 10);
			var editorContentRootScope = null;
			var servoyInternal = null;
			var fieldLocation = null;

			function fireSelectionChanged() {
				//Reference to editor should be gotten from Editor instance somehow
				//instance.fire(Editor.EVENT_TYPES.SELECTION_CHANGED, delta)
				$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, selection)
				delta.addedNodes.length = delta.removedNodes.length = 0
				timeout = null
			}

			$scope.contentWindow = $element.find('.contentframe')[0].contentWindow;
			$scope.glasspane = $element.find('.contentframe-overlay')[0];
			$scope.editorID = $element.attr('id');
			$scope.contentDocument = null;
			$scope.contentSizeFull = true;
			
			$scope.startAutoScroll = function (direction, callback){
				return $interval(function (){
				    var contentArea = ($element.find('.content-area')[0]);
				    var changeX = 0;
				    var changeY = 0;
				    switch(direction) {
				    	case "BOTTOM_AUTOSCROLL":
							if ((contentArea.scrollTop + contentArea.offsetHeight) === contentArea.scrollHeight)
							angular.element($scope.glasspane).height(angular.element($scope.glasspane).height()+5);
							contentArea.scrollTop += 5;
							changeY = 5;
							break;
				    	case "RIGHT_AUTOSCROLL":
							if ((contentArea.scrollLeft + contentArea.offsetWidth) === contentArea.scrollWidth)
							angular.element($scope.glasspane).width(angular.element($scope.glasspane).width()+5);
				    		contentArea.scrollLeft += 5;
				    		changeX = 5;
				    		break;
				    	case "LEFT_AUTOSCROLL":
				    		if(contentArea.scrollLeft >= 5) {
				    			contentArea.scrollLeft -= 5;
				    			changeX = -5;
				    		}
				    		break;
				    	case "TOP_AUTOSCROLL":
				    		if(contentArea.scrollTop >= 5) {
				    			contentArea.scrollTop -= 5;
				    			changeY = -5;
				    		}
				    		break;
				    }
				    
				    if (callback) callback($scope, $scope.selectionToDrag,changeX,changeY,0,0);
				    $scope.refreshEditorContent();
				},100);
			};

			$scope.registerDOMEvent = function(eventType, target, callback) {
				var eventCallback = callback.bind(this);
				if (target == "FORM") {
					$($scope.contentDocument).on(eventType, null, eventCallback)
				} else if (target == "EDITOR") {
					$($element).on(eventType, null, eventCallback);
				} else if (target == "CONTENT_AREA") {
					$($element.find('.content-area')[0]).on(eventType, null, eventCallback)
				} else if (target == "PALETTE") {
					$($element.find('.palette')[0]).on(eventType, null, eventCallback)
				} else if (target == "CONTENTFRAME_OVERLAY") {
					//workaround to make the contextmenu show on osx
					if (eventType == "contextmenu") $scope.ctxmenu = eventCallback;
					$($scope.glasspane).on(eventType, null, eventCallback)
				} else if (target == "BOTTOM_AUTOSCROLL") {
					$($element.find('.bottomAutoscrollArea')[0]).on(eventType, null, eventCallback)
				} else if (target == "RIGHT_AUTOSCROLL") {
					$($element.find('.rightAutoscrollArea')[0]).on(eventType, null, eventCallback)
				} else if (target == "LEFT_AUTOSCROLL") {
					$($element.find('.leftAutoscrollArea')[0]).on(eventType, null, eventCallback)
				} else if (target == "TOP_AUTOSCROLL") {
					$($element.find('.topAutoscrollArea')[0]).on(eventType, null, eventCallback)
				}
				
				return eventCallback;
			}
			
			$scope.unregisterDOMEvent = function(eventType, target, callback) {
				if (target == "FORM") {
					$($scope.contentDocument).off(eventType, null, callback)
				} else if (target == "EDITOR") {
					$($element).off(eventType, null, callback);
				} else if (target == "CONTENT_AREA") {
					$($element.find('.content-area')[0]).off(eventType, null, callback);
				} else if (target == "CONTENTFRAME_OVERLAY") {
					$($scope.glasspane).off(eventType, null, callback)
				} else if (target == "PALETTE") {
					$($element.find('.palette')[0]).off(eventType, null, callback);
				} else if (target == "BOTTOM_AUTOSCROLL") {
					$($element.find('.bottomAutoscrollArea')[0]).off(eventType, null, callback)
				} else if (target == "RIGHT_AUTOSCROLL") {
					$($element.find('.rightAutoscrollArea')[0]).off(eventType, null, callback)
				} else if (target == "LEFT_AUTOSCROLL") {
					$($element.find('.leftAutoscrollArea')[0]).off(eventType, null, callback)
				} else if (target == "TOP_AUTOSCROLL") {
					$($element.find('.topAutoscrollArea')[0]).off(eventType, null, callback)
				}
			}

			$scope.getFormName = function () {
			    return formName;
			}
			
			$scope.getContentAreaStyle = function() {
				var contentAreaStyle = {};
				if ($scope.isAbsoluteFormLayout()) {
					contentAreaStyle.minWidth = parseInt($scope.contentStyle.width, 10) + EDITOR_CONSTANTS.PART_LABEL_WIDTH + 20 + 'px';
					contentAreaStyle.minHeight = parseInt($scope.contentStyle.height, 10) + EDITOR_CONSTANTS.PART_LABEL_HEIGHT + 20 + 'px';
				}
				return contentAreaStyle;
			}

			//returns the ghost object with the specified uuid
			$scope.getGhost = function(uuid) {
					if ($scope.ghosts) {
						for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
							for (j = 0; j < $scope.ghosts.ghostContainers[i].ghosts.length; j++) {
								if ($scope.ghosts.ghostContainers[i].ghosts[j].uuid == uuid)
									return $scope.ghosts.ghostContainers[i].ghosts[j];
							}
						}
					}
					return null;
				}
				//returns an array of objects for the specified container uuid
			$scope.getContainedGhosts = function(uuid) {
				if ($scope.ghosts) {
					for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
						if ($scope.ghosts.ghostContainers[i].uuid == uuid)
							return $scope.ghosts.ghostContainers[i].ghosts;
					}
				}
				return null;
			}

			function getRealContainerElement(uuid) {
				var parent = $scope.ghostContainerElements[uuid];

				if (parent == undefined) {
					var defer = $q.defer();
					function testElement() {
						var p = $('.contentframe').contents().find('[svy-id="' + uuid + '"]');
						if (p[0] != undefined) {
							parent = p[0];
							$scope.ghostContainerElements[uuid] = parent;
							defer.resolve(parent);
						} else {
							$timeout(testElement, 400);
						}
					}
					$timeout(testElement, 400);
					$scope.ghostContainerElements[uuid] = defer.promise;
					return defer.promise;
				}
				return parent;
			}

			function getBounds(ghostContainer, parent) {
				var bounds = parent.getBoundingClientRect();
				ghostContainer.style.top = bounds.top;
				ghostContainer.style.left = bounds.left;
				ghostContainer.style.display = "block";
			}
			var realContainerPromise = null;
			$scope.getGhostContainerStyle = function(ghostContainer) {
				if (!$scope.isAbsoluteFormLayout()) {
					if (realContainerPromise == null) {
						var p = getRealContainerElement(ghostContainer.uuid);
						if (p.then) {
							realContainerPromise = p;
							p.then(function(parent) {
								realContainerPromise = null;
								getBounds(ghostContainer, parent);
							}, function() {
								realContainerPromise = null;
								ghostContainer.style.display = "none";
							});
						} else {
							getBounds(ghostContainer, p);
						}
					}
				} else {
					if (ghostContainer.style == undefined) {
						//TODO refactor out this 20px addition
						return {
							display: "block",
							left: "20px",
							top: "20px",
							width: $scope.contentStyle.width,
							height: $scope.contentStyle.height
						};
					}
					ghostContainer.style.display = "block";
				}
				return ghostContainer.style;
			}

			$scope.getGhostStyle = function(ghost) {
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // parts
					return {
						background: "#d0d0d0",
						top: ghost.location.y + "px",
						right: "-" + (EDITOR_CONSTANTS.PART_LABEL_WIDTH - 6) + "px",
						width: (EDITOR_CONSTANTS.PART_LABEL_WIDTH - 6) + "px",
						height: EDITOR_CONSTANTS.PART_LABEL_HEIGHT + "px",
						textAlign: "center",
						whiteSpace: "nowrap",
						cursor: "s-resize",
						overflow: "visible"
					};
				} else if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) { // the form
					return {
						left: 0,
						top: 0,
						width: ghost.size.width + "px",
						height: ghost.size.height + "px",
						padding: "3px"
					};
				}
				var xOffset = 20;
				var yOffset = 20;

				var style = {
					opacity: 0.7,
					padding: "3px",
					left: ghost.location.x + xOffset,
					top: ghost.location.y + yOffset,
					width: ghost.size.width,
					height: ghost.size.height
				};
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_INVISIBLE) {
					style.background = "#d0d0d0";
				}
				else if (ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
				{
					style.background = "#e4844a";
				}
				return style;
			}

			$scope.getGhostHRStyle = function(ghost) {
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // parts
					return {
						marginTop: "-4px",
						border: 0,
						borderTop: "1px dashed #000",
						width: (parseInt($scope.contentStyle.width, 10) + EDITOR_CONSTANTS.PART_LABEL_WIDTH - 15) + "px",
						float: "right"
					};
				} else {
					return {
						display: "none"
					};
				}
			}

			$scope.rearrangeGhosts = function(ghosts) {
				var overflow = 0;
				for (var i = 0; i < ghosts.length; i++) {
					var ghost = ghosts[i];
					var prevGhost = i > 0 ? ghosts[i - 1] : undefined;
					if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
						if ($('[svy-id="' + ghost.uuid + '"]')[0]) {
							var element = $('[svy-id="' + ghost.uuid + '"]')[0];
							var width = element.scrollWidth;
							if (prevGhost != undefined && ghost.location.y == prevGhost.location.y) {
								ghost.location.x += overflow;
							}
							if (width > ghost.size.width) {
								overflow += width - ghost.size.width;
								ghost.size.width = width;
							}
						}
					}
				}
				return true;
			}

			$scope.openContainedForm = function(ghost) {
				if (ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_PART) {
					$editorService.openContainedForm(ghost);
				}
			}

			$scope.getBeanModel = function(node) {
				if (node) {
					var name = node.getAttribute("svy-id");
					if (name) return editorContentRootScope.getDesignFormControllerScope().model(name, true);
				}
				return null;
			}

			$scope.getBeanModelOrGhost = function(node) {
				if (node) {
					var name = node.getAttribute("name");
					if (name)
						return editorContentRootScope.getDesignFormControllerScope().model(node.getAttribute("svy-id"), true);
					else {
						var ghostObject = $scope.getGhost(node.getAttribute("svy-id"));
						if (ghostObject && (ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_COMPONENT || ghostObject.type ==
								EDITOR_CONSTANTS.GHOST_TYPE_INVISIBLE))
							return ghostObject;
					}
				}
				return null;
			}

			$scope.updateGhostLocation = function(ghost, x, y) {
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // it is a part
					if (y <= ghost.min || (ghost.max != -1 && y >= ghost.max)) {
						// part is overlapping its neighbors
						return false;
					}
				}
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) {
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
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) {
					ghost.size.width = ghost.size.width + deltaWidth;
					$scope.contentStyle.width = ghost.size.width + "px";
					var part = $scope.getLastPartGhost();
					if (part != null) {
						$scope.updateGhostLocationLimits(part);
						if ($scope.updateGhostLocation(part, part.location.x, part.location.y + deltaHeight)) {
							ghost.size.height = ghost.size.height + deltaHeight;
							$scope.contentStyle.height = ghost.size.height + "px";
						}
					}
				} else if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART || ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
					// nop
				} else {
					ghost.size.height = ghost.size.height + deltaHeight;
					ghost.size.width = ghost.size.width + deltaWidth;
				}
			}

			$scope.getLastPartGhost = function() {
				var part = null;
				for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
					var container = $scope.ghosts.ghostContainers[i];
					for (j = 0; j < container.ghosts.length; j++) {
						if (container.ghosts[j].type == EDITOR_CONSTANTS.GHOST_TYPE_PART) part = container.ghosts[j];
					}
				}
				return part;
			}

			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				for (var i = 0; i < selection.length; i++) {
					var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
					if (ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART)) $scope.updateGhostLocationLimits(ghost);
				}
			})

			function getMousePosition(event) {
				var xMouse = -1;
				var yMouse = -1;
				if (event.pageX || event.pageY) {
					xMouse = event.pageX;
					yMouse = event.pageY;
				} else if (event.clientX || event.clientY) {
					xMouse = event.clientX;
					yMouse = event.clientY;
				}

				return {
					x: xMouse,
					y: yMouse
				};
			}

			$scope.getFixedKeyEvent = function(event) {
				var keyCode, isCtrl, isShift, isAlt;

				if (window.event) { //IE
					keyCode = window.event.keyCode;
					isCtrl = window.event.ctrlKey ? true : false;
					isShift = window.event.shiftKey ? true : false;
					isAlt = window.event.altKey ? true : false;
				} else { // firefox
					keyCode = event.which;
					isCtrl = event.ctrlKey ? true : false;
					isShift = event.shiftKey ? true : false;
					isAlt = event.altKey ? true : false;
				}

				return {
					keyCode: keyCode,
					isCtrl: isCtrl,
					isShift: isShift,
					isAlt: isAlt
				};
			}

			$scope.registerDOMEvent("mousedown", "CONTENTFRAME_OVERLAY", function(event) {
				fieldLocation = getMousePosition(event);
			});

			$scope.registerDOMEvent("mouseup", "CONTENTFRAME_OVERLAY", function(event) {
				var selection = $scope.getSelection();

				var isPartSelected = false;
				for (var i = 0; i < selection.length; i++) {
					var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
					if (ghost && (ghost.type == 19)) {
						isPartSelected = true;
						break;
					}
				}

				if (isPartSelected) {
					$timeout(function() {
						var promise = $editorService.getPartsStyles();
						promise.then(function(result) {
							var partsStyle = result.parts;
							var formScope = editorContentRootScope.getDesignFormControllerScope();
							for (i = 0; i < partsStyle.length; i++) {
								formScope[partsStyle[i].name + 'Style'] = partsStyle[i].style
							}
							editorContentRootScope.$apply();
						});
					}, 0);
				} else if (fieldLocation) {
					var currentMouseLocation = getMousePosition(event);
					if (fieldLocation.x == currentMouseLocation.x && fieldLocation.y == currentMouseLocation.y) {
						$editorService.updateFieldPositioner($scope.convertToContentPoint(fieldLocation));
					}
				}
			});

			$scope.convertToContentPoint = function(point) {
				var frameRect = $element.find('.contentframe')[0].getBoundingClientRect()
				if (point.x && point.y) {
					point.x = point.x - frameRect.left;
					point.y = point.y - frameRect.top;
				} else if (point.top && point.left) {
					point.left = point.left - frameRect.left;
					point.top = point.top - frameRect.top;
				}
				return point
			}

			$scope.convertToAbsolutePoint = function(point) {
				function isFiniteNumber(value) {
					return angular.isNumber(value) && isFinite(value);
				}
				var frameRect = $element.find('.content')[0].getBoundingClientRect()
				if (isFiniteNumber(point.x) && isFiniteNumber(point.y)) {
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
						selection.splice(idx, 1)
					}
				}
				if (dirty) {
					markDirty()
				}
			}
			$scope.setSelection = function(node) {
				var ar = Array.isArray(node) ? node : node ? [node] : []
				var dirty = ar.length || selection.length
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
				if (selection.length > 1) {
					var validSelection = new Array();
					for (var i = 0; i < selection.length; i++) {
						var ghost = $scope.getGhost(selection[i].getAttribute("svy-id"));
						if (ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART || ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM)) {
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
			
			$scope.refreshEditorContent = function() {
				if (editorContentRootScope) {
					// TODO this digest makes it slow when moving, do we really need this?
					//editorContentRootScope.$digest();
					$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_MOVED, selection)
				}
			}

			$scope.getEditorContentRootScope = function() {
				return editorContentRootScope;
			}

			$scope.contentStyle = {
				position: "absolute",
				top: "20px",
				left: "20px",
				right: "20px",
				minWidth: "992px",
				bottom: "20px"
			};
			$scope.glasspaneStyle = {};
			
			$scope.togglePointerEvents = {
				"pointer-events" : "none"
			};
			
			$scope.setPointerEvents = function (value){
			    $scope.togglePointerEvents["pointer-events"] = value;
			    $scope.$apply();
			}

			function redrawDecorators()
			{
				var selection = $editorService.getEditor().getSelection();
				if (selection && selection.length > 0) 
				{
					//redraw decorators, position may have changed
					// same issue as comment from adjustGlassPaneSize, we have to wait for render to be done
					$timeout(function(){
						$rootScope.$broadcast(EDITOR_EVENTS.RENDER_DECORATORS, selection);
					},200);
				}
			}
			
			$scope.setContentSize = function(width, height,fixedSize) {
				$scope.contentStyle.width = width;
				$scope.contentStyle.height = height;
				if (fixedSize) $scope.contentSizeFull = false;
				delete $scope.contentStyle.top;
				delete $scope.contentStyle.left;
				delete $scope.contentStyle.position;
				delete $scope.contentStyle.minWidth;
				delete $scope.contentStyle.bottom;
				delete $scope.contentStyle.right;
				delete $scope.contentStyle.h
				delete $scope.contentStyle.w
				adjustGlassPaneSize(width, height);
				if (fixedSize)
				{
					redrawDecorators()
				}	
			}
			$scope.setContentSizeFull = function(redraw) {
				$scope.contentStyle = {
					position: "absolute",
					top: "20px",
					left: "20px",
					right: "20px",
					minWidth: "992px",
					bottom: "20px"
				};
				$scope.contentSizeFull = true;
				delete $scope.contentStyle.width;
				delete $scope.contentStyle.height;
				delete $scope.contentStyle.h
				delete $scope.contentStyle.w
				adjustGlassPaneSize();
				if (redraw)
				{
					redrawDecorators()
				}	
			}
			$scope.getContentSize = function() {
				return {
					width: $scope.contentStyle.width,
					height: $scope.contentStyle.height
				};
			}
			$scope.isContentSizeFull = function() {
				return $scope.contentSizeFull;
			}

			$scope.setCursorStyle = function(cursor) {
				$scope.glasspane.style.cursor = cursor;
			}

			function equalGhosts(ghosts1, ghosts2) {
				if(ghosts1 && ghosts1.ghostContainers && ghosts2 && ghosts2.ghostContainers && (ghosts1.ghostContainers.length == ghosts2.ghostContainers.length)) {
					for (var i = 0; i < ghosts1.ghostContainers.length; i++) {
						if(ghosts1.ghostContainers[i].uuid != ghosts2.ghostContainers[i].uuid ||
							ghosts1.ghostContainers[i].ghosts.length != ghosts2.ghostContainers[i].ghosts.length) {
							return false;
						}
						for (var j = 0; j < ghosts1.ghostContainers[i].ghosts.length; j++) {
							if(ghosts1.ghostContainers[i].ghosts[j].uuid != ghosts2.ghostContainers[i].ghosts[j].uuid) {
								return false;
							}
						}
					}
					return true;
				}
				return false;
			}

			$scope.setGhosts = function(ghosts) {
				if(!equalGhosts($scope.ghosts, ghosts)) {
					$scope.ghosts = ghosts;
					$scope.ghostContainerElements = {}
	
					if ($scope.ghosts.ghostContainers) {
						for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
							for (j = 0; j < $scope.ghosts.ghostContainers[i].ghosts.length; j++) {
								if ($scope.ghosts.ghostContainers[i].ghosts[j].type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
									var changedGhosts = $scope.ghosts.ghostContainers[i].ghosts;
									$timeout(function() {
										$scope.rearrangeGhosts(changedGhosts)
									}, 0);
									break;
								}
							}
						}
					}
				}
			}

			function getScrollSizes(x) {
				var height = 0;
				var width = 0;
				for (var i = 0; i < x.length; i++) {
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
				return {
					height: height,
					width: width
				}
			}

			$scope.setContentSizes = function() {
				if ($scope.contentDocument) {
					var sizes = getScrollSizes($scope.contentDocument.querySelectorAll(".sfcontent"));
					if (sizes.height > 0 && sizes.width > 0) {
						var contentDiv = $element.find('.content-area')[0];
						if (contentDiv.clientHeight < sizes.height && (!$scope.contentStyle.h || $scope.contentStyle.h + 20 < sizes.height ||
								$scope.contentStyle.h - 20 > sizes.height)) {
							$scope.contentStyle.h = sizes.height
						}
						// not needed ?
//						if ($scope.isContentSizeFull()) {
//							if (contentDiv.clientWidth < sizes.width && (!$scope.contentStyle.w || $scope.contentStyle.w + 20 < sizes.width ||
//									$scope.contentStyle.w - 20 > sizes.width)) {
//								$scope.contentStyle.w = sizes.width
//								if (!$scope.isAbsoluteFormLayout()) {
//									$scope.contentStyle.width = (sizes.width + 20) + "px"
//								}
//							}
//						}
					}
					adjustGlassPaneSize();
				}
			}
			
			function adjustIFrameSize(){
		        	delete $scope.contentStyle.height;
		        	$element.find('.content')[0].style.height = "";
		        	$element.find('.content')[0].style.bottom = "20px";
		        	$element.find('.contentframe')[0].style.height = "100%";
		        	
		        	$scope.contentStyle.bottom = "20px";

		        	var h = editorContentRootScope.computeHeight();
		        	//if (h > $element.find('.content-area')[0].offsetHeight) {
		        	    delete $scope.contentStyle.bottom;
		        	    $scope.contentStyle.height = h;
		        	    $element.find('.content')[0].style.bottom = "";
		        	    $element.find('.content')[0].style.height = h + "px";
		        	    $element.find('.contentframe')[0].style.height = h + "px";
				//}
			}

			function adjustGlassPaneSize(gpWidth, gpHeight) {
				if ($scope.isAbsoluteFormLayout()) {
				    	var sizes = getScrollSizes($scope.contentDocument.querySelectorAll(".sfcontent"));
					if (sizes.height > 0 && sizes.width > 0) {
					    	var contentDiv = $element.find('.content-area')[0];
					    	var height = contentDiv.scrollHeight;
					    	var ghosts = $element.find('.ghostcontainer > .ghost');
					    	var width = contentDiv.scrollWidth;
					    	
					    	ghosts.each(function (index, ghost){
					    	    if (ghost.getBoundingClientRect().bottom > height)
					    		height = ghost.getBoundingClientRect().bottom;
					    	    if (ghost.getBoundingClientRect().right > width)
					    		width = ghost.getBoundingClientRect().right;
					    	});
					    	
						if (contentDiv.clientHeight < height) {
							$scope.glasspaneStyle.height = height + "px"; // 20 for the body ghost height
						} else {
							$scope.glasspaneStyle.height = '100%';
						}

						if (contentDiv.clientWidth < width) {
							$scope.glasspaneStyle.width = width + "px"; // 80 for the body ghost width
						} else
							$scope.glasspaneStyle.width = '100%';
					}
				} else {
					var contentDiv = $($scope.contentDocument).find('.svy-form')[0];
					if (contentDiv) {
					        if (contentDiv.offsetHeight > 0 ){
					            if ($scope.isContentSizeFull()) 
					            {	
					            	adjustIFrameSize();
					            }
					            
					            // we need to wait for the contentDiv to render with the values set for the content size
					            // now we just do a timeout, but should try a better way ...
					            $timeout(function() {
					            	var contentDivRendered = $($scope.contentDocument).find('.svy-form')[0];
	        						$scope.glasspaneStyle.width = (contentDivRendered.offsetWidth +20) + 'px';
	        						$scope.glasspaneStyle.height = (contentDivRendered.offsetHeight + 20) + 'px';
					            }, 200);
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
				//workaround to make the contextmenu show on osx
				editorContentRootScope.ctxmenu = $scope.ctxmenu;
				delete $scope.ctxmenu;
				servoyInternal = injector.get("$servoyInternal");
				$scope.glasspane.focus()
				$(function() {
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

						if (fixedKeyEvent.isCtrl) {
							var k = String.fromCharCode(fixedKeyEvent.keyCode).toLowerCase();
							if ('a' == k || 's' == k || (fixedKeyEvent.isShift && 'z' == k)) {
								if (fixedKeyEvent.isShift && ('s' == k || 'z' == k)) {
									// send the CTRL+SHIFT+S (save all) and CTRL+SHIFT+Z (open editor) key code to the server
									$editorService.keyPressed(objEvent);
								}
								return false;
							}
						}
						return true;
					});

					$(document).mousedown(function(objEvent) {
						$editorService.activated(objEvent);
						return true;
					});

					$element.find('.content-area').on("mousedown", function() {
						$scope.setContentSizes();
					});
				});


				var promise = $editorService.getGhostComponents({
					"resetPosition": true
				});
				promise.then(function(result) {
					$scope.setGhosts(result);
				});
				if (!$scope.editorInitialized) {
					$timeout(function() {
						if ($scope.isAbsoluteFormLayout()) {
							$scope.setContentSize(formWidth + "px", formHeight + "px");
						} else {
							$scope.setContentSizeFull();
						}
					}, 500);
				}

				var promise = $editorService.loadAllowedChildren();
				promise.then(function(result) {
					var allowedChildren = JSON.parse(result);
					$allowedChildren.set(allowedChildren);
					$scope.getEditorContentRootScope().allowedChildren = allowedChildren;
				});
				$scope.editorInitialized = true;
			});

			$element.on('renderGhosts.content', function(event) {
				var promise = $editorService.getGhostComponents(); //no parameter, then the ghosts are not repositioned
				promise.then(function(result) {
					$scope.setGhosts(result);
				});
			});

			$element.on('renderDecorators.content', function(event) {
				var toRun = function() {
					var shouldSetContentSizes = $scope.isAbsoluteFormLayout() || $scope.isContentSizeFull();
					if (selection.length > 0) {
						var ghost = $scope.getGhost(selection[0].getAttribute("svy-id"));
						if (shouldSetContentSizes && ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM)) {
							$scope.setContentSizes();
						} else {
							var promise = $editorService.getGhostComponents(); //no parameter, then the ghosts are not repositioned
							promise.then(function(result) {
								$scope.setGhosts(result);

								var nodes = Array.prototype.slice.call($scope.contentDocument.querySelectorAll("[svy-id]"));
								var ghosts = Array.prototype.slice.call($scope.glasspane.querySelectorAll("[svy-id]"));
								nodes = nodes.concat(ghosts);
								var matchedElements = []
								for (var s = 0; s < selection.length; s++) 
								{
									for (var i = 0; i < nodes.length; i++) {
										var element = nodes[i]
										if (selection[s].getAttribute("svy-id") == element.getAttribute("svy-id")) {
											matchedElements.push(element);
											break;
										}
									}
								}
								selection = matchedElements;
								if (selection.length != matchedElements.length) {
									$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, selection);
								} else {
									$rootScope.$broadcast(EDITOR_EVENTS.RENDER_DECORATORS, selection);
								}

							});
						}
					} else if(shouldSetContentSizes){
					    	$scope.setContentSizes();
					}
				}
				
				// run all this inside the angular digest loop - as it can change things that are watched
				if ($scope.$$phase) toRun(); // are we sure if it is a digest on a smaller nested scope that the right watches will run or should we always evalAsync on the editor scope ($scope) or even root scope?
				else $scope.$evalAsync(toRun);
			});

			$element.on('updateForm.content', function(event, formInfo) {
				if ($scope.isAbsoluteFormLayout()) {
					var ghost = $scope.getGhost(formInfo.uuid);
					if (ghost && (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) && (ghost.size.width != formInfo.w || ghost.size.height != formInfo.h)) {
						$rootScope.$apply(function() {
							$scope.setContentSize(formInfo.w + "px", formInfo.h + "px");
							ghost.size.width = formInfo.w;
							ghost.size.height = formInfo.h;

							if (selection.length > 0 && selection[0].getAttribute("svy-id") == formInfo.uuid) {
								$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, selection)
							}
						});
					}
				}
			});

			$editorService.registerEditor($scope);
			$editorService.connect().then(function() {
				var replacews = $webSocket.getURLParameter("replacewebsocket") ? "&replacewebsocket=true" : "";
				$scope.contentframe = "content/editor-content.html?id=%23" + $element.attr("id") + "&sessionid=" + $webSocket.getURLParameter(
						"c_sessionid") + "&windowname=" + formName + "&f=" + formName + "&s=" + $webSocket.getURLParameter("s") +
					replacews;
			})
			//init ghosts
			var promise = $editorService.getGhostComponents();
			promise.then(function(result) {
				$scope.setGhosts(result);
			});
			
			function isAllGhostContainersVisible() {
				if ($scope.ghosts.ghostContainers) {
					for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
						if(!$scope.ghosts.ghostContainers[i].style  || $scope.ghosts.ghostContainers[i].style.display !== "block") {
							return false;
						}	
					}
				}
				
				return true;
			}
			
			var windowHeight = 0;
			var windowWidth = 0;
			$($window).resize(function() {
				if (isAllGhostContainersVisible() && ($($window).height() != windowHeight || $($window).width() != windowWidth)) {
					windowHeight = $($window).height();
					windowWidth =  $($window).width();
					$element.trigger('renderDecorators.content');
				}
			});			
			
		},
		templateUrl: 'templates/editor.html',
		replace: true
	};

}).factory("$editorService", function($rootScope, $webSocket, $log, $q, $window, EDITOR_EVENTS, $rootScope, $timeout,
	$selectionUtils, $allowedChildren) {
	var realConsole = $window.console;
	$window.console = {
		log: function(msg) {
			if (typeof(consoleLog) != "undefined") {
				consoleLog("log", msg)
			} else if (realConsole) {
				realConsole.log(msg)
			} else alert(msg);

		},
		error: function(msg) {
			if (typeof(consoleLog) != "undefined") {
				consoleLog("error", msg)
			} else if (realConsole) {
				realConsole.error(msg)
			} else alert(msg);
		}
	}
	var wsSession = null;
	var connected = false;
	var deferred = null;

	function testWebsocket() {
		if (typeof(WebSocket) == 'undefined' || $webSocket.getURLParameter("replacewebsocket")) {
			if (typeof(SwtWebsocketBrowserFunction) != 'undefined') {
				WebSocket = SwtWebSocket
				var $currentSwtWebsockets = [];

				$window.addWebSocket = function(socket) {
					var id = $currentSwtWebsockets.length;
					$currentSwtWebsockets[id] = socket;
					return id;
				}

				function SwtWebSocket(url) {
					var me = this;
					me.id = $currentSwtWebsockets.length;
					$currentSwtWebsockets[me.id] = me;
					setTimeout(function() {
						SwtWebsocketBrowserFunction('open', url, me.id)
						me.onopen()
					}, 0);
				}

				SwtWebSocket.prototype.send = function(str) {
					SwtWebsocketBrowserFunction('send', str, this.id)
				}

				function SwtWebsocketClient(command, arg1, arg2, id) {
					if (command == 'receive') {
						$currentSwtWebsockets[id].onmessage({
							data: arg1
						})
					} else if (command == 'close') {
						$currentSwtWebsockets[parseInt(id)].onclose({
							code: arg1,
							reason: arg2
						})
						$currentSwtWebsockets[parseInt(id)] = null;
					} else if (command == 'error') {
						$currentSwtWebsockets[parseInt(id)].onerror(arg1)
					}
				}
				$window.SwtWebsocketClient = SwtWebsocketClient;
			} else {
				$timeout(testWebsocket, 100);
				return;
			}
		}
		wsSession = $webSocket.connect('', [$webSocket.getURLParameter('editorid')])
		wsSession.onopen(function() {
			connected = true;
			if (deferred) deferred.resolve();
			deferred = null;
			$rootScope.$broadcast(EDITOR_EVENTS.INITIALIZED)
		});
	}

	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		var sel = []
		for (var i = 0; i < selection.length; i++) {
			sel[sel.length] = selection[i].getAttribute("svy-id");
		}
		wsSession.callService('formeditor', 'setSelection', {
			selection: sel
		}, true)
	})
	var editorScope; //todo this should become a array if we want to support multiply editors on 1 html page.
	var inlineEdit;
	return {
		registerEditor: function(scope) {
			editorScope = scope;
		},
		getEditor: function() {
			return editorScope;
		},
		connect: function() {
			if (deferred) return deferred.promise;
			deferred = $q.defer();
			var promise = deferred.promise;
			if (!connected) testWebsocket();
			else {
				deferred.resolve();
				deferred = null;
			}
			return promise;
		},

		activated: function() {
			return wsSession.callService('formeditor', 'activated')
		},

		keyPressed: function(event) {
			wsSession.callService('formeditor', 'keyPressed', {
				ctrl: event.ctrlKey,
				shift: event.shiftKey,
				alt: event.altKey,
				keyCode: event.keyCode
			}, true)
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
			return wsSession.callService('formeditor', 'getBooleanState', {
				"isInheritedForm": true
			}, false)
		},

		isShowData: function() {
			return wsSession.callService('formeditor', 'getBooleanState', {
				"showData": true
			}, false)
		},

		isShowWireframe: function() {
			return wsSession.callService('formeditor', 'getBooleanState', {
				"showWireframe": true
			}, false)
		},

		toggleShowWireframe: function() {
			return wsSession.callService('formeditor', 'toggleShowWireframe', null, false)
		},

		createComponents: function(components) {
			wsSession.callService('formeditor', 'createComponents', components, true)
		},

		openElementWizard: function(elementType) {
			wsSession.callService('formeditor', 'openElementWizard', {
				elementType: elementType
			}, true)
		},

		updateFieldPositioner: function(location) {
			wsSession.callService('formeditor', 'updateFieldPositioner', {
				location: location
			}, true)
		},

		executeAction: function(action, params) {
			wsSession.callService('formeditor', action, params, true)
		},

		sameSize: function(width) {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var firstSize = null;
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModel(node);
					if (beanModel) {
						if (firstSize == null) {
							firstSize = beanModel.size;
						} else {
							var newSize;
							if (width) {
								newSize = {
									width: firstSize.width,
									height: beanModel.size.height
								};
							} else {
								newSize = {
									width: beanModel.size.width,
									height: firstSize.height
								};
							}
							obj[node.getAttribute("svy-id")] = newSize;
						}
					}
				}
				this.sendChanges(obj);
			}
		},

		updateSelection: function(ids) {
			if (editorScope.updateSel) $timeout.cancel(editorScope.updateSel);
			editorScope.updateSel = $timeout(function() {
				var prevSelection = editorScope.getSelection();
				var changed = false;
				var selection = [];
				if (ids && ids.length > 0) {
					var nodes = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[svy-id]"));
					var ghosts = Array.prototype.slice.call(editorScope.glasspane.querySelectorAll("[svy-id]"));
					nodes = nodes.concat(ghosts);
					for (var i = 0; i < nodes.length; i++) {
						var id = nodes[i].getAttribute("svy-id");
						if (ids.indexOf(id) != -1) {
							selection.push(nodes[i]);
							changed = changed || prevSelection.indexOf(nodes[i]) == -1;
							if (selection.length == ids.length) break;
						}
					}
				} else if (prevSelection.length > 0) {
					changed = true;
				}
				if (changed) editorScope.setSelection(selection);
			}, 400);
		},

		openContainedForm: function(ghost) {
			wsSession.callService('formeditor', 'openContainedForm', {
				"uuid": ghost.uuid
			}, true)
		},

		setInlineEditMode: function(edit) {
			inlineEdit = edit
			wsSession.callService('formeditor', 'setInlineEditMode', {
				"inlineEdit": inlineEdit
			}, true)
		},

		isInlineEditMode: function(){
			return inlineEdit;
		},
		getComponentPropertyWithTags: function(svyId, propertyName) {
			return wsSession.callService('formeditor', 'getComponentPropertyWithTags', {
				"svyId": svyId,
				"propertyName": propertyName
			}, false);
		},

		getShortcuts: function() {
			return wsSession.callService('formeditor', 'getShortcuts');
		},		

		toggleHighlight: function() {
			if (!editorScope.getEditorContentRootScope().design_highlight)
				editorScope.getEditorContentRootScope().design_highlight = "highlight_element";
			else editorScope.getEditorContentRootScope().design_highlight = null;

			editorScope.getEditorContentRootScope().$apply();
		},

		toggleShowData: function() {
			wsSession.callService('formeditor', 'toggleShowData', null, true);
		},

		updatePaletteOrder: function(paletteOrder) {
			return wsSession.callService('formeditor', 'updatePaletteOrder', paletteOrder, false);
		},

		openPackageManager: function() {
			return wsSession.callService('formeditor', 'openPackageManager', null, true);
		},
		
		showImageInOverlayDiv: function(url) {
			editorScope.previewOverlayImgURL = url;
			editorScope.displayOverlay = true;
		},
		
		loadAllowedChildren: function()
		{
			return wsSession.callService('formeditor', 'getAllowedChildren');
		},
		
		setStatusBarText: function(text) {
			var statusBarDiv = angular.element(document.querySelector('#statusbar'))[0];
			statusBarDiv.innerHTML = !text ? '&nbsp;' : text;
		}

		// add more service methods here
	}
}).factory("loadingIndicator", function() {
	//the loading indicator should not be shown in the editor
	return {
		showLoading: function() {},
		hideLoading: function() {}
	}
});
