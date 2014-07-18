angular.module('mouseselection',['editor']).run($editor){
	
	function onmousedown(event) {
		$editor.setSelection(calculatedSelection)
	}
	// register event on editor form iframe (see register event in the editor.js)
	$editor.registerDOMEvent("mousedown",FORM, onmousedown); // real selection in editor content iframe
	$editor.registerDOMEvent("mousedown",EDITOR, onmousedown); // selection of ghost stuff on the glasspane?
});