angular.module('dragselection', ['mouseselection']).run(function($rootScope, $pluginRegistry, $editorService, $selectionUtils, EDITOR_EVENTS, EDITOR_CONSTANTS, $interval) {
	$pluginRegistry.registerPlugin(function(editorScope) {

		var utils = $selectionUtils.getUtilsForScope(editorScope);
		var dragging = false;
		var autoscrollElementClientBounds;
		var dragStartEvent = null;
		editorScope.selectionToDrag = null;
		var dragCloneDiv = null;
		var COMPONENT_TYPE = 7;
		var initialParent = null;
		var dragNode = null;
		var autoscrollAreasEnabled = false;
		var configGhostDragState;
		var dragCopy = false; // whatever dragged elment is cloned, because of using ctrl+mouse move

		function onmousedown(event) {
			dragNode = utils.getNode(event);
			// skip dragging if it is an child element of a form reference
			if (event.button == 0 && dragNode) {
				dragStartEvent = event;
				initialParent = null;
				
				var ghostObject = editorScope.getGhost(dragNode.getAttribute("svy-id"));
				
				if (!editorScope.isAbsoluteFormLayout() && !ghostObject) {
					if (angular.element(dragNode).hasClass("formComponentChild")) {//do not grab if this is a form component element
						dragStartEvent = null;
					}
					initialParent = utils.getParent($(dragNode), dragNode.getAttribute("svy-layoutname")? "layout" : "component");
					dragCloneDiv = editorScope.getEditorContentRootScope().createTransportDiv(dragNode, event);
				}
			}
		}

		function onmouseup(event) {

			for (var direction in autoscrollStop) {
				if (angular.isDefined(autoscrollStop[direction])) {
					$interval.cancel(autoscrollStop[direction]);
					autoscrollStop[direction] = undefined;
				}
			}

			//disable mouse events on the autoscroll
			editorScope.setPointerEvents("none");
			
			autoscrollAreasEnabled = false;
			
			for (var direction in autoscrollEnter) {
				if(autoscrollEnter[direction]) editorScope.unregisterDOMEvent("mouseenter", direction, autoscrollEnter[direction]);
			}
			for (var direction in autoscrollLeave) {
				if(autoscrollLeave[direction]) {
					editorScope.unregisterDOMEvent("mouseleave", direction, autoscrollLeave[direction]);
					editorScope.unregisterDOMEvent("mouseup", direction, autoscrollLeave[direction]);
				}
			}


			if (event.button == 0) {
				dragStartEvent = null;
				if (dragCloneDiv) {
					dragCloneDiv.remove();
					dragCloneDiv = null;
				}
				editorScope.getEditorContentRootScope().drop_highlight = null;
				editorScope.getEditorContentRootScope().drag_highlight = null;
				editorScope.getEditorContentRootScope().$apply();
				if (dragging) {
					updateConfigObjectGhostsAndDropTargetWhileDragging(event, true);

					dragging = false;
					autoscrollElementClientBounds = undefined;

					// store the position changes
					var i = 0;
					var obj = {};
					var ghostObject;
					var node;
					var type = "component";
					var canDrop;
					var topContainer = null;
					var layoutName = null;
					var key;

					node = editorScope.selectionToDrag[i];
					if(node[0]) node = node[0];
					ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
					
					if (!editorScope.isAbsoluteFormLayout() && !ghostObject) {
						obj = (event.ctrlKey||event.metaKey) ? [] : {};
						for (i = 0; i < editorScope.selectionToDrag.length; i++) {
							type = "component";
							layoutName = node.getAttribute("svy-layoutname");
							if (layoutName) type = "layout"
							if (ghostObject)
								type = ghostObject.propertyType;
							canDrop = getDropNode(type, topContainer, layoutName, event, node.getAttribute("svy-id"));
							if (!canDrop.dropAllowed) {
								// full refresh the editor content, it can be moved to different places already.
								// TODO this is not enough
								editorScope.refreshEditorContent();
								continue;
							}
							if (canDrop.dropAllowed && !canDrop.beforeChild && !canDrop.append) {
								canDrop.beforeChild = node.nextElementSibling;
							}

							if (canDrop.beforeChild && canDrop.beforeChild.getAttribute("svy-id") === node.getAttribute("svy-id"))
								canDrop.beforeChild = canDrop.beforeChild.nextElementSibling;

							key = (event.ctrlKey||event.metaKey) ? i : node.uuid;
							if (key == undefined) {
								key = node.getAttribute("svy-id");
							}

							obj[key] = {};
							if((event.ctrlKey||event.metaKey)) {
								obj[key].uuid = node.getAttribute('cloneuuid');
								editorScope.selectionToDrag[i].remove();
							}

							if (canDrop.dropTarget) {
								obj[key].dropTargetUUID = canDrop.dropTarget.getAttribute("svy-id");
							}

							if (canDrop.beforeChild) {
								obj[key].rightSibling = canDrop.beforeChild.getAttribute("svy-id");
							}
						}
						if((event.ctrlKey||event.metaKey)) {
							$editorService.createComponents({
								"components": obj
							});
						}
						else {
							$editorService.moveResponsiveComponent(obj);	
						}
					} else {
						if (dragCopy && (event.ctrlKey||event.metaKey)) {
							var components = [];
							var size = 0;
							for (i = 0; i < editorScope.selectionToDrag.length; i++) {
								editorScope.selectionToDrag[i].remove();
								node = editorScope.selectionToDrag[i][0];
								var component = {};
								component.uuid = node.getAttribute('cloneuuid');
								component.x = node.location.x;
								component.y = node.location.y;
								if (component.x > 0 && component.y > 0) {
									components[size++] = component;
								}
							}
							if (size > 0) $editorService.createComponents({
								"components": components
							});
						} else {
							obj = {};
							for (i = 0; i < editorScope.selectionToDrag.length; i++) {
								node = editorScope.selectionToDrag[i];
								if (node.uuid) {
									if (node.type === COMPONENT_TYPE) // this is a component, so we have to move it
										obj[node.uuid] = {
											x: node.location.x,
											y: node.location.y
									}
								} else {
									var beanModel = editorScope.getBeanModel(node);
									if (beanModel) {										
										obj[node.getAttribute("svy-id")] = {
												x: beanModel.location.x,
												y: beanModel.location.y
										}
									} else {
										ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
										if (ghostObject) {
											if (ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP) {
												var groupElements = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[group-id='"+ghostObject.uuid+"']"));
												for (var i = 0; i < groupElements.length; i++)
												{
													var elem = groupElements[i];
													var beanModel = editorScope.getBeanModel(elem.parentElement);
													if (beanModel) {
														obj[elem.parentElement.getAttribute("svy-id")] = {
																x: beanModel.location.x,
																y: beanModel.location.y
														}
													}
												}
											} else {
												obj[node.getAttribute("svy-id")] = {
														x: ghostObject.location.x,
														y: ghostObject.location.y
												}

												if (ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_PART && ghostObject === editorScope.getLastPartGhost()) 
												{
													editorScope.setContentSize(editorScope.contentStyle.width, ghostObject.location.y+"px")
												}
											}
										}
									}
								}
							}

							if (configGhostDragState && configGhostDragState.ghostElementsInContainer) {
								// so a config obj. ghost drag operation finished; in "ghostElementsInContainer" we have the siblings of the dragged ghost(s)
								// or null for indexes of ghosts that were dragged; those need to be updated on server (in developer persist model) as well
								var ghostElements = configGhostDragState.ghostElementsInContainer;
								for (var i in ghostElements) {
									if (ghostElements[i]) {
										var svyID = ghostElements[i][0].getAttribute("svy-id");
										var ghostObject = editorScope.getGhost(svyID);

										obj[svyID] = {
												x: ghostObject.location.x,
												y: ghostObject.location.y
										}
									}
								}
							}

							editorScope.refreshEditorContent();
							$editorService.sendChanges(obj);
							$rootScope.$broadcast(EDITOR_EVENTS.RENDER_DECORATORS, editorScope.selectionToDrag);
						}
					}

					utils.setDraggingFromPallete(null);
				}
				
				// drag operation ended; clean up 
				editorScope.selectionToDrag = null;
				editorScope.glasspane.style.cursor = "";
				if (configGhostDragState) {
					configGhostDragState.dropTargetGhostElement.css("display", "none");
					configGhostDragState.dropTargetGhostElement.css("left", "");
					configGhostDragState = undefined;
				}
				dragCopy = false;
			}
		}

		var updateAbsoluteLayoutComponentsLocations = function (editorScope, draggedSelection, changeX, changeY, minX, minY) {
			var updated = new Set();
			for(var i=0;i< draggedSelection.length;i++) {
				var node = draggedSelection[i];
				if (node[0] && node[0].getAttribute('cloneuuid')){
					node[0].location.x += changeX;
					if(minX != undefined && node[0].location.x < minX) node[0].location.x = minX;
					node[0].location.y += changeY;
					if(minY != undefined && node[0].location.y < minY) node[0].location.y = minY;
					css = {
							top: node[0].location.y,
							left: node[0].location.x
					}
					node.css(css);
				}
				else {
					var css;
					var beanModel = editorScope.getBeanModel(node);
					var uuid = node.getAttribute("svy-id");
					var ghostObject = editorScope.getGhost(uuid);
					if (beanModel)
					{	
						if(minY == undefined || beanModel.location.y + changeY >= minY) beanModel.location.y = beanModel.location.y + changeY;
						if(minX == undefined || beanModel.location.x + changeX >= minX) beanModel.location.x = beanModel.location.x + changeX;
						
					    //it can happen that we have the node in the bean model but it is outside the form
					    //in this case do not update the css as that will be done in the 'if (ghostObject) {...}'
						if (!ghostObject && !updated.has(uuid)) { 
        						css = { top: beanModel.location.y, left: beanModel.location.x }
        						angular.element(node).css(css);
        						updated.add(uuid)
						}
					}
					if (ghostObject && !updated.has(uuid)) {
						if (ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
						{
							var groupElements = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[group-id='"+ghostObject.uuid+"']"));
							for (var i = 0; i < groupElements.length; i++)
							{
								var elem = groupElements[i];
								beanModel = editorScope.getBeanModel(elem.parentElement);
								if (beanModel) {
									beanModel.location.y = beanModel.location.y + changeY;
									if(minY != undefined && beanModel.location.y < minY) beanModel.location.y = minY;
									beanModel.location.x = beanModel.location.x + changeX;
									if(minX != undefined && beanModel.location.x < minX) beanModel.location.x = minX;
									css = { top: beanModel.location.y, left: beanModel.location.x }
									angular.element(elem.parentElement).css(css);
								}
							}
						}
						editorScope.updateGhostLocation(ghostObject, ghostObject.location.x + changeX, ghostObject.location.y + changeY)
						updated.add(uuid)
					}	
				}
			}
		}




		var t;

		var autoscrollStop = [];
		var autoscrollEnter = [];
		var autoscrollLeave = [];

		function addAutoscrollListeners(direction) {
			// enable mouse events on the auto-scroll
			editorScope.setPointerEvents("all");
			autoscrollEnter[direction] = editorScope.registerDOMEvent("mouseenter",direction, function(event){
				autoscrollStop[direction] = editorScope.startAutoScroll(direction, updateAbsoluteLayoutComponentsLocations);
			});

			autoscrollLeave[direction] = function (event){
				if (angular.isDefined(autoscrollStop[direction])) {
					$interval.cancel(autoscrollStop[direction]);
					autoscrollStop[direction] = undefined;
				}
				if (event.type == "mouseup")
					onmouseup(event);
			}

			editorScope.registerDOMEvent("mouseleave",direction, autoscrollLeave[direction]);
			editorScope.registerDOMEvent("mouseup",direction, autoscrollLeave[direction]);
		}	
		
		function getDropNode(type, topContainer, layoutName, event, svyId)
		{
			var canDrop = utils.getDropNode(type, topContainer, layoutName, event, undefined, svyId);
			canDrop.dropAllowed = canDrop.dropAllowed && angular.element(dragNode).hasClass("inheritedElement")
					&& initialParent[0].getAttribute("svy-id") !== canDrop.dropTarget.getAttribute("svy-id") ? false : canDrop.dropAllowed;
			return canDrop;
		}
		
		function isInsideAutoscrollElementClientBounds(clientX, clientY) {
			if (!autoscrollElementClientBounds) return false;
			
			var inside = false;
			var i = 0;
			while (!inside && i < 4) { // 4 == autoscrollElementClientBounds.length always
				var rect = autoscrollElementClientBounds[i++];
				inside = (clientX >= rect.left && clientX <= rect.right && clientY >= rect.top && clientY <= rect.bottom);
			}
			
			return inside;
		}

		function updateConfigObjectGhostsAndDropTargetWhileDragging(mouseEvent, prepareForDropping) {
			// "configGhostDragState" is already computed here; update the positions of siblings and drop target as needed
			// to make it clear to the developer where the ghost(s) that he drags will get dropped
			// configGhostDragState = { "parentGhostContainer": ..., "dropTargetGhostElement": ..., "ghostElementsInContainer": ..., "startX": ..., "selectionGhosts": ... };

			if (!configGhostDragState) return;

			// allow for any pending drop target animations to finish
			var currentMs = new Date().getTime();
			if (!prepareForDropping && configGhostDragState.lastUpdateMs && currentMs - configGhostDragState.lastUpdateMs < 255) {
				if (configGhostDragState.timeout) clearTimeout(configGhostDragState.timeout);
				configGhostDragState.timeout = setTimeout(updateConfigObjectGhostsAndDropTargetWhileDragging, 255 + configGhostDragState.lastUpdateMs - currentMs, mouseEvent, false);
				return;
			} else {
				if (configGhostDragState.timeout) {
					clearTimeout(configGhostDragState.timeout);
					configGhostDragState.timeout = undefined;
				}
				configGhostDragState.lastUpdateMs = currentMs;
			}

			var foundDropSpot = false;
			var ghostElements = configGhostDragState.ghostElementsInContainer;
			
			// we calculate both the visual DOM positions as well as the ghost.location. positions (that will be used at the end when dropping to be sent to server)
			var x = configGhostDragState.startX, ghostX = configGhostDragState.startGhostX;
			var y = configGhostDragState.startY, ghostY = configGhostDragState.startGhostY;
			
			// these 2 are useful in case the ghosts being dragged are from the beginning of the ghostContainer and we need to compute the first x position even though those are being moved
			var selJ = 0;
			var selWidthBeforeNonDraggedSibling = 0, selGhostWidthBeforeNonDraggedSibling = 0; 

			// first try to find the first x position of the ghost elements;
			if (!x && ghostElements.length > 0 && ghostElements[0]) {
				x = configGhostDragState.startX = ghostElements[0].offset().left; // elements in the array could be null (if those ghosts are really the ones being dragged right now)
				ghostX = configGhostDragState.startGhostX = configGhostDragState.parentGhostContainer.ghosts[0].location.x;
			}

			var i = 0;
			var dropTargetElementLeft = configGhostDragState.dropTargetGhostElement.offset().left;
			while (i < ghostElements.length) {
				if (ghostElements[i]) {
					var ghostElLeft = ghostElements[i].offset().left;
					if (angular.isUndefined(y)) {
						y = configGhostDragState.startY = ghostElements[i].offset().top;
						ghostY = configGhostDragState.startGhostY = configGhostDragState.parentGhostContainer.ghosts[i].location.y;
					}
					if (angular.isUndefined(x)) {
						x = configGhostDragState.startX = ghostElLeft - selWidthBeforeNonDraggedSibling;
						ghostX = configGhostDragState.startGhostX = configGhostDragState.parentGhostContainer.ghosts[i].location.x - selGhostWidthBeforeNonDraggedSibling;
					}
					if (!foundDropSpot && ghostElLeft + (dropTargetElementLeft > ghostElLeft ? ghostElements[i].outerWidth() : 0) > mouseEvent.clientX) {
						// we just found where to put the drop target
						configGhostDragState.dropTargetGhostElement.css("display", "inline");
						
						configGhostDragState.dropTargetGhostElement.offset({ left: x, top: y });
						configGhostDragState.dropTargetGhostX = ghostX;
						
						x += configGhostDragState.dropTargetGhostElement.outerWidth(); // this width is computed initially in prepareForDragging(...)
						ghostX += configGhostDragState.dragPlaceholderGhostWidth;
							
						foundDropSpot = true;
					}

					// set the new positions for sibling ghosts starting at "x" (might or might not be the same as before)
					var ghost = configGhostDragState.parentGhostContainer.ghosts[i];
					
					ghost.location.x = ghostX; // keep ghost obj. location up-to date
					ghostElements[i].offset({ left: x }); // set new location of ghost element
					
					x += ghostElements[i].outerWidth();
					ghostX += ghost.size.width;
				} else if (angular.isUndefined(x)) { // null entry in ghostElements means that that particular ghost is being dragged right now; we only care about when x is not defined (to determine initial first ghost x)
					selWidthBeforeNonDraggedSibling += $('[svy-id=' + configGhostDragState.selectionGhosts[selJ].uuid + ']').outerWidth();
					selGhostWidthBeforeNonDraggedSibling += configGhostDragState.selectionGhosts[selJ].size.width;
					selJ++;
				}
				i++;
			}
			if (!foundDropSpot && angular.isDefined(x) && angular.isDefined(y)) {
				configGhostDragState.dropTargetGhostElement.css("display", "inline");
				configGhostDragState.dropTargetGhostElement.offset({ left: x, top: y });
				configGhostDragState.dropTargetGhostX = ghostX;
			}
			
			if (prepareForDropping) {
				// really position the selected/being dragged ghosts where they should be - in the drop target location; that way we make sure when multiple ghosts are being dragged at once
				// that they drop in the right place - and get sent to server correctly (as depending on where the mouse 'grabbed' the selection, some ghost's x positions might be outside the drop target zone)
				var dropZoneOffsetX = configGhostDragState.dropTargetGhostElement.offset().left;
				var dropZoneGhostX = configGhostDragState.dropTargetGhostX;
				var dropOffsetY = configGhostDragState.dropTargetGhostElement.offset().top;
				for (i = 0; i < configGhostDragState.selectionGhosts.length; i++) {
					var selGhostEl = $('[svy-id=' + configGhostDragState.selectionGhosts[i].uuid + ']');
					configGhostDragState.selectionGhosts[i].location.x = dropZoneGhostX;
					configGhostDragState.selectionGhosts[i].location.y = configGhostDragState.startGhostY;
					selGhostEl.offset({ left: dropZoneOffsetX, top: dropOffsetY });
					dropZoneOffsetX += selGhostEl.outerWidth();
					dropZoneGhostX += configGhostDragState.selectionGhosts[i].size.width;
				}
			}
		}

		function prepareForDragging(mouseEvent) {
			dragging = true;

			autoscrollElementClientBounds = editorScope.getAutoscrollElementClientBounds();

			// if we started dragging one or more config object ghosts from an array of config objects, prepare all other
			// ghosts (add "ghost-dnd-mode" class) in that array for dragging/reordering and show the drop placeholder for
			// nice drag feedback; same has to be done for when dragging a new config obj. from palette, but then we don't
			// know the target yet; the target component/property will be prepared then later while moving the mouse
			var dragPlaceholderWidth = 0, dragPlaceholderGhostWidth = 0;

			var selectionElements = editorScope.getSelection();
			var parentGhostContainer, gc, selectionGhosts = [];
			for (var i = 0; i < selectionElements.length; i++)
			{
				var svy_id = selectionElements[i].getAttribute("svy-id");
				var ghost = editorScope.getGhost(svy_id);	
				if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) {
					gc = editorScope.getGhostContainerOf(ghost);
					if (!parentGhostContainer || (gc && parentGhostContainer == gc)) {
						parentGhostContainer = gc;
						dragPlaceholderWidth += $(selectionElements[i]).outerWidth();
						dragPlaceholderGhostWidth += ghost.size.width;
						selectionGhosts.push(ghost);
					} else {
						parentGhostContainer = undefined;
						break;				
					}
				} else {
					parentGhostContainer = undefined;
					break;
				}
			}
			if (parentGhostContainer) {
				if (parentGhostContainer.ghosts.length == selectionGhosts.length) {
					// one cannot reorder config ghosts when all ghosts in parent container are selected
					dragging = false;
				} else {
					// in case of dragging multiple config ghosts that are not adjacent to each-other, move them close to each other while they follow the mouse
					// first sort them by X as the user could have selected them in any order
					selectionGhosts.sort(function (ghostA, ghostB) {
						return ghostA.location.x - ghostB.location.x;
					});
					
					var grabbedGhost = editorScope.getGhost(dragNode.getAttribute("svy-id")); // the one that is under the mouse cursor when drag starts
					var grabbedIdx = selectionGhosts.indexOf(grabbedGhost);
	
					if (grabbedIdx >= 0) {
						// then put them one after the other, keeping the grabbed-with-mouse-ghost where it was
						var i;
						for (i = grabbedIdx + 1; i < selectionGhosts.length; i++)
							selectionGhosts[i].location.x = selectionGhosts[i - 1].location.x + selectionGhosts[i - 1].size.width;
						for (i = grabbedIdx - 1; i >= 0; i--)
							selectionGhosts[i].location.x = selectionGhosts[i + 1].location.x - selectionGhosts[i].size.width;
					}
					
					configGhostDragState = {
						"parentGhostContainer": parentGhostContainer,
						"selectionGhosts": selectionGhosts,
						"dropTargetGhostElement": $("#ghost-dnd-placeholder"),
						"ghostElementsInContainer": [] };
					
					// ok add the new class to the ghosts of the same parent but which are not part of the selection
					for (var i = 0; i < parentGhostContainer.ghosts.length; i++) {
						if (selectionGhosts.indexOf(parentGhostContainer.ghosts[i]) == -1) {
							var el = $('[svy-id=' + parentGhostContainer.ghosts[i].uuid + ']');
							configGhostDragState.ghostElementsInContainer.push(el);
							el.addClass("ghost-dnd-mode");
						} else {
							configGhostDragState.ghostElementsInContainer.push(null); // while dragging we do not touch the position of selected ghosts except for them following the mouse; I just put null here so that the indexes are in-sync with the ghostContainer.ghosts array
						}
					}
					configGhostDragState.dropTargetGhostElement.outerWidth(dragPlaceholderWidth);
					configGhostDragState.dragPlaceholderGhostWidth = dragPlaceholderGhostWidth;
				}
			}
			
			if (dragging) {
				$rootScope.$broadcast(EDITOR_EVENTS.HIDE_DECORATORS);
				utils.setDraggingFromPallete(true);
				if (dragCloneDiv) dragCloneDiv.css({display:'block'});
			}
		}
		
		function onmousemove(event) {
			if (dragStartEvent) {
				var i;
				var ghostObject;
				var node;
				var beanModel;
				if (!dragging) {
					if (Math.abs(dragStartEvent.clientX - event.clientX) > 5 || Math.abs(dragStartEvent.clientY - event.clientY) > 5) {
						prepareForDragging(event);
						if (!dragging) return;
					} else return;
				}
				updateConfigObjectGhostsAndDropTargetWhileDragging(event, false);

				// enable auto-scroll areas only if current mouse event is outside of them (this way, when starting to drag from an auto-scroll area it will not immediately auto-scroll)
				if (autoscrollElementClientBounds && !autoscrollAreasEnabled && !isInsideAutoscrollElementClientBounds(event.clientX, event.clientY)) {
					autoscrollAreasEnabled = true;
					
					addAutoscrollListeners("BOTTOM_AUTOSCROLL")
					addAutoscrollListeners("RIGHT_AUTOSCROLL")
					addAutoscrollListeners("TOP_AUTOSCROLL")
					addAutoscrollListeners("LEFT_AUTOSCROLL")
				}

				if ((event.ctrlKey || event.metaKey) && editorScope.selectionToDrag == null) {
					dragCopy = true;
					editorScope.selectionToDrag = [];
					var selection = editorScope.getSelection();
					for (i = 0; i < selection.length; i++) {
						node = selection[i];
						editorScope.selectionToDrag[i] = angular.element(node).clone();
						editorScope.selectionToDrag[i].attr('id', 'dragNode' + i);
						editorScope.selectionToDrag[i].attr('cloneuuid', node.getAttribute("svy-id"));						

						if(editorScope.isAbsoluteFormLayout()) {
							var posX, posY;
							beanModel = editorScope.getBeanModel(node);
							if (beanModel) {
								posX = beanModel.location.x;
								posY = beanModel.location.y;
							} else {
								ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
								posX = ghostObject.location.x;
								posY = ghostObject.location.y;
							}

							editorScope.selectionToDrag[i][0]['location'] = {
									x: posX,
									y: posY
							};

							editorScope.selectionToDrag[i].css({
								'z-index': 4
							});
							angular.element(selection[i]).parent().append(editorScope.selectionToDrag[i]);
						}
					}
				}

				if (!editorScope.selectionToDrag) {
					editorScope.selectionToDrag = editorScope.getSelection();
					editorScope.selectionToDrag = utils.addGhostsToSelection(editorScope.selectionToDrag);
				}

				if (editorScope.selectionToDrag.length > 0) {
					var firstSelectedNode = editorScope.selectionToDrag[0];
					if (firstSelectedNode[0]) firstSelectedNode = firstSelectedNode[0];
					ghostObject = editorScope.getGhost(firstSelectedNode.getAttribute("svy-id"));

					if (!editorScope.isAbsoluteFormLayout() && !ghostObject) {
						// so it is a d&d of a non-ghost object inside a responsive form
						if (dragCloneDiv){
							var css = editorScope.convertToContentPoint({
								position: 'absolute',
								top: event.pageY+1,
								left: event.pageX+1,
								display: 'block',
								'z-index': 4,
								transition: 'opacity .5s ease-in-out 0'
							});
							dragCloneDiv.css(css);
						}

						var type = "component";
						editorScope.getEditorContentRootScope().drop_highlight = type;
						var layoutName = firstSelectedNode.getAttribute("svy-layoutname");
						if (layoutName) {
							editorScope.getEditorContentRootScope().drop_highlight = layoutName;
							type = "layout";
						}
						
						utils.setDraggingFromPallete(type,true);

						editorScope.getEditorContentRootScope().$apply();

						var topContainer = null;

						var canDrop = getDropNode(type, topContainer, layoutName, event);
						if (!canDrop.dropAllowed) {
							editorScope.glasspane.style.cursor = "not-allowed";
						} else editorScope.glasspane.style.cursor = "pointer";

						dragStartEvent = event;

						if (canDrop.dropTarget && editorScope.selectionToDrag) {
								for (var i = 0; i < editorScope.selectionToDrag.length; i++) {
									var node = angular.element(editorScope.selectionToDrag[i]);
									if (editorScope.glasspane.style.cursor == "pointer") {
										if (canDrop.beforeChild) {
											node.insertBefore(canDrop.beforeChild);
										} else if (node.parent()[0] != canDrop.dropTarget || canDrop.append) {
											angular.element(canDrop.dropTarget).append(node);
										}
									}
								}
								if (t) clearTimeout(t);
								t = setTimeout(function(){
									editorScope.refreshEditorContent();
								}, 200);
							}
					} else {
						var changeX = event.clientX- dragStartEvent.clientX;
						var changeY = event.clientY- dragStartEvent.clientY;
						
						//make sure no element goes offscreen
						var canMove = true;
						var selection = editorScope.selectionToDrag;
						var formSize = editorScope.getContentSize();
						var formWidth = parseInt(formSize.width);
						var formHeight = parseInt(formSize.height);
						for (var i = 0; i < selection.length; i++)
						{
							var node = selection[i].nodeType === Node.ELEMENT_NODE ? selection[i] : selection[i][0];
							var beanModel = editorScope.getBeanModel(node);
							var svy_id = node.getAttribute("svy-id");
							var ghost = editorScope.getGhost(svy_id);	
							if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) continue;
							if (!beanModel) beanModel = ghost;
							if (beanModel && beanModel.location && (beanModel.location.y + changeY < 0 || beanModel.location.x + changeX < 0) )
							{
								canMove = false;
								break;
							}
						}
						
						if (canMove)
						{
							updateAbsoluteLayoutComponentsLocations(editorScope, editorScope.selectionToDrag, changeX, changeY);
						}
						dragStartEvent = event;
						
						editorScope.getEditorContentRootScope().drag_highlight = editorScope.selectionToDrag;
						editorScope.getEditorContentRootScope().$apply();
					}
				}
			}
		}

		angular.element('body').keyup(function(event) {
			//if control is released during drag, the copy is deleted and selected element must be moved
			if (dragStartEvent && dragStartEvent.ctrlKey && event.which == 17) {
				for (var i = 0; i < editorScope.selectionToDrag.length; i++) {
					editorScope.selectionToDrag[i].remove();
				}
				editorScope.selectionToDrag = editorScope.getSelection();
			}
		});

		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown", "CONTENTFRAME_OVERLAY", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup", "CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe
		editorScope.registerDOMEvent("mousemove", "CONTENTFRAME_OVERLAY", onmousemove); // real selection in editor content iframe
//		editorScope.registerDOMEvent("mouseleave", "CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe

	});
});
