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
			 * enterDragMode($event,item.name,package.packageName,item.tagName,item.model,item.allowedParents,layoutName)  for new components from the pallete
			 * enterDragMode($event,ghost,null,null,null,ghost,null) for a ghost 
			 */
			$scope.enterDragMode = function(event,componentName,packageName,tagName,model,type, allowedParents,layoutName)
			{
				var dragClone = null;
				var angularElement = null;
				var mouseentercallback;
				var mouseleavecallback;
				var mouseupcallback;
				var mousemovecallback = $scope.registerDOMEvent("mousemove","EDITOR", function(ev){
					if (dragClone)
					{
						var dropTarget = null;
						var css = { top: ev.pageY, left: ev.pageX }
						dragClone.css(css);
						if (angularElement && $scope.isAbsoluteFormLayout())
							angularElement.css($scope.convertToContentPoint(css));
						if (type == "layout" || type == "component") {
							var realName = layoutName?layoutName:"component";
							dropTarget = utils.getNode(ev, true);
							if (!dropTarget){
								// this is on the form, can this layout container be dropped on the form?
								if (allowedParents.indexOf("form") == -1){
									$scope.glasspane.style.cursor="no-drop";
									return;
								}
							}
							else {
								var allowedChildren = dropTarget.getAttribute("svy-allowed-children");
								if (!allowedChildren || !(allowedChildren.indexOf(realName) > 0))
								{
									$scope.glasspane.style.cursor="no-drop";
									return; // the drop target doesn't allow this layout container type
								}
								var dropTargetLayoutName = dropTarget.getAttribute("svy-layoutname");
								// is this element able to drop on the dropTarget?
								if (allowedParents && !allowedParents.indexOf(dropTargetLayoutName) == -1) {
									$scope.glasspane.style.cursor="no-drop";
									return;
								}
							}
							$scope.glasspane.style.cursor="";
						}
						else if (type != "component"){
							dropTarget = utils.getNode(ev);
							if (dropTarget && dropTarget.getAttribute("svy-types")){
								if (dropTarget.getAttribute("svy-types").indexOf(type) > 0)
									$scope.glasspane.style.cursor="";
								else
									$scope.glasspane.style.cursor="no-drop";
							}
							else $scope.glasspane.style.cursor="no-drop";
						}
						else {
							dropTarget = utils.getNode(ev, true);
							$scope.glasspane.style.cursor="";
						}
						
						if (dropTarget && angularElement && $scope.glasspane.style.cursor=="" && !$scope.isAbsoluteFormLayout()) {
							$(dropTarget).append(angularElement);
						}
					}
					else
					{
						dragClone = $(event.target).clone()
						utils.setDraggingFromPallete(type);
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
						angularElement.css('opacity', '1');
					}	
				});
				mouseleavecallback = $scope.registerDOMEvent("mouseenter","PALETTE", function(ev){
					if (angularElement)
					{
						dragClone.css('opacity', '1');
						angularElement.css('opacity', '0');
					}
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
						
						var dropTarget = utils.getNode(ev);
						if (dropTarget) {
							component.dropTargetUUID = dropTarget.getAttribute("svy-id");
						}
						if (type == "layout" || type == "component") {
							var realName = layoutName?layoutName:"component";
							dropTarget = utils.getNode(ev, true);
							if (!dropTarget){
								// this is on the form, can this layout container be dropped on the form?
								if (allowedParents.indexOf("form") == -1) return;
							}
							else {
								var allowedChildren = dropTarget.getAttribute("svy-allowed-children");
								if (!allowedChildren || !(allowedChildren.indexOf(realName) > 0))
									return; // the drop target doesn't allow this layout container type
								var dropTargetLayoutName = dropTarget.getAttribute("svy-layoutname");
								// is this element able to drop on the dropTarget?
								if (allowedParents && !allowedParents.indexOf(dropTargetLayoutName) == -1) return;
							}
						}
						else if (type!="component") {
							component.type = type;
							if (!dropTarget) return; // releasing a ghost, but no actual component underneath
							if (dropTarget) {
								if (!(dropTarget.getAttribute("svy-types").indexOf(type) > 0))
									return; // releasing a ghost, but component does not support this ghost type
							}
						}
						var flowLocation = utils.getFlowLocation(dropTarget,ev);
						if (flowLocation.leftSibling)
						{
							component.leftSibling = flowLocation.leftSibling;
						}
						if (flowLocation.rightSibling)
						{
							component.rightSibling = flowLocation.rightSibling;
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
						utils.setDraggingFromPallete(null);
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
