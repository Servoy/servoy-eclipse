angular.module('margin',[]).directive("margin", function()
{
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			node: '='
		},
		link: function($scope, $element, $attrs) {
			$scope.$watch('node', function() {
				var marginTop = parseFloat('0' + $scope.node.css('marginTop'))
				var marginRight = parseFloat('0' + $scope.node.css('marginRight'))
				var marginBottom = parseFloat('0' + $scope.node.css('marginBottom'))
				var marginLeft = parseFloat('0' + $scope.node.css('marginLeft'))
				
				$scope.t ={
					height: marginTop,
					left: 0,
					right: -marginLeft - 1,
					marginTop: -marginTop - 1,
					marginLeft: -marginLeft - 1
				}
				$scope.r ={
					width: marginRight,
					top: 0,
					bottom: -1,
					marginRight: -marginRight - 1,
					marginTop: -1
				}
				$scope.b ={
					height: marginBottom,
					left: 0,
					right: -marginLeft - 1,
					marginBottom: -marginBottom - 1,
					marginLeft: -marginLeft - 1
				}
				$scope.l ={
					width: marginLeft,
					top: 0,
					bottom: -1,
					marginLeft: -marginLeft - 1,
					marginTop: -1
				}
			})
		},
		templateUrl: 'js/modules/margin/margin.html',
		replace: false
	    };
	
});