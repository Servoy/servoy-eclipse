import { Component, ElementRef, OnInit, Renderer2, ViewChild, ChangeDetectionStrategy, inject } from '@angular/core';
import { EditorSessionService } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { VariantsPreviewComponent } from './variantspreview/variantspreview.component';
import { PaletteComponent } from './palette/palette.component';
import { ResizerComponent } from './resizer/resizer.component';
import { MouseSelectionComponent } from './mouseselection/mouseselection.component';
import { HighlightComponent } from './highlight/highlight.component';
import { GhostsContainerComponent } from './ghostscontainer/ghostscontainer.component';
import { DragselectionComponent } from './dragselection/dragselection.component';
import { DragselectionResponsiveComponent } from './dragselection-responsive/dragselection-responsive.component';
import { SameSizeIndicatorComponent } from './samesizeindicator/samesizeindicator.component';
import { AnchoringIndicatorComponent } from './anchoringindicator/anchoringindicator.component';
import { DynamicGuidesComponent } from './dynamicguides/dynamicguides.component';
import { EditorContentComponent } from './editorcontent/editorcontent.component';
import { AutoscrollComponent } from './autoscroll/autoscroll.component';
import { StatusBarComponent } from './statusbar/statusbar.component';
import { ContextMenuComponent } from './contextmenu/contextmenu.component';
import { InlineEditComponent } from './inlinedit/inlineedit.component';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ToolbarComponent, VariantsPreviewComponent, PaletteComponent, ResizerComponent, MouseSelectionComponent, HighlightComponent, GhostsContainerComponent, DragselectionComponent, DragselectionResponsiveComponent, SameSizeIndicatorComponent, AnchoringIndicatorComponent, DynamicGuidesComponent, EditorContentComponent, AutoscrollComponent, StatusBarComponent, ContextMenuComponent, InlineEditComponent]
})
export class DesignerComponent implements OnInit {

    @ViewChild('contentArea', { static: false }) contentArea!: ElementRef<HTMLElement>;

    public readonly editorSession = inject(EditorSessionService);
    public urlParser = inject(URLParserService);
    protected readonly renderer = inject(Renderer2);

    ngOnInit() {
        this.editorSession.connect();
        this.editorSession.registerCallback.subscribe(value => {
            if (this.contentArea) this.renderer.listen(this.contentArea.nativeElement, value.event, value.function);
        })

        this.renderer.listen('window', 'mouseup', (event: MouseEvent) => {
            if (event.button > 2) { // special mouse buttons are not allowed
                event.preventDefault();  // Stop the browser from navigating back or forward
                event.stopPropagation(); // Stop further propagation of the event
            }
        });
    }
}
