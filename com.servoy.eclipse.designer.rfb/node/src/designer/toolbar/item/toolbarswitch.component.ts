import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';

@Component({
    selector: 'designer-toolbar-switch',
    templateUrl: './toolbarswitch.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ToolbarSwitchComponent extends ToolbarItemComponent{
}
