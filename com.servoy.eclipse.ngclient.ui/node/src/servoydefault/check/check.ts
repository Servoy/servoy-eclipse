import { Renderer2, Component, ChangeDetectorRef, SimpleChanges, AfterViewInit, Input, ViewChild, ElementRef } from '@angular/core';
import { FormattingService } from '../../ngclient/servoy_public';
import { ServoyDefaultBaseField } from '../basefield';

@Component({
    selector: 'servoydefault-check',
    templateUrl: './check.html',
    styleUrls: ['./check.css']
})
export class ServoyDefaultCheck extends ServoyDefaultBaseField<HTMLInputElement> {
    @Input() horizontalAlignment: number;
    @ViewChild('input', { static: false }) input: ElementRef<HTMLInputElement>;

    selected = false;
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
        super(renderer, cdRef, formattingService);
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if(changes['horizontalAlignment'])
            this.setHorizontalAlignmentFlexbox(this.getNativeElement(), this.renderer, this.horizontalAlignment);
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
        console.log("attaching");
        console.log(this.getFocusElement());
        this.renderer.listen( this.getFocusElement(), 'click', (e) => {
            console.log(e);
            if (!this.readOnly && this.enabled) {
                this.itemClicked(e);
                if (this.onActionMethodID) this.onActionMethodID(e);
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
        this.pushUpdate();
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
