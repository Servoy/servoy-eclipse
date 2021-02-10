import { Directive, Input, OnInit, HostBinding, ElementRef, HostListener, OnDestroy } from '@angular/core';

@Directive({
    selector: '[sabloTabseq]'
})
export class SabloTabseq implements OnInit, OnDestroy {

    @Input('sabloTabseq') designTabSeq: number;
    @Input('sabloTabseqConfig') config: any;
    @HostBinding('attr.tabindex') tabindex: number;

    designChildIndexToArrayPosition = {};
    designChildTabSeq = []; // contains ordered numbers that will be keys in 'runtimeChildIndexes'; can have duplicates
    runtimeChildIndexes = {}; // map designChildIndex[i] -> runtimeIndex for child or designChildIndex[i] -> [runtimeIndex1, runtimeIndex2] in case there are multiple equal design time indexes
    runtimeIndex;
    initializing: boolean;
    isEnabled: boolean;

    constructor(private _elemRef: ElementRef) {
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
        this.tabindex = tabindex;
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
