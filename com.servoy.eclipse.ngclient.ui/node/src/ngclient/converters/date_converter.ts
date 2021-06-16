import { IConverter, PropertyContext } from '../../sablo/converter.service';

import { DateTime } from 'luxon';

export class DateConverter implements IConverter {

    fromServerToClient( serverJSONValue: string, _currentClientValue?: Date, _propertyContext?: PropertyContext) {
        const dateObj = new Date( serverJSONValue );
        return dateObj;
    }

    fromClientToServer( newClientData: Date, _oldClientData?: Date ) {
        if ( !newClientData ) return null;

        let r = newClientData;
        if ( typeof newClientData === 'string' || typeof newClientData === 'number' ) r = new Date( newClientData as string );
        if ( isNaN( r.getTime() ) ) throw new Error( 'Invalid date/time value: ' + newClientData );// what should happen in this scenario , should we return null;
        return DateTime.fromJSDate(r).toISO();
    }

}
