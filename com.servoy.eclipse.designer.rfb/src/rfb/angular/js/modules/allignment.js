angular.module('allignment',['toolbar','editor']).run(function($rootScope, $toolbar, TOOLBAR_CATEGORIES, EDITOR_EVENTS){
	var btnLeftAlign = {
		text: "LeftAlign",
		enabled: false,
		onclick: function() {
			// handle click
		},
	};
	
	$toolbar.add(btnLeftAlign, TOOLBAR_CATEGORIES.ELEMENTS);
	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		// disable or enable buttons.
		$rootScope.$apply(function() {
			btnLeftAlign.enabled = selection.length > 0;
		});
	})
});