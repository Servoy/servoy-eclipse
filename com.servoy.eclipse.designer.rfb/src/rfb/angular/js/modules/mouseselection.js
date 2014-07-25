angular.module('mouseselection',['editor']).run(function($rootScope, $editor){
	
	$editor.registerPlugin(function() {
		function onmousedown(event) {
			console.log(event);
			$rootScope.$apply(function() {$editor.setSelection(event.target);} )
			event.preventDefault();
		}
		// register event on editor form iframe (see register event in the editor.js)
		$editor.registerDOMEvent("mousedown","FORM", onmousedown); // real selection in editor content iframe
	})
});