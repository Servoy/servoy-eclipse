import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ShortcutService, Shortcut} from './window_service/shortcut.service';
import {WindowPluginService, PopupMenuShowCommand} from './window_service/window.service';
import {PopupMenuService, MenuItem, Popup} from './window_service/popupmenu.service';
import { ServoyPublicModule, PopupForm, SpecTypesService } from '@servoy/public';

@NgModule({
   declarations: [],
   imports: [CommonModule, ServoyPublicModule],
   providers: [WindowPluginService, ShortcutService, PopupMenuService],
   entryComponents: []
})
export class WindowServiceModule {
     constructor( specTypesService: SpecTypesService ) {
        specTypesService.registerType('window.popupMenuShowCommand', PopupMenuShowCommand);
        specTypesService.registerType('window.popup', Popup);
        specTypesService.registerType('window.shortcut', Shortcut);
        specTypesService.registerType('window.menuitem', MenuItem);
        specTypesService.registerType('window.popupform', PopupForm);
    }
}
