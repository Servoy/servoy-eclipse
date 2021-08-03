import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { DesignSizeService } from '../services/designsize.service';
import { EditorSessionService } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';

export enum TOOLBAR_CONSTANTS {
  LAYOUTS_COMPONENTS_CSS = "Layouts & Components CSS",
  COMPONENTS_CSS = "Components CSS",
  NO_CSS = "No CSS",
  LAYOUTS_COMPONENTS_CSS_ICON = "url(designer/assets/images/layouts_components_css.png)",
  COMPONENTS_CSS_ICON = "url(designer/assets/images/components_css.png)",
  NO_CSS_ICON = "url(designer/assets/images/no_css.png)"
}

export enum TOOLBAR_CATEGORIES {
	ELEMENTS,
	ORDERING,
	ORDERING_RESPONSIVE,
	ALIGNMENT,
	DISTRIBUTION,
	SIZING,
	GROUPING,
	ZOOM,
	ZOOM_LEVEL,
	FORM,
	DISPLAY,
	EDITOR,
	STICKY,
  SHOW_DATA,
	DESIGN_MODE,
	STANDARD_ACTIONS
}

@Component({
  selector: 'designer-toolbar',
  templateUrl: './toolbar.component.html'
})
export class ToolbarComponent implements OnInit {

  TOOLBAR_CATEGORIES = TOOLBAR_CATEGORIES;

  items: Map<TOOLBAR_CATEGORIES, ToolbarItem[]> = new Map();
  
  btnPlaceField: ToolbarItem;
  btnPlaceImage: ToolbarItem;
  btnPlacePortal: ToolbarItem;
  btnPlaceSplitPane: ToolbarItem;
  btnPlaceTabPanel: ToolbarItem;
  btnPlaceAccordion: ToolbarItem;
  btnHighlightWebcomponents: ToolbarItem;

  btnToggleShowData: ToolbarItem;
  btnToggleDesignMode: ToolbarItem;
  btnSolutionCss: ToolbarItem;

  btnTabSequence: ToolbarItem;
  btnZoomIn: ToolbarItem;
  btnZoomOut: ToolbarItem;
  btnSetMaxLevelContainer: ToolbarItem;
  btnSaveAsTemplate: ToolbarItem;

  btnHideInheritedElements: ToolbarItem;

  btnBringForward: ToolbarItem;
  btnSendBackward: ToolbarItem;
  btnBringToFront: ToolbarItem;
  btnSendToBack: ToolbarItem;

  btnMoveUp: ToolbarItem;
  btnMoveDown: ToolbarItem;

  btnSameWidth: ToolbarItem;
  btnSameHeight: ToolbarItem;

  btnLeftAlign: ToolbarItem;
  btnRightAlign: ToolbarItem;
  btnTopAlign: ToolbarItem;
  btnBottomAlign: ToolbarItem;
  btnCenterAlign: ToolbarItem;
  btnMiddleAlign: ToolbarItem;

  btnDistributeHorizontalSpacing: ToolbarItem;
  btnDistributeHorizontalCenters: ToolbarItem;
  btnDistributeLeftward: ToolbarItem;
  btnDistributeVerticalSpacing: ToolbarItem;
  btnDistributeVerticalCenters: ToolbarItem;
  btnDistributeUpward: ToolbarItem;

  btnGroup: ToolbarItem;
  btnUngroup: ToolbarItem;

  btnReload: ToolbarItem;

  btnClassicEditor: ToolbarItem;

  btnShowErrors: ToolbarItem;

  elements: ToolbarItem[];
  form: ToolbarItem[];
  display: ToolbarItem[];
  ordering: ToolbarItem[];
  alignment: ToolbarItem[];
  distribution: ToolbarItem[];
  sizing: ToolbarItem[];
  grouping: ToolbarItem[];
  zoom_level: ToolbarItem[];
  design_mode: ToolbarItem[];
  sticky: ToolbarItem[];
  zoom: ToolbarItem[];
  standard_actions: ToolbarItem[];
  show_data: ToolbarItem[];

  constructor(protected readonly editorSession: EditorSessionService, protected urlParser: URLParserService,
    @Inject(DOCUMENT) private doc: Document, protected designSize: DesignSizeService ) {
    this.createItems();
    this.designSize.createItems(this);
  }
  
  ngOnInit() {
    this.editorSession.getSession().onopen(() => {
      this.setupItems();
      this.designSize.setupItems(this);
    });
  }

  setupItems() {
    this.elements = this.getCategoryItems(TOOLBAR_CATEGORIES.ELEMENTS);
    this.form = this.getCategoryItems(TOOLBAR_CATEGORIES.FORM);
    this.standard_actions = this.getCategoryItems(TOOLBAR_CATEGORIES.STANDARD_ACTIONS);

		if (this.urlParser.isAbsoluteFormLayout()) {
			this.btnToggleDesignMode.enabled = false;
			this.btnZoomIn.hide = true;
			if (!this.urlParser.isShowingContainer())
			{
			  this.btnZoomOut.hide = true;
			}	
			if (this.urlParser.isHideDefault()) {
				this.btnPlaceImage.hide = true;
				this.btnPlacePortal.hide = true;
				this.btnPlaceSplitPane.hide = true;
				this.btnPlaceTabPanel.hide = true;
				this.btnPlaceAccordion.hide = true;
			}

      this.display = this.getCategoryItems(TOOLBAR_CATEGORIES.DISPLAY);
      this.ordering = this.getCategoryItems(TOOLBAR_CATEGORIES.ORDERING);
      this.alignment = this.getCategoryItems(TOOLBAR_CATEGORIES.ALIGNMENT);
      this.distribution = this.getCategoryItems(TOOLBAR_CATEGORIES.DISTRIBUTION);
      this.sizing = this.getCategoryItems(TOOLBAR_CATEGORIES.SIZING);
      //TODO move this outside the if when SVY-9108 Should be possible to group elements in responsive form. is done
      this.grouping = this.getCategoryItems(TOOLBAR_CATEGORIES.GROUPING);

		} else {
			this.btnPlaceField.hide = true;
			this.btnPlaceImage.hide = true;
			this.btnPlacePortal.hide = true;
			this.btnPlaceSplitPane.hide = true;
			this.btnPlaceTabPanel.hide = true;
			this.btnPlaceAccordion.hide = true;
			this.btnTabSequence.hide = true;
			this.btnClassicEditor.hide = true;
			this.btnHideInheritedElements.hide = true;

      this.ordering = this.getCategoryItems(TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
      this.zoom_level = this.getCategoryItems(TOOLBAR_CATEGORIES.ZOOM_LEVEL);
      this.design_mode = this.getCategoryItems(TOOLBAR_CATEGORIES.DESIGN_MODE);
      this.sticky = this.getCategoryItems(TOOLBAR_CATEGORIES.STICKY);
		}

    if (!this.urlParser.isAbsoluteFormLayout() || this.urlParser.isShowingContainer()) {
      this.zoom = this.getCategoryItems(TOOLBAR_CATEGORIES.ZOOM);
    }

		this.btnZoomOut.enabled = this.urlParser.isShowingContainer();
		const promise = this.editorSession.isShowData();
		promise.then((result) => {
			this.btnToggleShowData.state = result;
		});
		const wireframePromise = this.editorSession.isShowWireframe();
		wireframePromise.then((result) => {
			this.btnToggleDesignMode.state = result;
			this.editorSession.getState().showWireframe = result;
      // TODO:
			// this.editorSession.setContentSizes();
		});
		const highlightPromise = this.editorSession.isShowHighlight();
		highlightPromise.then((result) => {
			this.btnHighlightWebcomponents.state = result;
			this.editorSession.getState().design_highlight = result ? "highlight_element" : null;
		});
		const hideInheritedPromise = this.editorSession.isHideInherited();
		hideInheritedPromise.then((result) => {
			this.btnHideInheritedElements.state = result;
      // TODO:
			// this.editorSession.hideInheritedElements(result);
		});		
		const solutionLayoutsCssPromise = this.editorSession.isShowSolutionLayoutsCss();
		solutionLayoutsCssPromise.then((result) => {
			if (!result) this.btnSolutionCss.text = TOOLBAR_CONSTANTS.COMPONENTS_CSS;
			this.editorSession.getState().showSolutionLayoutsCss = result;
      // TODO:
			// this.editorSession.setContentSizes();
		});
		const solutionCssPromise = this.editorSession.isShowSolutionCss();
		solutionCssPromise.then((result) => {
			if (!result) this.btnSolutionCss.text = TOOLBAR_CONSTANTS.NO_CSS;
			this.editorSession.getState().showSolutionCss = result;
      // TODO:
			// this.editorSession.setContentSizes();
		});
		const zoomLevelPromise = this.editorSession.getZoomLevel();
		zoomLevelPromise.then((result) => {
			if (result)
			{
				this.btnSetMaxLevelContainer.initialValue = result;
				this.editorSession.getState().maxLevel = result;
			}
		});
		
		if (this.doc.getElementById("errorsDiv") !== null) {
			this.btnShowErrors.enabled = true;
			if (this.doc.getElementById("closeErrors")) {
  				this.doc.getElementById("closeErrors").addEventListener('click', (e) => {
				this.btnShowErrors.state = !this.btnShowErrors.state;
				this.doc.getElementById("errorsDiv").style.display = this.btnShowErrors.state ? 'block' : 'none';
  			});
		  }
    }
  }

  createItems() {
    this.btnPlaceField = new ToolbarItem(
      "Place Field Wizard",
      "toolbar/icons/field_wizard.png",
      true,
      () => {
        this.editorSession.openElementWizard('field')
      }
    );
  
    this.btnPlaceImage = new ToolbarItem(
      "Place Image Wizard",
      "toolbar/icons/image_wizard.png",
      true,
      () => {
        this.editorSession.openElementWizard('image');
      }
    );
    
    this.btnPlacePortal = new ToolbarItem(
      "Place Portal Wizard",
      "toolbar/icons/portal_wizard.png",
      true,
      () => {
        this.editorSession.openElementWizard('portal');
      }
    );
    
    this.btnPlaceSplitPane = new ToolbarItem(
      "Place SplitPane Wizard",
      "toolbar/icons/split.png",
      true,
      () => {
        this.editorSession.openElementWizard('splitpane');
      }
    );
    
    this.btnPlaceTabPanel = new ToolbarItem(
      "Place TabPanel Wizard",
      "toolbar/icons/tabs.png",
      true,
      () => {
        this.editorSession.openElementWizard('tabpanel');
      }
    );
    
    this.btnPlaceAccordion = new ToolbarItem(
      "Place Accordion Panel Wizard",
      "toolbar/icons/accordion.png",
      true,
      () => {
        this.editorSession.openElementWizard('accordion');
      }
    );
    
    this.btnHighlightWebcomponents = new ToolbarItem(
      "Highlight webcomponents",
      "toolbar/icons/highlight.png",
      true,
      () => {
        const promise = this.editorSession.toggleHighlight();
        promise.then((result) => {
          this.btnHighlightWebcomponents.state = result;
          this.editorSession.getState().design_highlight = result ? "highlight_element" : null;
        },)
      }
    );
    this.btnHighlightWebcomponents.state = true;

    this.add(this.btnPlaceField, TOOLBAR_CATEGORIES.ELEMENTS);
    this.add(this.btnPlaceImage, TOOLBAR_CATEGORIES.ELEMENTS);
    this.add(this.btnPlacePortal, TOOLBAR_CATEGORIES.ELEMENTS);
    this.add(this.btnPlaceSplitPane, TOOLBAR_CATEGORIES.ELEMENTS);
    this.add(this.btnPlaceTabPanel, TOOLBAR_CATEGORIES.ELEMENTS);
    this.add(this.btnPlaceAccordion, TOOLBAR_CATEGORIES.ELEMENTS);
    this.add(this.btnHighlightWebcomponents, TOOLBAR_CATEGORIES.ELEMENTS);


    this.btnToggleShowData = new ToolbarItem(
      "Data",
      "toolbar/icons/import.gif",
      true,
      () => {
        this.editorSession.toggleShowData();
      }
    );
    this.btnToggleShowData.state = false;
    
    this.btnToggleDesignMode = new ToolbarItem(
      "Exploded view",
      "toolbar/icons/exploded-view.png",
      true,
      () => {
          const promise = this.editorSession.toggleShowWireframe();
          promise.then(function(result) {
            this.toggleDesignMode.state = result;
            this.editorSession.getState().showWireframe = result;
            // TODO:
            // $rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
            // this.editorSession.setContentSizes();
          });
        }
    );
    this.btnToggleDesignMode.state = false;

    this.btnSolutionCss = new ToolbarItem(
			TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS,
      null,
			true,
			(selection) => {
				if (selection == TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS)
				{	
					if (!this.editorSession.getState().showSolutionCss)
					{
						this.toggleShowSolutionCss();
					}
					if (!this.editorSession.getState().showSolutionLayoutsCss)
					{
						this.toggleShowSolutionLayoutsCss();
					}
				}
				if (selection == TOOLBAR_CONSTANTS.COMPONENTS_CSS)
				{
					if (!this.editorSession.getState().showSolutionCss)
					{
						this.toggleShowSolutionCss();
					}
					if (this.editorSession.getState().showSolutionLayoutsCss)
					{
						this.toggleShowSolutionLayoutsCss();
					}
				}
				if (selection == TOOLBAR_CONSTANTS.NO_CSS)
				{
					if (this.editorSession.getState().showSolutionCss)
					{
						this.toggleShowSolutionCss();
					}
					if (!this.editorSession.getState().showSolutionLayoutsCss)
					{
						this.toggleShowSolutionLayoutsCss();
					}
				}
			}
    );
    this.btnSolutionCss.tooltip = "Enable/disable solution css";
    this.btnSolutionCss.getIconStyle = (selection) => { 
      if (selection == TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS) {
        return {'background-image':TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS_ICON};
      }
      if (selection == TOOLBAR_CONSTANTS.COMPONENTS_CSS) {
        return {'background-image':TOOLBAR_CONSTANTS.COMPONENTS_CSS_ICON};
      }
      if (selection == TOOLBAR_CONSTANTS.NO_CSS) {
        return {'background-image':TOOLBAR_CONSTANTS.NO_CSS_ICON};
      }
    };
    this.btnSolutionCss.list = [
      {"text":TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS, "iconStyle":{'background-image':TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS_ICON}},
      {"text":TOOLBAR_CONSTANTS.COMPONENTS_CSS, "iconStyle":{'background-image':TOOLBAR_CONSTANTS.COMPONENTS_CSS_ICON}}, 
      {"text":TOOLBAR_CONSTANTS.NO_CSS, "iconStyle":{'background-image':TOOLBAR_CONSTANTS.NO_CSS_ICON}}
    ];
    this.btnSolutionCss.onselection = (selection) => {
    this.btnSolutionCss.onclick(selection);
     return selection;
    }

    this.add(this.btnToggleShowData, TOOLBAR_CATEGORIES.SHOW_DATA);
    this.add(this.btnToggleDesignMode, TOOLBAR_CATEGORIES.DESIGN_MODE);
    this.add(this.btnSolutionCss, TOOLBAR_CATEGORIES.DESIGN_MODE);


    this.btnTabSequence = new ToolbarItem(
      "Set tab sequence",
      "images/th_horizontal.png",
      false,
      () => {
        this.editorSession.executeAction('setTabSequence');
      }
    );
    this.btnTabSequence.disabledIcon = "images/th_horizontal-disabled.png";
  
    this.btnZoomIn = new ToolbarItem(
      "Zoom in",
      "images/zoom_in.png",
      false,
      () => {
          this.editorSession.executeAction('zoomIn');
      }
    );
    this.btnZoomIn.disabledIcon = "images/zoom_in_disabled.png";
    
    this.btnZoomOut = new ToolbarItem(
      "Zoom out",
      "images/zoom_out.png",
      false,
      () => {
          this.editorSession.executeAction('zoomOut');
      }
    );
    this.btnZoomOut.disabledIcon = "images/zoom_out_disabled.png";
      
    this.btnSetMaxLevelContainer = new ToolbarItem(
      "Adjust zoom level",
      null,
      () => {
        return this.editorSession.getState().showWireframe;
      },
      null
    );
    this.btnSetMaxLevelContainer.max = 10;
    this.btnSetMaxLevelContainer.min = 3;
    this.btnSetMaxLevelContainer.decbutton_text = "Decrease zoom level";
    this.btnSetMaxLevelContainer.decIcon = "images/zoom_out_xs.png";
    this.btnSetMaxLevelContainer.incbutton_text = "Increase zoom level";
    this.btnSetMaxLevelContainer.incIcon = "images/zoom_in_xs.png";
    this.btnSetMaxLevelContainer.state = false;
    this.btnSetMaxLevelContainer.onSet = (value) => {
      this.editorSession.getState().maxLevel = value;
      // TODO:
      // $rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
      // this.editorSession.setZoomLevel(value);
    };

    
    this.btnSaveAsTemplate = new ToolbarItem(
      "Save as template...",
      "toolbar/icons/template_save.png",
      true,
      () => {
        this.editorSession.openElementWizard('saveastemplate');
      }
    );
  
    this.add(this.btnZoomIn, TOOLBAR_CATEGORIES.ZOOM);
    this.add(this.btnZoomOut, TOOLBAR_CATEGORIES.ZOOM);
    this.add(this.btnSetMaxLevelContainer, TOOLBAR_CATEGORIES.ZOOM_LEVEL);
    this.add(this.btnTabSequence, TOOLBAR_CATEGORIES.FORM);
    this.add(this.btnSaveAsTemplate, TOOLBAR_CATEGORIES.FORM);

    this.btnHideInheritedElements = new ToolbarItem(
      "Hide inherited elements",
      "images/hide_inherited.png",
      true,
      () => {
        if (this.btnHideInheritedElements.state) {
          this.btnHideInheritedElements.state = false;
          this.btnHideInheritedElements.text = "Show inherited elements";
        } else {
          this.btnHideInheritedElements.state = true;
          this.btnHideInheritedElements.text = "Hide inherited elements"
        }
        // TODO:
        //this.editorSession.hideInheritedElements(btnHideInheritedElements.state);
        this.editorSession.executeAction('toggleHideInherited');
      }
    );
    this.btnHideInheritedElements.disabledIcon = "images/hide_inherited-disabled.png";
    this.btnHideInheritedElements.state = false;
  
    this.add(this.btnHideInheritedElements, TOOLBAR_CATEGORIES.DISPLAY);
  
    this.btnBringForward = new ToolbarItem(
      "Bring forward",
      "images/bring_forward.png",
      false,
      () => {
        this.editorSession.executeAction('z_order_bring_to_front_one_step');
      }
    );
    this.btnBringForward.disabledIcon = "images/bring_forward-disabled.png";
  
    this.btnSendBackward = new ToolbarItem(
      "Send backward",
      "images/send_backward.png",
      false,
      () => {
        this.editorSession.executeAction('z_order_send_to_back_one_step');
      }
    );
    this.btnSendBackward.disabledIcon = "images/send_backward-disabled.png";
  
    this.btnBringToFront = new ToolbarItem(
      "Bring to front",
      "images/bring_to_front.png",
      false,
      () => {
        this.editorSession.executeAction('z_order_bring_to_front');
      }
    );
    this.btnBringToFront.disabledIcon = "images/bring_to_front-disabled.png";
  
    this.btnSendToBack = new ToolbarItem(
      "Send to back",
      "images/send_to_back.png",
      false,
      () => {
        this.editorSession.executeAction('z_order_send_to_back');
      }
    );
    this.btnSendToBack.disabledIcon = "images/send_to_back-disabled.png";

    this.add(this.btnBringForward, TOOLBAR_CATEGORIES.ORDERING);
    this.add(this.btnSendBackward, TOOLBAR_CATEGORIES.ORDERING);
    this.add(this.btnBringToFront, TOOLBAR_CATEGORIES.ORDERING);
    this.add(this.btnSendToBack, TOOLBAR_CATEGORIES.ORDERING);		
  
    this.btnMoveUp = new ToolbarItem(
      "Move to left inside parent container",
      "images/move_back.png",
      false,
      () => {
        this.editorSession.executeAction('responsive_move_up');
      }
    );
    this.btnMoveUp.disabledIcon = "images/move_back-disabled.png";
  
    this.btnMoveDown = new ToolbarItem(
      "Move to right inside parent container",
      "images/move_forward.png",
      false,
      () => {
        this.editorSession.executeAction('responsive_move_down');
      }
    );
    this.btnMoveDown.disabledIcon = "images/move_forward-disabled.png";

    this.add(this.btnMoveUp, TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
    this.add(this.btnMoveDown, TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
  
  
    this.btnSameWidth = new ToolbarItem(
      "Same width",
      "images/same_width.png",
      false,
      () => {
        // TODO:
        // this.editorSession.sameSize(true);
      }
    );
    this.btnSameWidth.disabledIcon = "images/same_width-disabled.png";
  
    this.btnSameHeight = new ToolbarItem(
      "Same height",
      "images/same_height.png",
      false,
      () => {
        // TODO:
        // this.editorSession.sameSize(false);
      }
    );
    this.btnSameHeight.disabledIcon = "images/same_height-disabled.png";
  
    this.add(this.btnSameWidth, TOOLBAR_CATEGORIES.SIZING);
    this.add(this.btnSameHeight, TOOLBAR_CATEGORIES.SIZING);
  
    this.btnLeftAlign = new ToolbarItem(
      "Align Left",
      "images/alignleft.png",
      false,
      () => {
        // TODO:
        // const selection = this.editorSession.getSelection();
        // if (selection && selection.length > 1) {
        //   var obj = {};
        //   var left = null;
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel) {
        //       if (left == null) {
        //         left = beanModel.location.x;
        //       } else if (left > beanModel.location.x) {
        //         left = beanModel.location.x;
        //       }
        //     }
        //   }
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel) {
        //       if (beanModel.location.x != left) {
        //         obj[node.getAttribute("svy-id")] = {
        //           x: left,
        //           y: beanModel.location.y
        //         };
        //       }
        //     }
        //   }
        //   this.editorSession.sendChanges(obj);
        // }
      }
    );
    this.btnLeftAlign.disabledIcon = "images/alignleft-disabled.png";
  
    this.btnRightAlign = new ToolbarItem(
      "Align Right",
      "images/alignright.png",
      false,
      () => {
        // TODO:
        // var selection = this.editorSession.getSelection();
        // if (selection && selection.length > 1) {
        //   var obj = {};
        //   var right = null;
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (right == null) {
        //       right = beanModel.location.x + beanModel.size.width;
        //     } else if (right < (beanModel.location.x + beanModel.size.width)) {
        //       right = beanModel.location.x + beanModel.size.width;
        //     }
        //   }
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel && (beanModel.location.x + beanModel.size.width) != right) {
        //       obj[node.getAttribute("svy-id")] = {
        //         x: (right - beanModel.size.width),
        //         y: beanModel.location.y
        //       };
        //     }
        //   }
        //   this.editorSession.sendChanges(obj);
        // }
      }
    );
    this.btnRightAlign.disabledIcon = "images/alignright-disabled.png";
  
    this.btnTopAlign = new ToolbarItem(
      "Align Top",
      "images/aligntop.png",
      false,
      () => {
        // TODO:
        // var selection = this.editorSession.getSelection();
        // if (selection && selection.length > 1) {
        //   var obj = {};
        //   var top = null;
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel) {
        //       if (top == null) {
        //         top = beanModel.location.y;
        //       } else if (top > beanModel.location.y) {
        //         top = beanModel.location.y;
        //       }
        //     }
        //   }
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel && beanModel.location.y != top) {
        //       obj[node.getAttribute("svy-id")] = {
        //         x: beanModel.location.x,
        //         y: top
        //       };
        //     }
        //   }
        //   this.editorSession.sendChanges(obj);
        // }
      }
    );
    this.btnTopAlign.disabledIcon = "images/aligntop-disabled.png";
  
    this.btnBottomAlign = new ToolbarItem(
      "Align Bottom",
      "images/alignbottom.png",
      false,
      () => {
        // TODO:
        // var selection = this.editorSession.getSelection();
        // if (selection && selection.length > 1) {
        //   var obj = {};
        //   var bottom = null;
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel) {
        //       if (bottom == null) {
        //         bottom = beanModel.location.y + beanModel.size.height;
        //       } else if (bottom < (beanModel.location.y + beanModel.size.height)) {
        //         bottom = beanModel.location.y + beanModel.size.height;
        //       }
        //     }
        //   }
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel && (beanModel.location.y + beanModel.size.height) != bottom) {
        //       obj[node.getAttribute("svy-id")] = {
        //         x: beanModel.location.x,
        //         y: (bottom - beanModel.size.height)
        //       };
        //     }
        //   }
        //   this.editorSession.sendChanges(obj);
        // }
      }
    );
    this.btnBottomAlign.disabledIcon = "images/alignbottom-disabled.png";
  
    this.btnCenterAlign = new ToolbarItem(
      "Align Center",
      "images/aligncenter.png",
      false,
      () => {
        // TODO:
        // var selection = this.editorSession.getSelection();
        // if (selection && selection.length > 1) {
        //   var obj = {};
        //   var centerElementModel = null;
        //   var sortedSelection = [];
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel) {
        //       if (sortedSelection.length == 0) {
        //         sortedSelection.splice(0, 0, beanModel);
        //       } else {
        //         var insertIndex = sortedSelection.length;
        //         for (var j = 0; j < sortedSelection.length; j++) {
        //           if ((beanModel.location.x + beanModel.size.width / 2) < (sortedSelection[j].location.x + sortedSelection[j].size.width / 2)) {
        //             insertIndex = j;
        //             break;
        //           }
        //         }
        //         sortedSelection.splice(insertIndex, 0, beanModel);
        //       }
        //     }
        //   }
        //   centerElementModel = sortedSelection[Math.round((sortedSelection.length - 1) / 2)];
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel && beanModel != centerElementModel) {
        //       obj[node.getAttribute("svy-id")] = {
        //         x: (centerElementModel.location.x + centerElementModel.size.width / 2 - beanModel.size.width / 2),
        //         y: beanModel.location.y
        //       };
        //     }
        //   }
        //   this.editorSession.sendChanges(obj);
        // }
      }
    );
    this.btnCenterAlign.disabledIcon = "images/aligncenter-disabled.png";

    this.btnMiddleAlign = new ToolbarItem(
      "Align Middle",
      "images/alignmid.png",
      false,
      () => {
        // TODO:
        // var selection = this.editorSession.getSelection();
        // if (selection && selection.length > 1) {
        //   var obj = {};
        //   var centerElementModel = null;
        //   var sortedSelection = [];
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel) {
        //       if (sortedSelection.length == 0) {
        //         sortedSelection.splice(0, 0, beanModel);
        //       } else {
        //         var insertIndex = sortedSelection.length;
        //         for (var j = 0; j < sortedSelection.length; j++) {
        //           if ((beanModel.location.y + beanModel.size.height / 2) < (sortedSelection[j].location.y + sortedSelection[j].size.height / 2)) {
        //             insertIndex = j;
        //             break;
        //           }
        //         }
        //         sortedSelection.splice(insertIndex, 0, beanModel);
        //       }
        //     }
        //   }
        //   centerElementModel = sortedSelection[Math.round((sortedSelection.length - 1) / 2)];
        //   for (var i = 0; i < selection.length; i++) {
        //     var node = selection[i];
        //     var beanModel = this.editorSession.getBeanModelOrGhost(node);
        //     if (beanModel && beanModel != centerElementModel) {
        //       obj[node.getAttribute("svy-id")] = {
        //         x: beanModel.location.x,
        //         y: (centerElementModel.location.y + centerElementModel.size.height / 2 - beanModel.size.height / 2)
        //       };
        //     }
        //   }
        //   this.editorSession.sendChanges(obj);
        // }
      }
    );
    this.btnMiddleAlign.disabledIcon = "images/alignmid-disabled.png";
  
    this.add(this.btnLeftAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
    this.add(this.btnRightAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
    this.add(this.btnTopAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
    this.add(this.btnBottomAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
    this.add(this.btnCenterAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
    this.add(this.btnMiddleAlign, TOOLBAR_CATEGORIES.ALIGNMENT);
  
    this.btnDistributeHorizontalSpacing = new ToolbarItem(
      "Horizontal Spacing",
      "images/distribute_hspace.png",
      false,
      () => {
        this.editorSession.executeAction('horizontal_spacing');
      }
    );
    this.btnDistributeHorizontalSpacing.disabledIcon = "images/distribute_hspace-disabled.png";
  
    this.btnDistributeHorizontalCenters = new ToolbarItem(
      "Horizontal Centers",
      "images/distribute_hcenters.png",
      false,
      () => {
        this.editorSession.executeAction('horizontal_centers');
      }
    );
    this.btnDistributeHorizontalCenters.disabledIcon = "images/distribute_hcenters-disabled.png";
  
    this.btnDistributeLeftward = new ToolbarItem(
      "Leftward",
      "images/distribute_leftward.png",
      false,
      () => {
        this.editorSession.executeAction('horizontal_pack');
      }
    );
    this.btnDistributeLeftward.disabledIcon = "images/distribute_leftward-disabled.png";
  
    this.btnDistributeVerticalSpacing = new ToolbarItem(
      "Vertical Spacing",
      "images/distribute_vspace.png",
      false,
      () => {
        this.editorSession.executeAction('vertical_spacing');
      }
    );
    this.btnDistributeVerticalSpacing.disabledIcon = "images/distribute_vspace-disabled.png";
  
    this.btnDistributeVerticalCenters = new ToolbarItem(
      "Vertical Centers",
      "images/distribute_vcenters.png",
      false,
      () => {
        this.editorSession.executeAction('vertical_centers');
      }
    );
    this.btnDistributeVerticalCenters.disabledIcon = "images/distribute_vcenters-disabled.png";
  
    this.btnDistributeUpward = new ToolbarItem(
      "Upward",
      "images/distribute_upward.png",
      false,
      () => {
        this.editorSession.executeAction('vertical_pack');
      }
    );
    this.btnDistributeUpward.disabledIcon = "images/distribute_upward-disabled.png";
  
    this.add(this.btnDistributeHorizontalSpacing, TOOLBAR_CATEGORIES.DISTRIBUTION);
    this.add(this.btnDistributeHorizontalCenters, TOOLBAR_CATEGORIES.DISTRIBUTION);
    this.add(this.btnDistributeLeftward, TOOLBAR_CATEGORIES.DISTRIBUTION);
    this.add(this.btnDistributeVerticalSpacing, TOOLBAR_CATEGORIES.DISTRIBUTION);
    this.add(this.btnDistributeVerticalCenters, TOOLBAR_CATEGORIES.DISTRIBUTION);
    this.add(this.btnDistributeUpward, TOOLBAR_CATEGORIES.DISTRIBUTION);
    
    this.btnGroup = new ToolbarItem(
      "Group",
      "images/group.png",
      false,
      () =>
        {
          this.editorSession.executeAction('createGroup');
        }
    );
    this.btnGroup.disabledIcon = "images/group-disabled.png";
  
    this.btnUngroup = new ToolbarItem(
      "Ungroup",
      "images/ungroup.png",
      false,
      () =>
      {
        this.editorSession.executeAction('clearGroup');
      }
    );
    this.btnUngroup.disabledIcon = "images/ungroup-disabled.png";

    this.add(this.btnGroup, TOOLBAR_CATEGORIES.GROUPING);
    this.add(this.btnUngroup, TOOLBAR_CATEGORIES.GROUPING);
  
    this.btnReload = new ToolbarItem(
      "Reload designer (use when component changes must be reflected)",
      "images/reload.png",
      true,
      () => {
        this.editorSession.executeAction('reload');
      }
    );
  
    this.add(this.btnReload, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);
    
    this.btnClassicEditor = new ToolbarItem(
      "Switch to classic editor",
      "images/classic_editor.png",
      true,
      () => {
        this.editorSession.executeAction('switchEditorClassic');
        }
    );
    
    this.add(this.btnClassicEditor, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);
    
    this.btnShowErrors = new ToolbarItem(
      "Show errors console",
      "toolbar/icons/error.png",
      false,
      () => {
          this.btnShowErrors.state = !this.btnShowErrors.state;
          this.doc.getElementById("errorsDiv").style.display = this.btnShowErrors.state ? 'block' : 'none';
        }
    );
    this.btnShowErrors.disabledIcon = "toolbar/icons/disabled_error.png";
    this.btnShowErrors.state = false;

    this.add(this.btnShowErrors, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);
  }

  add(item: ToolbarItem, category: TOOLBAR_CATEGORIES) {
    if(!this.items.has(category)) {
      this.items.set(category, new Array<ToolbarItem>());
    }
    this.items.get(category).push(item);
  }

  hasCategoryItems(category: TOOLBAR_CATEGORIES): boolean {
    return this.items.has(category) && this.items.get(category).length > 0;
  }

  getCategoryItems(category: TOOLBAR_CATEGORIES): ToolbarItem[] {
    return this.hasCategoryItems(category) ? this.items.get(category) : [];
  }

  toggleShowSolutionLayoutsCss()
  {
    const promise = this.editorSession.toggleShowSolutionLayoutsCss();
    promise.then(function(result) {
      this.editorSession.getState().showSolutionLayoutsCss = result;
      // TODO:
      // $rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
      // this.editorSession.setContentSizes();
    });
  }
  
  toggleShowSolutionCss()
  {
    const promise = this.editorSession.toggleShowSolutionCss();
    promise.then(function(result) {
      this.editorSession.getState().showSolutionCss = result;
      // TODO:
      // $rootScope.$broadcast(EDITOR_EVENTS.SELECTION_CHANGED, editorScope.getSelection());
      // this.editorSession.setContentSizes();
    });
  }

  // TODO:
	// $rootScope.$on(EDITOR_EVENTS.SELECTION_CHANGED, function(event, selection) {
	// 	// disable or enable buttons.
	// 	$rootScope.$evalAsync(function() {
	// 		btnTabSequence.enabled = selection.length > 1;
	// 		btnSameWidth.enabled = selection.length > 1;
	// 		btnSameHeight.enabled = selection.length > 1;
	// 		btnZoomOut.enabled = $editorService.isShowingContainer();
	// 		if (editorScope.isAbsoluteFormLayout()) {
	// 			btnDistributeHorizontalSpacing.enabled = selection.length > 2;
	// 			btnDistributeHorizontalCenters.enabled = selection.length > 2;
	// 			btnDistributeLeftward.enabled = selection.length > 2;
	// 			btnDistributeVerticalSpacing.enabled = selection.length > 2;
	// 			btnDistributeVerticalCenters.enabled = selection.length > 2;
	// 			btnDistributeUpward.enabled = selection.length > 2;

	// 			btnLeftAlign.enabled = selection.length > 1;
	// 			btnRightAlign.enabled = selection.length > 1;
	// 			btnTopAlign.enabled = selection.length > 1;
	// 			btnBottomAlign.enabled = selection.length > 1;
	// 			btnCenterAlign.enabled = selection.length > 1;
	// 			btnMiddleAlign.enabled = selection.length > 1;
	// 			//TODO move this outside the if when SVY-9108 Should be possible to group elements in responsive form. is done
	// 			btnGroup.enabled = selection.length >= 2;
	// 			btnUngroup.enabled = function() {
	// 				//at least one selected element should be a group
	// 				for (var i = 0; i < selection.length; i++)
	// 				{
	// 					var ghost = editorScope.getGhost(selection[i].getAttribute("svy-id"));
	// 					if (ghost && ghost.type == EDITOR_CONSTANTS.GHOST_TYPE_GROUP)
	// 					{
	// 						return true;
	// 					}
	// 				}
	// 				return false;
	// 			}();
	// 			btnBringForward.enabled = selection.length > 0;
	// 			btnSendBackward.enabled = selection.length > 0;
	// 			btnBringToFront.enabled = selection.length > 0;
	// 			btnSendToBack.enabled = selection.length > 0;				
	// 		}
	// 		else {
	// 			btnMoveUp.enabled = selection.length == 1;
	// 			btnMoveDown.enabled = selection.length == 1;
	// 			btnZoomIn.enabled = selection.length == 1 ;
	// 		}
	// 	});
	// })

}

export class ToolbarItem {

  hide = false;
  style: string;
  disabledIcon: string;
  faIcon: string;
  list: any[];
  getIconStyle: (text: string) => object;
  onselection: (text: string) => string;
  initialValue: number;
  min: number;
  max: number;
  incIcon: string;
  decIcon: string;
  decbutton_text: string;
  incbutton_text: string;
  state: boolean;
  tooltip: string;
  onSet: (value: any) => void;

  constructor(
    public text: string,
    public icon: string,
    public enabled: any,
    public onclick: (text?: string) => void) { 
  }
}