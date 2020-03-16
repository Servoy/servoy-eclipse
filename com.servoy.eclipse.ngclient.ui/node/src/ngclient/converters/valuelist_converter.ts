import { Injectable } from '@angular/core';
import { IConverter, ConverterService } from '../../sablo/converter.service'
import { IValuelist } from '../../sablo/spectypes.service'
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred } from '../../sablo/util/deferred';
import { Observable, of, from } from 'rxjs';

@Injectable()
export class ValuelistConverter implements IConverter {

    public static readonly FILTER = "filter";
    private static readonly HANDLED = "handledID";
    public static readonly ID_KEY = "id";
    private static readonly VALUE_KEY = "value";


    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper) {
    }

    fromServerToClient( serverJSONValue, currentClientValue?: Valuelist, propertyContext?:(propertyName: string)=>any ) : IValuelist {
        let newValue : Valuelist = currentClientValue;
        let state: ValuelistState = null;

        if (serverJSONValue) {

            // because we reuse directly what we get from server serverJSONValue.values and because valuelists can be foundset linked (forFoundset in .spec) but not actually bound to records (for example custom valuelist),
            // it is possible that foundsetLinked.js generates the whole viewport of the foundset using the same value comming from the server => this conversion will be called multiple times
            // with the same serverJSONValue so serverJSONValue.values might already be initialized... so skip it then
            newValue = serverJSONValue.values;
            if (newValue.state === undefined) {
                // initialize
//                $sabloConverters.prepareInternalState(newValue); - no need
                state = new ValuelistState();
                if (currentClientValue && currentClientValue.state)
                {
                    this.sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(state, currentClientValue.state);
                }
                else
                {
                   this.sabloDeferHelper.initInternalStateForDeferring(state, "svy valuelist * ");
                }

                //caching this value means for this specific valuelist instance that the display value will not be updated if that would be changed on the server end..
                state.realToDisplayCache = (currentClientValue && currentClientValue[ConverterService.INTERNAL_IMPL]) ?
                currentClientValue[ConverterService.INTERNAL_IMPL].realToDisplayCache : {};
                state.valuelistid = serverJSONValue.valuelistid;
                state.hasRealValues = serverJSONValue.hasRealValues;
                newValue = new Valuelist(this.sabloService, this.sabloDeferHelper, state, serverJSONValue.values);
            }

            // if we have a deferred filter request, notify that the new value has arrived
            if (serverJSONValue[ValuelistConverter.HANDLED]) {
                var handledIDAndState = serverJSONValue[ValuelistConverter.HANDLED]; // { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
                let defer: Deferred<any> = this.sabloDeferHelper.retrieveDeferForHandling(handledIDAndState[ValuelistConverter.ID_KEY], newValue.state);
                if (defer) {
                    if (handledIDAndState[ValuelistConverter.VALUE_KEY])
                    {
                        defer.resolve(newValue);
                    }
                    else
                        defer.reject('No value for '+handledIDAndState[ValuelistConverter.ID_KEY]);
                    }
                }
        }
        else {
                newValue = null;
                var oldInternalState = currentClientValue ? currentClientValue[ConverterService.INTERNAL_IMPL] : undefined; // internal state / $sabloConverters interface
                if (oldInternalState)
                    this.sabloDeferHelper.cancelAll(oldInternalState);
            }
            return newValue;
    }

    fromClientToServer( newClientData: Valuelist, oldClientData? ) {
        if (newClientData) {
            var newDataInternalState = newClientData.state;
            if (newDataInternalState.isChanged()) {
                var tmp = newDataInternalState.filterStringReq;
                delete newDataInternalState.filterStringReq;
                return tmp;
            }
        }
        return null; // should never happen

    }
}

class ValuelistState implements IDeferedState {
    public realToDisplayCache = new Map<string, Observable<object>>();
    public valuelistid: string;
    public filterStringReq: object;
    public changeNotifier: Function;
    public hasRealValues: boolean;

    deferred: {[key: string]: {defer: Deferred<any>, timeoutId: any}};
    currentMsgId: number;
    timeoutRejectLogPrefix: string;

    setChangeNotifier(changeNotifier) {
      this.changeNotifier = changeNotifier;
    }

    isChanged(): boolean {
        return this.filterStringReq !== undefined;
    }

    init(deferred:  {[key: string]: {defer: Deferred<any>, timeoutId: any}}, currentMsgId: number, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.currentMsgId = currentMsgId;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }
}

export class Valuelist extends Array<{displayValue: string, realValue: object}> implements IValuelist {

    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, public state: ValuelistState, values?: Array<{displayValue: string, realValue: object}>) {
        super();
        if (values) this.push(...values);
        //see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        //set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(Valuelist.prototype));
    }

    filterList(filterString: string): Observable<any> {
        // only block once
        this.state.filterStringReq = {};
        this.state.filterStringReq[ValuelistConverter.FILTER] = filterString;
        this.state.filterStringReq[ValuelistConverter.ID_KEY] = this.sabloDeferHelper.getNewDeferId(this.state);
        const promise = this.state.deferred[this.state.filterStringReq[ValuelistConverter.ID_KEY]].defer.promise;
        if (this.state.changeNotifier) this.state.changeNotifier();
        return from(promise);
    }

    hasRealValues(): boolean{
        return this.state.hasRealValues;
    }

    getDisplayValue(realValue: any): Observable<any> {
        if (realValue != null && realValue !== undefined) {
            if (this.state.valuelistid === undefined) {
                return of(realValue);
            } else {
                const key = realValue + '';
                if (this.state.realToDisplayCache[key] !== undefined) {
                    return this.state.realToDisplayCache[key];
                }
                const self = this;
                this.state.realToDisplayCache[key] = from(this.sabloService.callService('formService', 'getValuelistDisplayValue', { realValue: realValue, valuelist: this.state.valuelistid })
                    .then((val) => {
                    self.state.realToDisplayCache[key] = of(val === null ? '' : val);
                    return val;
                }));
                return this.state.realToDisplayCache[key];
            }
        }
        // the real value == null return a promise like function so that not constantly promises are made.
        return of('');
    }
}
