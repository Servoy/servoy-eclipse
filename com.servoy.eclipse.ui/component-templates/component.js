angular.module('${MODULENAME}',['servoy']).directive('${MODULENAME}', function() {  
    return {
      restrict: 'E',
      scope: {
    	  model: '=svyModel'
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: '${PACKAGENAME}/${NAME}/${NAME}.html'
    };
  })