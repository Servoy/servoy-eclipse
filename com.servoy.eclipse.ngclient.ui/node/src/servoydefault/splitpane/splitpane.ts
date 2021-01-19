import { Component, Input, Output, EventEmitter, ContentChild, TemplateRef, ChangeDetectorRef, SimpleChanges, Renderer2, ChangeDetectionStrategy} from '@angular/core';

import { ServoyApi, ServoyBaseComponent } from '../../ngclient/servoy_public';

import { Tab } from '../tabpanel/basetabpanel';

@Component( {
    selector: 'servoydefault-splitpane',
    templateUrl: './splitpane.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultSplitpane extends ServoyBaseComponent<HTMLDivElement> {

    @Input() onChangeMethodID;

    @Input() background;
    @Input() borderType;
    @Input() enabled;
    @Input() fontType;
    @Input() foreground;
    @Input() horizontalAlignment;
    @Input() location;
    @Input() readOnly;
    @Input() selectedTabColor;
    @Input() size;
    @Input() styleClass;
    @Input() tabOrientation;
    @Input() tabSeq;
    @Input() tabs: Array<Tab>;
    @Input() transparent;

    @Input() divLocation;
    @Output() divLocationChange = new EventEmitter();
    @Input() divSize;
    @Input() pane1MinSize;
    @Input() pane2MinSize;
    @Input() resizeWeight;


    @ContentChild( TemplateRef, {static: true} )
    templateRef: TemplateRef<any>;

    private leftTab: Tab;
    private rightTab: Tab;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        if (this.resizeWeight == undefined) this.resizeWeight = 0;
        if (this.pane1MinSize == undefined) this.pane1MinSize = 30;
        if (this.pane2MinSize == undefined) this.pane2MinSize = 30;
        if (this.divSize == undefined) this.divSize = 5;
        super.svyOnInit();
    }

    svyOnChanges(changes: SimpleChanges) {
        if(changes['tabs']) {
            this.leftTab = this.tabSwitch(this.leftTab, this.tabs?this.tabs[0]:null);
            this.rightTab = this.tabSwitch(this.rightTab, this.tabs?this.tabs[1]:null);
        }
        super.svyOnChanges(changes);
    }

    onChange( location ) {
        this.divLocation = location;
        this.divLocationChange.emit(this.divLocation);
        if (this.onChangeMethodID) this.onChangeMethodID(-1, new Event('change'));
    }

    getRightTab() {
        return this.rightTab?this.rightTab.containsFormId:null;
    }

    getLeftTab() {
        return this.leftTab?this.leftTab.containsFormId:null;
    }

    private tabSwitch(oldTab: Tab,newTab: Tab): Tab {
        if (oldTab && oldTab.containsFormId && newTab && newTab.containsFormId) {
            const promise = this.servoyApi.hideForm(oldTab.containsFormId,oldTab.relationName,null,newTab.containsFormId,newTab.relationName);
            promise.then((ok) => {
                if (!ok) {
                    // a splitpane can't block the hide so show should be called
                    this.servoyApi.formWillShow(newTab.containsFormId,newTab.relationName).
                        finally( () => this.cdRef.detectChanges());
                }
            });
        } else if (oldTab && oldTab.containsFormId) {
            this.servoyApi.hideForm(oldTab.containsFormId,oldTab.relationName);
        } else if (newTab && newTab.containsFormId) {
            this.servoyApi.formWillShow(newTab.containsFormId,newTab.relationName).
                        finally( () => this.cdRef.detectChanges());
        }
        return newTab;
    }
}
