angular.module('toolbaractions',['toolbar','editor']).run(function($rootScope, $toolbar, TOOLBAR_CATEGORIES, $editorService, $pluginRegistry, EDITOR_EVENTS){

	var editorScope = null;
	$pluginRegistry.registerPlugin(function(scope) {
		editorScope = scope;
	});
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
	
	var btnHighlightWebcomponents = {
			text: "Highlight webcomponents",
			icon: "toolbaractions/icons/highlight.png",
			enabled: true,
			onclick: function() {
				$editorService.toggleHighlight();
			},
	};

	$toolbar.add(btnPlaceField, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceImage, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlacePortal, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceSplitPane, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceTabPanel, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceAccordion, TOOLBAR_CATEGORIES.ELEMENTS);	
	$toolbar.add(btnHighlightWebcomponents, TOOLBAR_CATEGORIES.ELEMENTS);	
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

	var showingInheritedElements = true;
	
	var btnHideInheritedElements = {
			text: "Hide inherited elements",
			icon: "../../images/hide_inherited.gif",
			enabled: true,
			onclick: function() {
				$(editorScope.contentDocument).find('.inherited_element').each(function(index,element) {
					if (showingInheritedElements)
					{
						$(element).hide();
					}
					else
					{
						$(element).show();
					}
				});
				if (showingInheritedElements)
				{
					showingInheritedElements = false;
				}
				else
				{
					showingInheritedElements = true;
				}
			},
	};

	$toolbar.add(btnHideInheritedElements, TOOLBAR_CATEGORIES.DISPLAY);
	
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
			text: "Align Left",
			icon: "../../images/alignleft.gif",
			enabled: false,
			onclick: function() {
				var selection = editorScope.getSelection();
				if (selection && selection.length > 1)
				{
					var obj = {};
					var left = null;
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if(beanModel)
						{
							if (left == null)
							{
								left = beanModel.location.x;
							}
							else if (left > beanModel.location.x)
							{
								left = beanModel.location.x;
							}
						}
					}
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if(beanModel)
						{
							if (beanModel.location.x != left)
							{
								obj[node.getAttribute("svy-id")] = {x: left,y: beanModel.location.y};
							}
						}
					}
					$editorService.sendChanges(obj);
				}
			},
	};

	var btnRightAlign = {
			text: "Align Right",
			icon: "../../images/alignright.gif",
			enabled: false,
			onclick: function() {
				var selection = editorScope.getSelection();
				if (selection && selection.length > 1)
				{
					var obj = {};
					var right = null;
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (right == null)
						{
							right = beanModel.location.x+beanModel.size.width;
						}
						else if (right < (beanModel.location.x+beanModel.size.width))
						{
							right = beanModel.location.x+beanModel.size.width;
						}
					}
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel && (beanModel.location.x + beanModel.size.width) != right)
						{
							obj[node.getAttribute("svy-id")] = {x: (right - beanModel.size.width),y: beanModel.location.y};
						}
					}
					$editorService.sendChanges(obj);
				}
			},
	};

	var btnTopAlign = {
			text: "Align Top",
			icon: "../../images/aligntop.gif",
			enabled: false,
			onclick: function() {
				var selection = editorScope.getSelection();
				if (selection && selection.length > 1)
				{
					var obj = {};
					var top = null;
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel)
						{
							if (top == null)
							{
								top = beanModel.location.y;
							}
							else if (top > beanModel.location.y)
							{
								top = beanModel.location.y;
							}
						}
					}
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel && beanModel.location.y != top)
						{
							obj[node.getAttribute("svy-id")] = {x: beanModel.location.x,y: top};
						}
					}
					$editorService.sendChanges(obj);
				}
			},
	};

	var btnBottomAlign = {
			text: "Align Bottom",
			icon: "../../images/alignbottom.gif",
			enabled: false,
			onclick: function() {
				var selection = editorScope.getSelection();
				if (selection && selection.length > 1)
				{
					var obj = {};
					var bottom = null;
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel)
						{
							if (bottom == null)
							{
								bottom = beanModel.location.y + beanModel.size.height;
							}
							else if (bottom < (beanModel.location.y + beanModel.size.height))
							{
								bottom = beanModel.location.y + beanModel.size.height;
							}
						}
					}
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel && (beanModel.location.y + beanModel.size.height) != bottom)
						{
							obj[node.getAttribute("svy-id")] = {x: beanModel.location.x,y: (bottom - beanModel.size.height)};
						}
					}
					$editorService.sendChanges(obj);
				}
			},
	};

	var btnCenterAlign = {
			text: "Align Center",
			icon: "../../images/aligncenter.gif",
			enabled: false,
			onclick: function() {
				var selection = editorScope.getSelection();
				if (selection && selection.length > 1)
				{
					var obj = {};
					var centerElementModel = null;
					var sortedSelection = [];
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel)
						{
							if (sortedSelection.length == 0)
							{
								sortedSelection.splice(0,0,beanModel);
							}
							else
							{
								var insertIndex = sortedSelection.length;
								for (var j=0;j<sortedSelection.length;j++)
								{
									if ((beanModel.location.x + beanModel.size.width/2) < (sortedSelection[j].location.x + sortedSelection[j].size.width/2))
									{
										insertIndex = j;
										break;
									}
								}
								sortedSelection.splice(insertIndex,0,beanModel);
							}
						}
					}
					centerElementModel = sortedSelection[Math.round((sortedSelection.length-1)/2)];
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel && beanModel != centerElementModel)
						{
							obj[node.getAttribute("svy-id")] = {x: (centerElementModel.location.x + centerElementModel.size.width/2 - beanModel.size.width/2),y: beanModel.location.y};
						}
					}
					$editorService.sendChanges(obj);
				}
			},
	};

	var btnMiddleAlign = {
			text: "Align Middle",
			icon: "../../images/alignmid.gif",
			enabled: false,
			onclick: function() {
				var selection = editorScope.getSelection();
				if (selection && selection.length > 1)
				{
					var obj = {};
					var centerElementModel = null;
					var sortedSelection = [];
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel)
						{
							if (sortedSelection.length == 0)
							{
								sortedSelection.splice(0,0,beanModel);
							}
							else
							{
								var insertIndex = sortedSelection.length;
								for (var j=0;j<sortedSelection.length;j++)
								{
									if ((beanModel.location.y + beanModel.size.height/2) < (sortedSelection[j].location.y + sortedSelection[j].size.height/2))
									{
										insertIndex = j;
										break;
									}
								}
								sortedSelection.splice(insertIndex,0,beanModel);
							}
						}
					}
					centerElementModel = sortedSelection[Math.round((sortedSelection.length-1)/2)];
					for (var i=0;i<selection.length;i++)
					{
						var node = selection[i];
						var beanModel = editorScope.getBeanModelOrGhost(node);
						if (beanModel && beanModel != centerElementModel)
						{
							obj[node.getAttribute("svy-id")] = {x: beanModel.location.x,y: (centerElementModel.location.y + centerElementModel.size.height/2 - beanModel.size.height/2)};
						}
					}
					$editorService.sendChanges(obj);
				}
			},
	};

	$toolbar.add(btnLeftAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
	$toolbar.add(btnRightAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
	$toolbar.add(btnTopAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
	$toolbar.add(btnBottomAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
	$toolbar.add(btnCenterAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
	$toolbar.add(btnMiddleAlign, TOOLBAR_CATEGORIES.ALIGNMENT);

	var btnDistributeHorizontalSpacing = {
			text: "Horizontal Spacing",
			icon: "../../images/distribute_hspace.gif",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('horizontal_spacing');
			},
	};

	var btnDistributeHorizontalCenters = {
			text: "Horizontal Centers",
			icon: "../../images/distribute_hcenters.gif",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('horizontal_centers');
			},
	};

	var btnDistributeLeftward = {
			text: "Leftward",
			icon: "../../images/distribute_leftward.gif",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('horizontal_pack');
			},
	};

	var btnDistributeVerticalSpacing = {
			text: "Vertical Spacing",
			icon: "../../images/distribute_vspace.gif",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('vertical_spacing');
			},
	};

	var btnDistributeVerticalCenters = {
			text: "Vertical Centers",
			icon: "../../images/distribute_vcenters.gif",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('vertical_centers');
			},
	};

	var btnDistributeUpward = {
			text: "Upward",
			icon: "../../images/distribute_upward.gif",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('vertical_pack');
			},
	};

	$toolbar.add(btnDistributeHorizontalSpacing, TOOLBAR_CATEGORIES.DISTRIBUTION);
	$toolbar.add(btnDistributeHorizontalCenters, TOOLBAR_CATEGORIES.DISTRIBUTION);
	$toolbar.add(btnDistributeLeftward, TOOLBAR_CATEGORIES.DISTRIBUTION);
	$toolbar.add(btnDistributeVerticalSpacing, TOOLBAR_CATEGORIES.DISTRIBUTION);
	$toolbar.add(btnDistributeVerticalCenters, TOOLBAR_CATEGORIES.DISTRIBUTION);
	$toolbar.add(btnDistributeUpward, TOOLBAR_CATEGORIES.DISTRIBUTION);

	$rootScope.$on(EDITOR_EVENTS.INITIALIZED, function() {
		// disable or enable buttons.
		$rootScope.$apply(function() {
			var promise = $editorService.isInheritedForm();
			promise.then(function (result){
				if (!result)
				{
					btnHideInheritedElements.enabled = false;
				}
			});
		});
	})
	
	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		// disable or enable buttons.
		$rootScope.$apply(function() {
			btnTabSequence.enabled = selection.length > 1;
			btnSameWidth.enabled = selection.length > 1;
			btnSameHeight.enabled = selection.length > 1;

			if (editorScope.isAbsoluteFormLayout())
			{
				btnDistributeHorizontalSpacing.enabled = selection.length > 2;
				btnDistributeHorizontalCenters.enabled = selection.length > 2;
				btnDistributeLeftward.enabled = selection.length > 2;
				btnDistributeVerticalSpacing.enabled = selection.length > 2;
				btnDistributeVerticalCenters.enabled = selection.length > 2;
				btnDistributeUpward.enabled = selection.length > 2;

				btnLeftAlign.enabled = selection.length > 1;
				btnRightAlign.enabled = selection.length > 1;
				btnTopAlign.enabled = selection.length > 1;
				btnBottomAlign.enabled = selection.length > 1;
				btnCenterAlign.enabled = selection.length > 1;
				btnMiddleAlign.enabled = selection.length > 1;
			}
			btnBringForward.enabled = selection.length > 0;
			btnSendBackward.enabled = selection.length > 0;
			btnBringToFront.enabled = selection.length > 0;
			btnSendToBack.enabled = selection.length > 0;
		});
	})
	
});