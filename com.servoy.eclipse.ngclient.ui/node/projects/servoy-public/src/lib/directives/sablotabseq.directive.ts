import { Directive, Input, OnInit, ElementRef, HostListener, OnDestroy, ChangeDetectorRef, SimpleChanges, OnChanges } from '@angular/core';

@Directive({
    selector: '[sabloTabseq]',
    standalone: false
})
export class SabloTabseq implements OnInit, OnChanges, OnDestroy {

    @Input('sabloTabseq') designTabSeq: number;
    @Input('sabloTabseqConfig') config: SabloTabseqConfig;

    designChildIndexToArrayPosition = {};
    designChildTabSeq = []; // contains ordered numbers that will be keys in 'runtimeChildIndexes'; can have duplicates
    runtimeChildIndexes = {}; // map designChildIndex[i] -> runtimeIndex for child or designChildIndex[i] -> [runtimeIndex1, runtimeIndex2] in case there are multiple equal design time indexes
    runtimeIndex;
    initializing: boolean;
    isEnabled: boolean;

    constructor(private _elemRef: ElementRef, private cdRef: ChangeDetectorRef) {
    }

    // handle event: Child Servoy Tab Sequence registered
    @HostListener('registerCSTS', ['$event.detail[0]', '$event.detail[1]', '$event'])
    registerChildHandler(designChildIndex, runtimeChildIndex, event): boolean {
        if (this.designTabSeq === -2 || designChildIndex === -2) {
            this.recalculateIndexesHandler(designChildIndex ? designChildIndex : 0, false);
            event.stopPropagation();
            return false;
        }

        // insert it sorted
        let posInDesignArray = 0;
        for (let tz = 0; tz < this.designChildTabSeq.length && this.designChildTabSeq[tz] < designChildIndex; tz++) {
            posInDesignArray = tz + 1;
        }
        if (posInDesignArray === this.designChildTabSeq.length || this.designChildTabSeq[posInDesignArray] > designChildIndex) {
            this.designChildTabSeq.splice(posInDesignArray, 0, designChildIndex);

            // always keep in designChildIndexToArrayPosition[i] the first occurrance of design index i in the sorted designChildTabSeq array
            for (let tz = posInDesignArray; tz < this.designChildTabSeq.length; tz++) {
                this.designChildIndexToArrayPosition[this.designChildTabSeq[tz]] = tz;
            }
            this.runtimeChildIndexes[designChildIndex] = runtimeChildIndex;
        } else {
            // its == that means that we have dupliate design indexes; we treat this special - all same design index children as a list in one runtime index array cell
            if (!this.runtimeChildIndexes[designChildIndex].push) {
                this.runtimeChildIndexes[designChildIndex] = [this.runtimeChildIndexes[designChildIndex]];
            }
            this.runtimeChildIndexes[designChildIndex].push(runtimeChildIndex);
        }

        this.recalculateIndexesHandler(designChildIndex ? designChildIndex : 0, false);
        event.stopPropagation();
        return false;
    }

    @HostListener('unregisterCSTS', ['$event.detail[0]', '$event.detail[1]', '$event'])
    unregisterChildHandler(designChildIndex, runtimeChildIndex, event): boolean {
        if (this.designTabSeq === -2 || designChildIndex === -2) {
            event.stopPropagation();
            return false;
        }

        const posInDesignArray = this.designChildIndexToArrayPosition[designChildIndex];
        if (posInDesignArray !== undefined) {
            const keyInRuntimeArray = this.designChildTabSeq[posInDesignArray];
            const multipleEqualDesignValues = this.runtimeChildIndexes[keyInRuntimeArray].push;
            if (!multipleEqualDesignValues) {
                delete this.designChildIndexToArrayPosition[designChildIndex];
                for (const tmp in this.designChildIndexToArrayPosition) {
                    if (this.designChildIndexToArrayPosition[tmp] > posInDesignArray) this.designChildIndexToArrayPosition[tmp]--;
                }
                this.designChildTabSeq.splice(posInDesignArray, 1);
                delete this.runtimeChildIndexes[keyInRuntimeArray];
            } else {
                this.runtimeChildIndexes[keyInRuntimeArray].splice(this.runtimeChildIndexes[keyInRuntimeArray].indexOf(runtimeChildIndex), 1);
                if (this.runtimeChildIndexes[keyInRuntimeArray].length === 1) this.runtimeChildIndexes[keyInRuntimeArray] = this.runtimeChildIndexes[keyInRuntimeArray][0];
            }
        }
        event.stopPropagation();
        return false;
    }

    // handle event: child tree was now linked or some child needs extra indexes; runtime indexes can be computed starting at the given child;
    // recalculate Parent Servoy Tab Sequence
    @HostListener('recalculatePSTS', ['$event.detail[0]', '$event.detail[1]', '$event'])
    recalculateIndexesHandler(designChildIndex, initialRootRecalculate, event?): boolean {
        if (this.designTabSeq === -2 || designChildIndex === -2) {
            if(event) event.stopPropagation();
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
        this.trigger(this._elemRef.nativeElement.parentNode, 'recalculatePSTS', [0, false]);
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
                this.trigger(this._elemRef.nativeElement.parentNode, 'registerCSTS', [this.designTabSeq, this.runtimeIndex]);
            }
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        const change = changes['designTabSeq'];
        if (change && !change.firstChange) {
            if (!(this.config && this.config.root)) {
                if (change.previousValue !== -2) this.trigger(this._elemRef.nativeElement.parentNode, 'unregisterCSTS', [change.previousValue, this.runtimeIndex]);
                if (!this.designTabSeq) this.designTabSeq = 0;
                this.runtimeIndex.startIndex = -1;
                this.runtimeIndex.nextAvailableIndex = -1;
                this.initializing = true;

                if (this.designTabSeq !== -2) {
                    this.trigger(this._elemRef.nativeElement.parentNode, 'registerCSTS', [this.designTabSeq, this.runtimeIndex]);
                    // here we could send [0] instead of [designTabSeq] - it would potentially calculate more but start again from first parent available index,
                    // not higher index (the end user behavior being the same)
                    this.trigger(this._elemRef.nativeElement.parentNode, 'recalculatePSTS', [this.designTabSeq]);
                } else {
                    this.updateCurrentDomElTabIndex(); // -1 runtime
                }
            }
        }
    }

    recalculateChildRuntimeIndexesStartingAt(posInDesignArray /*inclusive*/, triggeredByParent): void {
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
            recalculateStartIndex = runtimeCI.push ? runtimeCI[runtimeCI.length - 1].nextAvailableIndex : runtimeCI.nextAvailableIndex;
        }

        for (let i = posInDesignArray; i < this.designChildTabSeq.length; i++) {
            const childRuntimeIndex = this.runtimeChildIndexes[this.designChildTabSeq[i]];
            if (childRuntimeIndex.push) {
                // multiple equal design time indexes as siblings
                let max = recalculateStartIndex;
                for (const k in childRuntimeIndex) {
                    if(childRuntimeIndex.hasOwnProperty(k)) {
                        childRuntimeIndex[k].startIndex = recalculateStartIndex;
                        // call recalculate on whole child; normally it only makes sense for same index siblings
                        // if they are not themselfes containers, just apply the given value
                        childRuntimeIndex[k].sablotabseq.recalculateChildRuntimeIndexesStartingAt(0, true);
                        if (max < childRuntimeIndex[k].nextAvailableIndex)
                            max = childRuntimeIndex[k].nextAvailableIndex;
                    }
                }
                recalculateStartIndex = max;
            } else {
                childRuntimeIndex.startIndex = recalculateStartIndex;
                childRuntimeIndex.sablotabseq.recalculateChildRuntimeIndexesStartingAt(0, true); // call recalculate on whole child
                recalculateStartIndex = childRuntimeIndex.nextAvailableIndex;
            }
        }

        if (this.initializing) this.initializing = undefined; // it's now considered initialized as first runtime index caluculation is done

        let parentRecalculateNeeded;
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
        if (parentRecalculateNeeded && !triggeredByParent) this.trigger(this._elemRef.nativeElement.parentNode, 'recalculatePSTS', [this.designTabSeq, false]);
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

    setDOMTabIndex(tabindex): void {
        if(this.config && this.config.tabSeqSetter) {
            this.config.tabSeqSetter.setTabIndex(tabindex);
        }
        else {
            this._elemRef.nativeElement.setAttribute("tabindex", tabindex);
        }
    }

    trigger(target, event: string, arg): void {
        const customEvent = new CustomEvent(event, {
            bubbles: true,
            detail: arg
          });
        target.dispatchEvent(customEvent);
    }

    ngOnDestroy(): void {
        // unregister current tabSeq from parent tabSeq container
        if(this._elemRef.nativeElement.parentNode) {
            this.trigger(this._elemRef.nativeElement.parentNode, 'unregisterCSTS', [this.designTabSeq, this.runtimeIndex]);
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
    tabSeqSetter?: {setTabIndex: (index: number)=>void};
}
