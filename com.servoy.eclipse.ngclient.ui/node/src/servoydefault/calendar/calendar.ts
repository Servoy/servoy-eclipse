import { Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges, Renderer2, ElementRef, ViewChild } from '@angular/core';

import { PropertyUtils, FormattingService,I18NProvider } from '../../ngclient/servoy_public'

import { DateTimeAdapter,OwlDateTimeIntl } from 'ng-pick-datetime';

import * as moment from 'moment';
import * as numeral from 'numeral';

@Component( {
    selector: 'servoydefault-calendar',
    templateUrl: './calendar.html',
        providers: [OwlDateTimeIntl]
} )
export class ServoyDefaultCalendar implements OnInit, OnChanges {
    @Input() name;
    @Input() servoyApi;

    @Input() onActionMethodID;
    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;
    @Input() onRightClickMethodID;

    @Input() background
    @Input() borderType
    @Input() dataProviderID
    @Output() dataProviderIDChange = new EventEmitter();
    @Input() displaysTags
    @Input() editable
    @Input() enabled
    @Input() findmode
    @Input() fontType
    @Input() foreground
    @Input() format
    @Input() horizontalAlignment
    @Input() location
    @Input() margin
    @Input() placeholderText
    @Input() readOnly
    @Input() selectOnEnter
    @Input() size
    @Input() styleClass
    @Input() tabSeq
    @Input() text
    @Input() toolTipText
    @Input() transparent
    @Input() valuelistID
    @Input() visible

    @ViewChild( 'element' ) elementRef: ElementRef;
    @ViewChild( 'inputElement' ) inputElementRef: ElementRef;
    
    public firstDayOfWeek = 1;
    public hour12Timer = false;
    public pickerType = "both";
    public showSecondsTimer  = false;

    constructor( private readonly renderer: Renderer2, 
                            private formattingService: FormattingService, 
                            i18nProvider:I18NProvider,
                            dateTimeAdapter: DateTimeAdapter<any> ,
                            owlDateTimeIntl:OwlDateTimeIntl) {
        dateTimeAdapter.setLocale( numeral.locale() );
        i18nProvider.getI18NMessages("servoy.button.ok","servoy.button.cancel").then((val)=> {
            if (val["servoy.button.ok"]) owlDateTimeIntl.setBtnLabel = val["servoy.button.ok"]
            if (val["servoy.button.cancel"]) owlDateTimeIntl.cancelBtnLabel = val["servoy.button.cancel"]
        })
        
        const ld = moment.localeData();
        this.firstDayOfWeek = ld.firstDayOfWeek();
        const  lts = ld.longDateFormat("LTS");
        this.hour12Timer = lts.indexOf("a") >= 0 || lts.indexOf("A") >= 0;
    }

    ngOnInit() {

    }

    public dateChanged( event ) {
        if ( event && event.value ) {
            this.dataProviderID = event.value.toDate()
        }
        else this.dataProviderID = null;
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case "borderType":
                    PropertyUtils.setBorder( this.elementRef.nativeElement, this.renderer, change.currentValue );
                    break;
                case "background":
                case "transparent":
                    this.renderer.setStyle( this.inputElementRef.nativeElement, "backgroundColor", this.transparent ? "transparent" : change.currentValue );
                    break;
                case "foreground":
                    this.renderer.setStyle( this.inputElementRef.nativeElement, "color", change.currentValue );
                    break;
                case "fontType":
                    this.renderer.setStyle( this.inputElementRef.nativeElement, "font", change.currentValue );
                    break;
                case "format":
                    //                setDateFormat($scope.model.format, 'display');
                    const format = change.currentValue.display;
                    const showCalendar = format.indexOf("y") >= 0 || format.indexOf("M") >= 0;
                    const showTime = format.indexOf("h") >= 0 || format.indexOf("H") >= 0 || format.indexOf("m") >= 0;
                    if (showCalendar) {
                        if (showTime) this.pickerType = "both";
                        else this.pickerType = "calendar"
                    }
                    else this.pickerType = "timer"
                    this.showSecondsTimer = format.indexOf("s") >= 0;
                    this.hour12Timer = format.indexOf("h") >= 0 || format.indexOf("a") >= 0 || format.indexOf("A") >= 0;
                    break;
                case "horizontalAlignment":
                    PropertyUtils.setHorizontalAlignment( this.inputElementRef.nativeElement, this.renderer, change.currentValue );
                    break;
                case "enabled":
                    if ( change.currentValue )
                        this.renderer.removeAttribute( this.inputElementRef.nativeElement, "disabled" );
                    else
                        this.renderer.setAttribute( this.inputElementRef.nativeElement, "disabled", "disabled" );
                    break;
                case "editable":
                    if ( change.currentValue )
                        this.renderer.removeAttribute( this.inputElementRef.nativeElement, "readonly" );
                    else
                        this.renderer.setAttribute( this.inputElementRef.nativeElement, "readonly", "readonly" );
                    break;
                case "placeholderText":
                    if ( change.currentValue ) this.renderer.setAttribute( this.elementRef.nativeElement, 'placeholder', change.currentValue );
                    else this.renderer.removeAttribute( this.elementRef.nativeElement, 'placeholder' );
                    break;
                case "margin":
                    if ( change.currentValue ) {
                        for ( let style in change.currentValue ) {
                            this.renderer.setStyle( this.elementRef.nativeElement, style, change.currentValue[style] );
                        }
                    }
                    break;
                case "selectOnEnter":
                    if ( change.currentValue ) PropertyUtils.addSelectOnEnter( this.inputElementRef.nativeElement, this.renderer );
                    break;
                case "size":
                    this.renderer.setStyle( this.inputElementRef.nativeElement, "height", change.currentValue["height"] + "px" );
                    break;
                case "styleClass":
                    if ( change.previousValue )
                        this.renderer.removeClass( this.inputElementRef.nativeElement, change.previousValue );
                    if ( change.currentValue )
                        this.renderer.addClass( this.inputElementRef.nativeElement, change.currentValue );
                    break;
            }
        }
    }

    update( val: string ) {
        console.log( val );
        this.dataProviderID = this.formattingService.parse( val, this.format, this.dataProviderID );
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    myapicall( a1, a2, a3 ) {
        console.log( "api call" + a1 + "," + a2 + "," + a3 );
    }

}
