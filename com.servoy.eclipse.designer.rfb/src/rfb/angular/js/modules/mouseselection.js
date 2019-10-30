angular.module('mouseselection', ['editor']).run(function($rootScope, $pluginRegistry, $selectionUtils,
	EDITOR_CONSTANTS, $editorService, EDITOR_EVENTS) {
	$pluginRegistry.registerPlugin(function(editorScope) {
		var selectedNodeMouseEvent;
		var lassoStarted = false;
		var mouseDownPosition = {
			"left": -1,
			"top": -1
		};
		var lassoDiv = editorScope.glasspane.firstElementChild
		var utils = $selectionUtils.getUtilsForScope(editorScope);

		function select(event, node) {
			if (event.ctrlKey || event.metaKey) {
				if (editorScope.getSelection().indexOf(node) !== -1) {
					editorScope.reduceSelection(node)
				} else {
					editorScope.extendSelection(node)
				}
			} else if (event.shiftKey) {
				/*
				 * Shift-Click does range select: all elements within the box defined by the uttermost top/left point of selected elements
				 * to the uttermost bottom-right of the clicked element
				 */
				if (!node || $(node).hasClass("ghost"))
				{
					editorScope.setSelection([]);
					return;
				}

				var selection = editorScope.getSelection();
				if (selection.length > 0) {
					var shiftSelectType = null;
					// this is a Shift-type select, it selects elements of the same type as the one selected
					if(event.altKey) {
						if(node.getAttribute("svy-layoutname") != selection[0].getAttribute("svy-layoutname")) {
							return;
						}
						shiftSelectType = selection[0].getAttribute("svy-layoutname") ? utils.SELECT_CONTAINER : utils.SELECT_COMPONENT;
					}

					var rec = node.getBoundingClientRect();
					var p1 = {
						top: rec.top,
						left: rec.left
					}
					var p2 = {
						top: rec.bottom,
						left: rec.right
					}
					for (var i = 0; i < selection.length; i++) {
						var rect = selection[i].getBoundingClientRect();
						p1 = {
							top: Math.min(p1.top, rect.top),
							left: Math.min(p1.left, rect.left)
						}
						p2 = {
							top: Math.max(p2.top, rect.bottom),
							left: Math.max(p2.left, rect.right)
						}
					}
					var elements = utils.getElementsByRectangle(p1, p2, 1, true, true,undefined,true, shiftSelectType);
					editorScope.setSelection(elements);
				} else {
					editorScope.setSelection(node);
				}
			} else if (event.button == 2 && editorScope.getSelection().indexOf(node) !== -1 && editorScope.getSelection().length > 1) {
				// if we right click on selected element while multiple selection,just show context menu and do not modify selection
				return;
			} else {
				editorScope.setSelection(node);
			}

		}

		function onmousedown(event) {
			var node = utils.getNode(event, false, false, true);
			var nonSelectableNode;
			if(node instanceof Array) {
				nonSelectableNode = node[1]
				node = node[0];
			}
			if(nonSelectableNode) {
				var message = nonSelectableNode.getAttribute("svy-non-selectable");
				if(message == "noname") {
					$editorService.setStatusBarText("Can't select a component without a name.");
					node = null;
				}
			}
			else {
				$editorService.setStatusBarText(null);
			}
			if (node) {
				if (editorScope.getSelection().indexOf(node) !== -1) {
					// its in the current selection, remember this for mouse up.
					selectedNodeMouseEvent = event;
				} else select(event, node);
			} else {
				if (event.button == 0) {
					editorScope.setSelection([])
				}
			}
		}

		function onmouseup(event) {
			if (selectedNodeMouseEvent) {
				if (event.pageX == selectedNodeMouseEvent.pageX && event.pageY == selectedNodeMouseEvent.pageY) {
					var node = utils.getNode(event);
					select(event, node);
				}
			}
			selectedNodeMouseEvent = null;
		}

		function onmousedownLasso(event) {
			if (event.button == 0 && !lassoStarted) {
				if(event.target && event.target.parentElement.hasAttribute('svy-id'))
				{
					var ghostObject = editorScope.getGhost(event.target.parentElement.getAttribute("svy-id"));
					if (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_PART)
					{
						return;
					}	
				}	
				var node = utils.getNode(event);
				var canStartLasso = !node;
				if (node) {
					var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
					canStartLasso = (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM);
				}

				if (canStartLasso) {
					startLasso(event);
				}
			}
		}

		function onmouseupLasso(event) {
			if (event.button == 0) {
				stopLasso(event);
			}
		}

		function startLasso(event) {
			mouseDownPosition = utils.getMousePosition(event, lassoStarted);
			lassoDiv.style.left = mouseDownPosition.left + 'px';
			lassoDiv.style.top = mouseDownPosition.top + 'px';
			lassoStarted = true;
		}

		function stopLasso(event) {
			if (lassoStarted) {
				var lassoMouseSelectPosition = utils.getMousePosition(event, lassoStarted);
				var p1 = mouseDownPosition;
				var p2 = lassoMouseSelectPosition;
				if (Math.abs(p1.left - p2.left) > 1 && Math.abs(p1.top - p2.top) > 1) {
					var selectedElements = utils.getElementsByRectangle(p1, p2, 100, true, true);
					
					//remove the duplicates if we have both ghost and bean for the same element
					var selection = selectedElements.slice();
					for (var i = selectedElements.length -1; i >= 0 ; i--)
					{
						if(selectedElements[i].classList.contains("svy-wrapper"))
						{
							var beanModel = editorScope.getBeanModel(selectedElements[i]);
							var svy_id = selectedElements[i].getAttribute("svy-id");
							var ghost = editorScope.getGhost(svy_id);	
							if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_CONFIGURATION) continue;
							if (beanModel && ghost)
							{
								selection.splice(i,1)
							}	
						}
					}
					
					editorScope.setSelection(selection);
				}
				lassoStarted = false;
				lassoDiv.style.display = 'none';
			}
		}

		function onmousemove(event) {
			if (lassoStarted) {
				mouseMovePosition = utils.getMousePosition(event, lassoStarted);
				if (mouseMovePosition.left < mouseDownPosition.left) {
					lassoDiv.style.left = mouseMovePosition.left + 'px';
				}
				if (mouseMovePosition.top < mouseDownPosition.top) {
					lassoDiv.style.top = mouseMovePosition.top + 'px'
				}
				var currentWidth = mouseMovePosition.left - mouseDownPosition.left;
				var currentHeight = mouseMovePosition.top - mouseDownPosition.top;
				lassoDiv.style.width = Math.abs(currentWidth) + 'px';
				lassoDiv.style.height = Math.abs(currentHeight) + 'px';
				lassoDiv.style.display = 'block';
			}
		}

		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown", "CONTENTFRAME_OVERLAY", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup", "CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe

		editorScope.registerDOMEvent("mousedown", "CONTENTFRAME_OVERLAY", onmousedownLasso);
		editorScope.registerDOMEvent("mouseup", "CONTENTFRAME_OVERLAY", onmouseupLasso);
		editorScope.registerDOMEvent("mousemove", "CONTENTFRAME_OVERLAY", onmousemove);
		
		editorScope.registerDOMEvent("dblclick", "CONTENTFRAME_OVERLAY", function(e) {
			var selection = editorScope.getSelection()
			if(selection && selection.length) {
				var el = angular.element(selection[0]);
				var attrDirectEdit = el.attr('directeditpropertyname');
				if (typeof attrDirectEdit == typeof undefined || attrDirectEdit == false) {
					if (el.hasClass('maxLevelDesign'))
					{
						$editorService.executeAction('zoomIn');
						$rootScope.$broadcast(EDITOR_EVENTS.RENDER_DECORATORS, editorScope.getSelection());
					}
					else
					{
						var fr = el.closest('.form_reference');
						if(fr.length) {
							editorScope.openContainedForm({"uuid" : fr.attr("svy-id")});
						}
					}
				}
			}
		});
	})
}).factory("$selectionUtils", function(EDITOR_CONSTANTS,$allowedChildren) {
	function hasClass(element, cls) {
		return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
	}

	function isGhostAlreadySelected(selection, ghost) {
		for (var i = 0; i < selection.length; i++) {
			if (selection[i].getAttribute("svy-id") == ghost.getAttribute("svy-id")) return true;
		}
		return false;
	}


	var draggingFromPallete = null;
	var designMode = false;

	return {
		getUtilsForScope: function(editorScope) {
			return {

				SELECT_COMPONENT: 1,
				SELECT_CONTAINER: 2,

				addGhostsToSelection: function(selection) {

					var addToSelection = [];

					for (var i = 0; i < selection.length; i++) {
						var node = selection[i];
						var ghostsForNode = editorScope.getContainedGhosts(node.getAttribute("svy-id"));
						if (ghostsForNode) {
							for (var j = 0; j < ghostsForNode.length; j++) {
								var ghost = $('[svy-id=' + ghostsForNode[j].uuid + ']')[0]
								if (!isGhostAlreadySelected(selection, ghost))
									addToSelection.push(ghost);
							}
						}
					}
					selection = selection.concat(addToSelection);
					return selection;
				},
				
				getParent: function getParent(dt,realName) {
					if (!dt || !dt[0]) return null;
					var allowedChildren = $allowedChildren.get(dt[0].getAttribute("svy-layoutname"));
					if (!allowedChildren || !(allowedChildren.indexOf(realName) >= 0)) {
						// maybe this is a component that has svy-types instead of svy-allowed-childrent
						allowedChildren = dt[0].getAttribute("svy-types");
						if (!allowedChildren || !(allowedChildren.indexOf(realName) >= 0)) {
							return this.getParent( $(dt).parent("[svy-id]"),realName); // the drop target doesn't allow this layout container type
						}
					}
					return dt;
				},

				getDropNode: function(type, topContainer, layoutName, event, componentName, skipNodeId) {
					var dropTarget = null;
					if (type == "layout" || (type == "component" && !editorScope.isAbsoluteFormLayout())) {
						var realName = layoutName ? layoutName : "component";

						dropTarget = this.getNode(event, true, skipNodeId);
						if (!dropTarget) {
							var formRect = $(".contentframe").get(0).getBoundingClientRect();
							//it can be hard to drop on bottom, so just allow it to the end
							var isForm = event.clientX > formRect.left && event.clientX < formRect.right &&
							event.clientY > formRect.top;
							// this is on the form, can this layout container be dropped on the form?
							if (!isForm || !topContainer) {
								return {
									dropAllowed: false
								};
							}
							var beforeNode = null;
							dropTarget = $(".contentframe").contents().find("#svyDesignForm").get(0);
							//droptarget is the form but has no svy-id
							for (var i=dropTarget.childNodes.length-1;i>=0;i--)
							{
								var node = dropTarget.childNodes[i];
								if (node && node.getAttribute && node.getAttribute('svy-id'))
								{
									var clientRec = node.getBoundingClientRect();
									var absolutePoint = editorScope.convertToAbsolutePoint({
										x: clientRec.right,
										y: clientRec.bottom
									});
									// if cursor is in rectangle between 0,0 and bottom right corner of component we consider it to be before that component
									// can we enhance it ?
									if (event.pageY < absolutePoint.y && event.pageX < absolutePoint.x)
									{
										beforeNode = node;
									}
									else
										break;

								}	
							}	
							return {
								dropAllowed: true,
								dropTarget: null,
								beforeChild: beforeNode
							};
						} else {
							var realDropTarget = this.getParent($(dropTarget),realName);
							if (realDropTarget == null) {
								return {
									dropAllowed: false
								};
							} else if (realDropTarget[0] != dropTarget) {
								var drop = dropTarget.clientWidth == 0 && dropTarget.clientHeight == 0 && dropTarget.firstElementChild ? dropTarget.firstElementChild : dropTarget;
								var clientRec = drop.getBoundingClientRect();
								var bottomPixels = (clientRec.bottom - clientRec.top) * 0.3;
								var rightPixels = (clientRec.right - clientRec.left) * 0.3;
								var absolutePoint = editorScope.convertToAbsolutePoint({
									x: clientRec.right,
									y: clientRec.bottom
								});
								if (event.pageY > (absolutePoint.y - bottomPixels) || event.pageX > (absolutePoint.x - rightPixels)) {
									// this is in the 30% corner (bottom or right) of the component
									// the beforeChild should be a sibling of the dropTarget (or empty if it is the last)

									dropTarget = dropTarget.nextElementSibling;

									// if there is no nextElementSibling then force it to append so that it is moved to the last position.
									if (!dropTarget) return {
										dropAllowed: true,
										dropTarget: realDropTarget[0],
										append: true
									};
								}
								if (dropTarget && !dropTarget.getAttribute('svy-id')) {
									dropTarget = dropTarget.nextElementSibling;
								}
								return {
									dropAllowed: true,
									dropTarget: realDropTarget[0],
									beforeChild: dropTarget
								};
							}
							else
							{
								// we drop directly on the node, try to determine its position between children
								var beforeNode = null;
								for (var i=dropTarget.childNodes.length-1;i>=0;i--)
								{
									var node = dropTarget.childNodes[i];
									if (node && node.getAttribute && node.getAttribute('svy-id'))
									{
										var clientRec = node.getBoundingClientRect();
										var absolutePoint = editorScope.convertToAbsolutePoint({
											x: clientRec.right,
											y: clientRec.bottom
										});
										// if cursor is in rectangle between 0,0 and bottom right corner of component we consider it to be before that component
										// can we enhance it ?
										if (event.pageY < absolutePoint.y && event.pageX < absolutePoint.x)
										{
											beforeNode = node;
										}
										else
											break;
												
									}	
								}
								if (!beforeNode)
								{
									// is this really last or we didn't detect it well, try again with new rectangle
									for (var i=dropTarget.childNodes.length-1;i>=0;i--)
									{
										var node = dropTarget.childNodes[i];
										if (node && node.getAttribute && node.getAttribute('svy-id'))
										{
											var clientRec = node.getBoundingClientRect();
											var absolutePoint = editorScope.convertToAbsolutePoint({
												x: clientRec.left,
												y: clientRec.bottom
											});
											// if cursor is in rectangle between bottom left corner of component and top right corner of form we consider it to be before that component
											// can we enhance it ?
											if (event.pageY < absolutePoint.y && event.pageX > absolutePoint.x)
											{
												beforeNode = node;
											}
											else
												break;
													
										}	
									}
								}	
								return {
									dropAllowed: true,
									dropTarget: dropTarget,
									beforeChild: beforeNode
								};
							}	
						}
					} else if (type != "component" && type != "template") {
						dropTarget = this.getNode(event);
						if (dropTarget && dropTarget.getAttribute("svy-types")) {
							if (dropTarget.getAttribute("svy-types").indexOf(type) <= 0)
								return {
									dropAllowed: false
								}; // the drop target doesn't support this type
						} else return {
							dropAllowed: false
						}; // ghost has no drop target or the drop target doesn't support any types
					} else {
						dropTarget = this.getNode(event, true);
						if (componentName !== undefined && dropTarget && dropTarget.getAttribute("svy-forbidden-components")) {
							if (dropTarget.getAttribute("svy-forbidden-components").indexOf(componentName) > 0)
								return {
									dropAllowed: false
								}; // the drop target doesn't suppor this component
						}
					}
					return {
						dropAllowed: true,
						dropTarget: dropTarget
					};
				},

				updateDesignMode: function(design) {
					if (!editorScope.isAbsoluteFormLayout()) {
						if (design) {
							designMode = editorScope.getEditorContentRootScope().showWireframe;
							editorScope.getEditorContentRootScope().showWireframe = true;
						} else
							editorScope.getEditorContentRootScope().showWireframe = designMode;
						editorScope.getEditorContentRootScope().$digest();
					}
				},
				setDraggingFromPallete: function(dragging,skipUpdateDesignMode) {
					draggingFromPallete = dragging;
					if (!skipUpdateDesignMode) this.updateDesignMode(dragging != null);
				},
				getDraggingFromPallete: function() {
					return draggingFromPallete;
				},

				adjustForPadding: function(mousePosition) {
					mousePosition.left -= parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px", ""));
					mousePosition.top -= parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px", ""));
					return mousePosition;
				},

				getMousePosition: function(event, lassoStarted) {
					var xMouseDown = -1;
					var yMouseDown = -1;
					if (event.pageX || event.pageY) {
						xMouseDown = event.pageX;
						yMouseDown = event.pageY;
					} else
					if (event.clientX || event.clientY) {
						xMouseDown = event.clientX;
						yMouseDown = event.clientY;
					}
					if (lassoStarted || hasClass(event.target, "contentframe-overlay") || hasClass(event.target, "decorator") || hasClass(event.target, "ghostcontainer")
							|| hasClass(event.target, "ghostContainerPropName") || hasClass(event.target, "ghost") || hasClass(event.target, "knob") || event.target.id == "highlight") {
						xMouseDown -= editorScope.glasspane.parentElement.offsetLeft;
						yMouseDown -= editorScope.glasspane.parentElement.offsetTop;
						xMouseDown += editorScope.glasspane.parentElement.scrollLeft;
						yMouseDown += editorScope.glasspane.parentElement.scrollTop;
					} else if (!hasClass(event.target, "contentframe-overlay") && !hasClass(event.target, "ghost")) {
						xMouseDown += parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px", ""));
						yMouseDown += parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px", ""));
					}
					return {
						"left": xMouseDown,
						"top": yMouseDown
					};
				},
				getNode: function(event, skipGlass, skipNodeId, returnNonSelectable) {
					var glassPaneMousePosition = this.getMousePosition(event);
					var glassPaneMousePosition1 = {
						top: glassPaneMousePosition.top + 0.5,
						left: glassPaneMousePosition.left + 0.5
					};
					var glassPaneMousePosition2 = {
						top: glassPaneMousePosition.top - 0.5,
						left: glassPaneMousePosition.left - 0.5
					};
					var nonSelectableNode;
					var elements = this.getElementsByRectangle(glassPaneMousePosition1, glassPaneMousePosition2, 0.000001, true, !skipGlass, skipNodeId);

					if (elements.length == 1) {
						if (!(angular.element(elements[0]).is("[svy-non-selectable]")))
							return elements[0];
						else {
							nonSelectableNode = elements[0]; 
						}
					}
					else if (elements.length > 1) {
						var node = elements[elements.length - 1];
						var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
						if (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM && !(angular.element(elements[elements.length - 2]).is("[svy-non-selectable]"))) {
							return elements[elements.length - 2];
						}
						else if (angular.element(elements[0]).is("[svy-non-selectable]")) {
							nonSelectableNode = elements[elements.length - 2];
						}
						if (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
						{
							function getMinFormIndexGroup(ghostObject)
							{
								if (ghostObject.type != EDITOR_CONSTANTS.GHOST_TYPE_GROUP) return Number.MAX_SAFE_INTEGER;
								var groupElements = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[group-id='"+ghostObject.uuid+"']"));
								var minGroupIndex =  Number.MAX_SAFE_INTEGER;
								for (var i = 0; i < groupElements.length; i++)
								{
									minGroupIndex = Math.min(minGroupIndex, parseInt(groupElements[i].getAttribute('form-index')));
								}
								return minGroupIndex;
							}
							
							var minGroupIndex = getMinFormIndexGroup(ghostObject);
							var idx = -1;
							var maxElemIndex = -1;
							//find max form index selected elem overlapping the group
							for (var i = 0; i < elements.length -2 ; i ++)
							{
								if (elements[i].firstElementChild.getAttribute('group-id') == ghostObject.uuid) continue;
								var formIndex;
								if (editorScope.getGhost(elements[i].getAttribute("svy-id"))) 
								{
									formIndex = getMinFormIndexGroup(editorScope.getGhost(elements[i].getAttribute("svy-id")));
								}
								else
								{
									formIndex = parseInt(elements[i].firstElementChild.getAttribute('form-index'));
								}
								
								if (maxElemIndex < formIndex)
								{
									maxElemIndex = formIndex;
									idx = i;
								}
							}
							if (maxElemIndex > minGroupIndex)
							{
								if(returnNonSelectable && nonSelectableNode) {
									return [elements[idx], nonSelectableNode];
								}
								else return elements[idx];
							}
						}
							
						// always return the one on top (visible); this is due to formIndex implementation
						var el = null;
						for (var i = elements.length-1; i >= 0; i--) {
							if (!(angular.element(elements[i]).is("[svy-non-selectable]"))) {
								if (el == null || this.hasGreaterZIndex(elements[i], el))
								{
									el = elements[i];
								}
								if ($(el).hasClass("ghost")) break;//no need to continue, ghosts are always on top
							}
							else {
								nonSelectableNode = elements[i];
							}
						}
						return (returnNonSelectable && nonSelectableNode) ? [el, nonSelectableNode] : el;
					}

					return (returnNonSelectable && nonSelectableNode) ? [null, nonSelectableNode] : null;
				},
				
				isUnknownElement: function(element) {
					return Object.prototype.toString.call(element) === '[object HTMLUnknownElement]';
				},
				
				collectMatchedElements: function(matchedElements, fromList, p1, p2, percentage) {
					for (var i = 0; i < fromList.length; i++) {
						var element = fromList[i]
						var rect = element.getBoundingClientRect();
						if( (this.isUnknownElement(element) || !element.clientWidth && !element.clientHeight) && element.firstElementChild && !this.isUnknownElement(element.firstElementChild)) {
							rect = element.firstElementChild.getBoundingClientRect();
						}
						var left = rect.left;
						var top = rect.top;
						if (element.parentElement.parentElement == editorScope.glasspane) {
							top = top - element.parentElement.parentElement.getBoundingClientRect().top;
							left = left - element.parentElement.parentElement.getBoundingClientRect().left;
						}
						var clientWidth = rect.width != 0 ? rect.width : element.offsetWidth;
						var clientHeight = rect.height != 0 ? rect.height : element.offsetHeight;
						
						if (this.isRectanglesOverlap(p1, p2, percentage, top, left, clientHeight, clientWidth)) {
							matchedElements.push(element);
						}
						else if (parseInt(window.getComputedStyle(element, ":before").height) > 0) {
							var computedStyle = window.getComputedStyle(element, ":before");
							//the top and left positions of the before pseudo element are computed as the sum of:
							//top/left position of the element, padding Top/Left of the element and margin Top/Left of the pseudo element
							var topb = top + parseInt($(element).css('paddingTop')) + parseInt(computedStyle.marginTop);
							var leftb = left + parseInt($(element).css('paddingLeft')) + parseInt(computedStyle.marginLeft);
							var height = parseInt(computedStyle.height);
							var width = parseInt(computedStyle.width);
							if (this.isRectanglesOverlap(p1, p2, percentage, topb, leftb, height, width)) {
								matchedElements.push(element);
							}
						}
					}
				},
				
				isRectanglesOverlap: function(p1, p2, percentage, top, left, clientHeight, clientWidth) {
					if (percentage == undefined || percentage == 100) { //Element must be fully enclosed
						if (p1.top <= top && p1.left <= left && p2.top >= top + clientHeight && p2.left >= left + clientWidth) {
							return true;
						}
					} else {
						var overlapX = Math.max(0, Math.min(p2.left, left + clientWidth) - Math.max(p1.left, left))
						var overlapY = Math.max(0, Math.min(p2.top, top + clientHeight) - Math.max(p1.top, top))

						if (((clientWidth * clientHeight) / 100) * percentage < (overlapX * overlapY)) {
							return true;
						}
					}
					return false;
				},

				getElementsByRectangle: function(p1, p2, percentage, fromDoc, fromGlass, skipId, skipConversion, selectType) {
					var temp = 0;
					if (p1.left > p2.left) {
						var temp = p1.left;
						p1.left = p2.left;
						p2.left = temp;
					}
					if (p1.top > p2.top) {
						var temp = p1.top;
						p1.top = p2.top;
						p2.top = temp;
					}
					var nodes = [];
					var ghosts = [];
					if (fromDoc) {
						var querySelectorAllString = "[svy-id]:not([svy-id = '" + (skipId == undefined ? '' : skipId) + "'])" ;
						if(selectType == this.SELECT_COMPONENT) {
							querySelectorAllString += ":not([svy-layoutname])";
						}
						else if(selectType == this.SELECT_CONTAINER) {
							querySelectorAllString += "[svy-layoutname]";
						}
						nodes = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll(querySelectorAllString));
					}
					if (fromGlass)
						ghosts = Array.prototype.slice.call(editorScope.glasspane.querySelectorAll("[svy-id]:not([svy-id = '" + (skipId == undefined ? '' : skipId) + "'])"));
					
					var matchedFromDoc = [];
					var matchedFromGlass = [];
					this.collectMatchedElements(matchedFromGlass, ghosts, p1, p2, percentage);
					if (!skipConversion)
					{
						p1 = this.adjustForPadding(p1);
						p2 = this.adjustForPadding(p2);

					}	
					this.collectMatchedElements(matchedFromDoc, nodes, p1, p2, percentage);

					var concat = matchedFromDoc.concat(matchedFromGlass);
					return concat;
				},
				hasGreaterZIndex: function(e1, e2) {
					//we are not interested in the z index if one is contained in the other
					if (e1.contains(e2) || e2.contains(e1)) return false;
					
					//we will not search for the z-index property higher than the common parent
					var limit = $(e1).parents().has(e2).first();					
					return this.getZIndex($(e1), limit) > this.getZIndex($(e2), limit);					
				},
				getZIndex: function(el, limit) {
					var zindex = el.css("z-index");
					if (zindex !== "auto" && zindex !== "inherit" && zindex !== "initial")
					{
						return parseInt(zindex);
					}
					if (!el.parent()[0].isEqualNode(limit[0]))
					{
						return this.getZIndex(el.parent(), limit);
					}
					return -9999999;//Number.MIN_SAFE_INTEGER doesn't work in ie
				},
				setupTopContainers: function(packages) {
					editorScope.topContainers = [];
					for (var i = 0; i< packages.length; i++) {
						if (packages[i].components[0] && packages[i].components[0].componentType == "layout") {
							for (var j = 0; j < packages[i].components.length; j++) {
								if (packages[i].components[j].topContainer) {
									editorScope.topContainers.push(packages[i].packageName + "." + packages[i].components[j].layoutName);
								}
							}
						}
					}
				},
				isTopContainer: function(container) {
					for (var i = 0; i < editorScope.topContainers.length; i++) {
            			if (editorScope.topContainers[i] == container) {
                			return true;
            			}
    				}
    				return false;
				}
			}
		}
	}
});
