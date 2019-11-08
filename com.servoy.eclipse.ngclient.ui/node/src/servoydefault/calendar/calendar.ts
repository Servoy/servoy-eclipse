import { Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges, Renderer2, ElementRef, ViewChild } from '@angular/core';

import { PropertyUtils, FormattingService,I18NProvider } from '../../ngclient/servoy_public'

import { DateTimeAdapter,OwlDateTimeIntl } from 'ng-pick-datetime';

import {ServoyDefaultBaseField} from '../basefield'

import * as moment from 'moment';
import * as numeral from 'numeral';

@Component( {
    selector: 'servoydefault-calendar',
    templateUrl: './calendar.html',
        providers: [OwlDateTimeIntl]
} )
export class ServoyDefaultCalendar extends  ServoyDefaultBaseField {

    @ViewChild( 'inputElement' ,{static: true} ) inputElementRef: ElementRef;
    
    public firstDayOfWeek = 1;
    public hour12Timer = false;
    public pickerType = "both";
    public showSecondsTimer  = false;

    constructor( renderer: Renderer2, 
                            formattingService: FormattingService, 
                            i18nProvider:I18NProvider,
                            dateTimeAdapter: DateTimeAdapter<any> ,
                            owlDateTimeIntl:OwlDateTimeIntl) {
        super(renderer,formattingService);
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
    
    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
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
                case "size":
                    this.renderer.setStyle( this.inputElementRef.nativeElement, "height", change.currentValue["height"] + "px" );
                    break;
            }
        }
        super.ngOnChanges(changes);
    }


    public dateChanged( event ) {
        if ( event && event.value ) {
            this.dataProviderID = event.value.toDate()
        }
        else this.dataProviderID = null;
        this.dataProviderIDChange.emit( this.dataProviderID );
    }
    
    getFocusElement() : any{
        return this.inputElementRef.nativeElement;
    }
}
