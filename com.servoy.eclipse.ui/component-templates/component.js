angular.module('${DIRECTIVENAME}',['servoy']).directive('${DIRECTIVENAME}', function() {  
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