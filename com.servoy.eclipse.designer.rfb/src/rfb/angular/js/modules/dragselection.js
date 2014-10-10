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
				
				if (event.ctrlKey)
				{
					var components = [];
					var size = 0;
					for (var i = 0; i < selectionToDrag.length; i++)
					{
						selectionToDrag[i].remove();
						var node = selectionToDrag[i][0];
						var component = {};
						component.uuid = node.getAttribute('cloneuuid');
						component.x = node.location.x;
						component.y = node.location.y;
						if (component.x > 0 && component.y > 0)
						{
							components[size++] = component;
						}
						
					}
					if (size > 0) $editorService.createComponents({"components": components}); 
				}
				else
				{
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
			}
			selectionToDrag = null;
		}

		function isGhostAlreadySelected(selection, ghost) {
			for(var i=0; i < selection.length; i++) {
				if (selection[i].getAttribute("svy-id") == ghost.uuid) return true;
			}
			return false;
		}
		
		function onmousemove(event) {
			if (dragStartEvent) {
				
				if (!dragging) {
					if ( Math.abs(dragStartEvent.screenX- event.screenX) > 0  || Math.abs(dragStartEvent.screenY- event.screenY) > 0) {
						dragging = true;
					}
				}
				
				if (event.ctrlKey && selectionToDrag == null)
				{
					selectionToDrag = [];
					var selection = editorScope.getSelection();
					var formState = editorScope.getFormState();
					for(var i = 0; i < selection.length; i++) 
					{
						var node = selection[i];
						selectionToDrag[i] = $(selection[i]).clone();
						var posX, posY;
						if (node.uuid) {
							posX = node.location.x;
							posY = node.location.y;
						}
						else {
							var name = node.getAttribute("name");
							var beanModel = formState.model[name];
							if (beanModel){
								posX = beanModel.location.x;
								posY = beanModel.location.y;
							}
							else 
							{
								var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
								posX = ghostObject.location.x;
								posY = ghostObject.location.y;
							}

							selectionToDrag[i] = $(node).clone();
							selectionToDrag[i].attr('id', 'dragNode'+i);
							selectionToDrag[i].attr('cloneuuid', node.getAttribute("svy-id"));
							selectionToDrag[i][0]['location'] = {x: posX, y:posY}; 
							selectionToDrag[i].css({'z-index': 4});
							$(selection[i]).parent().append(selectionToDrag[i]);
						}
					} 
				}
				
				if (!selectionToDrag) {
					selectionToDrag = editorScope.getSelection();
					var addToSelection = [];

					for(var i=0;i<selectionToDrag.length;i++) {
						var node = selectionToDrag[i];
						var ghostsForNode = editorScope.getContainedGhosts(node.getAttribute("svy-id"));
						if (ghostsForNode){
							for(var j=0; j < ghostsForNode.length; j++) {
								if(!isGhostAlreadySelected(selectionToDrag, ghostsForNode[j]))
									addToSelection.push(ghostsForNode[j]);
							}
						}
					}
					selectionToDrag = selectionToDrag.concat(addToSelection);
				}

				if (selectionToDrag.length > 0) {
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
								else if (node[0] && node[0].getAttribute('cloneuuid'))
								{
									node[0].location.x += changeX;
									node[0].location.y += changeY;
									var css = { top: node[0].location.y, left: node[0].location.x }
									node.css(css);
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
										editorScope.updateGhostLocation(ghostObject, ghostObject.location.x + changeX, ghostObject.location.y + changeY)
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
		
		$('body').keyup(function(event){
			//if control is released during drag, the copy is deleted and selected element must be moved
			if (dragStartEvent && dragStartEvent.ctrlKey && event.which == 17)
			{
				for (var i = 0; i < selectionToDrag.length; i++)
				{
					selectionToDrag[i].remove();
				}
				selectionToDrag = editorScope.getSelection();
			}
		 });
		
		// register event on editor form iframe (see register event in the editor.js)
		editorScope.registerDOMEvent("mousedown","CONTENTFRAME_OVERLAY", onmousedown); // real selection in editor content iframe
		editorScope.registerDOMEvent("mouseup","CONTENTFRAME_OVERLAY", onmouseup); // real selection in editor content iframe
		editorScope.registerDOMEvent("mousemove","CONTENTFRAME_OVERLAY", onmousemove); // real selection in editor content iframe
		
	})
		});
