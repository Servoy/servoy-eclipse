angular.module("toolbar",['allignment']).directive("toolbar", function(){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {
	      },
	      controller: function($scope, $element, $attrs, $toolbar) {
	    	  $scope.elements = $toolbar.getElements();
	      },
	      templateUrl: 'templates/toolbar.html',
	      replace: true
	    };
	
}).factory("$toolbar", function(TOOLBAR_CATEGORIES){
	var editor = [];
	var elements = [];
	var form = [];
	return {
		add: function(buttonState, category) {
			if(category == TOOLBAR_CATEGORIES.ELEMENTS) {
				elements.push(buttonState);
			}
		},
		
		getElements: function() {
			return elements;
		}
	}
}).value("TOOLBAR_CATEGORIES", {
	EDITOR: 1,
	ELEMENTS: 2,
	FORM: 3
})