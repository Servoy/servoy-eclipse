import { Injectable, Renderer2 } from '@angular/core';

import { SvyUtilsService, ComponentContributor, IComponentContributorListener } from '../ngclient/servoy_public';
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
    
    public componentCreated(element:any, renderer:Renderer2) {
        let attribute = element.getAttribute('keylistener');
        if (attribute)
        {
            renderer.listen(element, 'keyup', (event) => {
                let callback = this.getCallback(attribute);
                if (callback) { 
                    let ev = this.utils.createJSEvent(event, "keyup");
                    this.servoyService.executeInlineScript(callback.formname, callback.script, 
                            [ev, element.value, event.keyCode, event.altKey]);
                }
              })
        }
    }
    
    public addKeyListener(callbackKey:string, callback:Function, clearCB?: boolean) {
        if (clearCB) this._callbacks = [];
        this._callbacks.push({'callbackKey':callbackKey,'callback': callback });
        this.changeHandler.changed("keyListener","callbacks", this._callbacks);
    }
    
    public getCallback(callbackKey: String): Function {
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