angular.module('allignment',['toolbar','editor']).run($rootScope,$toolbar, EDITOR_EVENTS){
	var buttons = [{
		text: "Left Align",
		enabled: false,
		onclick: function() {
			// handle click
		},
	}];
	
	$toolbar.add(buttons);
	
	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGE, function(event, selection) {
		// disable or enable buttons.
		buttons[0].enabled = selection.length > 0;
	})
	
});