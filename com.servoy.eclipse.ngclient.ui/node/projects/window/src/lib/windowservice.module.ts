import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ShortcutService} from './window_service/shortcut.service';
import {WindowPluginService} from './window_service/window.service';
import {PopupMenuService} from './window_service/popupmenu.service';

@NgModule({
    declarations: [],
    imports: [CommonModule],
    providers: [WindowPluginService, ShortcutService, PopupMenuService]
})
export class WindowServiceModule {
}
