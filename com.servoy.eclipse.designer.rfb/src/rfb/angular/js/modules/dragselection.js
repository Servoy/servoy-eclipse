angular.module('dragselection',['editor']).run(function($rootScope, $pluginRegistry, $editorService)
{
	$pluginRegistry.registerPlugin(function(editorScope) {
		
		function onmousedown(event) {
			if (getNode(event)){
				dragStartEvent = event;
			}
				
		}
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
		
		function onmouseup(event) {
			dragStartEvent = null;
			if (dragging) {
				dragging = false;
				// store the position changes
				var selection = editorScope.getSelection();
				var formState = editorScope.getFormState();
				var obj = {};
				for(var i=0;i<selection.length;i++) {
					var node = selection[i];
					var name = node.getAttribute("name");
					var beanModel = formState.model[name];
					beanModel.location.y;
					beanModel.location.x 
					obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y}
				}
				$editorService.sendChanges(obj)
			}
		}
		var dragging = false;
		var dragStartEvent = null;
		function onmousemove(event) {
			var selection = editorScope.getSelection();
			if (dragStartEvent && selection.length > 0) {
				if (!dragging) {
					if ( Math.abs(dragStartEvent.screenX- event.screenX) > 3  || Math.abs(dragStartEvent.screenY- event.screenY) > 3) {
						dragging = true;
					}
				}
				if (dragging) {
					var formState = editorScope.getFormState();
					if (formState) {
						var changeX = event.screenX- dragStartEvent.screenX;
						var changeY = event.screenY- dragStartEvent.screenY;
						for(var i=0;i<selection.length;i++) {
							var node = selection[i];
							var name = node.getAttribute("name");
							var beanModel = formState.model[name];
							beanModel.location.y = beanModel.location.y + changeY;
							beanModel.location.x = beanModel.location.x + changeX;
						}
						dragStartEvent = event;
						editorScope.refreshEditorContent();
					}
				}
			}
		}
		
		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","FORM", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","FORM", onmouseup); // real selection in editor content iframe
		editorScope.registerDOMEvent("mousemove","FORM", onmousemove); // real selection in editor content iframe
		
	})
});
	