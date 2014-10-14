angular.module('placeelement',['toolbar','editor']).run(function($rootScope, $editorService, $toolbar, TOOLBAR_CATEGORIES, EDITOR_EVENTS){

	var btnPlaceField = {
		text: "Place Field Wizard",
		icon: "placeelement/field.gif",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('field');
		},
	};
	
	var btnPlaceImage = {
			text: "Place Image Wizard",
			icon: "placeelement/image.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('image');
			},
	};
	
	var btnPlacePortal = {
			text: "Place Portal Wizard",
			icon: "placeelement/portal.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('portal');
			},
	};
	
	var btnPlaceSplitPane = {
			text: "Place SplitPane Wizard",
			icon: "placeelement/split.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('splitpane');
			},
	};
		
	var btnPlaceTabPanel = {
			text: "Place TabPanel Wizard",
			icon: "placeelement/tabs.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('tabpanel');
			},
	};
	
	var btnPlaceAccordion = {
			text: "Place Accordion Panel Wizard",
			icon: "placeelement/accordion.jpg",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('accordion');
			},
	};	
	
	$toolbar.add(btnPlaceField, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceImage, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlacePortal, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceSplitPane, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceTabPanel, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceAccordion, TOOLBAR_CATEGORIES.ELEMENTS);

});