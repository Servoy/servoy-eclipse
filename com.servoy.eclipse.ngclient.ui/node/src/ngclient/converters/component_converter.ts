import {
    IParentAccessForSubpropertyChanges, ConverterService, isChanged, IChangeAwareValue,
    instanceOfChangeAwareValue, ChangeListenerFunction
} from '../../sablo/converter.service';
import { LoggerService, LoggerFactory, IFoundset, ViewportChangeEvent, ViewportChangeListener, IChildComponentPropertyValue } from '@servoy/public';
import {
    FoundsetViewportState, ViewportService, IPropertyContextCreatorForRow, ISomePropertiesInRowAreNotFoundsetLinked,
    ConversionInfoFromServerForViewport, RowUpdate, CellUpdatedFromServerListener
} from '../services/viewport.service';
import { ComponentCache } from '../types';
import { SabloService } from '../../sablo/sablo.service';
import { TypesRegistry, IType, IPropertyContext, PushToServerEnum, IWebObjectSpecification, PushToServerUtils } from '../../sablo/types_registry';
import { FormService } from '../form.service';
import { UIBlockerService } from '../services/ui_blocker.service';

export class ComponentType implements IType<ChildComponentPropertyValue> {

    public static readonly TYPE_NAME = 'component';

    private log: LoggerService;

    constructor(private converterService: ConverterService<unknown>, private readonly typesRegistry: TypesRegistry, logFactory: LoggerFactory,
        private viewportService: ViewportService, private readonly sabloService: SabloService, private uiBlockerService: UIBlockerService) {
        this.log = logFactory.getLogger('ComponentConverter');
    }

    public fromServerToClient(serverSentData: IServerSentData, currentClientValue: ChildComponentPropertyValue, propertyContext: IPropertyContext): ChildComponentPropertyValue {
        let newValue: ChildComponentPropertyValue = currentClientValue;

        if (serverSentData && serverSentData.propertyUpdates) {
            // granular updates received
            currentClientValue.getInternalState().updateFromServerArrived(serverSentData.propertyUpdates);
        } else if (!serverSentData || !serverSentData.n) { // .n means NO OPERATION
            // full contents received
            if (serverSentData) {
                newValue = new ChildComponentPropertyValue(serverSentData, currentClientValue, propertyContext,
                    this.converterService, this.sabloService, this.viewportService, this.typesRegistry, this.uiBlockerService, this.log);
            } else newValue = null;
        }

        return newValue;
    }

    public fromClientToServer(newClientData: ChildComponentPropertyValue, _oldClientData: ChildComponentPropertyValue, _propertyContext: IPropertyContext): [any, ChildComponentPropertyValue] | null {
        if (newClientData) {
            const internalState: ComponentTypeInternalState = newClientData.getInternalState();
            if (internalState.hasChanges()) {
                const tmp = internalState.requests;
                internalState.requests = [];
                return [tmp, newClientData];
            }
        }
        return [[], newClientData];
    }

}

export class ChildComponentPropertyValue extends ComponentCache implements IChangeAwareValue, IChildComponentPropertyValue {

    name: string;

    ///** this is the shared part of the model; you might want to use modelViewport (which uses this as prototype) instead if the child component has foundset-linked properties */
    //model: any; // already declared in parent class

    //handlers: any; // already declared in parent class

    api: any;
    foundsetConfig?: {
        recordBasedProperties?: Array<string>;
        apiCallTypes?: Array<any>;
    };

    /** @deprecated legacy - tableview forms; not used anymore I think */
    componentIndex?: number;
    /** @deprecated legacy - tableview forms; not used anymore I think */
    headerIndex?: number;

    /** this is the true cell viewport which is already composed inside IChildComponentPropertyValue of shared (non foundset dependent) part and row specific (foundset dependent props) part */
    modelViewport: { [property: string]: any }[];

    /**
     * This function has to be set/provided by the ng2 component that uses this child "component" typed property, because
     * that one controls the UI of the child components represented by this property (or child components in case of
     * foundset linked components like in list form component for example). Then when there are changes comming from server, the 'component'
     * property type will call this function as needed.
     */
    triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate: (propertiesChangedButNotByRef: { propertyName: string; newPropertyValue: any }[], relativeRowIndex: number) => void;

    /**
     * This gives a way to trigger handlers.
     *
     * In case this component is not foundset-linked, use mappedHandlers directly (which is a function).
     * In case this component is foundset-linked, use mappedHandlers.selectRecordHandler(rowId) to trigger the handler for the component of the correct row in the foundset.
     */
    public readonly mappedHandlers = new Map<string, { (): Promise<any>; selectRecordHandler(rowId: any): () => Promise<any> }>();

    private __internalState: ComponentTypeInternalState;

    constructor(serverSentData: IServerSentData, oldClientValue: ChildComponentPropertyValue, propertyContext: IPropertyContext, converterService: ConverterService<unknown>,
        sabloService: SabloService, viewportService: ViewportService, typesRegistry: TypesRegistry, uiBlockerService: UIBlockerService, log: LoggerService) {

        super(serverSentData.name, serverSentData.componentDirectiveName, serverSentData.elType, serverSentData.handlers, serverSentData.position, typesRegistry);

        const forFoundsetPropertyName = serverSentData.forFoundset;

        const componentSpecification = typesRegistry.getComponentSpecification(this.specName);
        this.__internalState = new ComponentTypeInternalState(this, oldClientValue?.__internalState, componentSpecification, converterService,
            viewportService, sabloService, uiBlockerService, log, this.typesRegistry, () => propertyContext.getProperty(forFoundsetPropertyName));

        this.__internalState.initializeFullValueFromServer(serverSentData);
    }

    // what is needed to give servoyApi to components follows (startEdit and sendChanges (for apply));
    // we don't need to give any of the others, as all those can be forwarded by the parent component from it's own servoyApi:
    // formWillShow, hideForm, getFormUrl, isInDesigner, ...

    // if you are wondering why there a two kinds of rowIds used below: we need to work with both component rowId and property inside component rowId if we have this scenario
    // listFormComponent
    //   - foundset property
    //   - childCompProperty of 'component' type (to which someTableCompThatCanEditCells is assigned) - that is foundset linked
    //       - foundsetProp - a prop that is linked to parent foundset of list form component (a related foundset)
    //       - columns[1].dp - that would be a dataprovider property that if foundset linked to the foundsetProp - so to the related foundset
    //
    // when a someTableCompThatCanEditCells in some list form component row calls startEdit or applyDataprovider on it's columns[1].dp,
    // the component type needs to send to server both the rowId in listFormComponent's foundset - that identifies correctly the
    // component's row on server as well as the rowId for the dataprovider columns[1].dp inside the related foundsetProp - so we know
    // what row in the related foundset is affected by that startEdit or applyDataprovider

    startEdit(propertyName: string, rowId?: any) {
        /** rowId is only needed if the component is linked to a foundset */
        FormService.pushEditingStarted(this.__internalState.getActualComponentModel(rowId), propertyName,
            (foundsetLinkedRowId: string, propertyNameToSend: string) => {
                const req: IOutgoingStartEdit = { svyStartEdit: { pn: propertyNameToSend } };

                if (rowId) req.svyStartEdit[ViewportService.ROW_ID_COL_KEY] = rowId; // rowId that identifies which component it is in the viewport of foundset linked components
                if (foundsetLinkedRowId) req.svyStartEdit._svyRowIdOfProp = foundsetLinkedRowId; // rowId that identifies the row inside the component
                // for the foundset linked DP of the component (that is linked to another property of type foundset inside the component itself)

                this.__internalState.requests.push(req);
                this.__internalState.notifyChangeListener();
            });
    }

    sendChanges(propertyName: string, newValue: any, oldValue: any, rowId?: string, isDataprovider?: boolean) {
        /** rowId is only needed if the component is linked to a foundset */
        this.__internalState.sendChanges(propertyName, newValue, oldValue, rowId, isDataprovider);
    }

    /** do not call this method from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState() {
        return this.__internalState;
    }

    /**
     * Adds a change listener that will get triggered when server sends granular or full modelViewport changes for this component.
     *
     * @see SabloService.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this same
     *          foundset get their changes applied you can use WebsocketSession.addIncomingMessageHandlingDoneTask
     * @param viewportChangeListener the listener to register.
     *
     * @return a listener unregister function.
     */
    public addViewportChangeListener(viewportChangeListener: ViewportChangeListener): () => void {
        this.__internalState.changeListeners.push(viewportChangeListener);
        return () => this.removeViewportChangeListener(viewportChangeListener);
    }

    public removeViewportChangeListener(viewportChangeListener: ViewportChangeListener): void {
        const index = this.__internalState.changeListeners.indexOf(viewportChangeListener);
        if (index > -1) {
            this.__internalState.changeListeners.splice(index, 1);
        }
    }

}

class ComponentTypeInternalState extends FoundsetViewportState implements ISomePropertiesInRowAreNotFoundsetLinked {

    // just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
    beanLayout: any = {}; // not really useful right now; just to be able to reuse existing form code

    modelUnwatch: (() => void)[] = null;

    public readonly propertyContextCreatorForRow: IPropertyContextCreatorForRow;

    private readonly changeListenerGeneratorForSmartNonFSLinkedProps: (propertyName: string, propValue: any) => ChangeListenerFunction;

    constructor(private readonly componentValue: ChildComponentPropertyValue,
        oldClientValueInternalState: ComponentTypeInternalState,
        public readonly componentSpecification: IWebObjectSpecification,
        public readonly converterService: ConverterService<unknown>,
        private readonly viewportService: ViewportService,
        public readonly sabloService: SabloService,
        private uiBlockerService: UIBlockerService,
        log: LoggerService,
        private readonly typesRegistry: TypesRegistry,
        forFoundset?: () => IFoundset) {

        super(forFoundset, log, sabloService);

        this.propertyContextCreatorForRow = {
            withRowValueAndPushToServerFor: (rowValue: any, propertyName: string) => ({
                // this does retrieve props from componentValue[ComponentType.MODEL_KEY] as well, see ComponentType.getRowModelCreatorFor(...) which constructs this 'rowValue'
                getProperty: (otherPropertyName: string) => rowValue[otherPropertyName],

                getPushToServerCalculatedValue: () =>
                    // getPropertyPushToServer not getPropertyDeclaredPushToServer
                    this.componentSpecification ? this.componentSpecification.getPropertyPushToServer(propertyName) : PushToServerEnum.REJECT

            } as IPropertyContext)
        } as IPropertyContextCreatorForRow;

        // even if it's a completely new value, keep listeners from old one if there is an old value
        if (oldClientValueInternalState && oldClientValueInternalState.changeListeners) this.changeListeners = oldClientValueInternalState.changeListeners;

        // changeListenerForSmartNonFSLinkedProps is only for model properties, viewport properties are handled in viewport.service.ts
        this.changeListenerGeneratorForSmartNonFSLinkedProps = (propertyName: string, propValue: IChangeAwareValue) => ((doNotPushNow?: boolean) => {
            if (!doNotPushNow) {
                // so smart property - no watch involved (it notifies itself as changed)

                // TODO we don't know here the rowId, as it's a non-fs-linked prop; so sendChanges will use shared model only as model, not the actual model of comp
                // so if in the future fromClientToServer will need to use a row model (props. depending on each other, in this case a non-fs-linked prop depending on
                // a fs. linked prop) we have to have here a rowId as well to be able to identify the correct row's component model; but this is unlikely to be needed
                this.componentValue.sendChanges(propertyName, propValue, propValue, undefined, false); // we know currently DPs are not smart props, so use false there
            }  // else this was triggered by an custom array or object change with push to server ALLOW - which should not send it automatically but just mark changes in the
            // nested values towards this root prop; so nothing to do here then
        });
    }

    initializeFullValueFromServer(serverSentData: IServerSentData): void {
        FormService.updateComponentModelPropertiesFromServer(serverSentData.model, this.componentValue,
            this.componentSpecification, this.converterService,
            this.getChangeListenerGeneratorForSmartNonFSLinkedProps(),
            (propertiesChangedButNotByRef: { propertyName: string; newPropertyValue: any }[]) => (this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate ?
                this.sabloService.addIncomingMessageHandlingDoneTask(() => {
                    if (this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate)
                        this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate(propertiesChangedButNotByRef, -1)
                }) : undefined));

        // component property is now be able to send itself entirely at runtime; we need to handle viewport conversions here as well
        const wholeViewportUpdateFromServer = serverSentData.model_vp;

        this.componentValue.foundsetConfig = serverSentData.foundsetConfig;
        this.componentValue.componentIndex = serverSentData.componentIndex;
        this.componentValue.headerIndex = serverSentData.headerIndex;

        this.componentValue.modelViewport = [];

        this.applyUpdatesAndHandleCellChangesThatAreNotByRef((cellUpdatedFromServerListener: (relativeRrowIndex: number, columnName: string, oldValue: any, newValue: any) => void) => {
            if (wholeViewportUpdateFromServer) {
                this.componentValue.modelViewport = this.viewportService.updateWholeViewport(this.componentValue.modelViewport,
                    this, wholeViewportUpdateFromServer, serverSentData._T,
                    this.componentSpecification, this.propertyContextCreatorForRow, false, this.getRowModelCreator(),
                    cellUpdatedFromServerListener);
            }

            if (serverSentData.handlers)
                for (const handlerName of Object.keys(serverSentData.handlers))
                    this.componentValue.mappedHandlers.set(handlerName, this.generateWrapperHandler(handlerName, this.componentValue));

            // restore smart watches and proxy notifiers; server side send changes are now applied
            this.ignoreChanges = false;
        });
    }

    updateFromServerArrived(granularUpdateFromServer: IGranularUpdatesFromServer): void {
        // remove smart notifiers and proxy notification effects for changes that come from server
        this.ignoreChanges = true;

        // see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
        const hasListeners = this.changeListeners.length > 0;
        const notificationParamForListeners: ViewportChangeEvent = hasListeners ? {} : undefined;

        const nonFSLinkedModelUpdates = granularUpdateFromServer.model;
        const wholeViewportUpdate = granularUpdateFromServer.model_vp;
        const viewportUpdate = granularUpdateFromServer.model_vp_ch;

        let done = false;

        if (nonFSLinkedModelUpdates) {
            // just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
            FormService.updateComponentModelPropertiesFromServer(nonFSLinkedModelUpdates, this.componentValue,
                this.componentSpecification, this.converterService,
                this.getChangeListenerGeneratorForSmartNonFSLinkedProps(),
                (propertiesChangedButNotByRef: { propertyName: string; newPropertyValue: any }[]) => (this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate ?
                    this.sabloService.addIncomingMessageHandlingDoneTask(() => {
                        if (this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate)
                            this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate(propertiesChangedButNotByRef, -1)
                    }) : undefined));
            done = true;
        }

        this.applyUpdatesAndHandleCellChangesThatAreNotByRef((cellUpdatedFromServerListener: (relativeRowIndex: number, columnName: string, oldValue: any, newValue: any) => void) => {
            if (wholeViewportUpdate) {
                const oldRows = this.componentValue.modelViewport.slice(); // create shallow copy of old rows as ref. will be the same otherwise

                this.componentValue.modelViewport = this.viewportService.updateWholeViewport(this.componentValue.modelViewport,
                    this, wholeViewportUpdate, granularUpdateFromServer._T,
                    this.componentSpecification,
                    this.propertyContextCreatorForRow, false, this.getRowModelCreator(),
                    cellUpdatedFromServerListener);
                if (hasListeners) notificationParamForListeners.viewportRowsCompletelyChanged = { oldValue: oldRows, newValue: this.componentValue.modelViewport };
                done = true;
            } else if (viewportUpdate) {
                this.viewportService.updateViewportGranularly(this.componentValue.modelViewport, this, viewportUpdate,
                    this.componentSpecification, this.propertyContextCreatorForRow,
                    false, this.getRowModelCreator(), cellUpdatedFromServerListener);
                if (hasListeners) {
                    notificationParamForListeners.viewportRowsUpdated = viewportUpdate; // viewPortUpdate was already prepared for listeners by $viewportModule.updateViewportGranularly
                }
                done = true;
            }
            if (!done) {
                this.log.error('svy component * Can\'t interpret component server update correctly: ' + JSON.stringify(granularUpdateFromServer, undefined, 2));
            }

            // restore smart watches and proxy notifiers; server side send changes are now applied
            this.ignoreChanges = false;

            if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
                this.log.spam(this.log.buildMessage(() => ('svy component * Firing founset listener notifications: ' + JSON.stringify(Object.keys(notificationParamForListeners)))));

                // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
                this.fireChanges(notificationParamForListeners);
            }
        });
    }

    /** Works for both model properties and viewport model properties */
    getClientSideType(propertyName: string, rowId: any): IType<any> {
        let clientSideType: IType<any> = this.getClientSideTypeOfModelProp(propertyName);
        if (!clientSideType) clientSideType = this.viewportService.getClientSideTypeFor(rowId, propertyName, this); // try viewport dynamic types

        return clientSideType;
    }

    /** Only for model properties, viewport properties are handled in viewport.ts. */
    getChangeListenerGeneratorForSmartNonFSLinkedProps(): (propertyName: string, propValue: any) => ChangeListenerFunction {
        return this.changeListenerGeneratorForSmartNonFSLinkedProps;
    }

    isFoundsetLinkedProperty(propertyName: string): boolean {
        return !!this.componentValue.foundsetConfig?.recordBasedProperties?.find(recBasedProp => (recBasedProp === propertyName));
    }

    sendChanges(propertyName: string, newValue: any, oldValue: any, rowId?: string, isDataprovider?: boolean) {
        /** rowId is only needed if the component is linked to a foundset */
        const clientSideType = this.getClientSideType(propertyName, rowId);
        const rowComponentModelValue = this.getActualComponentModel(rowId);

        if (isDataprovider) {
            let req: IOutgoingDataproviderApply;
            FormService.pushApplyDataprovider(rowComponentModelValue, propertyName, clientSideType,
                newValue, this.componentSpecification, this.converterService, oldValue,
                (foundsetLinkedDPRowId: string, propertyNameToSend: string, valueToSend: any) => {
                    req = { svyApply: { pn: propertyNameToSend, v: valueToSend } };

                    if (rowId) req.svyApply[ViewportService.ROW_ID_COL_KEY] = rowId; // rowId that identifies which component it is in the viewport of foundset linked components
                    if (foundsetLinkedDPRowId) req.svyApply._svyRowIdOfProp = foundsetLinkedDPRowId; // rowId that identifies the row inside the
                    //component for the foundset linked DP of the component (that is linked to another property of type foundset inside the component itself)
                }, this.typesRegistry);
            this.requests.push(req);
            this.notifyChangeListener();
        } else if (!rowId) {
            // non - foundset linked prop. to send
            if (!isChanged(newValue, oldValue)) {
                this.log.spam(this.log.buildMessage(() => ('svy component * dataPush nfsl (sendChanges) denied; no changes detected between new and old value!')));
            } else {
                const req: IOutgoingPropertyPush = { propertyChanges: {} };
                // even if clientSideType is still undefined, do the default conversion
                const converted: [any, any] = this.converterService.convertFromClientToServer(newValue, clientSideType, oldValue,
                    this.propertyContextCreatorForRow.withRowValueAndPushToServerFor(rowComponentModelValue, propertyName) // TODO will this ever need to look inside 'component' type real merged
                    // model viewport as well (so some properties to search for
                    // foundset linked properties on which they depend... I think currently this is not needed)? if yes then the parent component should provide
                    // a way to get to the actual model values per rowId or rowIndex and then we'd use that to generate property context generators per row
                );
                rowComponentModelValue[propertyName] = converted[1];
                if (instanceOfChangeAwareValue(converted[1])) {
                    converted[1].getInternalState().setChangeListener(this.getChangeListenerGeneratorForSmartNonFSLinkedProps()(propertyName, converted[1]));
                }

                req.propertyChanges[propertyName] = converted[0];
                this.requests.push(req);
                this.notifyChangeListener();
            }
        } else {
            // component code 'manually' requested a send change for some value in the modelViewport; handle that just as viewport does
            this.viewportService.sendCellChangeToServerBasedOnRowId(this.componentValue.modelViewport, this, undefined, rowId, propertyName,
                this.propertyContextCreatorForRow, newValue, oldValue);
        }
    }

    getActualComponentModel(rowId: string) {
        return rowId ?
            this.componentValue.modelViewport[this.forFoundset().viewPort.rows.findIndex((foundsetRow) => foundsetRow._svyRowId === rowId)] // model of a
            // foundset linked component for one row of the foundset
            : this.componentValue.model; // component is not foundset linked or rowId is not known to caller (probably dealing with a non-fs-linked property)
    }

    private generateWrapperHandler(handlerName: string, componentValue: ChildComponentPropertyValue) {
        const executeHandler = (args: IArguments, rowId: string) => {
            const handlerSpec = this.componentSpecification?.getHandler(handlerName);
            if ((!handlerSpec || !handlerSpec.ignoreNGBlockDuplicateEvents) && this.uiBlockerService.shouldBlockDuplicateEvents(componentValue.name, componentValue.model, handlerName, rowId)) {
                // reject execution
                this.log.debug('rejecting execution of handler "' + handlerName + '" on component "' + componentValue.name + '"; rowID = ' + rowId);
                return Promise.resolve(null);
            }
            const deferred = this.sabloService.createDeferredWSEvent();
            const newargs = this.converterService.getEventArgs(args, handlerName, handlerSpec);
            this.requests.push({
                handlerExec: {
                    eventType: handlerName,
                    args: newargs,
                    rowId,
                    defid: deferred.cmsgid
                }
            });
            this.notifyChangeListener();
            deferred.deferred.promise.finally(() => {
                this.uiBlockerService.eventExecuted(componentValue.name, componentValue.model, handlerName, rowId);
            });
            return deferred.deferred.promise.then((retVal) => this.converterService.convertFromServerToClient(retVal, handlerSpec?.returnType,
                undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES));
        };
        const eventHandler = (args: any, rowId: any) => executeHandler(args, rowId);
        // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
        const wrapper = function() {
            return eventHandler(arguments, null);
        };
        wrapper.selectRecordHandler = (rowId: any) =>
            // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
            function() {
                return eventHandler(arguments, rowId instanceof Function ? rowId() : rowId);
            };
        return wrapper;
    }

    private getRowModelCreator(): () => any {
        return () => {
            // the actual models for foundset linked components need to be 1 model per foundset row
            // all rows will have the non-foundset-linked properties from compValue.model which is set as prototype (so those are property values common to all of the components)
            // and we make sure any assignments or deletes for non-foundset-linked properties on such a model is applied to the shared compValue.model directly

            // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
            function ModelInSpecificRow() {
            }
            ModelInSpecificRow.prototype = this.componentValue.model;

            return new Proxy(new ModelInSpecificRow(), {
                set: (row: any, prop: any, v: any, receiver: any) => {
                    if (!this.isFoundsetLinkedProperty(prop)) {
                        delete row[prop]; // should always be undefined as it's not record linked but do make sure
                        return Reflect.set(this.componentValue.model, prop, v, this.componentValue.model); // forward non record based prop. assignments to shared model
                    } else return Reflect.set(row, prop, v, receiver);
                },

                deleteProperty: (row: any, prop: any) => {
                    if (!this.isFoundsetLinkedProperty(prop)) {
                        delete row[prop]; // should always be undefined as it's not record linked but do make sure
                        return Reflect.deleteProperty(this.componentValue.model, prop);
                    } else return Reflect.deleteProperty(row, prop);
                }
            });
        };
    }

    /** Works only for model properties, not for viewport model properties. */
    private getClientSideTypeOfModelProp(propertyName: string): IType<any> {
        let clientSideType: IType<any> = this.componentValue.dynamicClientSideTypes[propertyName]; // try dynamic types for non-viewport props.
        if (!clientSideType) { // try static types for props.
            if (this.componentSpecification) clientSideType = this.componentSpecification.getPropertyType(propertyName);
        }

        return clientSideType;
    }

    private applyUpdatesAndHandleCellChangesThatAreNotByRef(
        applyUpdatesFunction: (
            cellUpdatedFromServerListener: (relativeRowIndex: number, columnName: string, oldValue: any, newValue: any) => void
        ) => void
    ) {

        const cellsChangedButNotByRef: { relativeRowIndex: number; propertiesChangedButNotByRef: { propertyName: string; newPropertyValue: any }[] }[] = [];
        const cellUpdatedFromServerListener: CellUpdatedFromServerListener = (relativeRowIndex: number, columnName: string, oldValue: any, newValue: any) => {
            if (oldValue === newValue) {
                let lastIdxChangedButNotByRefEntries = cellsChangedButNotByRef.length > 0 ? cellsChangedButNotByRef[cellsChangedButNotByRef.length - 1] : undefined;
                if (!lastIdxChangedButNotByRefEntries || lastIdxChangedButNotByRefEntries.relativeRowIndex !== relativeRowIndex) {
                    lastIdxChangedButNotByRefEntries = { relativeRowIndex, propertiesChangedButNotByRef: [] };
                    cellsChangedButNotByRef.push(lastIdxChangedButNotByRefEntries);
                }
                lastIdxChangedButNotByRefEntries.propertiesChangedButNotByRef.push({ propertyName: columnName, newPropertyValue: newValue });
            }
        };

        applyUpdatesFunction(cellUpdatedFromServerListener);

        // trigger ngOnChanges for properties that had updates but the ref remained the same (as those will not automatically be triggered by root detectChanges())
        cellsChangedButNotByRef.forEach((rowEntriesChangedButNotByRef) => (this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate ?
            this.sabloService.addIncomingMessageHandlingDoneTask(() => {
                if (this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate)
                    this.componentValue.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate(rowEntriesChangedButNotByRef.propertiesChangedButNotByRef, rowEntriesChangedButNotByRef.relativeRowIndex)
            }) : undefined));
    }

}

interface IServerSentData {
    propertyUpdates?: IGranularUpdatesFromServer;
    n?: boolean;
    name?: string;
    componentDirectiveName?: string;
    /** usually undefined, except for default tabless panel and accordion (which share tabpanel .spec file but have different client side component names) */
    elType?: string,
    handlers?: Array<string>;
    forFoundset?: string;
    model?: Record<string, any>;
    model_vp?: any[];
    _T?: ConversionInfoFromServerForViewport;
    foundsetConfig?: {
        recordBasedProperties?: Array<string>;
        apiCallTypes?: Array<any>;
    };

    position?: { [property: string]: string }; // AngularFormGenerator.writePosition(...)

    /** @deprecated legacy - tableview forms; not used anymore I think */
    componentIndex?: number;
    /** @deprecated legacy - tableview forms; not used anymore I think */
    headerIndex?: number;
}

interface IGranularUpdatesFromServer {
    model: Record<string, any>;
    model_vp?: any[];
    model_vp_ch?: RowUpdate[];
    _T?: ConversionInfoFromServerForViewport;
}

interface IOutgoingStartEdit {
    svyStartEdit: {
        _svyRowId?: string;
        _svyRowIdOfProp?: string;
        pn: string;
    };
}

interface IOutgoingDataproviderApply {
    svyApply: {
        _svyRowId?: string;
        _svyRowIdOfProp?: string;
        pn: string;
        v: any;
    };
}

interface IOutgoingPropertyPush {
    propertyChanges: Record<string, any>; // actually it's currently only 1 change at a time, but key is property name there
}
