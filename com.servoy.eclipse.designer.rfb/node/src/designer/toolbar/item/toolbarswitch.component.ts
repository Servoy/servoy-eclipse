import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'designer-toolbar-switch',
    templateUrl: './toolbarswitch.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule]
})
export class ToolbarSwitchComponent extends ToolbarItemComponent{
}
