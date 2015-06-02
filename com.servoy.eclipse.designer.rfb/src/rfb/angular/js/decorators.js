angular.module("decorators",['editor','margin','resizeknobs']).directive("decorator", function($rootScope,EDITOR_EVENTS,EDITOR_CONSTANTS,$timeout){
	return {
	      restrict: 'E',
	      transclude: true,
	      controller: function($scope, $element, $attrs) {
			$scope.nodes = [];
			function adjustForPadding(mousePosition) {
				mousePosition.left += parseInt(angular.element($scope.glasspane.parentElement).css("padding-left").replace("px",""));
				mousePosition.top  += parseInt(angular.element($scope.glasspane.parentElement).css("padding-top").replace("px",""));
				return mousePosition;
			}
			function hasClass(element, cls) {
				return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
			}
			function renderDecorators(selection,renderResizeKnobs) {
				$timeout(function(){
					if (selection.length == 1){
						//when resizing the form, the server sends a refreshGhosts message that updates the form ghost div => the selection references a stale form ghost,
						//we need to search for the real form ghost div
						var formDiv = angular.element($scope.glasspane).find("[svy-id='"+selection[0].getAttribute("svy-id")+"']");
						if (formDiv[0] && formDiv[0].getAttribute("svy-id") == selection[0].getAttribute("svy-id"))
							selection[0] = formDiv[0];
					}
					selection.forEach(function(value, index, array) {
						var currentNode = $scope.nodes[index];
						if (!currentNode) {
							currentNode = {name:'',style:{},node:value}
							$scope.nodes[index] = currentNode;
						}
						var node = $(value)
						
						// this node could be the angular tag (replace is false, or dynamic template) with a 0 size
						// try to look if there is a child element that is better suited.
						var height = value.clientHeight
						var width = value.clientWidth
						if (height == 0 && width == 0) {
							var children = node.children();
							if (children.length == 1 && children[0].clientHeight > 0 && children[0].clientWidth > 0) {
								node = $(children[0]);
							}
							height = node.outerHeight();
							width = node.outerWidth();
						}
						
						currentNode.name =  node.attr('name');
						currentNode.node = node;
						var ghost = $scope.getGhost(node.attr("svy-id"));
						if (renderResizeKnobs)
						{
							if(ghost) {			
								if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_COMPONENT) 
								{
									currentNode.isResizable = {t:true, l:true, b:true, r:true};
								}
								else if (ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_FORM)
								{
									currentNode.isResizable = {t:false, l:false, b:true, r:true};
								}
								else
								{
									currentNode.isResizable = false;
								}
								height = node.outerHeight();
								width = node.outerWidth();
							}
							else {
								currentNode.isResizable = {t:true, l:true, b:true, r:true};
							}
						}	
						else
						{
							currentNode.isResizable = false;
						}	
						
						var offset = node.offset();
						
						//this is so that ghost elements decorators are positioned correctly
						if(node.parent().hasClass("ghostcontainer") && node.parent().parent().offset() != undefined) {
							offset.top -= node.parent().parent().offset().top;
							offset.left -= node.parent().parent().offset().left;
						}
						
						if (!hasClass(node.context,"ghost"))
							offset = adjustForPadding(offset)
						currentNode.style = {
							height: height,
							width: width,
							top: offset.top,
							left: offset.left,
							display: 'block'
						};
					})
					for(var i=selection.length;i<$scope.nodes.length;i++) {
						$scope.nodes[i].style.display = 'none';
					}
				});
			}
	    	  
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				renderDecorators(selection,true);
			})
			$rootScope.$on(EDITOR_EVENTS.SELECTION_MOVED, function(event, selection) {
				// do not render resize knobs while selection is moving; performance optimization
				renderDecorators(selection,false);
			})
			$rootScope.$on(EDITOR_EVENTS.RENDER_DECORATORS, function(event, selection) {
				renderDecorators(selection,true);
			})
	      },
	      templateUrl: 'templates/decorators.html',
	      replace: true
	    };
	
})
