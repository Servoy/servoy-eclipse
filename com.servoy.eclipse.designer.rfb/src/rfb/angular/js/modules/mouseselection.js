angular.module('mouseselection',['editor']).run(function($rootScope, $pluginRegistry){

	$pluginRegistry.registerPlugin(function(editorScope) {
		var selectedNodeMouseEvent;
		var lassoStarted = false;
		var mouseDownPosition = {"left":-1, "top":-1};
		var lassoDiv = editorScope.glasspane.firstElementChild

		function getNode(event) {
			var node = null;
			var element = event.target;
			do
			{
				if (element.hasAttribute("svy-id")) {
					node = element;
					// TODO do we really need to search for the most top level?
					// but if we have layout components in designer then we do need to select the nested.
				}
				element = element.parentNode;
			} while(element && element.hasAttribute)
				return node;
		}
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
					var elements = getElementsByRectangle(p1,p2,1)
					editorScope.setSelection(elements);
				}
				else {
					editorScope.setSelection(node);
				}
			} else {
				editorScope.setSelection(node);
			}
		}
		function getElementsByRectangle(p1, p2, percentage) {
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
			var nodes = editorScope.contentDocument.querySelectorAll("[svy-id]")
			var matchedElements = []
			for (var i = 0; i < nodes.length; i++) {
				var element = nodes[i]
				var rect = element.getBoundingClientRect();

				if (percentage == undefined || percentage == 100) { //Element must be fully enclosed
					if (p1.top <= rect.top && p1.left <= rect.left && p2.top >= rect.top + element.clientHeight && p2.left >= rect.left + element.clientWidth) {
						matchedElements.push(element)
					}
				} else {
					overlapX = Math.max(0, Math.min(p2.left, rect.left + element.clientWidth) - Math.max(p1.left, rect.left))
					overlapY = Math.max(0, Math.min(p2.top, rect.top + element.clientHeight) - Math.max(p1.top, rect.top))

					if ( ( (element.clientWidth * element.clientHeight) / 100) * percentage < (overlapX * overlapY)) {
						matchedElements.push(element)
					}
				}
			}
			return matchedElements
		}
		function onmousedown(event) {
			var node = getNode(event);
			if (node) {
				if (editorScope.getSelection().indexOf(node) !== -1) {
					// its in the current selection, remember this for mouse up.
					selectedNodeMouseEvent = event;
				}
				else select(event,node);
			}
			else {
				editorScope.setSelection([])
			}
			event.preventDefault();
		}
		function onmouseup(event) {
			if (selectedNodeMouseEvent) {
				if (event.pageX == selectedNodeMouseEvent.pageX && event.pageY == selectedNodeMouseEvent.pageY) {
					var node = getNode(event);
					select(event,node);
				}
			}
			selectedNodeMouseEvent = null;
			event.preventDefault();
		}

		function onmousedownLasso(event) {
			var node = getNode(event);
			if (!node) {
				startLasso(event);
			}
		}

		function onmouseupLasso(event) {
			stopLasso(event);
		}

		function startLasso(event) {
			mouseDownPosition = getMousePosition(event);
			lassoDiv.style.left = mouseDownPosition.left + 'px';
			lassoDiv.style.top = mouseDownPosition.top + 'px';
			editorScope.moveGlasspaneAbove();
			lassoStarted = true;
		}

		function stopLasso(event) {
			if (lassoStarted) {
				editorScope.moveGlasspaneBelow();
				var lassoMouseSelectPosition = getMousePosition(event);
				var p1 = adjustForPadding(mouseDownPosition);
				var p2 = adjustForPadding(lassoMouseSelectPosition);
				var selectedElements = getElementsByRectangle(p1,p2,100);
				editorScope.setSelection(selectedElements);
				lassoStarted = false;
				lassoDiv.style.width = '0px';
				lassoDiv.style.height = '0px';
			}
		}

		function adjustForPadding(mousePosition) {
			mousePosition.left -= parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px",""));
			mousePosition.top  -= parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px",""));
			return mousePosition;
		}

		function getMousePosition(event) {
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
			if (lassoStarted || hasClass(event.target,"contentframe-overlay")) {
				xMouseDown -= editorScope.glasspane.parentElement.offsetLeft;
				yMouseDown -= editorScope.glasspane.parentElement.offsetTop;
				xMouseDown += editorScope.glasspane.parentElement.scrollLeft;
				yMouseDown += editorScope.glasspane.parentElement.scrollTop;
			}
			else if (!hasClass(event.target,"contentframe-overlay")){
				xMouseDown += parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px",""));
				yMouseDown += parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px",""));
			}
			return {"left":xMouseDown, "top":yMouseDown};
		}

		function hasClass(element, cls) {
			return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
		}

		function onmousemove(event) {
			if (lassoStarted) {
				mouseMovePosition = getMousePosition(event);
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
			}
		}

		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","FORM", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","FORM", onmouseup); // real selection in editor content iframe

		editorScope.registerDOMEvent("mousedown","FORM", onmousedownLasso); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","FORM", onmouseupLasso); // real selection in editor content iframe
		editorScope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", onmousedownLasso); 
		editorScope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", onmouseupLasso); 
		editorScope.registerDOMEvent("mousemove","CONTENTFRAME_OVERLAY", onmousemove); 

	})
});