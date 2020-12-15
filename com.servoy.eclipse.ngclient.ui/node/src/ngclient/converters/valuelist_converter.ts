import { Injectable } from '@angular/core';
import { IConverter, ConverterService, PropertyContext } from '../../sablo/converter.service';
import { IValuelist, IChangeAwareValue, ChangeAwareState } from '../../sablo/spectypes.service';
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred } from '../../sablo/util/deferred';
import { Observable, of, from } from 'rxjs';

@Injectable()
export class ValuelistConverter implements IConverter {

  public static readonly FILTER = 'filter';
  private static readonly HANDLED = 'handledID';
  private static readonly VALUE_KEY = 'value';
  private static readonly DISPLAYVALUE = 'getDisplayValue';


  constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper) {
  }

  fromServerToClient(serverJSONValue:
        {
            values?: Array<{ displayValue: any; realValue: any }>;
            vl?: Valuelist;
            valuelistid?: string;
            hasRealValues?: boolean;
            realValueAreDates?: boolean;
            displayValueAreDates?: boolean;
            getDisplayValue?: string;
            id?: number;
            handledID?: {id: number};
        },
        currentClientValue?: Valuelist, _propertyContext?: PropertyContext): IValuelist {
    let newValue: Valuelist = currentClientValue;
    let state: ValuelistState = null;
    let deferredValue: any;

    if (serverJSONValue) {

      // because we reuse directly what we get from server serverJSONValue.values and because valuelists can be foundset linked (forFoundset in .spec)
      // but not actually bound to records (for example custom valuelist),
      // it is possible that foundsetLinked.js generates the whole viewport of the foundset using the same value comming from the server => this conversion will be called multiple times
      // with the same serverJSONValue so serverJSONValue.values might already be initialized... so skip it then
      if (serverJSONValue.values) {
        newValue = serverJSONValue.vl;
        if (!newValue) {
          // initialize
          //                $sabloConverters.prepareInternalState(newValue); - no need
          state = new ValuelistState();
          if (currentClientValue && currentClientValue.state) {
            this.sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(state, currentClientValue.state);
          } else {
            this.sabloDeferHelper.initInternalStateForDeferring(state, 'svy valuelist * ');
          }

          // caching this value means for this specific valuelist instance that the display value will not be updated if that would be changed on the server end..
          state.realToDisplayCache = (currentClientValue && currentClientValue.state) ?
            currentClientValue.state.realToDisplayCache : state.realToDisplayCache;
          state.valuelistid = serverJSONValue.valuelistid;
          state.hasRealValues = serverJSONValue.hasRealValues;
          if (serverJSONValue.realValueAreDates || serverJSONValue.displayValueAreDates) {
            serverJSONValue.values.forEach(element => {
              if (serverJSONValue.realValueAreDates && element.realValue) element.realValue = new Date(element.realValue);
              if (serverJSONValue.displayValueAreDates && element.displayValue) element.displayValue = new Date(element.displayValue);
            });
          }
          newValue = new Valuelist(this.sabloService, this.sabloDeferHelper, serverJSONValue.realValueAreDates, state, serverJSONValue.values);
          serverJSONValue.vl = newValue;
          deferredValue = newValue;
        }
      } else if (serverJSONValue.getDisplayValue) {
        // this is the GETDISPLAYVALUE
        newValue = currentClientValue;
        state = currentClientValue.state;
        deferredValue = serverJSONValue.getDisplayValue;
      }

      // if we have a deferred filter request, notify that the new value has arrived
      if (serverJSONValue.handledID) {
        const handledIDAndState = serverJSONValue.handledID; // { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
        const defer: Deferred<any> = this.sabloDeferHelper.retrieveDeferForHandling(handledIDAndState.id, newValue.state);
        if (defer) {
          if (handledIDAndState[ValuelistConverter.VALUE_KEY]) {
            defer.resolve(deferredValue);
          } else
            defer.reject('No value for ' + handledIDAndState.id);
        }
      }
    } else {
      newValue = null;
      const oldInternalState = currentClientValue ? currentClientValue.state : undefined; // internal state / $sabloConverters interface
      if (oldInternalState)
        this.sabloDeferHelper.cancelAll(oldInternalState);
    }
    return newValue;
  }

  fromClientToServer(newClientData: Valuelist, oldClientData?) {
    if (newClientData) {
      const newDataInternalState = newClientData.state;
      if (newDataInternalState.filterStringReq) {
        const tmp = newDataInternalState.filterStringReq;
        delete newDataInternalState.filterStringReq;
        return tmp;
      }
      if (newDataInternalState.diplayValueReq) {
        const tmp = newDataInternalState.diplayValueReq;
        delete newDataInternalState.diplayValueReq;
        return tmp;
      }
    }
    return null; // should never happen

  }
}

class ValuelistState extends ChangeAwareState implements IDeferedState {
  public realToDisplayCache = new Map<string, Observable<object>>();
  public valuelistid: string;
  public filterStringReq: {filter: string; id: number};
  public diplayValueReq: {getDisplayValue: string; id: number};
  public hasRealValues: boolean;

  deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } };
  currentMsgId: number;
  timeoutRejectLogPrefix: string;

  init(deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } }, currentMsgId: number, timeoutRejectLogPrefix: string) {
    this.deferred = deferred;
    this.currentMsgId = currentMsgId;
    this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
  }

  public isChanged() {
    return this.filterStringReq || this.diplayValueReq;
  }
}

export class Valuelist extends Array<{ displayValue: string; realValue: object }> implements IValuelist, IChangeAwareValue {

  constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, private realValueIsDate: boolean,
              public state: ValuelistState, values?: Array<{ displayValue: string; realValue: object }>) {
    super();
    if (values) this.push(...values);
    // see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
    // set prototype, since adding a create method is not really working if we have the values
    Object.setPrototypeOf(this, Object.create(Valuelist.prototype));
  }

  isRealValueDate(): boolean {
    return this.realValueIsDate;
  }

  getStateHolder(): ChangeAwareState {
    return this.state;
  }

  filterList(filterString: string): Observable<any> {
    // only block once
    this.state.filterStringReq = {
        filter: filterString,
        id:this.sabloDeferHelper.getNewDeferId(this.state)
    };
    const promise = this.state.deferred[this.state.filterStringReq.id].defer.promise;
    this.state.notifyChangeListener();
    return from(promise);
  }

  hasRealValues(): boolean {
    return this.state.hasRealValues;
  }

  getDisplayValue(realValue: any): Observable<any> {
    if (realValue != null && realValue !== undefined) {
      if (this.state.valuelistid === undefined) {
        return of(realValue);
      } else {
        const key = realValue + '';
        if (this.state.realToDisplayCache[key] !== undefined) {
          return from(this.state.realToDisplayCache[key]);
        }
        this.state.diplayValueReq = {
            getDisplayValue: realValue,
            id: this.sabloDeferHelper.getNewDeferId(this.state)
        };
        this.state.realToDisplayCache[key] = this.state.deferred[this.state.diplayValueReq.id].defer.promise.then( (val)  => {
          this.state.realToDisplayCache[key] = val;
          return val;
        });

        this.state.notifyChangeListener();
        return from(this.state.realToDisplayCache[key]);
      }
    }
    // the real value == null return a promise like function so that not constantly promises are made.
    return of('');
  }
}
