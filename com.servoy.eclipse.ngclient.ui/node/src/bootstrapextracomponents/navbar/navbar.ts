import { Component, SimpleChanges, Input, Renderer2, ChangeDetectorRef, Directive, ElementRef, OnInit, Output, EventEmitter, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { merge, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { ServoyBaseComponent, FormattingService, ServoyPublicService, BaseCustomObject, IValuelist } from '@servoy/public';

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

    @Input() onMenuItemClicked: (e: Event, menuItem: BaseMenuItem) => void;
    @Input() onBrandClicked: (e: Event) => void;

    focusSubjects = new Array<Subject<string>>();
    typeaheadInit = false;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, public formattingService: FormattingService,
        @Inject(DOCUMENT) private document: Document, private servoyService: ServoyPublicService) {
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
        (e.target as HTMLElement).blur();
    }

    onInputChange(menuItem: MenuItem, index: number) {
        this.servoyApi.apply('menuItems[' + index + '].dataProvider', menuItem.dataProvider);
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
    };

    doSvyApply(event: Event, index: number) {
        const menuItem = this.menuItems[index];
        if (!menuItem) {
            return;
        }
        if (menuItem.valuelist && menuItem.valuelist.length > 0 && menuItem.valuelist[0].displayValue) {
            let hasMatchingDisplayValue = false;
            for (const vlValue of menuItem.valuelist) {
                if ((event.target as HTMLInputElement).value === vlValue.displayValue) {
                    menuItem.dataProvider = vlValue.realValue;
                    menuItem.getStateHolder().getChangedKeys().add('dataProvider');
                    hasMatchingDisplayValue = true;
                    break;
                }
            }
            if (!hasMatchingDisplayValue) {
                menuItem.dataProvider = null;
                menuItem.getStateHolder().getChangedKeys().add('dataProvider');
                (event.target as HTMLInputElement).value = null;
            }
            menuItem.getStateHolder().notifyChangeListener();
        }
        else if (menuItem.valuelist && !menuItem.valuelist.hasRealValues())
        {
             menuItem.dataProvider = (event.target as HTMLInputElement).value;
             menuItem.getStateHolder().getChangedKeys().add('dataProvider');
        }
        this.menuItemsChange.emit(this.menuItems);
        this.navBarClicked(event);
    }
    
    valueChanged(value: { displayValue: string; realValue: any }, index: number) {
        const menuItem = this.menuItems[index];
        if (!menuItem) {
            return;
        }
        if (value && value.realValue !== undefined) menuItem.dataProvider = value.realValue;
        else if (value) menuItem.dataProvider = value;
        else menuItem.dataProvider = null;
        menuItem.getStateHolder().getChangedKeys().add('dataProvider');
        this.menuItemsChange.emit(this.menuItems);
    }
    
    navBarClicked(event: Event) {
        let $target = event.target as Element;
        if ($target.getAttribute('id') === 'navbar-collapse') {
            //click on navbar (background)
            return;
        }
        const li = $target.closest('li');
        if (li && li.classList.contains('disabled')) {
            //disabled entry
            return;
        }
        if (event.type === 'click' && $target.tagName === 'INPUT') {
            //skip simple click in Input
            return;
        }

        // apply the change to the dataprovider at the on enter
        if ($target.tagName === 'INPUT') {
            this.menuItemsChange.emit(this.menuItems);
        }

        /** adjust fixed position of navbar dropdown when right aligned */

        // if the user clicked on the icon contained in the navbar-dropdown 'anchor' set target to the parent (which contains svy-navar-dropdown)
        if ($target.parentElement.classList.contains('svy-navbar-dropdown')) {
            $target = $target.parentElement;
        }

        // if clicked on a dropdown menu
        if ($target.classList.contains('svy-navbar-dropdown')) { // if is a dropdown menu
            const parent = $target.parentElement;
            const nav = $target.closest('.navbar-nav'); // closest navbar anchestor
            const ul = parent.querySelector('ul'); // first child of type ul

            // only if is right aligned
            if (nav && ul && (nav.classList.contains('navbar-left') || nav.classList.contains('navbar-right'))) {

                const ITEM_POSITION = {
                    LEFT: 'left',
                    RIGHT: 'right'
                };

                let alignPosition: string;
                if (nav.classList.contains('navbar-right')) {
                    alignPosition = ITEM_POSITION.RIGHT;
                } else if (nav.classList.contains('navbar-left')) {
                    alignPosition = ITEM_POSITION.LEFT;
                }

                const dialog = $target.closest('.svy-dialog');

                // make sure the menu is not collapsed because min-width < 768
                const viewPortWidth = this.document.defaultView.innerWidth;
                //if (viewPortWidth >= 768) {
                if (!this.isCollapseIn()) {
                    const position = dialog ? dialog.getBoundingClientRect() : null;
                    // location relative to viewport
                    const boundingRect = $target[0].getBoundingClientRect();
                    // calculate fixed top/right position from either viewport or dialog
                    let alignLocation = 0;
                    if (alignPosition === ITEM_POSITION.RIGHT) {  // anchor the sub-menu to the right
                        let right: number;
                        if (dialog) {
                            right = position.left + position.width - (boundingRect.left + boundingRect.width);
                        } else {
                            right = viewPortWidth - (boundingRect.left + boundingRect.width);
                        }
                        alignLocation = right;
                    } else { // anchor the sub-menu to the left
                        let left: number;
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
        const itemClicked = this.getItem(event);
        this.makeItemActive(itemClicked);
        if (itemClicked && itemClicked.onAction) {
            const jsEvent = this.servoyService.createJSEvent(event, 'action');
            this.servoyService.executeInlineScript(itemClicked.onAction.formname, itemClicked.onAction.script, [jsEvent, this.createItemArg(itemClicked)]);
        } else if (itemClicked && this.onMenuItemClicked) {
            this.onMenuItemClicked(event, this.createItemArg(itemClicked));
        }
    }

    isCollapseIn(): boolean {
        const el = this.getNativeElement().querySelector('.navbar-collapse.collapse.in');
        if (el) {
            return true;
        } else {
            return false;
        }
    }

    createItemArg(item: BaseMenuItem): BaseMenuItem {
        let itemText = item.text;
        if (item instanceof MenuItem) {
            if (item.displayValue) {
                itemText = item.displayValue;
            }
            if (item.displayType === 'INPUT' || item.displayType === 'INPUT_GROUP') {
                itemText = item.dataProvider != null ? item.dataProvider + '' : null;
            }
        }
        return { itemId: item.itemId ? item.itemId : null, text: itemText ? itemText : null, userData: item.userData ? item.userData : null } as MenuItem;
    }

    getItem(event: Event) {
        const $target = event.target as Element;
        //collapse menu if in mobile view
        //if ($(window).width() < 768) {
        if (this.isCollapseIn()) {
            //if collapseOnClick is set don't collapse menu if we are selecting a drop-down
            if (this.collapseOnClick) {
                if (!$target.classList.contains('dropdown')) {

                    // check if is a SPAN direct child of dropdown
                    if ($target && $target.nodeName === 'SPAN') {
                        const parentNode = $target.parentElement;
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
            const dataMenuItemElement = $target.closest('[data-menu-item-id]');
            if (!dataMenuItemElement) {
                return null;
            }
            const itemId = dataMenuItemElement.getAttribute('data-menu-item-id');
            let itemClicked: BaseMenuItem;
            if (!itemId) {
                return null;
            } else {
                for (const i of Object.keys(this.menuItems)) {
                    const menuItem = this.menuItems[i];
                    if (menuItem.itemId === itemId) {
                        itemClicked = menuItem;
                        break;
                    }
                    if (menuItem.subMenuItems) {
                        //dropdown
                        for (const s of Object.keys(menuItem.subMenuItems)) {
                            if (menuItem.subMenuItems[s].itemId === itemId) {
                                itemClicked = menuItem.subMenuItems[s];
                                break;
                            }
                        }
                        if (itemClicked) {
                            break;
                        }
                    }
                }
                if (itemClicked instanceof MenuItem) {
                    if (itemClicked && (itemClicked.displayType === 'INPUT' || itemClicked.displayType === 'INPUT_GROUP')) {
                        itemClicked.displayValue = (event.target as HTMLInputElement).value;
                    }
                }
                return itemClicked;
            }
        } catch (e) {
            console.log('Error when trying to figure out navbar itemId: ' + e.message);
        }
        return null;
    }

    makeItemActive(item: BaseMenuItem) {
        if (!item || !this.markClickedItemActive) {
            return;
        }
        for (const i of Object.keys(this.menuItems)) {
            const menuItem = this.menuItems[i];
            if (menuItem.itemId === item.itemId) {
                menuItem.isActive = true;
            } else if (menuItem.isActive === true) {
                menuItem.isActive = false;
            }
        }
    }

    requestFocus(itemId: string) {
        const inputEl = this.getNativeElement().querySelector('[data-menu-item-id=' + itemId + ']');
        if (inputEl instanceof HTMLInputElement) inputEl.focus();
    }

    getLocation(itemId: string) {
        if (itemId) {
            const el = this.getNativeElement().querySelector('[data-menu-item-id=' + itemId + ']');
            if (el) {
                const position = el.getBoundingClientRect();
                return { x: position.left, y: position.top };
            }

        }
        return null;
    }

    getSize(itemId: string) {
        if (itemId) {
            const el = this.getNativeElement().querySelector('[data-menu-item-id=' + itemId + ']');
            if (el) {
                const position = el.getBoundingClientRect();
                return { width: position.width, height: position.height };
            }
        }
        return null;
    }

    getFilterValues(index: number) {
        this.initTypeaheads(this.menuItems);
        const item = this.menuItems[index];
        const focus$ = this.focusSubjects[index];
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
                const menuItem = items[i];
                if (menuItem.valuelist && (menuItem.displayType === 'INPUT' || menuItem.displayType === 'INPUT_GROUP')) {
                    this.focusSubjects[i] = new Subject<string>();
                } else {
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
        const el = this.getNativeElement().querySelector('.navbar-collapse');
        if (el) {
            if (el.classList.contains('show')) {
                this.renderer.removeClass(el, 'show');
            } else {
                this.renderer.addClass(el, 'show');
            }
        }
    }

    openMenu(event: MouseEvent) {
        let $target = event.target as Element;;
        $target = $target.closest('.dropdown');
        if ($target) {
            $target = $target.querySelector('.dropdown-menu');
            if ($target) {
                this.renderer.addClass($target, 'show');
                const closeOnClick = () => {
                    this.renderer.removeClass($target, 'show');
                    this.document.removeEventListener('mousedown', closeOnClick);
                };
                this.document.addEventListener('mousedown', closeOnClick);
            }
        }

    }
}
class BaseMenuItem extends BaseCustomObject {
    public text: string;
    public itemId: string;
    public tabindex: string;
    public enabled: boolean;
    public styleClass: string;
    public userData: any;
    public iconName: string;
    public onAction: { formname: string; script: string };
}

export class MenuItem extends BaseMenuItem {
    public attributes: Array<{ key: string; value: string }>;
    public subMenuItems: Array<SubMenuItem>;
    public position: string;
    public displayType: string;
    public dataProvider: any;
    public displayValue: string;
    public inputButtonText: string;
    public inputButtonStyleClass: string;
    public isActive: boolean;
    public tooltip: string;
    public valuelist: IValuelist;

    getWatchedProperties() {
        return [];
    }
}

class SubMenuItem extends BaseMenuItem {
    public isDivider: boolean;
}

@Directive({
    selector: '[svyAttributes]'
})
// eslint-disable-next-line @angular-eslint/directive-class-suffix
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
