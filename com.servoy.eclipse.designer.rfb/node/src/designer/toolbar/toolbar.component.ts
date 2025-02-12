import { Component, OnInit, Renderer2 } from '@angular/core';
import { DesignSizeService } from '../services/designsize.service';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { EditorContentService } from '../services/editorcontent.service';

export enum TOOLBAR_CONSTANTS {
    LAYOUTS_COMPONENTS_CSS = 'Solution CSS',
    COMPONENTS_CSS = 'No Solution CSS for Layouts',
    NO_CSS = 'No Solution CSS',
    SAME_SIZE = 'Selected Element Same Size Indicator',
    ANCHOR_INDICATOR = 'Selected Element Anchoring Indicator',
    LAYOUTS_COMPONENTS_CSS_ICON = 'url(designer/assets/images/layouts_components_css.png)',
    VISUAL_FEEDBACK_CSS_ICON = 'url(designer/assets/images/grid.png)',
    COMPONENTS_CSS_ICON = 'url(designer/assets/images/components_css.png)',
    PLACEMENT_GUIDE_CSS_ICON = 'url(designer/assets/images/snaptogrid.png)',
    NO_CSS_ICON = 'url(designer/assets/images/no_css.png)',
    CHECK_ICON = 'url(designer/assets/images/check.png)',
    BRING_FORWARD = 'Bring forward',
    SEND_BACKWARD = 'Send backward',
    BRING_TO_FRONT = 'Bring to front',
    SEND_TO_BACK = 'Send to back',
    BRING_FORWARD_ICON = 'url(designer/assets/images/bring_forward.png)',
    SEND_BACKWARD_ICON = 'url(designer/assets/images/send_backward.png)',
    BRING_TO_FRONT_ICON = 'url(designer/assets/images/bring_to_front.png)',
    SEND_TO_BACK_ICON = 'url(designer/assets/images/send_to_back.png)',
    SAME_WIDTH = 'Same Width',
    SAME_HEIGHT = 'Same Height',
    ALIGN_LEFT = 'Align Left',
    ALIGN_RIGHT = 'Align Right',
    ALIGN_TOP = 'Align Top',
    ALIGN_BOTTOM = 'Align Bottom',
    ALIGN_CENTER = 'Align Center',
    ALIGN_MIDDLE = 'Align Middle',
    HORIZONTAL_SPACING = 'Horizontal Spacing',
    HORIZONTAL_CENTERS = 'Horizontal Centers',
    LEFTWARD = 'Leftward',
    VERTICAL_SPACING = 'Vertical Spacing',
    VERTICAL_CENTERS = 'Vertical Centers',
    UPWARD = 'Upward'
}

export enum TOOLBAR_CATEGORIES {
    ELEMENTS,
    ORDERING,
    ORDERING_RESPONSIVE,
    ALIGNMENT,
    DISTRIBUTION,
    SIZING,
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
    templateUrl: './toolbar.component.html',
    standalone: false
})
export class ToolbarComponent implements OnInit, ISelectionChangedListener {

    TOOLBAR_CATEGORIES = TOOLBAR_CATEGORIES;

    items: Map<TOOLBAR_CATEGORIES, ToolbarItem[]> = new Map();

    btnPlaceField: ToolbarItem;
    btnHighlightWebcomponents: ToolbarItem;
    btnToggleDynamicGuides: ToolbarItem;

    btnToggleShowData: ToolbarItem;
    btnToggleDesignMode: ToolbarItem;
    btnSolutionCss: ToolbarItem;

    btnTabSequence: ToolbarItem;
    btnZoomIn: ToolbarItem;
    btnZoomOut: ToolbarItem;
    btnSetMaxLevelContainer: ToolbarItem;
    btnSaveAsTemplate: ToolbarItem;

    btnHideInheritedElements: ToolbarItem;
    btnVisualFeedbackOptions: ToolbarItem;
    btnOrderingActionsCSSForm: ToolbarItem;
    btnAlignActions: ToolbarItem;
    btnSpaceDistributionActions: ToolbarItem;
    
    btnMoveUp: ToolbarItem;
    btnMoveDown: ToolbarItem;

    btnReload: ToolbarItem;
    btnToggleI18NValues: ToolbarItem;

    btnShowErrors: ToolbarItem;

    elements: ToolbarItem[];
    form: ToolbarItem[];
    display: ToolbarItem[];
    ordering: ToolbarItem[];
    alignment: ToolbarItem[];
    distribution: ToolbarItem[];
    zoom_level: ToolbarItem[];
    design_mode: ToolbarItem[];
    sticky: ToolbarItem[];
    zoom: ToolbarItem[];
    standard_actions: ToolbarItem[];
    show_data: ToolbarItem[];

    constructor(protected readonly editorSession: EditorSessionService, protected urlParser: URLParserService,
        protected designSize: DesignSizeService, private readonly renderer: Renderer2, private editorContentService: EditorContentService) {
        this.createItems();
        this.designSize.createItems(this);
        this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit() {
        this.editorSession.getSession().onopen(() => {
            this.setupItems();
            this.designSize.setupItems();
        });
    }

    setupItems() {
        this.elements = this.getCategoryItems(TOOLBAR_CATEGORIES.ELEMENTS);
        this.form = this.getCategoryItems(TOOLBAR_CATEGORIES.FORM);
        this.standard_actions = this.getCategoryItems(TOOLBAR_CATEGORIES.STANDARD_ACTIONS);

        if (this.urlParser.isAbsoluteFormLayout()) {
            this.btnToggleDesignMode.enabled = false;
            this.btnZoomIn.hide = true;
            if (!this.urlParser.isShowingContainer()) {
                this.btnZoomOut.hide = true;
            }

            this.display = this.getCategoryItems(TOOLBAR_CATEGORIES.DISPLAY);
            this.ordering = [this.btnOrderingActionsCSSForm];
            this.alignment = [this.btnAlignActions];
            this.distribution = [this.btnSpaceDistributionActions];

            const ShowSameSizeIndicatorPromise = this.editorSession.showSameSizeIndicator();
            void ShowSameSizeIndicatorPromise.then((result: boolean) => {
                if (!result) {
                    this.btnVisualFeedbackOptions.list[1].iconStyle = { 'background-image': 'none' };
                }
                else {
                    this.btnVisualFeedbackOptions.list[1].iconStyle = { 'background-image': TOOLBAR_CONSTANTS.CHECK_ICON };
                }
                this.editorSession.setSameSizeIndicator(result);
            });

            const ShowAnchoringIndicatorPromise = this.editorSession.showAnchoringIndicator();
            void ShowAnchoringIndicatorPromise.then((result: boolean) => {
                if (!result) {
                    this.btnVisualFeedbackOptions.list[0].iconStyle = { 'background-image': 'none' };
                }
                else {
                    this.btnVisualFeedbackOptions.list[0].iconStyle = { 'background-image': TOOLBAR_CONSTANTS.CHECK_ICON };
                }
                this.editorSession.setAnchoringIndicator(result);
            });

            const guidesPromise = this.editorSession.isShowDynamicGuides();
            void guidesPromise.then((result: boolean) => {
                this.btnToggleDynamicGuides.state = result;
                this.editorSession.fireShowDynamicGuidesChangedListeners(result);
            });

        } else {
            this.btnPlaceField.hide = true;
            this.btnTabSequence.hide = true;
            this.btnHideInheritedElements.hide = true;

            this.ordering = this.getCategoryItems(TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
            this.zoom_level = this.getCategoryItems(TOOLBAR_CATEGORIES.ZOOM_LEVEL);
            this.design_mode = this.getCategoryItems(TOOLBAR_CATEGORIES.DESIGN_MODE);
            this.sticky = this.getCategoryItems(TOOLBAR_CATEGORIES.STICKY);
        }

        if (!this.urlParser.isAbsoluteFormLayout() || this.urlParser.isShowingContainer()) {
            this.zoom = this.getCategoryItems(TOOLBAR_CATEGORIES.ZOOM);
        }

        this.btnZoomOut.enabled = this.urlParser.isShowingContainer() != null;
        const promise = this.editorSession.isShowData();
        void promise.then((result: boolean) => {
            this.btnToggleShowData.state = result;
        });
        const wireframePromise = this.editorSession.isShowWireframe();
        void wireframePromise.then((result: boolean) => {
            this.btnToggleDesignMode.state = result;
            this.editorSession.getState().showWireframe = result;
            this.editorSession.stateListener.next('showWireframe');
            // always send showwireframe because this will also display the ghosts
            this.editorContentService.executeOnlyAfterInit(() => {
                this.editorContentService.sendMessageToIframe({ id: 'showWireframe', value: result });
            });
            // TODO:
            // this.editorSession.setContentSizes();
        });
        const highlightPromise = this.editorSession.isShowHighlight();
        void highlightPromise.then((result: boolean) => {
            this.btnHighlightWebcomponents.state = result;
            this.editorSession.fireHighlightChangedListeners(result);
        });
        const hideInheritedPromise = this.editorSession.isHideInherited();
        void hideInheritedPromise.then((result: boolean) => {
            this.btnHideInheritedElements.state = result;
            if (result) {
                this.applyHideInherited(result);
            }
        });
        
        const solutionLayoutsCssPromise = this.editorSession.isShowSolutionLayoutsCss();
        void solutionLayoutsCssPromise.then((result: boolean) => {
            if (!result) {
                this.btnSolutionCss.text = TOOLBAR_CONSTANTS.COMPONENTS_CSS;
                this.setSolutionLayoutsCss(result);
            }
            this.editorSession.getState().showSolutionSpecificLayoutContainerClasses = result;
        });
        const solutionCssPromise = this.editorSession.isShowSolutionCss();
        void solutionCssPromise.then((result: boolean) => {
            if (!result) {
            this.btnSolutionCss.text = TOOLBAR_CONSTANTS.NO_CSS;
            this.editorContentService.executeOnlyAfterInit(() => {
                this.setShowSolutionCss(result);
            });
        }
            this.editorSession.getState().showSolutionCss = result;
        });
        const zoomLevelPromise = this.editorSession.getZoomLevel();
        void zoomLevelPromise.then((result: number) => {
            if (result) {
                this.btnSetMaxLevelContainer.initialValue = result;
                this.editorSession.getState().maxLevel = result;
                this.editorContentService.executeOnlyAfterInit(() => {
                    this.editorContentService.sendMessageToIframe({ id: 'maxLevel', value: result });
                });
                this.editorSession.setZoomLevel(result);
            }
        });

        const showI18NValuesPromise = this.editorSession.isShowI18NValues();
        void showI18NValuesPromise.then((result: boolean) => {
            if (result) {
                this.btnToggleI18NValues.text = 'Show I18N text as keys';
                this.btnToggleI18NValues.state = true;
            }
        });

        if (this.editorContentService.getDesignerElementById('errorsDiv') !== null) {
            this.btnShowErrors.enabled = true;
            if (this.editorContentService.getDesignerElementById('closeErrors')) {
                this.editorContentService.getDesignerElementById('closeErrors').addEventListener('click', () => {
                    this.btnShowErrors.state = !this.btnShowErrors.state;
                    this.editorContentService.getDesignerElementById('errorsDiv').style.display = this.btnShowErrors.state ? 'block' : 'none';
                });
            }
        }
    }

    createItems() {
        this.btnPlaceField = new ToolbarItem(
            'Place Field Wizard',
            'toolbar/icons/field_wizard.png',
            true,
            () => {
                this.editorSession.openElementWizard('field')
            }
        );

        this.btnHighlightWebcomponents = new ToolbarItem(
            'Highlight webcomponents',
            'toolbar/icons/highlight.png',
            true,
            () => {
                const promise = this.editorSession.toggleHighlight();
                void promise.then((result: boolean) => {
                    this.btnHighlightWebcomponents.state = result;
                    this.editorSession.fireHighlightChangedListeners(result);
                })
            }
        );
        this.btnHighlightWebcomponents.state = true;

        if (this.urlParser.isAbsoluteFormLayout()) {
            this.btnToggleDynamicGuides = new ToolbarItem(
                'Toggle Dynamic Guides',
                'toolbar/icons/dynamicGuides.png',
                true,
                () => {
                    const promise = this.editorSession.toggleShowDynamicGuides();
                    void promise.then((result: boolean) => {
                        this.btnToggleDynamicGuides.state = result;
                        this.editorSession.fireShowDynamicGuidesChangedListeners(result);
                    })
                }
            );
        }

        this.add(this.btnPlaceField, TOOLBAR_CATEGORIES.ELEMENTS);
        this.add(this.btnHighlightWebcomponents, TOOLBAR_CATEGORIES.ELEMENTS);
        if (this.urlParser.isAbsoluteFormLayout()) { 
            this.add(this.btnToggleDynamicGuides, TOOLBAR_CATEGORIES.ELEMENTS);
        }


        this.btnToggleShowData = new ToolbarItem(
            'Data',
            'toolbar/icons/import.gif',
            true,
            () => {
                this.editorSession.toggleShowData();
            }
        );
        this.btnToggleShowData.state = false;

        this.btnToggleDesignMode = new ToolbarItem(
            'Exploded view',
            'toolbar/icons/exploded-view.png',
            true,
            () => {
                const promise = this.editorSession.toggleShowWireframe();
                void promise.then((result: boolean) => {
                    this.btnToggleDesignMode.state = result;
                    this.editorSession.getState().showWireframe = result;
                    this.editorContentService.sendMessageToIframe({ id: 'showWireframe', value: result });
                    // wait for css classes to be applied
                    setTimeout(()=>{this.editorSession.stateListener.next('showWireframe');}, 300);
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
                if (selection == TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS) {
                    if (!this.editorSession.getState().showSolutionCss) {
                        this.toggleShowSolutionCss();
                    }
                    if (!this.editorSession.getState().showSolutionSpecificLayoutContainerClasses) {
                        this.toggleShowSolutionLayoutsCss();
                    }
                }
                if (selection == TOOLBAR_CONSTANTS.COMPONENTS_CSS) {
                    if (!this.editorSession.getState().showSolutionCss) {
                        this.toggleShowSolutionCss();
                    }
                    if (this.editorSession.getState().showSolutionSpecificLayoutContainerClasses) {
                        this.toggleShowSolutionLayoutsCss();
                    }
                }
                if (selection == TOOLBAR_CONSTANTS.NO_CSS) {
                    if (this.editorSession.getState().showSolutionCss) {
                        this.toggleShowSolutionCss();
                    }
                    if (!this.editorSession.getState().showSolutionSpecificLayoutContainerClasses) {
                        this.toggleShowSolutionLayoutsCss();
                    }
                }
            }
        );
        this.btnSolutionCss.tooltip = 'Enable/disable solution css';
        this.btnSolutionCss.getIconStyle = (selection) => {
            if (selection == TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS) {
                return { 'background-image': TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS_ICON };
            }
            if (selection == TOOLBAR_CONSTANTS.COMPONENTS_CSS) {
                return { 'background-image': TOOLBAR_CONSTANTS.COMPONENTS_CSS_ICON };
            }
            if (selection == TOOLBAR_CONSTANTS.NO_CSS) {
                return { 'background-image': TOOLBAR_CONSTANTS.NO_CSS_ICON };
            }
        };
        this.btnSolutionCss.list = [
            { 'text': TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.LAYOUTS_COMPONENTS_CSS_ICON } },
            { 'text': TOOLBAR_CONSTANTS.COMPONENTS_CSS, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.COMPONENTS_CSS_ICON } },
            { 'text': TOOLBAR_CONSTANTS.NO_CSS, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.NO_CSS_ICON } }
        ];
        this.btnSolutionCss.onselection = (selection) => {
            this.btnSolutionCss.onclick(selection);
            return selection;
        }

        this.add(this.btnToggleShowData, TOOLBAR_CATEGORIES.SHOW_DATA);
        this.add(this.btnToggleDesignMode, TOOLBAR_CATEGORIES.DESIGN_MODE);
        this.add(this.btnSolutionCss, TOOLBAR_CATEGORIES.DESIGN_MODE);


        this.btnTabSequence = new ToolbarItem(
            'Set tab sequence',
            'images/th_horizontal.png',
            true,
            () => {
                const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    this.editorSession.executeAction('setTabSequence');
                }
            }
        );
        this.btnTabSequence.disabledIcon = 'images/th_horizontal-disabled.png';
        this.btnTabSequence.tooltip = 'Sets tab sequence (tabSeq property) on each selected component based on order of selection. There must be at least two selected components.';
        
        this.btnZoomIn = new ToolbarItem(
            'Zoom in',
            'images/zoom_in.png',
            false,
            () => {
                this.editorSession.executeAction('zoomIn');
            }
        );
        this.btnZoomIn.disabledIcon = 'images/zoom_in_disabled.png';

        this.btnZoomOut = new ToolbarItem(
            'Zoom out',
            'images/zoom_out.png',
            false,
            () => {
                this.editorSession.executeAction('zoomOut');
            }
        );
        this.btnZoomOut.disabledIcon = 'images/zoom_out_disabled.png';

        this.btnSetMaxLevelContainer = new ToolbarItem(
            'Maximum level for a container to be fully displayed',
            null,
            () => {
                return this.editorSession.getState().showWireframe;
            },
            (value) => {
                const lvl = parseInt(value);
                this.editorSession.getState().maxLevel = lvl;
                this.editorContentService.sendMessageToIframe({ id: 'maxLevel', value: value });
                this.editorSession.setZoomLevel(lvl);
            }
        );
        this.btnSetMaxLevelContainer.max = 10;
        this.btnSetMaxLevelContainer.min = 3;
        this.btnSetMaxLevelContainer.decbutton_text = 'Decrease maximum display level';
        this.btnSetMaxLevelContainer.decIcon = 'images/zoom_out_xs.png';
        this.btnSetMaxLevelContainer.incbutton_text = 'Increase maximum display level';
        this.btnSetMaxLevelContainer.incIcon = 'images/zoom_in_xs.png';
        this.btnSetMaxLevelContainer.state = false;

		// deprecated
        /*this.btnSaveAsTemplate = new ToolbarItem(
            'Save as template...',
            'toolbar/icons/template_save.png',
            true,
            () => {
                this.editorSession.openElementWizard('saveastemplate');
            }
        );*/

        this.add(this.btnZoomIn, TOOLBAR_CATEGORIES.ZOOM);
        this.add(this.btnZoomOut, TOOLBAR_CATEGORIES.ZOOM);
        this.add(this.btnSetMaxLevelContainer, TOOLBAR_CATEGORIES.ZOOM_LEVEL);
        this.add(this.btnTabSequence, TOOLBAR_CATEGORIES.FORM);
        //this.add(this.btnSaveAsTemplate, TOOLBAR_CATEGORIES.FORM);

        this.btnHideInheritedElements = new ToolbarItem(
            'Hide inherited elements',
            'images/hide_inherited.png',
            true,
            () => {
                if (this.btnHideInheritedElements.state) {
                    this.btnHideInheritedElements.state = false;
                    this.btnHideInheritedElements.text = 'Show inherited elements';
                } else {
                    this.btnHideInheritedElements.state = true;
                    this.btnHideInheritedElements.text = 'Hide inherited elements'
                }
                this.applyHideInherited(this.btnHideInheritedElements.state);
                this.editorSession.executeAction('toggleHideInherited');
            }
        );
        this.btnHideInheritedElements.disabledIcon = 'images/hide_inherited-disabled.png';
        this.btnHideInheritedElements.state = false;

        this.btnVisualFeedbackOptions = new ToolbarItem(
            '',
            'images/grid.png',
            true,
            null
        );

        this.btnVisualFeedbackOptions.tooltip = 'Visual feedback options';

        this.btnVisualFeedbackOptions.list = [
            { 'text': TOOLBAR_CONSTANTS.ANCHOR_INDICATOR, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.CHECK_ICON }, 'tooltip': 'Whether anchor indicator (hint image) is shown for a selected component.' },
            { 'text': TOOLBAR_CONSTANTS.SAME_SIZE, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.CHECK_ICON } , 'tooltip': 'Whether same width and same height indicators (hint images) are shown for a selected component and all components that match its width or height.'}
        ];

        this.btnVisualFeedbackOptions.onselection = (selection) => {
            if (selection == TOOLBAR_CONSTANTS.SAME_SIZE) {
                this.editorSession.setSameSizeIndicator(!this.editorSession.getState().sameSizeIndicator);
                if (this.editorSession.getState().sameSizeIndicator) {
                    this.btnVisualFeedbackOptions.list[1].iconStyle = { 'background-image': TOOLBAR_CONSTANTS.CHECK_ICON };
                }
                else {
                    this.btnVisualFeedbackOptions.list[1].iconStyle = { 'background-image': 'none' };
                }
            }
            if (selection == TOOLBAR_CONSTANTS.ANCHOR_INDICATOR) {
                this.editorSession.setAnchoringIndicator(!this.editorSession.getState().anchoringIndicator);
                if (this.editorSession.getState().anchoringIndicator) {
                    this.btnVisualFeedbackOptions.list[0].iconStyle = { 'background-image': TOOLBAR_CONSTANTS.CHECK_ICON };
                }
                else {
                    this.btnVisualFeedbackOptions.list[0].iconStyle = { 'background-image': 'none' };
                }
            }
            return selection;
        }


        this.add(this.btnHideInheritedElements, TOOLBAR_CATEGORIES.DISPLAY);
        this.add(this.btnVisualFeedbackOptions, TOOLBAR_CATEGORIES.DISPLAY);

        this.btnOrderingActionsCSSForm = new ToolbarItem(
            '',
            'images/send_to_back.png',
            true,
            null
        );
        
        this.btnOrderingActionsCSSForm.tooltip = 'Form Index (zIndex) ordering actions applied to selected element(s).If there is no selection or selected element doesn\'t have any overlapping neighbour components it doesn\'t do anything. It modifies the formIndex of selected element(s) and all the elements it has common space with.';
        
        this.btnOrderingActionsCSSForm.list = [
            { 'text': TOOLBAR_CONSTANTS.BRING_FORWARD, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.BRING_FORWARD_ICON }, 'tooltip': 'Moves selected element(s) one step up in zIndex layers.' },
            { 'text': TOOLBAR_CONSTANTS.SEND_BACKWARD, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.SEND_BACKWARD_ICON }, 'tooltip': 'Moves selected element(s) one step down in zIndex layers.' },
            { 'text': TOOLBAR_CONSTANTS.BRING_TO_FRONT, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.BRING_TO_FRONT_ICON }, 'tooltip': 'Moves selected element(s) to top so it is always fully visible (top most zIndex layer)' },
            { 'text': TOOLBAR_CONSTANTS.SEND_TO_BACK, 'iconStyle': { 'background-image': TOOLBAR_CONSTANTS.SEND_TO_BACK_ICON }, 'tooltip': 'Moves selected element(s) to bottom so it is the least visible one (all the elements it intersects will be on top of it).' }
        ];

        this.btnOrderingActionsCSSForm.onselection = (selection) => {
            if (selection == TOOLBAR_CONSTANTS.BRING_FORWARD) {
                 this.editorSession.executeAction('z_order_bring_to_front_one_step');
            }
            if (selection == TOOLBAR_CONSTANTS.SEND_BACKWARD) {
                 this.editorSession.executeAction('z_order_send_to_back_one_step');
            }
            if (selection == TOOLBAR_CONSTANTS.BRING_TO_FRONT) {
                 this.editorSession.executeAction('z_order_bring_to_front');
            }
            if (selection == TOOLBAR_CONSTANTS.SEND_TO_BACK) {
                 this.editorSession.executeAction('z_order_send_to_back');
            }
            return selection;
        }


        this.btnMoveUp = new ToolbarItem(
            'Move to left inside parent container',
            'images/move_back.png',
            false,
            () => {
                this.editorSession.executeAction('responsive_move_up');
            }
        );
        this.btnMoveUp.disabledIcon = 'images/move_back-disabled.png';

        this.btnMoveDown = new ToolbarItem(
            'Move to right inside parent container',
            'images/move_forward.png',
            false,
            () => {
                this.editorSession.executeAction('responsive_move_down');
            }
        );
        this.btnMoveDown.disabledIcon = 'images/move_forward-disabled.png';

        this.add(this.btnMoveUp, TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);
        this.add(this.btnMoveDown, TOOLBAR_CATEGORIES.ORDERING_RESPONSIVE);


         this.btnAlignActions = new ToolbarItem(
            '',
            'images/alignleft.png',
            true,
            null
        );
        
        this.btnAlignActions.tooltip = 'Align actions that will be applied to selected components. At least two components must be selected, otherwise will have no effect.';
        
        this.btnAlignActions.list = [
            { 'text': TOOLBAR_CONSTANTS.ALIGN_LEFT, 'iconStyle': { 'background-image': 'url(designer/assets/images/alignleft.png)' }, 'tooltip': 'Changes left position of all selected components to be the same as the position of left most component.' },
            { 'text': TOOLBAR_CONSTANTS.ALIGN_RIGHT, 'iconStyle': { 'background-image': 'url(designer/assets/images/alignright.png)' }, 'tooltip': 'Changes right position of all selected components to be the same as the position of right most.' },
            { 'text': TOOLBAR_CONSTANTS.ALIGN_TOP, 'iconStyle': { 'background-image': 'url(designer/assets/images/aligntop.png)' }, 'tooltip': 'Changes top position of all selected components to be the same as the position of top most component.' },
            { 'text': TOOLBAR_CONSTANTS.ALIGN_BOTTOM, 'iconStyle': { 'background-image': 'url(designer/assets/images/alignbottom.png)' }, 'tooltip': 'Changes bottom position of all selected components to be the same as the position of bottom most component.' },
            { 'text': TOOLBAR_CONSTANTS.ALIGN_CENTER, 'iconStyle': { 'background-image': 'url(designer/assets/images/aligncenter.png)' }, 'tooltip': 'Changes left position of all selected components so all components are vertically centered compared to component that is first selected.' },
            { 'text': TOOLBAR_CONSTANTS.ALIGN_MIDDLE, 'iconStyle': { 'background-image': 'url(designer/assets/images/alignmid.png)' }, 'tooltip': 'Changes top position of all selected components so all components are horizontally centered compared to component that is first selected.' },
            { 'text': TOOLBAR_CONSTANTS.SAME_WIDTH, 'iconStyle': { 'background-image': 'url(designer/assets/images/same_width.png)' }, 'tooltip': 'Changes width of all selected components to be the same as the width of component that is first selected.' },
            { 'text': TOOLBAR_CONSTANTS.SAME_HEIGHT, 'iconStyle': { 'background-image': 'url(designer/assets/images/same_height.png)' }, 'tooltip': 'Changes height of all selected components to be the same as the height of component that is first selected.' }
        ];

        this.btnAlignActions.onselection = (action) => {
            if (action == TOOLBAR_CONSTANTS.SAME_WIDTH) {
                 this.editorSession.sameSize(true);
            }
            if (action == TOOLBAR_CONSTANTS.SAME_HEIGHT) {
                 this.editorSession.sameSize(false);
            }
            if (action == TOOLBAR_CONSTANTS.ALIGN_LEFT) {
                const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    const obj = {};
                    let left: number = null;
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, true, false);
                            if (left == null) {
                                left = elementRect.x;
                            } else if (left > elementRect.x) {
                                left = elementRect.x;
                            }
                        }
                    }
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, false, true);
                            if (elementRect.x != left) {
                                obj[nodeid] = {
                                    x: left,
                                    y: elementRect.y
                                };
                            }
                        }
                    }
                    this.editorSession.sendChanges(obj);
                    this.updateSelection(); 
                }
            }
            if (action == TOOLBAR_CONSTANTS.ALIGN_RIGHT) {
                const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    const obj = {};
                    let right = null;
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, true, false);
                            if (right == null) {
                                right = elementRect.x + elementRect.width;
                            } else if (right < (elementRect.x + elementRect.width)) {
                                right = elementRect.x + elementRect.width;
                            }
                        }
                    }
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, false, true);
                            if ((elementRect.x + elementRect.width) != right) {
                                obj[nodeid] = {
                                    x: (right - elementRect.width),
                                    y: elementRect.y
                                };
                            }
                        }
                    }
                    this.editorSession.sendChanges(obj);
                    this.updateSelection();   
                }
            }
            if (action == TOOLBAR_CONSTANTS.ALIGN_TOP) {
                const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    const obj = {};
                    let top: number = null;
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, false, true);
                            if (top == null) {
                                top = elementRect.y;
                            } else if (top > elementRect.y) {
                                top = elementRect.y;
                            }
                        }
                    }
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, true, false);
                            if (elementRect.y != top) {
                                obj[nodeid] = {
                                    x: elementRect.x,
                                    y: top
                                };
                            }
                        }
                    }
                    this.editorSession.sendChanges(obj);
                    this.updateSelection();   
                }
            }
            if (action == TOOLBAR_CONSTANTS.ALIGN_BOTTOM) {
                const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    const obj = {};
                    let bottom = null;
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, false, true);
                            if (bottom == null) {
                                bottom = elementRect.y + elementRect.height;
                            } else if (bottom < (elementRect.y + elementRect.height)) {
                                bottom = elementRect.y + elementRect.height;
                            }
                        }
                    }
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, true, false);
                            if ((elementRect.y + elementRect.height) != bottom) {
                                obj[nodeid] = {
                                    x: elementRect.x,
                                    y: (bottom - elementRect.height)
                                };
                            }
                        }
                    }
                    this.editorSession.sendChanges(obj);
                    this.updateSelection();   
                }
            }
             if (action == TOOLBAR_CONSTANTS.ALIGN_CENTER) {
                  const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    const obj = {};
                    let centerElementModel: DOMRect = null;
                    const sortedSelection: Array<DOMRect> = [];
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, true, false);
                            if (sortedSelection.length == 0) {
                                sortedSelection[0] = elementRect;
                            } else {
                                let insertIndex = sortedSelection.length;
                                for (let j = 0; j < sortedSelection.length; j++) {
                                    if ((elementRect.x + elementRect.width / 2) < (sortedSelection[j].x + sortedSelection[j].width / 2)) {
                                        insertIndex = j;
                                        break;
                                    }
                                }
                                sortedSelection.splice(insertIndex, 0, elementRect);
                            }
                        }
                    }
                    centerElementModel = sortedSelection[Math.round((sortedSelection.length - 1) / 2)];
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, false, true);
                            if (elementRect.x != centerElementModel.x || elementRect.y != centerElementModel.y) {
                                obj[nodeid] = {
                                    x: (centerElementModel.x + centerElementModel.width / 2 - elementRect.width / 2),
                                    y: elementRect.y
                                };
                            }
                        }
                    }
                    this.editorSession.sendChanges(obj);
                    this.updateSelection();   
                }
            }
            if (action == TOOLBAR_CONSTANTS.ALIGN_MIDDLE) {
                 const selection = this.editorSession.getSelection();
                if (selection && selection.length > 1) {
                    const obj = {};
                    let centerElementModel: DOMRect = null;
                    const sortedSelection: Array<DOMRect> = [];
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, false, true);
                            if (sortedSelection.length == 0) {
                                sortedSelection[0] = elementRect;
                            } else {
                                let insertIndex = sortedSelection.length;
                                for (let j = 0; j < sortedSelection.length; j++) {
                                    if ((elementRect.y + elementRect.height / 2) < (sortedSelection[j].y + sortedSelection[j].height / 2)) {
                                        insertIndex = j;
                                        break;
                                    }
                                }
                                sortedSelection.splice(insertIndex, 0, elementRect);
                            }
                        }
                    }
                    centerElementModel = sortedSelection[Math.round((sortedSelection.length - 1) / 2)];
                    for (let i = 0; i < selection.length; i++) {
                        const nodeid = selection[i];
                        const element = this.editorContentService.getContentElement(nodeid);
                        if (element) {
                            const elementRect = element.getBoundingClientRect();
                            this.updateElementPositionUsingParentPosition(element, elementRect, true, false);
                            if (elementRect.x != centerElementModel.x || elementRect.y != centerElementModel.y) {
                                obj[nodeid] = {
                                    x: elementRect.x,
                                    y: (centerElementModel.y + centerElementModel.height / 2 - elementRect.height / 2)
                                };
                            }
                        }
                    }
                    this.editorSession.sendChanges(obj);
                    this.updateSelection();   
                }
            }
            return action;
        }

        this.btnSpaceDistributionActions = new ToolbarItem(
            '',
            'images/distribute_hcenters.png',
            true,
            null
        );
        
        this.btnSpaceDistributionActions.tooltip = 'Space distribution actions between selected components (horizontal or vertical space). At least three components must be selected, otherwise will have no effect.';
        
        this.btnSpaceDistributionActions.list = [
            { 'text': TOOLBAR_CONSTANTS.HORIZONTAL_SPACING, 'iconStyle': { 'background-image': 'url(designer/assets/images/distribute_hspace.png)' }, 'tooltip': 'Changes left position of all selected components so the in between horizontal space is distributed based on the horizontal position of selected elements.' },
            { 'text': TOOLBAR_CONSTANTS.HORIZONTAL_CENTERS, 'iconStyle': { 'background-image': 'url(designer/assets/images/distribute_hcenters.png)' }, 'tooltip': 'Changes left position of all selected components so the in between horizontal space is distributed based on the horizontal center of selected elements.' },
            { 'text': TOOLBAR_CONSTANTS.LEFTWARD, 'iconStyle': { 'background-image': 'url(designer/assets/images/distribute_leftward.png)' }, 'tooltip': 'Changes left position of all selected components so the in between horizontal space is distributed toward the left-most element of selected elements.' },
            { 'text': TOOLBAR_CONSTANTS.VERTICAL_SPACING, 'iconStyle': { 'background-image': 'url(designer/assets/images/distribute_vspace.png)' }, 'tooltip': 'Changes top position of all selected components so the in between vertical space is distributed based on the vertical position of selected elements.' },
            { 'text': TOOLBAR_CONSTANTS.VERTICAL_CENTERS, 'iconStyle': { 'background-image': 'url(designer/assets/images/distribute_vcenters.png)' }, 'tooltip': 'Changes top position of all selected components so the in between vertical space is distributed based on the vertical center of selected elements.' },
            { 'text': TOOLBAR_CONSTANTS.UPWARD, 'iconStyle': { 'background-image': 'url(designer/assets/images/distribute_upward.png)' }, 'tooltip': 'Changes top position of all selected components so the in between vertical space is distributed toward the top-most element of selected elements.' }
        ];

        this.btnSpaceDistributionActions.onselection = (action) => {
            if (action == TOOLBAR_CONSTANTS.HORIZONTAL_SPACING) {
                 this.editorSession.executeAction('horizontal_spacing');
            }
            if (action == TOOLBAR_CONSTANTS.HORIZONTAL_CENTERS) {
                 this.editorSession.executeAction('horizontal_centers');
            }
            if (action == TOOLBAR_CONSTANTS.LEFTWARD) {
                  this.editorSession.executeAction('horizontal_pack');
            }
            if (action == TOOLBAR_CONSTANTS.VERTICAL_SPACING) {
                this.editorSession.executeAction('vertical_spacing');
            }
            if (action == TOOLBAR_CONSTANTS.VERTICAL_CENTERS) {
                 this.editorSession.executeAction('vertical_centers');
            }
            if (action == TOOLBAR_CONSTANTS.UPWARD) {
                 this.editorSession.executeAction('vertical_pack');
            }
            this.updateSelection();
            return action;
        }
        
        this.btnReload = new ToolbarItem(
            'Reload designer (use when component changes must be reflected)',
            'images/reload.png',
            true,
            () => {
                this.editorSession.executeAction('reload');
            }
        );

        this.add(this.btnReload, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);

        this.btnToggleI18NValues = new ToolbarItem(
            'Show resolved I18N text',
            'toolbar/icons/i18n.png',
            true,
            () => {
                if (this.btnToggleI18NValues.state) {
                    this.btnToggleI18NValues.state = false;
                    this.btnToggleI18NValues.text = 'Show resolved I18N text';
                } else {
                    this.btnToggleI18NValues.state = true;
                    this.btnToggleI18NValues.text = 'Show I18N text as keys';
                }
                void this.editorSession.toggleShowI18NValues();
            }
        );
        this.btnToggleI18NValues.state = false;
        this.add(this.btnToggleI18NValues, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);

        this.btnShowErrors = new ToolbarItem(
            'Show errors console',
            'toolbar/icons/error.png',
            false,
            () => {
                this.btnShowErrors.state = !this.btnShowErrors.state;
                this.editorContentService.getDesignerElementById('errorsDiv').style.display = this.btnShowErrors.state ? 'block' : 'none';
            }
        );
        this.btnShowErrors.disabledIcon = 'toolbar/icons/disabled_error.png';
        this.btnShowErrors.state = false;

        this.add(this.btnShowErrors, TOOLBAR_CATEGORIES.STANDARD_ACTIONS);
    }

    add(item: ToolbarItem, category: TOOLBAR_CATEGORIES) {
        if (!this.items.has(category)) {
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

    toggleShowSolutionLayoutsCss() {
        const promise = this.editorSession.toggleShowSolutionLayoutsCss();
        void promise.then((result: boolean) => {
            this.editorSession.getState().showSolutionSpecificLayoutContainerClasses = result;
            this.setSolutionLayoutsCss(result);
        });
    }

    toggleShowSolutionCss() {
        const promise = this.editorSession.toggleShowSolutionCss();
        void promise.then((result: boolean) => {
            this.editorSession.getState().showSolutionCss = result;
            this.setShowSolutionCss(result);
        });
    }

    setSolutionLayoutsCss(state: boolean) {
        this.editorContentService.querySelectorAllInContent('[svy-solution-layout-class]').forEach(element => {
            const classes = element.getAttribute('svy-solution-layout-class');
            if (classes) {
                classes.split(' ').forEach(cssclass => {
                    if (state) {
                        this.renderer.addClass(element, cssclass);
                    }
                    else {
                        this.renderer.removeClass(element, cssclass);
                    }
                });
            }
        });
    }

    setShowSolutionCss(state) {
        this.editorContentService.querySelectorAllInContent('link[svy-stylesheet]').forEach(link  => {
            const htmlLink = link as HTMLLinkElement;
            htmlLink.disabled = !state;
        });
    }

    selectionChanged(selection: Array<string>): void {
        // do we need to enable/disable the actions ? maybe just keep them always enabled
        //this.btnTabSequence.enabled = selection.length > 1;
        //this.btnSameWidth.enabled = selection.length > 1;
        //this.btnSameHeight.enabled = selection.length > 1;
        this.btnZoomOut.enabled = this.urlParser.isShowingContainer() != null;
        if (this.urlParser.isAbsoluteFormLayout()) {
            //this.btnDistributeHorizontalSpacing.enabled = selection.length > 2;
           // this.btnDistributeHorizontalCenters.enabled = selection.length > 2;
            //this.btnDistributeLeftward.enabled = selection.length > 2;
            //this.btnDistributeVerticalSpacing.enabled = selection.length > 2;
            //this.btnDistributeVerticalCenters.enabled = selection.length > 2;
            //this.btnDistributeUpward.enabled = selection.length > 2;

            //this.btnLeftAlign.enabled = selection.length > 1;
            //this.btnRightAlign.enabled = selection.length > 1;
           // this.btnTopAlign.enabled = selection.length > 1;
           // this.btnBottomAlign.enabled = selection.length > 1;
            //this.btnCenterAlign.enabled = selection.length > 1;
            //this.btnMiddleAlign.enabled = selection.length > 1;
            //this.btnBringForward.enabled = selection.length > 0;
            //this.btnSendBackward.enabled = selection.length > 0;
            //this.btnBringToFront.enabled = selection.length > 0;
            //this.btnSendToBack.enabled = selection.length > 0;
        }
        else {
            this.btnMoveUp.enabled = selection.length == 1;
            this.btnMoveDown.enabled = selection.length == 1;
            this.btnZoomIn.enabled = selection.length == 1;
        }
    }

    applyHideInherited(hideInherited: boolean) {
        this.editorContentService.executeOnlyAfterInit(() => {
            
            this.updateSelection(hideInherited); //this is removing selection for hidden element

            const elements = this.editorContentService.querySelectorAllInContent('.inherited_element').concat(Array.from(this.editorContentService.querySelectorAll('.inherited_element')));
            elements.forEach((node) => {
                if (hideInherited) {
                    this.renderer.setStyle(node, 'visibility', 'hidden');
                }
                else {
                    this.renderer.setStyle(node, 'visibility', 'visible');
                    //if there is only one eleent in the current selection we must trigger a redraw decorators
                    const selection = this.editorSession.getSelection();
                    if (selection.length === 1) {
                        this.editorSession.updateSelection(selection, true);
                    }
                }
            });
            
            this.updateSameSizeIndicator(hideInherited);
        });
    }

    updateSameSizeIndicator(hideInherited: boolean) {
        if (hideInherited) {
            const initialSelection = this.editorSession.getSelection(); //note: in this context the hidden elements are no longer returned
            const elements = this.editorContentService.getAllContentElements(); //return all elements having an svy-id
            const filteredSvyIds: string[] = [];
    
            elements.forEach((element) => {
                let wrapper = element.parentElement;
                while (wrapper && !wrapper.classList.contains('svy-wrapper')) {
                    wrapper = wrapper.parentElement;
                }
    
                if (!(wrapper && wrapper.classList.contains('inherited_element'))) {
                    filteredSvyIds.push(element.getAttribute('svy-id'));
                }
            });
            this.editorSession.updateSelection(filteredSvyIds, true); //this is changing also the initial selection so we need to restore
            this.editorSession.setSelection(initialSelection);
        }
    }

    updateSelection(hideInherited?: boolean){
        if (hideInherited === undefined) {
           setTimeout(()=>{this.editorSession.setSelection(this.editorSession.getSelection());}, 100);
        } else if (hideInherited) {
            const selection = this.editorSession.getSelection();
            const filteredSelection: string[] = [];
        
            selection.forEach((selectionId) => {
              let wrapper = this.editorContentService.getContentElement(selectionId);
        
              while (wrapper && !wrapper.classList.contains('svy-wrapper')) {
                wrapper = wrapper.parentElement;
              }
        
              if (!(wrapper && wrapper.classList.contains('inherited_element'))) {
                filteredSelection.push(selectionId);
              }
            });
            this.editorSession.setSelection(filteredSelection);
          }
	}
	
	updateElementPositionUsingParentPosition(element: HTMLElement, elementRect: DOMRect, posX: boolean, posY: boolean) {
		const parentFC = element.closest('.svy-formcomponent');
		if (parentFC && !element.classList.contains('svy-formcomponent')) {
			const parentRect = parentFC.getBoundingClientRect();
			if (posX) {
				elementRect.x = elementRect.x - parentRect.x;
			}
			if (posY) {
				elementRect.y = elementRect.y - parentRect.y;
			}
		}
	}

}

export class ToolbarItem {

    hide = false;
    style: string;
    disabledIcon: string;
    faIcon: string;
    list: Array<{ text: string; iconStyle?: { 'background-image': string }; tooltip?: string}>;
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
    onSet: (value: unknown) => void;

    constructor(
        public text: string,
        public icon: string,
        public enabled: (() => boolean) | boolean,
        public onclick: (text?: string) => void) {
    }
}