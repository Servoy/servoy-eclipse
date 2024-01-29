import { Component, Inject, OnInit, Renderer2, OnDestroy, ViewChild, ElementRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { EditorSessionService, IShowDynamicGuidesChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'dynamic-guides',
    templateUrl: './dynamicguides.component.html',
    styleUrls: ['./dynamicguides.component.css']
})
export class DynamicGuidesComponent implements OnInit, OnDestroy, IContentMessageListener, IShowDynamicGuidesChangedListener {

    @Input() guides: Guide[] = [];

    previousPoint: {x: number, y: number};
    topAdjust: any;
    leftAdjust: number;
    snapData: {top: number, left: number, guideX?: number, guideY?: number, guides?:  Guide[] };  
    timeout: ReturnType<typeof setTimeout>;
    guidesEnabled: boolean;

    constructor(private el: ElementRef, protected readonly editorSession: EditorSessionService, private readonly renderer: Renderer2,
         private urlParser: URLParserService, private editorContentService: EditorContentService) {
       this.guidesEnabled = false;
       this.editorSession.addDynamicGuidesChangedListener(this);
    }

    showDynamicGuidesChanged(showGuides: boolean): void {
      this.guidesEnabled = showGuides;
    }

    ngOnInit(): void {
        this.editorSession.getSnapThreshold().then((thresholds: {alignment: number, distance: number}) => {
            this.editorContentService.executeOnlyAfterInit(() => {
                this.editorContentService.sendMessageToIframe({ id: 'snapThresholds', value: thresholds });
                if (thresholds.alignment > 0 || thresholds.distance > 0) {
                    const contentArea = this.editorContentService.getContentArea();
                    contentArea.addEventListener('mousemove', (event: MouseEvent) => this.onMouseMove(event));
                    contentArea.addEventListener('mouseup', (event: MouseEvent) => this.onMouseUp(event));
                }
            });
        });
        this.editorContentService.addContentMessageListener(this);
    }
    
      private renderGuides() {
        this.snapData.guides.forEach(guide => {
          const guideElement = this.renderer.createElement('div');
          this.renderer.setStyle(guideElement, 'position', 'absolute');
          this.renderer.setStyle(guideElement, 'left', `${guide.x + this.leftAdjust}px`);
          this.renderer.setStyle(guideElement, 'top', `${guide.y + this.topAdjust}px`);
          this.renderer.setStyle(guideElement, 'width', `${guide.width}px`);
          this.renderer.setStyle(guideElement, 'height', `${guide.height}px`);
          this.renderer.addClass(guideElement, guide.styleClass);
    
          this.renderer.appendChild(this.el.nativeElement, guideElement);
        });
      }
    
      private clearGuides() {
        const childNodes = this.el.nativeElement.childNodes;
        for (let i = childNodes.length - 1; i >= 0; i--) {
          this.renderer.removeChild(this.el.nativeElement, childNodes[i]);
        }
      }

    ngOnDestroy(): void {
        this.editorContentService.removeContentMessageListener(this);
    }

    onMouseUp(event: MouseEvent): void {
        this.snapData = null;
        this.editorContentService.sendMessageToIframe({ id: 'clearSnapCache'});
        this.clearGuides();
    }    

    private onMouseMove(event: MouseEvent) {
      if (!this.guidesEnabled || !this.editorSession.getState().dragging && !this.editorSession.getState().resizing) return;
      let point = { x: event.pageX, y: event.pageY };
      if (!this.topAdjust) {
        const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
        this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
        this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
      }
      const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
      point.x = point.x + this.editorContentService.getContentArea().scrollLeft - contentRect?.left - this.leftAdjust;
      point.y = point.y + this.editorContentService.getContentArea().scrollTop - contentRect?.top - this.topAdjust;
      if (this.previousPoint && this.previousPoint.x === point.x && this.previousPoint.y === point.y) return;

      this.editorContentService.sendMessageToIframe({
        id: 'getSnapTarget',
        p1: point,
        resizing: this.editorSession.getState().resizing ? this.editorContentService.getGlassPane().style.cursor.split('-')[0] : null
      });
      this.previousPoint = point;
    }

    contentMessageReceived(id: string, data: { property: string }) {
      if (id === 'snap') {
        this.setGuides(data);
      }
    }

    private setGuides(data: { property: string; }) {
        this.snapData = data['properties'];
        if (this.snapData?.guides) {
            this.clearGuides();
            this.renderGuides();
        }
    }
}

export class Guide {
    constructor(
      public x: number,
      public y: number,
      public width: number,
      public height: number,
      public styleClass: string
    ) {}
  }
