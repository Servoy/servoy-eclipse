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
    snapData: {top: number, left: number, guideX?: number, guideY?: number, snapX?: string, snapY?: string};
  

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

    onMouseUp(event: MouseEvent): void {
        this.snapData = null;
        this.editorContentService.sendMessageToIframe({ id: 'clearSnapCache'});
        this.renderer.setStyle(this.vertical.nativeElement, 'display', 'none');
        this.renderer.setStyle(this.vertical.nativeElement, 'height', '0px');
        this.renderer.setStyle(this.horizontal.nativeElement, 'display', 'none');
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
      if (this.previousPoint && this.previousPoint.x === point.x && this.previousPoint.y === point.y) return;
      this.editorContentService.sendMessageToIframe({ id: 'getSnapTarget', p1: point });
      this.previousPoint = point;*/
    }

    contentMessageReceived(id: string, data: { property: string }) {
        if (id === 'snap') {
            this.snapData = data['properties'];
            if (this.snapData?.snapX) {
                this.renderer.setStyle(this.vertical.nativeElement, 'left', this.snapData.guideX + this.leftAdjust + 'px');
                this.renderer.setStyle(this.vertical.nativeElement, 'top', this.snapData['startX'] + this.topAdjust + 'px');
                this.renderer.setStyle(this.vertical.nativeElement, 'height', this.snapData['lenX'] + 'px');
                this.renderer.setStyle(this.vertical.nativeElement, 'display', 'block');
            }
            else {
                this.renderer.setStyle(this.vertical.nativeElement, 'height', '0px');
                this.renderer.setStyle(this.vertical.nativeElement, 'display', 'none');
            }

            if (this.snapData?.snapY) {
                this.renderer.setStyle(this.horizontal.nativeElement, 'left', this.snapData['startY'] + this.leftAdjust + 'px');
                this.renderer.setStyle(this.horizontal.nativeElement, 'top',  this.snapData.guideY + this.topAdjust + 'px'); 
                this.renderer.setStyle(this.horizontal.nativeElement, 'width', this.snapData['lenY'] + 'px');
                this.renderer.setStyle(this.horizontal.nativeElement, 'display', 'block');
            }
            else {
                this.renderer.setStyle(this.horizontal.nativeElement, 'width', '0px');
                this.renderer.setStyle(this.horizontal.nativeElement, 'display', 'none');
            }
        }
    }
}
