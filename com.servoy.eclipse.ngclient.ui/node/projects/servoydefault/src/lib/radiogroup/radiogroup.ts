import { Component, OnInit, SimpleChanges, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject, DOCUMENT } from '@angular/core';
import { ServoyDefaultBaseChoice } from '../basechoice';
import { FormattingService, PropertyUtils } from '@servoy/public';


@Component({
    selector: 'servoydefault-radiogroup',
    templateUrl: './radiogroup.html',
    styleUrls: ['./radiogroup.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultRadiogroup extends ServoyDefaultBaseChoice {

    value: any;
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }
    
    svyOnChanges(changes: SimpleChanges) {
		for (const property of Object.keys(changes)) {
            switch (property) {
                case 'scrollbars':
					this.elementRef.nativeElement.classList.remove('horizontaldirection');
                    if (changes.scrollbars.currentValue === 36) {
						this.elementRef.nativeElement.classList.add('horizontaldirection');
					}
                    break;
            }
        }
		super.svyOnChanges(changes);
    }

    getFocusElement(): HTMLElement {
        let focusElement = super.getFocusElement();
        if (focusElement === null) focusElement = this.getNativeElement();
        return focusElement;
    }

    setSelectionFromDataprovider() {
        this.value = this.dataProviderID;
        if (this.valuelistID) {
            for (let i = 0; i < this.valuelistID.length; i++) {
                const item = this.valuelistID[i];
                if ((item.realValue + '') === (this.dataProviderID + '')) {
                    this.value = item.realValue;
                    break;
                }
            }
        }
    }

    itemClicked(event, index) {
        const newValue = event.target.value ? event.target.value : event.target.innerText;
        const changed = !(newValue === this.value);
        this.value = newValue;

        super.baseItemClicked(event, changed, newValue);
    }

    attachEventHandlers(element, index) {
        this.renderer.listen(element, 'click', (e) => {
            if (!this.readOnly && this.enabled) {
                this.itemClicked(e, index);
                if (this.onActionMethodID) this.onActionMethodID(e);
            }
        });
        super.attachEventHandlers(element, index);
    }

    getSelectedElements() {
        return [this.value];
    }
}
