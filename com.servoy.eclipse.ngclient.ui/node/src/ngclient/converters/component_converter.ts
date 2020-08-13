import { IConverter, PropertyContext, ConverterService } from '../../sablo/converter.service';
import { LoggerService, LoggerFactory } from '../../sablo/logger.service';
import { ViewportService } from '../services/viewport.service';
import { FoundsetChangeEvent, FoundsetConverter } from './foundset_converter';
import { ViewportRowUpdates, IComponentType } from '../../sablo/spectypes.service';
import { FoundsetLinkedConverter } from './foundsetLinked_converter';

export class ComponentConverter implements IConverter {

	static readonly PROPERTY_UPDATES_KEY = "propertyUpdates";
	static readonly MODEL_KEY = "model";
	static readonly MODEL_VIEWPORT_KEY = "model_vp";
	static readonly MODEL_VIEWPORT_CHANGES_KEY = "model_vp_ch";
	static readonly MODEL_VIEWPORT = "modelViewport";
	static readonly PROPERTY_NAME_KEY = "pn";
	static readonly VALUE_KEY = "v";
	static readonly NO_OP = "n";

    private log: LoggerService;

    constructor( private converterService: ConverterService, private viewportService: ViewportService, private logFactory:LoggerFactory) {
        this.log = logFactory.getLogger('ComponentConverter');
    }

    fromServerToClient(serverSentData: any, currentClientData: ComponentType, propertyContext: PropertyContext): IComponentType {
        var newValue = currentClientData;

        // see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
        const hasListeners = (currentClientData && currentClientData[ConverterService.INTERNAL_IMPL].viewportChangeListeners.length > 0);
        const notificationParamForListeners: FoundsetChangeEvent = hasListeners ?  {} : undefined;

        var childChangedNotifierGenerator; 
        if (serverSentData && serverSentData[ComponentConverter.PROPERTY_UPDATES_KEY]) {
            // granular updates received
            childChangedNotifierGenerator = this.getBeanPropertyChangeNotifierGenerator(currentClientData); 

            var internalState = currentClientData[ConverterService.INTERNAL_IMPL];
            var beanUpdate = serverSentData[ComponentConverter.PROPERTY_UPDATES_KEY];

            var modelBeanUpdate = beanUpdate[ComponentConverter.MODEL_KEY];
            var wholeViewportUpdate = beanUpdate[ComponentConverter.MODEL_VIEWPORT_KEY];
            var viewportUpdate = beanUpdate[ComponentConverter.MODEL_VIEWPORT_CHANGES_KEY];
            var done = false;

            if (modelBeanUpdate) {
                var beanModel = currentClientData[ComponentConverter.MODEL_KEY];

                // just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
                var beanLayout = internalState.beanLayout;
                var containerSize = {width: 0, height: 0};

                var modelUpdateConversionInfo = modelBeanUpdate[ConverterService.TYPES_KEY] ? this.converterService.getOrCreateInDepthProperty(internalState, ConverterService.TYPES_KEY)
                        : this.converterService.getInDepthProperty(internalState, ConverterService.TYPES_KEY);

                this.applyBeanData(beanModel, beanLayout, modelBeanUpdate, containerSize, childChangedNotifierGenerator,
                    modelUpdateConversionInfo, modelBeanUpdate[ConverterService.TYPES_KEY], propertyContext);
                done = true;
            }

            // if component is linked to a foundset, then record - dependent property values are sent over as as viewport representing values for the foundset property's viewport
            if (wholeViewportUpdate) {
                var oldRows = currentClientData[ComponentConverter.MODEL_VIEWPORT];
                if (oldRows == undefined) currentClientData[ComponentConverter.MODEL_VIEWPORT] = [];

                currentClientData[ComponentConverter.MODEL_VIEWPORT] = this.viewportService.updateWholeViewport(currentClientData[ComponentConverter.MODEL_VIEWPORT],
                        internalState, wholeViewportUpdate, beanUpdate[ConverterService.TYPES_KEY] && beanUpdate[ConverterService.TYPES_KEY][ComponentConverter.MODEL_VIEWPORT_KEY] ?
                                beanUpdate[ConverterService.TYPES_KEY][ComponentConverter.MODEL_VIEWPORT_KEY] : undefined, propertyContext);
                if (hasListeners) notificationParamForListeners.viewportRowsCompletelyChanged = { oldValue: oldRows, newValue: currentClientData[ComponentConverter.MODEL_VIEWPORT] };
                done = true;
            } else if (viewportUpdate) {
                var oldSize = currentClientData[ComponentConverter.MODEL_VIEWPORT].length;
                this.viewportService.updateViewportGranularly(currentClientData[ComponentConverter.MODEL_VIEWPORT], internalState, viewportUpdate,
                        beanUpdate[ConverterService.TYPES_KEY] && beanUpdate[ConverterService.TYPES_KEY][ComponentConverter.MODEL_VIEWPORT_CHANGES_KEY] ?
                                beanUpdate[ConverterService.TYPES_KEY][ComponentConverter.MODEL_VIEWPORT_CHANGES_KEY] : undefined, propertyContext, false);
                if (hasListeners) {
                    const upd : ViewportRowUpdates = viewportUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS];
                    notificationParamForListeners.viewportRowsUpdated = upd ; // viewPortUpdate[UPDATE_PREFIX + ROWS] was alre
                }

                done = true;
            }

            if (!done) {
                this.log.error("Can't interpret component server update correctly: " + JSON.stringify(serverSentData, undefined, 2));
            }
        } else if (serverSentData == undefined || !serverSentData[ComponentConverter.NO_OP]) {
            if (serverSentData) {
                // full contents received
                newValue = new ComponentType(
                    serverSentData.componentDirectiveName,
                    serverSentData.forFoundset,
                    serverSentData.foundsetConfig,
                    serverSentData.handlers,
                    serverSentData.model,
                    serverSentData.model_vp,
                    serverSentData.name);

                this.converterService.prepareInternalState(newValue, {});
                childChangedNotifierGenerator = this.getBeanPropertyChangeNotifierGenerator(newValue);

                var internalState = newValue[ConverterService.INTERNAL_IMPL];

                if (serverSentData[FoundsetLinkedConverter.FOR_FOUNDSET_PROPERTY] != undefined) {
                    // if it's linked to a foundset, keep that info in internal state; viewport.js needs it
                    var forFoundsetPropertyName = serverSentData[FoundsetLinkedConverter.FOR_FOUNDSET_PROPERTY];
                    internalState[FoundsetLinkedConverter.FOR_FOUNDSET_PROPERTY] = function() {
                        return propertyContext(forFoundsetPropertyName);
                    };
                    delete serverSentData[FoundsetLinkedConverter.FOR_FOUNDSET_PROPERTY];
                }
                var executeHandler = function(type, args, row, name, model) {
                    // TODO implement $uiBlocker
                    // if ($uiBlocker.shouldBlockDuplicateEvents(name, model, type, row))
                    // {
                    //     // reject execution
                    //     console.log("rejecting execution of: "+type +" on "+name +" row "+row);
                    //     return $q.resolve(null);
                    // }
                    
                    // var promiseAndCmsid = this.converterService.createDeferedEvent();
                    // var newargs = this.converterService.getEventArgs(args,type);
                    // internalState.requests.push({ handlerExec: {
                    //     eventType: type,
                    //     args: newargs,
                    //     rowId: row,
                    //     defid: promiseAndCmsid.defid
                    // }});
                    // if (internalState.changeNotifier) internalState.changeNotifier();
                    // promiseAndCmsid.promise.finally(function(){$uiBlocker.eventExecuted(name, model, type, row);});
                    // return promiseAndCmsid.promise;
                };

                // implement what $sabloConverters need to make this work
                internalState.setChangeNotifier = function(changeNotifier) {
                    internalState.changeNotifier = changeNotifier; 
                }
                internalState.isChanged = function() { return internalState.requests && (internalState.requests.length > 0); }

                // private impl
                internalState.requests = [];
                internalState.beanLayout = null; // not really useful right now; just to be able to reuse existing form code 

                // even if it's a completely new value, keep listeners from old one if there is an old value
                internalState.viewportChangeListeners = (currentClientData && currentClientData[ConverterService.INTERNAL_IMPL] ? currentClientData[ConverterService.INTERNAL_IMPL].viewportChangeListeners : []);

                /**
                 * Adds a change listener that will get triggered when server sends granular or full modelViewport changes for this component.
                 * 
                 * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this same foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
                 * @param listener the listener to register.
                 */
                newValue.addViewportChangeListener = function(listener) {
                    internalState.viewportChangeListeners.push(listener);
                    return  function () { return newValue.removeViewportChangeListener(listener); };
                }
                
                newValue.removeViewportChangeListener = function(listener) {
                    var index = internalState.viewportChangeListeners.indexOf(listener);
                    if (index > -1) {
                        internalState.viewportChangeListeners.splice(index, 1);
                    }
                }
                internalState.fireChanges = function(viewportChanges) {
                    for(var i = 0; i < internalState.viewportChangeListeners.length; i++) {
                        // TODO implement $webSocket.setIMHDTScopeHintInternal
                        //$webSocket.setIMHDTScopeHintInternal(componentScope);
                        internalState.viewportChangeListeners[i](viewportChanges);
                        //$webSocket.setIMHDTScopeHintInternal(undefined);
                    }
                }

                internalState.modelUnwatch = null;

                // calling applyBeanData initially to make sure any needed conversions are done on model's properties
                var beanData = serverSentData[ComponentConverter.MODEL_KEY];
                var beanModel: any = {};
                serverSentData[ComponentConverter.MODEL_KEY] = beanModel;

                // just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
                internalState.beanLayout = {};
                var containerSize = {width: 0, height: 0};

                var currentConversionInfo = beanData[ConverterService.TYPES_KEY] ?
                    this.converterService.getOrCreateInDepthProperty(internalState, ConverterService.TYPES_KEY) : 
                        this.converterService.getInDepthProperty(internalState, ConverterService.TYPES_KEY);

                this.applyBeanData(beanModel, internalState.beanLayout, beanData, containerSize, childChangedNotifierGenerator,
                    currentConversionInfo, beanData[ConverterService.TYPES_KEY], propertyContext);

                // component property is now be able to send itself entirely at runtime; we need to handle viewport conversions here as well
                var wholeViewport = serverSentData[ComponentConverter.MODEL_VIEWPORT_KEY];
                delete serverSentData[ComponentConverter.MODEL_VIEWPORT_KEY];
                serverSentData[ComponentConverter.MODEL_VIEWPORT] = [];

                if (wholeViewport) {
                    serverSentData[ComponentConverter.MODEL_VIEWPORT] = this.viewportService.updateWholeViewport(serverSentData[ComponentConverter.MODEL_VIEWPORT],
                            internalState, wholeViewport, serverSentData[ConverterService.TYPES_KEY] ?
                                    serverSentData[ConverterService.TYPES_KEY][ComponentConverter.MODEL_VIEWPORT_KEY] : undefined, propertyContext);
                }
                if (serverSentData[ConverterService.TYPES_KEY] != undefined) delete serverSentData[ConverterService.TYPES_KEY];

                if (!serverSentData.api) serverSentData.api = {};
                if (serverSentData.handlers)
                {
                    for (var key in serverSentData.handlers) 
                    {
                        var handler = serverSentData.handlers[key];
                        (function(key) {
                            var eventHandler = function (args,rowId)
                            {
                                return executeHandler(key, args, rowId, serverSentData.name, serverSentData.model);
                            }
                            var wrapper:any = function() {
                                return eventHandler(arguments, null);
                            }
                            wrapper.selectRecordHandler = function(rowId){
                                return function () { 
                                    return eventHandler(arguments,rowId instanceof Function?rowId():rowId) 
                                }
                            };
                            serverSentData.handlers[key] = wrapper;
                        })(key);
                    }
                }

                // here we don't specify any of the following as all those can be forwarded by the parent component from it's own servoyApi:
                // formWillShow, hideForm, getFormUrl
                serverSentData.servoyApi = {
                    /** rowId is only needed if the component is linked to a foundset */
                    startEdit: function(property, rowId) {
                        var req = { svyStartEdit: {} };

                        if (rowId) req.svyStartEdit[ViewportService.ROW_ID_COL_KEY] = rowId;
                        req.svyStartEdit[ComponentConverter.PROPERTY_NAME_KEY] = property;

                        internalState.requests.push(req);
                        if (internalState.changeNotifier) internalState.changeNotifier();
                    },

                    apply: function(property, modelOfComponent, rowId) {
                        /** rowId is only needed if the component is linked to a foundset */
                        var conversionInfo = internalState[ConverterService.TYPES_KEY];
                        if (!modelOfComponent) modelOfComponent = serverSentData[ComponentConverter.MODEL_KEY]; // if it's not linked to foundset componentModel will be undefined
                        var propertyValue = modelOfComponent[property];

                        if (conversionInfo && conversionInfo[property]) {
                            propertyValue = this.converterService.convertFromClientToServer(propertyValue, conversionInfo[property], undefined);
                        } else {
                            propertyValue = this.converterService.convertClientObject(propertyValue);
                        }

                        var req = { svyApply: {} };

                        if (rowId) req.svyApply[ViewportService.ROW_ID_COL_KEY] = rowId;
                        req.svyApply[ComponentConverter.PROPERTY_NAME_KEY] = property;
                        req.svyApply[ComponentConverter.VALUE_KEY] = propertyValue;

                        internalState.requests.push(req);
                        if (internalState.changeNotifier) internalState.changeNotifier();
                    }
                }
            }
        }
        
        if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
            // if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy component * firing founset listener notifications: " + JSON.stringify(Object.keys(notificationParamForListeners)));
            // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
            currentClientData[ConverterService.INTERNAL_IMPL].fireChanges(notificationParamForListeners);
        }

        return newValue;
    }

    fromClientToServer(newClientData: Object, oldClientData: Object): Object {
        if (newClientData) {
            var internalState = newClientData[ConverterService.INTERNAL_IMPL];
            if (internalState.isChanged()) {
                var tmp = internalState.requests;
                internalState.requests = [];
                return tmp;
            }
        }
        return [];
    }

	private getBeanPropertyChangeNotifierGenerator(propertyValue) {
        const componentThis = this;
		return function beanPropertyChangeNotifierGenerator(propertyName) {
			if (!propertyValue) return undefined;

			var internalState = propertyValue[ConverterService.INTERNAL_IMPL];
			return function beanPropertyChangeNotifier(oldValue, newValue, dumb) { // oldValue, newValue and dumb are only set when called from bean model in-depth/shallow watch; not set for smart properties
				if (dumb !== true) {
					// so smart property - no watch involved (it notifies itself as changed)
					oldValue = newValue = propertyValue[ComponentConverter.MODEL_KEY][propertyName];
				} 
				internalState.requests.push({ propertyChanges : componentThis.getChildPropertyChanges(propertyValue, oldValue, newValue, propertyName) });
				if (internalState.changeNotifier) internalState.changeNotifier();
			};
		};
    }
    
    private getChildPropertyChanges(componentState, oldPropertyValue, newPropertyValue, propertyName) {
		var internalState = componentState[ConverterService.INTERNAL_IMPL];
		var beanConversionInfo = this.converterService.getInDepthProperty(internalState, ConverterService.TYPES_KEY);
		
		// just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
		var containerSize = {width: 0, height: 0};

		return this.getComponentChanges(newPropertyValue, oldPropertyValue, beanConversionInfo,
            internalState.beanLayout, containerSize, propertyName, componentState.model);
    }
    
    private applyBeanData(beanModel, beanLayout, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, propertyContext: PropertyContext) {
        if (newConversionInfo) { // then means beanConversionInfo should also be defined - we assume that
            // beanConversionInfo will be granularly updated in the loop below
            // (to not drop other property conversion info when only one property is being applied granularly to the bean)
            beanData = this.converterService.convertFromServerToClient(beanData, newConversionInfo, beanModel, propertyContext);
        }

        // apply the new values and conversion info
        for (var key in beanData) {
            let oldModelValueForKey = beanModel[key];
            beanModel[key] = beanData[key];

            // remember conversion info for when it will be sent back to server - it might need special conversion as well
            if (newConversionInfo && newConversionInfo[key]) {
                let oldConversionInfoForKey = beanConversionInfo[key];
                beanConversionInfo[key] = newConversionInfo[key];
                
                // if the value changed and it wants to be in control of it's changes, or if the conversion info for this value changed (thus possibly preparing an old value for being change-aware without changing the value reference)
                if ((oldModelValueForKey !== beanData[key] || oldConversionInfoForKey !== newConversionInfo[key])
                        && beanData[key] && beanData[key][ConverterService.INTERNAL_IMPL] && beanData[key][ConverterService.INTERNAL_IMPL].setChangeNotifier) {
                    // setChangeNotifier can be called now after the new conversion info and value are set (changeNotifierGenerator(key) will probably use the values in model and that has to point to the new value if reference was changed)
                    // as setChangeNotifier on smart property types might end up calling the change notifier right away to announce it already has changes (because for example
                    // the convertFromServerToClient on that property type above might have triggered some listener to the component that uses it which then requested
                    // another thing from the property type and it then already has changes...) // TODO should we decouple this scenario? if we are still processing server to client changes when change notifier is called we could trigger the change notifier later/async for sending changes back to server...
                    let changeNotfier = changeNotifierGenerator(key);
                    beanData[key][ConverterService.INTERNAL_IMPL].setChangeNotifier(changeNotfier);
                    
                    // we check for changes anyway in case a property type doesn't do it itself as described in the comment above
                    if (beanData[key][ConverterService.INTERNAL_IMPL].isChanged && beanData[key][ConverterService.INTERNAL_IMPL].isChanged()) changeNotfier();
                }
            } else if (beanConversionInfo && beanConversionInfo[key] != undefined) delete beanConversionInfo[key]; // this prop. no longer has conversion info!
        }
        
        // if the model had a change notifier call it now after everything is set.
        var modelChangeFunction = beanModel.modelChangeNotifier;
        if (modelChangeFunction) {
            for (var key in beanData) {
                modelChangeFunction(key, beanModel[key]);
            }
        }

        //TODO
        //applyBeanLayout(beanModel, beanLayout, beanData, containerSize, true,useAnchoring,formname)
    }

    private getComponentChanges(now, prev, beanConversionInfo, beanLayout, parentSize, property, beanModel) {
		var changes:any = {}
		if (property) {
			if (beanConversionInfo && beanConversionInfo[property]) changes[property] = this.converterService.convertFromClientToServer(now, beanConversionInfo[property], prev);
			else changes[property] = this.converterService.convertClientObject(now)
		} else {
			// first build up a list of all the properties both have.
			var fulllist = ConverterService.getCombinedPropertyNames(now, prev);
			var prop;

			for (prop in fulllist) {
				var changed;
				if (prev && now) {
					changed = ConverterService.isChanged(now[prop], prev[prop], beanConversionInfo ? beanConversionInfo[prop] : undefined)
				} else {
					changed = true; // true if just one of them is undefined; both cannot be undefined at this point if we are already iterating on combined property names
				}

				if (changed) {
					changes[prop] = now[prop];
				}
			}
			for (prop in changes) {
				if (beanConversionInfo && beanConversionInfo[prop]) changes[prop] = this.converterService.convertFromClientToServer(changes[prop], beanConversionInfo[prop], prev ? prev[prop] : undefined);
				else changes[prop] = this.converterService.convertClientObject(changes[prop])
			}
        }
        
        if (changes.location || changes.size || changes.visible || changes.anchors) {
			if (beanLayout) {
                //TODO
				//applyBeanLayout(beanModel, beanLayout, changes, parentSize, false,useAnchoring,formname);
			}
		}

		return changes;
	}
}

export class ComponentType implements IComponentType {

    constructor(
        public componentDirectiveName: string,
        public forFoundset: string,
        public foundsetConfig: any,
        public handlers: any,
        public model: any,
        public modelViewport: any[],
        public name: string
        ) {
        }

    addViewportChangeListener(listener: any) {
        throw new Error("Method not implemented.");
    }
    removeViewportChangeListener(listener: any) {
        throw new Error("Method not implemented.");
    }
}