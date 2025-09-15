import { Directive, Input, OnInit, ElementRef, HostListener, OnDestroy, SimpleChanges, OnChanges } from '@angular/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[sabloTabseq]'
})
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class SabloTabseq implements OnInit, OnChanges, OnDestroy {

    @Input('sabloTabseq') designTabSeq: number;
    @Input('sabloTabseqConfig') config: SabloTabseqConfig;
    
    designChildIndexToArrayPosition: { [designChildTabSeq: number]: number } = {};
    designChildTabSeq: Array<number> = []; // contains ordered numbers that will be keys in 'runtimeChildIndexes'; can have duplicates

    // map designChildIndex[i] -> runtimeIndex for child or designChildIndex[i] -> [runtimeIndex1, runtimeIndex2] in case there are multiple equal design time indexes
    runtimeChildIndexes: { [designChildIndex: number]: RuntimeIndex | Array<RuntimeIndex> } = {};
    
    runtimeIndex: RuntimeIndex;
    initializing: boolean;
    isEnabled: boolean;

    constructor(private _elemRef: ElementRef<Element>/*, private _cdRef: ChangeDetectorRef*/) {
    }

    // handle event: Child Servoy Tab Sequence registered
    @HostListener('registerCSTS', ['$event'])
    registerChildHandler(event: CustomEvent<{ designChildIndex: number, runtimeChildIndex: RuntimeIndex}>): boolean {
        if (this.designTabSeq === -2 || event.detail.designChildIndex === -2) {
            this.recalculateIndexesHandler(event.detail.designChildIndex ? event.detail.designChildIndex : 0, false);
            event.stopPropagation();
            return false;
        }

        // insert it sorted
        let posInDesignArray = 0;
        for (let tz = 0; tz < this.designChildTabSeq.length && this.designChildTabSeq[tz] < event.detail.designChildIndex; tz++) {
            posInDesignArray = tz + 1;
        }
        if (posInDesignArray === this.designChildTabSeq.length || this.designChildTabSeq[posInDesignArray] > event.detail.designChildIndex) {
            this.designChildTabSeq.splice(posInDesignArray, 0, event.detail.designChildIndex);

            // always keep in designChildIndexToArrayPosition[i] the first occurrance of design index i in the sorted designChildTabSeq array
            for (let tz = posInDesignArray; tz < this.designChildTabSeq.length; tz++) {
                this.designChildIndexToArrayPosition[this.designChildTabSeq[tz]] = tz;
            }
            this.runtimeChildIndexes[event.detail.designChildIndex] = event.detail.runtimeChildIndex;
        } else {
            // its == that means that we have dupliate design indexes; we treat this special - all same design index children as a list in one runtime index array cell
            if (!isRuntimeIndexArray(this.runtimeChildIndexes[event.detail.designChildIndex])) {
                this.runtimeChildIndexes[event.detail.designChildIndex] = [this.runtimeChildIndexes[event.detail.designChildIndex] as RuntimeIndex];
            }
            (this.runtimeChildIndexes[event.detail.designChildIndex] as Array<RuntimeIndex>).push(event.detail.runtimeChildIndex);
        }

        this.recalculateIndexesHandler(event.detail.designChildIndex ? event.detail.designChildIndex : 0, false);
        event.stopPropagation();
        return false;
    }

    @HostListener('unregisterCSTS', ['$event'])
    unregisterChildHandler(event: CustomEvent<{ designChildIndex: number, runtimeChildIndex: RuntimeIndex}>): boolean {
        if (this.designTabSeq === -2 || event.detail.designChildIndex === -2) {
            event.stopPropagation();
            return false;
        }

        const posInDesignArray = this.designChildIndexToArrayPosition[event.detail.designChildIndex];
        if (posInDesignArray !== undefined) {
            const keyInRuntimeArray = this.designChildTabSeq[posInDesignArray];
            const runtimeChildIndexForKey = this.runtimeChildIndexes[keyInRuntimeArray];
            if (!isRuntimeIndexArray(runtimeChildIndexForKey)) {
                delete this.designChildIndexToArrayPosition[event.detail.designChildIndex];
                for (const tmp in this.designChildIndexToArrayPosition) {
                    if (this.designChildIndexToArrayPosition[tmp] > posInDesignArray) this.designChildIndexToArrayPosition[tmp]--;
                }
                this.designChildTabSeq.splice(posInDesignArray, 1);
                delete this.runtimeChildIndexes[keyInRuntimeArray];
            } else { // multiple equal design values
                runtimeChildIndexForKey.splice(runtimeChildIndexForKey.indexOf(event.detail.runtimeChildIndex), 1);
                if (runtimeChildIndexForKey.length === 1) this.runtimeChildIndexes[keyInRuntimeArray] = runtimeChildIndexForKey[0];
            }
        }
        event.stopPropagation();
        return false;
    }

    // handle event: child tree was now linked or some child needs extra indexes; runtime indexes can be computed starting at the given child;
    // recalculate Parent Servoy Tab Sequence
    @HostListener('recalculatePSTS', ['$event.detail.designChildIndex', '$event.detail.initialRootRecalculate', '$event'])
    recalculateIndexesHandler(designChildIndex: number, initialRootRecalculate: boolean, event?: Event): boolean {
        if (this.designTabSeq === -2 || designChildIndex === -2) {
            if (event) event.stopPropagation();
            return false;
        }

        if (!this.initializing) {
            // a new child is ready/linked; recalculate tab indexes for it and after it
            const startIdx = (this.designChildIndexToArrayPosition && this.designChildIndexToArrayPosition[designChildIndex] !== undefined) ?
                this.designChildIndexToArrayPosition[designChildIndex] : 0;
            this.recalculateChildRuntimeIndexesStartingAt(startIdx, false);
        } else if (initialRootRecalculate) {
            // this is $rootScope (one $parent extra cause the directive creates it); we always assume a sabloTabseq directive is bound to it;
            // now that it is linked we can do initial calculation of tre
            this.runtimeIndex.startIndex = this.runtimeIndex.nextAvailableIndex = 1;
            this.recalculateChildRuntimeIndexesStartingAt(0, true);
        } // else wait for parent tabSeq directives to get linked as well

        if(event) event.stopPropagation();
        return false;
    }

    @HostListener('disableTabseq', ['$event'])
    disableTabseq(event: CustomEvent<boolean>): boolean {
        this.isEnabled = false;
        this.recalculateChildRuntimeIndexesStartingAt(0, true);
        event.stopPropagation();
        return false;
    }

    @HostListener('enableTabseq', ['$event'])
    enableTabseq(event: CustomEvent<boolean>): boolean {
        this.isEnabled = true;
        this.triggerRecalculatePSTSInParent(0);
        event.stopPropagation();
        return false;
    }

    ngOnInit(): void {
        // called by angular in parents first then in children
        if (!this.designTabSeq) this.designTabSeq = 0;

        this.initializing = true;
        this.isEnabled = true;

        // runtime index -1 == SKIP focus traversal in browser
        // runtime index  0 == DEFAULT == design tab seq 0 (not tabIndex attr set to element or it's children)
        this.runtimeIndex = { startIndex: -1, nextAvailableIndex: -1, sablotabseq: this };
        // -1 runtime initially for all (in case some node in the tree has -2 design (skip) and children have >= 0,
        // at runtime all children should be excluded as wel)
        this.updateCurrentDomElTabIndex();

        // check to see if this is the top-most tabSeq container
        if (this.config && this.config.root) {
            // it's root tab seq container (so no parent); just do initial tree calculation
            this.recalculateIndexesHandler(0, true);
        } else {
            if (this.designTabSeq !== -2) {
                this.triggerRegisterCSTSInParent(this.designTabSeq, this.runtimeIndex);
            }
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        const change = changes['designTabSeq'];
        if (change && !change.firstChange) {
            if (!(this.config && this.config.root)) {
                if (change.previousValue !== -2) this.triggerUnregisterCSTSInParent(change.previousValue as number, this.runtimeIndex);
                if (!this.designTabSeq) this.designTabSeq = 0;
                this.runtimeIndex.startIndex = -1;
                this.runtimeIndex.nextAvailableIndex = -1;
                this.initializing = true;

                if (this.designTabSeq !== -2) {
                    this.triggerRegisterCSTSInParent(this.designTabSeq, this.runtimeIndex);
                    // here we could send [0] instead of [designTabSeq] - it would potentially calculate more but start again from first parent available index,
                    // not higher index (the end user behavior being the same)
                    this.triggerRecalculatePSTSInParent(this.designTabSeq);
                } else {
                    this.updateCurrentDomElTabIndex(); // -1 runtime
                }
            }
        }
    }

    recalculateChildRuntimeIndexesStartingAt(posInDesignArray: number /*inclusive*/, triggeredByParent: boolean): void {
        if (this.designTabSeq === -2) return;

        if (!this.isEnabled || this.runtimeIndex.startIndex === -1) {

            this.runtimeIndex.nextAvailableIndex = this.runtimeIndex.startIndex;
            this.runtimeIndex.startIndex = -1;
        } else if (this.designTabSeq === 0) {
            // this element doesn't set any tabIndex attribute (default behavior)
            this.runtimeIndex.nextAvailableIndex = this.runtimeIndex.startIndex;
            this.runtimeIndex.startIndex = 0;
        } else if (this.runtimeIndex.startIndex === 0) {
            this.runtimeIndex.nextAvailableIndex = 0;
        } else if (this.runtimeIndex.nextAvailableIndex === -1) {
            const reservedGap = (this.config && this.config.reservedGap) ? this.config.reservedGap : 0;
            this.runtimeIndex.nextAvailableIndex = this.runtimeIndex.startIndex + reservedGap;
        }

        if (posInDesignArray === 0) this.updateCurrentDomElTabIndex();

        let recalculateStartIndex = this.runtimeIndex.startIndex;
        if (posInDesignArray > 0 && posInDesignArray - 1 < this.designChildTabSeq.length) {
            const runtimeCI = this.runtimeChildIndexes[this.designChildTabSeq[posInDesignArray - 1]]; // this can be an array in case of multiple equal design indexes being siblings
            recalculateStartIndex = isRuntimeIndexArray(runtimeCI) ? runtimeCI[runtimeCI.length - 1].nextAvailableIndex : runtimeCI.nextAvailableIndex;
        }

        for (let i = posInDesignArray; i < this.designChildTabSeq.length; i++) {
            const childRuntimeIndex = this.runtimeChildIndexes[this.designChildTabSeq[i]];
            if (isRuntimeIndexArray(childRuntimeIndex)) {
                // multiple equal design time indexes as siblings
                let max = recalculateStartIndex;
                for (const oneChildRuntimeIndex of childRuntimeIndex) {
                    oneChildRuntimeIndex.startIndex = recalculateStartIndex;
                    // call recalculate on whole child; normally it only makes sense for same index siblings
                    // if they are not themselfes containers, just apply the given value
                    oneChildRuntimeIndex.sablotabseq.recalculateChildRuntimeIndexesStartingAt(0, true);
                    if (max < oneChildRuntimeIndex.nextAvailableIndex)
                        max = oneChildRuntimeIndex.nextAvailableIndex;
                }
                recalculateStartIndex = max;
            } else {
                childRuntimeIndex.startIndex = recalculateStartIndex;
                childRuntimeIndex.sablotabseq.recalculateChildRuntimeIndexesStartingAt(0, true); // call recalculate on whole child
                recalculateStartIndex = childRuntimeIndex.nextAvailableIndex;
            }
        }

        if (this.initializing) this.initializing = undefined; // it's now considered initialized as first runtime index caluculation is done

        let parentRecalculateNeeded: boolean;
        if (this.runtimeIndex.startIndex !== 0 && this.runtimeIndex.startIndex !== -1) {
            const ownTabIndexBump = this.hasOwnTabIndex() ? 1 : 0;
            parentRecalculateNeeded = (this.runtimeIndex.nextAvailableIndex < recalculateStartIndex + ownTabIndexBump);
            const reservedGap = (this.config && this.config.reservedGap) ? this.config.reservedGap : 0;
            if (parentRecalculateNeeded) this.runtimeIndex.nextAvailableIndex = recalculateStartIndex + reservedGap + ownTabIndexBump;
        } else {
            // start index 0 means default (no tabIndex attr. set)
            parentRecalculateNeeded = false;
        }

        // if this container now needs more tab indexes than it was reserved; a recalculate on parent needs to be triggered in this case
        if (parentRecalculateNeeded && !triggeredByParent) this.triggerRecalculatePSTSInParent(this.designTabSeq);
    }

    hasOwnTabIndex(): boolean {
        return (!this.config || !(this.config.container || this.config.root));
    }

    updateCurrentDomElTabIndex(): void {
        if (this.hasOwnTabIndex()) {
            if (this.runtimeIndex.startIndex !== 0) {
                this.setDOMTabIndex(this.runtimeIndex.startIndex);
            } else {
                this.setDOMTabIndex(undefined);
            }
        }
    }

    setDOMTabIndex(tabindex: number | undefined): void {
        if (this.config && this.config.tabSeqSetter) this.config.tabSeqSetter.setTabIndex(tabindex);
        else this._elemRef.nativeElement.setAttribute('tabindex', `${tabindex}`);
    }

    triggerRecalculatePSTSInParent(designChildIndex: number) {
        this.triggerInParent('recalculatePSTS', { designChildIndex, initialRootRecalculate: false });
    }
    
    triggerRegisterCSTSInParent(designChildIndex: number, runtimeChildIndex: RuntimeIndex) {
        this.triggerInParent('registerCSTS', { designChildIndex, runtimeChildIndex });
    }
    
    triggerUnregisterCSTSInParent(designChildIndex: number, runtimeChildIndex: RuntimeIndex) {
        this.triggerInParent('unregisterCSTS', { designChildIndex, runtimeChildIndex });
    }

    triggerInParent(eventName: string, arg: unknown): void {
        this._elemRef.nativeElement.parentNode.dispatchEvent(new CustomEvent(eventName, {
            bubbles: true,
            detail: arg
        }));
    }

    ngOnDestroy(): void {
        // unregister current tabSeq from parent tabSeq container
        if(this._elemRef.nativeElement.parentNode) {
            this.triggerUnregisterCSTSInParent(this.designTabSeq, this.runtimeIndex);
        }
    }
}

export interface SabloTabseqConfig {
    /** If this is the top-most tabSeq container. */
    root?: boolean;

    /** If this is a tabSeq container.
     *  Child DOM elements of this element are considered to be traversed by tab sequence at the 'design' tab index value of this container.
     *  So at runtime all child DOM elements of this element will get a tabIndex value that sets them between the parent element's siblings,
     *  according to the 'design' tab index value of the parent and it's siblings.
     */
    container?: boolean;

    /** Tells sablo-tabseq that it should 'reserve' a number of tabIndexes for that container on top of the ones it currently needs.
     *  That can help later on, if more elements are added to that container and it needs more tabIndexes assigned - it can just use them
     *  without recalculating the tabIndexes of the parents (so less calculations to be done in the browser) - at least for a while,
     *  until it runs out of reserved indexes.
     */
    reservedGap?: number;

    /**
     * By default 'tabindex' is added to the element of the 'sabloTabseq' directive. Using this helper function, that is called when
     * the 'tabindex' is about to be set, it is possible to add the 'tabindex' on a different (nested) element.
     */
    tabSeqSetter?: { setTabIndex: (index: number) => void };
}

interface RuntimeIndex { startIndex: number, nextAvailableIndex: number, sablotabseq: SabloTabseq }

const isRuntimeIndexArray = (o: RuntimeIndex | Array<RuntimeIndex>): o is Array<RuntimeIndex> => {
  return (o as Array<RuntimeIndex>).push !== undefined;
}