angular.module('resizeknobs',[]).directive("resizeknobs", function($window,EDITOR_EVENTS,$editorService)
{
	return {
		restrict: 'E',
		transclude: true,
		link: function($scope, $element, $attrs) {
			var sendChanges = function(){
				var selection = $scope.getSelection();
				var formState = $scope.getFormState();
				var obj = {};
				for(var i=0;i<selection.length;i++) {
					var node = selection[i];
					var name = node.getAttribute("name");
					var beanModel = formState.model[name];
					obj[node.getAttribute("svy-id")] = {x:beanModel.location.x,y:beanModel.location.y,width:beanModel.size.width,height:beanModel.size.height}
				}
				$editorService.sendChanges(obj)
			}
			var mousemovecallback;
			var mouseupcallback;
			var revertresizecallback;
			var cleanListeners = function(){
				if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","CONTENT_AREA",mousemovecallback);
				if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","CONTENT_AREA",mouseupcallback);
				if (revertresizecallback)  $scope.unregisterDOMEvent("keydown","CONTENT_AREA",revertresizecallback);
				mousemovecallback = null;
				mouseupcallback = null;
				revertresizecallback = null;
			}
			$scope.enterResizeMode = function(event,resizeInfo)
			{
				$scope.setCursorStyle(resizeInfo.direction +"-resize");
				event.preventDefault();
				event.stopPropagation();
				var lastresizeStartPosition = {
						x: event.screenX,
						y: event.screenY
					}
				var resizeSelection = function(ev){
					var selection = $scope.getSelection();
					var deltaX = ev.screenX - lastresizeStartPosition.x;
					var deltaY = ev.screenY - lastresizeStartPosition.y;
					var formState = $scope.getFormState();
					for(var i=0;i<selection.length;i++) {
						var node = selection[i];
						var name = node.getAttribute("name");
						var beanModel = formState.model[name];
						beanModel.location.y = beanModel.location.y + deltaY* resizeInfo.top;
						beanModel.location.x = beanModel.location.x + deltaX* resizeInfo.left;
						beanModel.size.width = beanModel.size.width + deltaX* resizeInfo.width;
						beanModel.size.height = beanModel.size.height + deltaY* resizeInfo.height;
					}
					$scope.refreshEditorContent();
					lastresizeStartPosition = {
							x: ev.screenX,
							y: ev.screenY
					}
				}
				var selection = $scope.getSelection();
				var formState = $scope.getFormState();
				for(var i=0;i<selection.length;i++) {
					var node = selection[i];
					var name = node.getAttribute("name");
					var beanModel = formState.model[name];
					node.originalSize = {};
					node.originalSize.width = beanModel.size.width;
					node.originalSize.height = beanModel.size.height;
					node.originalLocation = {};
					node.originalLocation.x = beanModel.location.x;
					node.originalLocation.y = beanModel.location.y;
				}
				cleanListeners();
				mousemovecallback = $scope.registerDOMEvent("mousemove","CONTENT_AREA",resizeSelection);
				mouseupcallback = $scope.registerDOMEvent("mouseup","CONTENT_AREA", function(ev){
					$scope.setCursorStyle("");
					cleanListeners();
					resizeSelection(ev);
					sendChanges();
				});
				revertresizecallback =  $scope.registerDOMEvent("keydown","CONTENT_AREA", function(ev){
					if (ev.keyCode == 27)
					{
						$scope.setCursorStyle("");
						cleanListeners();
						var selection = $scope.getSelection();
						var formState = $scope.getFormState();
						for(var i=0;i<selection.length;i++) {
							var node = selection[i];
							var name = node.getAttribute("name");
							var beanModel = formState.model[name];
							beanModel.size.width = node.originalSize.width;
							beanModel.size.height = node.originalSize.height ;
							beanModel.location.x = node.originalLocation.x;
							beanModel.location.y = node.originalLocation.y;
						}
						$scope.refreshEditorContent();
						sendChanges();
					}
				});
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