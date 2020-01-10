import { Injectable, Renderer2 } from '@angular/core';

import { SvyUtilsService, ComponentContributor, IComponentContributorListener, ServoyBaseComponent } from '../ngclient/servoy_public';
import {ServiceChangeHandler} from '../sablo/util/servicechangehandler'
import { ServoyService } from '../ngclient/servoy.service'

@Injectable()
export class KeyListener implements IComponentContributorListener {   
    private _callbacks: Callback[] = [];
    
    constructor(private componentContributor: ComponentContributor, private servoyService: ServoyService, private utils: SvyUtilsService, private changeHandler : ServiceChangeHandler){
        componentContributor.addComponentListener(this);
    }
    
    get callbacks(): Callback[] {
        return this._callbacks;
    }
    
    set callbacks(callbacks:Callback[]) {
        this._callbacks = callbacks;
    } 
    
    public componentCreated(component: ServoyBaseComponent) {
        let element = component.getNativeChild();
        let renderer = component.getRenderer();
        let attribute = element.getAttribute('keylistener');
        if (attribute)
        {
            renderer.listen(element, 'keyup', (event) => {
                let callback = this.getCallback(attribute);
                if (callback) { 
                    let ev = this.utils.createJSEvent(event, "keyup");
                    let capsLockEnabled = false;
                    if(event instanceof KeyboardEvent) {
                        capsLockEnabled = event.getModifierState("CapsLock")
                    } 
                    else if (event.originalEvent instanceof KeyboardEvent) {
                        capsLockEnabled = event.originalEvent.getModifierState("CapsLock");
                    }
                    this.servoyService.executeInlineScript(callback.formname, callback.script, 
                            [element.value, ev, event.keyCode, event.altKey, event.ctrlKey, event.shiftKey, capsLockEnabled]);
                }
              })
        }
    }
    
    public addKeyListener(callbackKey:string, callback:Function, clearCB?: boolean) {
        if (clearCB) this._callbacks = [];
        this._callbacks.push({'callbackKey':callbackKey,'callback': callback });
        this.changeHandler.changed("keyListener","callbacks", this._callbacks);
    }
    
    public removeKeyListener(callbackKey:string) : boolean {
        let len = this._callbacks.length;
        this._callbacks = this._callbacks.filter(c => c.callbackKey != callbackKey);
        if (len > this._callbacks.length)
        {
            this.changeHandler.changed("keyListener","callbacks", this._callbacks);
            return true;
        }
        return false;
    }
    
    private getCallback(callbackKey: String): Function {
        let cb = this._callbacks.find( c => c.callbackKey === callbackKey);
        return cb ? cb.callback : undefined;
    }
}

class Callback {
    public callbackKey:string;
    public callback: Function;
}

class Function {
    public formname: string;
    public script: string
}
