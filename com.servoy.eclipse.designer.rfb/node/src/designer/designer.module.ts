import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { DesignerComponent } from './designer.component';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { StatusBarComponent } from './statusbar/statusbar.component';
import { PaletteComponent } from './palette/palette.component';
import { ResizerComponent } from './resizer/resizer.component';
import { ContextMenuComponent } from './contextmenu/contextmenu.component';
import { MouseSelectionComponent } from './mouseselection/mouseselection.component';
import { HighlightComponent } from './highlight/highlight.component';
import { GhostsContainerComponent } from './ghostscontainer/ghostscontainer.component';
import { EditorContentComponent } from './editorcontent/editorcontent.component';
import {EditorSessionService} from './services/editorsession.service';
import { SabloModule } from '@servoy/sablo';
import { WindowRefService } from '@servoy/public';

@NgModule({
  declarations: [
    DesignerComponent,
    ToolbarComponent,
    StatusBarComponent,
    PaletteComponent,
    ResizerComponent,
    ContextMenuComponent,
    MouseSelectionComponent,
    HighlightComponent,
    GhostsContainerComponent,
    EditorContentComponent
  ],
  imports: [
    BrowserModule,
    SabloModule
  ],
  providers: [EditorSessionService, WindowRefService],
  bootstrap: [DesignerComponent]
})
export class DesignerModule { }
