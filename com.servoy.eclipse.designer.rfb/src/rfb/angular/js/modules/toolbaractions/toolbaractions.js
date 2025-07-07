angular.module('toolbaractions', ['toolbar', 'editor'])
.value("TOOLBAR_CONSTANTS",
{
	LAYOUTS_COMPONENTS_CSS: "Layouts & Components CSS",
	COMPONENTS_CSS: "Components CSS",
	NO_CSS: "No CSS",
	LAYOUTS_COMPONENTS_CSS_ICON: "url(images/layouts_components_css.png)",
	COMPONENTS_CSS_ICON: "url(images/components_css.png)",
	NO_CSS_ICON: "url(images/no_css.png)",
})
.run(function($rootScope, $toolbar, TOOLBAR_CATEGORIES, $editorService, $pluginRegistry, $selectionUtils, $window, EDITOR_EVENTS, EDITOR_CONSTANTS, TOOLBAR_CONSTANTS) {

	var editorScope = null;
	var utils = null;
	$pluginRegistry.registerPlugin(function(scope) {
		editorScope = scope;
		utils = $selectionUtils.getUtilsForScope(scope);
		if (editorScope.isAbsoluteFormLayout()) {
			btnToggleDesignMode.enabled = false;
			btnZoomIn.hide = true;
			if (!$editorService.isShowingContainer())
			{
				btnZoomOut.hide = true;
			}	
			if (editorScope.hideDefault()) {
				btnPlaceImage.hide = true;
				btnPlacePortal.hide = true;
				btnPlaceSplitPane.hide = true;
				btnPlaceTabPanel.hide = true;
				btnPlaceAccordion.hide = true;
			}

		} else {
			btnPlaceField.hide = true;
			btnPlaceImage.hide = true;
			btnPlacePortal.hide = true;
			btnPlaceSplitPane.hide = true;
			btnPlaceTabPanel.hide = true;
			btnPlaceAccordion.hide = true;
			btnTabSequence.hide = true;
			btnClassicEditor.hide = true;
			btnHideInheritedElements.hide = true;
		}
		btnZoomOut.enabled = $editorService.isShowingContainer();
		var promise = $editorService.isShowData();
		promise.then(function(result) {
			btnToggleShowData.state = result;
		});
		var wireframePromise = $editorService.isShowWireframe();
		wireframePromise.then(function(result) {
			btnToggleDesignMode.state = result;
			editorScope.getEditorContentRootScope().showWireframe = result;
			editorScope.getEditorContentRootScope().$digest();
			editorScope.$evalAsync( function() { $editorService.setContentSizes(); });
		});
		var highlightPromise = $editorService.isShowHighlight();
		highlightPromise.then(function(result) {
			btnHighlightWebcomponents.state = result;
			editorScope.getEditorContentRootScope().design_highlight = result ? "highlight_element" : null;
			editorScope.getEditorContentRootScope().$digest();
		});
		var hideInheritedPromise = $editorService.isHideInherited();
		hideInheritedPromise.then(function(result) {
			btnHideInheritedElements.state = result;
			$editorService.hideInheritedElements(result);
		});		
		var solutionLayoutsCssPromise = $editorService.isShowSolutionLayoutsCss();
		solutionLayoutsCssPromise.then(function(result) {
			if (!result) btnSolutionCss.text = TOOLBAR_CONSTANTS.COMPONENTS_CSS;
			editorScope.getEditorContentRootScope().showSolutionLayoutsCss = result;
			editorScope.getEditorContentRootScope().$digest();
			editorScope.$evalAsync( function() { $editorService.setContentSizes(); });
		});
		var solutionCssPromise = $editorService.isShowSolutionCss();
		solutionCssPromise.then(function(result) {
			if (!result) btnSolutionCss.text = TOOLBAR_CONSTANTS.NO_CSS;
			editorScope.getEditorContentRootScope().showSolutionCss = result;
			editorScope.getEditorContentRootScope().$digest();
			editorScope.$evalAsync( function() { $editorService.setContentSizes(); });
		});
		var zoomLevelPromise = $editorService.getZoomLevel();
		zoomLevelPromise.then(function(result) {
			if (result)
			{
				btnSetMaxLevelContainer.initialValue = result;
				editorScope.getEditorContentRootScope().maxLevel = result;
				editorScope.getEditorContentRootScope().$apply();
			}
		});
		
		if (document.getElementById("errorsDiv") !== null) {
			btnShowErrors.enabled = true;
			if (document.getElementById("closeErrors")) {
  				document.getElementById("closeErrors").addEventListener('click', function(e) {
				btnShowErrors.state = !btnShowErrors.state;
				document.getElementById("errorsDiv").style.display = btnShowErrors.state ? 'block' : 'none';
  			});
		}
  }

	});
	var btnPlaceField = {
		text: "Place Field Wizard",
		icon: "toolbaractions/icons/field_wizard.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('field');
		},
	};

	var btnPlaceImage = {
		text: "Place Image Wizard",
		icon: "toolbaractions/icons/image_wizard.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('image');
		},
	};

	var btnPlacePortal = {
		text: "Place Portal Wizard",
		icon: "toolbaractions/icons/portal_wizard.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('portal');
		},
	};

	var btnPlaceSplitPane = {
		text: "Place SplitPane Wizard",
		icon: "toolbaractions/icons/split.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('splitpane');
		},
	};

	var btnPlaceTabPanel = {
		text: "Place TabPanel Wizard",
		icon: "toolbaractions/icons/tabs.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('tabpanel');
		},
	};

	var btnPlaceAccordion = {
		text: "Place Accordion Panel Wizard",
		icon: "toolbaractions/icons/accordion.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('accordion');
		},
	};

	var btnHighlightWebcomponents = {
		text: "Highlight webcomponents",
		icon: "toolbaractions/icons/highlight.png",
		state: true,
		enabled: true,
		onclick: function() {
			var promise = $editorService.toggleHighlight();
			promise.then(function(result) {
				btnHighlightWebcomponents.state = result;
				editorScope.getEditorContentRootScope().design_highlight = result ? "highlight_element" : null;
				editorScope.getEditorContentRootScope().$apply();
			})
		},
	};

	$toolbar.add(btnPlaceField, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceImage, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlacePortal, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceSplitPane, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceTabPanel, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnPlaceAccordion, TOOLBAR_CATEGORIES.ELEMENTS);
	$toolbar.add(btnHighlightWebcomponents, TOOLBAR_CATEGORIES.ELEMENTS);

	var btnToggleShowData = {
		text: "Data",
		icon: "toolbaractions/icons/import.gif",
		enabled: true,
		state: false,
		onclick: function() {
			$editorService.toggleShowData();
		}
	};

	var btnToggleDesignMode = {
		text: "Exploded view",
		icon: "toolbaractions/icons/exploded-view.png",
		enabled: true,
		state: false,
		onclick: function() {
			var promise = $editorService.toggleShowWireframe();
			promise.then(function(result) {
				btnToggleDesignMode.state = result;
				editorScope.getEditorContentRootScope().showWireframe = result;
				editorScope.getEditorContentRootScope().$apply();
				$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
				editorScope.$evalAsync( function() { $editorService.setContentSizes(); });
			});
		}
	};

	function toggleShowSolutionLayoutsCss()
	{
		var promise = $editorService.toggleShowSolutionLayoutsCss();
		promise.then(function(result) {
			editorScope.getEditorContentRootScope().showSolutionLayoutsCss = result;
			editorScope.getEditorContentRootScope().$apply();
			$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
		    editorScope.$evalAsync( function() { $editorService.setContentSizes(); });			
		});
	};
	
	function toggleShowSolutionCss()
	{
		var promise = $editorService.toggleShowSolutionCss();
		promise.then(function(result) {
			editorScope.getEditorContentRootScope().showSolutionCss = result;
			editorScope.getEditorContentRootScope().$apply();
			$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
			editorScope.$evalAsync( function() { $editorService.setContentSizes(); });
		});
	};
	
	var btnSolutionCss = {
			text: TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS,
			tooltip: "Enable/disable solution css",
			getIconStyle: function(selection){ 
				if (selection == TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS) {
					return {'background-image':TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS_ICON};
				}
				if (selection == TOOLBAR_CONSTANTS.COMPONENTS_CSS) {
					return {'background-image':TOOLBAR_CONSTANTS.COMPONENTS_CSS_ICON};
				}
				if (selection == TOOLBAR_CONSTANTS.NO_CSS) {
					return {'background-image':TOOLBAR_CONSTANTS.NO_CSS_ICON};
				}
			},
			enabled: true,
			onclick: function(selection) {
				if (selection == TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS)
				{	
					if (!editorScope.getEditorContentRootScope().showSolutionCss)
					{
						toggleShowSolutionCss();
					}
					if (!editorScope.getEditorContentRootScope().showSolutionLayoutsCss)
					{
						toggleShowSolutionLayoutsCss();
					}
				}
				if (selection == TOOLBAR_CONSTANTS.COMPONENTS_CSS)
				{
					if (!editorScope.getEditorContentRootScope().showSolutionCss)
					{
						toggleShowSolutionCss();
					}
					if (editorScope.getEditorContentRootScope().showSolutionLayoutsCss)
					{
						toggleShowSolutionLayoutsCss();
					}
				}
				if (selection == TOOLBAR_CONSTANTS.NO_CSS)
				{
					if (editorScope.getEditorContentRootScope().showSolutionCss)
					{
						toggleShowSolutionCss();
					}
					if (!editorScope.getEditorContentRootScope().showSolutionLayoutsCss)
					{
						toggleShowSolutionLayoutsCss();
					}
				}
			},
			list: [{"text":TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS, "iconStyle":{'background-image':TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS_ICON}},
			 {"text":TOOLBAR_CONSTANTS.COMPONENTS_CSS, "iconStyle":{'background-image':TOOLBAR_CONSTANTS.COMPONENTS_CSS_ICON}}, 
			 {"text":TOOLBAR_CONSTANTS.NO_CSS, "iconStyle":{'background-image':TOOLBAR_CONSTANTS.NO_CSS_ICON}}],
			onselection: function(selection) {
				this.onclick(selection);
				return selection;
			}
		};

	$toolbar.add(btnToggleShowData, TOOLBAR_CATEGORIES.SHOW_DATA);
	$toolbar.add(btnToggleDesignMode, TOOLBAR_CATEGORIES.DESIGN_MODE);
	$toolbar.add(btnSolutionCss, TOOLBAR_CATEGORIES.DESIGN_MODE);

	var btnTabSequence = {
		text: "Set tab sequence",
		icon: "../../images/th_horizontal.png",
		disabledIcon: "../../images/th_horizontal-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('setTabSequence');
		},
	};

	var btnZoomIn = {
			text: "Zoom in",
			icon: "../../images/zoom_in.png",
			disabledIcon: "../../images/zoom_in_disabled.png",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('zoomIn');
			},
		};
	
	var btnZoomOut = {
			text: "Zoom out",
			icon: "../../images/zoom_out.png",
			disabledIcon: "../../images/zoom_out_disabled.png",
			enabled: false,
			onclick: function() {
				$editorService.executeAction('zoomOut');
			},
		};
		
	var btnSetMaxLevelContainer = {
		max: 10,
		min: 3,
		decbutton_text: "Decrease zoom level",
		decIcon: "../../images/zoom_out_xs.png",
		incbutton_text: "Increase zoom level",
		incIcon: "../../images/zoom_in_xs.png",
		text: "Adjust zoom level",
		enabled: function(){
			return editorScope.getEditorContentRootScope().showWireframe;
		},
		state: false,
		onSet: function(value) {
			editorScope.getEditorContentRootScope().maxLevel = value;
			editorScope.getEditorContentRootScope().$apply();
			$rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
			$editorService.setZoomLevel(value);
		},
	}
	
	// deprecated
	/*var btnSaveAsTemplate = {
		text: "Save as template...",
		icon: "toolbaractions/icons/template_save.png",
		enabled: true,
		onclick: function() {
			$editorService.openElementWizard('saveastemplate');
		},
	};*/

	$toolbar.add(btnZoomIn, TOOLBAR_CATEGORIES.ZOOM);
	$toolbar.add(btnZoomOut, TOOLBAR_CATEGORIES.ZOOM);
	$toolbar.add(btnSetMaxLevelContainer, TOOLBAR_CATEGORIES.ZOOM_LEVEL);
	$toolbar.add(btnTabSequence, TOOLBAR_CATEGORIES.FORM);
	//$toolbar.add(btnSaveAsTemplate, TOOLBAR_CATEGORIES.FORM);

	var btnHideInheritedElements = {
		text: "Hide inherited elements",
		icon: "../../images/hide_inherited.png",
		disabledIcon: "../../images/hide_inherited-disabled.png",
		enabled: true,
		state: false,
		onclick: function() {
			if (btnHideInheritedElements.state) {
				btnHideInheritedElements.state = false;
				this.text = "Show inherited elements";
			} else {
				btnHideInheritedElements.state = true;
				this.text = "Hide inherited elements"
			}
			$editorService.hideInheritedElements(btnHideInheritedElements.state);
			$editorService.executeAction('toggleHideInherited');
			editorScope.setSelection([]);
		},
	};

	$toolbar.add(btnHideInheritedElements, TOOLBAR_CATEGORIES.DISPLAY);

	var btnBringForward = {
		text: "Bring forward",
		icon: "../../images/bring_forward.png",
		disabledIcon: "../../images/bring_forward-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('z_order_bring_to_front_one_step');
		},
	};

	var btnSendBackward = {
		text: "Send backward",
		icon: "../../images/send_backward.png",
		disabledIcon: "../../images/send_backward-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('z_order_send_to_back_one_step');
		},
	};

	var btnBringToFront = {
		text: "Bring to front",
		icon: "../../images/bring_to_front.png",
		disabledIcon: "../../images/bring_to_front-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('z_order_bring_to_front');
		},
	};

	var btnSendToBack = {
		text: "Send to back",
		icon: "../../images/send_to_back.png",
		disabledIcon: "../../images/send_to_back-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('z_order_send_to_back');
		},
	};
	$toolbar.add(btnBringForward, TOOLBAR_CATEGORIES.ORDERING);
	$toolbar.add(btnSendBackward, TOOLBAR_CATEGORIES.ORDERING);
	$toolbar.add(btnBringToFront, TOOLBAR_CATEGORIES.ORDERING);
	$toolbar.add(btnSendToBack, TOOLBAR_CATEGORIES.ORDERING);		

	var btnMoveUp = {
		text: "Move to left inside parent container",
		icon: "../../images/move_back.png",
		disabledIcon: "../../images/move_back-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('responsive_move_up');
		},
	};

	var btnMoveDown = {
		text: "Move to right inside parent container",
		icon: "../../images/move_forward.png",
		disabledIcon: "../../images/move_forward-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('responsive_move_down');
		},
	};
	$toolbar.add(btnMoveUp, TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
	$toolbar.add(btnMoveDown, TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);


	var btnSameWidth = {
		text: "Same width",
		icon: "../../images/same_width.png",
		disabledIcon: "../../images/same_width-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.sameSize(true);
		},
	};

	var btnSameHeight = {
		text: "Same height",
		icon: "../../images/same_height.png",
		disabledIcon: "../../images/same_height-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.sameSize(false);
		},
	};

	$toolbar.add(btnSameWidth, TOOLBAR_CATEGORIES.SIZING);
	$toolbar.add(btnSameHeight, TOOLBAR_CATEGORIES.SIZING);

	var btnLeftAlign = {
		text: "Align Left",
		icon: "../../images/alignleft.png",
		disabledIcon: "../../images/alignleft-disabled.png",
		enabled: false,
		onclick: function() {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var left = null;
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel) {
						if (left == null) {
							left = beanModel.location.x;
						} else if (left > beanModel.location.x) {
							left = beanModel.location.x;
						}
					}
				}
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel) {
						if (beanModel.location.x != left) {
							obj[node.getAttribute("svy-id")] = {
								x: left,
								y: beanModel.location.y
							};
						}
					}
				}
				$editorService.sendChanges(obj);
			}
		},
	};

	var btnRightAlign = {
		text: "Align Right",
		icon: "../../images/alignright.png",
		disabledIcon: "../../images/alignright-disabled.png",
		enabled: false,
		onclick: function() {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var right = null;
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (right == null) {
						right = beanModel.location.x + beanModel.size.width;
					} else if (right < (beanModel.location.x + beanModel.size.width)) {
						right = beanModel.location.x + beanModel.size.width;
					}
				}
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel && (beanModel.location.x + beanModel.size.width) != right) {
						obj[node.getAttribute("svy-id")] = {
							x: (right - beanModel.size.width),
							y: beanModel.location.y
						};
					}
				}
				$editorService.sendChanges(obj);
			}
		},
	};

	var btnTopAlign = {
		text: "Align Top",
		icon: "../../images/aligntop.png",
		disabledIcon: "../../images/aligntop-disabled.png",
		enabled: false,
		onclick: function() {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var top = null;
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel) {
						if (top == null) {
							top = beanModel.location.y;
						} else if (top > beanModel.location.y) {
							top = beanModel.location.y;
						}
					}
				}
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel && beanModel.location.y != top) {
						obj[node.getAttribute("svy-id")] = {
							x: beanModel.location.x,
							y: top
						};
					}
				}
				$editorService.sendChanges(obj);
			}
		},
	};

	var btnBottomAlign = {
		text: "Align Bottom",
		icon: "../../images/alignbottom.png",
		disabledIcon: "../../images/alignbottom-disabled.png",
		enabled: false,
		onclick: function() {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var bottom = null;
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel) {
						if (bottom == null) {
							bottom = beanModel.location.y + beanModel.size.height;
						} else if (bottom < (beanModel.location.y + beanModel.size.height)) {
							bottom = beanModel.location.y + beanModel.size.height;
						}
					}
				}
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel && (beanModel.location.y + beanModel.size.height) != bottom) {
						obj[node.getAttribute("svy-id")] = {
							x: beanModel.location.x,
							y: (bottom - beanModel.size.height)
						};
					}
				}
				$editorService.sendChanges(obj);
			}
		},
	};

	var btnCenterAlign = {
		text: "Align Center",
		icon: "../../images/aligncenter.png",
		disabledIcon: "../../images/aligncenter-disabled.png",
		enabled: false,
		onclick: function() {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var centerElementModel = null;
				var sortedSelection = [];
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel) {
						if (sortedSelection.length == 0) {
							sortedSelection.splice(0, 0, beanModel);
						} else {
							var insertIndex = sortedSelection.length;
							for (var j = 0; j < sortedSelection.length; j++) {
								if ((beanModel.location.x + beanModel.size.width / 2) < (sortedSelection[j].location.x + sortedSelection[j].size.width / 2)) {
									insertIndex = j;
									break;
								}
							}
							sortedSelection.splice(insertIndex, 0, beanModel);
						}
					}
				}
				centerElementModel = sortedSelection[Math.round((sortedSelection.length - 1) / 2)];
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel && beanModel != centerElementModel) {
						obj[node.getAttribute("svy-id")] = {
							x: (centerElementModel.location.x + centerElementModel.size.width / 2 - beanModel.size.width / 2),
							y: beanModel.location.y
						};
					}
				}
				$editorService.sendChanges(obj);
			}
		},
	};

	var btnMiddleAlign = {
		text: "Align Middle",
		icon: "../../images/alignmid.png",
		disabledIcon: "../../images/alignmid-disabled.png",
		enabled: false,
		onclick: function() {
			var selection = editorScope.getSelection();
			if (selection && selection.length > 1) {
				var obj = {};
				var centerElementModel = null;
				var sortedSelection = [];
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel) {
						if (sortedSelection.length == 0) {
							sortedSelection.splice(0, 0, beanModel);
						} else {
							var insertIndex = sortedSelection.length;
							for (var j = 0; j < sortedSelection.length; j++) {
								if ((beanModel.location.y + beanModel.size.height / 2) < (sortedSelection[j].location.y + sortedSelection[j].size.height / 2)) {
									insertIndex = j;
									break;
								}
							}
							sortedSelection.splice(insertIndex, 0, beanModel);
						}
					}
				}
				centerElementModel = sortedSelection[Math.round((sortedSelection.length - 1) / 2)];
				for (var i = 0; i < selection.length; i++) {
					var node = selection[i];
					var beanModel = editorScope.getBeanModelOrGhost(node);
					if (beanModel && beanModel != centerElementModel) {
						obj[node.getAttribute("svy-id")] = {
							x: beanModel.location.x,
							y: (centerElementModel.location.y + centerElementModel.size.height / 2 - beanModel.size.height / 2)
						};
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
		icon: "../../images/distribute_hspace.png",
		disabledIcon: "../../images/distribute_hspace-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('horizontal_spacing');
		},
	};

	var btnDistributeHorizontalCenters = {
		text: "Horizontal Centers",
		icon: "../../images/distribute_hcenters.png",
		disabledIcon: "../../images/distribute_hcenters-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('horizontal_centers');
		},
	};

	var btnDistributeLeftward = {
		text: "Leftward",
		icon: "../../images/distribute_leftward.png",
		disabledIcon: "../../images/distribute_leftward-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('horizontal_pack');
		},
	};

	var btnDistributeVerticalSpacing = {
		text: "Vertical Spacing",
		icon: "../../images/distribute_vspace.png",
		disabledIcon: "../../images/distribute_vspace-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('vertical_spacing');
		},
	};

	var btnDistributeVerticalCenters = {
		text: "Vertical Centers",
		icon: "../../images/distribute_vcenters.png",
		disabledIcon: "../../images/distribute_vcenters-disabled.png",
		enabled: false,
		onclick: function() {
			$editorService.executeAction('vertical_centers');
		},
	};

	var btnDistributeUpward = {
		text: "Upward",
		icon: "../../images/distribute_upward.png",
		disabledIcon: "../../images/distribute_upward-disabled.png",
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
	
	var btnGroup =
	{
			text: "Group",
			icon: "../../images/group.png",
			disabledIcon: "../../images/group-disabled.png",
			enabled: false,
			onclick: function()
			{
				$editorService.executeAction('createGroup');
			}
	};

	var btnUngroup =
	{
		text: "Ungroup",
		icon: "../../images/ungroup.png",
		disabledIcon: "../../images/ungroup-disabled.png",
		enabled: false,
		onclick: function()
		{
			$editorService.executeAction('clearGroup');
		}
	};
	$toolbar.add(btnGroup, TOOLBAR_CATEGORIES.GROUPING);
	$toolbar.add(btnUngroup, TOOLBAR_CATEGORIES.GROUPING);

	var btnReload = {
		text: "Reload designer (use when component changes must be reflected)",
		icon: "../../images/reload.png",
		enabled: true,
		onclick: function() {
			$editorService.executeAction('reload');
		},
	}

	$toolbar.add(btnReload, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);
	
	
	var btnShowErrors = {
			text: "Show errors console",
			icon: "toolbaractions/icons/error.png",
			disabledIcon: "toolbaractions/icons/disabled_error.png",
			state: false,
			enabled: false,
			onclick: function() {
				btnShowErrors.state = !btnShowErrors.state;
				document.getElementById("errorsDiv").style.display = btnShowErrors.state ? 'block' : 'none';
			}
	}
	$toolbar.add(btnShowErrors, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);

	$rootScope.$on(EDITOR_EVENTS.INITIALIZED, function() {
		// disable or enable buttons.
		$rootScope.$apply(function() {
			var promise = $editorService.isInheritedForm();
			promise.then(function(result) {
				if (!result) {
					btnHideInheritedElements.enabled = false;
				}
			});
		});
	})

	$rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
		// disable or enable buttons.
		$rootScope.$evalAsync(function() {
			btnTabSequence.enabled = selection.length > 1;
			btnSameWidth.enabled = selection.length > 1;
			btnSameHeight.enabled = selection.length > 1;
			btnZoomOut.enabled = $editorService.isShowingContainer();
			if (editorScope.isAbsoluteFormLayout()) {
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
				//TODO move this outside the if when SVY-9108 Should be possible to group elements in responsive form. is done
				btnGroup.enabled = selection.length >= 2;
				btnUngroup.enabled = function() {
					//at least one selected element should be a group
					for (var i = 0; i < selection.length; i++)
					{
						var ghost = editorScope.getGhost(selection[i].getAttribute("svy-id"));
						if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
						{
							return true;
						}
					}
					return false;
				}();
				btnBringForward.enabled = selection.length > 0;
				btnSendBackward.enabled = selection.length > 0;
				btnBringToFront.enabled = selection.length > 0;
				btnSendToBack.enabled = selection.length > 0;				
			}
			else {
				btnMoveUp.enabled = selection.length == 1;
				btnMoveDown.enabled = selection.length == 1;
				btnZoomIn.enabled = selection.length == 1 ;
			}
		});
	})

});
