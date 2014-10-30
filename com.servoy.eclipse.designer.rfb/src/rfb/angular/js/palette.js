angular.module("palette",['ui.bootstrap']).directive("palette", function($editorService,$compile,$selectionUtils){
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs, $http) {
			$scope.packages = [];
			var utils = $selectionUtils.getUtilsForScope($scope);

			$http({method: 'GET', url: '/designer/palette'}).success(function(data) {
				$scope.packages = data;
				for(var i = 0; i < data.length; i++) {
					data[i].isOpen = "true";
				}
			});
			/**
			 * enterDragMode($event,item.name,package.packageName,item.tagName,item.model)  for new components from the pallete
			 * enterDragMode($event,ghost,null,null,null,ghost) for a ghost 
			 */
			$scope.enterDragMode = function(event,componentName,packageName,tagName,model,type)
			{
				var dragClone = null;
				var angularElement = null;
				var mouseentercallback;
				var mouseleavecallback;
				var mouseupcallback;
				var mousemovecallback = $scope.registerDOMEvent("mousemove","EDITOR", function(ev){
					if (dragClone)
					{
						var css = { top: ev.pageY, left: ev.pageX }
						dragClone.css(css);
						css = $scope.convertToContentPoint(css);
						if (angularElement)
							angularElement.css(css);
						if (type){
							var dropTarget = utils.getNode(ev);
							if (dropTarget && dropTarget.getAttribute("svy-types")){
								if (dropTarget.getAttribute("svy-types").indexOf(type) > 0)
									$scope.glasspane.style.cursor="";
								else
									$scope.glasspane.style.cursor="no-drop";
							}
							else $scope.glasspane.style.cursor="no-drop";
						}else $scope.glasspane.style.cursor="";
					}
					else
					{
						dragClone = $(event.target).clone()
						dragClone.attr('id', 'dragNode')
						dragClone.css({
							position: 'absolute',
							top: event.pageY,
							left: event.pageX,
							'z-index': 4,
							'pointer-events': 'none',
							'list-style-type': 'none'
						})
						$('body').append(dragClone);
						if (!type) {
							angularElement = $scope.getEditorContentRootScope().createComponent('<div style="border-style: dotted;"><'+tagName+' svy-model=\'model\' svy-api=\'api\' svy-handlers=\'handlers\' svy-autoapply-disabled=\'true\'/></div>',model);
							var elWidth = model.size ? model.size.width : 100;
							var elHeight = model.size ? model.size.height : 100;
							var css = $scope.convertToContentPoint({
								position: 'absolute',
								top: event.pageY,
								left: event.pageX,
								width: (elWidth +'px'),
								height: (elHeight +'px'),
								'z-index': 4,
								opacity: 0,
								transition: 'opacity .5s ease-in-out 0'
							});
							angularElement.css(css)
						}
					}
				});
				mouseentercallback = $scope.registerDOMEvent("mouseenter","CONTENTFRAME_OVERLAY", function(ev){
					if (!type)
						dragClone.css('opacity', '0');
					if (angularElement)
						angularElement.css('opacity', '1');
				});
				mouseleavecallback = $scope.registerDOMEvent("mouseenter","PALETTE", function(ev){
					if (!type)
						dragClone.css('opacity', '1');
					if (angularElement)
						angularElement.css('opacity', '0');
				});
				mouseupcallback = $scope.registerDOMEvent("mouseup","EDITOR", function(ev){
					if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","EDITOR",mousemovecallback);
					if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","EDITOR",mouseupcallback);
					if (mouseentercallback) $scope.unregisterDOMEvent("mouseenter","CONTENTFRAME_OVERLAY",mouseentercallback);
					if (mouseleavecallback) $scope.unregisterDOMEvent("mouseenter","PALETTE",mouseleavecallback);
					$scope.glasspane.style.cursor="";
					if (angularElement)
					{
						angularElement.remove();
					}
					if (dragClone)
					{
						dragClone.remove();
						var component = {};
						if (type) {
							component.type = type;
							var dropTarget = utils.getNode(ev);
							if (!dropTarget) return; // releasing a ghost, but no actual component underneath
							if (dropTarget) {
								if (!(dropTarget.getAttribute("svy-types").indexOf(type) > 0))
									return; // releasing a ghost, but component does not support this ghost type
								component.dropTargetUUID = dropTarget.getAttribute("svy-id");
							}
						}
						component.name = componentName;
						component.packageName = packageName;
						component.x = ev.pageX;
						component.y = ev.pageY;
						component = $scope.convertToContentPoint(component);
						if (component.x >0 && component.y >0)
						{
							$editorService.createComponent(component); 
						}
					}
				});
			}
		},
		templateUrl: 'templates/palette.html',
		replace: true
	};

})
