angular.module('mouseselection',['editor']).run(function($rootScope, $pluginRegistry,$selectionUtils){
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
					var elements = utils.getElementsByRectangle(p1,p2,1)
					editorScope.setSelection(elements);
				}
				else {
					editorScope.setSelection(node);
				}
			} else {
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
				event.preventDefault();
		}
		function onmouseup(event) {
				if (selectedNodeMouseEvent) {
					if (event.pageX == selectedNodeMouseEvent.pageX && event.pageY == selectedNodeMouseEvent.pageY) {
						var node = utils.getNode(event);
						select(event,node);
					}
				}
				selectedNodeMouseEvent = null;
				event.preventDefault();
		}

		function onmousedownLasso(event) {
			if(event.button == 0 && !lassoStarted) {
				var node = utils.getNode(event);
				if (!node) {
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
				var p1 = utils.adjustForPadding(mouseDownPosition);
				var p2 = utils.adjustForPadding(lassoMouseSelectPosition);
				var selectedElements = utils.getElementsByRectangle(p1,p2,1);
				editorScope.setSelection(selectedElements);
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
}).factory("$selectionUtils", function() {
	function hasClass(element, cls) {
		return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
	}

	return {
		getUtilsForScope: function(editorScope) {
			return {
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
					if (lassoStarted || hasClass(event.target,"contentframe-overlay")|| hasClass(event.target,"ghost")) {
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
				getNode: function(event) {
					var glassPaneMousePosition = this.getMousePosition(event);
					var glassPaneMousePosition1 = {top:glassPaneMousePosition.top + 1,left:glassPaneMousePosition.left + 1};
					var glassPaneMousePosition2 = {top:glassPaneMousePosition.top - 1,left:glassPaneMousePosition.left - 1};
					/*first get elements with glasspane coordinates 
					 * (this list may also contain real components - these need to be removed as they have a different coordinate system) */
					var glassPaneElements = this.getElementsByRectangle(glassPaneMousePosition1,glassPaneMousePosition2,0.001);
					//adjust for padding (added by phone and tablet canvas size) so that we have the correct position to get elements from the form
					var p = this.adjustForPadding(glassPaneMousePosition);
					var p2 = {top:p.top + 1,left:p.left + 1}
					p.top = p.top-1
					p.left = p.left-1;
					/*now get elements from the form 
					 * (this list may also contain elements from the glasspane - these need to be removed as they have a different coordinate system)*/
					var elements = this.getElementsByRectangle(p,p2,0.001);
					
					function isOnGlassPane (element) {
						return element.parentElement.parentElement == editorScope.glasspane;
					};
					function isNotOnGlassPane (element) {
						return element.parentElement.parentElement !== editorScope.glasspane;
					};
					elements = elements.filter(isNotOnGlassPane);
					glassPaneElements = glassPaneElements.filter(isOnGlassPane);
					
					/*unify the lists*/
					if (glassPaneElements) {
						if (glassPaneElements.length > 0 ){
							elements = elements.concat(glassPaneElements)
						}
					}
						
					if (elements.length == 1) return elements[0];
					else if (elements.length > 1) {
						var smallest = elements[0];
						var pixels = smallest.clientWidth * smallest.clientHeight;
						for(var i=1;i<elements.length;i++) {
							var el = elements[i];
							var elPixels = el.clientWidth * el.clientHeight;
							if (elPixels < pixels) {
								pixels = elPixels;
								smallest = el;
							}
							else if (elPixels == pixels && el.parentElement == smallest)
							{
								//select child if same size as parent
								smallest = el;
							}
						}
						return smallest;
					}
					
					return null;
				},
				getElementsByRectangle: function(p1, p2, percentage) {
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
					var nodes = Array.prototype.slice.call(editorScope.contentDocument.querySelectorAll("[svy-id]"));
					var ghosts = Array.prototype.slice.call(editorScope.glasspane.querySelectorAll("[svy-id]"));
					var concat = nodes.concat(ghosts);
					var matchedElements = []
					for (var i = 0; i < concat.length; i++) {
						var element = concat[i]
						var rect = element.getBoundingClientRect();
						var left = rect.left;
						var top = rect.top;
						
						if (element.parentElement.parentElement == editorScope.glasspane) {
							top = top - element.parentElement.parentElement.getBoundingClientRect().top;
							left = left - element.parentElement.parentElement.getBoundingClientRect().left;
						}

						if (percentage == undefined || percentage == 100) { //Element must be fully enclosed
							if (p1.top <= top && p1.left <= left && p2.top >= top + element.clientHeight && p2.left >= left + element.clientWidth) {
								matchedElements.push(element)
							}
						} else {
							var overlapX = Math.max(0, Math.min(p2.left, left + element.clientWidth) - Math.max(p1.left, left))
							var overlapY = Math.max(0, Math.min(p2.top, top + element.clientHeight) - Math.max(p1.top, top))

							if ( ( (element.clientWidth * element.clientHeight) / 100) * percentage < (overlapX * overlapY)) {
								matchedElements.push(element)
							}
						}
					}
					return matchedElements
				}
			}
		}
	}
});
