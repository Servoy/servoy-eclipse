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
			return allowedChildren[layoutName ? layoutName : "topContainer"];
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
	HIDE_DECORATORS: "HIDE_DECORATORS",
	RENDER_PALETTE: "RENDER_PALETTE",
	ADJUST_SIZE: "ADJUST_SIZE"
}).value("EDITOR_CONSTANTS", {
	PART_LABEL_WIDTH: 100,
	PART_LABEL_HEIGHT: 22,
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
            var markDirtyTimeoutRef;
            var markDirtyTimeoutMs = 1;
			var delta = {
				addedNodes: [],
				removedNodes: []
			}
			var selection = [];
			
			var selectedConfigGhosts = [];

			function markDirty() {
				if (markDirtyTimeoutRef) {
					clearTimeout(markDirtyTimeoutRef);
				}
                markDirtyTimeoutMs = 1;
                markDirtyTimeoutRef = $timeout(executeMarkDirtyLater, markDirtyTimeoutMs);
			}
			
			function executeMarkDirtyLater() {
                if (markDirtyTimeoutMs < 250 && !editorContentRootScope.getDesignFormControllerScope) {
                    // postpone until editorContentRootScope gets initialized
                    markDirtyTimeoutRef = $timeout(executeMarkDirtyLater, markDirtyTimeoutMs);
                    markDirtyTimeoutMs += 50;
                } else {
                    //Reference to editor should be gotten from Editor instance somehow
                    //instance.fire(Editor.EVENT_TYPES.SELECTION_CHANGED, delta)
                    $rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, selection)
                    delta.addedNodes.length = delta.removedNodes.length = 0
                    
                    markDirtyTimeoutRef = null;
                }
            }

			 $document.bind('selectstart', function(event) {
				 if (!$editorService.isInlineEditMode())
				 {
					 return false
				 }	 
		      })
							    
			var formName = $webSocket.getURLParameter("f");
			var formLayout = $webSocket.getURLParameter("l");
			var hideDefault = $webSocket.getURLParameter("hd");
			var marqueeSelectOuter = $webSocket.getURLParameter("mso");
			var cssPosition = $webSocket.getURLParameter("p");
			var formWidth = parseInt($webSocket.getURLParameter("w"), 10);
			var formHeight = parseInt($webSocket.getURLParameter("h"), 10);
			var formComponent = $webSocket.getURLParameter("fc");
			var editorContentRootScope = null;
			var servoyInternal = null;
			var fieldLocation = null;

			$scope.contentWindow = $element.find('.contentframe')[0].contentWindow;
			$scope.glasspane = $element.find('.contentframe-overlay')[0];
			$scope.contentArea = $element.find('.content-area')[0];
			$scope.editorID = $element.attr('id');
			$scope.contentDocument = null;
			$scope.contentSizeFull = true;
			
			$scope.startAutoScroll = function (direction, callback) {
				var autoScrollPixelSpeed = 2;
				return $interval(function () {
				    var contentArea = $scope.contentArea;
				    var changeX = 0;
				    var changeY = 0;
				    switch(direction) {
				    	case "BOTTOM_AUTOSCROLL":
							if ((contentArea.scrollTop + contentArea.offsetHeight) === contentArea.scrollHeight)
								angular.element($scope.glasspane).height(angular.element($scope.glasspane).height() + autoScrollPixelSpeed);
							contentArea.scrollTop += autoScrollPixelSpeed;
							changeY = autoScrollPixelSpeed;
							break;
				    	case "RIGHT_AUTOSCROLL":
							if ((contentArea.scrollLeft + contentArea.offsetWidth) === contentArea.scrollWidth)
								angular.element($scope.glasspane).width(angular.element($scope.glasspane).width() + autoScrollPixelSpeed);
				    		contentArea.scrollLeft += autoScrollPixelSpeed;
				    		changeX = autoScrollPixelSpeed;
				    		break;
				    	case "LEFT_AUTOSCROLL":
				    		if(contentArea.scrollLeft >= autoScrollPixelSpeed) {
				    			contentArea.scrollLeft -= autoScrollPixelSpeed;
				    			changeX = -autoScrollPixelSpeed;
				    		} else {
				    			changeX = -contentArea.scrollLeft;
				    			contentArea.scrollLeft = 0;
				    		}
				    		break;
				    	case "TOP_AUTOSCROLL":
				    		if(contentArea.scrollTop >= autoScrollPixelSpeed) {
				    			contentArea.scrollTop -= autoScrollPixelSpeed;
				    			changeY = -autoScrollPixelSpeed;
				    		} else {
				    			changeY = -contentArea.scrollTop;
				    			contentArea.scrollTop -= 0;
				    		}
				    		break;
				    }
				    
				    if (autoScrollPixelSpeed < 15) autoScrollPixelSpeed++;
				    
				    if (callback) callback($scope, $scope.selectionToDrag, changeX, changeY, 0, 0);
				    $scope.refreshEditorContent();
				}, 50);
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
			
			$scope.getAutoscrollElementClientBounds = function() {
				var bottomAutoscrollArea = $element.find('.bottomAutoscrollArea')[0];
				
				var autoscrollElementClientBounds;
				if (bottomAutoscrollArea) {
					autoscrollElementClientBounds = [];
					autoscrollElementClientBounds[0] = bottomAutoscrollArea.getBoundingClientRect();
					autoscrollElementClientBounds[1] = $element.find('.rightAutoscrollArea')[0].getBoundingClientRect();
					autoscrollElementClientBounds[2] = $element.find('.leftAutoscrollArea')[0].getBoundingClientRect();
					autoscrollElementClientBounds[3] = $element.find('.topAutoscrollArea')[0].getBoundingClientRect();
				}
				return autoscrollElementClientBounds; // else it's probably a responsive form
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

			// returns the ghost object with the specified uuid
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
			
			// returns the ghost container object that contains the given ghost
			$scope.getGhostContainerOf = function(ghost) {
				if ($scope.ghosts) {
					for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
						for (j = 0; j < $scope.ghosts.ghostContainers[i].ghosts.length; j++) {
							if ($scope.ghosts.ghostContainers[i].ghosts[j] == ghost)
								return $scope.ghosts.ghostContainers[i];
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

				if (!$scope.testElementTimeouts) $scope.testElementTimeouts = {};
				if (parent == undefined) {
					var defer = $q.defer();
					function testElement() {
						var p = $('.contentframe').contents().find('[svy-id="' + uuid + '"]');
						if (p[0] != undefined) {
							parent = p[0];
							$scope.ghostContainerElements[uuid] = parent;
							defer.resolve(parent);
							delete $scope.testElementTimeouts[uuid];
						} else if (isGhostContainer(uuid)) {
							$scope.testElementTimeouts[uuid] = $timeout(testElement, 400);
						}
                        else{
                            defer.reject();
                        }
					}
					if ($scope.testElementTimeouts[uuid] == undefined) {
						$scope.testElementTimeouts[uuid] = $timeout(testElement, 400);
					}
					$scope.ghostContainerElements[uuid] = defer.promise;
					return defer.promise;
				}
				return parent;
			}
			
			function isGhostContainer(uuid) {
				if ($scope.ghosts.ghostContainers) {
						for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
							if 	($scope.ghosts.ghostContainers[i].uuid === uuid)
							 return true;						
						}
					}
				return false;
			}

			var realContainerPromise = {};
			$scope.getGhostContainerStyle = function(ghostContainer) {
				function showAndPositionGhostContainer(ghostContainer, parentCompBounds) {
					if (!ghostContainer.style) ghostContainer.style = {};
					
					// add semi-transparent background with alternating color in case there are multiple ghost containers on the same component (multiple droppable properties on the same comp)
					var odd = (ghostContainer.containerPositionInComp % 2);
					ghostContainer.style['background-color'] = odd ? "rgba(150, 150, 150, 0.05)" : "rgba(0, 100, 80, 0.05)";
					ghostContainer.style['color'] = odd ? "rgb(150, 150, 150)" : "rgb(0, 100, 80)";
					if (odd) {
						ghostContainer.style['border-top'] = ghostContainer.style['border-bottom'] = 'dashed 1px';
					}
					ghostContainer.style.visibility = "visible";
					
					var spaceForEachContainer = 62 /*(basicWebComponent.getSize().height / totalGhostContainersOfComp)*/;
					var emptySpaceTopBeforGhosts = 0; // see if we have room to add some extra pixels to top location - it shows nicer on large components when space is available
					if (parentCompBounds.height > ghostContainer.totalGhostContainersOfComp * spaceForEachContainer + 30) emptySpaceTopBeforGhosts = 30;
					
					// the 20 extra pixels on location left/top are to compensate for content area padding
					ghostContainer.style.left = (parentCompBounds.left + 20) + "px";
					ghostContainer.style.top = (parentCompBounds.top + ghostContainer.containerPositionInComp * spaceForEachContainer + emptySpaceTopBeforGhosts + 20) + "px";
					ghostContainer.style.width = parentCompBounds.width + "px";
					ghostContainer.style.height = spaceForEachContainer + "px";
				}
				
				if (!$scope.isAbsoluteFormLayout()) {
					if (realContainerPromise[ghostContainer.uuid] == null) {
						var p = getRealContainerElement(ghostContainer.uuid);
						if (p.then) {
							realContainerPromise[ghostContainer.uuid] = p;
							p.then(function(parent) {
								delete realContainerPromise[ghostContainer.uuid];
								showAndPositionGhostContainer(ghostContainer, parent.getBoundingClientRect());
								ghostContainer.style.display = "block";
							}, function() {
								delete realContainerPromise[ghostContainer.uuid];
								ghostContainer.style.display = "none";
							});
						} else {
							showAndPositionGhostContainer(ghostContainer, p.getBoundingClientRect());
							if (p?.parentElement.classList.contains('maxLevelDesign')) {
								ghostContainer.style.display = "none";
							} else {
								ghostContainer.style.display = "block";
							}	
						}
					}
				} else {
					if (typeof ghostContainer.containerPositionInComp != "undefined") {
						if (!ghostContainer.style) {
							showAndPositionGhostContainer(ghostContainer, ghostContainer.parentCompBounds);
						}
					} else {
						//TODO refactor out this 20px addition
						return {
							display: "block",
							left: "20px",
							top: "20px",
							width: $scope.contentStyle.width,
							height: $scope.contentStyle.height
						};
					}
					if (ghostContainer.style) ghostContainer.style.display = "block";
				}
				return ghostContainer.style;
			}

			$scope.getGhostStyle = function(ghost, container) {
				var style;
				
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // parts
					var filterGhostParts = container.ghosts.filter(ghost => ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART);
					var onlyBodyPart =  filterGhostParts.length === 1 && filterGhostParts[0].text.toLowerCase() === "body";
					style = {
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
					if (onlyBodyPart) {
						style['visibility'] = "hidden";
					}
				} else if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) { // the form
					style = {
						left: 0,
						top: 0,
						width: ghost.size.width + "px",
						height: ghost.size.height + "px",
						padding: "3px"
					};
				} else {
					var xOffset = 20;
					var yOffset = 20;
					if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_COMPONENT) {
						// show outside component at exact location
						xOffset = 0;
						yOffset = 0;
					}
					// SOME of these are set directly in editor.css for .ghost-dnd-placeholder; if you change things that should affect
					// the placeholder used when moving droppable config custom objects via drag&drop, please change them in editor.css as well
					style = {
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
					else if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION)
					{
						style.background = "#ffbb37";
					}
					else if (ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
					{
						style.background = "#e4844a";
					}
				}
				
				if (ghost.selected) {
					// else, when "selected" is set change colors so that the user knows it's selected
					style.background = "#07f";
					style.color = "#fff";
				}
				return style;
			}

			$scope.getGhostHRStyle = function(ghost) {
				if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) { // parts
					var hrStyle = {
							marginTop: "-4px",
							border: 0,
							borderTop: "1px dashed #000",
							borderBottom: "5px dashed transparent",
							width: (parseInt($scope.contentStyle.width, 10) + EDITOR_CONSTANTS.PART_LABEL_WIDTH - 15) + "px",
							float: "right"
						};
					if (ghost == $scope.getLastPartGhost())
					{
						hrStyle.borderBottom = "3px dashed transparent";
					}	
					return hrStyle;
				} else {
					return {
						display: "none"
					};
				}
			}
			
			$scope.openContainedForm = function(ghost) {
				if (ghost.type != EDITOR_CONSTANTS.GHOST_TYPE_PART) {
					$editorService.openContainedForm(ghost);
				}
			}

			$scope.getBeanModel = function(node) {
				if (node && editorContentRootScope.getDesignFormControllerScope) {
					var name = node.getAttribute("svy-id");
					if (name && editorContentRootScope.getDesignFormControllerScope) return editorContentRootScope.getDesignFormControllerScope().model(name, true);
				}
				return null;
			}

			$scope.getBeanModelOrGhost = function(node) {
				if (node && editorContentRootScope.getDesignFormControllerScope) {
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
					if(ghost.size.height < 1) ghost.size.height = 1;
					ghost.size.width = ghost.size.width + deltaWidth;
					if(ghost.size.width < 1) ghost.size.width = 1;
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

			function updateGhostsAccordingToSelection(selection) {
				// remove selection class from all previously selected ghost elements
				for (var i in selectedConfigGhosts) {
					delete selectedConfigGhosts[i].selected;
				}
				selectedConfigGhosts.length = 0;
				
				for (var i = 0; i < selection.length; i++) {
					var svyId = selection[i].getAttribute("svy-id");
					var ghost = $scope.getGhost(svyId);
					if (ghost) {
						if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) $scope.updateGhostLocationLimits(ghost)
						if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION || ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_PART) {
							selectedConfigGhosts.push(ghost);
							ghost.selected = true;
						}
					}
				}
			}

			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				updateGhostsAccordingToSelection(selection);
			})
			
			$rootScope.$on(EDITOR_EVENTS.INITIALIZED, function() {
				$editorService.requestSelection();
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
				var keyCode, isCtrl, isShift, isAlt, isMeta;

				if (window.event) { //IE
					keyCode = window.event.keyCode;
					isCtrl = window.event.ctrlKey ? true : false;
					isShift = window.event.shiftKey ? true : false;
					isAlt = window.event.altKey ? true : false;
					isMeta = window.event.metaKey ? true : false;
				} else { // firefox
					keyCode = event.which;
					isCtrl = event.ctrlKey ? true : false;
					isShift = event.shiftKey ? true : false;
					isAlt = event.altKey ? true : false;
					isMeta = event.metaKey ? true : false;
				}

				return {
					keyCode: keyCode,
					isCtrl: isCtrl,
					isShift: isShift,
					isAlt: isAlt,
					isMeta: isMeta
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
				return (formLayout == "absolute" || formLayout == "csspos");
			}
			
			$scope.isCSSPositionFormLayout = function() {
				return formLayout == "csspos";
			}
			$scope.hideDefault = function() {
				return hideDefault == "true";
			}
			
			$scope.isFormComponent = function() {
				return formComponent === "true";
			}
			
			$scope.isMarqueeSelectOuter = function() {
				return marqueeSelectOuter === "true";
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

			$scope.redrawDecorators = function()
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
			
			$scope.setMainContainerSize = function() {
				var maincontainer = $($scope.contentDocument).find('*[data-maincontainer="true"]');
				if(maincontainer) {
					maincontainer.css('min-height', $element.find('.contentframe').css('min-height'));
					maincontainer.css('min-width', $element.find('.contentframe').css('min-width'));
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
				// we need to apply the changes to dom earlier in order to adjust the to the new size
				$element.find('.content')[0].style.width = width + "px";
				$element.find('.content')[0].style.right = "";
				$element.find('.content')[0].style.minWidth = "";
				if (!$scope.isAbsoluteFormLayout()) {
					$($scope.contentDocument).find('.svy-form').css('width', width);
					$($scope.contentDocument).find('.svy-form').css('height', height);
					$scope.setMainContainerSize();
					$element.find('.content')[0].style.height = height;
		       		$element.find('.contentframe')[0].style.height = height;
				}
				$scope.adjustGlassPaneSize(width, height);
				$scope.redrawDecorators();
			}
			
			var initialWidth;
			$scope.getFormInitialWidth = function() {
				if (!initialWidth)
				{
					initialWidth = $element.find('.content')[0].getBoundingClientRect().width + "px";
				}
				return initialWidth;
			}
			
			$scope.setContentSizeFull = function(redraw) {
				$scope.contentStyle = {
					position: "absolute",
					top: "20px",
					left: "20px",
					right: "20px",
					bottom: "20px"
				};
				$scope.contentSizeFull = true;
				delete $scope.contentStyle.width;
				delete $scope.contentStyle.height;
				delete $scope.contentStyle.h
				delete $scope.contentStyle.w
				$($scope.contentDocument).find('.svy-form').css('height', '');
				$($scope.contentDocument).find('.svy-form').css('width', '');
				$scope.adjustGlassPaneSize();
				if (redraw)
				{
					$scope.redrawDecorators()
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
				if (ghosts1 && ghosts1.ghostContainers && ghosts2 && ghosts2.ghostContainers && (ghosts1.ghostContainers.length == ghosts2.ghostContainers.length)) {
					for (var i = 0; i < ghosts1.ghostContainers.length; i++) {
						if (ghosts1.ghostContainers[i].uuid != ghosts2.ghostContainers[i].uuid ||
							ghosts1.ghostContainers[i].ghosts.length != ghosts2.ghostContainers[i].ghosts.length) {
							return false;
						}
						for (var j = 0; j < ghosts1.ghostContainers[i].ghosts.length; j++) {
							if (ghosts1.ghostContainers[i].ghosts[j].uuid != ghosts2.ghostContainers[i].ghosts[j].uuid || ghosts1.ghostContainers[i].ghosts[j].text != ghosts2.ghostContainers[i].ghosts[j].text) {
								return false;
							}
						}
					}
					return true;
				}
				return false;
			}

			function rearrangeGhosts(ghosts, attemptNo) {
				// timeout is needed here because new ghost content needs to get rendered after angular digest cycle updates their inline style
				$timeout(function() {
					var tryAgain = false;
					var overflow = 0;
					for (var i = 0; i < ghosts.length; i++) {
						var ghost = ghosts[i];
						var prevGhost = i > 0 ? ghosts[i - 1] : undefined;
						if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
							// note: initially, all config ghosts sent from server have a default width of 80px
							// allow longer content in ghosts
							var jqElement = $('[svy-id="' + ghost.uuid + '"]');
							if (jqElement.length > 0) {
								var width = Math.min(jqElement[0].scrollWidth, 200); // just do limit it to some high enough value (2.5 x default)
								if (prevGhost != undefined && ghost.location.y == prevGhost.location.y) {
									ghost.location.x += overflow;
								}
								if (width > ghost.size.width) {
									overflow += width - ghost.size.width;
									ghost.size.width = width;
								} else if (width <= 0) tryAgain = true; // not rendered yet?
							} else tryAgain = true; // not rendered yet?
						}
					}
					
					// as responsive draws it's ghosts much later then anchored/absolute, we try to wait until we get valid values for ghost element widths from browser
					// we give up after 5 sec just in case the ghost is not appearing at all for some reason; we don't want to execute timeouts forever
					if (tryAgain && attemptNo < 51) rearrangeGhosts(ghosts, attemptNo + 1);
				}, attemptNo == 1 ? 0 : 100);
			}

			$scope.setGhosts = function(ghosts) {
				if (!equalGhosts($scope.ghosts, ghosts)) {
					flushGhostContainers();
					$scope.ghosts = ghosts;
	
					if ($scope.ghosts.ghostContainers) {
						for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
							for (j = 0; j < $scope.ghosts.ghostContainers[i].ghosts.length; j++) {
								if ($scope.ghosts.ghostContainers[i].ghosts[j].type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
									rearrangeGhosts($scope.ghosts.ghostContainers[i].ghosts, 1);
									break;
								}
							}
						}
					}
					
					var hideInheritedPromise = $editorService.isHideInherited();
					hideInheritedPromise.then(function(result) {
						$editorService.hideInheritedElements(result);
					});
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
					
					 $scope.$evalAsync( function() {  
					 	if (!$scope.isAbsoluteFormLayout() && $($scope.contentDocument).find('.svy-form')[0] && $($scope.contentDocument).find('.svy-form')[0].offsetHeight < ($scope.contentArea.clientHeight - 40))
						{ 
							$scope.getEditorContentRootScope().sfcontentStyle = {'height': $scope.contentArea.clientHeight - 40+'px'};
						} 
						$scope.adjustGlassPaneSize();
					});
					
				}
			}
			
			$scope.adjustIFrameSize = function(){
		        	delete $scope.contentStyle.height;
		        	if (!$scope.isAbsoluteFormLayout()) {
						$scope.glasspaneStyle['min-height'] = $scope.contentArea.clientHeight-20+'px';
			        	$element.find('.content')[0].style['min-height'] = $scope.contentArea.clientHeight-40+'px';
			        	$element.find('.content')[0].style.height = "";
			        }
			        else
			       	{
		        		$element.find('.content')[0].style.bottom = "20px";
		        	}
		        	$element.find('.contentframe')[0].style.height = "100%";
		        	
		        	$scope.contentStyle.bottom = "20px";

		        	var h = editorContentRootScope.computeHeight();
		        	//if (h > $element.find('.content-area')[0].offsetHeight) {
		        	    delete $scope.contentStyle.bottom;
		        	    $scope.contentStyle.height = h;
		        	    if (!$scope.isAbsoluteFormLayout()) {
			        	    $element.find('.content')[0].style['min-height'] = $scope.contentArea.clientHeight-40+'px';
			        	    $element.find('.contentframe')[0].style['min-height'] = $scope.contentArea.clientHeight-40+'px';
			        	}
			        	else
			       		{
		        	    	$element.find('.content')[0].style.bottom = "";
		        	    }
		        	  	$element.find('.content')[0].style.height = h + "px";
		        	    $element.find('.contentframe')[0].style.height = h + "px";
				//}
			}

			$scope.adjustGlassPaneSize = function(gpWidth, gpHeight) {
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
							$scope.glasspaneStyle['min-height'] = $scope.contentArea.clientHeight-40+'px';
							$scope.glasspaneStyle.height = height + "px";// 20 for the body ghost height
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
					            	$scope.adjustIFrameSize();
					            }
					            
					            // we need to wait for the contentDiv to render with the values set for the content size
					            // now we just do a timeout, but should try a better way ...
					            $timeout(function() {
					            	var contentDivRendered = $($scope.contentDocument).find('.svy-form')[0];
	        						$scope.glasspaneStyle.width = (contentDivRendered.offsetWidth +20) + 'px';
	        						$scope.glasspaneStyle.height = (contentDivRendered.offsetHeight + 20) + 'px';
					            }, 200);
					        }
							else {
								var maincontainer =  $($scope.contentDocument).find('*[data-maincontainer="true"]')[0];
								if (maincontainer) {
									$scope.glasspaneStyle['min-height'] = (maincontainer.offsetHeight + 20) + 'px';
									$scope.glasspaneStyle.width = (maincontainer.offsetWidth +20) + 'px';
									$element.find('.content')[0].style['min-height'] = maincontainer.offsetHeight+'px';
									$element.find('.contentframe')[0].style.height = maincontainer.offsetHeight+'px';
								}
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
				var deferred = $q.defer();
				$scope.contentLoaded = deferred.promise;
				editorContentRootScope.$on(EDITOR_EVENTS.ADJUST_SIZE, function() {
					$editorService.adjustSizes();
					deferred.resolve();
				})
				//call set content sizes 
				//in the less likely situation in which we missed the CONTENT_LOADED event
				//because the editorContentRootScope was not set yet
				editorContentRootScope.$evalAsync(function() { $editorService.setContentSizes()});
				
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
							$editorService.keyPressed(objEvent);
							// send the DELETE key code to the server
							return false;
						} 
						// f4 open form hierarchy
						if (fixedKeyEvent.keyCode == 115) {
							// send the F4 key code to the server
							$editorService.keyPressed(objEvent);
							return false;
						}
						// refresh
						if (fixedKeyEvent.keyCode == 116) {
							// send the F5 key code to the server
							$editorService.keyPressed(objEvent);
							return false;
						}
						return true;
					});
					$(document).keydown(function(objEvent) {
						var fixedKeyEvent = $scope.getFixedKeyEvent(objEvent);
						
						if ( objEvent.target.id != 'directEdit' && (fixedKeyEvent.isCtrl || fixedKeyEvent.isMeta || fixedKeyEvent.isAlt)) {
							$editorService.keyPressed(objEvent);
							return false;
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

				var fontPromise = $editorService.getSystemFont();
				fontPromise.then(function(result){
				    $document[0].body.style.fontFamily = result.font;
				    $document[0].body.style.fontWeight = 400;
					$document[0].body.style.fontSize = result.size+'px';
					$document[0].body.style.lineHeight = '18px';
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

			$element.on('flushGhostContainerElements.content', function(event) {
				flushGhostContainers();
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
									var selElemSvyId = selection[s].getAttribute("svy-id");
									for (var i = 0; i < nodes.length; i++) {
										var element = nodes[i]
										if (selElemSvyId == element.getAttribute("svy-id")) {
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
									if (selectedConfigGhosts.length > 0) updateGhostsAccordingToSelection(selection); // if config ghosts were reordered (for ex. extra table columns), after drop, the DOM elements for the ghost identified by ID changed - painting the selection needs to change as well
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
				var containerID =  $webSocket.getURLParameter("cont") ? ("&cont="+$webSocket.getURLParameter("cont")) : "";
				$scope.contentframe = "content/editor-content.html?id=%23" + $element.attr("id") + "&clientnr=" + $webSocket.getURLParameter(
						"c_clientnr") + "&windowname=" + formName + "&f=" + formName + "&s=" + $webSocket.getURLParameter("s") +
					 replacews + containerID;
			})
			
			function areAllGhostContainersVisible() {
				if ($scope.ghosts && $scope.ghosts.ghostContainers) {
					for (i = 0; i < $scope.ghosts.ghostContainers.length; i++) {
						if (!$scope.ghosts.ghostContainers[i].style || $scope.ghosts.ghostContainers[i].style.display !== "block") {
							return false;
						}	
					}
				}
				
				return true;
			}
			
			var windowHeight = 0;
			var windowWidth = 0;
			$($window).resize(function() {
				if (areAllGhostContainersVisible() && ($($window).height() != windowHeight || $($window).width() != windowWidth)) {
					windowHeight = $($window).height();
					windowWidth =  $($window).width();
					$element.trigger('renderDecorators.content');
				}
			});		
			
			function flushGhostContainers() {
				$scope.ghostContainerElements = {};
				if ($scope.testElementTimeouts) {
					var timeouts = $scope.testElementTimeouts;
					$scope.testElementTimeouts = {};
					for (var uuid in timeouts) {
						if (timeouts.hasOwnProperty(uuid) && timeouts[uuid] !== undefined){
						 clearTimeout(timeouts[uuid]);
						}
					}
				}
			}	
			
		},
		templateUrl: 'templates/editor.html',
		replace: true
	};

}).factory("$editorService", function($rootScope, $webSocket, $log, $q, $window, EDITOR_EVENTS, $timeout,
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
		wsSession = $webSocket.connect('', [$webSocket.getURLParameter('clientnr')])
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
			var keyCode = event.keyCode;
			if ((event.metaKey && event.key == 'Meta') || (event.ctrlKey && event.key == 'Control') || (event.altKey && event.key == 'Alt')) { 
				//standalone special keys have a javascript keyCode (91 = Meta, 17 = Ctrl, 18 = Alt) which may be wrongly interpreted in the KeyPressHandler (server side)
				//they must produce no action by themselves
				keyCode = 0
			} 
			wsSession.callService('formeditor', 'keyPressed', {
				ctrl: event.ctrlKey,
				shift: event.shiftKey,
				alt: event.altKey,
				meta: event.metaKey,
				keyCode: keyCode
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
		
		getSystemFont: function(node) {
			return wsSession.callService('formeditor', 'getSystemFont', null, false)
		},

		requestSelection: function(node) {
			return wsSession.callService('formeditor', 'requestSelection', null, true)
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
			var res = wsSession.callService('formeditor', 'toggleShow', {
				"show" : "showWireframeInDesigner"
			}, false);
			this.getEditor().redrawDecorators();
			return res;
		},
		
		isShowSolutionLayoutsCss: function() {
			return wsSession.callService('formeditor', 'getBooleanState', {
				"showSolutionLayoutsCss": true
			}, false)
		},
		
		toggleShowSolutionLayoutsCss: function() {
			var res =  wsSession.callService('formeditor', 'toggleShow', {
				"show" : "showSolutionLayoutsCssInDesigner"
			}, false);
			this.getEditor().redrawDecorators();
			return res;
		},
		
		isShowSolutionCss: function() {
			return wsSession.callService('formeditor', 'getBooleanState', {
				"showSolutionCss": true
			}, false)
		},
		
		toggleShowSolutionCss: function() {
			return wsSession.callService('formeditor', 'toggleShow', {
				"show" : "showSolutionCssInDesigner"
			}, false);
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

		isShowingContainer : function(){
			return $webSocket.getURLParameter("cont");
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

		updateSelection: function(ids, timeout) {
			if (editorScope.updateSel)
			{
				$timeout.cancel(editorScope.updateSel);
			}
			function tryUpdateSelection () {
				var prevSelection = editorScope.getSelection();
				var changed = false;
				var selection = [];
				if (ids && ids.length > 0) {
					if (!editorScope.contentDocument || !editorScope.glasspane)
					{
						$timeout(tryUpdateSelection, 200);
						return;
					}
					var nodes = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[svy-id]"));
					var ghosts = Array.prototype.slice.call(editorScope.glasspane.querySelectorAll("[svy-id]"));
					nodes = nodes.concat(ghosts);
					if (nodes.length == 0)
					{
						$timeout(tryUpdateSelection, 200);
						return;
					}	
					for (var i = 0; i < nodes.length; i++) {
						var id = nodes[i].getAttribute("svy-id");
						if (ids.indexOf(id) != -1) {
							selection.push(nodes[i]);
							changed = changed || prevSelection.indexOf(nodes[i]) == -1;
							if (selection.length == ids.length) break;
						}
					}
					if (selection.length !== prevSelection.length)
						changed = true;
				} else if (prevSelection.length > 0) {
					changed = true;
				}
				if (changed) editorScope.setSelection(selection);
			}
			editorScope.updateSel = $timeout(tryUpdateSelection, typeof timeout == 'number' ? timeout : 400);
		},
		
		setDirty: function(isDirty){
			editorScope.isDirty = isDirty;
		},
		
		refreshPalette: function()
		{
			$rootScope.$broadcast(EDITOR_EVENTS.RENDER_PALETTE);
			return true;
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
			return wsSession.callService('formeditor', 'toggleShow', {
				"show" : "showHighlightInDesigner"
			}, false);
		},

		isShowHighlight: function() {
			return wsSession.callService('formeditor', 'getBooleanState', {
				"showHighlight": true
			}, false)
		},

		toggleShowData: function() {
			wsSession.callService('formeditor', 'toggleShowData', null, true);
		},
		
		isHideInherited: function() {
			return wsSession.callService('formeditor', 'getBooleanState', {
				"isHideInherited": false
			}, false)
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
		},
		
		hideInheritedElements: function(hide)
		{
			var inherited_elements = $(editorScope.contentDocument).find('.inherited_element');
			var ghosts = $(editorScope.contentArea).find('.inherited_element');
			inherited_elements.add(ghosts).each(function(index, element) {
				if (hide) {
					$(element).hide();
				} else {
					$(element).show();
				}
			});
			if (editorScope.getEditorContentRootScope() != null && hide != editorScope.getEditorContentRootScope().hideInherited)
			{
				editorScope.getEditorContentRootScope().hideInherited = hide;
				editorScope.getEditorContentRootScope().$digest();
			}
		},
		
		getSuperForms: function() {
			return wsSession.callService('formeditor', 'getSuperForms');
		},
		
		setCssAnchoring: function(selection, anchors) {
			wsSession.callService('formeditor', 'setCssAnchoring', {"selection":selection, "anchors":anchors}, true);
		},
		
		setContentSizes: function() {
			editorScope.setContentSizes();		
		},
		
		adjustSizes: function() {
			editorScope.adjustIFrameSize();
			editorScope.adjustGlassPaneSize();
			editorScope.setMainContainerSize();
			editorScope.redrawDecorators();
		},
		
		getFormFixedSize: function() {
			return wsSession.callService('formeditor', 'getFormFixedSize');
		},

		setFormFixedSize: function(args) {
			return wsSession.callService('formeditor', 'setFormFixedSize', args);
		},
		
		getZoomLevel: function() {
			return wsSession.callService('formeditor', 'getZoomLevel', {}, false)
		},
		
		setZoomLevel: function(value) {
			return wsSession.callService('formeditor', 'setZoomLevel', {
				"zoomLevel": value
			}, false)
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
