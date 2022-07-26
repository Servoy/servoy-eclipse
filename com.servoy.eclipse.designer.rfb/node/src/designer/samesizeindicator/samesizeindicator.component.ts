import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-samesize-indicator',
    templateUrl: './samesizeindicator.component.html',
    styleUrls: ['./samesizeindicator.component.css']
})
export class SameSizeIndicatorComponent implements AfterViewInit, OnDestroy, ISelectionChangedListener {
    SAME_WIDTH_IMAGE = 'designer/assets/images/samewidthindicator.png';
    SAME_HEIGHT_IMAGE = 'designer/assets/images/sameheightindicator.png';

    sameSizeIndicator: boolean;
    editorStateSubscription: Subscription;
    indicators: SameSizeIndicator[];

    constructor(protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
        this.editorSession.addSelectionChangedListener(this);
    }

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'sameSizeIndicator') {
                this.sameSizeIndicator = this.editorSession.getState().sameSizeIndicator;
                if (!this.sameSizeIndicator) this.indicators = [];
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
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

                const elements = this.editorContentService.getAllContentElements();
                Array.from(elements).forEach(node => {
                    if (element != node && node.parentElement.closest('.svy-responsivecontainer') == null) {
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

    private addSameSizeIndicator(newindicators: SameSizeIndicator[], elementRect: DOMRect, horizontal: boolean) {
        newindicators.push(new SameSizeIndicator(horizontal ? this.SAME_WIDTH_IMAGE : this.SAME_HEIGHT_IMAGE, this.editorContentService.getTopPositionIframe() + elementRect.top + 1, this.editorContentService.getLeftPositionIframe() + (horizontal ? elementRect.left + elementRect.width - 14 : elementRect.left + 2)));
    }
}

export class SameSizeIndicator {

    constructor(
        public url: string,
        public top: number,
        public left: number) {
    }
}
