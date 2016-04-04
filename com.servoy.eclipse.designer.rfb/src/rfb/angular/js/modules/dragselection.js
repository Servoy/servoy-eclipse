angular.module('dragselection', ['mouseselection']).run(function($rootScope, $pluginRegistry, $editorService, $selectionUtils, EDITOR_CONSTANTS, $interval) {
	$pluginRegistry.registerPlugin(function(editorScope) {

		var utils = $selectionUtils.getUtilsForScope(editorScope);
		var dragging = false;
		var dragStartEvent = null;
		editorScope.selectionToDrag = null;
		var dragCloneDiv = null;
		var COMPONENT_TYPE = 7;

		function onmousedown(event) {
			var dragNode = utils.getNode(event);
			if (event.button == 0 && dragNode) {
				dragStartEvent = event;
				if(!editorScope.isAbsoluteFormLayout()){
					if (angular.element(dragNode).hasClass("inheritedElement")) {//do not grab if this is an inherited element
						dragStartEvent = null;
					}
					dragCloneDiv = editorScope.getEditorContentRootScope().createTransportDiv(dragNode, event);
				}
			}
		}

		function onmouseup(event) {
		    
		    	if (event.type == "mouseup"){
                		if (angular.isDefined(stop)) {
                		    $interval.cancel(stop);
                		    stop = undefined;
                		}
                		//disable mouse events on the bottom_autoscroll
        			editorScope.setPointerEvents("none");
                		if (bottomAutoscrollEnter) editorScope.unregisterDOMEvent("mouseenter", "BOTTOM_AUTOSCROLL", bottomAutoscrollEnter);
                		if (bottomAutoscrollLeave) editorScope.unregisterDOMEvent("mouseleave", "BOTTOM_AUTOSCROLL", bottomAutoscrollLeave);
                		if (bottomAutoscrollMouseup) editorScope.unregisterDOMEvent("mouseup", "BOTTOM_AUTOSCROLL", bottomAutoscrollMouseup);
		    	}
        		
			if (event.button == 0) {
				dragStartEvent = null;
				if (dragCloneDiv) {
			    	    dragCloneDiv.remove();
			    	    dragCloneDiv = null;
			    	}
				editorScope.getEditorContentRootScope().drop_highlight = null;
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
							canDrop = utils.getDropNode(type, topContainer, layoutName, event);
							if (!canDrop.dropAllowed) {
								// full refresh the editor content, it can be moved to different places already.
								// TODO this is not enough
								editorScope.refreshEditorContent();
								continue;
							}
							if (canDrop.dropAllowed && !canDrop.beforeChild) {
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
													var beanModel = editorScope.getBeanModel(elem);
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
				if (event.type == "mouseup"){
				    editorScope.selectionToDrag = null;
				}
			}
		}
		
		var updateAbsoluteLayoutComponentsLocations = function (editorScope, draggedSelection, changeX, changeY) {
			for(var i=0;i< draggedSelection.length;i++) {
				var node = draggedSelection[i];
				if (node[0] && node[0].getAttribute('cloneuuid')){
						node[0].location.x += changeX;
						node[0].location.y += changeY;
						css = {
							top: node[0].location.y,
							left: node[0].location.x
						}
						node.css(css);
				}
				else {
				    	var css;
					
					var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
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
									beanModel.location.x = beanModel.location.x + changeX;
									css = { top: beanModel.location.y, left: beanModel.location.x }
									angular.element(elem.parentElement).css(css);
								}
							}
						}
						editorScope.updateGhostLocation(ghostObject, ghostObject.location.x + changeX, ghostObject.location.y + changeY)
					}
					else {
        					var beanModel = editorScope.getBeanModel(node);
        					    if (beanModel){
        						beanModel.location.y = beanModel.location.y + changeY;
        						beanModel.location.x = beanModel.location.x + changeX;
        						css = { top: beanModel.location.y, left: beanModel.location.x }
        						angular.element(node).css(css);
        						
        					}	
					}
					
				}
			}
		}

		var t;
		var stop;
		var bottomAutoscrollEnter;
		var bottomAutoscrollLeave;
		var bottomAutoscrollMouseup;
		function onmousemove(event) {
			if (dragStartEvent) {
				var i;
				var ghostObject;
				var node;
				var beanModel;
				if (!dragging) {
					if (Math.abs(dragStartEvent.screenX - event.screenX) > 5 || Math.abs(dragStartEvent.screenY - event.screenY) > 5) {
						dragging = true;
						utils.setDraggingFromPallete(true);
						
						//if the click starts in the bottom 20px and going up, 
						//then do not enable the bottom div & start dragging downwards
						if ((event.clientY <= editorScope.glasspane.clientHeight - 20)||(dragStartEvent.screenY - event.screenY > 0)){
						    	//enable mouse events on the bottom_autoscroll
						    	editorScope.setPointerEvents("all");
        						bottomAutoscrollEnter = editorScope.registerDOMEvent("mouseenter","BOTTOM_AUTOSCROLL", function(event){
        						   stop = editorScope.startBottomAutoScroll(updateAbsoluteLayoutComponentsLocations);
        						});
        						
        						bottomAutoscrollLeave = editorScope.registerDOMEvent("mouseleave","BOTTOM_AUTOSCROLL", function(){
        						    if (angular.isDefined(stop)) {
        						            $interval.cancel(stop);
        						            stop = undefined;
        						    }
        						});
        						
        						bottomAutoscrollMouseup = editorScope.registerDOMEvent("mouseup","BOTTOM_AUTOSCROLL", function(){
        						    if (angular.isDefined(stop)) {
        						            $interval.cancel(stop);
        						            stop = undefined;
        						    }
        						});
						}
						
						
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
						
						
					    	editorScope.getEditorContentRootScope().$apply();

						var topContainer = null;

						ghostObject = editorScope.getGhost(firstSelectedNode.getAttribute("svy-id"));
						if (ghostObject) return;

						
						var canDrop = utils.getDropNode(type, topContainer, layoutName, event);
						if (!canDrop.dropAllowed) {
							editorScope.glasspane.style.cursor = "no-drop";
						} else editorScope.glasspane.style.cursor = "";

						dragStartEvent = event;

						if (t) clearTimeout(t);
						t = setTimeout(function() {
							if (canDrop.dropTarget && editorScope.selectionToDrag) {
								for (var i = 0; i < editorScope.selectionToDrag.length; i++) {
									var node = angular.element(editorScope.selectionToDrag[i]);
									if (editorScope.glasspane.style.cursor == "") {
										if (canDrop.beforeChild) {
											node.insertBefore(canDrop.beforeChild);
										} else if (node.parent()[0] != canDrop.dropTarget || canDrop.append) {
											angular.element(canDrop.dropTarget).append(node);
										}
									}
								}
								editorScope.refreshEditorContent();
							}
						}, 200);
					} else {
						var changeX = event.screenX- dragStartEvent.screenX;
						var changeY = event.screenY- dragStartEvent.screenY;
						updateAbsoluteLayoutComponentsLocations(editorScope, editorScope.selectionToDrag, changeX, changeY);
						editorScope.refreshEditorContent();
						dragStartEvent = event;
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
		editorScope.registerDOMEvent("mouseleave", "CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe

	});
});
