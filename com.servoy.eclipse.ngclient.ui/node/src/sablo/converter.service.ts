import { Injectable } from '@angular/core';
import { LoggerService, LoggerFactory } from '@servoy/public';
import { ObjectType } from './converters/object_converter';
import { DateType } from './converters/date_converter';
import { IType, IPropertyContext, TypesRegistry, IWebObjectFunction, PushToServerUtils } from '../sablo/types_registry';

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

@Injectable({
    providedIn: 'root'
})
export class ConverterService {

    public static CONVERSION_CL_SIDE_TYPE_KEY = '_T';
    public static VALUE_KEY = '_V';

    private log: LoggerService;
    private lookedUpObjectType: IType<any>;

    constructor(logFactory: LoggerFactory, private typesRegistry: TypesRegistry) {
        this.log = logFactory.getLogger('ConverterService');

        const dateType = new DateType();
        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SVY, dateType);
        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SABLO, dateType);
        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, this)); // used for 'object' type as well as for the default conversions
    }

    public static getCombinedPropertyNames(now: any, prev: any) {
        const fulllist = {};
        if (prev) {
            const prevNames = Object.getOwnPropertyNames(prev);
            for (const prevPropName of prevNames) {
                fulllist[prevPropName] = true;
            }
        }
        if (now) {
            const nowNames = Object.getOwnPropertyNames(now);
            for (const nowPropName of nowNames) {
                fulllist[nowPropName] = true;
            }
        }
        return fulllist;
    }

    public convertFromServerToClient(serverSentData: any,
            typeOfData: IType<any>,
            currentClientData: any,
            /* some types decide at runtime the type needed on client - for example dataprovider type could send date, and we will store that info here: */
            dynamicPropertyTypesHolder: { [nameOrIndex: string]: IType<any> },
            keyForDynamicTypes: string,
            propertyContext: IPropertyContext): any {

        let convertedData = serverSentData;
        if (typeOfData) {
            convertedData = typeOfData.fromServerToClient(serverSentData, currentClientData, propertyContext);

            // if no dynamic type, remove any previously stored dynamic type for this value
            if (dynamicPropertyTypesHolder && keyForDynamicTypes) delete dynamicPropertyTypesHolder[keyForDynamicTypes];
        } else if (serverSentData instanceof Object && serverSentData[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY] !== undefined) {
            // NOTE: default server conversions will end up here with 'object' type if they need any Date conversions (value or sub-property/sub-elements)

            // so a conversion is required client side but the type is not known beforehand on client (can be a result of
            // JSONUtils.java # defaultToJSONValue(...) or could be a dataprovider type in a record view - foundset based
            // impl. should handle these varying simple types nicer already inside special property types such as foundset/component/foundsetLinked)

            const lookedUpType = this.typesRegistry.getAlreadyRegisteredType(serverSentData[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]);
            if (lookedUpType) {
                // if caller is interested in storing dynamic types do that; (remember dynamic conversion info for when it will be sent back to server - it might need special conversion as well)
                if (dynamicPropertyTypesHolder && keyForDynamicTypes) dynamicPropertyTypesHolder[keyForDynamicTypes] = lookedUpType;

                convertedData = lookedUpType.fromServerToClient(serverSentData[ConverterService.VALUE_KEY], currentClientData, propertyContext);

            } else { // needed type not found - will not convert
                this.log.error('no such type was registered (s->c varying or default type conversion) for: '
                    + JSON.stringify(serverSentData[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY], null, 2) + '.');
            }
        } else if (dynamicPropertyTypesHolder && keyForDynamicTypes)
            delete dynamicPropertyTypesHolder[keyForDynamicTypes]; // no dynamic type; so remove any previously stored dynamic type for this value

        return convertedData;
    }

    /**
     * @return It CANNOT RETURN null, it will always return a tuple with the two values that can be null; return a tuple with first element being the JSON value
     * to send to the server and the second element being the new value of the property on client (some prop. types like array and
     * custom object can change the new value references on client into Proxy instances of those values - to intercept shallow pushToServer subvalues inside them);
     * so the caller needs to update the property values in it's parent. Most types will just return [someJSONToBeSentToServer, newClientData].
     */
    public convertFromClientToServer(newClientData: any,
            typeOfData: IType<any>,
            oldClientData: any,
            propertyContext: IPropertyContext): [any, any] {

        let ret: [any, any] | null;
        if (typeOfData) ret = typeOfData.fromClientToServer(newClientData, oldClientData, propertyContext);
        else {
            // this should rarely or never happen... but do our best to not fail sending (for example due to Date
            // instances that can't be stringified via standard JSON)
            if (! this.lookedUpObjectType) this.lookedUpObjectType = this.typesRegistry.getAlreadyRegisteredType(ObjectType.TYPE_NAME);

            if (this.lookedUpObjectType) ret = this.lookedUpObjectType.fromClientToServer(newClientData, oldClientData, propertyContext);
            else { // 'object' type not found?! it should always be there - no default conversion can be done...
                this.log.error('"object" type was not registered (c->s default conversion) for.');
                ret = [newClientData, newClientData];
            }
        }

        if (!ret) ret = [null, newClientData]; // keep value unchanged, null JSON to send to server
        else if (ret[0] === undefined) ret[0] = null; // JSON does not allow undefined; use null instead...

        return ret;
    };

    /**
     * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
     * return obj[a][b][c] making sure that if any does not exist or is null (for example b) it will be set to {}.
     */
    public getOrCreateInDepthProperty(...args: any[]) {
        if (args.length === 0) return undefined;

        let ret = args[0];
        if (ret === undefined || ret === null || args.length === 1) return ret;
        for (let i = 1; i < args.length; i++) {
            const p = ret;
            ret = ret[args[i]];
            if (ret === undefined || ret === null) {
                ret = {};
                p[args[i]] = ret;
            }
        }
        return ret;
    }

    /**
     * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
     * return obj[a][b][c] making sure that if any does not exist or is null it will just return null/undefined instead of erroring out.
     */
    public getInDepthProperty(...args: any[]) {
        if (args.length === 0) return undefined;

        let ret = args[0];
        if (ret === undefined || ret === null || args.length === 1) return ret;

        for (let i = 1; i < args.length; i++) {
            ret = ret[args[i]];
            if (ret === undefined || ret === null) {
                return i === args.length - 1 ? ret : undefined;
            }
        }

        return ret;
    }

    public getEventArgs(args, eventName: string, handlerSpecification: IWebObjectFunction) {
        const newargs = [];
        for (const i of args) {
            let arg = args[i];
            if (arg && arg.originalEvent) arg = arg.originalEvent;

            // TODO these two ifs could be moved to a "JSEvent" client side type implementation (and if spec is correct it will work through the normal types system)
            if (arg instanceof MouseEvent || arg instanceof KeyboardEvent) {
                const $event = arg;
                const eventObj = {};
                let modifiers = 0;
                if ($event.shiftKey) modifiers = modifiers || SwingModifiers.SHIFT_MASK;
                if ($event.metaKey) modifiers = modifiers || SwingModifiers.META_MASK;
                if ($event.altKey) modifiers = modifiers || SwingModifiers.ALT_MASK;
                if ($event.ctrlKey) modifiers = modifiers || SwingModifiers.CTRL_MASK;

                eventObj['type'] = 'event';
                eventObj['eventName'] = eventName;
                eventObj['modifiers'] = modifiers;
                eventObj['timestamp'] = new Date().getTime();
                eventObj['x'] = $event['pageX'];
                eventObj['y'] = $event['pageY'];
                eventObj['data'] = $event['data'];
                arg = eventObj;
            } else if (arg instanceof Event) {
                const eventObj = {};
                eventObj['type'] = 'event';
                eventObj['eventName'] = eventName;
                eventObj['timestamp'] = new Date().getTime();
                eventObj['data'] = arg['data'];
                arg = eventObj;
            } else arg = this.convertFromClientToServer(arg, handlerSpecification?.getArgumentType(i), undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES)[0];

            newargs.push(arg);
        }
        return newargs;
    }
}

export function isChanged(now: any, prev: any) {
    if (instanceOfChangeAwareValue(now)) {
        return now.getInternalState().hasChanges();
    }

    if (now === prev) return false;
    if (now && prev) {
        if (now instanceof Array) {
            if (prev instanceof Array) {
                if (now.length !== prev.length) return true;
            } else {
                return true;
            }
        }
        if (now instanceof Date) {
            if (prev instanceof Date) {
                return now.getTime() !== prev.getTime();
            }
            return true;
        }

        if ((now instanceof Object) && (prev instanceof Object)) {
            // first build up a list of all the properties both have.
            const fulllist = this.getCombinedPropertyNames(now, prev);
            for (const prop in fulllist) {
                // TODO I think this comment line below can be removed - as ng2/new Angular I think no longer needs this check...
                // ng repeat creates a child scope for each element in the array any scope has a $$hashKey property which must be ignored since it is not part of the model
                if (prev[prop] !== now[prop]) {
                    if (prop === '$$hashKey') continue; // TODO I think this line can be removed - as ng2/new Angular I think no longer needs this check...
                    if (typeof now[prop] === 'object') {
                        if (isChanged(now[prop], prev[prop])) {
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

export const instanceOfChangeAwareValue = (obj: any): obj is IChangeAwareValue =>
    obj != null && obj.getInternalState instanceof Function;

/**
 * @param doNotPushNow if this is true, then the change notification is not meant to trigger the push to server right away (but just notify that there are changes
 *                  in case the implementor needs to know that). Otherwise, if it is false of undefined, it should trigger an actual push to server as well.
 */
export type ChangeListenerFunction = ((doNotPushNow?: boolean) => void);

export interface IChangeAwareValue {

    /** do not call this methods from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState(): ChangeAwareState;
}

export class ChangeAwareState {

    public allChanged = false;

    private changeListener: ChangeListenerFunction;
//    private inNotify = false;

    /**
     * @param doNotPushNow if this is true, then the change notification is not meant to trigger the push to server right away (but just notify that there are changes
     *                  in case the implementor needs to know that). Otherwise, if it is false of undefined, it should trigger an actual push to server as well.
     */
    markAllChanged(notifyListener: boolean, doNotPushNow?: boolean): void {
        this.allChanged = true;
        if (notifyListener) this.notifyChangeListener(doNotPushNow);
    }

    hasChanges(): boolean {
        return this.allChanged/* || this.inNotify*/;
    }

    clearChanges() {
        this.allChanged = false;
    }

    setChangeListener(callback: ChangeListenerFunction): void {
        this.changeListener = callback;
    }

    /**
     * @param doNotPushNow if this is true, then the change notification is not meant to trigger the push to server right away (but just notify that there are changes
     *                  in case the implementor needs to know that). Otherwise, if it is false of undefined, it should trigger an actual push to server as well.
     */
    public notifyChangeListener(doNotPushNow?: boolean): void {
//        this.inNotify = true;
        if (this.changeListener) this.changeListener(doNotPushNow);
//        this.inNotify = false;
    }

}

/**
 * This interface contains methods that SubpropertyChangeByReferenceHandler needs to trigger in parent of changed subproperties based on what happened.
 */
export interface IParentAccessForSubpropertyChanges<KeyT> {

    shouldIgnoreChangesBecauseFromOrToServerIsInProgress(): boolean;

    /**
     * @param doNotPushNow if this is true, then the change notification is not meant to trigger the push to server right away (but just notify that there are changes
     *                  in case the implementor needs to know that). Otherwise, if it is false of undefined, it should trigger an actual push to server as well.
     */
    changeNeedsToBePushedToServer(key: KeyT, oldValue: any, doNotPushNow?: boolean): void;

}

/**
 * Class that is meant to handle subproperty changes by reference (so when a full reference is replaced in a custom object/array/component model) for subproperties
 * with pushToServer ALLOW (without pushing it right away), SHALLOW or DEEP.
 */
export class SubpropertyChangeByReferenceHandler {

    constructor(public readonly parentAccess: IParentAccessForSubpropertyChanges<number | string>) {}

    public setPropertyAndHandleChanges(parentValue: any, propertyName: number | string, value: any, doNotPushNow?: boolean) {
        const oldValue = parentValue[propertyName];
        parentValue[propertyName] = value;

        // if the value of this property is changed, mark it as such and notify if needed
        this.markIfChanged(propertyName, value, oldValue, doNotPushNow);

        // unregister as listener to old value if needed
        this.setChangeListenerToSubValueIfNeeded(oldValue, undefined);

        // register as listener to new value if needed
        this.setChangeListenerToSubValueIfNeeded(parentValue[propertyName] /* markIfChanged call above might have upated it again due to clientToServer conversion for array/custom
                                                                                        obj returning new proxy for client side created values; so read it again instead of using "value"*/,
            (dontPushNow?: boolean) => {
                this.parentAccess.changeNeedsToBePushedToServer(propertyName, parentValue[propertyName], dontPushNow);
            });
    }

    protected markIfChanged(propertyName: number | string, newObject: any, oldObject: any, doNotPushNow?: boolean) {
        if (this.testChanged(newObject, oldObject)) {
            this.parentAccess.changeNeedsToBePushedToServer(propertyName, oldObject, doNotPushNow);

            return true;
        }
        return false;
    }

    private setChangeListenerToSubValueIfNeeded(value: any, changeListener: ChangeListenerFunction): void {
        if (instanceOfChangeAwareValue(value)) {
            // child is able to handle it's own change mechanism
            value.getInternalState().setChangeListener(changeListener);
        }
    }

    private testChanged(newObject: any, oldObject: any) {
        if (newObject !== oldObject) {
            // this value has changed by reference; so it needs to be fully sent to server
            if (instanceOfChangeAwareValue(newObject)) {
                newObject.getInternalState().markAllChanged(false);
                return true;
            } else return isChanged(newObject, oldObject); // dumb content; different reference - see if by any chance it's the same value and doesn't need to be sent anyway
        }
        if (typeof newObject == 'object') {
            if (instanceOfChangeAwareValue(newObject)) {
                return newObject.getInternalState().hasChanges();
            } else return false; // old and new are same ref so no changes
        }
        return false;
    }

}
