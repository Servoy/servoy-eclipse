angular.module('toolbaractions',['toolbar','editor']).run(function($rootScope, $toolbar, TOOLBAR_CATEGORIES, $editorService, EDITOR_EVENTS){

	var btnPlaceField = {
			text: "Place Field Wizard",
			icon: "toolbaractions/icons/field.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('field');
			},
	};
	
	var btnPlaceImage = {
			text: "Place Image Wizard",
			icon: "toolbaractions/icons/image.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('image');
			},
	};
	
	var btnPlacePortal = {
			text: "Place Portal Wizard",
			icon: "toolbaractions/icons/portal.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('portal');
			},
	};

	var btnPlaceSplitPane = {
			text: "Place SplitPane Wizard",
			icon: "toolbaractions/icons/split.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('splitpane');
			},
	};

	var btnPlaceTabPanel = {
			text: "Place TabPanel Wizard",
			icon: "toolbaractions/icons/tabs.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('tabpanel');
			},
	};

	var btnPlaceAccordion = {
			text: "Place Accordion Panel Wizard",
			icon: "toolbaractions/icons/accordion.jpg",
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

	var btnTabSequence = {
		text: "Set tab sequence",
		icon: "toolbaractions/icons/th_horizontal.gif",
		enabled: false,
		onclick: function() {
			// handle click
		},
	};
	
	var btnSaveAsTemplate = {
			text: "Save as template...",
			icon: "toolbaractions/icons/template.gif",
			enabled: true,
			onclick: function() {
				$editorService.openElementWizard('saveastemplate');
			},
	};

	$toolbar.add(btnTabSequence, TOOLBAR_CATEGORIES.FORM);
	$toolbar.add(btnSaveAsTemplate, TOOLBAR_CATEGORIES.FORM);
	
	var btnLeftAlign = {
			text: "Alignment",
			icon: "toolbaractions/icons/distribute_leftward.gif",
			enabled: false,
			onclick: function() {
				// handle click
			},
	};
		
	$toolbar.add(btnLeftAlign, TOOLBAR_CATEGORIES.LAYOUT);
	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		// disable or enable buttons.
		$rootScope.$apply(function() {
			btnLeftAlign.enabled = selection.length > 0;
		});
	})
});