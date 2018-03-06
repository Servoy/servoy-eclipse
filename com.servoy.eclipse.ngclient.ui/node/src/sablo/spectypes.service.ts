import { Injectable } from '@angular/core';

import {ConverterService} from './converter.service'

@Injectable()
export class SpecTypesService {
    private registeredTypes = new Map<string,typeof BaseCustomObject>();
    private registeredProperties = new Map<string,typeof BaseCustomObject>();
    
    createType(name:string):BaseCustomObject {
       const classRef = this.registeredTypes.get(name);
       if (classRef) {
           return new classRef();
       }
       console.log("returning just the basic custom object for  " + name + " none of the properties will be monitored");
       return new BaseCustomObject();
    }
    
    registerType(name:string, classRef:typeof BaseCustomObject, specProperties:Array<string>) {
      this.registeredTypes.set(name, classRef);  
      classRef["__specProperties"] = specProperties;
    }
    
    getProperties(classRef):Array<string> {
        return classRef["__specProperties"];
    }
}

export interface ICustomObject {
    getStateHolder():State;
}

export class BaseCustomObject implements ICustomObject {
    private state = new State();
    
    public getStateHolder() {
        return this.state;
    }
}

export class State {
    private changes = new Array<string>();
    private notifiers = new Array<()=>void>();
    
    public conversionInfo = {};
    public ignoreChanges = false;
    
    public allChanged = true;
    
    public markIfChanged(propertyName:string,newObject:any, oldObject:any) {
        if (this.testChanged(propertyName, newObject, oldObject)) {
            this.pushChange(propertyName);
            return true;
        }
        return false;
    }
    
    public getChanges():Array<string> {
        return this.changes;
    }
    
   public clearChanges() {
        this.changes = new Array<string>();
    }
    
   public addChangeNotifier(callback:()=>void) {
        this.notifiers.push(callback);
    }
    
    private pushChange(propertyName) {
        if (this.ignoreChanges) return;
        this.changes.push(propertyName);
        this.notifiers.forEach((callback) => {
            callback();
        })
    }
    
    private testChanged(propertyName:string,newObject:any, oldObject:any) {
        if (newObject !== oldObject) return true;
        if (typeof newObject == "object") {
            if (this.instanceOfCustomObject(newObject))  { 
                return newObject.getStateHolder().getChanges().length > 0;
            }
            else {
                return ConverterService.isChanged( newObject, oldObject, this.conversionInfo[propertyName] );
             }
        }
        return false;
    }
    
    private  instanceOfCustomObject(object: any): object is ICustomObject {
        return 'getStateHolder' in object;
    }

}
