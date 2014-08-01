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
	
	var isTablePortrait = true;
	var btnTableSize = {
			text: "Table",
			icon: "designsize/ipad-landscape-portrait-64x64.png",
			enabled: true,
			onclick: function() {
				if(isTablePortrait) 
					editor.setContentSize("768px", "1024px");
				else
					editor.setContentSize("1024px", "768px");
				isTablePortrait = !isTablePortrait;
			},
		};
	$toolbar.add(btnTableSize, TOOLBAR_CATEGORIES.STICKY);	
	
	var isPhonePortrait = true;
	var btnMobileSize = {
			text: "Phone",
			icon: "designsize/iphone-portrait-64x64.png",
			enabled: true,
			onclick: function() {
				if(isPhonePortrait) 
					editor.setContentSize("320px", "568px");
				else
					editor.setContentSize("568px", "320px");
				isPhonePortrait = !isPhonePortrait;
			},
		};
	$toolbar.add(btnMobileSize, TOOLBAR_CATEGORIES.STICKY);
});