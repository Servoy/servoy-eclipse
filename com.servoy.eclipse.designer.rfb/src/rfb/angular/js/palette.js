angular.module("palette",[]).directive("palette", function(){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {},
	      controller: function($scope, $element, $attrs) {
	    	 
	      },
	      templateUrl: 'templates/palette.html',
	      replace: true
	    };
	
})