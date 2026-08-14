import { LoggerFactory, LoggerService, SabloTabseq, TooltipDirective } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { Component, Renderer2, ElementRef, SimpleChanges, ChangeDetectionStrategy, input, viewChild } from '@angular/core';
import { ServoyDefaultBaseField } from '../basefield';

@Component( {
    selector: 'servoydefault-radio',
    templateUrl: './radio.html',
    styleUrls: ['./radio.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, TooltipDirective, SabloTabseq]
} )
export class ServoyDefaultRadio extends ServoyDefaultBaseField<HTMLInputElement> {
    override readonly horizontalAlignment = input<any>(undefined);

    readonly input = viewChild<ElementRef<HTMLInputElement>>('input');

    selected = false;
    private log!: LoggerService;

    svyOnChanges( changes: SimpleChanges ) {
        this.setHorizontalAlignmentFlexbox( this.getNativeElement(), this.renderer, this.horizontalAlignment() );
        super.svyOnChanges( changes );
        if (changes.dataProviderID) {
			setTimeout(()=>{
				this.setSelectionFromDataprovider();
				this.input()!.nativeElement.checked = this.selected;
			});
		}
    }

    getFocusElement() {
        return this.input()!.nativeElement;
    }

    public setHorizontalAlignmentFlexbox( element: any, renderer: Renderer2, halign: any ) {
        if ( halign !== -1 ) {
            if ( halign === 0 ) {
                renderer.setStyle( element, 'justify-content', 'center' );
            } else if ( halign === 4 ) {
                renderer.setStyle( element, 'justify-content', 'flex-end' );
            } else {
                renderer.setStyle( element, 'justify-content', 'flex-start' );
            }
        }
    }

    attachHandlers() {
        this.renderer.listen( this.getFocusElement(), 'click', (e) => {
            if (!this.readOnly() && this.enabled()) {
                this.itemClicked(e);
                if (this.onActionMethodID()) this.onActionMethodID()(e);
            }
        });
        super.attachHandlers();
    }

    itemClicked( event: any ) {
		if (!this.selected) {
			if ( event.target.localName === 'span' || event.target.localName === 'label' || event.target.localName === 'input' )
            	this.selected = !this.selected;

        	if ( this.valuelistID() && this.valuelistID()[0] )
            	// eslint-disable-next-line eqeqeq
            	this.dataProviderID.set(this.dataProviderID() == this.valuelistID()[0].realValue ? null : this.valuelistID()[0].realValue);
        	else if ( typeof this.dataProviderID() === 'string' )
            	// eslint-disable-next-line eqeqeq
            	this.dataProviderID.set(this.dataProviderID() == '1' ? '0' : '1');
        	else
            	this.dataProviderID.set(this.dataProviderID() > 0 ? 0 : 1);
        	this.pushUpdate();
		} 
    }

    getSelectionFromDataprovider() {
        if ( !this.dataProviderID() )
            return false;
        if ( this.valuelistID() && this.valuelistID()[0] ) {
            // eslint-disable-next-line eqeqeq
            return this.dataProviderID() == this.valuelistID()[0].realValue;
        } else if ( typeof this.dataProviderID() === 'string' ) {
            // eslint-disable-next-line eqeqeq
            return this.dataProviderID() == '1';
        } else {
            return this.dataProviderID() > 0;
        }
    }

    setSelectionFromDataprovider() {
        this.selected = this.getSelectionFromDataprovider();
    }

    needsScrollbarInformation(): boolean {
        return false;
    }
}
