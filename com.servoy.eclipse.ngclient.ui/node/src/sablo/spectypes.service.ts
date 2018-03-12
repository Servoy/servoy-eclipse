import { Injectable } from '@angular/core';

import {ConverterService} from './converter.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';


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
    
    enhanceArrayType(array:Array<any>, iterableDiffers: IterableDiffers ) {
        if (!instanceOfCustomObject(array)) {
            array["stateHolder"] = new ArrayState(array,iterableDiffers);
            Object.defineProperty(array, 'getStateHolder', {
                enumerable: false,
                value: function() { return this.stateHolder}
            });
            Object.defineProperty(array, 'markForChanged', {
                enumerable: false,
                value: function() {  this.stateHolder.notify()}
            });
            array["stateHolder"].initDiffer(); 
        }
    }
    
    registerType(name:string, classRef:typeof BaseCustomObject, specProperties:Array<string>) {
      this.registeredTypes.set(name, classRef);  
      classRef["__specProperties"] = specProperties;
    }
    
    getProperties(classRef):Array<string> {
        return classRef["__specProperties"];
    }
}


export function  instanceOfCustomObject(object: any): object is ICustomObject {
    return object.getStateHolder != undefined;
}

export function  instanceOfCustomArray<T>(object: any): object is ICustomArray<T> {
    return object.getStateHolder != undefined && object.markForChanged != undefined;
}


export interface ICustomObject {
    getStateHolder():State;
}

export interface ICustomArray<T> extends Array<T> {
    getStateHolder():ArrayState;
    markForChanged():void;
}

export class BaseCustomObject implements ICustomObject {
    private state = new State();
    
    public getStateHolder() {
        return this.state;
    }
}

export class State  {
    private static counter = 0;
    
    private hash = State.counter++;
    private change = 0;
    
    private notifiers = new Array<()=>void>();

 private changes = new Array<string|number>();
 public conversionInfo = {};
    public ignoreChanges = false;
    
    public allChanged = true;
    
    public markIfChanged(propertyName:string|number,newObject:any, oldObject:any) {
        if (this.testChanged(propertyName, newObject, oldObject)) {
            this.pushChange(propertyName);
            return true;
        }
        return false;
    }
    
    public getChanges():Array<string|number> {
        return this.changes;
    }
    
   public clearChanges() {
        this.changes = new Array<string>();
        this.allChanged = false;
    }

   public addChangeNotifier(callback:()=>void) {
       this.notifiers.push(callback);
   }
   
   public notify() {
       this.notifiers.forEach((callback) => {
           callback();
       })
   }
   
   public getHashKey():string {
       return this.hash + "_" + this.change;
   }
   
   protected testChanged(propertyName:string|number,newObject:any, oldObject:any) {
       if (newObject !== oldObject) return true;
       if (typeof newObject == "object") {
           if (instanceOfCustomObject(newObject))  { 
               return newObject.getStateHolder().getChanges().length > 0;
           }
           else {
               return ConverterService.isChanged( newObject, oldObject, this.conversionInfo[propertyName] );
            }
       }
       return false;
   }
    
    
    private pushChange(propertyName:string|number) {
        if (this.ignoreChanges) return;
        if (this.changes.length == 0) this.change++;
        this.changes.push(propertyName);
        this.notify();
    }
}

export class ArrayState extends State {
    private differ:IterableDiffer<Array<any>>;

    constructor(private array:Array<any>, private iterableDiffers: IterableDiffers) {
        super();
	}
    
   public  initDiffer() {
       this.differ = this.iterableDiffers.find( this.array).create(  (index: number, item:any) => {
           if (instanceOfCustomObject(item)) {
               return item.getStateHolder().getHashKey();
           }
           return item;
       } );
        this.differ.diff( this.array );
    }
   
   public clearChanges() {
       super.clearChanges();
       this.initDiffer()
   }
    
    public getChanges():Array<string|number> {
        const changes = super.getChanges();
        const arrayChanges = this.differ.diff(this.array);
        if ( arrayChanges ) {
            let addedOrRemoved = 0;
            arrayChanges.forEachAddedItem(( record ) => {
                addedOrRemoved++;
                if (changes.indexOf(record.currentIndex) == -1) {
                    changes.push(record.currentIndex)
                }
            } );
            arrayChanges.forEachRemovedItem(( record ) => {
                addedOrRemoved--;
                if (changes.indexOf(record.previousIndex) == -1) {
                    changes.push(record.previousIndex)
                }
            } );
            
            arrayChanges.forEachMovedItem((record) => {
                if (instanceOfCustomObject(record.item)) {
                    return record.item.getStateHolder().allChanged = true;
                }
            })
            if ( addedOrRemoved != 0 ) {
                // size changed, for now send whole array
                this.allChanged = true;
            }
            else {
                changes.sort((a:number, b:number) => {
                    return a - b;
                })
            }
        }
        return changes;
    }
}
