import { NgModule } from "@angular/core";
import { CommonModule } from '@angular/common';
import {ShortcutService} from './shortcut.service';
import {WindowService} from './window.service';
import {PopupMenuService} from './popupmenu.service';
import { SabloModule } from '../sablo/sablo.module';

@NgModule({
   declarations: [],
   imports: [CommonModule, SabloModule],
   providers: [WindowService, ShortcutService, PopupMenuService ]
})
export class WindowServiceModule {}