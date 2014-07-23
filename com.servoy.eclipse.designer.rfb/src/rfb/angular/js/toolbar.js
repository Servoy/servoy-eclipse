angular.module("toolbar",[]).directive("toolbar", function(){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {},
	      controller: function($scope, $element, $attrs,$toolbar) {
	    	 
	      },
	      templateUrl: 'templates/toolbar.html',
	      replace: true
	    };
	
}).factory("$toolbar", function(){
	var buttons = [];
	return {
		add: function(buttonStates, category) {
			// is it a category per call?
			buttons.push(buttonState);
		}
	}
}).value("CATEGORIES", {
	EDITOR: 1,
	ELEMENTS: 2,
	FORM: 3
})