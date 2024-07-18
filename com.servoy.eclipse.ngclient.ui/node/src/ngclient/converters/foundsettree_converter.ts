import { IChangeAwareValue, ChangeAwareState, IUIDestroyAwareValue } from '../../sablo/converter.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred, IFoundsetTree } from '@servoy/public';
import { IType, IPropertyContext } from '../../sablo/types_registry';

export class FoundsetTreeType implements IType<FoundsetTree> {

    public static readonly TYPE_NAME = 'foundsettree';

    constructor(private sabloDeferHelper: SabloDeferHelper) {
    }

    fromServerToClient(serverJSONValue: any,
        currentClientValue: FoundsetTree, _propertyContext: IPropertyContext): FoundsetTree {

        let newValue: FoundsetTree = currentClientValue;
        let deferredValue: any;

        if (serverJSONValue) {
            if (serverJSONValue.getChildren) {
                // this is the getChildren and granular updates
                newValue = currentClientValue;
                if (serverJSONValue.handledID) {
                    deferredValue = serverJSONValue.getChildren;
                } else{
                     newValue.getInternalState().newChildren = serverJSONValue.getChildren;
                }
            } else if (serverJSONValue.newCheckedValues){
                // this is update on checked values coming from server
                 newValue = currentClientValue;
                 newValue.getInternalState().newCheckedValues = serverJSONValue.newCheckedValues;
            } else {
                const internalState = new FoundsetTreeState();
                if (currentClientValue && currentClientValue.getInternalState()) {
                    this.sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(internalState, currentClientValue.getInternalState());
                } else {
                    this.sabloDeferHelper.initInternalStateForDeferring(internalState, 'svy valuelist * ');
                }
                newValue = new FoundsetTree(this.sabloDeferHelper, internalState, serverJSONValue);
                deferredValue = newValue;
            }

            // if we have a deferred getChildren request, notify that the new value has arrived
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

    fromClientToServer(newClientData: FoundsetTree, _oldClientData: FoundsetTree, _propertyContext: IPropertyContext): [any, FoundsetTree] | null {
        if (newClientData) {
            const newDataInternalState = newClientData.getInternalState();
            if (newDataInternalState.getChildrenReq) {
                const tmp = newDataInternalState.getChildrenReq;
                delete newDataInternalState.getChildrenReq;
                return [tmp, newClientData];
            }
            if (newDataInternalState.updateSelectionReq) {
                const tmp = newDataInternalState.updateSelectionReq;
                delete newDataInternalState.updateSelectionReq;
                return [tmp, newClientData];
            }
            if (newDataInternalState.updateCheckboxValueReq) {
                const tmp = newDataInternalState.updateCheckboxValueReq;
                delete newDataInternalState.updateCheckboxValueReq;
                return [tmp, newClientData];
            }
        }
        return null; // should never happen
    }

}

class FoundsetTreeState extends ChangeAwareState implements IDeferedState {
    public getChildrenReq: { getChildren: string; id: number; level: number };
    public updateSelectionReq: {updateSelection: Array<string>};
    public updateCheckboxValueReq: {updateCheckboxValue: string; value: boolean};
    public newChildren: {key: any};
    public newCheckedValues: {key: boolean};

    deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } };
    timeoutRejectLogPrefix: string;

    init(deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } }, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }

    hasChanges(): boolean {
        return super.hasChanges() || this.getChildrenReq !== undefined || this.updateSelectionReq !== undefined || this.updateCheckboxValueReq !== undefined;
    }

    clearChanges(): void {
        super.clearChanges();
        this.getChildrenReq = undefined;
        this.updateSelectionReq = undefined;
        this.updateCheckboxValueReq = undefined;
    }

}

export class FoundsetTree extends Array<any> implements IFoundsetTree, IChangeAwareValue, IUIDestroyAwareValue {

    constructor(private sabloDeferHelper: SabloDeferHelper,
        private internalState: FoundsetTreeState, values?: Array<any>) {
        super();
        if (values) this.push(...values);
        // see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        // set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(FoundsetTree.prototype));
    }

    /**
     * This is meant for internal use only; do not call this in component code.
     */
    getInternalState(): FoundsetTreeState {
        return this.internalState;
    }

    uiDestroyed(): void{
        this.sabloDeferHelper.cancelAll(this.getInternalState());
    }

    getChildren(parentID: string, level: number): Promise<any> {
        // only block once
        this.internalState.getChildrenReq = {
            getChildren: parentID,
            level,
            id: this.sabloDeferHelper.getNewDeferId(this.internalState)
        };
        const promise = this.internalState.deferred[this.internalState.getChildrenReq.id].defer.promise;
        this.internalState.notifyChangeListener();
        return promise;
    }

    updateSelection(idarray: Array<string>): void{
        this.internalState.updateSelectionReq = {
            updateSelection: idarray,
        };
        this.internalState.notifyChangeListener();
    }

     updateCheckboxValue(id: string, value: boolean): void{
        this.internalState.updateCheckboxValueReq = {
            updateCheckboxValue: id,
            value
        };
        this.internalState.notifyChangeListener();
    }

    getAndResetNewChildren(): {key: any}{
        const tmp = this.getInternalState().newChildren;
        this.getInternalState().newChildren = null;
        return tmp;
    }

    getAndResetUpdatedCheckboxValues(): {key: boolean}{
        const tmp = this.getInternalState().newCheckedValues;
        this.getInternalState().newCheckedValues = null;
        return tmp;
    }
}
