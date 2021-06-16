import { Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges, ViewChild, ViewChildren,
        TemplateRef,  Directive, ElementRef, Renderer2, ChangeDetectionStrategy, ChangeDetectorRef, SimpleChange, Inject } from '@angular/core';

import { FormCache, StructureCache, FormComponentCache, ComponentCache, instanceOfApiExecutor, PartCache, FormComponentProperties } from '../types';

import { ServoyService } from '../servoy.service';

import { SabloService } from '../../sablo/sablo.service';
import { LoggerService, LoggerFactory } from '@servoy/public';

import { ServoyApi } from '../servoy_api';
import { FormService } from '../form.service';
import { DOCUMENT } from '@angular/common';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    // eslint-disable-next-line
    selector: 'svy-form',
    changeDetection: ChangeDetectionStrategy.OnPush,
    /* eslint-disable max-len */
    template: `
      <div *ngIf="formCache.absolute" [ngStyle]="getAbsoluteFormStyle()" class="svy-form" [ngClass]="formClasses" svyAutosave> <!-- main div -->
           <div *ngFor="let part of formCache.parts" [svyContainerStyle]="part"> <!-- part div -->
               <div *ngFor="let item of part.items" [svyContainerStyle]="item" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component or formcomponent -->
                </div>
          </div>
      </div>
      <div *ngIf="!formCache.absolute" class="svy-form svy-respform svy-overflow-auto" [ngClass]="formClasses"> <!-- main container div -->
            <ng-template *ngFor="let item of formCache.mainStructure.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>  <!-- component or responsive div  -->
      </div>

      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [svyContainerStyle]="state" class="svy-layoutcontainer">
               <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
          </div>
      </ng-template>
      <ng-template  #formComponentAbsoluteDiv  let-state="state" >
          <div [svyContainerStyle]="state.formComponentProperties" style="position:relative" class="svy-formcomponent">
               <div *ngFor="let item of state.items" [svyContainerStyle]="item" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component  -->
               </div>
          </div>
      </ng-template>
      <ng-template  #formComponentResponsiveDiv  let-state="state" >
          <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component  -->
      </ng-template>
      <!-- component template generate start -->
<ng-template #servoycoreDefaultLoadingIndicator let-callback="callback" let-state="state"><servoycore-defaultLoadingIndicator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [size]="state.model.size" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-defaultLoadingIndicator></ng-template>
<ng-template #servoycoreErrorbean let-callback="callback" let-state="state"><servoycore-errorbean  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [error]="state.model.error" [location]="state.model.location" [size]="state.model.size" [toolTipText]="state.model.toolTipText" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-errorbean></ng-template>
<ng-template #servoycoreFormcomponent let-callback="callback" let-state="state"><servoycore-formcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [location]="state.model.location" [size]="state.model.size" [styleClass]="state.model.styleClass" *ngIf="state.model.visible" [width]="state.model.width" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-formcomponent></ng-template>
<ng-template #servoycoreFormcontainer let-callback="callback" let-state="state"><servoycore-formcontainer  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [location]="state.model.location" [relationName]="state.model.relationName" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" *ngIf="state.model.visible" [waitForData]="state.model.waitForData" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoycore-formcontainer></ng-template>
<ng-template #servoycoreListformcomponent let-callback="callback" let-state="state"><servoycore-listformcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [foundset]="state.model.foundset" [location]="state.model.location" [pageLayout]="state.model.pageLayout" [paginationStyleClass]="state.model.paginationStyleClass" [readOnly]="state.model.readOnly" [responsivePageSize]="state.model.responsivePageSize" [rowStyleClass]="state.model.rowStyleClass" [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider" [selectionClass]="state.model.selectionClass" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" *ngIf="state.model.visible" [onSelectionChanged]="callback.getHandler(state,'onSelectionChanged')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-listformcomponent></ng-template>
<ng-template #servoycoreNavigator let-callback="callback" let-state="state"><servoycore-navigator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [currentIndex]="state.model.currentIndex" [hasMore]="state.model.hasMore" [location]="state.model.location" [maxIndex]="state.model.maxIndex" [minIndex]="state.model.minIndex" [size]="state.model.size" [setSelectedIndex]="callback.getHandler(state,'setSelectedIndex')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-navigator></ng-template>
<ng-template #servoycoreSlider let-callback="callback" let-state="state"><servoycore-slider  [animate]="state.model.animate" [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [enabled]="state.model.enabled" [location]="state.model.location" [max]="state.model.max" [min]="state.model.min" [orientation]="state.model.orientation" [range]="state.model.range" [size]="state.model.size" [step]="state.model.step" *ngIf="state.model.visible" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [onCreateMethodID]="callback.getHandler(state,'onCreateMethodID')" [onSlideMethodID]="callback.getHandler(state,'onSlideMethodID')" [onStartMethodID]="callback.getHandler(state,'onStartMethodID')" [onStopMethodID]="callback.getHandler(state,'onStopMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-slider></ng-template>
<ng-template #servoydefaultButton let-callback="callback" let-state="state"><servoydefault-button  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [hideText]="state.model.hideText" [horizontalAlignment]="state.model.horizontalAlignment" [imageMediaID]="state.model.imageMediaID" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [mediaOptions]="state.model.mediaOptions" [mnemonic]="state.model.mnemonic" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [showFocus]="state.model.showFocus" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [textRotation]="state.model.textRotation" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [verticalAlignment]="state.model.verticalAlignment" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDoubleClickMethodID]="callback.getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-button></ng-template>
<ng-template #servoydefaultCalendar let-callback="callback" let-state="state"><servoydefault-calendar  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-calendar></ng-template>
<ng-template #servoydefaultCheck let-callback="callback" let-state="state"><servoydefault-check  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-check></ng-template>
<ng-template #servoydefaultCheckgroup let-callback="callback" let-state="state"><servoydefault-checkgroup  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-checkgroup></ng-template>
<ng-template #servoydefaultCombobox let-callback="callback" let-state="state"><servoydefault-combobox  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-combobox></ng-template>
<ng-template #servoydefaultHtmlarea let-callback="callback" let-state="state"><servoydefault-htmlarea  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-htmlarea></ng-template>
<ng-template #servoydefaultHtmlview let-callback="callback" let-state="state"><servoydefault-htmlview  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-htmlview></ng-template>
<ng-template #servoydefaultImagemedia let-callback="callback" let-state="state"><servoydefault-imagemedia  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-imagemedia></ng-template>
<ng-template #servoydefaultLabel let-callback="callback" let-state="state"><servoydefault-label  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [hideText]="state.model.hideText" [horizontalAlignment]="state.model.horizontalAlignment" [imageMediaID]="state.model.imageMediaID" [labelFor]="state.model.labelFor" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [mediaOptions]="state.model.mediaOptions" [mnemonic]="state.model.mnemonic" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [showFocus]="state.model.showFocus" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [textRotation]="state.model.textRotation" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [verticalAlignment]="state.model.verticalAlignment" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDoubleClickMethodID]="callback.getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-label></ng-template>
<ng-template #servoydefaultListbox let-callback="callback" let-state="state"><servoydefault-listbox  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [multiselectListbox]="state.model.multiselectListbox" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-listbox></ng-template>
<ng-template #servoydefaultPassword let-callback="callback" let-state="state"><servoydefault-password  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-password></ng-template>
<ng-template #servoydefaultRadio let-callback="callback" let-state="state"><servoydefault-radio  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-radio></ng-template>
<ng-template #servoydefaultRadiogroup let-callback="callback" let-state="state"><servoydefault-radiogroup  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-radiogroup></ng-template>
<ng-template #servoydefaultRectangle let-callback="callback" let-state="state"><servoydefault-rectangle  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [containsFormID]="state.model.containsFormID" [cssPosition]="state.model.cssPosition" [enabled]="state.model.enabled" [foreground]="state.model.foreground" [lineSize]="state.model.lineSize" [location]="state.model.location" [roundedRadius]="state.model.roundedRadius" [shapeType]="state.model.shapeType" [size]="state.model.size" [transparent]="state.model.transparent" *ngIf="state.model.visible" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoydefault-rectangle></ng-template>
<ng-template #servoydefaultSpinner let-callback="callback" let-state="state"><servoydefault-spinner  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-spinner></ng-template>
<ng-template #servoydefaultSplitpane let-callback="callback" let-state="state"><servoydefault-splitpane  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [divLocation]="state.model.divLocation" (divLocationChange)="callback.datachange(state,'divLocation',$event)" [divSize]="state.model.divSize" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [pane1MinSize]="state.model.pane1MinSize" [pane2MinSize]="state.model.pane2MinSize" [readOnly]="state.model.readOnly" [resizeWeight]="state.model.resizeWeight" [selectedTabColor]="state.model.selectedTabColor" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabOrientation]="state.model.tabOrientation" [tabs]="state.model.tabs" (tabsChange)="callback.datachange(state,'tabs',$event)" [tabSeq]="state.model.tabSeq" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoydefault-splitpane></ng-template>
<ng-template #servoydefaultTabpanel let-callback="callback" let-state="state"><servoydefault-tabpanel  [activeTabIndex]="state.model.activeTabIndex" (activeTabIndexChange)="callback.datachange(state,'activeTabIndex',$event)" [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [readOnly]="state.model.readOnly" [selectedTabColor]="state.model.selectedTabColor" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabIndex]="state.model.tabIndex" (tabIndexChange)="callback.datachange(state,'tabIndex',$event)" [tabOrientation]="state.model.tabOrientation" [tabs]="state.model.tabs" (tabsChange)="callback.datachange(state,'tabs',$event)" [tabSeq]="state.model.tabSeq" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoydefault-tabpanel></ng-template>
<ng-template #servoydefaultTablesspanel let-callback="callback" let-state="state"><servoydefault-tablesspanel  [activeTabIndex]="state.model.activeTabIndex" (activeTabIndexChange)="callback.datachange(state,'activeTabIndex',$event)" [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [enabled]="state.model.enabled" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [readOnly]="state.model.readOnly" [selectedTabColor]="state.model.selectedTabColor" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabIndex]="state.model.tabIndex" (tabIndexChange)="callback.datachange(state,'tabIndex',$event)" [tabOrientation]="state.model.tabOrientation" [tabs]="state.model.tabs" (tabsChange)="callback.datachange(state,'tabs',$event)" [tabSeq]="state.model.tabSeq" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoydefault-tablesspanel></ng-template>
<ng-template #servoydefaultTextarea let-callback="callback" let-state="state"><servoydefault-textarea  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-textarea></ng-template>
<ng-template #servoydefaultTextfield let-callback="callback" let-state="state"><servoydefault-textfield  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-textfield></ng-template>
<ng-template #servoydefaultTypeahead let-callback="callback" let-state="state"><servoydefault-typeahead  [servoyAttributes]="state.model.servoyAttributes" [background]="state.model.background" [borderType]="state.model.borderType" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [format]="state.model.format" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" (locationChange)="callback.datachange(state,'location',$event)" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" (sizeChange)="callback.datachange(state,'size',$event)" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" *ngIf="state.model.visible" [onActionMethodID]="callback.getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="callback.getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="callback.getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="callback.getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="callback.getHandler(state,'onRightClickMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoydefault-typeahead></ng-template>
     <!-- component template generate end -->
   `
   /* eslint-enable max-len */
})

export class FormComponent implements OnDestroy, OnChanges {
    @ViewChild('svyResponsiveDiv', { static: true }) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild('formComponentAbsoluteDiv', { static: true }) readonly formComponentAbsoluteDiv: TemplateRef<any>;
    @ViewChild('formComponentResponsiveDiv', { static: true }) readonly formComponentResponsiveDiv: TemplateRef<any>;

    // component viewchild template generate start
    @ViewChild('servoydefaultTextfield', { static: true }) readonly servoydefaultTextfield: TemplateRef<any>;
    @ViewChild('servoydefaultTextarea', { static: true }) readonly servoydefaultTextarea: TemplateRef<any>;
    @ViewChild('servoydefaultButton', { static: true }) readonly servoydefaultButton: TemplateRef<any>;
    @ViewChild('servoydefaultLabel', { static: true }) readonly servoydefaultLabel: TemplateRef<any>;
    @ViewChild('servoydefaultRectangle', { static: true }) readonly servoydefaultRectangle: TemplateRef<any>;
    @ViewChild('servoydefaultTabpanel', { static: true }) readonly servoydefaultTabpanel: TemplateRef<any>;
    @ViewChild('servoydefaultTablesspanel', { static: true }) readonly servoydefaultTablesspanel: TemplateRef<any>;
    @ViewChild('servoydefaultSplitpane', { static: true }) readonly servoydefaultSplitpane: TemplateRef<any>;
    @ViewChild('servoydefaultCalendar', { static: true }) readonly servoydefaultCalendar: TemplateRef<any>;
    @ViewChild('servoydefaultCombobox', { static: true }) readonly servoydefaultCombobox: TemplateRef<any>;
    @ViewChild('servoydefaultTypeahead', { static: true }) readonly servoydefaultTypeahead: TemplateRef<any>;
    @ViewChild('servoydefaultCheck', { static: true }) readonly servoydefaultCheck: TemplateRef<any>;
    @ViewChild('servoydefaultCheckgroup', { static: true }) readonly servoydefaultCheckgroup: TemplateRef<any>;
    @ViewChild('servoydefaultRadio', { static: true }) readonly servoydefaultRadio: TemplateRef<any>;
    @ViewChild('servoydefaultRadiogroup', { static: true }) readonly servoydefaultRadiogroup: TemplateRef<any>;
    @ViewChild('servoydefaultPassword', { static: true }) readonly servoydefaultPassword: TemplateRef<any>;
    @ViewChild('servoydefaultHtmlarea', { static: true }) readonly servoydefaultHtmlarea: TemplateRef<any>;
    @ViewChild('servoydefaultSpinner', { static: true }) readonly servoydefaultSpinner: TemplateRef<any>;
    @ViewChild('servoydefaultHtmlview', { static: true }) readonly servoydefaultHtmlview: TemplateRef<any>;
    @ViewChild('servoydefaultListbox', { static: true }) readonly servoydefaultListbox: TemplateRef<any>;
    @ViewChild('servoydefaultImagemedia', { static: true }) readonly servoydefaultImagemedia: TemplateRef<any>;

    @ViewChild('servoycoreSlider', { static: true }) readonly servoycoreSlider: TemplateRef<any>;
    @ViewChild('servoycoreErrorbean', { static: true }) readonly servoycoreErrorbean: TemplateRef<any>;
    @ViewChild('servoycoreListformcomponent', { static: true }) readonly servoycoreListformcomponent: TemplateRef<any>;
    @ViewChild('servoycoreFormcontainer', { static: true }) readonly servoycoreFormcontainer: TemplateRef<any>;

    // component viewchild template generate end



    @Input() name: string;

    formClasses: string[];

    formCache: FormCache;

    absolutFormPosition = {};

    private handlerCache: { [property: string]: { [property: string]: () => void } } = {};
    private servoyApiCache: { [property: string]: ServoyApi } = {};
    private componentCache: { [property: string]: ServoyBaseComponent<any> } = {};
    private log: LoggerService;
    private _containers: { added: any; removed: any; };
    private _cssstyles: { [x: string]: any; };

    constructor(private formservice: FormService, private sabloService: SabloService,
                private servoyService: ServoyService, logFactory: LoggerFactory,
                private changeHandler: ChangeDetectorRef,
                private el: ElementRef, private renderer: Renderer2,
                @Inject(DOCUMENT) private document: Document) {
        this.log = logFactory.getLogger('FormComponent');
    }

    public detectChanges() {
        this.changeHandler.markForCheck();
    }

    public formCacheChanged(cache: FormCache): void {
        this.formCache = cache;
        this.detectChanges();
    }

    public getFormCache(): FormCache {
        return this.formCache;
    }

    propertyChanged(componentName: string, property: string, value: any): void {
        const comp = this.componentCache[componentName];
        if (comp) {
            const change = {};
            change[property] = new SimpleChange(value,value,false);
            comp.ngOnChanges(change);
            // this is kind of like a push so we should trigger this.
            comp.detectChanges();
        }
    }

    @Input('containers')
    set containers(containers: {added: any, removed: any}) {
        if (!containers) return;
        this._containers = containers;
        for (let containername in containers.added) {
            const container = this.getContainerByName(containername);
            if (container) {
                containers.added[containername].forEach((cls: string) => this.renderer.addClass(container, cls));
            }
        }
        for (let containername in containers.removed) {
            const container = this.getContainerByName(containername);
            if (container) {
                containers.removed[containername].forEach((cls: string) => this.renderer.removeClass(container, cls));
            }
        }
    }

    get containers() {
        return this._containers;
    }

    @Input('cssStyles')
    set cssstyles(cssStyles: { [x: string]: any; }) {
        if (!cssStyles) return;
        this._cssstyles = cssStyles;
        for (let containername in cssStyles) {
            const container = this.getContainerByName(containername);
            if (container) {
                const stylesMap = cssStyles[containername];
                for (let key in stylesMap) {
                    this.renderer.setStyle(container, key, stylesMap[key]);
                }
            }
        }
    }

    get cssstyles() {
        return this._cssstyles;
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.name) {
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
            this.handlerCache = {};
            this.servoyApiCache = {};
            this.componentCache = {};

            this.sabloService.callService('formService', 'formLoaded', { formname: this.name }, true);
            this.renderer.setAttribute(this.el.nativeElement,'name', this.name);

        }
    }

    ngOnDestroy() {
        this.formservice.destroy(this);
    }

    getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return item.tagname? this[item.tagname]: this.svyResponsiveDiv;
        } else if (item instanceof FormComponentCache ) {
            if (item.hasFoundset) return this.servoycoreListformcomponent;
            return item.responsive ? this.formComponentResponsiveDiv : this.formComponentAbsoluteDiv;
        } else {
            if (this[item.type] === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ('Template for ' + item.type + ' was not found, please check form_component template.')));
            }
            return this[item.type];
        }
    }

    getTemplateForLFC(state: ComponentCache ): TemplateRef<any> {
        if (state.type.includes('formcomponent')) {
            return state.model.containedForm.absoluteLayout ? this.formComponentAbsoluteDiv : this.formComponentResponsiveDiv;
        } else {
            // TODO: this has to be replaced with a type property on the state object
            let compDirectiveName = state.type;
            const index = compDirectiveName.indexOf('-');
            compDirectiveName =  compDirectiveName.replace('-','');
            return this[compDirectiveName.substring(0, index) + compDirectiveName.charAt(index).toUpperCase() + compDirectiveName.substring(index + 1)];
        }
    }

    public getAbsoluteFormStyle() {
        const formData = this.formCache.getComponent('');

        for (const key in this.absolutFormPosition){
            if (this.absolutFormPosition.hasOwnProperty(key)){
                delete this.absolutFormPosition[key];
            }
        }
        this.absolutFormPosition['left'] = '0px';
        this.absolutFormPosition['top'] = '0px';
        this.absolutFormPosition['right'] = '0px';
        this.absolutFormPosition['bottom'] = '0px';
        this.absolutFormPosition['position'] = 'absolute';

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
            if (formData.model.hasExtraParts || this.el.nativeElement.parentNode.closest('.svy-form') == null)
            {
                // see svyFormstyle from ng1
                this.absolutFormPosition['minWidth'] = formData.model.size.width + 'px';
                this.absolutFormPosition['minHeight'] = formData.model.size.height + 'px';
            }
        }
        return this.absolutFormPosition;
    }

    public isFormAvailable(name: string): boolean {
        // console.log("isFormAvailable: " + name + " " +  this.formservice.hasFormCacheEntry( name));
        return this.formservice.hasFormCacheEntry(name);
    }

    datachange(component: ComponentCache, property: string, value, dataprovider: boolean) {
        const model = this.formCache.getComponent(component.name).model;
        const oldValue = model[property];
        this.formCache.getComponent(component.name).model[property] = value;
        this.formservice.sendChanges(this.name, component.name, property, value, oldValue, dataprovider);
    }

    getHandler(item: ComponentCache, handler: string) {
        let itemCache = this.handlerCache[item.name];
        if (itemCache == null) {
            itemCache = {};
            this.handlerCache[item.name] = itemCache;
        }
        let func = itemCache[handler];
        if (func == null && item.handlers && item.handlers.indexOf(handler) >= 0) {
            const me = this;
            // eslint-disable-next-line
            func = function() {
                return me.formservice.executeEvent(me.name, item.name, handler, arguments);
            };
            itemCache[handler] = func;
        }
        return func;
    }

    registerComponent(component: ServoyBaseComponent<any> ): void {
        this.componentCache[component.name] = component;
    }

    unRegisterComponent(component: ServoyBaseComponent<any> ): void {
        delete this.componentCache[component.name];
    }

    getServoyApi(item: ComponentCache) {
        let api = this.servoyApiCache[item.name];
        if (api == null) {
            api = new FormComponentServoyApi(item, this.name, this.formCache.absolute, this.formservice, this.servoyService, this);
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    public callApi(componentName: string, apiName: string, args: any, path?: string[]): any {
        if (path && path.length > 0) {
            const comp = this.componentCache[path[0]];
            if (instanceOfApiExecutor(comp)) {
                comp.callApi(path[1], apiName, args, path.slice(2));
            } else {
                this.log.error('trying to call api: ' + apiName + ' on component: ' + componentName + ' with path: ' + path +
                 ', but comp: ' + (comp == null?' is not found':comp.name + ' doesnt implement IApiExecutor') );
            }

        } else {
            const comp = this.componentCache[componentName];
            const proto = Object.getPrototypeOf(comp);
            if (proto[apiName]) {
                return proto[apiName].apply(comp, args);
            } else {
                this.log.error(this.log.buildMessage(() => ('Api ' + apiName + ' for component ' + componentName + ' was not found, please check component implementation.')));
                return null;
            }
        }
    }

    private getContainerByName(containername: string) : Element {
       return this.document.querySelector('[name="'+this.name+'.'+containername+'"]');
    }
}

class FormComponentServoyApi extends ServoyApi {
    constructor(item: ComponentCache,
                formname: string,
                absolute: boolean,
                formservice: FormService,
                servoyService: ServoyService,
                private fc: FormComponent) {
        super(item,formname,absolute,formservice,servoyService);
    }

    registerComponent(comp: ServoyBaseComponent<any> ) {
     this.fc.registerComponent(comp);
    }

    unRegisterComponent(comp: ServoyBaseComponent<any> ) {
     this.fc.unRegisterComponent(comp);
    }
}

@Directive({ selector: '[svyContainerStyle]' })
export class AddAttributeDirective implements OnInit {
    @Input() svyContainerStyle: StructureCache | ComponentCache | FormComponentCache | PartCache | FormComponentProperties;

    constructor(private el: ElementRef, private renderer: Renderer2, @Inject(FormComponent) private parent: FormComponent) { }

    ngOnInit() {
        if ('classes' in this.svyContainerStyle) {
            this.svyContainerStyle.classes.forEach(cls => this.renderer.addClass(this.el.nativeElement, cls));
        }

        if ('layout' in this.svyContainerStyle) {
            for (const key of Object.keys(this.svyContainerStyle.layout)) {
                this.renderer.setStyle(this.el.nativeElement, key, this.svyContainerStyle.layout[key]);
            }
        }
        if ('attributes' in this.svyContainerStyle) {
              for (const key of Object.keys(this.svyContainerStyle.attributes)) {
                this.renderer.setAttribute(this.el.nativeElement, key, this.svyContainerStyle.attributes[key]);
                if (key === 'name' && this.svyContainerStyle instanceof StructureCache) this.restoreCss(); //set the containers css and classes after a refresh if it's the case
            }
        }
    }

    private restoreCss() {
        if ('attributes' in this.svyContainerStyle && this.svyContainerStyle.attributes.name.indexOf('.') > 0) {
            const name = this.svyContainerStyle.attributes.name.split('.')[1];
            if (this.parent.cssstyles && this.parent.cssstyles[name]) {
                const stylesMap = this.parent.cssstyles[name];
                for (let k in stylesMap) {
                    this.renderer.setStyle(this.el.nativeElement, k, stylesMap[k]);
                }
            }
            if (this.parent.containers) {
                if (this.parent.containers.added && this.parent.containers.added[name]) {
                    this.parent.containers.added[name].forEach((cls: string) => this.renderer.addClass(this.el.nativeElement, cls));
                }
                if (this.parent.containers.removed && this.parent.containers.removed[name]) {
                    this.parent.containers.removed[name].forEach((cls: string) => this.renderer.removeClass(this.el.nativeElement, cls));
                }
            }
        }
    }
}



