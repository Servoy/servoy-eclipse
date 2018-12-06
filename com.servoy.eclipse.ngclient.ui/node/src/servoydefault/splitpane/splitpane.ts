import { Component, ViewChild, Input, Output, EventEmitter, ContentChild, TemplateRef, OnInit, OnChanges, SimpleChanges, Renderer2} from '@angular/core';

import { ServoyApi, ServoyBaseComponent } from '../../ngclient/servoy_public'

import { Tab } from '../tabpanel/basetabpanel'

@Component( {
    selector: 'servoydefault-splitpane',
    templateUrl: './splitpane.html'
} )
export class ServoyDefaultSplitpane extends ServoyBaseComponent implements  OnInit, OnChanges {

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
    @Input() tabs: Array<Tab>
    @Input() transparent;
    @Input() visible;

    @Input() divLocation;
    @Output() divLocationChange = new EventEmitter();
    @Input() divSize;
    @Input() pane1MinSize;
    @Input() pane2MinSize
    @Input() resizeWeight; 


    @ContentChild( TemplateRef )
    templateRef: TemplateRef<any>;
    
    private leftTab:Tab;
    private rightTab:Tab;

    constructor(renderer:Renderer2) {
        super(renderer);
    }
    
    ngOnInit() {
        if (this.resizeWeight == undefined) this.resizeWeight = 0;
        if (this.pane1MinSize == undefined) this.pane1MinSize = 30;
        if (this.pane2MinSize == undefined) this.pane2MinSize = 30;
        if (this.divSize == undefined) this.divSize = 5;
    }
    
    ngOnChanges(changes: SimpleChanges) {
        if(changes["tabs"])
        {
            this.leftTab = this.tabSwitch(this.leftTab, this.tabs?this.tabs[0]:null);
            this.rightTab = this.tabSwitch(this.rightTab, this.tabs?this.tabs[1]:null);
        }
    }
    
    onChange( location ) {
        this.divLocation = location;
        this.divLocationChange.emit(this.divLocation);
        if (this.onChangeMethodID) this.onChangeMethodID(-1, new Event("change"));
    }
    
    getRightTab() {
        return this.rightTab?this.rightTab.containsFormId:null;
    }
    
    getLeftTab() {
        return this.leftTab?this.leftTab.containsFormId:null;
    }
    
    private tabSwitch(oldTab:Tab,newTab:Tab):Tab {
        if (oldTab && newTab) {
            const promise = this.servoyApi.hideForm(oldTab.containsFormId,oldTab.relationName,null,newTab.containsFormId,newTab.relationName);
            promise.then((ok) => {
                if (!ok) {
                    // a splitpane can't block the hide so show should be called
                    this.servoyApi.formWillShow(newTab.containsFormId,newTab.relationName);
                }
            })
        }
        else if (oldTab) {
            this.servoyApi.hideForm(oldTab.containsFormId,oldTab.relationName);
        }
        else if (newTab) {
            this.servoyApi.formWillShow(newTab.containsFormId,newTab.relationName);
        }
        return newTab;
    }
}
