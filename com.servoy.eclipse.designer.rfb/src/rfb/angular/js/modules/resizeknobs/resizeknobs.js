angular.module('resizeknobs',[]).directive("resizeknobs", function(EDITOR_EVENTS,$editorService)
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
			$scope.enterResizeMode = function(event,resizeInfo)
			{
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
				mousemovecallback = $scope.registerDOMEvent("mousemove","FORM",resizeSelection);
				mouseupcallback = $scope.registerDOMEvent("mouseup","FORM", function(ev){
					$scope.unregisterDOMEvent("mousemove","FORM",mousemovecallback);
					$scope.unregisterDOMEvent("mouseup","FORM",mouseupcallback);
					resizeSelection(ev);
					sendChanges();
				});
			}
			$scope.cancelResizeMode = function (){
				$scope.unregisterDOMEvent("mousemove","FORM",mousemovecallback);
				$scope.unregisterDOMEvent("mouseup","FORM",mouseupcallback);
				sendChanges();
			}
		},
		templateUrl: 'js/modules/resizeknobs/resizeknobs.html',
		replace: false
	    };
	
});