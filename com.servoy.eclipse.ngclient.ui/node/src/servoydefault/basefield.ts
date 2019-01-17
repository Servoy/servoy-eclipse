import {
  OnInit,
  Input,
  Output,
  EventEmitter,
  SimpleChanges,
  Renderer2,
} from '@angular/core';

import {PropertyUtils, FormattingService} from '../ngclient/servoy_public'

import {ServoyDefaultBaseComponent} from './basecomponent'

export class ServoyDefaultBaseField extends  ServoyDefaultBaseComponent implements OnInit{

    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() editable;
    @Input() findmode;
    @Input() placeholderText;
    @Input() readOnly;
    @Input() selectOnEnter;
    @Input() valuelistID;

    valueBeforeChange: any;

    constructor(renderer: Renderer2, private formattingService : FormattingService) {
        super(renderer);
    }

    ngOnInit() {
      if(this.onFocusGainedMethodID)
        this.renderer.listen( this.getNativeElement(), 'focus', ( e ) => {
          this.onFocusGainedMethodID(e);
          this.valueBeforeChange = this.getNativeElement().value;
        } );
      if(this.onFocusLostMethodID)
        this.renderer.listen( this.getNativeElement(), 'blur', ( e ) => {
          this.onFocusLostMethodID(e);
          this.valueBeforeChange = this.getNativeElement().value;
        } );
      super.ngOnInit();
    }

    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case "editable":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.elementRef.nativeElement,  "readonly" );
                    else
                        this.renderer.setAttribute(this.elementRef.nativeElement,  "readonly", "readonly" );
                    break;
                case "placeholderText":
                    if ( change.currentValue ) this.renderer.setAttribute(this.getNativeElement(),   'placeholder', change.currentValue );
                    else  this.renderer.removeAttribute(this.getNativeElement(),  'placeholder' );
                    break;
                case "selectOnEnter":
                    if ( change.currentValue ) PropertyUtils.addSelectOnEnter( this.elementRef.nativeElement, this.renderer );
                    break;

            }
        }
        super.ngOnChanges(changes);
    }

    update( val: string ) {
        if(!this.findmode && this.format) {
            var newDataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
            if(this.dataProviderID == newDataProviderID) {
                this.getNativeElement().value = this.valueBeforeChange;
            }
            else {
                this.dataProviderID = newDataProviderID;
            }
        }
        else this.dataProviderID = val;
        this.dataProviderIDChange.emit( this.dataProviderID );
    }
}
