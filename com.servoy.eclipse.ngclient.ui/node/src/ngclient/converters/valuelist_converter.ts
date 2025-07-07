import { IChangeAwareValue, ChangeAwareState, IUIDestroyAwareValue } from '../../sablo/converter.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred, IValuelist } from '@servoy/public';
import { Observable, of, from } from 'rxjs';
import { IType, IPropertyContext } from '../../sablo/types_registry';

export class ValuelistType implements IType<Valuelist> {

    public static readonly TYPE_NAME = 'valuelist';

    constructor(private sabloDeferHelper: SabloDeferHelper) {
    }

    fromServerToClient(serverJSONValue: IValuelistTValueFromServer,
        currentClientValue: Valuelist, _propertyContext: IPropertyContext): Valuelist {

        // TODO it seems that the valuelist type server side never sends type info, just a "realValueAreDates" boolean that is used only in NG2
        // (it either writes default to JSON a value in case of a response to 'getDisplayValue' or a full valuelist
        // generated as java array/map with default to JSON conversion); but it always changes Date to String in
        // it's impl. before applying default to JSON conversions so no client side type info is needed then; so client side here as well it will not apply
        // any server-to-client conversion on values, not even for dates - because they are already strings... changing it so that it works properly with
        // Date conversions to generate actual Date values for entries might break existing components / formatters? though because they now expect strings instead of dates

        let newValue: Valuelist = currentClientValue;
        let deferredValue: any;

        if (serverJSONValue) {
            // because we reuse directly what we get from server serverJSONValue.values and because valuelists can be foundset linked (forFoundset in .spec)
            // but not actually bound to records (for example custom valuelist),
            // it is possible that foundsetLinked.js generates the whole viewport of the foundset using the same value comming from the server => this conversion will be called multiple times
            // with the same serverJSONValue so serverJSONValue.values might already be initialized... so skip it then
            if (serverJSONValue.values) {
                newValue = serverJSONValue.vl; // this .vl is assigned below - for the situation described in long comment above
                if (!newValue) {
                    // initialize
                    const internalState = new ValuelistState();
                    if (currentClientValue && currentClientValue.getInternalState()) {
                        this.sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(internalState, currentClientValue.getInternalState());
                    } else {
                        this.sabloDeferHelper.initInternalStateForDeferring(internalState, 'svy valuelist * ');
                    }

                    // caching this value means for this specific valuelist instance that the display value will not be updated if that would be changed on the server end..
                    internalState.realToDisplayCache = (currentClientValue && currentClientValue.getInternalState()) ?
                        currentClientValue.getInternalState().realToDisplayCache : internalState.realToDisplayCache;
                    internalState.valuelistid = serverJSONValue.valuelistid;
                    internalState.hasRealValues = serverJSONValue.hasRealValues;
                    if (serverJSONValue.realValueType === 'Date' || serverJSONValue.displayValueType === 'Date') {
                        serverJSONValue.values.forEach(element => {
                            if (serverJSONValue.realValueType === 'Date' && element.realValue) element.realValue = new Date(element.realValue);
                            if (serverJSONValue.displayValueType === 'Date' && element.displayValue) element.displayValue = new Date(element.displayValue);
                        });
                    }
                    newValue = new Valuelist(this.sabloDeferHelper, serverJSONValue.realValueType, internalState, serverJSONValue.values);
                    serverJSONValue.vl = newValue;
                    deferredValue = newValue;
                }
            } else if (serverJSONValue.getDisplayValue) {
                // this is the GETDISPLAYVALUE
                newValue = currentClientValue;
                deferredValue = serverJSONValue.getDisplayValue;
            }

            // if we have a deferred filter request, notify that the new value has arrived
            if (serverJSONValue.handledID) {
                const handledIDAndState = serverJSONValue.handledID; // { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
                this.sabloDeferHelper.resolveDeferedEvent(handledIDAndState.id, newValue.getInternalState(), handledIDAndState.value ?
                    deferredValue : 'No value for ' + handledIDAndState.id, handledIDAndState.value);
            }
        } else {
            newValue = null;
            const oldInternalState = currentClientValue ? currentClientValue.getInternalState() : undefined; // internal state / $sabloConverters interface
            if (oldInternalState)
                this.sabloDeferHelper.cancelAll(oldInternalState);
        }
        return newValue;
    }

    fromClientToServer(newClientData: Valuelist, _oldClientData: Valuelist, _propertyContext: IPropertyContext): [any, Valuelist] | null {
        if (newClientData) {
            const newDataInternalState = newClientData.getInternalState();
            if (newDataInternalState.filterStringReq) {
                const tmp = newDataInternalState.filterStringReq;
                delete newDataInternalState.filterStringReq;
                return [tmp, newClientData];
            }
            if (newDataInternalState.diplayValueReq) {
                const tmp = newDataInternalState.diplayValueReq;
                delete newDataInternalState.diplayValueReq;
                return [tmp, newClientData];
            }
        }
        return null; // should never happen
    }

}

class ValuelistState extends ChangeAwareState implements IDeferedState {
    public realToDisplayCache = new Map<string, Observable<any>>();
    public valuelistid: number;
    public filterStringReq: { filter: string; id: number };
    public diplayValueReq: { getDisplayValue: string; id: number };
    public hasRealValues: boolean;

    deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } };
    timeoutRejectLogPrefix: string;

    init(deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } }, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }

    hasChanges(): boolean {
        return super.hasChanges() || this.diplayValueReq !== undefined || this.filterStringReq !== undefined;
    }
    
    clearChanges(): void {
        super.clearChanges();
        this.diplayValueReq = this.filterStringReq = undefined;
    }

}

export class Valuelist extends Array<{ displayValue: string; realValue: any }> implements IValuelist, IChangeAwareValue, IUIDestroyAwareValue {

    constructor(private sabloDeferHelper: SabloDeferHelper, private realValueType: string,
        private internalState: ValuelistState, values?: Array<{ displayValue: string; realValue: any }>) {
        super();
        if (values) this.push(...values);
        // see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        // set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(Valuelist.prototype));
    }

    isRealValueDate(): boolean {
        return this.realValueType === 'Date';
    }

    isRealValueUUID(): boolean {
        return this.realValueType === 'UUID';
    }
    
    /**
     * This is meant for internal use only; do not call this in component code.
     */
    getInternalState(): ValuelistState {
        return this.internalState;
    }

    uiDestroyed(): void{
        this.sabloDeferHelper.cancelAll(this.getInternalState());
        this.internalState.realToDisplayCache.clear();
    }

    filterList(filterString: string): Observable<any> {
        // only block once
        this.internalState.filterStringReq = {
            filter: filterString,
            id: this.sabloDeferHelper.getNewDeferId(this.internalState)
        };
        const promise = this.internalState.deferred[this.internalState.filterStringReq.id].defer.promise;
        this.internalState.notifyChangeListener();
        return from(promise);
    }

    hasRealValues(): boolean {
        return this.internalState.hasRealValues;
    }

    getDisplayValue(realValue: any): Observable<any> {
        if (realValue != null && realValue !== undefined) {
            if (this.internalState.valuelistid === undefined) {
                return of(realValue);
            } else {
                const key = realValue + '';
                let promiseOrValue = this.internalState.realToDisplayCache[key];
                if (promiseOrValue !== undefined) {
                    if (promiseOrValue instanceof Promise) 
                        return from(promiseOrValue);
                    return of(promiseOrValue);
                }
                this.internalState.diplayValueReq = {
                    getDisplayValue: realValue,
                    id: this.sabloDeferHelper.getNewDeferId(this.internalState)
                };
                this.internalState.realToDisplayCache[key] = this.internalState.deferred[this.internalState.diplayValueReq.id].defer.promise.then((val) => {
                    this.internalState.realToDisplayCache[key] = val;
                    return val;
                }).catch(() => {
                     delete this.internalState.realToDisplayCache[key];
                });

                this.internalState.notifyChangeListener();
                promiseOrValue = this.internalState.realToDisplayCache[key];
                 if (promiseOrValue instanceof Promise) 
                    return from(promiseOrValue);
                return of(promiseOrValue); 
            }
        }
        // the real value == null return a promise like function so that not constantly promises are made.
        return of('');
    }
}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal interface. Do not use. */
export interface IValuelistTValueFromServer {
    values?: Array<{ displayValue: any; realValue: any }>;
    vl?: Valuelist;
    valuelistid?: number;
    hasRealValues?: boolean;
    realValueType?: string;
    displayValueType?: string;
    getDisplayValue?: any;
    id?: number;
    handledID?: { id: number; value: any };
}