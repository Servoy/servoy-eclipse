import { IType, IPropertyContext } from '../../sablo/types_registry';

import { DateTime } from 'luxon';

export class DateType implements IType<Date> {

    public static readonly TYPE_NAME_SVY = 'svy_date';
    public static readonly TYPE_NAME_SABLO = 'Date';

    fromServerToClient(serverJSONValue: string, _currentClientValue?: Date, _propertyContext?: IPropertyContext) {
        const dateObj = (serverJSONValue != null) ? new Date(serverJSONValue) : serverJSONValue;
        return dateObj as Date;
    }

    fromClientToServer(newClientData: Date, _oldClientData?: Date, _propertyContext?: IPropertyContext): [any, Date] | null {
        if (!newClientData) return null;

        let r = newClientData;
        if (typeof newClientData === 'string' || typeof newClientData === 'number') r = new Date(newClientData as string);
        if (isNaN(r.getTime())) throw new Error('Invalid date/time value: ' + newClientData);// what should happen in this scenario , should we return null;
        return [DateTime.fromJSDate(r).toISO(), newClientData];
    }

}
