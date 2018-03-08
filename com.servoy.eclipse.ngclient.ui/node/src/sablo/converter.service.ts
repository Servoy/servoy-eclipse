import { Injectable } from '@angular/core';

@Injectable()
export class ConverterService {
    public static readonly INTERNAL_IMPL =  '__internalState';
    public static readonly TYPES_KEY = 'svy_types'; // TODO this should be sablo_types...
    // objects that have a function named like this in them will send to server the result of that function call when no conversion type is available (in case of
    // usage as handler arg. for example where we don't know the arg. types on client)
    public static readonly DEFAULT_CONVERSION_TO_SERVER_FUNC = "_dctsf";

    private customPropertyConverters:{[s:string]:IConverter} = {};

    constructor() {
    }
    
    public  convertFromServerToClient(serverSentData, conversionInfo, currentClientData?, scope?, modelGetter?) {
        if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
            var customConverter = this.customPropertyConverters[conversionInfo];
            if (customConverter) serverSentData = customConverter.fromServerToClient(serverSentData, currentClientData, scope, modelGetter);
            else { //converter not found - will not convert
//                $log.error("cannot find type converter (s->c) for: '" + conversionInfo + "'.");
            }
        } else if (conversionInfo) {
            for (var conKey in conversionInfo) {
                serverSentData[conKey] = this.convertFromServerToClient(serverSentData[conKey], conversionInfo[conKey], currentClientData ? currentClientData[conKey] : undefined, scope, modelGetter); // TODO should componentScope really stay the same here? 
            }
        }
        return serverSentData;
    }
    
    public convertFromClientToServer(newClientData, conversionInfo, oldClientData?) {
        if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
            var customConverter = this.customPropertyConverters[conversionInfo];
            if (customConverter) return customConverter.fromClientToServer(newClientData, oldClientData);
            else { //converter not found - will not convert
//                $log.error("cannot find type converter (c->s) for: '" + conversionInfo + "'.");
                return newClientData;
            }
        } else if (conversionInfo) {
            var retVal =   Array.isArray(newClientData)  ? [] : {};// was: (Array.isArray ? Array.isArray(newClientData) : $.isArray(newClientData)) ? [] : {};
            for (var conKey in conversionInfo) {
                retVal[conKey] = this.convertFromClientToServer(newClientData[conKey], conversionInfo[conKey], oldClientData ? oldClientData[conKey] : undefined);
            }
            return retVal;
        } else {
            return newClientData;
        }
    };
    
   public convertClientObject (value) {
        if (value instanceof Date) {
            value  = this.convertFromClientToServer(value, "Date", null);
        } else if (value && typeof value[ConverterService.DEFAULT_CONVERSION_TO_SERVER_FUNC] == 'function') {
            return value[ConverterService.DEFAULT_CONVERSION_TO_SERVER_FUNC]();
        }
        return value;
    }
   
   public static isChanged(now, prev, conversionInfo) {
       if ((typeof conversionInfo === 'string' || typeof conversionInfo === 'number') && now && now[ConverterService.INTERNAL_IMPL] && now[ConverterService.INTERNAL_IMPL].isChanged) {
           return now[ConverterService.INTERNAL_IMPL].isChanged();
       }

       if (now === prev) return false;
       if (now && prev) {
           if (now instanceof Array) {
               if (prev instanceof Array) {
                   if (now.length != prev.length) return true;
               } else {
                   return true;
               }
           }
           if (now instanceof Date) {
               if (prev instanceof Date) {
                   return now.getTime() != prev.getTime();
               }
               return true;
           }

           if ((now instanceof Object) && (prev instanceof Object)) {
               // first build up a list of all the properties both have.
               var fulllist = this.getCombinedPropertyNames(now, prev);
               for (var prop in fulllist) {
                   if(prop == "$$hashKey") continue; // ng repeat creates a child scope for each element in the array any scope has a $$hashKey property which must be ignored since it is not part of the model
                   if (prev[prop] !== now[prop]) {
                       if (typeof now[prop] == "object") {
                           if (this.isChanged(now[prop],prev[prop], conversionInfo ? conversionInfo[prop] : undefined)) {
                               return true;
                           }
                       } else {
                           return true;
                       }
                   }
               }
               return false;
           }
       }
       return true;
   }
   
   public prepareInternalState(propertyValue, internalStateValue) {
       if (!propertyValue.hasOwnProperty(ConverterService.INTERNAL_IMPL))
       {
           if (Object.defineProperty) {
               // try to avoid unwanted iteration/non-intended interference over the private property state
               Object.defineProperty(propertyValue, ConverterService.INTERNAL_IMPL, {
                   configurable: false,
                   enumerable: false,
                   writable: false,
                   value: internalStateValue
               });
           } else propertyValue[ConverterService.INTERNAL_IMPL] = internalStateValue;
       } 
//       else $log.warn("An attempt to prepareInternalState on value '" + propertyValue + "' which already has internal state was ignored.");
   }
   
   /**
    * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
    * return obj[a][b][c] making sure that if any does not exist or is null (for example b) it will be set to {}.
    */
   public getOrCreateInDepthProperty(...args) {
       if (arguments.length == 0) return undefined;

       var ret = arguments[0];
       if (ret == undefined || ret === null || arguments.length == 1) return ret;
       var p;
       var i;
       for (i = 1; i < arguments.length; i++) {
           p = ret;
           ret = ret[arguments[i]];
           if (ret === undefined || ret === null) {
               ret = {};
               p[arguments[i]] = ret;
           }
       }
       return ret;
   }
   
   /**
    * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
    * return obj[a][b][c] making sure that if any does not exist or is null it will just return null/undefined instead of erroring out.
    */
   public getInDepthProperty(...args) {
       if (arguments.length == 0) return undefined;

       var ret = arguments[0];
       if (ret == undefined || ret === null || arguments.length == 1) return ret;
       var i;
       for (i = 1; i < arguments.length; i++) {
           ret = ret[arguments[i]];
           if (ret === undefined || ret === null) {
               return i == arguments.length - 1 ? ret : undefined;
           }
       }

       return ret;
   }
    
    /**
     * Registers a custom client side property handler into the system. These handlers are useful
     * for custom property types that require some special handling when received through JSON from server-side
     * or for sending content updates back. (for example convert received JSON into a different JS object structure that will be used
     * by beans or just implement partial updates for more complex property contents)
     *  
     * @param customHandler an object with the following methods/fields:
     * {
     * 
     *              // Called when a JSON update is received from the server for a property
     *              // @param serverSentJSONValue the JSON value received from the server for the property
     *              // @param currentClientValue the JS value that is currently used for that property in the client; can be null/undefined if
     *              //        conversion happens for service API call parameters for example...
     *              // @param scope scope that can be used to add component/service and property related watches; can be null/undefined if
     *              //        conversion happens for service/component API call parameters for example...
     *              // @param modelGetter a function that returns the model that can be used to find other properties of the service/component if needed (if the
     *              //        property is 'linked' to another one); can be null/undefined if conversion happens for service/component API call parameters for example...
     *              // @return the new/updated client side property value; if this returned value is interested in triggering
     *              //         updates to server when something changes client side it must have these member functions in this[$sabloConverters.INTERNAL_IMPL]:
     *              //              setChangeNotifier: function(changeNotifier) - where changeNotifier is a function that can be called when
     *              //                                                          the value needs to send updates to the server; this method will
     *              //                                                          not be called when value is a call parameter for example, but will
     *              //                                                          be called when set into a component's/service's property/model
     *              //              isChanged: function() - should return true if the value needs to send updates to server // TODO this could be kept track of internally
     *              fromServerToClient: function (serverSentJSONValue, currentClientValue, scope, modelGetter) { (...); return newClientValue; },
     * 
     *              // Converts from a client property JS value to a JSON that will be sent to the server.
     *              // @param newClientData the new JS client side property value
     *              // @param oldClientData the old JS JS client side property value; can be null/undefined if
     *              //        conversion happens for service API call parameters for example...
     *              // @return the JSON value to send to the server.
     *              fromClientToServer: function(newClientData, oldClientData) { (...); return sendToServerJSON; }
     * 
     *              // Some 'smart' property types need an angular scope to register watches to; this method will get called on them
     *              // when the scope that they should use changed (old scope could get destroyed and then after a while a new one takes it's place).
     *              // This gives such property types a way to keep their watches operational even on the new scope.
     *              // @param clientValue the JS client side property value
     *              // @param scope the new scope. If null it means that the previous scope just got destroyed and property type should perform needed cleanup.
     *              updateAngularScope: function(clientValue, scope) { (...); }
     * 
     * }
     */
    public registerCustomPropertyHandler(propertyTypeID:string, customHandler:IConverter, overwrite?:boolean) {
        if (overwrite == false && this.customPropertyConverters[propertyTypeID] ) return; 
        this.customPropertyConverters[propertyTypeID] = customHandler;
    }
    
    public getEventArgs(args,eventName)
    {
        var newargs = []
        for (var i = 0; i < args.length; i++) {
            var arg = args[i]
            if (arg && arg.originalEvent) arg = arg.originalEvent;
            if(arg  instanceof MouseEvent ||arg  instanceof KeyboardEvent){
                var $event = arg;
                var eventObj = {}
                var modifiers = 0;
                if($event.shiftKey) modifiers = modifiers||SwingModifiers.SHIFT_MASK;
                if($event.metaKey) modifiers = modifiers||SwingModifiers.META_MASK;
                if($event.altKey) modifiers = modifiers|| SwingModifiers.ALT_MASK;
                if($event.ctrlKey) modifiers = modifiers || SwingModifiers.CTRL_MASK;

                eventObj['type'] = 'event'; 
                eventObj['eventName'] = eventName; 
                eventObj['modifiers'] = modifiers;
                eventObj['timestamp'] = new Date().getTime();
                eventObj['x']= $event['pageX'];
                eventObj['y']= $event['pageY'];
                arg = eventObj
            }
            else if (arg instanceof Event)
            {
                var eventObj = {}
                eventObj['type'] = 'event'; 
                eventObj['eventName'] = eventName;
                eventObj['timestamp'] = new Date().getTime();
                arg = eventObj
            }
            else arg = this.convertClientObject(arg); // TODO should be $sabloConverters.convertFromClientToServer(now, beanConversionInfo[property] ?, undefined);, but as we do not know handler arg types, we just do default conversion (for dates & types that use $sabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC)

            newargs.push(arg)
        }
        return newargs;
    }
    
    private static getCombinedPropertyNames(now,prev) {
        var fulllist = {}
        if (prev) {
            var prevNames = Object.getOwnPropertyNames(prev);
            for(var i=0; i < prevNames.length; i++) {
                fulllist[prevNames[i]] = true;
            }
        }
        if (now) {
            var nowNames = Object.getOwnPropertyNames(now);
            for(var i=0;i < nowNames.length;i++) {
                fulllist[nowNames[i]] = true;
            }
        }
        return fulllist;
    }
}

export interface IConverter {
    fromServerToClient(serverSentData:Object, currentClientData:Object, scope?:Object, modelGetter?:Object):Object;
    fromClientToServer(newClientData:Object, oldClientData:Object):Object;
}

class SwingModifiers {
    public static readonly SHIFT_MASK = 1;
    public static readonly CTRL_MASK = 2;
    public static readonly META_MASK = 4;
    public static readonly ALT_MASK = 8;
    public static readonly ALT_GRAPH_MASK = 32;
    public static readonly BUTTON1_MASK = 16;
    public static readonly BUTTON2_MASK = 8;
    public static readonly SHIFT_DOWN_MASK = 64;
    public static readonly CTRL_DOWN_MASK = 128;
    public static readonly META_DOWN_MASK = 256;
    public static readonly ALT_DOWN_MASK = 512;
    public static readonly BUTTON1_DOWN_MASK = 1024;
    public static readonly BUTTON2_DOWN_MASK = 2048;
    public static readonly DOWN_MASK = 4096;
    public static readonly ALT_GRAPH_DOWN_MASK = 8192;
}