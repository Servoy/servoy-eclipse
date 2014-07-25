angular.module('mouseselection',['editor']).run(function($editor){
	
	$editor.registerPlugin(function() {
		function onmousedown(event) {
			console.log(event);
			event.preventDefault();
		}
		// register event on editor form iframe (see register event in the editor.js)
		$editor.registerDOMEvent("mousedown","FORM", onmousedown); // real selection in editor content iframe
	})
});