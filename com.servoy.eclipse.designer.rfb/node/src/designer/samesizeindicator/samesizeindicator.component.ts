import { Component, OnDestroy, ChangeDetectionStrategy, inject, effect } from '@angular/core';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'designer-samesize-indicator',
    templateUrl: './samesizeindicator.component.html',
    styleUrls: ['./samesizeindicator.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SameSizeIndicatorComponent implements OnDestroy, ISelectionChangedListener, IContentMessageListener  {
    SAME_WIDTH_IMAGE = 'designer/assets/images/samewidthindicator.png';
    SAME_HEIGHT_IMAGE = 'designer/assets/images/sameheightindicator.png';

    indicators!: SameSizeIndicator[];
    
    protected readonly editorSession = inject(EditorSessionService);
    private editorContentService = inject(EditorContentService);

    constructor() {
        this.editorSession.addSelectionChangedListener(this);
        this.editorContentService.addContentMessageListener(this);
        effect(() => {
            const sameSizeOn = this.editorSession.sameSizeIndicator();
            if (!sameSizeOn) this.indicators = [];
        });
        effect(() => {
            const dragging = this.editorSession.dragging();
            if (dragging) {
                this.indicators = null!;
            } else {
                this.selectionChanged(this.editorSession.getSelection());
            }
        });
    }

    ngOnDestroy(): void {
        this.editorContentService.removeContentMessageListener(this);
    }
    
    contentMessageReceived(id: string) {
        if (id === 'redrawDecorators') {
            this.selectionChanged(this.editorSession.getSelection());
        }
    }

    selectionChanged(selection: string[]): void {
        const newindicators: SameSizeIndicator[] = [];

        if (this.editorSession.sameSizeIndicator() && selection && selection.length == 1) {
            const nodeid = selection[0];
            this.editorContentService.executeOnlyAfterInit(() => {
                const element = this.editorContentService.getContentElement(nodeid);
                if (!element || element.parentElement!.closest('.svy-responsivecontainer')) return;
                let addedSameWidth = false;
                let addedSameHeight = false;

                const elementRect = element.getBoundingClientRect();

                const elements = this.removeHiddenElements(this.editorContentService.getAllContentElements());
                Array.from(elements).forEach(node => {
                    if (element != node && node.parentElement!.closest('.svy-responsivecontainer') == null && !element.classList.contains('svy-formcomponent')) {
                        const position = node.getBoundingClientRect();
                        if (position.width >= 5 && position.width == elementRect.width) {
                            this.addSameSizeIndicator(newindicators, position, true);
                            addedSameWidth = true;
                        }
                        if (position.height >= 5 && position.height == elementRect.height) {
                            this.addSameSizeIndicator(newindicators, position, false);
                            addedSameHeight = true;
                        }
                    }
                });
                if (addedSameWidth) {
                    this.addSameSizeIndicator(newindicators, elementRect, true);
                }
                if (addedSameHeight) {
                    this.addSameSizeIndicator(newindicators, elementRect, false);
                }
            });
        }
        this.indicators = newindicators;
    }

    private removeHiddenElements(elements: HTMLElement[]) {
        const filteredElements = elements.filter((element) => {
            let wrapper = element.parentElement;
            
            while (wrapper && !wrapper.classList.contains('svy-wrapper')) {
              wrapper = wrapper.parentElement;
            }
            
            if (wrapper && window.getComputedStyle(wrapper).getPropertyValue('visibility') === 'hidden') {
              return false;
            }
      
          return true;
        });
        return filteredElements;
    }

    private addSameSizeIndicator(newindicators: SameSizeIndicator[], elementRect: DOMRect, horizontal: boolean) {
        newindicators.push(new SameSizeIndicator(
            horizontal ? this.SAME_WIDTH_IMAGE : this.SAME_HEIGHT_IMAGE,
            this.editorContentService.getGlasspaneTopDistance() + elementRect.top + 1,
            this.editorContentService.getGlasspaneLeftDistance() + (horizontal ? elementRect.left + elementRect.width - 14 : elementRect.left + 2)
        ));
    }
}

export class SameSizeIndicator {

    constructor(
        public url: string,
        public top: number,
        public left: number) {
    }
}
