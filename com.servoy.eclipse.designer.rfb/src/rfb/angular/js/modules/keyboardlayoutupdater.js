angular.module('keyboardlayoutupdater', ['editor']).run(function($pluginRegistry, $editorService, $selectionUtils, $timeout) {
	
	$pluginRegistry.registerPlugin(function(editorScope) {
	
		var boundsUpdating = false;
		var highlightDiv = angular.element(document.querySelector('#highlight'))[0];
		var utils = $selectionUtils.getUtilsForScope(editorScope);
		
		function onkeydown(event) {
			var fixedKeyEvent = editorScope.getFixedKeyEvent(event);            
            if(fixedKeyEvent.keyCode > 36 && fixedKeyEvent.keyCode < 41) {	// cursor key
            	var selection = editorScope.getSelection();
				if (selection.length > 0) {
					var formState = editorScope.getFormState();
					if (formState) {
						boundsUpdating = true;
						var changeX = 0, changeY = 0, changeW = 0, changeH = 0;
						
						var magnitude = 1;
						if(fixedKeyEvent.isAlt) {
							magnitude = 20;
						}
						else if(fixedKeyEvent.isCtrl) {
							magnitude = 10;
						}
						
						var isResize = fixedKeyEvent.isShift;
						
						switch(fixedKeyEvent.keyCode) {
							case 37:
								if(isResize) {
									changeW = -magnitude;
								}
								else {
									changeX = -magnitude;
								}
								break;
							case 38:
								if(isResize) {
									changeH = -magnitude;
								}
								else {
									changeY = -magnitude;	
								}
								break;
							case 39:
								if(isResize) {
									changeW = magnitude;
								}
								else {
									changeX = magnitude;	
								}								
								break;
							case 40:
								if(isResize) {
									changeH = magnitude;
								}
								else {
									changeY = magnitude;	
								}								
								break;			
						}
						
						selection = utils.addGhostsToSelection(selection);
						
						if (selection.length > 0)
							highlightDiv.style.display = 'none';

						for(var i=0;i<selection.length;i++) {
							var node = selection[i];
							var beanModel = editorScope.getBeanModel(node);
							if (beanModel){
								if(isResize) {
									beanModel.size.width = beanModel.size.width + changeW;
									beanModel.size.height = beanModel.size.height + changeH;
								}
								else {
									beanModel.location.y = beanModel.location.y + changeY;
									beanModel.location.x = beanModel.location.x + changeX;	
								}
							}
							else if(!isResize) {
								var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
								editorScope.updateGhostLocation(ghostObject, ghostObject.location.x + changeX, ghostObject.location.y + changeY)										
							}
						}
						editorScope.refreshEditorContent();
						return false;
					}
				}
            }
		}

		editorScope.sendUpdates = null;
		function onkeyup(event) {
			if (editorScope.sendUpdates) $timeout.cancel(editorScope.sendUpdates);
			editorScope.sendUpdates = $timeout(function(){
				editorScope.sendUpdates = null;
				if(boundsUpdating) {
					boundsUpdating = false;
					var obj = {};
					var selection = editorScope.getSelection();
					
					if (selection.length > 0)
						highlightDiv.style.display = 'none';
					
					selection = utils.addGhostsToSelection(selection);
					
					for(var i=0;i<selection.length;i++) {
						var node = selection[i];
						var beanModel = editorScope.getBeanModel(node);
						if (beanModel){
							obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y,width:beanModel.size.width,height:beanModel.size.height}
						}
						else {
							var ghostObject = editorScope.getGhost(node.getAttribute("svy-id"));
							obj[node.getAttribute("svy-id")] = {x:ghostObject.location.x,y:ghostObject.location.y}
						}
					}
					$editorService.sendChanges(obj);
				}	
			},100);
		}

		editorScope.registerDOMEvent("keydown","CONTENTFRAME_OVERLAY", onkeydown);
		editorScope.registerDOMEvent("keyup","CONTENTFRAME_OVERLAY", onkeyup);

	});
});