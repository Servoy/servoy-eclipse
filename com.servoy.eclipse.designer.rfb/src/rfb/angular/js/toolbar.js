angular.module("toolbar").directive("toolbar", function($scope,$toolbar){
	
	$scope.buttons = $toolbar.getButtons();
	
	
}).factory("$toolbar", function(){
	var buttons = [];
	return {
		add: function(buttonStates, category) {
			// is it a category per call?
			buttons.push(buttonState);
		}
	}
}).values("CATEGORIES", {
	EDITOR: 1,
	ELEMENTS: 2,
	FORM: 3
})