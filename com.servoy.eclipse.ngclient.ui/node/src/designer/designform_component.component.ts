import {
    Component, Input, OnDestroy, OnChanges, SimpleChanges, ViewChild,
    TemplateRef, ElementRef, Renderer2, ChangeDetectionStrategy, ChangeDetectorRef, Inject, ViewEncapsulation,
    HostListener
} from '@angular/core';

import { FormCache, StructureCache, FormComponentCache, ComponentCache, IFormComponent } from '../ngclient/types';

import { ServoyService } from '../ngclient/servoy.service';

import { LoggerService, LoggerFactory, ServoyBaseComponent, WindowRefService } from '@servoy/public';

import { ServoyApi } from '../ngclient/servoy_api';
import { FormService } from '../ngclient/form.service';
import { DOCUMENT } from '@angular/common';
import { AbstractFormComponent } from '../ngclient/form/form_component.component';
import { TypesRegistry} from '../sablo/types_registry';

@Component({
    // eslint-disable-next-line
    selector: 'svy-designform',
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrls: ['./designform.css'],
    encapsulation: ViewEncapsulation.None,
    /* eslint-disable max-len */
    template: `
      <div *ngIf="formCache.absolute" [ngStyle]="getAbsoluteFormStyle()" class="svy-form" [ngClass]="formClasses"> <!-- main div -->
          <div *ngFor="let part of formCache.parts" [svyContainerStyle]="part" [svyContainerLayout]="part.layout" [svyContainerClasses]="part.classes"> <!-- part div -->
          </div>
          <div *ngFor="let item of formCache.partComponentsCache" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngClass]="{'invisible_element' : item.model.svyVisible === false, 'inherited_element' : item.model.svyInheritedElement}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component or formcomponent -->
          </div>
           <div *ngFor="let item of formCache.formComponents" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngClass]="{'invisible_element' : item.model.svyVisible === false, 'inherited_element' : item.model.svyInheritedElement}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component or formcomponent -->
          </div>
          <div *ngIf="draggedElementItem" [svyContainerStyle]="draggedElementItem" [svyContainerLayout]="draggedElementItem['layout']" class="svy-wrapper" style="position:absolute" id="svy_draggedelement">
                   <ng-template [ngTemplateOutlet]="getTemplate(draggedElementItem)" [ngTemplateOutletContext]="{ state:draggedElementItem, callback:this }"></ng-template>
          </div>
      </div>
      <div *ngIf="!formCache.absolute && name!=='VariantsForm'" class="svy-form svy-respform svy-overflow-auto" [ngClass]="formClasses"> <!-- main container div -->
            <div *ngIf="draggedElementItem" [svyContainerStyle]="draggedElementItem" [svyContainerLayout]="draggedElementItem['layout']" class="svy-wrapper" style="position:absolute" id="svy_draggedelement">
                   <ng-template [ngTemplateOutlet]="getTemplate(draggedElementItem)" [ngTemplateOutletContext]="{ state:draggedElementItem, callback:this }"></ng-template>
            </div>
            <ng-template *ngFor="let item of formCache.mainStructure?.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>  <!-- component or responsive div  -->
      </div>
      <div *ngIf="!formCache.absolute && name==='VariantsForm'" class="svy-form svy-respform svy-overflow-auto" [ngClass]="formClasses"> <!-- main container div -->
            <div (mousedown)="onVariantsMouseDown($event)" *ngFor="let item of formCache.mainStructure?.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" style="position:absolute">
                <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>
            </div>
      </div>
      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" [ngClass]="getNGClass(state)" class="svy-layoutcontainer">
               <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
          </div>
      </ng-template>
      <ng-template  #cssPositionContainer  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
            <div *ngFor="let item of state.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
            </div>
          </div>
      </ng-template>
      <!-- structure template generate start -->
      <!-- structure template generate end -->
      <ng-template  #formComponentAbsoluteDiv  let-state="state" >
          <div [ngClass]="{'invisible_element' : state.model.svyVisible === false}" [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" style="position:relative" class="svy-formcomponent">
               <div *ngFor="let item of state.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngClass]="{'invisible_element' : item.model.svyVisible === false}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component  -->
               </div>
               <div *ngIf="!state.items || !state.items.length">FormComponentContainer, select a form.</div>
          </div>
      </ng-template>
      <ng-template  #formComponentResponsiveDiv  let-state="state" >
          <servoycore-formcomponent-responsive-container [ngClass]="{'invisible_element' : state.model.svyVisible === false}" [items]="state.items" [class]="state.model.styleClass" [formComponent]="this"></servoycore-formcomponent-responsive-container>
          <div *ngIf="!state.items || !state.items.length">FormComponentContainer, select a form.</div>
      </ng-template>
      <!-- component template generate start -->
<ng-template #servoycoreDefaultLoadingIndicator let-callback="callback" let-state="state"><servoycore-defaultLoadingIndicator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [size]="state.model.size" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-defaultLoadingIndicator></ng-template>
<ng-template #servoycoreErrorbean let-callback="callback" let-state="state"><servoycore-errorbean  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [error]="state.model.error" [location]="state.model.location" [size]="state.model.size" [toolTipText]="state.model.toolTipText" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-errorbean></ng-template>
<ng-template #servoycoreFormcomponent let-callback="callback" let-state="state"><servoycore-formcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [location]="state.model.location" [size]="state.model.size" [styleClass]="state.model.styleClass" [width]="state.model.width" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-formcomponent></ng-template>
<ng-template #servoycoreFormcontainer let-callback="callback" let-state="state"><servoycore-formcontainer  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [location]="state.model.location" [relationName]="state.model.relationName" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [waitForData]="state.model.waitForData" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoycore-formcontainer></ng-template>
<ng-template #servoycoreListformcomponent let-callback="callback" let-state="state"><servoycore-listformcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [foundset]="state.model.foundset" [location]="state.model.location" [pageLayout]="state.model.pageLayout" [paginationStyleClass]="state.model.paginationStyleClass" [readOnly]="state.model.readOnly" [responsivePageSize]="state.model.responsivePageSize" [rowStyleClass]="state.model.rowStyleClass" [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider" [selectionClass]="state.model.selectionClass" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [onSelectionChanged]="callback.getHandler(state,'onSelectionChanged')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-listformcomponent></ng-template>
<ng-template #servoycoreNavigator let-callback="callback" let-state="state"><servoycore-navigator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [currentIndex]="state.model.currentIndex" [hasMore]="state.model.hasMore" [location]="state.model.location" [maxIndex]="state.model.maxIndex" [minIndex]="state.model.minIndex" [size]="state.model.size" [setSelectedIndex]="callback.getHandler(state,'setSelectedIndex')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-navigator></ng-template>
<ng-template #servoycoreSlider let-callback="callback" let-state="state"><servoycore-slider  [animate]="state.model.animate" [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [enabled]="state.model.enabled" [location]="state.model.location" [max]="state.model.max" [min]="state.model.min" [orientation]="state.model.orientation" [range]="state.model.range" [size]="state.model.size" [step]="state.model.step" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [onCreateMethodID]="callback.getHandler(state,'onCreateMethodID')" [onSlideMethodID]="callback.getHandler(state,'onSlideMethodID')" [onStartMethodID]="callback.getHandler(state,'onStartMethodID')" [onStopMethodID]="callback.getHandler(state,'onStopMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-slider></ng-template>
     <!-- component template generate end -->
   `
    /* eslint-enable max-len */
})

export class DesignFormComponent extends AbstractFormComponent implements OnDestroy, OnChanges, IFormComponent {

    @ViewChild('svyResponsiveDiv', { static: true }) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild('cssPositionContainer', { static: true }) readonly cssPositionContainer: TemplateRef<any>;
    // structure viewchild template generate start
    // structure viewchild template generate end
    @ViewChild('formComponentAbsoluteDiv', { static: true }) readonly formComponentAbsoluteDiv: TemplateRef<any>;
    @ViewChild('formComponentResponsiveDiv', { static: true }) readonly formComponentResponsiveDiv: TemplateRef<any>;

    // component viewchild template generate start
    @ViewChild('servoycoreSlider', { static: true }) readonly servoycoreSlider: TemplateRef<any>;
    @ViewChild('servoycoreErrorbean', { static: true }) readonly servoycoreErrorbean: TemplateRef<any>;
    @ViewChild('servoycoreListformcomponent', { static: true }) readonly servoycoreListformcomponent: TemplateRef<any>;
    @ViewChild('servoycoreFormcontainer', { static: true }) readonly servoycoreFormcontainer: TemplateRef<any>;
    // component viewchild template generate end

    @Input() name: string;

    formClasses: string[];
    formCache: FormCache;

    absolutFormPosition = {};
    showWireframe = false;
    draggedElementItem: ComponentCache | StructureCache;
    insertedCloneParent: StructureCache;
    insertedClone: ComponentCache | StructureCache;
    dragItem: Element;
    isVariantForm: boolean;
    variantsContainer: StructureCache;
    insertedVariants: Array<StructureCache>;

    private servoyApiCache: { [property: string]: ServoyApi } = {};
    private log: LoggerService;
    private designMode: boolean;
    private maxLevel = 3;
    private dropHighlight: string = null;
    private dropHighlightIgnoredIds: Array<string> = null;
    private allowedChildren: unknown;
    private variantContainerMargin = 2;
    private variantItemMargin = 10;
    private variantsLoaded = false;
    
    private leftPos: Map<string, number> = new Map();
    private rightPos: Map<string, number> = new Map();
    private topPos: Map<string, number> = new Map();
    private bottomPos : Map<string, number> = new Map();
    private middleV: Map<string, number> = new Map();
    private middleH : Map<string, number> = new Map();
    private rectangles : DOMRect[];
    private element: Element;
    
    private snapThreshold: number = 0;
    private equalDistanceThreshold: number = 0;

    constructor(private formservice: FormService,
            private servoyService: ServoyService, logFactory: LoggerFactory,
            private changeHandler: ChangeDetectorRef,
            private el: ElementRef, protected renderer: Renderer2,
            @Inject(DOCUMENT) private document: Document,
            private windowRefService: WindowRefService,
            private typesRegistry: TypesRegistry) {

        super(renderer);
        formservice.setDesignerMode();
        this.log = logFactory.getLogger('FormComponent');

        this.isVariantForm = (this.name === 'VariantsForm');
        this.windowRefService.nativeWindow.addEventListener('message', (event) => {
            if (event.data.id === 'createElement') {
                const elWidth = event.data.model.size ? event.data.model.size.width : 200;
                const elHeight = event.data.model.size ? event.data.model.size.height : 100;
                const model = { width: elWidth + 'px', height: elHeight + 'px' };
                model['top'] = '-200px';
                model['left'] = '-200px';
                const model_inserted = { width: elWidth + 'px', height: elHeight + 'px' };
                if (event.data.type === 'layout') {
                    //we are in responsive layout
                    this.draggedElementItem = new StructureCache(event.data.model.tagname, event.data.model.classes, event.data.attributes, null ,null, false, model_inserted);
                    this.insertedClone = new StructureCache(event.data.model.tagname, event.data.model.classes, event.data.attributes, null, 'insertedClone', false, model_inserted);
                    if (event.data.children) {
                        event.data.children.forEach(child => {
                            (this.draggedElementItem as StructureCache).addChild(new StructureCache(child.model.tagName, child.model.classes, child.attributes));
                            (this.insertedClone as StructureCache).addChild(new StructureCache(child.model.tagName, child.model.classes, child.attributes));
                        });
                    }
                } else {
                    this.draggedElementItem = new ComponentCache('dragged_element', event.data.name, undefined, [], model, typesRegistry).initForDesigner(event.data.model);
                    this.insertedClone = new ComponentCache(event.data.model.tagname, event.data.name, undefined, [], model_inserted,
                                                typesRegistry).initForDesigner(event.data.model); // TODO only in responsive
                }
                this.designMode = this.showWireframe;
                this.showWireframe = true;
            }
            if (event.data.id === 'sendVariantsSize') {
                this.sendVariantSizes();
            }
            if (event.data.id === 'createVariants') {
                this.variantsLoaded = false;

                const attributes = {};
                attributes['designclass'] =  'variant_item';
                attributes['svy-title'] = 'flex-item';

                if (!this.variantsContainer) {
                    this.variantsContainer = this.formCache.getLayoutContainer('variants-responsive-grid');
                }

                this.insertedVariants = new Array<StructureCache>();

                const variants = event.data.variants;
                let index = 0;
                for(const variant of variants) {
                    const variantAttributes = Object.assign({}, attributes);
                    const variantId = 'variantId_' + index++;
                    variantAttributes['svy-id'] = variantId;
                    const layout = { width: variant.width + 'px', height: variant.height + 'px' };

                    const variantElement = new ComponentCache(null, event.data.name, undefined, [], layout, typesRegistry);
                    variantElement.initForDesigner(JSON.parse(JSON.stringify(event.data.model).slice()));

                    const componentModel = variantElement.model;
                    componentModel.variant = variant.classes; // this is hardcoded property name "variant" should be changed to really get the variant property
                    componentModel._variantName = variant.name;
                    componentModel.size.width = variant.width;
                    componentModel.size.height = variant.height;
                    componentModel.text = variant.displayName;
                    componentModel.id = variant.name;

                    this.insertedClone = new StructureCache(null, ['flex-item'], variantAttributes , null, variantId);
                    this.insertedClone.addChild(variantElement);
                    this.variantsContainer.addChild(this.insertedClone, null);
                    this.insertedVariants.push(this.insertedClone);
                }

                this.designMode = this.showWireframe;
                this.showWireframe = true;
                this.variantsLoaded = true;
            }
            if (event.data.id === 'destroyVariants') {

                if (this.insertedVariants) {
                    if (!this.variantsContainer) {
                        this.variantsContainer = this.formCache.getLayoutContainer('variants-responsive-grid');
                    }
                    if (this.insertedVariants) {
                        for (const insertedVariant of this.insertedVariants) {//need to remove previous existing variants
                            this.variantsContainer.removeChild(insertedVariant);
                        }
                    }
                }

                this.insertedVariants = null;
                this.variantsLoaded = false;
                this.showWireframe = this.designMode;

            }
            if (event.data.id === 'createDraggedComponent') {
                this.insertedClone = this.formCache.getLayoutContainer(event.data.uuid);
                if (this.insertedClone) {
                    if (event.data.dragCopy) {
                        const parent = this.insertedClone.parent;
                        this.insertedClone = new StructureCache(this.insertedClone.tagname, this.insertedClone.classes, this.insertedClone.attributes, this.insertedClone.items);
                        parent.addChild(this.insertedClone);
                    }
                    this.draggedElementItem = new StructureCache(this.insertedClone.tagname, this.insertedClone.classes, Object.assign({}, this.insertedClone.attributes), this.insertedClone.items);
                    this.draggedElementItem.attributes['svy-id'] = 'clone';

                } else {
                    //if it's not a layout it must be a component
                    this.insertedClone = this.formCache.getComponent(event.data.uuid);
                    const oldModel = this.insertedClone.model;
                    if (event.data.dragCopy) {
                        const parent = this.insertedClone.parent;
                        this.insertedClone = new ComponentCache(this.insertedClone.name + 'clone', this.insertedClone.specName, this.insertedClone.type, this.insertedClone.handlers,
                            this.insertedClone.layout, this.typesRegistry).initForDesigner(oldModel);

                        parent.addChild(this.insertedClone);
                    }

                    this.draggedElementItem = new ComponentCache('dragged_element', this.insertedClone.specName, this.insertedClone.type, this.insertedClone.handlers, this.insertedClone.layout,
                                                    this.typesRegistry).initForDesigner(oldModel);
                }
                this.insertedCloneParent = this.insertedClone.parent;
                this.designMode = this.showWireframe;
                this.showWireframe = true;
            }
            if (event.data.id === 'insertDraggedComponent') {
                if (this.insertedCloneParent) this.insertedCloneParent.removeChild(this.insertedClone);
                this.insertedCloneParent = null;
                let beforeChild = null;
                if (event.data.insertBefore) {
                    beforeChild = this.formCache.getComponent(event.data.insertBefore);
                    if (beforeChild == null) beforeChild = this.formCache.getLayoutContainer(event.data.insertBefore);
                }

                if (event.data.dropTarget) {
                    this.insertedCloneParent = this.formCache.getLayoutContainer(event.data.dropTarget);
                } else if (!this.formCache.absolute) {
                    if (this.formCache.mainStructure == null) {
                        this.formCache.mainStructure = new StructureCache(null, null);
                    }
                    if (this.insertedCloneParent !== this.formCache.mainStructure) {
                        this.insertedCloneParent = this.formCache.mainStructure;
                    }
                }
                if (this.insertedCloneParent) this.insertedCloneParent.addChild(this.insertedClone, beforeChild);
            }
            if (event.data.id === 'removeDragCopy') {
                if (this.insertedCloneParent) this.insertedCloneParent.removeChild(this.insertedClone);
                this.insertedClone = this.formCache.getLayoutContainer(event.data.uuid);
                if (!this.insertedClone) {
                    this.insertedClone = this.formCache.getComponent(event.data.uuid);
                }
                this.insertedCloneParent.addChild(this.insertedClone, event.data.insertBefore);
            }
            if (event.data.id === 'destroyElement') {
                if (this.draggedElementItem || this.insertedCloneParent || this.insertedClone) {
                    this.draggedElementItem = null;
                    if (!event.data.existingElement && this.insertedCloneParent) this.insertedCloneParent.removeChild(this.insertedClone);
                    this.insertedCloneParent = null;
                    this.insertedClone = null;
                    this.showWireframe = this.designMode;
                }
            }
            if (event.data.id === 'showWireframe') {
                const changed = this.showWireframe !== event.data.value;
                this.showWireframe = event.data.value;
                this.windowRefService.nativeWindow.parent.postMessage({ id: 'renderGhosts', formname : this.name }, '*');
            }
            if (event.data.id === 'maxLevel') {
                this.maxLevel = parseInt(event.data.value, 10);
            }
            if (event.data.id === 'dropHighlight') {
                this.dropHighlight = event.data.value ? event.data.value.dropHighlight : null;
                this.dropHighlightIgnoredIds = event.data.value ? event.data.value.dropHighlightIgnoredIds : null;
            }
            if (event.data.id === 'allowedChildren') {
                this.allowedChildren = event.data.value;
            }
            if (event.data.id === 'snapThresholds') { 
                this.snapThreshold = parseInt(event.data.value.alignment, 0);
                this.equalDistanceThreshold = parseInt(event.data.value.distance, 0);
            }
            if (event.data.id === 'getSnapTarget') {
            
                if (this.leftPos.size == 0) {
					this.rectangles = [];
					this.element = this.document.elementsFromPoint(event.data.p1.x, event.data.p1.y).find(e => e.getAttribute('svy-id'));
					const uuid = this.element?.getAttribute('svy-id');
                    for (let comp of this.formCache.componentCache.values()) {
                        if (comp.name == '') continue;
                        const id = comp.name;
                        const bounds = this.document.querySelector("[svy-id='"+id+"']").getBoundingClientRect();
                        this.leftPos.set(id, bounds.left);
                        this.rightPos.set(id, bounds.right);
                        this.topPos.set(id, bounds.top);
                        this.bottomPos.set(id, bounds.bottom);
                        this.middleV.set(id, (bounds.top + bounds.bottom)/2);
                        this.middleH.set(id, (bounds.left + bounds.right)/2);
                        if (id !== uuid) this.rectangles.push(bounds);
                    }
                    const sortfn = (a, b) => a[1] - b[1];
                    this.leftPos = new Map([...this.leftPos].sort(sortfn));
                    this.rightPos = new Map([...this.rightPos].sort(sortfn));
                    this.topPos = new Map([...this.topPos].sort(sortfn));
                    this.bottomPos = new Map([...this.bottomPos].sort(sortfn));
                    this.middleH = new Map([...this.middleH].sort(sortfn));
                    this.middleV = new Map([...this.middleV].sort(sortfn));
                }
            	
                let props = this.getSnapProperties(event.data.p1);              
                this.windowRefService.nativeWindow.parent.postMessage({ id: 'snap', properties: props }, '*');
            }
            if (event.data.id === 'clearSnapCache') {
                this.leftPos = new Map();
                this.rightPos = new Map();
                this.topPos = new Map();
                this.bottomPos = new Map();
                this.middleV = new Map();
                this.middleH = new Map();
                this.rectangles = [];
                
            }
            this.detectChanges();
        });
    }
    
    private isSnapInterval(uuid, coordinate, posMap) {
        for (let [key, value] of posMap) {
            if (key === uuid) continue;
            if ((coordinate > value - this.snapThreshold) && (coordinate < value + this.snapThreshold)) {
                //return the first component id that matches the coordinate
                return {uuid: key};
            }
        }
        return null;        
    }
    
    private getDraggedElementRect(point: {x: number, y: number}): DOMRect {
         let rect = this.element?.getBoundingClientRect();
         if (!this.element?.getAttribute('svy-id') && this.draggedElementItem) {
			 const item = this.draggedElementItem as ComponentCache;
			 rect = new DOMRect(point.x, point.y, item.model.size.width, item.model.size.height);
		}
        return rect;
    } 
    
    private getSnapProperties(point: {x: number, y: number}) {
			this.element = this.document.elementsFromPoint(point.x, point.y).find(e => e.getAttribute('svy-id')); //TODO check
            const uuid = this.element?.getAttribute('svy-id');
            if (!uuid && !this.draggedElementItem) return { top: point.y, left: point.x, guides: []};
            
            const rect = this.getDraggedElementRect(point);
            let properties = {initPoint: point, top: point.y, left: point.x, cssPosition: {}, guides: []};
            let snapX = this.isSnapInterval(uuid, rect.left, this.leftPos);
            if (snapX?.uuid) {
                properties.left = this.leftPos.get(snapX.uuid);
            }
            else {
                snapX = this.isSnapInterval(uuid, rect.left, this.rightPos);
                if (snapX?.uuid) {
                    properties.left =  this.rightPos.get(snapX.uuid);
                }
            }
            
			let guideX;
            if (snapX) {
                properties.cssPosition['left'] = snapX;
                if (!properties.cssPosition['left']) properties.cssPosition['left'] = properties.left;
				guideX = properties.left;
            }
            else {
                snapX = this.isSnapInterval(uuid, rect.right, this.rightPos);
                guideX = this.rightPos.get(snapX?.uuid);
                properties.left = snapX ? this.rightPos.get(snapX.uuid) : properties.left;
                if (!snapX) { 
                    snapX = this.isSnapInterval(uuid, rect.right, this.leftPos);
                    if (snapX?.uuid) {
                        properties.left =  this.leftPos.get(snapX.uuid);
                        guideX = this.leftPos.get(snapX.uuid);
                    }
                }
                if (snapX){
                    properties.cssPosition['right'] = snapX;
                    properties.left -= rect.width;
                    if (!properties.cssPosition['right']) properties.cssPosition['right'] = properties.left;
                }
                else {
                    snapX = this.isSnapInterval(uuid, (rect.left+rect.right)/2, this.middleH);
                    if (snapX){
                        properties.cssPosition['middleH'] = snapX;
                        properties.left = this.middleH.get(snapX.uuid) - rect.width/2;
						guideX = this.middleH.get(snapX.uuid);
                    }
                }
            }

            if (snapX) {
                if (this.topPos.get(snapX.uuid) < rect.top) {                    
                    properties.guides.push(new Guide(guideX, this.topPos.get(snapX.uuid), 1, rect.bottom - this.topPos.get(snapX.uuid), 'snap' ));
                }
                else {
                    properties.guides.push(new Guide(guideX, rect.top, 1, this.topPos.get(snapX.uuid) - rect.top, 'snap' ));
                }
            }
                
			let guideY;
            let snapY = this.isSnapInterval(uuid, rect.top, this.topPos);
            if (snapY?.uuid) {
                properties.top = this.topPos.get(snapY.uuid);
            }
            else {
                snapY = this.isSnapInterval(uuid, rect.top, this.bottomPos);
                if (snapY?.uuid) {
                    properties.top = this.bottomPos.get(snapY.uuid);
                }
            }
            
            if (snapY) {
                properties.cssPosition['top'] = snapY;
                guideY = properties.top;
            }
            else {
                snapY = this.isSnapInterval(uuid, rect.bottom, this.bottomPos);
                if (snapY?.uuid) {
                    guideY = this.bottomPos.get(snapY.uuid);
                    properties.top = snapY ? this.bottomPos.get(snapY.uuid) : properties.top;
                }
                if (!snapY){
                    snapY = this.isSnapInterval(uuid, rect.bottom, this.topPos);
                    if (snapY?.uuid) {
                        properties.top = this.topPos.get(snapY.uuid);
                        guideY = this.topPos.get(snapY.uuid);
                    }
                }
                if (snapY) {
                    properties.cssPosition['bottom'] = snapY;
                    properties.top -= rect.height;
                }
                else {
                    snapY = this.isSnapInterval(uuid, (rect.top + rect.bottom)/2, this.middleV);
                    if (snapY?.uuid){
                        properties.cssPosition['middleV'] = snapY;
                        properties.top = this.middleV.get(snapY.uuid) - rect.height/2;
                        guideY = this.middleV.get(snapY.uuid);
                    }
                }
            }
            
            if (snapY) {
                if (this.leftPos.get(snapY.uuid) < rect.left) {
                    properties.guides.push(new Guide(this.leftPos.get(snapY.uuid), guideY, rect.right - this.leftPos.get(snapY.uuid) , 1, 'snap' ));
                }
                else {
                    properties.guides.push(new Guide(rect.left, guideY, this.leftPos.get(snapY.uuid) - rect.left , 1, 'snap' ));
                }
            }
            
            //equal distance guides
            this.addEqualDistanceVerticalGuides(rect, properties);
            this.addEqualDistanceHorizontalGuides(rect, properties);

            return properties.guides.length == 0 ? null : properties;
    }
    
    private addEqualDistanceVerticalGuides(rect: DOMRect, properties: any ): void {
		const overlappingX = this.getOverlappingRectangles(rect, 'x');
        for (let pair of overlappingX){
			const e1 = pair[0];
            const e2 = pair[1];   
            if (e2.top > e1.bottom && rect.top > e2.bottom) {
				const dist = e2.top - e1.bottom;
				if (Math.abs(dist - rect.top + e2.bottom) < this.equalDistanceThreshold) {
					properties.top = e2.bottom + dist;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				this.addVerticalGuides(e1, e2, r, dist, properties);
    				break;
				}
			}
			if (e2.top > e1.bottom && e1.top > rect.bottom) {
				const dist = e2.top - e1.bottom;
				if (Math.abs(dist - e1.top + rect.bottom) < this.equalDistanceThreshold) {
					properties.top = e1.top - dist - rect.height;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				this.addVerticalGuides(r, e1, e2, dist, properties);
    				break;
				}
			}
			if (e2.top > rect.bottom && rect.top > e1.bottom) {
				const dist = (e2.top - e1.bottom) / 2;
				if (Math.abs(e1.bottom + dist - rect.top - rect.height / 2) < this.equalDistanceThreshold) {
					properties.top = e1.bottom + dist - rect.height/2;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				this.addVerticalGuides(e1, r, e2, dist - rect.height/2, properties);
    				break;
				}
			}
		}
	}
	
	private addEqualDistanceHorizontalGuides(rect: DOMRect, properties: any ): void {
		const overlappingY = this.getOverlappingRectangles(rect, 'y');
        for (let pair of overlappingY){
			const e1 = pair[0];
            const e2 = pair[1];
            if (e2.left > e1.right && rect.left > e2.right) {
				const dist = e2.left - e1.right;
				if (Math.abs(dist - rect.left + e2.right) < this.equalDistanceThreshold) {                  
                	properties.left = e2.right + dist;
                 	const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
    				this.addHorizontalGuides(e1, e2, r, dist, properties);
    				break;
                }
			}
			if (e1.left > rect.right && e2.left > e1.right) {
				const dist = e2.left - e1.right;
				if (Math.abs(dist - rect.right + e1.left) < this.equalDistanceThreshold) {
               		properties.left = e1.left - dist - rect.width;
                   	const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
                   	this.addHorizontalGuides(r, e1, e2, dist, properties);
                   	break;
               	}
			}
			if (e2.left > rect.right && rect.left > e1.right)  {
				const dist = (e2.left - e1.right) / 2;   
               	if (Math.abs(e1.right + dist - rect.left - rect.width / 2) < this.equalDistanceThreshold) {
					properties.left = e1.right + dist - rect.width/2;
	    			const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
    				this.addHorizontalGuides(e1, r, e2, dist - rect.width/2, properties);
    				break;
				}
			}
		}
	}
    
    private getOverlappingRectangles(rect: DOMRect, axis: 'x' | 'y'): DOMRect[][] {
		let overlaps = this.rectangles.filter(r => this.isOverlap(rect, r, axis));
		const pairs: DOMRect[][] = [];
    	for (let i = 0; i < overlaps.length - 1; i++) {
        	for (let j = i + 1; j < overlaps.length; j++) {
            	if (overlaps[i] !== rect && overlaps[j] !== rect) {
                	pairs.push([overlaps[i], overlaps[j]]);
            	}
        	}
    	}
    	return pairs;
	}
    
    private isOverlap(rect: DOMRect, eRect: DOMRect, axis: 'x' | 'y'): boolean {
		if (axis === 'x') {
        	return (rect.left >= eRect.left && rect.left <= eRect.right) ||
               	(rect.right >= eRect.left && rect.right <= eRect.right);
    	} else if (axis === 'y') {
        	return (rect.top >= eRect.top && rect.top <= eRect.bottom) ||
               (rect.bottom >= eRect.top && rect.bottom <= eRect.bottom);
    	}
    	return false;
	}
	
	private getDOMRect(uuid: string) : DOMRect {
		return new DOMRect(this.leftPos.get(uuid), this.topPos.get(uuid), 
                	this.rightPos.get(uuid) - this.leftPos.get(uuid),
                	this.bottomPos.get(uuid) - this.topPos.get(uuid));
	}
	
	private addVerticalGuides(e1: DOMRect, e2: DOMRect, r: DOMRect, dist: number, properties: any): void {
	    const right = Math.max(r.right, e1.right, e2.right);
	   
	    properties.guides.push(new Guide(e1.right, e1.bottom, this.getGuideLength(right, e1.right), 1, 'dist'));
	    properties.guides.push(new Guide(right + 10, e1.bottom, 1, dist, 'dist'));
	    const len = this.getGuideLength(right, e2.right);
	    properties.guides.push(new Guide(e2.right, e2.top, len, 1, 'dist'));
	    properties.guides.push(new Guide(e2.right, e2.bottom, len, 1, 'dist'));
	    properties.guides.push(new Guide(right + 10, e2.bottom, 1, dist, 'dist'));
	    properties.guides.push(new Guide(r.right, r.top, this.getGuideLength(right, r.right), 1, 'dist'));
	}
	
	private addHorizontalGuides(e1: DOMRect, e2: DOMRect, r: DOMRect, dist: number, properties: any): void {
	    let bottom = Math.max(r.bottom, e1.bottom, e2.bottom);

	    properties.guides.push(new Guide(e1.right, e1.bottom, 1, this.getGuideLength(bottom, e1.bottom), 'dist'));
	    properties.guides.push(new Guide(e1.right, bottom + 10, dist, 1, 'dist'));
	    const len = this.getGuideLength(bottom, e2.bottom);
	    properties.guides.push(new Guide(e2.left, e2.bottom, 1, len, 'dist'));
	    properties.guides.push(new Guide(e2.right, e2.bottom, 1, len, 'dist'));
	    properties.guides.push(new Guide(e2.right, bottom + 10, dist, 1, 'dist'));
	    properties.guides.push(new Guide(r.left, r.bottom, 1, this.getGuideLength(bottom, r.bottom), 'dist'));
	}
	
	private getGuideLength(max: number, x: number): number {
		return max - x + 15;
	}

    sendVariantSizes() {
        const variants = this.document.getElementsByClassName('variant_item');
        if (!this.variantsLoaded || variants.length === 0) {
            return;
        }
        const container = this.document.getElementsByClassName('variants_container').item(0).parentElement;
        const formHeight = Math.ceil(container.getBoundingClientRect().height) + 2 * this.variantContainerMargin;
        let formWidth = 0;
        for (const variant of variants) {
            const variantChild = variant.firstChild.firstChild as Element;
            formWidth = Math.max(formWidth, variantChild.clientLeft + Math.ceil(variantChild.getBoundingClientRect().width) + 2*this.variantItemMargin);
        }
        formWidth += 2 * this.variantContainerMargin;
        this.windowRefService.nativeWindow.parent.parent.parent.postMessage({ id: 'resizePopover',
            formWidth,
            formHeight}, '*');
    }

    public onVariantsMouseDown(event: MouseEvent) {
        event.stopPropagation();
        let targetElement = document.elementFromPoint(event.pageX, event.pageY);
        if (targetElement.tagName === 'DIV' || targetElement.tagName === 'div') {
            return; //click outside any element
        }
        let variantId: string;
        while (targetElement) {
            if (targetElement.attributes.getNamedItem('svy-id')) {
                variantId = targetElement.attributes.getNamedItem('svy-id').nodeValue;
                break;
            } else {
                targetElement = targetElement.parentElement;
            }
        }
        const targetHeight = Math.ceil(targetElement.getBoundingClientRect().height); //height of the flex item
        targetElement = targetElement.firstElementChild;
        //not adding 3 px then the text content is getting clipped after drop
        const targetWidth = Math.ceil(targetElement.getBoundingClientRect().width) + 3;

        let selectedVariant: StructureCache;
        if (variantId) {
            for (const variant of this.insertedVariants) {
                if (variantId === variant.id) {
                    selectedVariant = variant;
                    break;
                }
            }
        }
        if (selectedVariant) {
            const model = Object.assign({}, (selectedVariant.items[0] as ComponentCache).model);
            model.size.width = targetWidth;
            model.size.height = targetHeight;
            this.windowRefService.nativeWindow.parent.postMessage({ id: 'onVariantMouseDown', pageX: event.pageX, pageY: event.pageY, model: selectedVariant.items[0].model}, '*');
        }
    }

    ngAfterViewInit() {
        this.windowRefService.nativeWindow.parent.postMessage({ id: 'afterContentInit', formname : this.name }, '*');
    }

    public detectChanges() {
        this.changeHandler.detectChanges();
    }

    public formCacheChanged(): void {
        this.detectChanges();
    }

    public formCacheRefresh(): void {
        this.initFormCache();
        this.detectChanges();
    }

    public getFormCache(): FormCache {
        return this.formCache;
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.name) {
            this.initFormCache();

            //this.sabloService.callService('formService', 'formLoaded', { formname: this.name }, true);
            this.renderer.setAttribute(this.el.nativeElement, 'name', this.name);

        }
    }

    ngOnDestroy() {
        this.formservice.destroy(this.name);
    }

    getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return item.tagname ? this[item.tagname] : (item.cssPositionContainer ? this.cssPositionContainer : this.svyResponsiveDiv);
        } else if (item instanceof FormComponentCache) {
            if (item.hasFoundset) return this.servoycoreListformcomponent;
            return item.responsive ? this.formComponentResponsiveDiv : this.formComponentAbsoluteDiv;
        } else {
            if (this[item.type] === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ('Template for ' + item.type + ' was not found, please check form_component template.')));
            }
            return this[item.type];
        }
    }

    getTemplateForLFC(state: ComponentCache): TemplateRef<any> {
        if (state.type.includes('formcomponent')) {
            return state.model.containedForm.absoluteLayout ? this.formComponentAbsoluteDiv : this.formComponentResponsiveDiv;
        } else {
            // TODO: this has to be replaced with a type property on the state object
            // TODO - hmm type is already camel case here with dashes removed normally - so I don't think we need the indexOf, replace etc anymore
            let compDirectiveName = state.type;
            const index = compDirectiveName.indexOf('-');
            compDirectiveName = compDirectiveName.replace('-', '');
            return this[compDirectiveName.substring(0, index) + compDirectiveName.charAt(index).toUpperCase() + compDirectiveName.substring(index + 1)];
        }
    }

    public getAbsoluteFormStyle() {
        const formData = this.formCache.getComponent('');

        for (const key in this.absolutFormPosition) {
            if (this.absolutFormPosition.hasOwnProperty(key)) {
                delete this.absolutFormPosition[key];
            }
        }
        this.absolutFormPosition['left'] = '0px';
        this.absolutFormPosition['top'] = '0px';
        this.absolutFormPosition['right'] = '0px';
        this.absolutFormPosition['bottom'] = '0px';
        this.absolutFormPosition['position'] = 'absolute';
        this.absolutFormPosition['overflow'] = 'hidden';

        if (formData.model.borderType) {
            const borderStyle = formData.model.borderType;
            for (const key of Object.keys(borderStyle)) {
                this.absolutFormPosition[key] = borderStyle[key];
            }
        }
        if (formData.model.transparent) {
            this.absolutFormPosition['backgroundColor'] = 'transparent';
        }

        if (formData.model.addMinSize) {
            if (formData.model.hasExtraParts || this.el.nativeElement.parentNode.closest('.svy-form') == null) {
                // see svyFormstyle from ng1
                this.absolutFormPosition['minWidth'] = formData.model.size.width + 'px';
                this.absolutFormPosition['minHeight'] = formData.model.size.height + 'px';
            }
        }
        return this.absolutFormPosition;
    }

    public isFormAvailable(name: string): boolean {
        return this.formservice.hasFormCacheEntry(name);
    }

    datachange(_component: ComponentCache, _property: string, _value: any, _dataprovider: boolean) {
        // no operation needed for this dataprovider change event
    }

    getHandler(_item: ComponentCache, _handler: string) {
        return null;
    }

    registerComponent(component: ServoyBaseComponent<any>): void {
        this.componentCache[component.name] = component;
    }

    unRegisterComponent(component: ServoyBaseComponent<any>): void {
        delete this.componentCache[component.name];
    }

    getServoyApi(item: ComponentCache) {
        let api = this.servoyApiCache[item.name];
        if (api == null) {
            api = new FormComponentDesignServoyApi(item, this.name, this.formCache.absolute, this.formservice, this.servoyService, this);
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    getNGClass(item: StructureCache): { [klass: string]: any } {
        const ngclass = {};
        ngclass[item.attributes.designclass] = this.showWireframe;
        ngclass['maxLevelDesign'] = this.showWireframe && item.getDepth() === this.maxLevel;
        const children = item.items.length;
        if (children > 0 && children < 10) {
            ngclass['containerChildren' + children] = this.showWireframe && item.getDepth() === this.maxLevel;
        }
        if (children >= 10) {
            ngclass['containerChildren10'] = this.showWireframe && item.getDepth() === this.maxLevel;
        }
        ngclass['drop_highlight'] = this.canContainDraggedElement(item.attributes['svy-layoutname'], item.attributes['svy-id']);
        return ngclass;
    }

    public callApi(_componentName: string, _apiName: string, _args: any, _path?: string[]): any {
        return null;
    }

    getContainerByName(containername: string): Element {
        return this.document.querySelector('[name="' + this.name + '.' + containername + '"]');
    }

    updateFormStyleClasses(_ngutilsstyleclasses: string): void {

    }

    private initFormCache(): void {
        // really make sure all form state is reverted to default
        // Form Instances are reused for tabpanels that have a template reference to this.
        this.formCache = this.formservice.getFormCache(this);
        const styleClasses: string = this.formCache.getComponent('').model.styleClass;
        if (styleClasses)
            this.formClasses = styleClasses.split(' ');
        else
            this.formClasses = null;
        this._containers = this.formCache.getComponent('').model.containers;
        this._cssstyles = this.formCache.getComponent('').model.cssstyles;
        this.servoyApiCache = {};
        this.componentCache = {};
        this.leftPos = new Map();
        this.rightPos = new Map();
        this.topPos = new Map();
        this.bottomPos = new Map();
        this.middleV = new Map();
        this.middleH = new Map();
    }

    private canContainDraggedElement(container: string, svyid: string): boolean {
        if (this.dropHighlight === null) return false;
        if (this.dropHighlightIgnoredIds && svyid && this.dropHighlightIgnoredIds.indexOf(svyid) >= 0) return false;
        const drop = this.dropHighlight.split('.');
        const allowedChildren = this.allowedChildren[container];
        if (allowedChildren && allowedChildren.indexOf(drop[1]) >= 0) return true; //component

        for (const layout of Object.keys(allowedChildren)) {
            const a = allowedChildren[layout].split('.');
            if (a[0] === drop[0] && ((a[1] === '*') || (a[1] === drop[1]))) return true;
        }
        return false;
    }

}

class FormComponentDesignServoyApi extends ServoyApi {
    constructor(item: ComponentCache,
        formname: string,
        absolute: boolean,
        formservice: FormService,
        servoyService: ServoyService,
        private fc: DesignFormComponent) {
        super(item, formname, absolute, formservice, servoyService, true);
    }

    registerComponent(comp: ServoyBaseComponent<any>) {
        this.fc.registerComponent(comp);
    }

    unRegisterComponent(comp: ServoyBaseComponent<any>) {
        this.fc.unRegisterComponent(comp);
    }

    public formWillShow(_formname: string, _relationname?: string, _formIndex?: number): Promise<boolean> {
        return new Promise<any>(resolve => {
            resolve(true);
        });
    }

    public hideForm(_formname: string, _relationname?: string, _formIndex?: number,
        _formNameThatWillShow?: string, _relationnameThatWillBeShown?: string, _formIndexThatWillBeShown?: number): Promise<boolean> {
        return new Promise<any>(resolve => {
            resolve(true);
        });
    }

    public apply(_propertyName: string, _value: any) {
        // noop
    }

}

class Guide {
   x: number;
   y: number;
  width: number;
  height: number;
   styleClass: string;
    constructor(x: number,  y: number,
            width: number, height: number, styleClass: string) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.styleClass = styleClass;
    }
}





