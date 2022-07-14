import { Component, AfterViewInit, OnDestroy, Inject, ViewChild, ElementRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Subscription } from 'rxjs';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { SameSizeIndicator } from '../samesizeindicator/samesizeindicator.component';
import { URLParserService } from '../services/urlparser.service';

@Component({
    selector: 'designer-anchoring-indicator',
    templateUrl: './anchoringindicator.component.html',
    styleUrls: ['./anchoringindicator.component.css']
})
export class AnchoringIndicatorComponent implements AfterViewInit, OnDestroy, ISelectionChangedListener {
    TOP_LEFT_IMAGE = 'designer/assets/images/anchoringtopleft.png';
    TOP_RIGHT_IMAGE = 'designer/assets/images/anchoringtopright.png';
    BOTTOM_LEFT_IMAGE = 'designer/assets/images/anchoringbottomleft.png';
    BOTTOM_RIGHT_IMAGE = 'designer/assets/images/anchoringbottomright.png';
    BOTTOM_RIGHT_LEFT_IMAGE = 'designer/assets/images/anchoringbottomrightleft.png';
    TOP_RIGHT_LEFT_IMAGE = 'designer/assets/images/anchoringtoprightleft.png';
    TOP_LEFT_BOTTOM_IMAGE = 'designer/assets/images/anchoringtopleftbottom.png';
    TOP_RIGHT_BOTTOM_IMAGE = 'designer/assets/images/anchoringtoprightbottom.png';
    TOP_RIGHT_LEFT_BOTTOM_IMAGE = 'designer/assets/images/anchoringtoprightleftbottom.png';

    anchoringIndicator: boolean;
    editorStateSubscription: Subscription;
    indicator: SameSizeIndicator;
    topAdjust: number;
    leftAdjust: number;

    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected urlParser: URLParserService,) {
        this.editorSession.addSelectionChangedListener(this);
    }

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'anchoringIndicator') {
                this.anchoringIndicator = this.editorSession.getState().anchoringIndicator;
                if (!this.anchoringIndicator) this.indicator = null;
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
    }

    selectionChanged(selection: Array<string>): void {
        this.indicator = null;
        if (this.anchoringIndicator && selection && selection.length == 1) {
            const frameElem = this.doc.querySelector('iframe');
            const frameRect = frameElem.getBoundingClientRect();
            this.topAdjust = frameRect.top;
            this.leftAdjust = frameRect.left;
            const nodeid = selection[0];
            const element = frameElem.contentWindow.document.querySelector("[svy-id='" + nodeid + "']");
            if (element) {
                const elementRect = element.getBoundingClientRect();
                let image: string;
                if (!this.urlParser.isCSSPositionFormLayout()) {
                    const selectionAnchor = parseInt(element.getAttribute('svy-anchors'));
                    if (selectionAnchor == 0 || selectionAnchor == 9) {
                        image = this.TOP_LEFT_IMAGE;
                    }
                    else if (selectionAnchor == 3)  {
                        image = this.TOP_RIGHT_IMAGE;
                    }
                    else if (selectionAnchor == 12) {
                        image = this.BOTTOM_LEFT_IMAGE;
                    }
                    else if (selectionAnchor == 6) {
                        image = this.BOTTOM_RIGHT_IMAGE;
                    }
                    else if (selectionAnchor == 14) {
                        image = this.BOTTOM_RIGHT_LEFT_IMAGE;
                    }
                    else if (selectionAnchor == 11) {
                        image = this.TOP_RIGHT_LEFT_IMAGE;
                    }
                    else if (selectionAnchor == 13) {
                        image = this.TOP_LEFT_BOTTOM_IMAGE;
                    }
                    else if (selectionAnchor == 7) {
                        image = this.TOP_RIGHT_BOTTOM_IMAGE;
                    }
                    else if (selectionAnchor == 15) {
                        image = this.TOP_RIGHT_LEFT_BOTTOM_IMAGE;
                    }
                }
                else {
                    const wrapper = element.closest('.svy-wrapper') as HTMLDivElement;
                    if (wrapper.style.top) {
                        if (wrapper.style.left) {
                            if (wrapper.style.bottom) {
                                if (wrapper.style.right) {
                                    image = this.TOP_RIGHT_LEFT_BOTTOM_IMAGE;
                                }
                                else {
                                    image = this.TOP_LEFT_BOTTOM_IMAGE;
                                }
                            }
                            else {
                                if (wrapper.style.right) {
                                    image = this.TOP_RIGHT_LEFT_IMAGE;
                                }
                                else {
                                    image = this.TOP_LEFT_IMAGE;
                                }
                            }
                        }
                        else {
                            if (wrapper.style.bottom) {
                                image = this.TOP_RIGHT_BOTTOM_IMAGE;
                            }
                            else {
                                image = this.TOP_RIGHT_IMAGE;
                            }
                        }
                    }
                    else {
                        if (wrapper.style.left) {
                            if (wrapper.style.right) {
                                image = this.BOTTOM_RIGHT_LEFT_IMAGE;
                            }
                            else {
                                image = this.BOTTOM_LEFT_IMAGE;
                            }
                        }
                        else {
                            image = this.BOTTOM_RIGHT_IMAGE;
                        }
                    }
                }
                this.indicator = new SameSizeIndicator(image, this.topAdjust + elementRect.top + 1, elementRect.left + this.leftAdjust + elementRect.width + 2);
            }
        }
    }
}