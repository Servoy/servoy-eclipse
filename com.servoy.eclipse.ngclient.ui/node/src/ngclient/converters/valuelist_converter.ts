import { Injectable } from '@angular/core';
import { IConverter, ConverterService } from '../../sablo/converter.service'
import { IValuelist } from '../../sablo/spectypes.service'
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred } from '../../sablo/util/deferred';

@Injectable()
export class ValuelistConverter implements IConverter {
    
    public static readonly FILTER = "filter";
    private static readonly HANDLED = "handledID";
    public static readonly ID_KEY = "id";
    private static readonly VALUE_KEY = "value";
    
    
    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper) {
    }
     
    fromServerToClient( serverJSONValue, currentClientValue?: Valuelist) : IValuelist {
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
    public realToDisplayCache = new Map();
    public valuelistid: string;
    public filterStringReq: Object;
    public changeNotifier: Function;
    public hasRealValues: boolean;
    
    deferred: Object;
    currentMsgId: number;
    timeoutRejectLogPrefix: string;
    
    setChangeNotifier(changeNotifier) {
      this.changeNotifier = changeNotifier;
    }
    
    isChanged() : boolean {
        return this.filterStringReq !== undefined; 
    }
    
    init(deferred: Object, currentMsgId: number, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.currentMsgId = currentMsgId;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }
}

export class Valuelist extends Array<Object> implements IValuelist {
   
    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, public state: ValuelistState, values?: Array<Object>) {
        super();
        if (values) this.push(...values);
        //see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        //set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(Valuelist.prototype)); 
    }
    
    filterList(filterString: string) :  Promise<any> {
        // only block once
        this.state.filterStringReq = {};
        this.state.filterStringReq[ValuelistConverter.FILTER] = filterString;
        this.state.filterStringReq[ValuelistConverter.ID_KEY] = this.sabloDeferHelper.getNewDeferId(this.state);
        let promise = this.state.deferred[this.state.filterStringReq[ValuelistConverter.ID_KEY]].defer.promise;
        if (this.state.changeNotifier) this.state.changeNotifier();
        return promise;
    } 
    
    hasRealValues() : boolean{
        return this.state.hasRealValues;
    }
    
    getDisplayValue(realValue:any): Promise<any> {
        if (realValue != null && realValue != undefined) {
            if (this.state.valuelistid == undefined) { 
                return Promise.resolve(realValue);
            }
            else {
                var key = realValue + '';
                if (this.state.realToDisplayCache[key] !== undefined) {
                    // if this is a promise return that.
                    if (this.state.realToDisplayCache[key] && typeof(this.state.realToDisplayCache[key].then) === 'function')
                        return this.state.realToDisplayCache[key]; 
                    // if the value is in the cache then return a promise like object
                    // that has a then function that will be resolved right away when called. So that it is more synch api.
                    return Promise.resolve(this.state.realToDisplayCache[key]);
                }
                var self = this;
                this.state.realToDisplayCache[key] = this.sabloService.callService('formService', 'getValuelistDisplayValue', { realValue: realValue, valuelist: this.state.valuelistid })
                    .then((val) => {
                    self.state.realToDisplayCache[key] = val;
                    return val;
                });
                return this.state.realToDisplayCache[key];
            }
        }
        // the real value == null return a promise like function so that not constantly promises are made.
        return Promise.resolve("");
    }
}