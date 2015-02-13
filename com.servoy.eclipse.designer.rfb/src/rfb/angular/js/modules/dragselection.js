angular.module('dragselection',['mouseselection']).run(function($rootScope, $pluginRegistry, $editorService,$selectionUtils)
		{
	$pluginRegistry.registerPlugin(function(editorScope) {

		var utils = $selectionUtils.getUtilsForScope(editorScope);
		var dragging = false;
		var dragStartEvent = null;
		var selectionToDrag = null;
		var COMPONENT_TYPE = 7;

		function onmousedown(event) {
			if (event.button == 0 && utils.getNode(event)){
				dragStartEvent = event;
			}
		}

		function onmouseup(event) {
			if(event.button == 0)
			{
				dragStartEvent = null;
				if (dragging) {
					utils.setDraggingFromPallete(null);
					dragging = false;
					// store the position changes
					var selection = editorScope.getSelection();

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
						if (!editorScope.isAbsoluteFormLayout()) {
							var obj = {};
							for (var i=0; i < selectionToDrag.length; i++) {
								var node = selectionToDrag[i];
								var type = "component";
								var topContainer = null;
								var layoutName = null;
								var canDrop = utils.getDropNode(type, topContainer,layoutName,event);
								if (!canDrop.dropAllowed)  {
									// full refresh the editor content, it can be moved to different places already.
									// TODO this is not enough
									editorScope.refreshEditorContent();
									continue;
								}
								if (canDrop.dropAllowed && !canDrop.beforeChild) {
									canDrop.beforeChild = node.nextElementSibling;
								}
								
								var key = node.uuid;
								if (!key) {
									key = node.getAttribute("svy-id");
								}
							
								obj[key] = {};
								
								if (canDrop.dropTarget) {
									obj[key].dropTargetUUID = canDrop.dropTarget.getAttribute("svy-id");
								}
								
								if (canDrop.beforeChild) {
									obj[key].rightSibling = canDrop.beforeChild.getAttribute("svy-id");
								}
							}
							$editorService.moveResponsiveComponent(obj);
						}
						else {
							var obj = {};
							for (var i=0; i < selectionToDrag.length; i++) {
								var node = selectionToDrag[i];
								if (node.uuid) {
									posX = node.location.x;
									posY = node.location.y;
									if (node.type === COMPONENT_TYPE) // this is a component, so we have to move it
									obj[node.uuid] = {x:node.location.x,y:node.location.y}
								} 
								else {
									var beanModel = editorScope.getBeanModel(node);
									if (beanModel){
										beanModel.location.y;
										beanModel.location.x
										obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y}
									}
									else {
										var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
										if (ghostObject)
										{
											obj[node.getAttribute("svy-id")] = {x:ghostObject.location.x,y:ghostObject.location.y}
										}	
									}
								}
							}
							$editorService.sendChanges(obj);
						}
					}
				}
				selectionToDrag = null;
			}
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
					for(var i = 0; i < selection.length; i++) 
					{
						var node = selection[i];
						selectionToDrag[i] = $(selection[i]).clone();
						var posX, posY;
						var beanModel = editorScope.getBeanModel(node);
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
				
				if (!selectionToDrag) {
					selectionToDrag = editorScope.getSelection();
					selectionToDrag = utils.addGhostsToSelection(selectionToDrag);
				}
				
				if (selectionToDrag.length > 0) {
					if (dragging) {
						if (!editorScope.isAbsoluteFormLayout()) {
							var type = "component";
							var layoutName = selectionToDrag[0].getAttribute("svy-layoutname");
							if (layoutName) type = "layout"
							utils.setDraggingFromPallete(type);
							var topContainer = null;
							var canDrop = utils.getDropNode(type, topContainer,layoutName,event);
							if (!canDrop.dropAllowed) {
								editorScope.glasspane.style.cursor="no-drop";
							}
							else editorScope.glasspane.style.cursor="";
							
							if ( canDrop.dropTarget) {
								for(var i=0;i<selectionToDrag.length;i++) {
									var node = $(selectionToDrag[i]);
									if (editorScope.glasspane.style.cursor=="") {
										if (canDrop.beforeChild) {
											node.insertBefore(canDrop.beforeChild);
										}
										else if (node.parent()[0] != canDrop.dropTarget || canDrop.append){
											$(canDrop.dropTarget).append(node);
										}
									}
								}
								dragStartEvent = event;
								editorScope.refreshEditorContent();
							}
						}
						else {
							var formState = editorScope.getFormState();
							if (formState) {
								var changeX = event.screenX- dragStartEvent.screenX;
								var changeY = event.screenY- dragStartEvent.screenY;
								for(var i=0;i<selectionToDrag.length;i++) {
									var node = selectionToDrag[i];
									if (node[0] && node[0].getAttribute('cloneuuid'))
									{
										node[0].location.x += changeX;
										node[0].location.y += changeY;
										var css = { top: node[0].location.y, left: node[0].location.x }
										node.css(css);
									}
									else {
										var beanModel = editorScope.getBeanModel(node);
										if (beanModel){
											beanModel.location.y = beanModel.location.y + changeY;
											beanModel.location.x = beanModel.location.x + changeX;
										}
										else 
										{
											var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
											if (ghostObject)
											{
												editorScope.updateGhostLocation(ghostObject, ghostObject.location.x + changeX, ghostObject.location.y + changeY)
											}	
										}
									}
								}
								editorScope.refreshEditorContent();
								dragStartEvent = event;
							}
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
