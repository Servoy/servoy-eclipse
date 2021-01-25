import { NgModule } from "@angular/core";
import {ShortcutService} from './shortcut.service';
import {WindowService} from './window.service';
import {PopupMenuService} from './popupmenu.service';

@NgModule({
   providers: [WindowService, ShortcutService, PopupMenuService ]
})
export class WindowServiceModule {}