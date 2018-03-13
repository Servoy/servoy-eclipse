import { Injectable } from '@angular/core';

import { ConverterService } from './converter.service'

@Injectable()
export class SpecTypesService {

    private registeredTypes = new Map<string, typeof BaseCustomObject>();

    createType( name: string ): BaseCustomObject {
        const classRef = this.registeredTypes.get( name );
        if ( classRef ) {
            return new classRef();
        }
        console.log( "returning just the basic custom object for  " + name + " none of the properties will be monitored" );
        return new BaseCustomObject();
    }

    registerType( name: string, classRef: typeof BaseCustomObject, specProperties: Array<string> ) {
        this.registeredTypes.set( name, classRef );
        classRef["__specProperties"] = specProperties;
    }

    getProperties( classRef ): Array<string> {
        return classRef["__specProperties"];
    }
    
    guessType( val: any ): string {
        let guess = null;
        
        if ( BaseCustomObject.isBaseCustomObject( val ) ) {
            guess = "JSON_obj";
        } // else TODO add an if branch for arrays as well below
//        else { // try to find it in types?
//            this.registeredTypes.forEach(function(typeConstructorValue, typeNameKey) {
//                if (val instanceof typeConstructorValue) guess = typeNameKey; // this wouldn't return the converter name like 'JSON_obj' but rather the actual name from spec of the custom type like "(...).tab"
//            });
//        }
        return guess;
    }

}

export abstract class IChangeAwareValue {
    abstract getStateHolder(): ChangeAwareState;

    public static isChangeAwareValue( obj: any ): obj is IChangeAwareValue {
        return obj != null && ( <IChangeAwareValue>obj ).getStateHolder instanceof Function;
    }
}

export interface ICustomObject extends IChangeAwareValue {
    getStateHolder(): BaseCustomObjectState;
}

export class BaseCustomObject implements ICustomObject {
    private state = new BaseCustomObjectState();

    public getStateHolder() {
        return this.state;
    }

    public static isBaseCustomObject( obj: any ): obj is BaseCustomObject {
        return obj != null && ( <BaseCustomObject>obj ).state instanceof BaseCustomObjectState;
    }
}

export class ChangeAwareState {
    
    // TODO do we really need a list or is one notifier enough?
    private changeListener: () => void;
    
    public allChanged = true;
    
    public markAllChanged(notifyListener : boolean) {
        this.allChanged = true;
        if (notifyListener) this.notifyChangeListener();
    }

    public setChangeListener( callback: () => void ): void {
        this.changeListener = callback;
    }

    protected notifyChangeListener(): void {
        if (this.changeListener) this.changeListener();
    }
    
}

export class BaseCustomObjectState extends ChangeAwareState {
    private changedKeys = new Array<string>();

    public conversionInfo = {};
    public ignoreChanges = false;

    private setChangeListenerToSubValueIfNeeded( value: any, changeListener: () => void ): void {
        if ( IChangeAwareValue.isChangeAwareValue( value ) ) {
            // child is able to handle it's own change mechanism
            value.getStateHolder().setChangeListener( changeListener );
        }
    }
    
    public setPropertyAndHandleChanges(_thisBaseCustoomObject, internalPropertyName, propertyName, value) {
        const oldValue = _thisBaseCustoomObject[internalPropertyName];
        
        // if the value of this property is changed, mark it as such and notify if needed
        this.markIfChanged( propertyName, value, oldValue );
        
        // unregister as listener to old value if needed
        this.setChangeListenerToSubValueIfNeeded(oldValue, undefined);
        
        _thisBaseCustoomObject[internalPropertyName] = value;
        
        // register as listener to new value if needed
        this.setChangeListenerToSubValueIfNeeded(value, () => {
            this.markIfChanged( propertyName, value, value );
        });
        
        // this value has changed by reference; so it needs to be fully sent to server - except for when it now arrived from the server and is being set (in which case ignoreChanges is true)
        if (!this.ignoreChanges && IChangeAwareValue.isChangeAwareValue( value )) value.getStateHolder().markAllChanged(true);
    }


    protected markIfChanged( propertyName: string, newObject: any, oldObject: any ) {
        if ( this.testChanged( propertyName, newObject, oldObject ) ) {
            this.pushChange( propertyName );
            return true;
        }
        return false;
    }

    public getChangedKeys(): Array<string> {
        return this.changedKeys;
    }

    public clearChangedKeys() {
        this.changedKeys = new Array<string>();
    }

    private pushChange( propertyName ) {
        if ( this.ignoreChanges ) return;
        this.changedKeys.push( propertyName );
        this.notifyChangeListener();
    }

    private testChanged( propertyName: string, newObject: any, oldObject: any ) {
        if ( newObject !== oldObject ) return true;
        if ( typeof newObject == "object" ) {
            if ( this.instanceOfCustomObject( newObject ) ) {
                return newObject.getStateHolder().getChangedKeys().length > 0;
            } else {
                return ConverterService.isChanged( newObject, oldObject, this.conversionInfo[propertyName] );
            }
        }
        return false;
    }

    private instanceOfCustomObject( object: any ): object is ICustomObject {
        return 'getStateHolder' in object;
    }

}
