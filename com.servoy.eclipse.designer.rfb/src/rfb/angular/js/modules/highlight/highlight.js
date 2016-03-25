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
			if (node && enabled && shouldDrawIfDragging(node) && !editorScope.highlight) {
				if (node.parentElement != undefined && node.parentElement.parentElement !== editorScope.glasspane) {
					if (node.clientWidth == 0 && node.clientHeight == 0 && node.firstChild) node = node.firstChild;
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
					//get to the first dom element that is a servoy component or layoutContainer
					while (node.parentElement && !node.getAttribute("svy-id")) node = node.parentElement;
					if (!angular.element(node).hasClass("inheritedElement")) {
							if (!utils.getDraggingFromPallete())
							{
								highlightDiv.style.cursor = "pointer";
							}	
							highlightDiv.style.outline = "";
					}
					else {
							highlightDiv.style.cursor="";
							highlightDiv.style.outline = "1px solid #FFBBBB";
					}
				}
				else {
					highlightDiv.style.display = 'none';
					highlightDiv.style.cursor="";
					highlightDiv.style.outline = "";
				}
			}
			else {
				highlightDiv.style.display = 'none';
				highlightDiv.style.cursor="";
				highlightDiv.style.outline = "";
			}
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