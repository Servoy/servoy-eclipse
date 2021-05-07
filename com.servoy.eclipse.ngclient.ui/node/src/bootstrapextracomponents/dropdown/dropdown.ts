import { Component, SimpleChanges, Input, Renderer2, ChangeDetectorRef } from '@angular/core';
import { BaseCustomObject, ServoyBaseComponent, ServoyPublicService } from '@servoy/public';

@Component({
    selector: 'bootstrapextracomponents-dropdown',
    templateUrl: './dropdown.html'
})
export class ServoyBootstrapExtraDropdown extends ServoyBaseComponent<HTMLDivElement> {

    @Input() styleClass: string;
    @Input() buttonStyleClass: string;
    @Input() imageStyleClass: string;
    @Input() isButton: boolean;
    @Input() isSplitButton: string;
    @Input() visible: boolean;
    @Input() text: string;
    @Input() enabled: boolean;
    @Input() toolTipText: string;

    @Input() menuItems: Array<MenuItem>;

    @Input() onMenuItemSelected: (e: Event, menuItem: BaseMenuItem) => void;
    @Input() onAction: (e: Event) => void;


    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private servoyService: ServoyPublicService) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
    }

    buttonClicked(e: Event) {
        if (this.onAction) {
            this.onAction(e);
        }
    }

    menuClicked(e: Event, menuItem: MenuItem) {
        if (menuItem.onAction) {
            const jsEvent = this.servoyService.createJSEvent(e, 'action');
            this.servoyService.executeInlineScript(menuItem.onAction.formname, menuItem.onAction.script, [jsEvent, this.createItemArg(menuItem)]);
        } else if (this.onMenuItemSelected) {
             this.onMenuItemSelected(event, this.createItemArg(menuItem));
        }
    }

    createItemArg(menuItem: MenuItem): BaseMenuItem {
        return {itemId : menuItem.itemId , text: menuItem.text, userData : menuItem.userData} as BaseMenuItem;
    }
}
class BaseMenuItem extends BaseCustomObject {
    public text: string;
    public itemId: string;
    public userData: any;
}

export class MenuItem extends BaseMenuItem {
    public isDivider: boolean;
    public enabled: boolean;
    public iconName: string;
    public onAction: { formname: string; script: string };
}

