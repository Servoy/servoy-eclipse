import { Component, SimpleChanges, Input, ViewChildren, Renderer2, ChangeDetectorRef, Directive, ElementRef, OnInit, QueryList, Output, EventEmitter, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { merge, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { IValuelist } from '../../sablo/spectypes.service';
import { FormattingService } from '../../ngclient/servoy_public';
import { ServoyService } from '../../ngclient/servoy.service';
import { SvyUtilsService } from '../../ngclient/servoy_public';
import { BaseCustomObject } from '../../sablo/spectypes.service';

@Component({
    selector: 'bootstrapextracomponents-navbar',
    templateUrl: './navbar.html'
})
export class ServoyBootstrapExtraNavbar extends ServoyBaseComponent<HTMLDivElement> {

    @Input() brandText: string;
    @Input() styleClass: string;
    @Input() brandTextTabindex: string;
    @Input() brandLogo: string;
    @Input() brandLogoStyleClass: string;
    @Input() brandLogoTabindex: string;
    @Input() visible: boolean;
    @Input() inverse: boolean;
    @Input() fixed: string;
    @Input() markClickedItemActive: boolean;
    @Input() iconCollapseStyleClass: string;
    @Input() collapsing: boolean;
    @Input() collapseOnClick: boolean;

    @Input() menuItems: Array<MenuItem>;
    @Output() menuItemsChange = new EventEmitter();

    @Input() onMenuItemClicked: (e: Event, menuItem: MenuItem) => void;
    @Input() onBrandClicked: (e: Event) => void;

    focusSubjects = new Array<Subject<string>>();
    typeaheadInit = false;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, public formattingService: FormattingService, @Inject(DOCUMENT) private document: Document, private servoyService: ServoyService, private utils: SvyUtilsService) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes.menuItems) {
            this.initTypeaheads(changes.menuItems.currentValue);
        }
    }

    isTrustedHTML(): boolean {
        if (this.servoyApi.trustAsHtml()) {
            return true;
        }
        return false;
    }

    applyBlurOnEnter(e: KeyboardEvent) {
        if (this.formattingService.testKeyPressed(e, 13)) {
            this.doBlur(e);
        }
    }

    applyClickOnEnter(e: KeyboardEvent) {
        if (this.formattingService.testKeyPressed(e, 13)) {
            this.navBarClicked(e);
        }
    }

    doBlur(e: Event) {
        (<HTMLElement>e.target).blur();
    }

    onInputChange(menuItem : MenuItem) {
        menuItem.getStateHolder().getChangedKeys().add("dataProvider");
        menuItem.getStateHolder().notifyChangeListener();
        this.menuItemsChange.emit(this.menuItems);
    }

    resultFormatter = (result: { displayValue: string; realValue: any }) => {
        if (result.displayValue === null) return '';
        //return this.formattingService.format(result.displayValue, null, false);
        return result.displayValue;
    };

    inputFormatter = (result: any) => {
        if (result === null) return '';
        if (result.displayValue !== undefined) result = result.displayValue;
       // return this.formattingService.format(result, null, false);
       return result;
    }

    doSvyApply(event: Event, index: number) {
        const menuItem = this.menuItems[index];
        if (!menuItem) {
            return;
        }
        if (menuItem.valuelist && menuItem.valuelist.length > 0 && menuItem.valuelist[0].displayValue) {
            let hasMatchingDisplayValue = false;
            for (let i = 0; i < menuItem.valuelist.length; i++) {
                if ((<HTMLInputElement>event.target).value === menuItem.valuelist[i].displayValue) {
                    menuItem.dataProvider = menuItem.valuelist[i].realValue;
                    menuItem.getStateHolder().getChangedKeys().add("dataProvider");
                    hasMatchingDisplayValue = true;
                    break;
                }
            }
            if (!hasMatchingDisplayValue) {
                menuItem.dataProvider = null;
                menuItem.getStateHolder().getChangedKeys().add("dataProvider");
                (<HTMLInputElement>event.target).value = null;
            }
            menuItem.getStateHolder().notifyChangeListener();
        }
        this.menuItemsChange.emit(this.menuItems);
        this.navBarClicked(event, index);
    }

    navBarClicked(event: Event, index?: number) {
        let $target = <Element>event.target;
        if ($target.getAttribute('id') == 'navbar-collapse') {
            //click on navbar (background)
            return;
        }
        var li = $target.closest('li');
        if (li && li.classList.contains('disabled')) {
            //disabled entry
            return;
        }
        if (event.type == 'click' && $target.tagName == 'INPUT') {
            //skip simple click in Input
            return;
        }

        // apply the change to the dataprovider at the on enter
        if ($target.tagName == 'INPUT') {
            this.menuItemsChange.emit(this.menuItems);
        }

        /** adjust fixed position of navbar dropdown when right aligned */

        // if the user clicked on the icon contained in the navbar-dropdown 'anchor' set target to the parent (which contains svy-navar-dropdown)
        if ($target.parentElement.classList.contains('svy-navbar-dropdown')) {
            $target = $target.parentElement;
        }

        // if clicked on a dropdown menu
        if ($target.classList.contains('svy-navbar-dropdown')) { // if is a dropdown menu
            let parent = $target.parentElement;
            let nav = $target.closest('.navbar-nav'); // closest navbar anchestor
            let ul = parent.querySelector('ul'); // first child of type ul

            // only if is right aligned
            if (nav && ul && (nav.classList.contains('navbar-left') || nav.classList.contains('navbar-right'))) {

                const ITEM_POSITION = {
                    LEFT: 'left',
                    RIGHT: 'right'
                }

                let alignPosition: string;
                if (nav.classList.contains('navbar-right')) {
                    alignPosition = ITEM_POSITION.RIGHT
                } else if (nav.classList.contains('navbar-left')) {
                    alignPosition = ITEM_POSITION.LEFT;
                }

                let dialog = $target.closest('.svy-dialog')

                // make sure the menu is not collapsed because min-width < 768
                let viewPortWidth = this.document.defaultView.innerWidth;
                //if (viewPortWidth >= 768) {
                if (!this.isCollapseIn()) {
                    let position = dialog ? dialog.getBoundingClientRect() : null;
                    // location relative to viewport
                    var boundingRect = $target[0].getBoundingClientRect();
                    // calculate fixed top/right position from either viewport or dialog
                    var alignLocation = 0;
                    if (alignPosition == ITEM_POSITION.RIGHT) {  // anchor the sub-menu to the right
                        var right: number;
                        if (dialog) {
                            right = position.left + position.width - (boundingRect.left + boundingRect.width);
                        } else {
                            right = viewPortWidth - (boundingRect.left + boundingRect.width);
                        }
                        alignLocation = right;
                    } else { // anchor the sub-menu to the left
                        var left: number;
                        if (dialog) {
                            left = boundingRect.left - position.left;
                        } else {
                            left = boundingRect.left;
                        }
                        alignLocation = left;
                    }

                    // TODO shall i manage if item if navbar is anchored to the bottom !?
                    let top: number;
                    if (dialog) {
                        top = boundingRect.top + boundingRect.height - position.top;
                    } else {
                        top = boundingRect.top + boundingRect.height;
                    }

                    this.renderer.setStyle(ul, 'position', 'fixed');
                    this.renderer.setStyle(ul, alignPosition, alignLocation + 'px');
                    this.renderer.setStyle(ul, 'top', top + 'px');
                } else {        // restore default style for the list dropdown
                    this.renderer.setStyle(ul, 'position', 'static');
                    this.renderer.setStyle(ul, 'right', 'auto');
                    this.renderer.setStyle(ul, 'top', '100%');
                }
            }
        }
        let itemClicked = this.getItem(event);
        this.makeItemActive(itemClicked);
        if (itemClicked && itemClicked.onAction) {
            var jsEvent = this.utils.createJSEvent(event, 'action');
            this.servoyService.executeInlineScript(itemClicked.onAction.formname, itemClicked.onAction.script, [jsEvent, this.createItemArg(itemClicked)]);
        } else if (itemClicked && this.onMenuItemClicked) {
            this.onMenuItemClicked(event, this.createItemArg(itemClicked));
        }
    }

    isCollapseIn(): boolean {
        var el = this.getNativeElement().querySelector('.navbar-collapse.collapse.in');
        if (el) {
            return true;
        } else {
            return false;
        }
    }

    createItemArg(item: MenuItem): MenuItem {
        var itemText = item.text;
        if (item.displayValue) {
            itemText = item.displayValue;
        }
        if (item.displayType == 'INPUT' || item.displayType == 'INPUT_GROUP') {
            itemText = item.dataProvider != null ? item.dataProvider + '' : null;
        }
        return { itemId: item.itemId ? item.itemId : null, text: itemText ? itemText : null, userData: item.userData ? item.userData : null } as MenuItem;
    }

    getItem(event: Event) {
        let $target = <Element>event.target;
        //collapse menu if in mobile view             
        //if ($(window).width() < 768) {   
        if (this.isCollapseIn()) {
            //if collapseOnClick is set don't collapse menu if we are selecting a drop-down
            if (this.collapseOnClick) {
                if (!$target.classList.contains('dropdown')) {

                    // check if is a SPAN direct child of dropdown
                    if ($target && $target.nodeName == "SPAN") {
                        var parentNode = $target.parentElement;
                        if (!parentNode || !parentNode.classList.contains('dropdown')) {
                            this.document.getElementById('#' + this.servoyApi.getMarkupId() + '-toggle-button').click();
                        }
                    } else {
                        this.document.getElementById('#' + this.servoyApi.getMarkupId() + '-toggle-button').click();
                    }
                }
            }
        }
        try {
            let dataMenuItemElement = $target.closest('[data-menu-item-id]');
            if (!dataMenuItemElement) {
                return null;
            }
            let itemId = dataMenuItemElement.getAttribute('data-menu-item-id');
            let itemClicked;
            if (!itemId) {
                return null;
            } else {
                for (let i = 0; i < this.menuItems.length; i++) {
                    let menuItem = this.menuItems[i];
                    if (menuItem.itemId == itemId) {
                        itemClicked = menuItem;
                        break;
                    }
                    if (menuItem.subMenuItems) {
                        //dropdown
                        for (var s = 0; s < menuItem.subMenuItems.length; s++) {
                            if (menuItem.subMenuItems[s].itemId == itemId) {
                                itemClicked = menuItem.subMenuItems[s];
                                break;
                            }
                        }
                        if (itemClicked) {
                            break;
                        }
                    }
                }
                if (itemClicked && (itemClicked.displayType == 'INPUT' || itemClicked.displayType == 'INPUT_GROUP')) {
                    itemClicked.displayValue = (<HTMLInputElement>event.target).value;
                }
                return itemClicked;
            }
        } catch (e) {
            console.log('Error when trying to figure out navbar itemId: ' + e.message);
        }
        return null;
    }

    makeItemActive(item: MenuItem) {
        if (!item || !this.markClickedItemActive) {
            return;
        }
        for (let i = 0; i < this.menuItems.length; i++) {
            const menuItem = this.menuItems[i];
            if (menuItem.itemId == item.itemId) {
                menuItem.isActive = true;
            } else if (menuItem.isActive == true) {
                menuItem.isActive = false;
            }
        }
    }

    requestFocus(itemId: String) {
        let inputEl = this.getNativeElement().querySelector("[data-menu-item-id=" + itemId + "]");
        if (inputEl instanceof HTMLInputElement) inputEl.focus();
    }

    getLocation(itemId: String) {
        if (itemId) {
            let el = this.getNativeElement().querySelector("[data-menu-item-id=" + itemId + "]");
            if (el) {
                let position = el.getBoundingClientRect();
                return { x: position.left, y: position.top };
            }

        }
        return null;
    }

    getSize(itemId: String) {
        if (itemId) {
            let el = this.getNativeElement().querySelector("[data-menu-item-id=" + itemId + "]");
            if (el) {
                let position = el.getBoundingClientRect();
                return { width: position.width, height: position.height };
            }
        }
        return null;
    }

    getFilterValues(index: number) {
        this.initTypeaheads(this.menuItems);
        let item = this.menuItems[index];
        let focus$ = this.focusSubjects[index];
        return (text$: Observable<string>) => {
            const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
            const inputFocus$ = focus$;

            return merge(debouncedText$, inputFocus$).pipe(switchMap(term => (term === '' ? of(item.valuelist)
                : item.valuelist.filterList(term))));
        };

    }

    initTypeaheads(items: Array<MenuItem>) {
        if (!this.typeaheadInit && items) {
            this.typeaheadInit = true;
            for (let i = 0; i < items.length; i++) {
                let menuItem = items[i];
                if (menuItem.valuelist && (menuItem.displayType == 'INPUT' || menuItem.displayType == 'INPUT_GROUP')) {
                    this.focusSubjects[i] = new Subject<string>();
                }
                else {
                    this.focusSubjects[i] = null;
                }
            }
        }
    }

    onFocus(index: number) {
        this.focusSubjects[index].next('');
    }

    clickBrand(event: Event) {
        if (this.onBrandClicked) {
            this.onBrandClicked(event);
        }
    }

    toggleCollapse() {
        var el = this.getNativeElement().querySelector('.navbar-collapse');
        if (el) {
            if (el.classList.contains('show')) {
                this.renderer.removeClass(el, 'show');
            }
            else {
                this.renderer.addClass(el, 'show');
            }
        }
    }

    openMenu(event: MouseEvent) {
        let $target = <Element>event.target;
        $target = $target.closest('.dropdown');
        if ($target) {
            $target = $target.querySelector('.dropdown-menu');
            if ($target) {
                this.renderer.addClass($target, 'show');
                const closeOnClick = (event: MouseEvent) => {
                    this.renderer.removeClass($target, 'show');
                    this.document.removeEventListener('mousedown', closeOnClick);
                }
                this.document.addEventListener('mousedown', closeOnClick);
            }
        }

    }
}
export class MenuItem extends BaseCustomObject {
    public attributes: Array<{ key: string; value: string }>;
    public text: string;
    public itemId: string;
    public tabindex: string;
    public enabled: boolean;
    public styleClass: string;
    public userData: any;
    public onAction: Object;
    public subMenuItems: Array<SubMenuItem>;
    public iconName: string;
    public position: string;
    public displayType: string;
    public dataProvider: any;
    public displayValue: string;
    public inputButtonText: string;
    public inputButtonStyleClass: string;
    public isActive: boolean;
    public tooltip: string;
    public valuelist: IValuelist;
}

class SubMenuItem {
    public text: string;
    public itemId: string;
    public tabindex: string;
    public enabled: boolean;
    public styleClass: string;
    public userData: any;
    public iconName: string;
    public onAction: Object;
    public isDivider: boolean;
}

@Directive({
    selector: '[svyAttributes]'
})
export class SvyAttributes implements OnInit {
    @Input('svyAttributes') attributes: Array<{ key: string; value: string }>;

    constructor(private el: ElementRef, private renderer: Renderer2) {

    }

    ngOnInit(): void {
        if (this.attributes) {
            this.attributes.forEach(attribute => this.renderer.setAttribute(this.el.nativeElement, attribute.key, attribute.value));
        }
    }
}