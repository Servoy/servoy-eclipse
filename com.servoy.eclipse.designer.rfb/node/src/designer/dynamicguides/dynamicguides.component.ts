import { Component, AfterViewInit, Renderer2, ElementRef, ChangeDetectionStrategy, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EditorSessionService } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { EditorContentService } from '../services/editorcontent.service';
import { DynamicGuidesService, Guide, SnapData } from '../services/dynamicguides.service';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dynamic-guides',
    templateUrl: './dynamicguides.component.html',
    styleUrls: ['./dynamicguides.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DynamicGuidesComponent implements AfterViewInit {

  guides = input<Guide[]>([]);

  topAdjust: any;
  leftAdjust!: number;
  snapData!: { top: number, left: number, guideX?: number, guideY?: number, guides?: Guide[] } | null;

  private el = inject(ElementRef);
  protected readonly editorSession = inject(EditorSessionService);
  private readonly renderer = inject(Renderer2);
  private urlParser = inject(URLParserService);
  private editorContentService = inject(EditorContentService);
  private guidesService = inject(DynamicGuidesService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
      this.editorContentService.executeOnlyAfterInit(() => {
        this.editorSession.getSnapThreshold().then((thresholds: any) => {
            if (thresholds.alignment > 0 || thresholds.distance > 0) {
                const contentArea = this.editorContentService.getContentArea();
                contentArea.addEventListener('mouseup', () => this.onMouseUp());
            }
        });
    });
  }

  ngAfterViewInit(): void {
    this.guidesService.snapDataListener.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value: SnapData | null) => {
      if (value) this.setGuides(value);
    })
  }

  private renderGuides() {
    if (!this.topAdjust) {
      const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
      this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
      this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
    }
    this.snapData!.guides!.forEach(guide => {
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

  onMouseUp(): void {
    this.snapData = null;
    this.clearGuides();
  }

  private setGuides(data: SnapData) {
	this.clearGuides();
    if (!this.editorSession.dragging() && !this.editorSession.resizing()) {
      return;
    }
    this.snapData = data;
    if (this.snapData?.guides) {
      this.renderGuides();
    }
  }
}
