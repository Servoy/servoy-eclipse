angular.module('mouseselection',['editor']).run(function($rootScope, $pluginRegistry){
	
	$pluginRegistry.registerPlugin(function(editorScope) {
		var selectedNodeMouseEvent;
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
		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","FORM", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","FORM", onmouseup); // real selection in editor content iframe
	})
});