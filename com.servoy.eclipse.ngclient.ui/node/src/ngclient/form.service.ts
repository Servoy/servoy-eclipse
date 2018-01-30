import { Injectable,TemplateRef } from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {WebsocketService} from '../sablo/websocket.service';

import {FormComponent} from './svy-form/svy-form.component'

@Injectable()
export class FormService {
    
    private formsCache:Map<String,FormCache>;

  constructor(private websocket:WebsocketService) {
      
      this.formsCache = new Map();
      this.websocket.messages.filter(message=>message.formname != null).subscribe(message=>{
          var formCache = this.formsCache.get(message.formname as string);
          if (formCache != null) {
              var comp = formCache.getComponent(message.componentname as string);
              comp.model[message.property as string] = message.value;
          }
      }); 
      
      // some sample data
      var absolute= true;
      var formCache = new FormCache();
      
      if (absolute) {
          formCache.add(new ComponentCache("text1","svyTextfield", {dataprovider:4},[],{left:"10px",top:"10px"}));
          formCache.add(new ComponentCache("text2","svyTextfield", {dataprovider:5},[],{left:"20px",top:"40px"}));
          formCache.add(new ComponentCache("button1","svyButton", {dataprovider:6},['click'],{left:"10px",top:"70px"}));
          formCache.add(new ComponentCache("button2","svyButton", {dataprovider:7},[],{left:"30px",top:"100px"}));
      }
      else {
          var main = new StructureCache(["container-fluid"]);
          var row = main.addChild(new StructureCache(["row"]) );
          row.addChild(new StructureCache(["col-md-4"],[new ComponentCache("text1","svyTextfield", {dataprovider:4},[],{})]));
          row.addChild(new StructureCache(["col-md-4"],[new ComponentCache("text2","svyTextfield", {dataprovider:5},[],{})])); 
          row.addChild(new StructureCache(["col-md-4"],[new ComponentCache("button1","svyButton", {dataprovider:6},['click'],{}),
                                                              new ComponentCache("button2","svyButton", {dataprovider:7},[],{})])); 
          formCache.mainStructure = main;
      }
      this.formsCache.set("test", formCache);
  }
  
  public getFormCache(form:FormComponent):FormCache {
      var formCache = this.formsCache.get(form.name);
      if (formCache != null) return formCache;

  }
  
  public sendChanges(formname:string,componentname:string,property:string,value:object) {
      this.websocket.sendChanges(formname, componentname , property ,  value);
  }
  
  public executeEvent(formname:string, componentname:string, handler:string,args:IArguments) {
      this.websocket.executeEvent(formname, componentname , handler ,  args);
  }
}

export class FormCache {
    private componentCache:Map<String,ComponentCache>;
    private _mainStructure:StructureCache;

    private _items:Array<ComponentCache>;

    constructor() {
        this.componentCache = new Map();
        this._items = [];
    }
    public add(comp:ComponentCache) {
        this.componentCache.set(comp.name, comp);
        this._items.push(comp);
    }
    set  mainStructure(structure:StructureCache) {
        this._mainStructure = structure;
        this.findComponents(structure);
   }
    
    get mainStructure():StructureCache {
        return this._mainStructure;
    }
    
    get absolute():boolean  {
        return this._mainStructure == null;
    }
    get items():Array<ComponentCache> {
      return  this._items;
    }
    
    public getComponent(name:string):ComponentCache {
        return this.componentCache.get(name);
    }
    
    private findComponents(structure:StructureCache) {
        structure.items.forEach(item => {
            if (item instanceof StructureCache) {
                this.findComponents(item);
            }
            else {
                this.add(item);
            }
        })
    }
}

export class ComponentCache {
    constructor(public  readonly name:string, 
                            public readonly type, 
                            public readonly model:{ [property: string]:Object }, 
                            public readonly handlers:Array<String>,
                            public readonly layout:{ [property: string]:string }) {
    }
}

export class StructureCache {
    constructor(public readonly classes:Array<string>, 
                           public readonly  items?:Array<StructureCache|ComponentCache>) {
        if (!this.items) this.items = [];
    }
    
    addChild(child:StructureCache|ComponentCache):StructureCache{
        this.items.push(child);
        if (child instanceof StructureCache)
            return child as StructureCache;
        return null;
    }
}
