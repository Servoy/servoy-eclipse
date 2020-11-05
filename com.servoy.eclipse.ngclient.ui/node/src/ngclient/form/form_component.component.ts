import { Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges, ViewChild, ViewChildren, TemplateRef, QueryList, Directive, ElementRef, Renderer2, NgModule } from '@angular/core';

import { FormService, FormCache, StructureCache, FormComponentCache, ComponentCache, ListFormComponentCache } from '../form.service';

import { ServoyService } from '../servoy.service'

import { SabloService } from '../../sablo/sablo.service'
import { LoggerService, LoggerFactory } from '../../sablo/logger.service'

import { ServoyApi } from '../servoy_api'

@Component({
    selector: 'svy-form',
    template: `
      <div *ngIf="formCache.absolute" [ngStyle]="getAbsoluteFormStyle()" class="svy-form" svyAutosave> <!-- main div -->
           <div *ngFor="let part of formCache.parts" [config]="part"> <!-- part div -->
               <div *ngFor="let item of part.items" [config]="item" class="svy-wrapper" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component or formcomponent -->
                </div>
          </div>
      </div>
      <div *ngIf="!formCache.absolute" [config]="formCache.mainStructure"> <!-- main container div -->
            <ng-template *ngFor="let item of formCache.mainStructure.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component or responsive div  -->
            <ng-template *ngFor="let formComponent of formCache.formComponents" [ngTemplateOutlet]="getTemplate(formComponent)" [ngTemplateOutletContext]="{state:formComponent}"></ng-template>
      </div>

      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [config]="state">
               <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>
          </div>
      </ng-template>
      <ng-template  #formComponentAbsoluteDiv  let-state="state" >
          <div [config]="state.formComponentProperties" style="position:relative" class="svy-formcomponent">
               <div *ngFor="let item of state.items" [config]="item" class="svy-wrapper" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component  -->
               </div>
          </div>
      </ng-template>
      <ng-template  #formComponentResponsiveDiv  let-state="state" >
          <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component  -->
      </ng-template>
      <!-- component template generate start -->
      <ng-template #servoydefaultTextfield let-state="state"><servoydefault-textfield  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoydefault-textfield></ng-template>
      <ng-template #servoydefaultTextarea let-state="state"><servoydefault-textarea  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoydefault-textarea></ng-template>
      <ng-template #servoydefaultButton let-state="state">
        <servoydefault-button *ngIf = "state.model.visible" [borderType]="state.model.borderType" [foreground]="state.model.foreground" [hideText]="state.model.hideText"
                              [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent"
                              [textRotation]="state.model.textRotation" [mnemonic]="state.model.mnemonic" [text]="state.model.text"
                              [toolTipText]="state.model.toolTipText" [imageMediaID]="state.model.imageMediaID" [fontType]="state.model.fontType"
                              [margin]="state.model.margin" [visible]="state.model.visible" [format]="state.model.format" [mediaOptions]="state.model.mediaOptions"
                              [dataProviderID]="state.model.dataProviderID" [showFocus]="state.model.showFocus"
                              [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)"
                              [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location"
                              (locationChange)="datachange(state.name,'location',$event)" [rolloverCursor]="state.model.rolloverCursor"
                              [rolloverImageMediaID]="state.model.rolloverImageMediaID" [tabSeq]="state.model.tabSeq" [verticalAlignment]="state.model.verticalAlignment"
                              [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')"
                              [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp>
        </servoydefault-button>
      </ng-template>
      <ng-template #servoydefaultCombobox let-state="state">
        <servoydefault-combo *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes"
                                   [onActionMethodID]="getHandler(state,'onActionMethodID')"
                                   [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')"
                                   [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')"
                                   [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')"
                                   [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')"
                                   [dataProviderID]="state.model.dataProviderID"
                                   (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)"
                                   [toolTipText]="state.model.toolTipText"
                                   [format]="state.model.format"
                                   [name]="state.name" [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent"
                                   [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText"
                                   [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable"
                                   [tabSeq]="state.model.tabSeq"
                                   [horizontalAlignment]="state.model.horizontalAlignment"
                                   [background]="state.model.background"
                                   [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)"
                                   [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)"
                                    [valuelistID]="state.model.valuelistID" #cmp></servoydefault-combo>
      </ng-template>
      <ng-template #servoydefaultTypeahead let-state="state">
        <servoydefault-typeahead *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes"
                                   [onActionMethodID]="getHandler(state,'onActionMethodID')"
                                   [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')"
                                   [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')"
                                   [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')"
                                   [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')"
                                   [dataProviderID]="state.model.dataProviderID"
                                   (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)"
                                   [toolTipText]="state.model.toolTipText"
                                   [format]="state.model.format"
                                   [name]="state.name" [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent"
                                   [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText"
                                   [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable"
                                   [tabSeq]="state.model.tabSeq"
                                   [horizontalAlignment]="state.model.horizontalAlignment"
                                   [background]="state.model.background"
                                   [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)"
                                   [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)"
                                   [valuelistID]="state.model.valuelistID"#cmp></servoydefault-typeahead>
      </ng-template>
      <ng-template #servoydefaultLabel let-state="state"><servoydefault-label  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [labelFor]="state.model.labelFor" [foreground]="state.model.foreground" [hideText]="state.model.hideText" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [textRotation]="state.model.textRotation" [mnemonic]="state.model.mnemonic" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [imageMediaID]="state.model.imageMediaID" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [format]="state.model.format" [mediaOptions]="state.model.mediaOptions" [dataProviderID]="state.model.dataProviderID" [showFocus]="state.model.showFocus" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [tabSeq]="state.model.tabSeq" [verticalAlignment]="state.model.verticalAlignment" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoydefault-label></ng-template>
      <ng-template #servoydefaultTabpanel let-state="state">
            <servoydefault-tabpanel  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [fontType]="state.model.fontType" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs"  [readOnly]="state.model.readOnly" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp>
                <ng-template let-name='name'>
                    <svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form>
                </ng-template>
            </servoydefault-tabpanel>
      </ng-template>
      <ng-template #servoydefaultTablesspanel let-state="state">
            <servoydefault-tablesspanel  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [fontType]="state.model.fontType" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs" [readOnly]="state.model.readOnly" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp>
                <ng-template let-name='name'>
                    <svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form>
                </ng-template>
            </servoydefault-tablesspanel>
      </ng-template>
      <ng-template #servoydefaultSplitpane let-state="state"><servoydefault-splitpane  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [pane1MinSize]="state.model.pane1MinSize" [fontType]="state.model.fontType" [pane2MinSize]="state.model.pane2MinSize" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs" (tabsChange)="datachange(state.name,'tabs',$event)" [readOnly]="state.model.readOnly" [foreground]="state.model.foreground" [divLocation]="state.model.divLocation" (divLocationChange)="datachange(state.name,'divLocation',$event)" [resizeWeight]="state.model.resizeWeight" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [divSize]="state.model.divSize" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoydefault-splitpane></ng-template>
      <ng-template #servoydefaultCalendar let-state="state"><servoydefault-calendar  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [horizontalAlignment]="state.model.horizontalAlignment" [findmode]="state.model.findmode" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [placeholderText]="state.model.placeholderText" [selectOnEnter]="state.model.selectOnEnter" [text]="state.model.text" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoydefault-calendar></ng-template>
      <ng-template #servoydefaultCheck let-state="state"><servoydefault-check *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [format]="state.model.format" [foreground]="state.model.foreground"  [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [name]="state.name" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly"[scrollbars]="state.model.scrollbars" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" (sizeChange)="datachange(state.name,'size',$event)" (locationChange)="datachange(state.name,'location',$event)" [servoyAttributes]="state.model.svy_attributes" #cmp></servoydefault-check></ng-template>
      <ng-template #servoydefaultCheckgroup let-state="state"><servoydefault-checkgroup *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [format]="state.model.format" [foreground]="state.model.foreground"  [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [name]="state.name" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" (sizeChange)="datachange(state.name,'size',$event)" (locationChange)="datachange(state.name,'location',$event)" #cmp></servoydefault-checkgroup></ng-template>
      <ng-template #servoydefaultRadiogroup let-state="state"><servoydefault-radiogroup *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [format]="state.model.format" [foreground]="state.model.foreground"  [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [name]="state.name" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" #cmp></servoydefault-radiogroup></ng-template>
      <ng-template #servoydefaultPassword let-state="state"><servoydefault-password *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [visible]="state.model.visible" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" #cmp></servoydefault-password></ng-template>
      <ng-template #servoydefaultHtmlarea let-state="state"><servoydefault-htmlarea *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [editable]="state.model.editable" [name]="state.name" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [visible]="state.model.visible" #cmp></servoydefault-htmlarea></ng-template>
      <ng-template #servoydefaultSpinner let-state="state"><servoydefault-spinner *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID"  (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [editable]="state.model.editable" [name]="state.name" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" #cmp></servoydefault-spinner></ng-template>
      <ng-template #servoydefaultRectangle let-state="state"><servoydefault-rectangle  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [lineSize]="state.model.lineSize" [roundedRadius]="state.model.roundedRadius" [shapeType]="state.model.shapeType" [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoydefault-rectangle></ng-template>
      <ng-template #servoydefaultHtmlview let-state="state"><servoydefault-htmlview  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [text]="state.model.text" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name"  [scrollbars]="state.model.scrollbars" [toolTipText]="state.model.toolTipText" [tabSeq]="state.model.tabSeq" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [dataProviderID]="state.model.dataProviderID" #cmp></servoydefault-htmlview></ng-template>
      <ng-template #servoydefaultListbox let-state="state">
        <servoydefault-listbox *ngIf = "state.model.visible" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes"
                                   [onActionMethodID]="getHandler(state,'onActionMethodID')"
                                   [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')"
                                   [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')"
                                   [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')"
                                   [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')"
                                   [dataProviderID]="state.model.dataProviderID"
                                   (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)"
                                   [format]="state.model.format"
                                   [name]="state.name"
                                   [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled"
                                   [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)"
                                   [scrollbars]="state.model.scrollbars" [toolTipText]="state.model.toolTipText" [tabSeq]="state.model.tabSeq"
                                   [multiselectListbox]="state.model.multiselectListbox"
                                   [valuelistID]="state.model.valuelistID"#cmp></servoydefault-listbox>
      </ng-template>
      <ng-template #servoydefaultImagemedia let-state="state"><servoydefault-imagemedia  *ngIf = "state.model.visible" [borderType]="state.model.borderType" [text]="state.model.text" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [editable]="state.model.editable" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name"  [scrollbars]="state.model.scrollbars" [toolTipText]="state.model.toolTipText" [tabSeq]="state.model.tabSeq" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [dataProviderID]="state.model.dataProviderID" #cmp></servoydefault-imagemedia></ng-template>

      <ng-template #servoycoreErrorbean let-state="state"><servoycore-errorbean [error]="state.model.error" [servoyApi]="getServoyApi(state)" [toolTipText]="state.model.toolTipText" #cmp></servoycore-errorbean></ng-template>
      <ng-template #servoycoreSlider let-state="state"><servoycore-slider  *ngIf = "state.model.visible" [styleClass]="state.model.styleClass" [min]="state.model.min" [max]="state.model.max" [orientation]="state.model.orientation" [step]="state.model.step" [enabled]="state.model.enabled" [toolTipText]="state.model.toolTipText" [visible]="state.model.visible" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [onCreateMethodID]="getHandler(state,'onCreateMethodID')" [onSlideMethodID]="getHandler(state,'onSlideMethodID')" [onStartMethodID]="getHandler(state,'onStartMethodID')" [onStopMethodID]="getHandler(state,'onStopMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoycore-slider></ng-template>
      <ng-template #servoycoreListformcomponent let-state="state">
            <servoycore-listformcomponent [parentForm]="self"
                *ngIf = "state.model.visible"  
                (foundsetChange)="datachange(state.name,'foundset',$event)"
                [listFormComponent]="state" 
                [responsivePageSize]="state.model.responsivePageSize" 
                [pageLayout]="state.model.pageLayout" 
                [selectionChangedHandler]="getHandler(state,'onSelectionChanged')" 
                #cmp>
            </servoycore-listformcomponent>
       </ng-template>

      <ng-template #servoyextraTable let-state="state">
        <servoyextra-table *ngIf = "state.model.visible" [foundset]="state.model.foundset" 
                        (foundsetChange)="datachange(state.name,'foundset',$event)"
                        [name]="state.name"
                        [columns]="state.model.columns" 
                        [currentPage]="state.model.currentPage"
                        [pageSize]="state.model.pageSize"
                        [styleClass]="state.model.styleClass"
                        [sortStyleClass]="state.model.sortStyleClass"
                        [selectionClass]="state.model.selectionClass"
                        [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider"
                        [tabSeq]="state.model.tabSeq"
                        [visible]="state.model.visible"
                        [enableColumnResize]="state.model.enableColumnResize"
                        [enableSort]="state.model.enableSort"
                        [responsiveHeight]="state.model.responsiveHeight"
                        [responsiveDynamicHeight]="state.model.responsiveDynamicHeight"
                        [minRowHeight]="state.model.minRowHeight"
                        [sortupClass]="state.model.sortupClass"
                        [sortdownClass]="state.model.sortdownClass"
                        [sortColumnIndex]="state.model.sortColumnIndex"
                        [sortDirection]="state.model.sortDirection"
                        [lastSelectionFirstElement]="state.model.lastSelectionFirstElement"
                        [performanceSettings]="state.model.performanceSettings"
                        [keyCodeSettings]="state.model.keyCodeSettings"
                        [servoyApi]="getServoyApi(state)"
                        [servoyAttributes]="state.model.svy_attributes"
                        [onViewPortChanged]="getHandler(state,'onViewPortChanged')"
                        [onCellClick]="getHandler(state,'onCellClick')"
                        [onCellDoubleClick]="getHandler(state,'onCellDoubleClick')"
                        [onCellRightClick]="getHandler(state,'onCellRightClick')"
                        [onHeaderClick]="getHandler(state,'onHeaderClick')"
                        [onHeaderRightClick]="getHandler(state,'onHeaderRightClick')"
                        [onColumnResize]="getHandler(state,'onColumnResize')"
                        [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')"
                        [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')"  
                        [absoluteLayout]="formCache.absolute"                                        
        #cmp>
        </servoyextra-table>
    </ng-template>
    
    <ng-template #servoyextraHtmlarea let-state="state">
        <servoyextra-htmlarea *ngIf = "state.model.visible" 
                        [onActionMethodID]="getHandler(state,'onActionMethodID')"
                        [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')"
                        [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')"
                        [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')"
                        [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')"
                        [dataProviderID]="state.model.dataProviderID"
                        (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)"
                        [enabled]="state.model.enabled" 
                        [editable]="state.model.editable" 
                        [findmode]="state.model.findmode" 
                        [name]="state.name"
                        [placeholderText]="state.model.placeholderText" 
                        [readOnly]="state.model.readOnly" 
                        [responsiveHeight]="state.model.responsiveHeight" 
                        [servoyApi]="getServoyApi(state)"
                        [servoyAttributes]="state.model.svy_attributes" 
                        [scrollbars]="state.model.scrollbars" 
                        [styleClass]="state.model.styleClass" 
                        [tabSeq]="state.model.tabSeq" 
                        [text]="state.model.text" 
                        [toolTipText]="state.model.toolTipText" 
                        [visible]="state.model.visible" 
                        #cmp>
        </servoyextra-htmlarea>
    </ng-template>
        
    <ng-template #servoyextraImagelabel let-state="state">
            <servoyextra-imagelabel  *ngIf = "state.model.visible" 
                        [enabled]="state.model.enabled" 
                        [visible]="state.model.visible" 
                        [name]="state.name"  
                        [media]="state.model.media" 
                        [styleClass]="state.model.styleClass" 
                        [servoyApi]="getServoyApi(state)"
                        [servoyAttributes]="state.model.svy_attributes" 
                        [tabSeq]="state.model.tabSeq" 
                        [onActionMethodID]="getHandler(state,'onActionMethodID')" 
                        [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" 
                        #cmp>
            </servoyextra-imagelabel>
    </ng-template>
     
    <ng-template #bootstrapcomponentsCalendar let-state="state"><servoybootstrap-calendar  *ngIf = "state.model.visible" [visible]="state.model.visible" [format]="state.model.format" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [placeholderText]="state.model.placeholderText" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoybootstrap-calendar></ng-template>
    <ng-template #bootstrapcomponentsCalendarinline let-state="state"><servoybootstrap-calendarinline  *ngIf = "state.model.visible" [visible]="state.model.visible" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [toolTipText]="state.model.toolTipText"[onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')"[servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.svy_attributes" [name]="state.name" #cmp></servoybootstrap-calendarinline></ng-template>
    <ng-template #bootstrapcomponentsButton
        let-state="state">
        <servoybootstrap-button
            *ngIf = "state.model.visible" 
            [visible]="state.model.visible" 
            [showAs]="state.model.showAs" 
            [imageStyleClass]="state.model.imageStyleClass" 
            [styleClass]="state.model.styleClass" 
            [enabled]="state.model.enabled" 
            [size]="state.model.size" 
            [cssPosition]="state.model.cssPosition" 
            [location]="state.model.location" 
            [servoyAttributes]="state.model.svy_attributes" 
            [text]="state.model.text" 
            [tabSeq]="state.model.tabSeq" 
            [toolTipText]="state.model.toolTipText" 
            [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" 
            [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" 
            [onActionMethodID]="getHandler(state,'onActionMethodID')" 
            [servoyApi]="getServoyApi(state)" 
            [name]="state.name" 
            #cmp>
        </servoybootstrap-button>
    </ng-template>

    <ng-template #bootstrapcomponentsTablesspanel let-state="state"><servoybootstrap-tablesspanel  *ngIf = "state.model.visible" [visible]="state.model.visible" [containedForm]="state.model.containedForm" [styleClass]="state.model.styleClass" [size]="state.model.size" [relationName]="state.model.relationName" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [tabSeq]="state.model.tabSeq" [waitForData]="state.model.waitForData" [height]="state.model.height" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoybootstrap-tablesspanel></ng-template>
    <ng-template #bootstrapcomponentsSelect let-state="state"><servoybootstrap-select  *ngIf = "state.model.visible" [multiselect]="state.model.multiselect" [selectSize]="state.model.selectSize" [visible]="state.model.visible" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [valuelistID]="state.model.valuelistID" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [placeholderText]="state.model.placeholderText" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-select></ng-template>
    <ng-template #bootstrapcomponentsList let-state="state"><servoybootstrap-list  *ngIf = "state.model.visible" [visible]="state.model.visible" [editable]="state.model.editable" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [valuelistID]="state.model.valuelistID" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [placeholderText]="state.model.placeholderText" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-list></ng-template>
    <ng-template #bootstrapcomponentsTextbox let-state="state"><servoybootstrap-textbox  *ngIf = "state.model.visible" [visible]="state.model.visible" [autocomplete]="state.model.autocomplete" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [inputType]="state.model.inputType" (inputTypeChange)="datachange(state.name,'inputType',$event)" [placeholderText]="state.model.placeholderText" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-textbox></ng-template>
    <ng-template #bootstrapcomponentsDatalabel let-state="state"><servoybootstrap-datalabel  *ngIf = "state.model.visible" [showAs]="state.model.showAs" [visible]="state.model.visible" [format]="state.model.format" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" [enabled]="state.model.enabled" [valuelistID]="state.model.valuelistID" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-datalabel></ng-template>
    <ng-template #bootstrapcomponentsImagemedia let-state="state"><servoybootstrap-imagemedia  *ngIf = "state.model.visible" [media]="state.model.media"  [visible]="state.model.visible" [alternate]="state.model.alternate" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" [enabled]="state.model.enabled" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-imagemedia></ng-template>
    <ng-template #bootstrapcomponentsLabel let-state="state"><servoybootstrap-label *ngIf = "state.model.visible" [showAs]="state.model.showAs" [labelFor]="state.model.labelFor" [visible]="state.model.visible" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [text]="state.model.text" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-label></ng-template>
    <ng-template #bootstrapcomponentsTextarea let-state="state"><servoybootstrap-textarea  *ngIf = "state.model.visible" [maxLength]="state.model.maxLength" [visible]="state.model.visible" [editable]="state.model.editable" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [placeholderText]="state.model.placeholderText" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-textarea></ng-template>
    <ng-template #bootstrapcomponentsCheckbox let-state="state"><servoybootstrap-checkbox  *ngIf = "state.model.visible" [showAs]="state.model.showAs" [visible]="state.model.visible" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [text]="state.model.text" [tabSeq]="state.model.tabSeq" [selectedValue]="state.model.selectedValue" [toolTipText]="state.model.toolTipText" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-checkbox></ng-template>
    <ng-template #bootstrapcomponentsChoicegroup let-state="state"><servoybootstrap-choicegroup  *ngIf = "state.model.visible" [showAs]="state.model.showAs" [visible]="state.model.visible" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [valuelistID]="state.model.valuelistID" [findmode]="state.model.findmode" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [inputType]="state.model.inputType" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-choicegroup></ng-template>
    <ng-template #bootstrapcomponentsTabpanel let-state="state"><servoybootstrap-tabpanel  *ngIf = "state.model.visible" [visible]="state.model.visible" [containerStyleClass]="state.model.containerStyleClass" [tabs]="state.model.tabs" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [styleClass]="state.model.styleClass" [activeTabIndex]="state.model.activeTabIndex" [showTabCloseIcon]="state.model.showTabCloseIcon" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [closeIconStyleClass]="state.model.closeIconStyleClass" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [tabSeq]="state.model.tabSeq" [height]="state.model.height" [onTabClickedMethodID]="getHandler(state,'onTabClickedMethodID')" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [onTabCloseMethodID]="getHandler(state,'onTabCloseMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoybootstrap-tabpanel></ng-template>
    <ng-template #bootstrapcomponentsAccordion let-state="state"><servoybootstrap-accordion  *ngIf = "state.model.visible" [visible]="state.model.visible" [containerStyleClass]="state.model.containerStyleClass" [tabs]="state.model.tabs" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [styleClass]="state.model.styleClass" [activeTabIndex]="state.model.activeTabIndex" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [tabSeq]="state.model.tabSeq" [height]="state.model.height" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoybootstrap-accordion></ng-template>
    <ng-template #bootstrapcomponentsTypeahead let-state="state"><servoybootstrap-typeahead  *ngIf = "state.model.visible" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [valuelistID]="state.model.valuelistID" (valuelistIDChange)="datachange(state.name,'valuelistID',$event)" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [placeholderText]="state.model.placeholderText" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-typeahead></ng-template>
    <ng-template #bootstrapcomponentsCombobox let-state="state"><servoybootstrap-combobox  *ngIf = "state.model.visible" [visible]="state.model.visible" [format]="state.model.format" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [valuelistID]="state.model.valuelistID" (valuelistIDChange)="datachange(state.name,'valuelistID',$event)" [size]="state.model.size" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [servoyAttributes]="state.model.svy_attributes" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoybootstrap-combobox></ng-template>

    <ng-template #aggridGroupingtable let-state="state">
     <nggrids-datagrid [myFoundset]="state.model.myFoundset"
         (foundsetChange)="datachange(state.name,'myFoundset',$event)"
         [columns]="state.model.columns"
         [readOnly]="state.model.readOnly"
         [readOnlyColumnIds]="state.model.readOnlyColumnIds"
         [hashedFoundsets]="state.model.hashedFoundsets"
         [filterModel]="state.model.filterModel"
         [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider"
         [arrowsUpDownMoveWhenEditing]="state.model.arrowsUpDownMoveWhenEditing"
         [_internalExpandedState]="state.model._internalExpandedState"
         [_internalFormEditorValue]="state.model._internalFormEditorValue"
         [servoyApi]="getServoyApi(state)"
         [onSort]="getHandler(state,'onSort')"
         [onColumnFormEditStarted]="getHandler(state,'onColumnFormEditStarted')"
         #cmp>
     </nggrids-datagrid>
 </ng-template>
     <!-- component template generate end -->
   `
})

export class FormComponent implements OnInit, OnDestroy, OnChanges {
    @ViewChild('svyResponsiveDiv', { static: true }) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild('formComponentAbsoluteDiv', { static: true }) readonly formComponentAbsoluteDiv: TemplateRef<any>;
    @ViewChild('formComponentResponsiveDiv', { static: true }) readonly formComponentResponsiveDiv: TemplateRef<any>;
    // component template generate start
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

    @ViewChild('servoyextraTable', { static: true }) readonly servoyextraTable: TemplateRef<any>;
    @ViewChild('servoyextraHtmlarea', { static: true }) readonly servoyextraHtmlarea: TemplateRef<any>;
    @ViewChild('servoyextraImagelabel', { static: true }) readonly servoyextraImagelabel: TemplateRef<any>;

    @ViewChild('bootstrapcomponentsCalendar', { static: true }) readonly bootstrapcomponentsCalendar: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsCalendarinline', { static: true }) readonly bootstrapcomponentsCalendarinline: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsButton', { static: true }) readonly bootstrapcomponentsButton: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsCombobox', { static: true }) readonly bootstrapcomponentsCombobox: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsCheckbox', { static: true }) readonly bootstrapcomponentsCheckbox: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsChoicegroup', { static: true }) readonly bootstrapcomponentsChoicegroup: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsImagemedia', { static: true }) readonly bootstrapcomponentsImagemedia: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsList', { static: true }) readonly bootstrapcomponentsList: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsTablesspanel', { static: true }) readonly bootstrapcomponentsTablesspanel: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsLabel', { static: true }) readonly bootstrapcomponentsLabel: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsDatalabel', { static: true }) readonly bootstrapcomponentsDatalabel: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsTextarea', { static: true }) readonly bootstrapcomponentsTextarea: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsTextbox', { static: true }) readonly bootstrapcomponentsTextbox: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsSelect', { static: true }) readonly bootstrapcomponentsSelect: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsTabpanel', { static: true }) readonly bootstrapcomponentsTabpanel: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsAccordion', { static: true }) readonly bootstrapcomponentsAccordion: TemplateRef<any>;
    @ViewChild('bootstrapcomponentsTypeahead', { static: true }) readonly bootstrapcomponentsTypeahead: TemplateRef<any>;
    // component template generate end

    @ViewChild('aggridGroupingtable', { static: true }) readonly aggridGroupingtable: TemplateRef<any>;

    @ViewChildren('cmp') readonly components: QueryList<Component>;

    @Input() readonly name;

    formCache: FormCache;

    private handlerCache: { [property: string]: { [property: string]: (e) => void } } = {};
    private servoyApiCache: { [property: string]: ServoyApi } = {};
    private log: LoggerService;
    private self: FormComponent;

    constructor(private formservice: FormService, private sabloService: SabloService, private servoyService: ServoyService, private logFactory: LoggerFactory) {
        this.self = this;
        this.log = logFactory.getLogger('FormComponent');
    }

    ngOnChanges(changes: SimpleChanges) {
        this.ngOnInit();
    }

    getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return this.svyResponsiveDiv;
        }
        else if (item instanceof ListFormComponentCache) {
            return this.servoycoreListformcomponent;
        }
        else if (item instanceof FormComponentCache ) {
            return item.responsive ? this.formComponentResponsiveDiv : this.formComponentAbsoluteDiv;
        }
        else {
            if (this[item.type] === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ('Template for ' + item.type + ' was not found, please check form_component template.')));
            }
            return this[item.type]
        }
    }

    ngOnInit() {
        this.formCache = this.formservice.getFormCache(this);
        this.sabloService.callService('formService', 'formLoaded', { formname: this.name }, true);
    }

    ngOnDestroy() {
        this.formservice.destroy(this);
    }

    public getAbsoluteFormStyle() {
        const formData = this.formCache.getComponent('');
        const position = {
            left: '0px',
            top: '0px',
            right: '0px',
            bottom: '0px',
            position: 'absolute',
            minWidth: undefined,
            minHeight: undefined,
            backgroundColor: undefined
        };
        if (formData.model.borderType) {
            const borderStyle = formData.model.borderType;
            for (var key in borderStyle) {
                position[key] = position[key];
            }
        }
        if (formData.model.transparent) {
            position.backgroundColor = 'transparent';
        }

        if (formData.model.addMinSize) {
            position.minWidth = formData.model.size.width + 'px';
            position.minHeight = formData.model.size.height + 'px';
        }
        return position;
    }

    public isFormAvailable(name): boolean {
        //console.log("isFormAvailable: " + name + " " +  this.formservice.hasFormCacheEntry( name));
        return this.formservice.hasFormCacheEntry(name);
    }

    datachange(component: string, property: string, value) {
        const model = (!component.includes("formcomponent")) ? this.formCache.getComponent(component).model : this.formCache.getFormComponent(component).model;
        const oldValue = model[property];
        if (!component.includes("formcomponent")) {
            this.formCache.getComponent(component).model[property] = value;
        } else {
            this.formCache.getFormComponent(component).model[property] = value;
        }
        this.formservice.sendChanges(this.name, component, property, value, oldValue);
    }

    getHandler(item: ComponentCache, handler: string) {
        let itemCache = this.handlerCache[item.name];
        if (itemCache == null) {
            itemCache = {};
            this.handlerCache[item.name] = itemCache;
        }
        let func = itemCache[handler];
        if (func == null) {
            if(item.handlers instanceof Array && item.handlers.indexOf(handler) >= 0) {
                const me = this;
                func = function(e) {
                return me.formservice.executeEvent(me.name, item.name, handler, arguments);
                }
                itemCache[handler] = func;
            }
            else if(item.handlers && item.handlers[handler]){
                func = function(e) {
                    item.handlers[handler]();
                }
            }
        }
        return func;
    }

    getServoyApi(item: ComponentCache) {
        let api = this.servoyApiCache[item.name];
        if (api == null) {
            api = new ServoyApi(item, this.name, this.formCache.absolute, this.formservice, this.servoyService);
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    public callApi(componentName: string, apiName: string, args: object): any {
        let comp = this.components.find(item => item['name'] == componentName);
        let proto = Object.getPrototypeOf(comp)
        if (proto[apiName])
        {
            return proto[apiName].apply(comp, args); 
        }
        else
        {
            this.log.error(this.log.buildMessage(() => ('Api ' + apiName + ' for component '+ componentName +' was not found, please check component implementation.')));
            return null;
        }    
    }
}

@Directive({ selector: '[config]' })
export class AddAttributeDirective implements OnInit {
    @Input() config;

    constructor(private el: ElementRef, private renderer: Renderer2) { }

    ngOnInit() {
        if (this.config.classes) {
            this.config.classes.forEach(cls => this.renderer.addClass(this.el.nativeElement, cls));
        }
        if (this.config.styles) {
            for (let key in this.config.styles) {
                this.renderer.setStyle(this.el.nativeElement, key, this.config.styles[key]);
            }
        }
        if (this.config.layout) {
            for (let key in this.config.layout) {
                this.renderer.setStyle(this.el.nativeElement, key, this.config.layout[key]);
            }
        }
    }
}



