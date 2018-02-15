import { Component, Input,OnInit,OnDestroy,ViewChild,ViewChildren,TemplateRef,QueryList,Directive,ElementRef,Renderer2} from '@angular/core';

import {FormService,FormCache,StructureCache,ComponentCache} from '../form.service';

import {SabloService} from '../../sablo/sablo.service'


@Component({
  selector: 'svy-form',
  template: `
          <div *ngIf="formCache.absolute"> <!-- main div -->
              <div *ngFor="let item of formCache.items" [config]="item" class="svy_wrapper" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component  -->
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
      <ng-template  #servoydefaultTextfield  let-state="state" >
        <servoydefault-textfield [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [name]="state.name" #cmp></servoydefault-textfield>
      </ng-template>
      <ng-template  #servoydefaultButton let-state="state">
        <servoydefault-button [text]="state.model.text" [onActionMethodID]="getHandler(state, 'onActionMethodID')" [name]="state.name" #cmp></servoydefault-button>
      </ng-template>
   `
})
export class FormComponent implements OnInit, OnDestroy {
  @ViewChild('svyResponsiveDiv') readonly svyResponsiveDiv:TemplateRef<any>;
  @ViewChild('servoydefaultTextfield') readonly servoydefaultTextfield:TemplateRef<any>;
  @ViewChild('servoydefaultButton') readonly servoydefaultButton:TemplateRef<any>;
  
  @ViewChildren('cmp') readonly components:QueryList<Component>;
  
  @Input() readonly name;
  
  formCache:FormCache;
  
  constructor(private formservice:FormService, private sabloService:SabloService) { 
  }
  
  getTemplate(item:StructureCache|ComponentCache):TemplateRef<any> {
      if (item instanceof StructureCache) {
          return this.svyResponsiveDiv;
      }
      else {
          return this[item.type]
      }
  }

  ngOnInit() {
      this.formCache = this.formservice.getFormCache(this);
      this.sabloService.callService('formService', 'formLoaded', { formname: this.name }, true)
  }
  
  ngOnDestroy() {
  }
  
  private datachange(component:string,property:string,value) {
      const model = this.formCache.getComponent(component).model;
      const oldValue = model[property];
      this.formCache.getComponent(component).model[property] = value;
     this.formservice.sendChanges(this.name, component, property,value,oldValue);
  } 
  
  private getHandler(item:ComponentCache, handler:string) {
      if (item.handlers && item.handlers.indexOf(handler) >= 0) {
          var me = this;
          return function(e) {
              me.formservice.executeEvent(me.name, item.name, handler,arguments);
          }
      }
  }
  
  public callApi(componentName:string, apiName:string,args:object) {
      var comp = this.components.find(item =>item['name'] == componentName);
      var proto = Object.getPrototypeOf(comp)
      proto[apiName].apply(comp,args);
  }
}

@Directive({ selector: '[config]' })
export class AddAttributeDirective implements OnInit {
    @Input() config;
    
    constructor(private el: ElementRef, private renderer:Renderer2) {}
    
    ngOnInit() {
        if (this.config.classes) {
            this.config.classes.forEach( cls => this.renderer.addClass(this.el.nativeElement, cls));
        }
        if (this.config.styles) {
           for (var key in this.config.styles) {
               this.renderer.setStyle(this.el.nativeElement, key, this.config.styles[key]);
           }
        }
        if (this.config.layout) {
            for (var key in this.config.layout) {
                this.renderer.setStyle(this.el.nativeElement, key, this.config.layout[key]);
            }
         }
    }
}

