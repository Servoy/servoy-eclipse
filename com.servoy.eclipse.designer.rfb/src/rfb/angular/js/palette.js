angular.module("palette",['ui.bootstrap']).directive("palette", function($editorService,$compile,$selectionUtils,$rootScope, EDITOR_EVENTS){
	return {
		restrict: 'E',
		transclude: true,
		controller: function($scope, $element, $attrs, $http) {
			$scope.packages = [];
			var utils = $selectionUtils.getUtilsForScope($scope);

			var loadPalette = function()
			{
				$http({method: 'GET', url: '/designer/palette'}).success(function(data) {
					$scope.packages = data;
					for(var i = 0; i < data.length; i++) {
						data[i].isOpen = "true";
					}
				});
			}
			loadPalette();
			$rootScope.$on(EDITOR_EVENTS.RELOAD_PALETTE, function(e){
				loadPalette();
			});
			
			/**
			 * enterDragMode($event,item.name,package.packageName,item.tagName,item.model,item.topContainer,layoutName)  for new components from the pallete
			 * enterDragMode($event,ghost,null,null,null,ghost,null) for a ghost 
			 */
			$scope.enterDragMode = function(event,componentName,packageName,tagName,model,type, topContainer,layoutName)
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
						if (angularElement && $scope.isAbsoluteFormLayout())
							angularElement.css($scope.convertToContentPoint(css));
						var canDrop = utils.getDropNode(type, topContainer,layoutName,ev);
						if (!canDrop.dropAllowed) {
							$scope.glasspane.style.cursor="no-drop";
						}
						else $scope.glasspane.style.cursor="";
						
						if ( canDrop.dropTarget  && !$scope.isAbsoluteFormLayout()  && angularElement) {
							if ($scope.glasspane.style.cursor=="") {
								if (canDrop.beforeChild) {
									angularElement.insertBefore(canDrop.beforeChild);
									angularElement.css('opacity', '1');
								}
								else if (angularElement.parent()[0] != canDrop.dropTarget || canDrop.append){
									$(canDrop.dropTarget).append(angularElement);
									angularElement.css('opacity', '1');
								}
							}
							else {
								angularElement.css('opacity', '0');
								angularElement.remove();
							}
						}
					}
					else
					{
						dragClone = $(event.target).clone()
						utils.setDraggingFromPallete(type);
						$scope.setSelection(null);
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
						if (type=='component' || type == "layout") {
							if (type=='component') {
								angularElement = $scope.getEditorContentRootScope().createComponent('<div style="border-style: dotted;"><'+tagName+' svy-model=\'model\' svy-api=\'api\' svy-handlers=\'handlers\' svy-autoapply-disabled=\'true\'/></div>',model);
							}
							else {
								// tagname is the full element
								angularElement = $scope.getEditorContentRootScope().createComponent(tagName);
							}
							if ($scope.isAbsoluteFormLayout()) {
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
							else {
								angularElement.css('opacity', '0');
							}
						}
					}
				});
				mouseentercallback = $scope.registerDOMEvent("mouseenter","CONTENTFRAME_OVERLAY", function(ev){
					if (angularElement)
					{
						dragClone.css('opacity', '0');
						if ($scope.isAbsoluteFormLayout()) {
							angularElement.css('opacity', '1');
						}
					}	
				});
				mouseleavecallback = $scope.registerDOMEvent("mouseenter","PALETTE", function(ev){
					if (angularElement)
					{
						dragClone.css('opacity', '1');
						if ($scope.isAbsoluteFormLayout()) {
							angularElement.css('opacity', '0');
						}
					}
				});
				mouseupcallback = $scope.registerDOMEvent("mouseup","EDITOR", function(ev){
					if (mousemovecallback) $scope.unregisterDOMEvent("mousemove","EDITOR",mousemovecallback);
					if (mouseupcallback)  $scope.unregisterDOMEvent("mouseup","EDITOR",mouseupcallback);
					if (mouseentercallback) $scope.unregisterDOMEvent("mouseenter","CONTENTFRAME_OVERLAY",mouseentercallback);
					if (mouseleavecallback) $scope.unregisterDOMEvent("mouseenter","PALETTE",mouseleavecallback);
					$scope.glasspane.style.cursor="";
					if (dragClone)
					{
						var canDrop = utils.getDropNode(type, topContainer,layoutName,ev);
						utils.setDraggingFromPallete(null);
						dragClone.remove();
						if (angularElement)
						{
							// if the drop was still allowed an the beforeChild is not set
							// then ask the current angularelement for the next sibling because it 
							// is in the location where it should be dropped but the canDrop matched purely on the parent
							if (canDrop.dropAllowed && !canDrop.beforeChild) {
								canDrop.beforeChild = angularElement[0].nextElementSibling;
							}
							angularElement.remove();
						}
						if (!canDrop.dropAllowed) return;
						
						var component = {};
						if (canDrop.dropTarget) {
							component.dropTargetUUID = canDrop.dropTarget.getAttribute("svy-id");
						}
						
						if (canDrop.beforeChild) {
							component.rightSibling = canDrop.beforeChild.getAttribute("svy-id");
						}
						component.name = componentName;
						component.packageName = packageName;
						component.x = ev.pageX;
						component.y = ev.pageY;
						if (model){
							component.w = model.size ? model.size.width : 100;
							component.h = model.size ? model.size.height : 100;
						}
						else {
							component.w = 100;
							component.h = 100;
						}
						if (type != "component" && type != "layout") {		
							component.type = type;
								if (!(canDrop.dropTarget.getAttribute("svy-types").indexOf(type) > 0))		
									return; // releasing a ghost, but component does not support this ghost type		
						}
						component = $scope.convertToContentPoint(component);
						if (component.x >0 && component.y >0)
						{
							$editorService.createComponent(component); 
						}
					} 
					else if (angularElement)
					{
						angularElement.remove();
					}
				});
			}
		},
		templateUrl: 'templates/palette.html',
		replace: true
	};

})
