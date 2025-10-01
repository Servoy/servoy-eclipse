import { Component } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';

@Component({
    selector: 'designer-toolbar-button',
    templateUrl: './toolbarbutton.component.html',
    standalone: false
})
export class ToolbarButtonComponent extends ToolbarItemComponent{
}