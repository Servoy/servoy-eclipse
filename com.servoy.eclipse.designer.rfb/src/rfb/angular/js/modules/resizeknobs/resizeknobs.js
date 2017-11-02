angular.module('resizeknobs',[]).directive("resizeknobs", function($window,EDITOR_EVENTS,EDITOR_CONSTANTS,$editorService)
{
	return {
		restrict: 'E',
		transclude: true,
		link: function($scope, $element, $attrs) {
			var sendChanges = function(){
				var selection = $scope.getSelection();
				var obj = {};
				var refreshGhosts = false;
				for(var i=0;i<selection.length;i++) {
					var node = selection[i];
					var beanModel = $scope.getBeanModel(node);
					if(!beanModel) {
						beanModel = $scope.getGhost(node.getAttribute("svy-id"));
					}
					if(beanModel) {
						if(beanModel.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM) {
							obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y,width:beanModel.size.width,height:beanModel.size.height}
							var part = $scope.getLastPartGhost();
							if (part != null) obj[part.uuid] = {x:part.location.x, y:part.location.y};
							refreshGhosts = true;
						}
						else
						{
							obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y,width:beanModel.size.width,height:beanModel.size.height}
							var ghostsForNode = $editorService.getEditor().getContainedGhosts(node.getAttribute("svy-id"));
							if (ghostsForNode){
								for(var j=0; j < ghostsForNode.length; j++) {
									var ghost = $editorService.getEditor().getGhost(ghostsForNode[j].uuid);
									obj[ghostsForNode[j].uuid] = {x:ghost.location.x,y:ghost.location.y};
								}
							}							
						}
					}
				}
				$editorService.sendChanges(obj);
				
				if (refreshGhosts)
				{
					var promise = $editorService.getGhostComponents();
					promise.then(function(result) {
						$scope.setGhosts(result);
					});
				}
			}
			var mousemovecallback;
			var mouseupcallback, mouseleavecallback;
			var revertresizecallback;
			var cleanListeners = function(){
				if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","CONTENT_AREA",mousemovecallback);
				if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","CONTENT_AREA",mouseupcallback);
				if (mouseleavecallback)  $scope.unregisterDOMEvent("mouseleave","CONTENT_AREA",mouseleavecallback);
				if (revertresizecallback)  $scope.unregisterDOMEvent("keydown","CONTENT_AREA",revertresizecallback);
				mousemovecallback = null;
				mouseupcallback = null;
				mouseleavecallback = null;
				revertresizecallback = null;
			}

			$scope.enterResizeMode = function(event,resizeInfo)
			{
				if(event.button == 0)
				{
					$scope.setCursorStyle(resizeInfo.direction +"-resize");
					event.preventDefault();
					event.stopPropagation();
					var lastresizeStartPosition = {
							x: event.clientX,
							y: event.clientY
						}
					var resizeSelection = function(ev){					
						var selection = $scope.getSelection();
						var deltaX = ev.clientX - lastresizeStartPosition.x;
						var deltaY = ev.clientY - lastresizeStartPosition.y;
						for(var i=0;i<selection.length;i++) {
							var node = selection[i];
							var beanModel = $scope.getBeanModel(node);
							if(beanModel) {
								beanModel.location.y = beanModel.location.y + deltaY* resizeInfo.top;
								beanModel.location.x = beanModel.location.x + deltaX* resizeInfo.left;

								beanModel.size.width = beanModel.size.width + deltaX* resizeInfo.width;
								if(beanModel.size.width < 1) beanModel.size.width = 1;
								beanModel.size.height = beanModel.size.height + deltaY* resizeInfo.height;
								if(beanModel.size.height < 1) beanModel.size.height = 1;
								var css = { top: beanModel.location.y, left: beanModel.location.x, width: beanModel.size.width, height: beanModel.size.height}
								$(node).css(css);
								if(resizeInfo.position == 'l' || resizeInfo.position == 't' || resizeInfo.position == 'tl') {
									var ghostsForNode = $editorService.getEditor().getContainedGhosts(node.getAttribute("svy-id"));
									if (ghostsForNode){
										for(var j=0; j < ghostsForNode.length; j++) {
											var ghost = $editorService.getEditor().getGhost(ghostsForNode[j].uuid);
											$scope.updateGhostLocation(ghost, ghost.location.x + deltaX*resizeInfo.left, ghost.location.y + deltaY*resizeInfo.top);
										}
									}
								}
							}
							else {
								var ghostObject = $scope.getGhost(node.getAttribute("svy-id"));
								if(ghostObject.type == EDITOR_CONSTANTS.GHOST_TYPE_COMPONENT) {
									$scope.updateGhostLocation(ghostObject, ghostObject.location.x + deltaX*resizeInfo.left, ghostObject.location.y + deltaY*resizeInfo.top)
								}
								$scope.updateGhostSize(ghostObject, deltaX*resizeInfo.width, deltaY*resizeInfo.height)
							}
						}
						$scope.refreshEditorContent();
						lastresizeStartPosition = {
								x: ev.clientX,
								y: ev.clientY
						}
					}
					
					var onmouseup = function(ev) {
						$scope.setCursorStyle("");
						cleanListeners();
						resizeSelection(ev);
						sendChanges();				
					}					
					
					var selection = $scope.getSelection();
					for(var i=0;i<selection.length;i++) {
						var node = selection[i];
						var beanModel = $scope.getBeanModel(node);
						if(beanModel) {
							node.originalSize = {};
							node.originalSize.width = beanModel.size.width;
							node.originalSize.height = beanModel.size.height;
							node.originalLocation = {};
							node.originalLocation.x = beanModel.location.x;
							node.originalLocation.y = beanModel.location.y;
						}
					}
					cleanListeners();
					mousemovecallback = $scope.registerDOMEvent("mousemove","CONTENT_AREA",resizeSelection);
					mouseupcallback = $scope.registerDOMEvent("mouseup","CONTENT_AREA", onmouseup);
					mouseleavecallback = $scope.registerDOMEvent("mouseleave","CONTENT_AREA", onmouseup);
					revertresizecallback =  $scope.registerDOMEvent("keydown","CONTENT_AREA", function(ev){
						if (ev.keyCode == 27)
						{
							$scope.setCursorStyle("");
							cleanListeners();
							var selection = $scope.getSelection();
							for(var i=0;i<selection.length;i++) {
								var node = selection[i];
								var beanModel = $scope.getBeanModel(node);
								if(beanModel) {
									beanModel.size.width = node.originalSize.width;
									beanModel.size.height = node.originalSize.height ;
									beanModel.location.x = node.originalLocation.x;
									beanModel.location.y = node.originalLocation.y;
								}
							}
							$scope.refreshEditorContent();
							sendChanges();
						}
					});
				}
			}
			$scope.cancelResizeMode = function (){
				$scope.setCursorStyle("");
				cleanListeners();
				sendChanges();
			}
		},
		templateUrl: 'js/modules/resizeknobs/resizeknobs.html',
		replace: false
	    };
	
});