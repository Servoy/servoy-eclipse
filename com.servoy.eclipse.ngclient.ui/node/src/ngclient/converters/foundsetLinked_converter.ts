import { Injectable } from '@angular/core';
import { IConverter, ConverterService } from '../../sablo/converter.service'
import { LoggerService, LoggerFactory } from '../../sablo/logger.service'
import { IFoundset, ChangeListener, ChangeEvent, FoundsetTypeConstants, FoundsetLinkedTypeConstants } from '../../sablo/spectypes.service'
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred } from '../../sablo/util/deferred';
import { SabloUtils } from '../../sablo/websocket.service';
import { ViewportService } from '../services/viewport.service';


@Injectable()
export class FoundsetLinkedConverter implements IConverter {
    
    public static readonly SINGLE_VALUE = "sv";
    public static readonly SINGLE_VALUE_UPDATE = "svu";
    public static readonly VIEWPORT_VALUE = "vp";
    public static readonly VIEWPORT_VALUE_UPDATE = "vpu";
    public static readonly CONVERSION_NAME = "fsLinked";
    public static readonly PROPERTY_CHANGE = "propertyChange";
    public static readonly PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched

     
    private log: LoggerService;

    constructor(private converterService: ConverterService, private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, private viewportService: ViewportService, private logFactory:LoggerFactory) {
        this.log = logFactory.getLogger("FoundsetLinkedPropertyValue");
    }
    
    public fromServerToClient(serverJSONValue, currentClientValue, propertyContext) {
        let newValue : FoundsetLinked = currentClientValue;

        // remove watches to avoid an unwanted detection of received changes
        //TODO no watches anymore!!! removeAllWatches(currentClientValue);

        if (serverJSONValue !== null && serverJSONValue !== undefined) {
            let didSomething = false;
            let internalState = newValue !== undefined ? newValue.state : undefined;
            if (internalState == undefined) {
                newValue = new FoundsetLinked();
                newValue.state.setChangeListeners(currentClientValue);
                internalState = newValue.state;
            }

            if (serverJSONValue[FoundsetTypeConstants.FOR_FOUNDSET_PROPERTY] !== undefined) {
                // the foundset that this property is linked to; keep that info in internal state; viewport.js needs it
                let forFoundsetPropertyName = serverJSONValue[FoundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
                internalState.forFoundset = () => { return propertyContext(forFoundsetPropertyName); };
                didSomething = true;
            }

            if (typeof serverJSONValue[FoundsetLinkedConverter.PUSH_TO_SERVER] !== 'undefined') {
                internalState.push_to_server = serverJSONValue[FoundsetLinkedConverter.PUSH_TO_SERVER];
            }

            let childChangedNotifier;
            
            if (serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE] !== undefined) {
                internalState.singleValueState = undefined;
                internalState.recordLinked = true;
                
                this.viewportService.updateViewportGranularly(newValue, internalState, serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE],
                        this.converterService.getInDepthProperty(serverJSONValue, ConverterService.TYPES_KEY, FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE),
                         propertyContext, true, null); //TODO row prototype??
                this.log.spam("svy foundset linked * firing change listener: granular updates...");
                internalState.fireChanges(serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE]);
            } else {
                // the rest will always be treated as a full viewport update (single values are actually going to generate a full viewport of 'the one' new value)
                let conversionInfos;
                let updateWholeViewportFunc : Function = this.getUpdateWholeViewportFunc(propertyContext);
                
                let wholeViewport;
                if (serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] !== undefined || serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE_UPDATE] !== undefined) {
                    // just update single value from server and make copies of it to duplicate
                    let conversionInfo = this.converterService.getInDepthProperty(serverJSONValue, ConverterService.TYPES_KEY, serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] !== undefined ? FoundsetLinkedConverter.SINGLE_VALUE : FoundsetLinkedConverter.SINGLE_VALUE_UPDATE);
                    let singleValue =serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] !== undefined ? serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] : serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE_UPDATE];
                    internalState.singleValueState = {};
                    internalState.singleValueState.updateWholeViewport = updateWholeViewportFunc;
                    wholeViewport = this.handleSingleValue(singleValue, internalState, conversionInfo);
                    conversionInfos = internalState.singleValueState.conversionInfos;
                    // addBackWatches below (end of function) will add a watch for foundset prop. size to regenerate the viewport when that changes - fill it up with single values
                } else if (serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE] !== undefined) {
                    internalState.singleValueState = undefined;
                    internalState.recordLinked = true;
                    
                    wholeViewport = serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE];
                    conversionInfos = this.converterService.getInDepthProperty(serverJSONValue, ConverterService.TYPES_KEY, FoundsetLinkedConverter.VIEWPORT_VALUE);
                }
                
                if (wholeViewport !== undefined) updateWholeViewportFunc(newValue, internalState, wholeViewport, conversionInfos);
                else if (!didSomething) this.log.error(this.log.buildMessage(() => "Can't interpret foundset linked prop. server update correctly: " + JSON.stringify(serverJSONValue, undefined, 2)));
            }
        }
        
        if (serverJSONValue[FoundsetLinkedTypeConstants.ID_FOR_FOUNDSET] === null) {
            if (newValue.idForFoundset !== undefined) newValue.idForFoundset = undefined;
        } else if (serverJSONValue[FoundsetLinkedTypeConstants.ID_FOR_FOUNDSET] !== undefined) {
            newValue.idForFoundset = serverJSONValue[FoundsetLinkedTypeConstants.ID_FOR_FOUNDSET];
        }
        
        // restore/add model watch
        //TODO no watches to add back..... addBackWatches(newValue, componentScope);
        return newValue;
    }
    
    public fromClientToServer(newClientData, oldClientData) {
        if (newClientData) {
            let internalState : FoundsetLinkedState = newClientData.state;
            if (internalState.isChanged()) {
                if (!internalState.recordLinked) {
                    // we don't need to send rowId to server in this case; we just need value
                    for (let index in internalState.requests) {
                        internalState.requests[index][FoundsetLinkedConverter.PROPERTY_CHANGE] = internalState.requests[index].viewportDataChanged.value;
                        delete internalState.requests[index].viewportDataChanged;
                    }
                }
                let tmp = internalState.requests;
                internalState.requests = [];
                return tmp;
            }
        }
        return [];
    }
    
    private getUpdateWholeViewportFunc(propertyContext) {
        return (propValue: FoundsetLinked, internalState: FoundsetLinkedState, wholeViewport, conversionInfos) => {
            let viewPortHolder = { "tmp" : propValue };
            this.viewportService.updateWholeViewport(viewPortHolder, "tmp", internalState, wholeViewport, conversionInfos, propertyContext);
            
            // updateWholeViewport probably changed "tmp" reference to value of "wholeViewport"...
            // update current value reference because that is what is present in the model
            propValue.splice(0, propValue.length);
            let tmp = viewPortHolder["tmp"];
            for (let tz = 0; tz < tmp.length; tz++) propValue.push(tmp[tz]);
            
            if (propValue && internalState && internalState.changeListeners.length > 0) {
                let notificationParamForListeners : ChangeEvent;
                notificationParamForListeners[FoundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue: propValue, newValue: propValue }; // should we not set oldValue here? old one has changed into new one so basically we do not have old content anymore...
                
                this.log.spam("svy foundset linked * firing change listener: full viewport changed...");
                // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
                internalState.fireChanges(notificationParamForListeners);
            }
        }
    }
    
    private handleSingleValue(singleValue, internalState: FoundsetLinkedState, conversionInfo) {
        // this gets called for values that are not actually record linked, and we 'fake' a viewport containing the same value on each row in the array
        internalState.recordLinked = false;
        
        // *** BEGIN we need the following in addBackWatches that is also called by updateAngularScope, that is why they are stored in internalState (iS)
        internalState.singleValueState.generateWholeViewportFromOneValue = (internalState, vpSize) => {
            if (vpSize === undefined) vpSize = 0;
            let wholeViewport = [];
            internalState.singleValueState.conversionInfos = conversionInfo ? [] : undefined; 
            
            for (let index = vpSize - 1; index >= 0; index--) {
                wholeViewport.push(singleValue);
                if (conversionInfo) internalState.singleValueState.conversionInfos.push(conversionInfo);
            }
            return wholeViewport;
        }
        internalState.singleValueState.initialVPSize = this.converterService.getInDepthProperty(internalState.forFoundset(), "viewPort", "size");
        // *** END
        
        return internalState.singleValueState.generateWholeViewportFromOneValue(internalState, internalState.singleValueState.initialVPSize);
    }
}

class FoundsetLinked extends Array<Object> {
    state : FoundsetLinkedState;
    idForFoundset : string;

    constructor(){
        super();
        this.state = new FoundsetLinkedState();
        //see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        //set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(FoundsetLinked.prototype));
    }
}

//TODO is this really needed?? or we can reuse the fs state class??
class FoundsetLinkedState {
    
   
    changeListeners: Array<ChangeListener>;//TODO check
    requests = [];
    conversionInfo = [];
    singleValueState = undefined;
    changeNotifier: Function;
    selectionUpdateDefer: Deferred<any>;
    forFoundset: Function;
    recordLinked: boolean = false;
    push_to_server: string = undefined; //TODO check
    
    constructor() {}
    
    public addChangeListener(listener: (change: ChangeEvent) => void) {
        this.changeListeners.push(listener);
        return () => this.removeChangeListener(listener);
    }
    
    public removeChangeListener(listener) {
        let index = this.changeListeners.indexOf(listener);
        if (index > -1) {
            this.changeListeners.splice(index, 1);
        }
    }
    
    public setChangeListeners(currentClientValue)
    {
        this.changeListeners = currentClientValue && currentClientValue.state ? currentClientValue.state.changeListeners : [];
    }
    
    public fireChanges(foundsetChanges: ChangeEvent) {
        for(let i = 0; i < this.changeListeners.length; i++) {
            //TODO needed?? what is componentScope? $webSocket.setIMHDTScopeHintInternal(componentScope);
            this.changeListeners[i](foundsetChanges);
            //TODO $webSocket.setIMHDTScopeHintInternal(undefined);
        }
    }
    
    public setChangeNotifier(changeNotifier) {
        this.changeNotifier = changeNotifier;
    }
    
    public isChanged() {
        return this.requests && (this.requests.length > 0);
    }
}