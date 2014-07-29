angular.module('mouseselection',['editor']).run(function($rootScope, $pluginRegistry){
	
	$pluginRegistry.registerPlugin(function(editorScope) {
		var lastMouseDownEvent;
		function getNode(event) {
			var node = null;
			var element = event.target;
			do
			{
				if (element.hasAttribute("svy-model") && element.hasAttribute("name")) {
					node = element;
					// TODO search for the most top level?
					// but if we have layout components in designer then we do need to select the nested.
				}
				element = element.parentElement;
			} while(element)
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
				//TODO: implement
				/*
				 * Shift-Click does range select: all elements within the box defined by the uttermost top/left point of selected elements
				 * to the uttermost bottom-right of the clicked element
				 */
			} else {
				editorScope.setSelection(node)
			}
		}
		function onmousedown(event) {
			var node = getNode(event);
			if (node) {
				if (editorScope.getSelection().indexOf(node) !== -1) {
					// its in the current selection, remember this for mouse up.
					lastMouseDownEvent = event;
				}
				else select(event,node);
			}
			else {
				editorScope.setSelection([])
			}
			event.preventDefault();
		}
		function onmouseup(event) {
			if (lastMouseDownEvent) {
				if (event.pageX == lastMouseDownEvent.pageX && event.pageY == lastMouseDownEvent.pageY) {
					var node = getNode(event);
					select(event,node);
				}
			}
			lastMouseDownEvent = null;
			event.preventDefault();
		}
		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","FORM", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","FORM", onmouseup); // real selection in editor content iframe
	})
});