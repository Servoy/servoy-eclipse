import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';
import { NgClass, NgStyle } from '@angular/common';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';

@Component({
    selector: 'designer-toolbar-button',
    templateUrl: './toolbarbutton.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [NgClass, NgbDropdown, NgbDropdownToggle, NgStyle, NgbDropdownMenu, NgbDropdownItem]
})
export class ToolbarButtonComponent extends ToolbarItemComponent{
}
