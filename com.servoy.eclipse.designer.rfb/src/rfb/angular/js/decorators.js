angular.module("decorators",['editor','margin','resizeknobs']).directive("decorator", function($rootScope,EDITOR_EVENTS){
	return {
	      restrict: 'E',
	      transclude: true,
	      controller: function($scope, $element, $attrs) {
			$scope.nodes = [];
			function renderDecorators(selection) {
				selection.forEach(function(value, index, array) {
					var currentNode = $scope.nodes[index];
					if (!currentNode) {
						currentNode = {name:'',style:{},node:value}
						$scope.nodes[index] = currentNode;
					}
					var node = $(value)
					currentNode.name =  node.attr('name');
					currentNode.node = node;
					var offset = node.offset()
					var x = (window.pageXOffset !== undefined) ? window.pageXOffset : (document.documentElement || document.body.parentNode || document.body).scrollLeft;
					var y = (window.pageYOffset !== undefined) ? window.pageYOffset : (document.documentElement || document.body.parentNode || document.body).scrollTop;
					offset.top -= y;
					offset.left -= x;
					var height = node.outerHeight()
					var width = node.outerWidth()
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
			}
	    	  
			$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
				renderDecorators(selection);
			})
	      },
	      templateUrl: 'templates/decorators.html',
	      replace: true
	    };
	
})
