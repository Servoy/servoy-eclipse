angular.module('designsize',['toolbar','editor']).run(function($rootScope, $toolbar, TOOLBAR_CATEGORIES, $pluginRegistry){
	var editor;
	$pluginRegistry.registerPlugin(function(editorScope) {
		editor = editorScope;
	});

	var btnDesktopSize = {
			text: "Desktop",
			icon: "designsize/imac-64x64.png",
			enabled: true,
			onclick: function() { 
				editor.setContentSize("100%", "100%");
			},
		};
	$toolbar.add(btnDesktopSize, TOOLBAR_CATEGORIES.STICKY);
	
	var isPortrait = true;
	var lastClicked;
	var btnTableSize = {
			text: "Table",
			icon: "designsize/ipad-landscape-portrait-64x64.png",
			enabled: true,
			onclick: function() {
				if(lastClicked == "Table") isPortrait = !isPortrait;
				if(isPortrait) 
					editor.setContentSize("768px", "1024px");
				else
					editor.setContentSize("1024px", "768px");
				lastClicked = "Table";
			},
		};
	$toolbar.add(btnTableSize, TOOLBAR_CATEGORIES.STICKY);	

	var btnMobileSize = {
			text: "Phone",
			icon: "designsize/iphone-portrait-64x64.png",
			enabled: true,
			onclick: function() {
				if(lastClicked == "Phone") isPortrait = !isPortrait;
				if(isPortrait) 
					editor.setContentSize("320px", "568px");
				else
					editor.setContentSize("568px", "320px");
				lastClicked = "Phone";
			},
		};
	$toolbar.add(btnMobileSize, TOOLBAR_CATEGORIES.STICKY);
	
	var btnRotate = {
			text: "Rotate",
			icon: "designsize/gear-1-64x64.png",
			enabled: true,
			onclick: function() {
				isPortrait = !isPortrait;
				var size = editor.getContentSize();
				editor.setContentSize(size.height, size.width);
			},
		};
	$toolbar.add(btnRotate, TOOLBAR_CATEGORIES.STICKY);	
});