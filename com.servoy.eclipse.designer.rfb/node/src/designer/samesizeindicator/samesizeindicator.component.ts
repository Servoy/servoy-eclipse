import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'designer-samesize-indicator',
    templateUrl: './samesizeindicator.component.html',
    styleUrls: ['./samesizeindicator.component.css'],
    standalone: false
})
export class SameSizeIndicatorComponent implements AfterViewInit, OnDestroy, ISelectionChangedListener, IContentMessageListener  {
    SAME_WIDTH_IMAGE = 'designer/assets/images/samewidthindicator.png';
    SAME_HEIGHT_IMAGE = 'designer/assets/images/sameheightindicator.png';

    sameSizeIndicator: boolean;
    editorStateSubscription: Subscription;
    indicators: SameSizeIndicator[];
    
    constructor(protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
        this.editorSession.addSelectionChangedListener(this);
        this.editorContentService.addContentMessageListener(this);
    }

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'sameSizeIndicator') {
                this.sameSizeIndicator = this.editorSession.getState().sameSizeIndicator;
                if (!this.sameSizeIndicator) this.indicators = [];
            }
            if (id == 'dragging'){
                if (this.editorSession.getState().dragging){
                    this.indicators = null;
                }
                else{
                    this.selectionChanged(this.editorSession.getSelection());
                }
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
    }
    
    contentMessageReceived(id: string) {
        if (id === 'redrawDecorators') {
            this.selectionChanged(this.editorSession.getSelection());
        }
    }

    selectionChanged(selection: Array<string>): void {
        const newindicators: SameSizeIndicator[] = [];

        if (this.sameSizeIndicator && selection && selection.length == 1) {
            const nodeid = selection[0];
            this.editorContentService.executeOnlyAfterInit(() => {
                const element = this.editorContentService.getContentElement(nodeid);
                if (!element || element.parentElement.closest('.svy-responsivecontainer')) return;
                let addedSameWidth = false;
                let addedSameHeight = false;

                const elementRect = element.getBoundingClientRect();

                const elements = this.removeHiddenElements(this.editorContentService.getAllContentElements());
                Array.from(elements).forEach(node => {
                    if (element != node && node.parentElement.closest('.svy-responsivecontainer') == null && !element.classList.contains('svy-formcomponent')) {
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

    private removeHiddenElements(elements: Array<HTMLElement>) {
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
        newindicators.push(new SameSizeIndicator(horizontal ? this.SAME_WIDTH_IMAGE : this.SAME_HEIGHT_IMAGE, this.editorContentService.getGlasspaneTopDistance() + elementRect.top + 1, this.editorContentService.getGlasspaneLeftDistance() + (horizontal ? elementRect.left + elementRect.width - 14 : elementRect.left + 2)));
    }
}

export class SameSizeIndicator {

    constructor(
        public url: string,
        public top: number,
        public left: number) {
    }
}
