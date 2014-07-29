(function(){
	function MarginDecoratorPlugin(){
		//TODO: use constants
		//TODO: should the needed CSS be included here as well?
		this.registerDecorator('element', '<div class="margin t"/><div class="margin r"/><div class="margin b"/><div class="margin l"/>')
	}
	Editor.registerPlugin('margin-decorator', MarginDecoratorPlugin)
}())