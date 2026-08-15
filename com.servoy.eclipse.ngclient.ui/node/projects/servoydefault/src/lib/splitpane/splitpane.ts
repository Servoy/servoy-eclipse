import { SabloTabseq, ServoyBaseComponent } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, model, input, contentChild, TemplateRef, SimpleChanges, ChangeDetectionStrategy} from '@angular/core';


import { Tab } from '../tabpanel/basetabpanel';
import { BGSplitter } from './bg_splitter/bg_splitter.component';
import { BGPane } from './bg_splitter/bg_pane.component';

@Component( {
    selector: 'servoydefault-splitpane',
    templateUrl: './splitpane.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, SabloTabseq, BGSplitter, BGPane]
} )
export class ServoyDefaultSplitpane extends ServoyBaseComponent<HTMLDivElement> {

    readonly onChangeMethodID = input<any>(undefined);

    readonly background = input<any>(undefined);
    readonly borderType = input<any>(undefined);
    readonly enabled = input<any>(undefined);
    readonly fontType = input<any>(undefined);
    readonly foreground = input<any>(undefined);
    readonly horizontalAlignment = input<any>(undefined);
    readonly location = input<any>(undefined);
    readonly readOnly = input<any>(undefined);
    readonly selectedTabColor = input<any>(undefined);
    readonly size = input<any>(undefined);
    readonly styleClass = input<any>(undefined);
    readonly tabOrientation = input<any>(undefined);
    readonly tabSeq = input<any>(undefined);
    readonly tabs = input<Array<Tab>>(undefined!);
    readonly transparent = input<any>(undefined);

    readonly divLocation = model<any>(undefined as any);
    readonly divSize = input<any>(5);
    readonly pane1MinSize = input<any>(30);
    readonly pane2MinSize = input<any>(30);
    readonly resizeWeight = input<any>(0);


    readonly templateRef = contentChild(TemplateRef);

    private leftTab!: Tab;
    private rightTab!: Tab;

    svyOnInit() {
        super.svyOnInit();
    }

    svyOnChanges(changes: SimpleChanges) {
        if(changes['tabs']) {
            this.leftTab = this.tabSwitch(this.leftTab, (this.tabs() ? this.tabs()[0] : null) as Tab, 0);
            this.rightTab = this.tabSwitch(this.rightTab, (this.tabs() ? this.tabs()[1] : null) as Tab, 1);
        }
        super.svyOnChanges(changes);
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'styleClass':
                        if (change.previousValue) {
                            const array = change.previousValue.trim().split(' ');
                            array.filter((elementStyleClass: string) => elementStyleClass !== '').forEach(
                                (elementStyleClass: string) => this.renderer.removeClass(this.getNativeElement(), elementStyleClass)
                            );
                        }
                        if (change.currentValue) {
                            const array = change.currentValue.trim().split(' ');
                            array.filter((elementStyleClass: string) => elementStyleClass !== '').forEach(
                                (elementStyleClass: string) => this.renderer.addClass(this.getNativeElement(), elementStyleClass)
                            );
                        }
                    break;
                }
            }
        }
    }

    onChange( location: any ) {
        this.divLocation.set(location);
        if (this.onChangeMethodID()) this.onChangeMethodID()(-1, new Event('change'));
    }

    getRightTab() {
        return this.rightTab?this.rightTab.containsFormId:null;
    }

    getLeftTab() {
        return this.leftTab?this.leftTab.containsFormId:null;
    }

    private tabSwitch(oldTab: Tab,newTab: Tab, index : number): Tab {
        if (oldTab && oldTab.containsFormId && newTab && newTab.containsFormId) {
            const promise = this.servoyApi().hideForm(oldTab.containsFormId,oldTab.relationName,undefined,newTab.containsFormId,newTab.relationName, index);
            promise.then((ok) => {
                if (!ok) {
                    // a splitpane can't block the hide so show should be called
                    this.servoyApi().formWillShow(newTab.containsFormId,newTab.relationName, index).
                        finally( () => this.cdRef.detectChanges());
                }
            });
        } else if (oldTab && oldTab.containsFormId) {
            this.servoyApi().hideForm(oldTab.containsFormId,oldTab.relationName);
        } else if (newTab && newTab.containsFormId) {
            this.servoyApi().formWillShow(newTab.containsFormId,newTab.relationName, index).
                        finally( () => this.cdRef.detectChanges());
        }
        return newTab;
    }
}
