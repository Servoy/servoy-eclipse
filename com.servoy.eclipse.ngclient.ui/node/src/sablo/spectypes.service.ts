import { Injectable } from '@angular/core';

import { ConverterService } from './converter.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';


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

    enhanceArrayType<T>( array: Array<T>, iterableDiffers: IterableDiffers ): ICustomArray<T> {
        if ( !instanceOfChangeAwareValue( array ) ) {
            array["stateHolder"] = new ArrayState( array, iterableDiffers );
            Object.defineProperty( array, 'getStateHolder', {
                enumerable: false,
                value: function() { return this.stateHolder }
            } );
            Object.defineProperty( array, 'markForChanged', {
                enumerable: false,
                value: function() { this.stateHolder.notifyChangeListener() }
            } );
            array["stateHolder"].initDiffer();
        }
        return <ICustomArray<T>>array;
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

        if ( instanceOfCustomArray( val ) ) {
            guess = "JSON_arr";
        } else if ( instanceOfBaseCustomObject( val ) ) {
            guess = "JSON_obj";
        } // else TODO do any other types need guessing? 
        //        else { // try to find it in types?
        //            this.registeredTypes.forEach(function(typeConstructorValue, typeNameKey) {
        //                if (val instanceof typeConstructorValue) guess = typeNameKey; // this wouldn't return the converter name like 'JSON_obj' but rather the actual name from spec of the custom type like "(...).tab"
        //            });
        //        }
        return guess;
    }
}

export function instanceOfChangeAwareValue( obj: any ): obj is IChangeAwareValue {
    return obj != null && ( <IChangeAwareValue>obj ).getStateHolder instanceof Function;
}

export function instanceOfCustomArray<T>( obj: any ): obj is ICustomArray<T> {
    return instanceOfChangeAwareValue( obj ) && ( <ICustomArray<T>>obj ).markForChanged instanceof Function;
}

export function instanceOfBaseCustomObject( obj: any ): obj is BaseCustomObject {
    return instanceOfChangeAwareValue( obj ) && ( <BaseCustomObject>obj ).getStateHolder() instanceof BaseCustomObjectState;
}

export interface IChangeAwareValue {
    getStateHolder(): ChangeAwareState;
}

export interface ICustomObject extends IChangeAwareValue {
    getStateHolder(): BaseCustomObjectState;
}

export interface ICustomArray<T> extends Array<T>, IChangeAwareValue {
    getStateHolder(): ArrayState;
    markForChanged(): void;
}

export interface IValuelist extends Array<any>  {
    filterList(filterString:string): Promise<any>;
    getDisplayValue(realValue:any): Promise<any>;
    hasRealValues(): boolean;
}

export class BaseCustomObject implements ICustomObject {
    private state = new BaseCustomObjectState();

    public getStateHolder() {
        return this.state;
    }

}

export class ChangeAwareState {

    private changeListener: () => void;

    protected _allChanged = true;

    markAllChanged( notifyListener: boolean ): void {
        this._allChanged = true;
        if ( notifyListener ) this.notifyChangeListener();
    }

    hasChanges(): boolean {
        return this._allChanged;
    }

    get allChanged() {
        return this._allChanged;
    }

    setChangeListener( callback: () => void ): void {
        this.changeListener = callback;
    }

    public notifyChangeListener(): void {
        if ( this.changeListener ) this.changeListener();
    }

}

export class BaseCustomObjectState extends ChangeAwareState {

    // provide a hash that lets arrays that contain custom objects know that the object has changed or not
    private static counter = 0;
    private change = 0;
    private hash = BaseCustomObjectState.counter++;

    private changedKeys = new Array<string | number>();

    public conversionInfo = {};
    public ignoreChanges = false;

    hasChanges() {
        return super.hasChanges() || this.getChangedKeys().length > 0; // leave this as a method call as some subclasses might compute the changedKeys inside getChangedKeys()
    }

    private setChangeListenerToSubValueIfNeeded( value: any, changeListener: () => void ): void {
        if ( instanceOfChangeAwareValue( value ) ) {
            // child is able to handle it's own change mechanism
            value.getStateHolder().setChangeListener( changeListener );
        }
    }

    public setPropertyAndHandleChanges( _thisBaseCustoomObject, internalPropertyName, propertyName, value ) {
        const oldValue = _thisBaseCustoomObject[internalPropertyName];

        // if the value of this property is changed, mark it as such and notify if needed
        this.markIfChanged( propertyName, value, oldValue );

        // unregister as listener to old value if needed
        this.setChangeListenerToSubValueIfNeeded( oldValue, undefined );

        _thisBaseCustoomObject[internalPropertyName] = value;

        // register as listener to new value if needed
        this.setChangeListenerToSubValueIfNeeded( value, () => {
            this.markIfChanged( propertyName, value, value );
        } );

        // this value has changed by reference; so it needs to be fully sent to server - except for when it now arrived from the server and is being set (in which case ignoreChanges is true)
        if ( !this.ignoreChanges && instanceOfChangeAwareValue( value ) ) value.getStateHolder().markAllChanged( false );
    }


    protected markIfChanged( propertyName: string | number, newObject: any, oldObject: any ) {
        if ( this.testChanged( propertyName, newObject, oldObject ) ) {
            this.pushChange( propertyName );
            return true;
        }
        return false;
    }

    public getChangedKeys(): Array<string | number> {
        return this.changedKeys;
    }

    public clearChanges() {
        this.changedKeys = new Array<string | number>();
        this._allChanged = false;
    }

    private pushChange( propertyName ) {
        if ( this.ignoreChanges ) return;
        if ( this.changedKeys.length == 0 ) this.change++;

        if (this.changedKeys.indexOf(propertyName) < 0) {
            this.changedKeys.push( propertyName );
            this.notifyChangeListener();
        }
    }

    private testChanged( propertyName: string | number, newObject: any, oldObject: any ) {
        if ( newObject !== oldObject ) return true;
        if ( typeof newObject == "object" ) {
            if ( instanceOfChangeAwareValue( newObject ) ) {
                return newObject.getStateHolder().hasChanges();
            } else {
                return ConverterService.isChanged( newObject, oldObject, this.conversionInfo[propertyName] );
            }
        }
        return false;
    }

    public getHashKey(): string {
        return this.hash + "_" + this.change;
    }

}

export class ArrayState extends BaseCustomObjectState {
    private differ: IterableDiffer<Array<any>>;

    constructor( private array: Array<any>, private iterableDiffers: IterableDiffers ) {
        super();
    }

    public initDiffer() {
        this.differ = this.iterableDiffers.find( this.array ).create(( index: number, item: any ) => {
            if ( instanceOfBaseCustomObject( item ) ) {
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

    public getChangedKeys(): Array<string | number> {
        const changes = super.getChangedKeys();
        const arrayChanges = this.differ.diff( this.array );
        if ( arrayChanges ) {
            let addedOrRemoved = 0;
            arrayChanges.forEachAddedItem(( record ) => {
                addedOrRemoved++;
                if ( changes.indexOf( record.currentIndex ) == -1 ) {
                    changes.push( record.currentIndex );
                }
            } );
            arrayChanges.forEachRemovedItem(( record ) => {
                addedOrRemoved--;
                if ( changes.indexOf( record.previousIndex ) == -1 ) {
                    changes.push( record.previousIndex );
                }
            } );

            arrayChanges.forEachMovedItem(( record ) => {
                if ( instanceOfChangeAwareValue( record.item ) ) {
                    return record.item.getStateHolder().markAllChanged( false );
                }
            } )
            if ( addedOrRemoved != 0 ) {
                // size changed, for now send whole array
                this.markAllChanged( false );
            }
            else {
                changes.sort(( a: number, b: number ) => {
                    return a - b;
                } );
            }
        }
        return changes;
    }
    
}