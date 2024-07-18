import { FoundsetChangeEvent, LoggerService, LoggerFactory, IFoundset, ViewportChangeListener } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { ViewportService, FoundsetViewportState, ConversionInfoFromServerForViewport, IPropertyContextCreatorForRow, RowUpdate } from '../services/viewport.service';
import { IType, IPropertyContext, PushToServerEnum } from '../../sablo/types_registry';
import { IChangeAwareValue } from '../../sablo/converter.service';

export class FoundsetLinkedType implements IType<FoundsetLinkedValue> {

    public static readonly TYPE_NAME = 'fsLinked';

    private static readonly PROPERTY_CHANGE = 'propertyChange';

    private log: LoggerService;

    constructor(private sabloService: SabloService, private viewportService: ViewportService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('FoundsetLinkedPropertyValue');
    }

    public fromServerToClient(serverSentData: IServerSentData, currentClientValue: FoundsetLinkedValue, propertyContext: IPropertyContext): FoundsetLinkedValue {
        if (serverSentData === null) {
            if (currentClientValue) currentClientValue.getInternalState().dispose();
            return null;
        }

        // foundset linked properties always have a value both on client and on server (if wrapped value is null, foundset linked prop. will be an
        // array of null values in sync with the foundset prop's viewport)
        let newValue: FoundsetLinkedValue;
        let didSomethingWithServerSentData = false;

        if (currentClientValue) newValue = currentClientValue;
        else {
            const forFoundsetPropertyName = serverSentData.forFoundset;
            didSomethingWithServerSentData = true;
            newValue = new FoundsetLinkedValue(this.viewportService, this.sabloService, propertyContext, () => propertyContext.getProperty(forFoundsetPropertyName), this.log);
        }

        newValue = newValue.getInternalState().applyIncommingServerData(didSomethingWithServerSentData, serverSentData);

        return newValue;
    }

    public fromClientToServer(newClientData: FoundsetLinkedValue, _oldClientData: FoundsetLinkedValue, _propertyContext: IPropertyContext): [any, FoundsetLinkedValue] | null {
        if (newClientData) {
            const internalState: FSLinkedInternalState = newClientData.getInternalState();
            if (internalState.hasChanges()) {
                if (!internalState.recordLinked) {
                    // we don't need to send rowId to server in this case; we just need value
                     for (const req of internalState.requests) {
                        req[FoundsetLinkedType.PROPERTY_CHANGE] = req.viewportDataChanged.value;
                        delete req.viewportDataChanged;
                    }
                }
                const tmp = internalState.requests;
                internalState.requests = [];
                return [tmp, newClientData];
            }
        }
        return [[], newClientData];
    }

}

export class FoundsetLinkedValue extends Array<any> implements IChangeAwareValue {

    idForFoundset?: string;
    private __internalState: FSLinkedInternalState;

    /** Initializes internal state of a new value */
    public constructor(viewportService: ViewportService, sabloService: SabloService, propertyContext: IPropertyContext,
                            forFoundset: () => IFoundset, log: LoggerService) {
        super();

        // keep internal details separate; sabloConverters.prepareInternalState(this) call below will populate this with a non-iterable entry in the object
        this.__internalState = new FSLinkedInternalState(this, propertyContext, forFoundset, log, viewportService, sabloService);
    }

    /** do not call this method from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState() {
        return this.__internalState;
    }

    /**
     * Adds a change listener that will get triggered when server sends changes for this foundset linked property.
     *
     * @see SabloService.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this
     *          foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
     * @param listener the listener to register.
     */
    public addChangeListener(listener: ViewportChangeListener) {
        this.__internalState.changeListeners.push(listener);
        return () => this.removeChangeListener(listener);
    }

    public removeChangeListener(listener: ViewportChangeListener) {
        const index = this.__internalState.changeListeners.indexOf(listener);
        if (index > -1) {
            this.__internalState.changeListeners.splice(index, 1);
        }
    }

    public dataChanged(index: number, newValue: any, oldValue?: any) {
        const propertyContext = this.__internalState.getPropertyContextCreatorForRow().withRowValueAndPushToServerFor(undefined, undefined);
        if (!propertyContext || propertyContext.getPushToServerCalculatedValue() < PushToServerEnum.ALLOW) return; // we ignore all changes if server will block them anyway

        if (newValue === undefined) newValue = null;
        // we don't really need to update the whole viewport; if changes are queued to be sent to server, that will cause data to be changed
        // in the foundset viewport, those will anyway be sent from server causing an update of the viewport here

        this.__internalState.viewportService.sendCellChangeToServerBasedOnRowId(this, this.__internalState, undefined,
                this.__internalState.forFoundset().viewPort.rows[index]._svyRowId, undefined,
                this.__internalState.getPropertyContextCreatorForRow(), newValue, oldValue);
    }

}

class FSLinkedInternalState extends FoundsetViewportState {

    recordLinked = false;
    private propertyContextCreatorForRow: IPropertyContextCreatorForRow;
    private singleValueState: SingleValueState = undefined;

    constructor(private foundsetLinkedValue: FoundsetLinkedValue, propertyContext: IPropertyContext, forFoundset: () => IFoundset, log: LoggerService,
                    public viewportService: ViewportService, protected sabloService: SabloService) {
        super(forFoundset, log, sabloService);

        this.propertyContextCreatorForRow = {
               // currently foundset prop columns always have foundset prop's pushToServer so only one property context needed;
            withRowValueAndPushToServerFor: (_rowValue: any, _propertyName: string) => propertyContext

        } as IPropertyContextCreatorForRow;
    }

    getPropertyContextCreatorForRow() {
        return this.propertyContextCreatorForRow;
    }

    applyIncommingServerData(didSomethingWithServerSentData: boolean, serverSentData: IServerSentData): FoundsetLinkedValue {
        if (serverSentData != null && serverSentData !== undefined) {
            // remove smart notifiers and proxy notification effects for changes that come from server
            this.ignoreChanges = true;

            if (serverSentData.vpu !== undefined) { // VIEWPORT_VALUE_UPDATE
                if (this.singleValueState) this.singleValueState.dispose(); // will probably never happen (DP has to changed from non-record-linked to record linked on server)
                this.singleValueState = undefined;
                this.recordLinked = true;

                this.viewportService.updateViewportGranularly(this.foundsetLinkedValue, this, serverSentData.vpu,
                        undefined, this.propertyContextCreatorForRow, true);

                // restore smart watches and proxy notifiers; server side send changes are now applied
                this.ignoreChanges = false;

                this.log.spam(this.log.buildMessage(() => ('svy foundset linked * Firing change listener: granular updates... ')));
                if (this.changeListeners.length > 0) this.fireChanges({ viewportRowsUpdated: serverSentData.vpu });
            } else {
                // the rest will always be treated as a full viewport update (single values are actually going to generate a full viewport of 'the one' new value)
                let conversionInfoFromServerForViewport: ConversionInfoFromServerForViewport;

                let wholeViewport: any[];
                if (serverSentData.sv !== undefined || serverSentData.svu !== undefined) { // SINGLE_VALUE or SINGLE_VALUE_UPDATE
                    // just update single value from server and make copies of it to duplicate
                    const singleValue = serverSentData.sv !== undefined ? serverSentData.sv : serverSentData.svu;

                    if (!this.singleValueState) this.singleValueState = new SingleValueState(this.sabloService, this);
                    this.recordLinked = false;

                    wholeViewport = this.singleValueState.generateWholeViewportFromOneValue(singleValue, this.forFoundset()?.viewPort.size, serverSentData._T as string);
                    conversionInfoFromServerForViewport = this.singleValueState.getConversionInfo();
                } else if (serverSentData.vp !== undefined) {
                    // actual full viewport update from server - but not with a single value but with actual foundset linked values
                    if (this.singleValueState) this.singleValueState.dispose(); // will probably never happen (DP has to changed from non-record-linked to record linked on server)
                    this.singleValueState = undefined;
                    this.recordLinked = true;

                    wholeViewport = serverSentData.vp;
                    conversionInfoFromServerForViewport = serverSentData._T as ConversionInfoFromServerForViewport;
                }

                if (wholeViewport !== undefined) this.updateWholeViewport(wholeViewport, conversionInfoFromServerForViewport);
                else if (!didSomethingWithServerSentData && !serverSentData.forFoundset)
                    // it is possible in form designer (not real client) that serverSentData.forFoundset is the only thing that is received
                    // multiple times, due to multiple calls to toTemplateJSON on server; so allow that without logging an error
                    this.log.error('Can\'t interpret foundset linked prop. server update correctly: ' + JSON.stringify(serverSentData, undefined, 2));

                // restore smart watches and proxy notifiers; server side send changes are now applied
                this.ignoreChanges = false; // this is normally already done by updateWholeViewportFunc(...) call above; but to be sure...
            }

            if (serverSentData.idForFoundset === null) {
                if (this.foundsetLinkedValue.idForFoundset !== undefined) delete this.foundsetLinkedValue.idForFoundset;
            } else if (serverSentData.idForFoundset !== undefined) {
                // make it non-iterable as the newValue is an array an ppl. might iterate over it - they wont expect this in the iterations
                if (Object.defineProperty) {
                    Object.defineProperty(this.foundsetLinkedValue, 'idForFoundset', {
                        configurable: true,
                        enumerable: false,
                        writable: true,
                        value: serverSentData.idForFoundset
                    });
                } else this.foundsetLinkedValue.idForFoundset = serverSentData.idForFoundset;
            }

        } // else should never happen; server should always send a value; see
          // com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType.toSabloComponentValue() - it always returns a value

        return this.foundsetLinkedValue;
    }

    public updateWholeViewport(wholeViewport: any[], conversionInfo: ConversionInfoFromServerForViewport) {
        // disable smart watches and proxy notifiers; server side send changes are going to be applied
        this.ignoreChanges = true;
        const oldVal = this.foundsetLinkedValue.slice(); // create shallow copy of old rows as ref. will be the same otherwise

        // normally foundsetLinkedValue will remain the same reference below except for when it is the first initialization from
        // server and only if we have SHALLOW/DEEP watches in spec for this prop., in which case a proxy of the initial value will be returned by viewport code
        this.foundsetLinkedValue = this.viewportService.updateWholeViewport(this.foundsetLinkedValue, this, wholeViewport,
                                        conversionInfo, undefined, this.propertyContextCreatorForRow, true);

        // restore smart watches and proxy notifiers; server side send changes are now applied
        this.ignoreChanges = false;

        if (this.changeListeners.length > 0) {
            this.log.spam(this.log.buildMessage(() => ('svy foundset linked * firing change listener: full viewport changed...')));
            // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
            this.fireChanges({ viewportRowsCompletelyChanged: { oldValue: oldVal, newValue: this.foundsetLinkedValue } });
        }
    }

    public dispose() {
        if (this.singleValueState) this.singleValueState.dispose();
    }

}

class SingleValueState {

    private viewPortSize: number;
    private singleValue: any;
    private conversionInfo: ConversionInfoFromServerForViewport;
    private viewportSizeChangedListener: () => void;

    constructor(sabloService: SabloService, private iS: FSLinkedInternalState) {
        // add a listener for foundset prop. size to regenerate the viewport when that changes - fill it up again fully with single values
        sabloService.addIncomingMessageHandlingDoneTask(() => { // do it after all incomming properties have been converted so we are sure to have the forFoundset prop. ready
            const fs: IFoundset = iS.forFoundset();
            if (fs) {
                this.viewportSizeChangedListener = fs.addChangeListener((event: FoundsetChangeEvent) => {
                    if (event.viewPortSizeChanged || event.fullValueChanged) {
                        this.checkFoundsetSizeAndRegenerateIfNeeded();
                    }
                });
            }

            // if both foundset and foundset linked come in the same json from server, it might
            // happen that the foundset linked fromServerToClient is done before the foundset one - so the foundset prop. might not yet be available
            // or not updated (so the single value viewport is not populated in that case because we do not know the size); in that case we need to
            // regenerateWholeViewportDueToSizeChange once when addIncomingMessageHandlingDoneTask starts executing (then all props. from that data burst are processed)
            // ALSO, if fs is null (but change was applied after current property) we need to generate an empty array so that it doesn't remain with an obsolete single-value foundset size
            this.checkFoundsetSizeAndRegenerateIfNeeded();
        });
    }
    
    private checkFoundsetSizeAndRegenerateIfNeeded() {
        let newSize = this.iS.forFoundset()?.viewPort.size;
        if (newSize === undefined || newSize === null) newSize = 0;
        if (newSize === this.viewPortSize) return;

        const wholeViewport = this.regenerateWholeViewportDueToSizeChange(newSize);
        this.iS.updateWholeViewport(wholeViewport, this.conversionInfo);
    }

    dispose() {
        if (this.viewportSizeChangedListener) {
            this.viewportSizeChangedListener();
            this.viewportSizeChangedListener = undefined;
        }
    }

    getConversionInfo() {
        return this.conversionInfo;
    }

    /** This builds (from the single value from server) a viewport-from-server-equivalent to be used with viewportService code  */
    generateWholeViewportFromOneValue(singleValue: any, vpSize: number | undefined | null, conversionInfoFromServer: string): Array<any> {
        // this gets called for values that are not actually record linked, and we 'fake' a viewport containing the same value on each row in the array
        if (vpSize === undefined || vpSize === null) vpSize = 0;
        this.viewPortSize = vpSize;
        this.singleValue = singleValue;

        if (conversionInfoFromServer) {
            // we got from server the conversion type for this single value; as we generate a viewport of that value we must give the ViewportService code
            // optimized conversion info as one would receive from server for a full viewport
            this.conversionInfo = { mT: conversionInfoFromServer };
        } else this.conversionInfo = undefined;

        return this.generateWholeViewportFromOneValueInternal();
    }

    /** This re-builds based on previously stored singleValue (from the single value from server) a viewport-from-server-equivalent to be used with viewportService code. */
    regenerateWholeViewportDueToSizeChange(vpSize: number): Array<any> {
        this.viewPortSize = vpSize;
        return this.generateWholeViewportFromOneValueInternal();
    }

    private generateWholeViewportFromOneValueInternal() {
        const wholeViewport = [];
        for (let index = this.viewPortSize - 1; index >= 0; index--) {
            wholeViewport.push(this.singleValue);
        }
        return wholeViewport;
    }

}

interface IServerSentData {

    forFoundset?: string;

    /** VIEWPORT_VALUE_UPDATE */
    vpu?: RowUpdate[];

    /** CONVERSION_CL_SIDE_TYPE_KEY */
    _T?: ConversionInfoFromServerForViewport | string;

    /** SINGLE_VALUE */
    sv?: any;

    /** SINGLE_VALUE_UPDATE */
    svu?: any;

    /** VIEWPORT_VALUE */
    vp?: any[];

    /** ID_FOR_FOUNDSET */
    idForFoundset?: string | null;

}
