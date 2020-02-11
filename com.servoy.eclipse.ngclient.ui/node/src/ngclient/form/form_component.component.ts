import { Component, Input, OnInit, OnDestroy, OnChanges,SimpleChanges, ViewChild, ViewChildren, TemplateRef, QueryList, Directive, ElementRef, Renderer2, NgModule } from '@angular/core';

import { FormService, FormCache, StructureCache, FormComponentCache, ComponentCache } from '../form.service';

import { ServoyService } from '../servoy.service'

import { SabloService } from '../../sablo/sablo.service'
import { LoggerService, LoggerFactory } from '../../sablo/logger.service'

import { ServoyApi } from '../servoy_api'

@Component( {
    selector: 'svy-form',
    template: `
      <div *ngIf="formCache.absolute" [ngStyle]="getAbsoluteFormStyle()"> <!-- main div -->
          <div *ngFor="let item of formCache.items" [config]="item" class="svy-wrapper" style="position:absolute"> <!-- wrapper div -->
               <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component  -->
          </div>
          <div *ngFor="let formComponent of formCache.formComponents" [config]="formComponent" style="position:absolute" [config]="formComponent"> <!-- main container div -->
            <ng-template *ngFor="let item of formComponent.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>
          </div>
      </div>
      <div *ngIf="!formCache.absolute" [config]="formCache.mainStructure"> <!-- main container div -->
            <ng-template *ngFor="let item of formCache.mainStructure.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component or responsive div  -->
      </div>
          
      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [config]="state">
                     <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>
          </div>
      </ng-template>
      <ng-template  #formComponentDiv  let-state="state" >
          <div [config]="state">
                     <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>
          </div>
      </ng-template>
      <!-- component template generate start -->
      <ng-template #servoydefaultTextfield let-state="state"><servoydefault-textfield  [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp></servoydefault-textfield></ng-template>
      <ng-template #servoydefaultTextarea let-state="state"><servoydefault-textarea  [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp></servoydefault-textarea></ng-template>
      <ng-template #servoydefaultButton let-state="state">
        <servoydefault-button [borderType]="state.model.borderType" [foreground]="state.model.foreground" [hideText]="state.model.hideText" 
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
                              [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp>
        </servoydefault-button>
      </ng-template>
      <ng-template #servoydefaultCombobox let-state="state">
        <servoydefault-combo [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes"
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
        <servoydefault-typeahead [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes"
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
      <ng-template #servoydefaultLabel let-state="state"><servoydefault-label  [borderType]="state.model.borderType" [labelFor]="state.model.labelFor" [foreground]="state.model.foreground" [hideText]="state.model.hideText" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [textRotation]="state.model.textRotation" [mnemonic]="state.model.mnemonic" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [imageMediaID]="state.model.imageMediaID" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [format]="state.model.format" [mediaOptions]="state.model.mediaOptions" [dataProviderID]="state.model.dataProviderID" [showFocus]="state.model.showFocus" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [tabSeq]="state.model.tabSeq" [verticalAlignment]="state.model.verticalAlignment" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp></servoydefault-label></ng-template>
      <ng-template #servoydefaultTabpanel let-state="state">
            <servoydefault-tabpanel  [borderType]="state.model.borderType" [fontType]="state.model.fontType" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs"  [readOnly]="state.model.readOnly" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp>
                <ng-template let-name='name'>
                    <svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form>
                </ng-template>
            </servoydefault-tabpanel>
      </ng-template>
      <ng-template #servoydefaultTablesspanel let-state="state">
            <servoydefault-tablesspanel  [borderType]="state.model.borderType" [fontType]="state.model.fontType" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs" [readOnly]="state.model.readOnly" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp>
                <ng-template let-name='name'>
                    <svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form>
                </ng-template>
            </servoydefault-tablesspanel>
      </ng-template>
      <ng-template #servoydefaultSplitpane let-state="state"><servoydefault-splitpane  [borderType]="state.model.borderType" [pane1MinSize]="state.model.pane1MinSize" [fontType]="state.model.fontType" [pane2MinSize]="state.model.pane2MinSize" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs" (tabsChange)="datachange(state.name,'tabs',$event)" [readOnly]="state.model.readOnly" [foreground]="state.model.foreground" [divLocation]="state.model.divLocation" (divLocationChange)="datachange(state.name,'divLocation',$event)" [resizeWeight]="state.model.resizeWeight" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [divSize]="state.model.divSize" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoydefault-splitpane></ng-template>
      <ng-template #servoydefaultCalendar let-state="state"><servoydefault-calendar  [borderType]="state.model.borderType" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [horizontalAlignment]="state.model.horizontalAlignment" [findmode]="state.model.findmode" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [placeholderText]="state.model.placeholderText" [selectOnEnter]="state.model.selectOnEnter" [text]="state.model.text" [tabSeq]="state.model.tabSeq" [toolTipText]="state.model.toolTipText" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp></servoydefault-calendar></ng-template>
      <ng-template #servoydefaultCheck let-state="state"><servoydefault-check [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [format]="state.model.format" [foreground]="state.model.foreground"  [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [name]="state.name" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly"[scrollbars]="state.model.scrollbars" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" (sizeChange)="datachange(state.name,'size',$event)" (locationChange)="datachange(state.name,'location',$event)" [servoyAttributes]="state.model.attributes" #cmp></servoydefault-check></ng-template>
      <ng-template #servoydefaultCheckgroup let-state="state"><servoydefault-checkgroup [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [format]="state.model.format" [foreground]="state.model.foreground"  [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [name]="state.name" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" (sizeChange)="datachange(state.name,'size',$event)" (locationChange)="datachange(state.name,'location',$event)" #cmp></servoydefault-checkgroup></ng-template>
      <ng-template #servoydefaultRadiogroup let-state="state"><servoydefault-radiogroup [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [format]="state.model.format" [foreground]="state.model.foreground"  [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [name]="state.name" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [scrollbars]="state.model.scrollbars" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" #cmp></servoydefault-radiogroup></ng-template>
      <ng-template #servoydefaultPassword let-state="state"><servoydefault-password [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" [displaysTags]="state.model.displaysTags" [editable]="state.model.editable" [enabled]="state.model.enabled" [findmode]="state.model.findmode" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [placeholderText]="state.model.placeholderText" [readOnly]="state.model.readOnly" [selectOnEnter]="state.model.selectOnEnter" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [visible]="state.model.visible" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" #cmp></servoydefault-password></ng-template>     
      <ng-template #servoydefaultHtmlarea let-state="state"><servoydefault-htmlarea [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [editable]="state.model.editable" [name]="state.name" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [visible]="state.model.visible" #cmp></servoydefault-htmlarea></ng-template>
      <ng-template #servoydefaultSpinner let-state="state"><servoydefault-spinner [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [background]="state.model.background" [borderType]="state.model.borderType" [dataProviderID]="state.model.dataProviderID"  (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [displaysTags]="state.model.displaysTags" [enabled]="state.model.enabled" [editable]="state.model.editable" [name]="state.name" [fontType]="state.model.fontType" [foreground]="state.model.foreground" [horizontalAlignment]="state.model.horizontalAlignment" [location]="state.model.location" [margin]="state.model.margin" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [visible]="state.model.visible" #cmp></servoydefault-spinner></ng-template>
      <ng-template #servoydefaultRectangle let-state="state"><servoydefault-rectangle  [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [lineSize]="state.model.lineSize" [roundedRadius]="state.model.roundedRadius" [shapeType]="state.model.shapeType" [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp></servoydefault-rectangle></ng-template>
      <ng-template #servoydefaultHtmlview let-state="state"><servoydefault-htmlview  [borderType]="state.model.borderType" [text]="state.model.text" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name"  [scrollbars]="state.model.scrollbars" [toolTipText]="state.model.toolTipText" [tabSeq]="state.model.tabSeq" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [dataProviderID]="state.model.dataProviderID" #cmp></servoydefault-htmlview></ng-template>
      <ng-template #servoydefaultListbox let-state="state">
        <servoydefault-listbox [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes"
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
      <ng-template #servoydefaultImagemedia let-state="state"><servoydefault-imagemedia  [borderType]="state.model.borderType" [text]="state.model.text" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [editable]="state.model.editable" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [visible]="state.model.visible" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name"  [scrollbars]="state.model.scrollbars" [toolTipText]="state.model.toolTipText" [tabSeq]="state.model.tabSeq" [onActionMethodID]="getHandler(state,'onActionMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [dataProviderID]="state.model.dataProviderID" #cmp></servoydefault-imagemedia></ng-template>
      
      <ng-template #servoycoreErrorbean let-state="state"><servoycore-errorbean [error]="state.model.error" [servoyApi]="getServoyApi(state)" [toolTipText]="state.model.toolTipText" #cmp></servoycore-errorbean></ng-template>
      <ng-template #servoycoreSlider let-state="state"><servoycore-slider  [styleClass]="state.model.styleClass" [min]="state.model.min" [max]="state.model.max" [orientation]="state.model.orientation" [step]="state.model.step" [enabled]="state.model.enabled" [toolTipText]="state.model.toolTipText" [visible]="state.model.visible" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [onCreateMethodID]="getHandler(state,'onCreateMethodID')" [onSlideMethodID]="getHandler(state,'onSlideMethodID')" [onStartMethodID]="getHandler(state,'onStartMethodID')" [onStopMethodID]="getHandler(state,'onStopMethodID')" [servoyApi]="getServoyApi(state)" [servoyAttributes]="state.model.attributes" [name]="state.name" #cmp></servoycore-slider></ng-template>
      
      <ng-template #servoydefaultTable let-state="state"><servoydefault-table [foundset]="state.model.foundset" [columns]="state.model.columns" #cmp></servoydefault-table></ng-template>
      <!-- component template generate end -->
   `
} )

export class FormComponent implements OnInit, OnDestroy, OnChanges {
    @ViewChild( 'svyResponsiveDiv' ,{static: true}) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild( 'formComponentDiv' ,{static: true}) readonly formComponentDiv: TemplateRef<any>;
    // component template generate start
    @ViewChild( 'servoydefaultTextfield' ,{static: true}) readonly servoydefaultTextfield: TemplateRef<any>;
    @ViewChild( 'servoydefaultTextarea' ,{static: true}) readonly servoydefaultTextarea: TemplateRef<any>;
    @ViewChild( 'servoydefaultButton',{static: true} ) readonly servoydefaultButton: TemplateRef<any>;
    @ViewChild( 'servoydefaultLabel' ,{static: true}) readonly servoydefaultLabel: TemplateRef<any>;
    @ViewChild( 'servoydefaultRectangle' ,{static: true}) readonly servoydefaultRectangle: TemplateRef<any>;
    @ViewChild( 'servoydefaultTabpanel',{static: true}) readonly servoydefaultTabpanel: TemplateRef<any>;
    @ViewChild( 'servoydefaultTablesspanel' ,{static: true}) readonly servoydefaultTablesspanel: TemplateRef<any>;
    @ViewChild( 'servoydefaultSplitpane',{static: true} ) readonly servoydefaultSplitpane: TemplateRef<any>;
    @ViewChild( 'servoydefaultCalendar',{static: true} ) readonly servoydefaultCalendar: TemplateRef<any>;
    
    @ViewChild( 'servoydefaultCombobox',{static: true}) readonly servoydefaultCombobox: TemplateRef<any>;
    @ViewChild( 'servoydefaultTypeahead' ,{static: true}) readonly servoydefaultTypeahead: TemplateRef<any>;
    @ViewChild( 'servoydefaultCheck' ,{static: true}) readonly servoydefaultCheck: TemplateRef<any>;
    @ViewChild( 'servoydefaultCheckgroup' ,{static: true}) readonly servoydefaultCheckgroup: TemplateRef<any>;
    @ViewChild( 'servoydefaultRadiogroup',{static: true} ) readonly servoydefaultRadiogroup: TemplateRef<any>;
    @ViewChild( 'servoydefaultPassword' , {static: true}) readonly servoydefaultPassword: TemplateRef<any>;
    @ViewChild( 'servoydefaultHtmlarea' , {static: true}) readonly servoydefaultHtmlarea: TemplateRef<any>;
    @ViewChild( 'servoydefaultSpinner', {static: true}) readonly servoydefaultSpinner: TemplateRef<any>;
    @ViewChild( 'servoydefaultHtmlview',{static: true} ) readonly servoydefaultHtmlview: TemplateRef<any>;
    @ViewChild( 'servoydefaultListbox' ,{static: true}) readonly servoydefaultListbox: TemplateRef<any>;
    @ViewChild( 'servoydefaultImagemedia' ,{static: true}) readonly servoydefaultImagemedia: TemplateRef<any>;
    @ViewChild( 'servoycoreSlider' ,{static: true}) readonly servoycoreSlider: TemplateRef<any>;
    @ViewChild( 'servoycoreErrorbean' ,{static: true}) readonly servoycoreErrorbean: TemplateRef<any>;
    
    @ViewChild( 'servoydefaultTable' ,{static: true}) readonly servoydefaultTable: TemplateRef<any>;
    
  // component template generate end

    @ViewChildren( 'cmp' ) readonly components: QueryList<Component>;

    @Input() readonly name;

    formCache: FormCache;

    private handlerCache: { [property: string]: { [property: string]: ( e ) => void } } = {};
    private servoyApiCache: { [property: string]: ServoyApi } = {};
    private log: LoggerService;

    constructor( private formservice: FormService, private sabloService: SabloService, private servoyService: ServoyService, private logFactory : LoggerFactory ) {
        this.log = logFactory.getLogger("FormComponent");
    }
    
    ngOnChanges( changes: SimpleChanges ) {
        this.ngOnInit();
    }
    
    getTemplate( item: StructureCache | ComponentCache | FormComponentCache ): TemplateRef<any> {
        if ( item instanceof StructureCache ) {
            return this.svyResponsiveDiv;
        } else if ( item instanceof FormComponentCache ) {
            return this.formComponentDiv;
        }
        else {
            if (this[item.type] === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ("Template for "+item.type+" was not found, please check form_component template.")));
            }
            return this[item.type]
        }
    }

    ngOnInit() {
        this.formCache = this.formservice.getFormCache( this );
        this.sabloService.callService( 'formService', 'formLoaded', { formname: this.name }, true );
    }

    ngOnDestroy() {
        this.formservice.destroy(this);
    }
    
    public getAbsoluteFormStyle() {
        const formData = this.formCache.getComponent("");
//        console.log(formData)
        var position = {position:"absolute"};
        if(this.formCache.getComponent('svy_default_navigator') != null) {
            position['left'] = "70px";
        } 
        else if (this.formCache.navigatorForm &&  this.formCache.navigatorForm.name && this.formCache.navigatorForm.name.lastIndexOf("default_navigator_container.html") == -1)
        {
            position['left'] = this.formCache.navigatorForm.size.width + "px";
        }
        
        return position;
    }

    public isFormAvailable( name ): boolean {
        //console.log("isFormAvailable: " + name + " " +  this.formservice.hasFormCacheEntry( name));
        return this.formservice.hasFormCacheEntry( name );
    }

    private datachange( component: string, property: string, value ) {
        const model = this.formCache.getComponent( component ).model;
        const oldValue = model[property];
        this.formCache.getComponent( component ).model[property] = value;
        this.formservice.sendChanges( this.name, component, property, value, oldValue );
    }

    private getHandler( item: ComponentCache, handler: string ) {
        let itemCache = this.handlerCache[item.name];
        if ( itemCache == null ) {
            itemCache = {};
            this.handlerCache[item.name] = itemCache;
        }
        let func = itemCache[handler];
        if ( func == null ) {
            if ( item.handlers && item.handlers.indexOf( handler ) >= 0 ) {
                const me = this;
                func = function( e ) {
                    me.formservice.executeEvent( me.name, item.name, handler, arguments );
                }
                itemCache[handler] = func;
            }
        }
        return func;
    }

    private getServoyApi( item: ComponentCache ) {
        let api = this.servoyApiCache[item.name];
        if ( api == null ) {
            api = new ServoyApi( item, this.name, this.formCache.absolute, this.formservice, this.servoyService );
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    public callApi( componentName: string, apiName: string, args: object ) : any{
        let comp = this.components.find( item => item['name'] == componentName );
        let proto = Object.getPrototypeOf( comp )
        return proto[apiName].apply( comp, args );
    }
}

@Directive( { selector: '[config]' } )
export class AddAttributeDirective implements OnInit {
    @Input() config;

    constructor( private el: ElementRef, private renderer: Renderer2 ) { }

    ngOnInit() {
        if ( this.config.classes ) {
            this.config.classes.forEach( cls => this.renderer.addClass( this.el.nativeElement, cls ) );
        }
        if ( this.config.styles ) {
            for ( let key in this.config.styles ) {
                this.renderer.setStyle( this.el.nativeElement, key, this.config.styles[key] );
            }
        }
        if ( this.config.layout ) {
            for ( let key in this.config.layout ) {
                this.renderer.setStyle( this.el.nativeElement, key, this.config.layout[key] );
            }
        }
    }
}



