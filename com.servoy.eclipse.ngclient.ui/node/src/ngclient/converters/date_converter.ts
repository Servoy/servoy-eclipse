import { IConverter } from '../../sablo/converter.service'

import * as moment from 'moment';

export class DateConverter implements IConverter {
    
    fromServerToClient( serverJSONValue, currentClientValue?, propertyContext?:(propertyName: string)=>any) {
        var dateObj = new Date( serverJSONValue );
        return dateObj;
    }

    fromClientToServer( newClientData, oldClientData? ) {
        if ( !newClientData ) return null;

        var r = newClientData;
        if ( typeof newClientData === 'string' || typeof newClientData === 'number' ) r = new Date( newClientData as string );
        if ( isNaN( r.getTime() ) ) throw new Error( "Invalid date/time value: " + newClientData )// what should happen in this scenario , should we return null;
        return moment( r ).format();
    }

}