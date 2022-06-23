import { Component, AfterViewInit, OnDestroy, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Subscription } from 'rxjs';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';

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
    topAdjust : number;
    leftAdjust : number;
    
    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document) {
        this.editorSession.addSelectionChangedListener(this);
    }

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'sameSizeIndicator') {
                this.sameSizeIndicator = this.editorSession.getState().sameSizeIndicator;
                if (!this.sameSizeIndicator)  this.indicators = [];
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
    }

    selectionChanged(selection: Array<string>): void {
        const newindicators: SameSizeIndicator[] = [];

        if (this.sameSizeIndicator && selection && selection.length == 1) {
            const frameElem = this.doc.querySelector('iframe');
            const frameRect = frameElem.getBoundingClientRect();
            this.topAdjust = frameRect.top;
            this.leftAdjust = frameRect.left;
            const nodeid = selection[0];
            const element = frameElem.contentWindow.document.querySelector("[svy-id='" + nodeid + "']");
            if (element) {
                let addedSameWidth: boolean = false;
                let addedSameHeight: boolean = false;

                const elementRect = element.getBoundingClientRect();

                const elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
                Array.from(elements).forEach(node => {
                    if (element != node) {
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
            }
        }
        this.indicators = newindicators;
    }

    private addSameSizeIndicator(newindicators: SameSizeIndicator[], elementRect: DOMRect, horizontal: boolean) {
        newindicators.push(new SameSizeIndicator(horizontal ? this.SAME_WIDTH_IMAGE : this.SAME_HEIGHT_IMAGE, this.topAdjust + elementRect.top + 1, this.leftAdjust + (horizontal ? elementRect.left + elementRect.width - 14 : elementRect.left + 2)));
    }
}

export class SameSizeIndicator {

    constructor(
        public url: string,
        public top: number,
        public left: number) {
    }
}
