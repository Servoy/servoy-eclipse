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
		icon: "../../images/th_horizontal.gif",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('setTabSequence');
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
	
	var btnBringForward = {
			text: "Bring forward",
			icon: "../../images/bring_forward.png",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('z_order_bring_to_front_one_step');
			},
	};
	
	var btnSendBackward = {
			text: "Send backward",
			icon: "../../images/send_backward.png",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('z_order_send_to_back_one_step');
			},
	};
	
	var btnBringToFront = {
			text: "Bring to front",
			icon: "../../images/bring_to_front.png",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('z_order_bring_to_front');
			},
	};
	
	var btnSendToBack = {
			text: "Send to back",
			icon: "../../images/send_to_back.png",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('z_order_send_to_back');
			},
	};
	$toolbar.add(btnBringForward, TOOLBAR_CATEGORIES.ORDERING);
	$toolbar.add(btnSendBackward, TOOLBAR_CATEGORIES.ORDERING);
	$toolbar.add(btnBringToFront, TOOLBAR_CATEGORIES.ORDERING);
	$toolbar.add(btnSendToBack, TOOLBAR_CATEGORIES.ORDERING);
	
	var btnSameWidth = {
			text: "Same width",
			icon: "../../images/same_width.gif",
			enabled: false,
			onclick: function() {
				$editorService.sameSize(true);
			},
	};
	
	var btnSameHeight = {
			text: "Same height",
			icon: "../../images/same_height.gif",
			enabled: false,
			onclick: function() {
				$editorService.sameSize(false);
			},
	};
	
	$toolbar.add(btnSameWidth, TOOLBAR_CATEGORIES.SIZING);
	$toolbar.add(btnSameHeight, TOOLBAR_CATEGORIES.SIZING);
	
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
			btnTabSequence.enabled = selection.length > 1;
			btnSameWidth.enabled = selection.length > 1;
			btnSameHeight.enabled = selection.length > 1;
			btnLeftAlign.enabled = selection.length > 0;
			btnBringForward.enabled = selection.length > 0;
			btnSendBackward.enabled = selection.length > 0;
			btnBringToFront.enabled = selection.length > 0;
			btnSendToBack.enabled = selection.length > 0;
		});
	})
});