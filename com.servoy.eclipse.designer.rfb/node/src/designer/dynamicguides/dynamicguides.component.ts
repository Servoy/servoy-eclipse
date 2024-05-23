import { Component, AfterViewInit, Renderer2, OnDestroy, ElementRef, Input } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { EditorContentService } from '../services/editorcontent.service';
import { DynamicGuidesService, Guide, SnapData } from '../services/dynamicguides.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'dynamic-guides',
  templateUrl: './dynamicguides.component.html',
  styleUrls: ['./dynamicguides.component.css']
})
export class DynamicGuidesComponent implements AfterViewInit, OnDestroy {

  @Input() guides: Guide[] = [];

  topAdjust: any;
  leftAdjust: number;
  snapData: { top: number, left: number, guideX?: number, guideY?: number, guides?: Guide[] };
  subscription: Subscription;

  constructor(private el: ElementRef, protected readonly editorSession: EditorSessionService, private readonly renderer: Renderer2,
    private urlParser: URLParserService, private editorContentService: EditorContentService, private guidesService: DynamicGuidesService) {
      this.editorContentService.executeOnlyAfterInit(() => {
        this.editorSession.getSnapThreshold().then((thresholds: { alignment: number, distance: number }) => {
            if (thresholds.alignment > 0 || thresholds.distance > 0) {
                const contentArea = this.editorContentService.getContentArea();
                contentArea.addEventListener('mouseup', () => this.onMouseUp());
            }
        });
    });
  }

  ngAfterViewInit(): void {
    this.subscription = this.guidesService.snapDataListener.subscribe((value: SnapData) => {
      this.setGuides(value);
    })
  }

  private renderGuides() {
    if (!this.topAdjust) {
      const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
      this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
      this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
    }
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
    this.subscription.unsubscribe();
  }

  onMouseUp(): void {
    this.snapData = null;
    this.clearGuides();
  }

  private setGuides(data: SnapData) {
	this.clearGuides();
    if (!this.editorSession.getState().dragging && !this.editorSession.getState().resizing) {
      return;
    }
    this.snapData = data;
    if (this.snapData?.guides) {
      this.renderGuides();
    }
  }
}