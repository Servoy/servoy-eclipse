import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';
import { SameSizeIndicator } from '../samesizeindicator/samesizeindicator.component';
import { URLParserService } from '../services/urlparser.service';

@Component({
    selector: 'designer-anchoring-indicator',
    templateUrl: './anchoringindicator.component.html',
    styleUrls: ['./anchoringindicator.component.css'],
    standalone: false
})
export class AnchoringIndicatorComponent implements AfterViewInit, OnDestroy, ISelectionChangedListener, IContentMessageListener {
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
    
    constructor(protected readonly editorSession: EditorSessionService, protected editorContentService: EditorContentService, protected urlParser: URLParserService,) {
        this.editorSession.addSelectionChangedListener(this);
        this.editorContentService.addContentMessageListener(this);
    }

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'anchoringIndicator') {
                this.anchoringIndicator = this.editorSession.getState().anchoringIndicator;
                if (!this.anchoringIndicator) this.indicator = null;
            }
            if (id == 'dragging'){
                if (this.editorSession.getState().dragging){
                    this.indicator = null;
                }
                else{
                    this.selectionChanged(this.editorSession.getSelection());
                }
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
        this.editorContentService.removeContentMessageListener(this);
    }

    contentMessageReceived(id: string) {
        if (id === 'redrawDecorators') {
            this.selectionChanged(this.editorSession.getSelection());
        }
    }

    selectionChanged(selection: Array<string>): void {
        this.indicator = null;
        if (this.anchoringIndicator && selection && selection.length == 1) {
            this.editorContentService.executeOnlyAfterInit(() => {
                const element = this.editorContentService.getContentElement(selection[0])
                if (element) {
                    if (element.parentElement.closest('.svy-responsivecontainer') || element.closest('.svy-csspositioncontainer')) return;
                    const elementRect = element.getBoundingClientRect();
                    let wrapperRect: DOMRect = null;
                    let image: string;
                    if (!this.urlParser.isCSSPositionFormLayout()) {
                        const selectionAnchor = parseInt(element.getAttribute('svy-anchors'));
                        if (selectionAnchor == 0 || selectionAnchor == 9) {
                            image = this.TOP_LEFT_IMAGE;
                        }
                        else if (selectionAnchor == 3) {
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
                        const wrapper: HTMLDivElement = element.closest('.svy-wrapper');
                        if (element.classList.contains('svy-formcomponent')) {
							wrapperRect = wrapper.getBoundingClientRect();
						}
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
                    this.indicator = new SameSizeIndicator(image, this.editorContentService.getGlasspaneTopDistance() + (wrapperRect ? wrapperRect.top : elementRect.top) + 1, (wrapperRect ? wrapperRect.left : elementRect.left) + this.editorContentService.getGlasspaneLeftDistance() + (wrapperRect ? wrapperRect.width : elementRect.width) + 2);
                }
            });
        }
    }
}