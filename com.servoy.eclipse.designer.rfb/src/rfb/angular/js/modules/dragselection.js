angular.module('dragselection',['mouseselection']).run(function($rootScope, $pluginRegistry, $editorService,$selectionUtils)
		{
	$pluginRegistry.registerPlugin(function(editorScope) {

		var utils = $selectionUtils.getUtilsForScope(editorScope);
		var dragging = false;
		var dragStartEvent = null;
		var selectionToDrag = null;

		function onmousedown(event) {
			if (utils.getNode(event)){
				dragStartEvent = event;
			}

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
					if (beanModel){
						beanModel.location.y;
						beanModel.location.x
						obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y}
					}
					else {
						var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
						obj[node.getAttribute("svy-id")] = {x:ghostObject.location.x,y:ghostObject.location.y}
					}
				}
				$editorService.sendChanges(obj);
			}
			selectionToDrag = null;
		}

		function onmousemove(event) {
			if (dragStartEvent) {
				if (!selectionToDrag) {
					selectionToDrag = editorScope.getSelection();
					var addToSelection = [];
					for(var i=0;i<selectionToDrag.length;i++) {
						var node = selectionToDrag[i];
						var ghostsForNode = editorScope.getContainedGhosts(node.getAttribute("svy-id"));
						if (ghostsForNode)
							addToSelection = addToSelection.concat(ghostsForNode);
					}
					selectionToDrag = selectionToDrag.concat(addToSelection);
				}
				if (selectionToDrag.length > 0) {
					if (!dragging) {
						if ( Math.abs(dragStartEvent.screenX- event.screenX) > 0  || Math.abs(dragStartEvent.screenY- event.screenY) > 0) {
							dragging = true;
						}
					}
					if (dragging) {
						var formState = editorScope.getFormState();
						if (formState) {
							var changeX = event.screenX- dragStartEvent.screenX;
							var changeY = event.screenY- dragStartEvent.screenY;
							for(var i=0;i<selectionToDrag.length;i++) {
								var node = selectionToDrag[i];
								if (node.uuid) {
									node.location.x += changeX;
									node.location.y += changeY;
								}
								else {
									var name = node.getAttribute("name");
									var beanModel = formState.model[name];
									if (beanModel){
										beanModel.location.y = beanModel.location.y + changeY;
										beanModel.location.x = beanModel.location.x + changeX;
									}
									else 
									{
										var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
										ghostObject.location.x += changeX;
										ghostObject.location.y += changeY;
									}
								}
								editorScope.refreshEditorContent();
							}
							dragStartEvent = event;
						}
					}
				}
			}

		}

		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe
		editorScope.registerDOMEvent("mousemove","CONTENTFRAME_OVERLAY", onmousemove); // real selection in editor content iframe

	})
		});
