import { AutoscrollComponent } from './autoscroll/autoscroll.component';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { DesignerComponent } from './designer.component';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { StatusBarComponent } from './statusbar/statusbar.component';
import { SameSizeIndicatorComponent } from './samesizeindicator/samesizeindicator.component';
import { AnchoringIndicatorComponent } from './anchoringindicator/anchoringindicator.component';
import { PaletteComponent, SearchTextPipe, SearchTextDeepPipe } from './palette/palette.component';
import { ResizerComponent } from './resizer/resizer.component';
import { ResizeEditorWidthComponent } from './resizeeditorwidth/resizeeditorwidth.component';
import { ResizeEditorHeightComponent } from './resizeeditorheight/resizeeditorheight.component';
import { ContextMenuComponent } from './contextmenu/contextmenu.component';
import { MouseSelectionComponent, PositionMenuDirective } from './mouseselection/mouseselection.component';
import { HighlightComponent } from './highlight/highlight.component';
import { GhostsContainerComponent } from './ghostscontainer/ghostscontainer.component';
import { EditorContentComponent } from './editorcontent/editorcontent.component';
import {EditorSessionService} from './services/editorsession.service';
import {EditorContentService} from './services/editorcontent.service';
import {URLParserService} from './services/urlparser.service';
import { WindowRefService, ServoyPublicModule } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';
import {DragDropModule} from '@angular/cdk/drag-drop';
import { ToolbarButtonComponent } from './toolbar/item/toolbarbutton.component';
import { ToolbarSpinnerComponent } from './toolbar/item/toolbarspinner.component';
import { ToolbarSwitchComponent } from './toolbar/item/toolbarswitch.component';
import { DesignSizeService } from './services/designsize.service';
import { ResizeKnobDirective } from './directives/resizeknob.directive';
import {DesignerUtilsService} from './services/designerutils.service';
import { DragselectionComponent } from './dragselection/dragselection.component';
import { DragselectionResponsiveComponent } from './dragselection-responsive/dragselection-responsive.component';
import { InlineEditComponent } from './inlinedit/inlineedit.component';
import { KeyboardLayoutDirective } from './directives/keyboardlayout.directive';
import { VariantsContentComponent } from './variantscontent/variantscontent.component';
import { VariantsPreviewComponent } from './variantspreview/variantspreview.component';
import { DynamicGuidesComponent } from './dynamicguides/dynamicguides.component';
import { DynamicGuidesService } from './services/dynamicguides.service';

@NgModule({ declarations: [
        DesignerComponent,
        ToolbarComponent,
        ToolbarButtonComponent,
        ToolbarSpinnerComponent,
        ToolbarSwitchComponent,
        StatusBarComponent,
        SameSizeIndicatorComponent,
        AnchoringIndicatorComponent,
        PaletteComponent,
        ResizerComponent,
        ResizeEditorWidthComponent,
        ResizeEditorHeightComponent,
        ContextMenuComponent,
        MouseSelectionComponent,
        HighlightComponent,
        GhostsContainerComponent,
        EditorContentComponent,
        SearchTextPipe,
        SearchTextDeepPipe,
        ResizeKnobDirective,
        KeyboardLayoutDirective,
        PositionMenuDirective,
        DragselectionComponent,
        DragselectionResponsiveComponent,
        InlineEditComponent,
        VariantsContentComponent,
        VariantsPreviewComponent,
        AutoscrollComponent,
        DynamicGuidesComponent
    ],
    bootstrap: [DesignerComponent], imports: [BrowserModule,
        ServoyPublicModule,
        FormsModule,
        CommonModule,
        NgbModule,
        DragDropModule], providers: [EditorSessionService, URLParserService, WindowRefService, DesignSizeService, DesignerUtilsService, EditorContentService, DynamicGuidesService, provideHttpClient(withInterceptorsFromDi())] })
export class DesignerModule { }
