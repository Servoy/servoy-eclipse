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
          <div *ngIf="draggedElementItem" [svyContainerStyle]="draggedElementItem" [svyContainerLayout]="draggedElementItem.layout" class="svy-wrapper" style="position:absolute" id="svy_draggedelement">
                   <ng-template [ngTemplateOutlet]="getTemplate(draggedElementItem)" [ngTemplateOutletContext]="{ state:draggedElementItem, callback:this }"></ng-template>
          </div>
      </div>
      <div *ngIf="!formCache.absolute && name!=='VariantsForm'" class="svy-form svy-respform svy-overflow-auto" [ngClass]="formClasses"> <!-- main container div -->
            <div *ngIf="draggedElementItem" [svyContainerStyle]="draggedElementItem" [svyContainerLayout]="draggedElementItem.layout" class="svy-wrapper" style="position:absolute" id="svy_draggedelement">
                   <ng-template [ngTemplateOutlet]="getTemplate(draggedElementItem)" [ngTemplateOutletContext]="{ state:draggedElementItem, callback:this }"></ng-template>
            </div>
            <ng-template *ngFor="let item of formCache.mainStructure?.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>  <!-- component or responsive div  -->
      </div>
      <div *ngIf="!formCache.absolute && name==='VariantsForm'" class="svy-form svy-respform svy-overflow-auto" [ngClass]="formClasses" (keyup)="onVariantKeyup($event)"> <!-- main container div -->
            <div (mousedown)="onVariantsMouseDown($event)" (keyup)="onVariantKeyup($event)" *ngFor="let item of formCache.mainStructure?.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" style="position:absolute">
                <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>
            </div>
      </div>
      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" [ngClass]="getNGClass(state)" class="svy-layoutcontainer">
               <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
          </div>
      </ng-template>
      <ng-template  #cssPositionContainer  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [ngClass]="getNGClass(state)" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
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
          <servoycore-formcomponent-responsive-container [svyContainerStyle]="state.formComponentProperties" [svyContainerAttributes]="state.formComponentProperties.attributes" [ngClass]="{'invisible_element' : state.model.svyVisible === false}" [items]="state.items" [class]="state.model.styleClass" [formComponent]="this"></servoycore-formcomponent-responsive-container>
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
                this.windowRefService.nativeWindow.parent.postMessage({ id: 'onDestroyVariants'}, '*');

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
                if (this.insertedCloneParent) {
                    this.insertedCloneParent.addChild(this.insertedClone, beforeChild);
                    if (event.data.uuids) {
                        event.data.uuids.forEach(uuid => {
                            let insertedClone: ComponentCache | StructureCache = this.formCache.getLayoutContainer(uuid); 
                            if (!insertedClone) {
                                insertedClone = this.formCache.getComponent(uuid);
                            }
                            const insertedCloneParent = insertedClone.parent;
                            insertedCloneParent.removeChild(insertedClone);
                            this.insertedCloneParent.addChild(insertedClone, beforeChild);
                        });
                    }
                }
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
                if (changed){
                    Array.from( this.formCache.formComponents.values()).forEach(formComponent => {
                       if (formComponent.hasFoundset && formComponent.model.containedForm && !formComponent.model.containedForm.absoluteLayout){
                           // just trigger the change detection
                          formComponent.model.editable = ! formComponent.model.editable;
                       }
                    });
                }
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
            this.detectChanges();
        });
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

    public onVariantKeyup(event: KeyboardEvent) {
        if (event.keyCode === 27) {
            this.windowRefService.nativeWindow.parent.postMessage({ id: 'variantsEscapePressed'}, '*');
        }
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
            if (item.type === 'menu') return;
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
        if (!item.cssPositionContainer || item.getDepth() != 0) {
        	ngclass[item.attributes.designclass] = this.showWireframe;
        }
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
        this.formservice.resolveComponentCache(this);
        const styleClasses: string = this.formCache.getComponent('').model.styleClass;
        if (styleClasses)
            this.formClasses = styleClasses.split(' ');
        else
            this.formClasses = null;
        this._containers = this.formCache.getComponent('').model.containers;
        this._cssstyles = this.formCache.getComponent('').model.cssstyles;
        this.servoyApiCache = {};
        this.componentCache = {};
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





