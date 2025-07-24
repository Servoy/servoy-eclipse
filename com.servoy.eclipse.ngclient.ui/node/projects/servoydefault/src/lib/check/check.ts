
import { Renderer2, Component, ChangeDetectorRef, SimpleChanges, Input, ViewChild, ElementRef, ChangeDetectionStrategy, Inject, DOCUMENT } from '@angular/core';
import { FormattingService } from '@servoy/public';
import { ServoyDefaultBaseField } from '../basefield';

@Component({
    selector: 'servoydefault-check',
    templateUrl: './check.html',
    styleUrls: ['./check.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultCheck extends ServoyDefaultBaseField<HTMLInputElement> {
    @Input() horizontalAlignment: number;
    @ViewChild('input', { static: false }) input: ElementRef<HTMLInputElement>;

    selected = false;
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        for (const property in changes) {
            switch (property) {
                case 'dataProviderID':
                    this.setSelectionFromDataprovider();
                    break;
                case'horizontalAlignment':
                    this.setHorizontalAlignmentFlexbox(this.getNativeElement(), this.renderer, this.horizontalAlignment);
                    break;
            }
        }
    }
    
    getFocusElement() {
        return this.input.nativeElement;
    }

    public setHorizontalAlignmentFlexbox(element: any, renderer: Renderer2, halign: number) {
        if (halign !== -1) {
            if (halign === 0) {
                renderer.setStyle(element, 'justify-content', 'center');
            } else if (halign === 4) {
                renderer.setStyle(element, 'justify-content', 'flex-end');
            } else {
                renderer.setStyle(element, 'justify-content', 'flex-start');
            }
        }
    }

    attachHandlers() {
        this.renderer.listen( this.getFocusElement(), 'click', (e) => {
            if ((!this.readOnly && this.enabled) || this.findmode) {
                this.itemClicked(e);
            }
        });
        super.attachHandlers();
    }

    itemClicked(event: any) {
        if (event.target.localName === 'span' || event.target.localName === 'label') {
            this.selected = !this.selected;
        }

        if (this.valuelistID && this.valuelistID[0])
            this.dataProviderID = this.dataProviderID === this.valuelistID[0].realValue ? null : this.valuelistID[0].realValue;
        else if (typeof this.dataProviderID === 'string')
            this.dataProviderID = this.dataProviderID === '1' ? '0' : '1';
        else
            this.dataProviderID = this.dataProviderID > 0 ? 0 : 1;
        setTimeout(() => {
            this.pushUpdate();
            if (this.onActionMethodID) this.onActionMethodID(event);
        }, 0);
    }

    getSelectionFromDataprovider() {
        if (!this.dataProviderID)
            return false;
        if (this.valuelistID && this.valuelistID[0]) {
            return this.dataProviderID === this.valuelistID[0].realValue;
        } else if (typeof this.dataProviderID === 'string') {
            return this.dataProviderID === '1';
        } else {
            return this.dataProviderID > 0;
        }
    }

    setSelectionFromDataprovider() {
        this.selected = this.getSelectionFromDataprovider();
    }

    needsScrollbarInformation(): boolean {
        return false;
    }
}
