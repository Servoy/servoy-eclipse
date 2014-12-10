angular.module('highlight', ['editor']).run(function($pluginRegistry, $editorService, $selectionUtils,$timeout) {
	
	$pluginRegistry.registerPlugin(function(editorScope) {
		var utils = $selectionUtils.getUtilsForScope(editorScope);
		
		var highlightDiv = angular.element(document.querySelector('#highlight'))[0];
		var event = null;
		var enabled = true;
		var execute = null;
		
		function shouldDrawIfDragging(dropTarget) {
			if (utils.getDraggingFromPallete() != null) {
				if (!editorScope.isAbsoluteFormLayout())
				{
					// always draw for flow layout
					return true;
				}
				var draggedItem = utils.getDraggingFromPallete();
				return ((dropTarget.getAttribute("svy-types") != null) && (dropTarget.getAttribute("svy-types").indexOf(draggedItem) > 0)) 
			}
			return true;
		}
		
		function drawHighlightDiv() {
			var node = utils.getNode(event);
			if (node && enabled && shouldDrawIfDragging(node)) {
				if (node.parentElement.parentElement !== editorScope.glasspane) {
					highlightDiv.style.display = 'block';
					var rect = node.getBoundingClientRect();
					var left = rect.left;
					var top = rect.top;
					top = top + parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-top").replace("px",""));
					left = left + parseInt(angular.element(editorScope.glasspane.parentElement).css("padding-left").replace("px",""));
					highlightDiv.style.left = left + 'px';
					highlightDiv.style.top = top + 'px';
					highlightDiv.style.height = rect.height + 'px';
					highlightDiv.style.width = rect.width + 'px';
				}
				else
					highlightDiv.style.display = 'none';
			}
			else
				highlightDiv.style.display = 'none';
		}
		
		function disableHighlightDiv(){
			highlightDiv.style.display = 'none';
			enabled = false;
		}
		function enableHighlightDiv(){
			enabled = true;
		}

		function onmousemove(e) {
			if (execute)
				$timeout.cancel(execute);
			event = e;
			execute = $timeout(drawHighlightDiv,10);
		}
		
		editorScope.registerDOMEvent("mousemove","CONTENTFRAME_OVERLAY", onmousemove); // real selection in editor content iframe
		editorScope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", disableHighlightDiv); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", enableHighlightDiv); // real selection in editor content iframe
	});
});