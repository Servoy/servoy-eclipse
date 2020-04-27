import { Injectable } from '@angular/core';
import { IConverter, ConverterService, PropertyContext } from '../../sablo/converter.service'
import { LoggerService, LoggerFactory } from '../../sablo/logger.service'
import { ChangeListener, ViewportChangeEvent } from '../../sablo/spectypes.service'
import { ViewportService, IViewportConversion } from '../services/viewport.service';
import { FoundsetChangeEvent, Foundset } from '../converters/foundset_converter';


@Injectable()
export class FoundsetLinkedConverter implements IConverter {
    
    private static readonly SINGLE_VALUE = "sv";
    private static readonly SINGLE_VALUE_UPDATE = "svu";
    private static readonly VIEWPORT_VALUE = "vp";
    private static readonly VIEWPORT_VALUE_UPDATE = "vpu";
    private static readonly PROPERTY_CHANGE = "propertyChange";
    private static readonly PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched

    private static readonly FOR_FOUNDSET_PROPERTY: string = 'forFoundset';
    private static readonly ID_FOR_FOUNDSET = "idForFoundset";
     
    private log: LoggerService;

    constructor(private converterService: ConverterService, private viewportService: ViewportService, logFactory:LoggerFactory) {
        this.log = logFactory.getLogger("FoundsetLinkedPropertyValue");
    }
    
    public fromServerToClient(serverJSONValue:object, currentClientValue: FoundsetLinked, propertyContext:PropertyContext) {
        let newValue : FoundsetLinked = currentClientValue;

        if (serverJSONValue !== null && serverJSONValue !== undefined) {
            let didSomething = false;
            let internalState : FoundsetLinkedState = newValue !== undefined ? newValue.state : undefined;
            if (internalState == undefined) {
                newValue = new FoundsetLinked(this.viewportService, this.converterService);
                newValue.state.setChangeListeners(currentClientValue);
                internalState = newValue.state;
            }

            if (serverJSONValue[FoundsetLinkedConverter.FOR_FOUNDSET_PROPERTY] !== undefined) {
                // the foundset that this property is linked to; keep that info in internal state; viewport.js needs it
                let forFoundsetPropertyName = serverJSONValue[FoundsetLinkedConverter.FOR_FOUNDSET_PROPERTY];
                internalState.forFoundset = () => { return propertyContext(forFoundsetPropertyName); };
                didSomething = true;
            }

            if (typeof serverJSONValue[FoundsetLinkedConverter.PUSH_TO_SERVER] !== 'undefined') {
                internalState.push_to_server = serverJSONValue[FoundsetLinkedConverter.PUSH_TO_SERVER];
            }
            
            if (serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE] !== undefined) {
                internalState.singleValueState = undefined;
                internalState.recordLinked = true;
                
                this.viewportService.updateViewportGranularly(newValue, internalState, serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE],
                        this.converterService.getInDepthProperty(serverJSONValue, ConverterService.TYPES_KEY, FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE),
                         propertyContext, true);
                this.log.spam("svy foundset linked * firing change listener: granular updates...");
                internalState.fireChanges(serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE_UPDATE]);
            } else {
                // the rest will always be treated as a full viewport update (single values are actually going to generate a full viewport of 'the one' new value)
                
                let wholeViewport: any[];
                let conversionInfo: any[];
                if (serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] !== undefined || serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE_UPDATE] !== undefined) {
                    // just update single value from server and make copies of it to duplicate
                    conversionInfo = this.converterService.getInDepthProperty(serverJSONValue, ConverterService.TYPES_KEY, serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] !== undefined ? FoundsetLinkedConverter.SINGLE_VALUE : FoundsetLinkedConverter.SINGLE_VALUE_UPDATE);
                    const singleValue = serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] !== undefined ? serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE] : serverJSONValue[FoundsetLinkedConverter.SINGLE_VALUE_UPDATE];
                    internalState.singleValueState = new SingleValueState(this, propertyContext, conversionInfo);
                    wholeViewport = internalState.handleSingleValue(singleValue);
                    const fs: Foundset = internalState.forFoundset();
                    fs.addChangeListener((event: FoundsetChangeEvent) => {
                        if (event.viewPortSizeChanged) {
                            if (event.viewPortSizeChanged.newValue === internalState.singleValueState.viewPortSize) return;
                            internalState.singleValueState.viewPortSize = event.viewPortSizeChanged.newValue;
                            if (event.viewPortSizeChanged.newValue == undefined)  internalState.singleValueState.viewPortSize = 0;
                            wholeViewport = internalState.singleValueState.generateWholeViewportFromOneValue(singleValue)
                            internalState.singleValueState.updateWholeViewport(newValue, internalState, wholeViewport);
                        }
                    });

                } else if (serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE] !== undefined) {
                    internalState.singleValueState = undefined;
                    internalState.recordLinked = true;
                    conversionInfo = this.converterService.getInDepthProperty(serverJSONValue, ConverterService.TYPES_KEY, FoundsetLinkedConverter.VIEWPORT_VALUE);
                    wholeViewport = serverJSONValue[FoundsetLinkedConverter.VIEWPORT_VALUE];
                }
                
                if (wholeViewport !== undefined) this.updateWholeViewport(newValue, internalState, wholeViewport, conversionInfo, propertyContext);
                else if (!didSomething) this.log.error(this.log.buildMessage(() => "Can't interpret foundset linked prop. server update correctly: " + JSON.stringify(serverJSONValue, undefined, 2)));
            }
        }
        
        if (serverJSONValue[FoundsetLinkedConverter.ID_FOR_FOUNDSET] === null) {
            if (newValue.idForFoundset !== undefined) newValue.idForFoundset = undefined;
        } else if (serverJSONValue[FoundsetLinkedConverter.ID_FOR_FOUNDSET] !== undefined) {
            newValue.idForFoundset = serverJSONValue[FoundsetLinkedConverter.ID_FOR_FOUNDSET];
        }
        return newValue;
    }

    public updateWholeViewport(propValue: FoundsetLinked, internalState: FoundsetLinkedState, wholeViewport: any[], conversionInfos: any[], propertyContext: PropertyContext){
        let rows = propValue.viewportService.updateWholeViewport([], internalState, wholeViewport, conversionInfos, propertyContext);
        
        // updateWholeViewport probably changed "rows" reference to value of "wholeViewport"...
        // update current value reference because that is what is present in the model
        propValue.splice(0, propValue.length);
        for (let tz = 0; tz < rows.length; tz++) propValue.push(rows[tz]);
        
        if (propValue && internalState && internalState.changeListeners.length > 0) {
            let notificationParamForListeners : ViewportChangeEvent;
            notificationParamForListeners.viewportRowsCompletelyChanged = { oldValue: propValue, newValue: propValue }; // should we not set oldValue here? old one has changed into new one so basically we do not have old content anymore...
            
            this.log.spam("svy foundset linked * firing change listener: full viewport changed...");
            // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
            internalState.fireChanges(notificationParamForListeners);
        }
    }
    
    public fromClientToServer(newClientData: FoundsetLinked, oldClientData?: any) {
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
}

export class FoundsetLinked extends Array<Object> {
    state : FoundsetLinkedState;
    idForFoundset : string;

    constructor(public viewportService: ViewportService, converterService: ConverterService){
        super();
        this.state = new FoundsetLinkedState(converterService);
        //see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        //set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(FoundsetLinked.prototype));
    }

    public dataChanged(index: number, newValue: any, oldValue?: any) {
        if (this.state.push_to_server == undefined) return; //we ignore all changes

        if (newValue !== undefined) {
            if (this.state.singleValueState && newValue !== oldValue) {
                let wholeViewport =  this.state.handleSingleValue(newValue);	
                if (wholeViewport !== undefined) this.state.singleValueState.updateWholeViewport(this, this.state, wholeViewport);				
            }
            this.viewportService.queueChange(this, this.state, this.state.push_to_server, index, null, newValue, oldValue);
        }
    }
}

class FoundsetLinkedState implements IViewportConversion {
    
    viewportConversions: Record<string, object>[] = [];
    changeListeners: Array<ChangeListener>;
    requests = [];
    conversionInfo = [];
    singleValueState : SingleValueState = undefined;
    changeNotifier: Function;
    forFoundset: () => Foundset;
    recordLinked: boolean = false;
    push_to_server: boolean = undefined;// value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    
    constructor(private converterService: ConverterService) {}
    
    public addChangeListener(listener: (change: ViewportChangeEvent) => void) {
        this.changeListeners.push(listener);
        return () => this.removeChangeListener(listener);
    }
    
    public removeChangeListener(listener: (change: ViewportChangeEvent) => void) {
        let index = this.changeListeners.indexOf(listener);
        if (index > -1) {
            this.changeListeners.splice(index, 1);
        }
    }
    
    public setChangeListeners(currentClientValue: FoundsetLinked)
    {
        this.changeListeners = currentClientValue && currentClientValue.state ? currentClientValue.state.changeListeners : [];
    }
    
    public fireChanges(foundsetChanges: ViewportChangeEvent) {
        for(let i = 0; i < this.changeListeners.length; i++) {
            this.changeListeners[i](foundsetChanges);
        }
    }
    
    public setChangeNotifier(changeNotifier: Function) {
        this.changeNotifier = changeNotifier;
    }
    
    public isChanged() {
        return this.requests && (this.requests.length > 0);
    }

    public handleSingleValue(singleValue: any) {
        // this gets called for values that are not actually record linked, and we 'fake' a viewport containing the same value on each row in the array
        this.recordLinked = false;
        this.singleValueState.viewPortSize = this.converterService.getInDepthProperty(this.forFoundset(), "viewPort", "size");
        // *** END
        
        return this.singleValueState.generateWholeViewportFromOneValue(singleValue);
    }
}

class SingleValueState {
    viewPortSize: number;
    conversionInfos: any[];

    constructor(private converter: FoundsetLinkedConverter, private propertyContext: PropertyContext, private conversionInfo: any[]) {
    }

    public updateWholeViewport(propValue: FoundsetLinked, internalState: FoundsetLinkedState, wholeViewport: any[]) {
        this.converter.updateWholeViewport(propValue, internalState, wholeViewport, this.conversionInfos, this.propertyContext );
    }

    public generateWholeViewportFromOneValue(singleValue: any)  {
        const wholeViewport = Array(this.viewPortSize).fill(singleValue);   
        this.conversionInfos = this.conversionInfo ? Array(this.viewPortSize).fill(this.conversionInfo) : undefined;
        return wholeViewport;
    }
}