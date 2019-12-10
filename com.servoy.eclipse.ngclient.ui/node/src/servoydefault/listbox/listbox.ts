import {Component, Renderer2, ViewChild, Input, ElementRef, ChangeDetectorRef, AfterViewInit, SimpleChanges} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-listbox',
    templateUrl: './listbox.html'
} )
export class ServoyDefaultListBox extends ServoyDefaultBaseField implements AfterViewInit{
    @Input() multiselectListbox;
    
    selectedValues: any[];
        
    @ViewChild('element', {static: false}) elementRef:ElementRef;
    
    private changes: SimpleChanges;
    
    constructor(private changeDetectorRef : ChangeDetectorRef,  renderer: Renderer2, formattingService : FormattingService) { 
        super(renderer,formattingService);
    }
    
    ngOnInit() {
        //this method should do nothing
    }
    
    ngAfterViewInit() {
        this.ngOnChanges( this.changes );
        this.changeDetectorRef.detectChanges();
        this.addAttributes(); 
        this.attachFocusListeners(this.getFocusElement());
        this.attachHandlers(); 
    }

    ngOnChanges( changes: SimpleChanges ) {
        if ( !this.elementRef ) {
            if ( this.changes == null ) {
                this.changes = changes;
            }
            else {
                for ( let property in changes ) {
                    this.changes[property] = changes[property];
                }
            }
        }
        else {
            for ( let property in changes ) {
                let change = changes[property];
                switch ( property ) {
                    case "dataProviderID":
                        this.selectedValues = this.dataProviderID.split('\n');
                        break;

                }
            }
            super.ngOnChanges(changes);
        }
    }
    
    multiUpdate( isMultiSelect: boolean ) {
        var select = this.elementRef.nativeElement;
        var newValue = select.value;
        if (isMultiSelect && newValue)
        {
            var result = [];
            var options = select && select.options;
            var opt;

            for (var i=0, iLen=options.length; i<iLen; i++) {
              opt = options[i];

              if (opt.selected) {
                result.push(opt.text);
              }
            }
            
            newValue = result.join('\n');
            this.selectedValues = result;
        }   
        this.update(newValue);
    }
}
