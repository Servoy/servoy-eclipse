angular.module("toolbar",['alignment','designsize']).directive("toolbar", function(TOOLBAR_CATEGORIES){
	return {
	      restrict: 'E',
	      transclude: true,
	      scope: {
	      },
	      controller: function($scope, $element, $attrs, $toolbar) {
	    	  $scope.elements = $toolbar.getButtons(TOOLBAR_CATEGORIES.ELEMENTS);
	    	  $scope.sticky = $toolbar.getButtons(TOOLBAR_CATEGORIES.STICKY);
	      },
	      templateUrl: 'templates/toolbar.html',
	      replace: true
	    };
	
}).factory("$toolbar", function(TOOLBAR_CATEGORIES){
	var buttons= [];
	return {
		add: function(buttonState, category) {
			if(!buttons[category]){
				buttons[category] = [];
			}
			buttons[category].push(buttonState);
		},
		
		getButtons: function(category) {
			return buttons[category];
		}
	}
}).value("TOOLBAR_CATEGORIES", {
	EDITOR: "editor",
	ELEMENTS: "elements",
	FORM: "forms",
	STICKY: "sticky"
})