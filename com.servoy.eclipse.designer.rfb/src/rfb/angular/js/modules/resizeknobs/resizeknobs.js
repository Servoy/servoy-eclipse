angular.module('resizeknobs',[]).directive("resizeknobs", function(EDITOR_EVENTS)
{
	return {
		restrict: 'E',
		transclude: true,
		link: function($scope, $element, $attrs) {
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
					for(var i=0;i<selection.length;i++) {
						selection[i].parentNode.style.width = parseInt(selection[i].parentNode.style.width.substring(0,selection[i].parentNode.style.width.length-2)) + (deltaX* resizeInfo.width) + 'px';
						selection[i].parentNode.style.left = parseInt(selection[i].parentNode.style.left.substring(0,selection[i].parentNode.style.left.length-2)) + (deltaX* resizeInfo.left) + 'px';
						selection[i].parentNode.style.height = parseInt(selection[i].parentNode.style.height.substring(0,selection[i].parentNode.style.height.length-2)) + (deltaY* resizeInfo.height) + 'px';
						selection[i].parentNode.style.top = parseInt(selection[i].parentNode.style.top.substring(0,selection[i].parentNode.style.top.length-2)) + (deltaY* resizeInfo.top) + 'px';
					}
					$scope.$emit(EDITOR_EVENTS.SELECTION_CHANGED,selection);
					lastresizeStartPosition = {
							x: ev.screenX,
							y: ev.screenY
					}
				}
				$scope.registerDOMEvent("mousemove","FORM",resizeSelection);
				$scope.registerDOMEvent("mouseup","FORM", function(ev){
					$scope.unregisterDOMEvent("mousemove","FORM");
					$scope.unregisterDOMEvent("mouseup","FORM");
					resizeSelection(ev);
				});
			}
			$scope.cancelResizeMode = function (){
				$scope.unregisterDOMEvent("mousemove","FORM");
				$scope.unregisterDOMEvent("mouseup","FORM");
			}
		},
		templateUrl: 'js/modules/resizeknobs/resizeknobs.html',
		replace: false
	    };
	
});