import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ShortcutService} from './window_service/shortcut.service';
import {WindowService} from './window_service/window.service';
import {PopupMenuService} from './window_service/popupmenu.service';
import { ServoyPublicModule } from '@servoy/public';

@NgModule({
   declarations: [],
   imports: [CommonModule, ServoyPublicModule],
   providers: [WindowService, ShortcutService, PopupMenuService],
   entryComponents: []
})
export class WindowServiceModule {}
