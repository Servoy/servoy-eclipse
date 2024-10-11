import { IType, ITypesRegistryForSabloConverters, IPropertyContext } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';
import { DateType } from './date_converter';
import { LoggerService, LoggerFactory } from '@servoy/public';

// object type / default conversions
export class ObjectType implements IType<any> {

    public static readonly TYPE_NAME = 'object';

    private log: LoggerService;

	constructor(private readonly typesRegistry: ITypesRegistryForSabloConverters,
                private readonly converterService: ConverterService<unknown>,
                logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('ObjectType');
    }

	fromServerToClient(serverJSONValue: any, currentClientValue: any, propertyContext: IPropertyContext): any {
		// this means that it's either a property with defined 'object' type (with any value) or the result of a server-side
		// default conversion which is a JSON array or a JSON object that has a nested value with conversion(s); so convert any nested values that need it
		if (serverJSONValue instanceof Object) { // arrays are objects as well
			if (serverJSONValue[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY] !== undefined) {
				// it's already another type; for example a Date; convert it directly
				serverJSONValue = this.converterService.convertFromServerToClient(serverJSONValue, undefined,
						currentClientValue, undefined, undefined, propertyContext);
			} else {
				// see which nested sub-property/sub-element
				for (const i in serverJSONValue) // works for both arrays (indexes) and objects (keys) in JS
					if (serverJSONValue[i] instanceof Object && serverJSONValue[i][ConverterService.CONVERSION_CL_SIDE_TYPE_KEY] !== undefined)
						serverJSONValue[i] = this.converterService.convertFromServerToClient(serverJSONValue[i], undefined,
								                currentClientValue ? currentClientValue[i] : undefined, undefined, undefined, propertyContext);
			}
		}

		return serverJSONValue;
	}

    fromClientToServer(newClientData: any, oldClientData: any, propertyContext: IPropertyContext): [any, any] {
        let [valueToSend, newValueRef, cyclicDepError] =  this.fromClientToServerInternal(newClientData, oldClientData, propertyContext, new Set(), false);
        if (cyclicDepError) this.log.error("Value that will be sent to server (with nested cyclic refs set to null): " + JSON.stringify(valueToSend));

        return [valueToSend, newValueRef];
    }

    private fromClientToServerInternal(newClientData: any, oldClientData: any, propertyContext: IPropertyContext, alreadyProcessedNestingValues: Set<unknown>, cyclicDepError: boolean): [any, any, boolean] {
		let retVal = newClientData;
        
		// default conversion to server (for date values)
		if (newClientData instanceof Date) {
			const dateType = this.typesRegistry.getAlreadyRegisteredType(DateType.TYPE_NAME_SABLO);

			if (dateType) {
				// as this is an object type write also the type of date being sent; this works in 'object' type similarly to how dynamic client
				// side types are received from server but the other way around
				retVal = {};
				retVal[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY] = 'date'; // this is the server side name for DatePropertyType
				retVal[ConverterService.VALUE_KEY] = this.converterService.convertFromClientToServer(newClientData,
				        		 dateType , oldClientData, propertyContext)[0];
			}
		} else if (typeof newClientData === 'object' && !(newClientData instanceof Event) && !(newClientData instanceof Element)) {
            if (alreadyProcessedNestingValues.has(newClientData)) {
                if (!cyclicDepError) this.log.error(new Error("fromClientToServer: plain 'object' typed data cannot be sent to server (recursive same references will be changed to null) when it contains circular references. One of the circular refs is: " + newClientData));
                retVal = null;
                cyclicDepError = true;
            } else {
                alreadyProcessedNestingValues.add(newClientData);
                
    			let isChanged = false;
    			let newRetVal = {};
    			for (const i in newClientData) { // works for both arrays (indexes) and objects (keys) in JS
    				const oldEl = newClientData[i];
    				const newEl = this.fromClientToServerInternal(oldEl, oldClientData ? oldClientData[i] : undefined, propertyContext, alreadyProcessedNestingValues, cyclicDepError);
                    cyclicDepError ||= newEl[2];
    				if (oldEl !== newEl[0]) {
    					isChanged = true;
    					newRetVal[i] = newEl[0];
    				} else {
    					newRetVal[i] = oldEl;
    				}
    			}
    			if(isChanged) retVal = newRetVal;
            }
		}

		return [retVal, newClientData, cyclicDepError];
	}

}
