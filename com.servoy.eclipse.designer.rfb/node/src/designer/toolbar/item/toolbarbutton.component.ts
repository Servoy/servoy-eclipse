import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';

@Component({
    selector: 'designer-toolbar-button',
    templateUrl: './toolbarbutton.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ToolbarButtonComponent extends ToolbarItemComponent{
}
