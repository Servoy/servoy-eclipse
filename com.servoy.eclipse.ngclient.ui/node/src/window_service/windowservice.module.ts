import { NgModule } from "@angular/core";
import {ShortcutService} from './shortcut.service';
import {WindowService} from './window.service';

@NgModule({
   providers: [WindowService, ShortcutService ]
})
export class WindowServiceModule {}