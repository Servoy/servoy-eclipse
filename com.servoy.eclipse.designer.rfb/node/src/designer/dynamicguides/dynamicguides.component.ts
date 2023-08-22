import { Component, Inject, OnInit, Renderer2, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { EditorSessionService, IShowHighlightChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'dynamic-guides',
    templateUrl: './dynamicguides.component.html',
    styleUrls: ['./dynamicguides.component.css']
})
export class DynamicGuidesComponent implements OnInit, OnDestroy, IContentMessageListener {

    @ViewChild('horizontalGuide', { static: false }) horizontal: ElementRef<HTMLElement>;
    @ViewChild('verticalGuide', { static: false }) vertical: ElementRef<HTMLElement>;

    previousPoint: {x: number, y: number};
    topAdjust: any;
    leftAdjust: number;
    snapData: {top: number, left: number, right?: number, bottom?: number, snapX?: string, snapY?: string};
  

    constructor(protected readonly editorSession: EditorSessionService, private readonly renderer: Renderer2,
         private urlParser: URLParserService, private editorContentService: EditorContentService) {
       
    }

    ngOnInit(): void {
        this.editorContentService.getContentArea().addEventListener('mousemove', (event: MouseEvent) => this.onMouseMove(event));
        this.editorContentService.getContentArea().addEventListener('mouseup', (event: MouseEvent) => this.onMouseUp(event));
        this.editorContentService.addContentMessageListener(this);
    }

    ngOnDestroy(): void {
        this.editorContentService.removeContentMessageListener(this);
    }

    onMouseUp(event: MouseEvent): any {
        this.snapData = null;
        this.renderer.setStyle(this.vertical.nativeElement, 'height', '0px');
        this.renderer.setStyle(this.horizontal.nativeElement, 'width', '0px');
    }    

    private onMouseMove(event: MouseEvent) {
      /*if (!this.editorSession.getState().dragging) return;
      let point = { x: event.pageX, y: event.pageY };
      if (!this.topAdjust) {
        const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
        this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
        this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
    }
      const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
      point.x = point.x + this.editorContentService.getContentArea().scrollLeft - contentRect?.left - this.leftAdjust;
      point.y = point.y + this.editorContentService.getContentArea().scrollTop - contentRect?.top - this.topAdjust;
      if (this.previousPoint && (Math.abs(this.previousPoint.x - point.x) < 5 || Math.abs(this.previousPoint.y - point.y) < 5)) return;
      this.editorContentService.sendMessageToIframe({ id: 'getSnapTarget', p1: point });
      this.previousPoint = point;*/
    }

    contentMessageReceived(id: string, data: { property: string }) {
        if (id === 'snap') {
            this.snapData = data['properties'];
            if (this.snapData?.snapX) {
                const snapX = this.editorContentService.getContentElement(this.snapData.snapX);
                const content = snapX.getBoundingClientRect();
                this.renderer.setStyle(this.vertical.nativeElement, 'left', ( this.snapData.right ? this.snapData.right : this.snapData.left) + this.leftAdjust + 'px');
                const t = Math.min(this.snapData.top, content.top);
                this.renderer.setStyle(this.vertical.nativeElement, 'top', t + this.topAdjust + 'px');
                const h = this.snapData.bottom? this.snapData.bottom : Math.max(content.bottom, this.previousPoint.y) ;
                this.renderer.setStyle(this.vertical.nativeElement, 'height', h + 'px');
            }
            else {
                this.renderer.setStyle(this.vertical.nativeElement, 'height', '0px');
            }

            if (this.snapData?.snapY) {
                const snapY = this.editorContentService.getContentElement(this.snapData.snapY);
                const content = snapY.getBoundingClientRect();
                const l = Math.min(this.snapData.left, content.left);
                this.renderer.setStyle(this.horizontal.nativeElement, 'left', l + this.leftAdjust + 'px');
                this.renderer.setStyle(this.horizontal.nativeElement, 'top',  this.snapData.top + this.topAdjust + 'px'); //TODO fix for bottom edge
                this.renderer.setStyle(this.horizontal.nativeElement, 'width', '100%'); //TODO get extra info for the width
            }
            else {
                this.renderer.setStyle(this.horizontal.nativeElement, 'width', '0px');
            }
        }
    }
}
