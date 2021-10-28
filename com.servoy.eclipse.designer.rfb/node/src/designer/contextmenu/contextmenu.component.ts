import { DOCUMENT } from '@angular/common';
import { Component, ElementRef, Inject, OnInit, ViewChild } from '@angular/core';
import { GHOST_TYPES } from '../ghostscontainer/ghostscontainer.component';
import { EditorSessionService, PaletteComp } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';

export enum SHORTCUT_IDS {
    SET_TAB_SEQUENCE_ID = 'com.servoy.eclipse.designer.rfb.settabseq',
    SAME_WIDTH_ID = 'com.servoy.eclipse.designer.rfb.samewidth',
    SAME_HEIGHT_ID = 'com.servoy.eclipse.designer.rfb.sameheight',
    TOGGLE_ANCHORING_TOP_ID = 'com.servoy.eclipse.designer.rfb.anchorTop',
    TOGGLE_ANCHORING_RIGHT_ID = 'com.servoy.eclipse.designer.rfb.anchorRight',
    TOGGLE_ANCHORING_BOTTOM_ID = 'com.servoy.eclipse.designer.rfb.anchorBottom',
    TOGGLE_ANCHORING_LEFT_ID = 'com.servoy.eclipse.designer.rfb.anchorLeft',
    BRING_TO_FRONT_ONE_STEP_ID = 'com.servoy.eclipse.designer.rfb.bringtofrontonestep',
    SEND_TO_BACK_ONE_STEP_ID = 'com.servoy.eclipse.designer.rfb.sendtobackonestep',
    BRING_TO_FRONT_ID = 'com.servoy.eclipse.designer.rfb.bringtofront',
    SEND_TO_BACK_ID = 'com.servoy.eclipse.designer.rfb.sendtoback',
    OPEN_SCRIPT_ID = 'com.servoy.eclipse.ui.OpenFormJsAction',
    OPEN_SUPER_SCRIPT_ID = 'com.servoy.eclipse.designer.rfb.openscripteditor',
    GROUP_ID = 'com.servoy.eclipse.designer.rfb.group',
    UNGROUP_ID = 'com.servoy.eclipse.designer.rfb.ungroup',
    OPEN_FORM_HIERARCHY_ID = 'com.servoy.eclipse.ui.OpenFormHierarchyAction'
}
@Component({
    selector: 'designer-contextmenu',
    templateUrl: './contextmenu.component.html',
    styleUrls: ['./contextmenu.component.css']
})
export class ContextMenuComponent implements OnInit {

    @ViewChild('element') element: ElementRef<HTMLElement>;
    contentArea: HTMLElement;

    menuItems: ContextmenuItem[];

    selection: string[];
    selectionAnchor = 0;

    constructor(protected readonly editorSession: EditorSessionService,
        protected urlParser: URLParserService, @Inject(DOCUMENT) private doc: Document) {
    }

    ngOnInit(): void {
        void this.editorSession.getShortcuts().then((shortcuts: { [key: string]: string; }) => {
            void this.editorSession.getSuperForms().then((superForms: Array<string>) => {
                this.setup(shortcuts, superForms);
            });
        });
    }

    private setup(shortcuts: { [key: string]: string; }, superForms: Array<string>): void {
        this.createItems(shortcuts, superForms);
        this.contentArea = this.doc.querySelector('.content-area');
        this.contentArea.addEventListener('contextmenu', (event: MouseEvent) => {
            let node: HTMLElement;
            const frameElem = this.doc.querySelector('iframe');
            this.selection = this.editorSession.getSelection();
            if (this.selection && this.selection.length == 1) {
                node = this.contentArea.querySelector("[svy-id='" + this.selection[0] + "']")
                if (node && node.hasAttribute('svy-ghosttype') && node.getAttribute('svy-ghosttype') === GHOST_TYPES.GHOST_TYPE_PART) {
                    event.preventDefault();
                    event.stopPropagation();
                    return;
                }
                node = frameElem.contentWindow.document.querySelector("[svy-id='" + this.selection[0] + "']");
                if (node && node.hasAttribute('svy-anchors')) {
                    this.selectionAnchor = parseInt(node.getAttribute('svy-anchors'));
                }
            }
            if (!node) {
                node = frameElem.contentWindow.document.querySelector('.svy-form');
            }
            if (node) {
                for (let i = 0; i < this.menuItems.length; i++) {
                    if (this.menuItems[i].text === 'Add') {
                        const allowedChildren = node.getAttribute('svy-types') != null ? [] : this.editorSession.getAllowedChildrenForContainer(node.getAttribute('svy-layoutname'));
                        const types = node.getAttribute('svy-types');
                        if (allowedChildren || types) {
                            this.menuItems[i].getItemClass = () => { return 'dropdown-submenu' };
                            this.menuItems[i].subMenu = [];
                            const typesArray: Array<{ type: string; property: string }> = [];

                            if (types) {
                                const typesA = types.slice(1, -1).split(',');
                                const propertiesA = node.getAttribute('svy-types-properties').slice(1, -1).split(',');

                                for (let x = 0; x < typesA.length; x++) {
                                    typesArray.push({ type: typesA[x], property: propertiesA[x] });
                                }
                            }
                            if (allowedChildren)
                                for (const child of allowedChildren) {
                                    const submenuItem = new ContextmenuItem(this.getDisplayName(child),
                                        () => {
                                            this.hide();
                                            let component: PaletteComp = {} as PaletteComp;
                                            if (node.getAttribute('svy-id')) component.dropTargetUUID = node.getAttribute('svy-id');

                                            if (child.indexOf('.') > 0) {
                                                const nameAndPackage = child.split('.');
                                                component.name = nameAndPackage[1];
                                                component.packageName = nameAndPackage[0];
                                            } else {
                                                component.name = child;
                                                component.packageName = undefined;
                                            }
                                            component = this.convertToContentPoint(component) as PaletteComp;
                                            this.editorSession.createComponent(component);
                                        });
                                    this.menuItems[i].subMenu.push(submenuItem);
                                }
                            for (const type of typesArray) {
                                const submenuItem = new ContextmenuItem(type.type + ' for ' + type.property,
                                    () => {
                                        this.hide();
                                        let component: PaletteComp = {} as PaletteComp;
                                        if (node.getAttribute('svy-id')) component.dropTargetUUID = node.getAttribute('svy-id');

                                        component.type = type.type;
                                        component.ghostPropertyName = type.property;

                                        component = this.convertToContentPoint(component) as PaletteComp;
                                        this.editorSession.createComponent(component);
                                    }
                                );
                                this.menuItems[i].subMenu.push(submenuItem);
                            }
                        }
                        else {
                            this.menuItems[i].getItemClass = function() { return 'invisible' };
                        }
                    }
                }
            }

            this.show(event);
            this.adjustMenuPosition(this.element.nativeElement);
            event.preventDefault();
            event.stopPropagation();
        });
        this.doc.body.addEventListener('click', () => {
            this.hide();
        });
        this.doc.body.addEventListener('keyup', (event: KeyboardEvent) => {
            if (event.keyCode == 27) {
                // esc key, close menu
                this.hide();
            }
        });
    }

    private show(event: MouseEvent) {
        this.element.nativeElement.style.display = 'block';
        this.element.nativeElement.style.left = event.pageX - this.contentArea.offsetLeft + 'px';
        this.element.nativeElement.style.top = event.pageY - this.contentArea.offsetTop + 'px';
    }

    private hide() {
        this.element.nativeElement.style.display = 'none';
    }

    public adjustMenuPosition(nativeElement?: HTMLElement) {
        const viewport = {
            top: window.pageYOffset,
            left: window.pageXOffset,
            right: window.pageXOffset + window.innerWidth,
            bottom: window.pageYOffset + window.innerHeight
        };

        if (nativeElement) {
            const bounds = this.getElementOffset(nativeElement);
            bounds.right = bounds.left + nativeElement.offsetWidth;
            bounds.bottom = bounds.top + nativeElement.offsetHeight;

            let left = bounds.left;
            let top = bounds.top;

            if (bounds.bottom > viewport.bottom) {
                //-10 to make it closer to the cursor
                top -= nativeElement.offsetHeight - 10;
            }
            if (bounds.right > viewport.right) {
                left -= nativeElement.offsetWidth - 10;
            }

            this.element.nativeElement.style.left = left - this.contentArea.offsetLeft + 'px';
            this.element.nativeElement.style.top = top - this.contentArea.offsetTop + 'px';
        }
        else {
            const submenu = this.doc.querySelector('.dropdown-submenu:hover');
            if (submenu) {
                const menu: HTMLElement = submenu.querySelector('.dropdown-menu');
                //the submenu can only be displayed on the right or left side of the contextmenu
                if (this.element.nativeElement.offsetWidth + this.getElementOffset(this.element.nativeElement).left + menu.offsetWidth > viewport.right) {
                    //+5 to make it overlap the menu a bit
                    menu.style.left = -1 * menu.offsetWidth + 5 + 'px';
                }
                else {
                    menu.style.left = this.element.nativeElement.offsetWidth - 5 + 'px';
                }
            }
        }
    }

    private getElementOffset(nativeElement: HTMLElement): { top: number; left: number; right?: number; bottom?: number } {
        const rect = nativeElement.getBoundingClientRect();
        const win = nativeElement.ownerDocument.defaultView;
        return {
            top: rect.top + win.pageYOffset,
            left: rect.left + win.pageXOffset
        };
    }

    private createItems(shortcuts: { [key: string]: string; }, forms: Array<string>) {
        this.menuItems = new Array<ContextmenuItem>();
        let entry: ContextmenuItem;

        entry = new ContextmenuItem(
            'Revert Form',
            () => {
                this.hide();
                this.editorSession.executeAction('revertForm');
            }
        );
        entry.getItemClass = () => {
            if (this.editorSession.isDirty() === true) {
                return 'enabled';
            } else {
                return 'disabled';
            }
        };
        this.menuItems.push(entry);

        entry = new ContextmenuItem(
            'Set Tab Sequence',
            () => {
                this.editorSession.executeAction('setTabSequence');
            }
        );
        entry.getItemClass = () => {
            if (!this.selection || this.selection.length < 2) {
                return 'disabled';
            }
            return ''
        };
        this.menuItems.push(entry);

        if (this.urlParser.isAbsoluteFormLayout()) {
            // sizing
            const sizingActions = new Array<ContextmenuItem>();

            entry = new ContextmenuItem(
                'Same Width',
                () => {
                    this.editorSession.sameSize(true);
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/same_width.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.SAME_WIDTH_ID];
            entry.getItemClass = () => {
                if (!this.selection || this.selection.length < 2) return 'disabled';
                return '';
            };
            sizingActions.push(entry);

            entry = new ContextmenuItem(
                'Same Height',
                () => {
                    this.editorSession.sameSize(false);
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/same_height.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.SAME_HEIGHT_ID];
            entry.getItemClass = () => {
                if (!this.selection || this.selection.length < 2) return 'disabled';
                return '';
            };
            sizingActions.push(entry);

            entry = new ContextmenuItem(
                'Sizing',
                null
            );
            entry.getItemClass = () => {
                return 'dropdown-submenu';
            };
            entry.subMenu = sizingActions;

            // anchoring
            const anchoringActions = new Array<ContextmenuItem>();
            if (!this.urlParser.isCSSPositionFormLayout()) {
                entry = new ContextmenuItem(
                    'Top',
                    () => {
                        this.setAnchoring(1, 4);
                    }
                );
                entry.getIconStyle = () => {
                    if (this.isAnchored(1)) {
                        return { 'background-image': 'url(designer/assets/images/check.png)' };
                    }
                    return null;
                };
                entry.shortcut = shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_TOP_ID];
                entry.getItemClass = () => {
                    if (!this.hasSelection(1) || !this.urlParser.isAbsoluteFormLayout()) return 'disabled';
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Right',
                    () => {
                        this.setAnchoring(2, 8);
                    }
                );
                entry.getIconStyle = () => {
                    if (this.isAnchored(2)) return { 'background-image': 'url(designer/assets/images/check.png)' };
                };
                entry.shortcut = shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_RIGHT_ID];
                entry.getItemClass = () => {
                    if (!this.hasSelection(1) || !this.urlParser.isAbsoluteFormLayout()) return 'disabled';
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Bottom',
                    () => {
                        this.setAnchoring(4, 1);
                    }
                );
                entry.getIconStyle = () => {
                    if (this.isAnchored(4)) return { 'background-image': 'url(designer/assets/images/check.png)' };
                };
                entry.shortcut = shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_BOTTOM_ID];
                entry.getItemClass = () => {
                    if (!this.hasSelection(1) || !this.urlParser.isAbsoluteFormLayout()) return 'disabled';
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Left',
                    () => {
                        this.setAnchoring(8, 2);
                    }
                );
                entry.getIconStyle = () => {
                    if (this.isAnchored(8)) return { 'background-image': 'url(designer/assets/images/check.png)' };
                };
                entry.shortcut = shortcuts[SHORTCUT_IDS.TOGGLE_ANCHORING_LEFT_ID];
                entry.getItemClass = () => {
                    if (!this.hasSelection(1) || !this.urlParser.isAbsoluteFormLayout()) return 'disabled';
                };
                anchoringActions.push(entry);
            } else {
                entry = new ContextmenuItem(
                    'Top/Left',
                    () => {
                        this.setCssAnchoring('0', '-1', '-1', '0');
                    }
                );
                entry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/anchor-top-left.png)' };
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Top/Right',
                    () => {
                        this.setCssAnchoring('0', '0', '-1', '-1');
                    }
                );
                entry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/anchor-top-right.png)' };
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Top/Left/Right',
                    () => {
                        this.setCssAnchoring('0', '0', '-1', '0');
                    }
                );
                entry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/anchor-top-left-right.png)' };
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Bottom/Left',
                    () => {
                        this.setCssAnchoring('-1', '-1', '0', '0');
                    }
                );
                entry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/anchor-bottom-left.png)' };
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Bottom/Right',
                    () => {
                        this.setCssAnchoring('-1', '0', '0', '-1');
                    }
                );
                entry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/anchor-bottom-right.png)' };
                };
                anchoringActions.push(entry);

                entry = new ContextmenuItem(
                    'Other...',
                    () => {
                        if (this.selection && this.selection.length > 0) {
                            this.editorSession.setCssAnchoring(this.selection, null);
                        }
                    }
                );
                entry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/anchor-bottom-right.png)' };
                };
                anchoringActions.push(entry);
            }

            entry = new ContextmenuItem(
                'Anchoring',
                null
            );
            entry.getItemClass = () => {
                return 'dropdown-submenu';
            };
            entry.subMenu = anchoringActions;
            this.menuItems.push(entry);

            //arrange
            const arrangeActions = new Array<ContextmenuItem>();

            entry = new ContextmenuItem(
                'Bring forward',
                () => {
                    this.hide();
                    this.editorSession.executeAction('z_order_bring_to_front_one_step');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/bring_forward.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.BRING_TO_FRONT_ONE_STEP_ID];
            entry.getItemClass = () => {
                if (!this.hasSelection()) return 'disabled';
            };
            arrangeActions.push(entry);

            entry = new ContextmenuItem(
                'Send backward',
                () => {
                    this.hide();
                    this.editorSession.executeAction('z_order_send_to_back_one_step');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/send_backward.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.SEND_TO_BACK_ONE_STEP_ID];
            entry.getItemClass = () => {
                if (!this.hasSelection()) return 'disabled';
            };
            arrangeActions.push(entry);

            entry = new ContextmenuItem(
                'Bring to front',
                () => {
                    this.hide();
                    this.editorSession.executeAction('z_order_bring_to_front');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/bring_to_front.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.BRING_TO_FRONT_ID];
            entry.getItemClass = () => {
                if (!this.hasSelection()) return 'disabled';
            };
            arrangeActions.push(entry);

            entry = new ContextmenuItem(
                'Send to back',
                () => {
                    this.hide();
                    this.editorSession.executeAction('z_order_send_to_back');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/send_to_back.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.SEND_TO_BACK_ID];
            entry.getItemClass = () => {
                if (!this.hasSelection()) return 'disabled';
            };
            arrangeActions.push(entry);

            entry = new ContextmenuItem(
                'Arrange',
                null
            );
            entry.getItemClass = () => {
                return 'dropdown-submenu';
            };
            entry.subMenu = arrangeActions;
            this.menuItems.push(entry);


            const groupingActions = new Array<ContextmenuItem>();

            entry = new ContextmenuItem(
                'Group',
                () => {
                    this.hide();
                    this.editorSession.executeAction('createGroup');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/group.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.GROUP_ID];
            entry.getItemClass = () => {
                if (!this.selection || this.selection.length < 2) return 'disabled';
            };
            groupingActions.push(entry);

            entry = new ContextmenuItem(
                'Ungroup',
                () => {
                    this.hide();
                    this.editorSession.executeAction('clearGroup');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/ungroup.png)' };
            };
            entry.shortcut = shortcuts[SHORTCUT_IDS.UNGROUP_ID];
            entry.getItemClass = () => {
                if (!this.hasSelection()) return 'disabled';
                for (let i = 0; i < this.selection.length; i++) {
                    const node = this.doc.querySelector("[svy-id='" + this.selection[i] + "']")
                    if (node && node.hasAttribute('svy-ghosttype') && node.getAttribute('svy-ghosttype') === GHOST_TYPES.GHOST_TYPE_GROUP) {
                        return '';
                    }
                }
                return 'disabled';
            };
            groupingActions.push(entry);

            entry = new ContextmenuItem(
                'Grouping',
                null
            );
            entry.getItemClass = () => {
                return 'dropdown-submenu'
            };
            entry.subMenu = groupingActions;
            this.menuItems.push(entry);
        } else { //this is an Responsive Layout
            entry = new ContextmenuItem(
                'Add',
                null
            );
            this.menuItems.push(entry);

            entry = new ContextmenuItem(
                'Zoom in',
                () => {
                    this.hide();
                    this.editorSession.executeAction('zoomIn');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/zoom_in.png)' };
            };
            entry.getItemClass = () => {
                if (this.hasSelection(1)) return 'enabled'; else return 'disabled'
            };
            this.menuItems.push(entry);
        }

        if (!this.urlParser.isAbsoluteFormLayout() || this.urlParser.isShowingContainer()) {
            entry = new ContextmenuItem(
                'Zoom out',
                () => {
                    this.hide();
                    this.editorSession.executeAction('zoomOut');
                }
            );
            entry.getIconStyle = () => {
                return { 'background-image': 'url(designer/assets/images/zoom_out.png)' };
            };
            entry.getItemClass = () => {
                if (this.urlParser.isShowingContainer()) return 'enabled'; else return 'disabled';
            };
            this.menuItems.push(entry);
        }

        entry = new ContextmenuItem(
            '',
            null
        );
        entry.getItemClass = () => {
            return 'dropdown-divider';
        };
        this.menuItems.push(entry);

        entry = new ContextmenuItem(
            'Save as template ...',
            () => {
                this.hide();
                this.editorSession.openElementWizard('saveastemplate');
            }
        );
        // entry.getIconStyle = () => {
        // 	return {'background-image':"url(designer/assets/images/template.png)"};
        // };
        this.menuItems.push(entry);

        entry = new ContextmenuItem(
            'Open in Script Editor',
            () => {
                this.hide();
                this.editorSession.executeAction('openScript');
            }
        );
        entry.getIconStyle = () => {
            return { 'background-image': 'url(designer/assets/images/js.png)' };
        };
        entry.shortcut = forms.length > 1 ? shortcuts[SHORTCUT_IDS.OPEN_SUPER_SCRIPT_ID] : shortcuts[SHORTCUT_IDS.OPEN_SCRIPT_ID]
        entry.getItemClass = () => {
            if (!this.urlParser.isFormComponent()) return 'enabled'; else return 'disabled';
        };

        if (forms.length > 1 && !this.urlParser.isFormComponent()) {
            const superFormsActions = new Array<ContextmenuItem>();
            let subformEntry: ContextmenuItem;
            for (let i = 0; i < forms.length; i++) {
                subformEntry = new ContextmenuItem(
                    forms[i] + '.js',
                    () => {
                        this.hide();
                        this.editorSession.executeAction('openScript', { 'f': forms[i] });
                    }
                );
                subformEntry.getIconStyle = () => {
                    return { 'background-image': 'url(designer/assets/images/js.png)' };
                };
                subformEntry.shortcut = i == 0 ? shortcuts[SHORTCUT_IDS.OPEN_SCRIPT_ID] : undefined;
                superFormsActions.push(subformEntry);
            }
            entry.subMenu = superFormsActions;
            entry.getItemClass = () => {
                return 'dropdown-submenu';
            };
        }
        this.menuItems.push(entry);

        entry = new ContextmenuItem(
            'Delete',
            () => {
                this.hide();
                //46 == delete key
                this.editorSession.keyPressed({ 'keyCode': 46 });
            }
        );
        // entry.getIconStyle = () => {
        // 	return {'background-image':"url(designer/assets/images/delete.gif)"};
        // };
        this.menuItems.push(entry);

        entry = new ContextmenuItem(
            'Open Form Hierarchy',
            () => {
                this.hide();
                this.editorSession.executeAction('openFormHierarchy');
            }
        );
        entry.getIconStyle = () => {
            return { 'background-image': 'url(designer/assets/images/forms.png)' };
        };
        entry.shortcut = shortcuts[SHORTCUT_IDS.OPEN_FORM_HIERARCHY_ID];
        entry.getItemClass = () => {
            if (!this.hasSelection(1)) return 'enabled';
        };
        this.menuItems.push(entry);
    }

    private hasSelection(selectionSize?: number): boolean {
        if (this.selection && this.selection.length > 0 && (selectionSize == undefined || this.selection.length == selectionSize))
            return true;
        return false;
    }

    private isAnchored(anchor: number): boolean {
        if (this.selection && this.selection.length == 1) {
            if (this.selectionAnchor == 0)
                this.selectionAnchor = 1 + 8; // top left
            if ((this.selectionAnchor & anchor) == anchor) {
                return true;
            }
        }
        return false;
    }

    private setAnchoring(anchor: number, opposite: number) {
        if (this.selection && this.selection.length == 1) {
            const obj = {};
            const nodeid = this.selection[0];
            const frameElem = this.doc.querySelector('iframe');
            const node = frameElem.contentWindow.document.querySelector("[svy-id='" + nodeid + "']");
            if (node && node.hasAttribute('svy-anchors')) {
                let beanAnchor = parseInt(node.getAttribute('svy-anchors'));
                if (beanAnchor == 0)
                    beanAnchor = 1 + 8; // top left
                if ((beanAnchor & anchor) == anchor) {
                    // already exists, remove it
                    beanAnchor = beanAnchor - anchor;
                    if ((beanAnchor & opposite) != opposite) beanAnchor += opposite;
                } else {
                    beanAnchor = beanAnchor + anchor;
                }
                obj[nodeid] = { anchors: beanAnchor }
                this.editorSession.sendChanges(obj);
            }
        }
    }

    private setCssAnchoring(top: string, right: string, bottom: string, left: string) {
        if (this.selection && this.selection.length > 0) {
            this.editorSession.setCssAnchoring(this.selection, { top: top, right: right, bottom: bottom, left: left });
        }
    }

    private findComponentDisplayName(arrayOfComponents: Array<{ name: string; displayName: string }>, componentName: string) {
        if (arrayOfComponents && arrayOfComponents.length) {
            for (let j = 0; j < arrayOfComponents.length; j++) {
                if (arrayOfComponents[j].name == componentName) {
                    return arrayOfComponents[j].displayName;
                }
            }
        }
        return null;
    }

    private getDisplayName(componentName: string): string {
        const packages = this.editorSession.getState().packages;
        if (packages && packages.length) {
            const packageAndComponent = componentName.split('.');
            if (componentName == 'component' || packageAndComponent[1] == '*') return 'Component [...]';
            if (componentName == 'template') return 'Template [...]';
            for (let i = 0; i < packages.length; i++) {
                if (packages[i].packageName == packageAndComponent[0]) {
                    let displayName = this.findComponentDisplayName(packages[i].components, packageAndComponent[1]);
                    if (displayName) return displayName;

                    const categories = packages[i].categories;
                    if (categories) {
                        for (const property in categories) {
                            displayName = this.findComponentDisplayName(categories[property], packageAndComponent[1]);
                            if (displayName) return displayName;
                        }
                    }
                }
            }
        }
        return componentName;
    }

    private convertToContentPoint(point: { x?: number; y?: number; left?: number; top?: number }) {
        const glasspane = this.doc.querySelector('.contentframe-overlay');
        const frameRect = glasspane.getBoundingClientRect();

        if (point.x && point.y) {
            point.x = point.x - frameRect.left;
            point.y = point.y - frameRect.top;
        } else if (point.top && point.left) {
            point.left = point.left - frameRect.left;
            point.top = point.top - frameRect.top;
        }
        return point
    }
}

export class ContextmenuItem {

    getIconStyle: () => object;
    shortcut: string;
    getItemClass: () => string;
    subMenu: ContextmenuItem[];

    constructor(
        public text: string,
        public execute: () => void) {
    }
}
