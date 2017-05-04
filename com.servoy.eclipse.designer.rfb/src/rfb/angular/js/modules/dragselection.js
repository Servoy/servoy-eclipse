angular.module('dragselection', ['mouseselection']).run(function($rootScope, $pluginRegistry, $editorService, $selectionUtils, EDITOR_EVENTS, EDITOR_CONSTANTS, $interval) {
	$pluginRegistry.registerPlugin(function(editorScope) {

		var utils = $selectionUtils.getUtilsForScope(editorScope);
		var dragging = false;
		var dragStartEvent = null;
		editorScope.selectionToDrag = null;
		var dragCloneDiv = null;
		var COMPONENT_TYPE = 7;
		var initialParent = null;
		var dragNode = null;

		function onmousedown(event) {
			dragNode = utils.getNode(event);
			// skip dragging if it is an child element of a form reference
			if (event.button == 0 && dragNode) {
				dragStartEvent = event;
				initialParent = null;
				if(!editorScope.isAbsoluteFormLayout()){
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

					dragging = false;
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

					if (!editorScope.isAbsoluteFormLayout()) {
						obj = (event.ctrlKey||event.metaKey) ? [] : {};
						for (i = 0; i < editorScope.selectionToDrag.length; i++) {
							node = editorScope.selectionToDrag[i];
							if(node[0]) node = node[0];
							type = "component";
							layoutName = node.getAttribute("svy-layoutname");
							if (layoutName) type = "layout"
							ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
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

							//support for reordering ghosts in responsive layout - if this is a ghost then only allow dropping on top of a sibling ghost
							if (!ghostObject || (angular.element(canDrop.dropTarget).parent() !== angular.element(node).parent)) {
								if (canDrop.dropTarget) {
									obj[key].dropTargetUUID = canDrop.dropTarget.getAttribute("svy-id");
								}

								if (canDrop.beforeChild) {
									obj[key].rightSibling = canDrop.beforeChild.getAttribute("svy-id");
								}
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
						if ((event.ctrlKey||event.metaKey)) {
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
						}
						else {
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
										beanModel.location.y;
										beanModel.location.x
										obj[node.getAttribute("svy-id")] = {
												x: beanModel.location.x,
												y: beanModel.location.y
										}
									} else {
										ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
										if (ghostObject) {
											if (ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
											{
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
											}
											else
											{
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
							editorScope.refreshEditorContent();
							$editorService.sendChanges(obj);
						}
					}

					utils.setDraggingFromPallete(null);
				}
				editorScope.selectionToDrag = null;
				editorScope.glasspane.style.cursor = "";
			}
		}

		var updateAbsoluteLayoutComponentsLocations = function (editorScope, draggedSelection, changeX, changeY, minX, minY) {
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
					var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
					if (beanModel)
					{	
						if(minY == undefined || beanModel.location.y + changeY >= minY) beanModel.location.y = beanModel.location.y + changeY;
						if(minX == undefined || beanModel.location.x + changeX >= minX) beanModel.location.x = beanModel.location.x + changeX;
						
					    //it can happen that we have the node in the bean model but it is outside the form
					    //in this case do not update the css as that will be done in the 'if (ghostObject) {...}'
						if (!ghostObject) { 
        						css = { top: beanModel.location.y, left: beanModel.location.x }
        						angular.element(node).css(css);
						}
					}
					if (ghostObject) {
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
					}	
				}
			}
		}




		var t;

		var autoscrollStop = [];
		var autoscrollEnter = [];
		var autoscrollLeave = [];

		function addAutoscrollListeners(direction) {
			//enable mouse events on the autoscroll
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
		function onmousemove(event) {
			if (dragStartEvent) {
				var i;
				var ghostObject;
				var node;
				var beanModel;
				if (!dragging) {
					if (Math.abs(dragStartEvent.screenX - event.screenX) > 5 || Math.abs(dragStartEvent.screenY - event.screenY) > 5) {
						dragging = true;
						$rootScope.$broadcast(EDITOR_EVENTS.HIDE_DECORATORS);
						utils.setDraggingFromPallete(true);

						//if the click starts in the bottom 20px and going up, 
						//then do not enable the bottom div & start dragging downwards
						if ((event.clientY <= editorScope.glasspane.clientHeight - 20) || (dragStartEvent.screenY - event.screenY > 0)){
							addAutoscrollListeners("BOTTOM_AUTOSCROLL")
						}

						//if the click starts in the right 20px and going right, 
						//then do not enable the bottom div & start dragging downwards
						if ((event.clientX <= editorScope.glasspane.clientWidth - 20) || (dragStartEvent.screenX - event.screenX > 0)){
							addAutoscrollListeners("RIGHT_AUTOSCROLL")
						}


						addAutoscrollListeners("TOP_AUTOSCROLL")
						addAutoscrollListeners("LEFT_AUTOSCROLL")

						
						if (dragCloneDiv) dragCloneDiv.css({display:'block'});
					} else return;
				}

				if ((event.ctrlKey || event.metaKey) && editorScope.selectionToDrag == null) {
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
					if (!editorScope.isAbsoluteFormLayout()) {
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
						var firstSelectedNode = editorScope.selectionToDrag[0];
						if(firstSelectedNode[0]) firstSelectedNode = firstSelectedNode[0];

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

						ghostObject = editorScope.getGhost(firstSelectedNode.getAttribute("svy-id"));
						if (ghostObject) return;


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
						var changeX = event.screenX- dragStartEvent.screenX;
						var changeY = event.screenY- dragStartEvent.screenY;
						
						//make sure no element goes offscreen
						var canMove = true;
						var selection = editorScope.selectionToDrag;
						//depending if the cursor is inside or outside the form area we want to remove from the selection 
						//the ghosts which have also bean model, or respectively the elements which also have ghosts 
						var toRemove = [];
						var removeGhosts = event.screenX > formWidth || event.screenY > formHeight;
						var formSize = editorScope.getContentSize();
						var formWidth = parseInt(formSize.width);
						var formHeight = parseInt(formSize.height);
						for (var i = 0; i < selection.length; i++)
						{
							var beanModel = editorScope.getBeanModel(selection[i]);
							var svy_id = selection[i].getAttribute("svy-id");
							var ghost = editorScope.getGhost(svy_id);		
							if (beanModel && ghost && toRemove.indexOf(svy_id) == -1)
							{
								toRemove.push(svy_id);
							}
							if (!beanModel) beanModel = ghost;
							if (beanModel && beanModel.location && (beanModel.location.y + changeY < 0 || beanModel.location.x + changeX < 0) )
							{
								canMove = false;
								break;
							}
						}
						
						if (canMove)
						{
							for (var i = 0; i < toRemove.length; i++)
							{
								for (var j = 0; j < editorScope.selectionToDrag.length; j++)
								{
									if (editorScope.selectionToDrag[j].getAttribute("svy-id") == toRemove[i])
									{
										if (removeGhosts && editorScope.selectionToDrag[j].classList.contains("ghost") || !removeGhosts && editorScope.selectionToDrag[j].classList.contains("svy-wrapper"))
										{
											editorScope.selectionToDrag.splice(j,1);
											break;
										}
									}
								}
							}									
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
