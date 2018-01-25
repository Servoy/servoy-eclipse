import { Component, Input,OnInit,OnDestroy,ViewChild,ViewChildren,TemplateRef,QueryList,Directive,ElementRef,Renderer2} from '@angular/core';

import {FormService,FormCache,StructureCache,ComponentCache} from '../form.service';


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
      <ng-template  #svyTextfield  let-state="state" >
        <svy-textfield [dataprovider]="state.model.dataprovider" (dataproviderChange)="datachange(state.name,'dataprovider',$event)" [name]="state.name" #cmp></svy-textfield>
      </ng-template>
      <ng-template  #svyButton let-state="state">
        <svy-button [dataprovider]="state.model.dataprovider" [click]="getHandler(state, 'click')" [name]="state.name" #cmp></svy-button>
      </ng-template>
   `
})
export class FormComponent implements OnInit, OnDestroy {
  @ViewChild('svyResponsiveDiv') readonly svyResponsiveDiv:TemplateRef<any>;
  @ViewChild('svyTextfield') readonly svyTextfield:TemplateRef<any>;
  @ViewChild('svyButton') readonly svyButton:TemplateRef<any>;
  
  @ViewChildren('cmp') readonly components:QueryList<Component>;
  
  @Input() readonly name;
  
  formCache:FormCache;
  
  constructor(private formservice:FormService) { 
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
  }
  
  ngOnDestroy() {
  }
  
  private datachange(component:string,property:string,value) {
      this.formCache.getComponent(component).model[property] = value;
     this.formservice.sendChanges(this.name, component, property,value);
  } 
  
  private getHandler(item:ComponentCache, handler:string) {
      if (item.handlers.indexOf(handler) >= 0) {
          var me = this;
          return function(e) {
              me.formservice.executeEvent(this.name, item.name, handler,arguments);
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

