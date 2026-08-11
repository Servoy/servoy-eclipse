import { Component, Input, model, ContentChild, TemplateRef, SimpleChanges, ChangeDetectionStrategy} from '@angular/core';

import { ServoyBaseComponent } from '@servoy/public';

import { Tab } from '../tabpanel/basetabpanel';

@Component( {
    selector: 'servoydefault-splitpane',
    templateUrl: './splitpane.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultSplitpane extends ServoyBaseComponent<HTMLDivElement> {

    @Input() onChangeMethodID: any;

    @Input() background: any;
    @Input() borderType: any;
    @Input() enabled: any;
    @Input() fontType: any;
    @Input() foreground: any;
    @Input() horizontalAlignment: any;
    @Input() location: any;
    @Input() readOnly: any;
    @Input() selectedTabColor: any;
    @Input() size: any;
    @Input() styleClass: any;
    @Input() tabOrientation: any;
    @Input() tabSeq: any;
    @Input() tabs!: Array<Tab>;
    @Input() transparent: any;

    readonly divLocation = model<any>(undefined as any);
    @Input() divSize: any;
    @Input() pane1MinSize: any;
    @Input() pane2MinSize: any;
    @Input() resizeWeight: any;


    @ContentChild( TemplateRef, {static: true} )
    templateRef!: TemplateRef<any>;

    private leftTab!: Tab;
    private rightTab!: Tab;

    svyOnInit() {
        if (this.resizeWeight == undefined) this.resizeWeight = 0;
        if (this.pane1MinSize == undefined) this.pane1MinSize = 30;
        if (this.pane2MinSize == undefined) this.pane2MinSize = 30;
        if (this.divSize == undefined) this.divSize = 5;
        super.svyOnInit();
    }

    svyOnChanges(changes: SimpleChanges) {
        if(changes['tabs']) {
            this.leftTab = this.tabSwitch(this.leftTab, (this.tabs ? this.tabs[0] : null) as Tab, 0);
            this.rightTab = this.tabSwitch(this.rightTab, (this.tabs ? this.tabs[1] : null) as Tab, 1);
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
        if (this.onChangeMethodID) this.onChangeMethodID(-1, new Event('change'));
    }

    getRightTab() {
        return this.rightTab?this.rightTab.containsFormId:null;
    }

    getLeftTab() {
        return this.leftTab?this.leftTab.containsFormId:null;
    }

    private tabSwitch(oldTab: Tab,newTab: Tab, index : number): Tab {
        if (oldTab && oldTab.containsFormId && newTab && newTab.containsFormId) {
            const promise = this.servoyApi.hideForm(oldTab.containsFormId,oldTab.relationName,undefined,newTab.containsFormId,newTab.relationName, index);
            promise.then((ok) => {
                if (!ok) {
                    // a splitpane can't block the hide so show should be called
                    this.servoyApi.formWillShow(newTab.containsFormId,newTab.relationName, index).
                        finally( () => this.cdRef.detectChanges());
                }
            });
        } else if (oldTab && oldTab.containsFormId) {
            this.servoyApi.hideForm(oldTab.containsFormId,oldTab.relationName);
        } else if (newTab && newTab.containsFormId) {
            this.servoyApi.formWillShow(newTab.containsFormId,newTab.relationName, index).
                        finally( () => this.cdRef.detectChanges());
        }
        return newTab;
    }
}
