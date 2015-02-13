angular.module('mouseselection',['editor']).run(function($rootScope, $pluginRegistry,$selectionUtils,EDITOR_CONSTANTS){
	$pluginRegistry.registerPlugin(function(editorScope) {
		var selectedNodeMouseEvent;
		var lassoStarted = false;
		var mouseDownPosition = {"left":-1, "top":-1};
		var lassoDiv = editorScope.glasspane.firstElementChild
		var utils = $selectionUtils.getUtilsForScope(editorScope);
		
		function select(event, node) {
			if (event.ctrlKey) {
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
				var selection = editorScope.getSelection();
				if (selection.length > 0) {
					var rec = node.getBoundingClientRect();
					var p1 = {top:rec.top,left:rec.left}
					var p2 = {top:rec.bottom,left:rec.right}
					for(var i=0;i<selection.length;i++) {
						var rect = selection[i].getBoundingClientRect();
						p1 = {top:Math.min(p1.top, rect.top),left:Math.min(p1.left, rect.left)}
						p2 = {top:Math.max(p2.top, rect.bottom),left:Math.max(p2.left, rect.right)}
					}
					var elements = utils.getElementsByRectangle(p1,p2,1,true,true)
					editorScope.setSelection(elements);
				}
				else {
					editorScope.setSelection(node);
				}
			} 
			else if (event.button == 2 && editorScope.getSelection().indexOf(node) !== -1 && editorScope.getSelection().length > 1)
			{
				// if we right click on selected element while multiple selection,just show context menu and do not modify selection
				return;
			}
			else {
				editorScope.setSelection(node);
			}
		}
		function onmousedown(event) {
				var node = utils.getNode(event);
				if (node) {
					if (editorScope.getSelection().indexOf(node) !== -1) {
						// its in the current selection, remember this for mouse up.
						selectedNodeMouseEvent = event;
					}
					else select(event,node);
				}
				else {
					if(event.button == 0)
					{
						editorScope.setSelection([])
					}
				}
		}
		function onmouseup(event) {
				if (selectedNodeMouseEvent) {
					if (event.pageX == selectedNodeMouseEvent.pageX && event.pageY == selectedNodeMouseEvent.pageY) {
						var node = utils.getNode(event);
						select(event,node);
					}
				}
				selectedNodeMouseEvent = null;
		}

		function onmousedownLasso(event) {
			if(event.button == 0 && !lassoStarted) {
				var node = utils.getNode(event);
				var canStartLasso = !node;
				if(node) {
					var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
					canStartLasso = (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM);
				}
				
				if(canStartLasso) {
					startLasso(event);
				}
			}
		}

		function onmouseupLasso(event) {
			if(event.button == 0)
			{
				stopLasso(event);
			}
		}

		function startLasso(event) {
			mouseDownPosition = utils.getMousePosition(event,lassoStarted);
			lassoDiv.style.left = mouseDownPosition.left + 'px';
			lassoDiv.style.top = mouseDownPosition.top + 'px';
			lassoStarted = true;
		}

		function stopLasso(event) {
			if (lassoStarted) {
				var lassoMouseSelectPosition = utils.getMousePosition(event,lassoStarted);
				var p1 = mouseDownPosition;
				var p2 = lassoMouseSelectPosition;
				if(Math.abs(p1.left - p2.left) > 1 && Math.abs(p1.top - p2.top) > 1) {
					var selectedElements = utils.getElementsByRectangle(p1,p2,100,true,true);
					editorScope.setSelection(selectedElements);	
				}
				lassoStarted = false;
				lassoDiv.style.display = 'none';
			}
		}
		function onmousemove(event) {
			if (lassoStarted) {
				mouseMovePosition = utils.getMousePosition(event,lassoStarted);
				if (mouseMovePosition.left < mouseDownPosition.left){
					lassoDiv.style.left = mouseMovePosition.left + 'px';
				}
				if (mouseMovePosition.top < mouseDownPosition.top){
					lassoDiv.style.top = mouseMovePosition.top +'px'
				}
				var currentWidth = mouseMovePosition.left - mouseDownPosition.left;
				var currentHeight = mouseMovePosition.top - mouseDownPosition.top;
				lassoDiv.style.width = Math.abs(currentWidth) + 'px';
				lassoDiv.style.height = Math.abs(currentHeight) + 'px';
				lassoDiv.style.display = 'block';
			}
		}
		
		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe

		editorScope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", onmousedownLasso); 
		editorScope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", onmouseupLasso); 
		editorScope.registerDOMEvent("mousemove","CONTENTFRAME_OVERLAY", onmousemove);
	})
}).factory("$selectionUtils", function(EDITOR_CONSTANTS) {
	function hasClass(element, cls) {
		return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
	}
	
	function isGhostAlreadySelected(selection, ghost) {
		for(var i=0; i < selection.length; i++) {
			if (selection[i].getAttribute("svy-id") == ghost.getAttribute("svy-id")) return true;
		}
		return false;
	}
	
	
	var draggingFromPallete = null;

	return {
		getUtilsForScope: function(editorScope) {
			return {
				
				addGhostsToSelection: function(selection) {
					
					var addToSelection = [];

					for(var i=0;i<selection.length;i++) {
						var node = selection[i];
						var ghostsForNode = editorScope.getContainedGhosts(node.getAttribute("svy-id"));
						if (ghostsForNode){
							for(var j=0; j < ghostsForNode.length; j++) {
								var ghost = $('[svy-id='+ghostsForNode[j].uuid+']')[0]
								if(!isGhostAlreadySelected(selection, ghost))
									addToSelection.push(ghost);
							}
						}
					}
					selection = selection.concat(addToSelection);
					return selection;
				},
				
				getDropNode: function(type,topContainer,layoutName,event) {
					var dropTarget = null;
					if (type == "layout" || (type == "component" && !editorScope.isAbsoluteFormLayout())) {
						var realName = layoutName?layoutName:"component";
						
						console.log("type: " + type +",parents: " + topContainer + ", lname:" + layoutName)
						dropTarget = this.getNode(event, true);
						if (!dropTarget){
							// this is on the form, can this layout container be dropped on the form?
							if (!topContainer){
								return {dropAllowed:false};
							}
							return {dropAllowed:true,dropTarget:null};
						}
						else {
							function getParent(dt) {
								if (!dt || !dt[0]) return null;
								var allowedChildren = dt[0].getAttribute("svy-allowed-children");
								if (!allowedChildren || !(allowedChildren.indexOf(realName) > 0))
								{
									return getParent( $(dt).parent("[svy-id]")); // the drop target doesn't allow this layout container type
								}
								return dt;
							}
							var realDropTarget = getParent($(dropTarget));
							if (realDropTarget == null) {
								return {dropAllowed:false};
							}
							else if (realDropTarget[0] != dropTarget) {
								var clientRec = dropTarget.getBoundingClientRect();
								var mousePos = this.getMousePosition(event);
								var bottomPixels = (clientRec.bottom - clientRec.top) * 0.3;
								var rightPixels = (clientRec.right - clientRec.left) * 0.3;
								if (mousePos.top > (clientRec.bottom - bottomPixels) || mousePos.left > (clientRec.right - rightPixels)) {
									// this is in the 30% corner (bottom or right) of the component
									// the beforeChild should be a sibling of the dropTarget (or empty if it is the last)
									dropTarget = dropTarget.nextElementSibling;
									// if there is no nextElementSibling then force it to append so that it is moved to the last position.
									if (!dropTarget) return {dropAllowed:true,dropTarget:realDropTarget[0],append:true};
								}
								return {dropAllowed:true,dropTarget:realDropTarget[0],beforeChild:dropTarget};
							}
						}
					}
					else if (type != "component"){
						dropTarget = this.getNode(event);
						if (dropTarget && dropTarget.getAttribute("svy-types")){
							if (dropTarget.getAttribute("svy-types").indexOf(type) <= 0)
								return {dropAllowed:false}; // the drop target doesn't suppor this type
						}
						else return {dropAllowed:false}; // ghost has no drop target or the drop target doesnt support any types
					}
					else {
						dropTarget = this.getNode(event, true);
					}
					return {dropAllowed:true,dropTarget:dropTarget};
				},
				
				setDraggingFromPallete: function(dragging){
					draggingFromPallete = dragging;
					if (!editorScope.isAbsoluteFormLayout())
					{
						// TODO this is bootstrap layout specific, should be based on svy-xx attribute?
						var rows = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll(".row"));
						if (dragging)
						{
							//drag started
							for (var i = 0; i < rows.length; i++) {
								$(rows[i]).addClass('rowDesign');
							}
						}
						else
						{
							//drag end
							for (var i = 0; i < rows.length; i++) {
								$(rows[i]).removeClass('rowDesign');
							}
						}
						editorScope.refreshEditorContent();
					}
				},
				getDraggingFromPallete: function(){
					return draggingFromPallete;
				},
				
				adjustForPadding:function (mousePosition) {
					mousePosition.left -= parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px",""));
					mousePosition.top  -= parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px",""));
					return mousePosition;
				},
				
				getMousePosition: function(event,lassoStarted) {
					var xMouseDown = -1;
					var yMouseDown = -1;
					if (event.pageX || event.pageY){
						xMouseDown = event.pageX;
						yMouseDown = event.pageY;
					}
					else 
						if (event.clientX || event.clientY){
							xMouseDown = event.clientX;
							yMouseDown = event.clientY;			
						}
					if (lassoStarted || hasClass(event.target,"contentframe-overlay") || hasClass(event.target,"ghost") || event.target.id == "highlight") {
						xMouseDown -= editorScope.glasspane.parentElement.offsetLeft;
						yMouseDown -= editorScope.glasspane.parentElement.offsetTop;
						xMouseDown += editorScope.glasspane.parentElement.scrollLeft;
						yMouseDown += editorScope.glasspane.parentElement.scrollTop;
					}
					else if (!hasClass(event.target,"contentframe-overlay") && !hasClass(event.target,"ghost")){
						xMouseDown += parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px",""));
						yMouseDown += parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px",""));
					}
					return {"left":xMouseDown, "top":yMouseDown};
				},
				getNode: function(event, skipGlass) {
					var glassPaneMousePosition = this.getMousePosition(event);
					var glassPaneMousePosition1 = {top:glassPaneMousePosition.top + 1,left:glassPaneMousePosition.left + 1};
					var glassPaneMousePosition2 = {top:glassPaneMousePosition.top - 1,left:glassPaneMousePosition.left - 1};
					var elements = this.getElementsByRectangle(glassPaneMousePosition1,glassPaneMousePosition2,0.000001,true,!skipGlass);
						
					if (elements.length == 1)
						return elements[0];
					else 
						if (elements.length > 1) {
							var node = elements[elements.length-1];
							var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
							if (ghostObject && ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM && !(angular.element(elements[elements.length-2]).is("[svy-non-selectable]"))) {
								return elements[elements.length-2];
							}
							// always return the one on top (visible); this is due to formIndex implementation
							return elements[elements.length-1]
						}
					
					return null;
				},

				collectMatchedElements: function (matchedElements, fromList, p1, p2, percentage) {
					for (var i = 0; i < fromList.length; i++) {
						var element = fromList[i]
						var rect = element.getBoundingClientRect();
						var left = rect.left;
						var top = rect.top;
						
						if (element.parentElement.parentElement == editorScope.glasspane) {
							top = top - element.parentElement.parentElement.getBoundingClientRect().top;
							left = left - element.parentElement.parentElement.getBoundingClientRect().left;
						}

						var clientWidth = element.clientWidth;
						if (clientWidth == 0)
						{
							if ( element.firstChild &&  element.firstChild.clientWidth > 0)
							{
								clientWidth = element.firstChild.clientWidth
							}
							else
							{
								clientWidth = element.offsetWidth;
							}	
						}	
						var clientHeight = element.clientHeight;
						if (clientHeight == 0)
						{
							if ( element.firstChild &&  element.firstChild.clientHeight > 0)
							{
								clientHeight = element.firstChild.clientHeight
							}
							else
							{
								clientHeight = element.offsetHeight;
							}	
						}
						
						if (percentage == undefined || percentage == 100) { //Element must be fully enclosed
							if (p1.top <= top && p1.left <= left && p2.top >= top + clientHeight && p2.left >= left + clientWidth) {
								matchedElements.push(element)
							}
						} else {
							var overlapX = Math.max(0, Math.min(p2.left, left + clientWidth) - Math.max(p1.left, left))
							var overlapY = Math.max(0, Math.min(p2.top, top + clientHeight) - Math.max(p1.top, top))

							if ( ( (clientWidth * clientHeight) / 100) * percentage < (overlapX * overlapY)) {
								matchedElements.push(element)
							}
						}
					}
				},
				
				getElementsByRectangle: function(p1, p2, percentage, fromDoc, fromGlass) {
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
					if (fromDoc)
						nodes = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[svy-id]"));
					
					if (fromGlass)
						ghosts = Array.prototype.slice.call(editorScope.glasspane.querySelectorAll("[svy-id]"));
					
					var matchedFromDoc = [];
					var matchedFromGlass = [];
					this.collectMatchedElements(matchedFromGlass, ghosts, p1, p2, percentage);
					p1 = this.adjustForPadding(p1);
					p2 = this.adjustForPadding(p2);
					this.collectMatchedElements(matchedFromDoc, nodes, p1, p2, percentage);
					
					var concat = matchedFromDoc.concat(matchedFromGlass);
					return concat;
				},
			}
		}
	}
});
