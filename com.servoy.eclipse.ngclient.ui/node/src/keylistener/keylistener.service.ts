import { Injectable, Renderer2 } from '@angular/core';

import { SvyUtilsService, ComponentContributor, IComponentContributorListener } from '../ngclient/servoy_public';
import { ServoyService } from '../ngclient/servoy.service'

@Injectable()
export class KeyListener implements IComponentContributorListener {   
    private static callbacks: any[] = []; //TODO getServiceScope??
    
    constructor(private componentContributor: ComponentContributor, private servoyService: ServoyService, private utils: SvyUtilsService){
        componentContributor.addComponentListener(this);
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
    
    public addKeyListener(callbackKey:string, callback:Object, clearCB?: boolean) {
        if (!KeyListener.callbacks || clearCB) KeyListener.callbacks = [];
        KeyListener.callbacks.push({ 'callbackKey': callbackKey, 'callback': callback });
    }
    
    public getCallback(callbackKey: String) {
        let cb = KeyListener.callbacks.find( c => c.callbackKey === callbackKey);
        return cb ? cb.callback : undefined;
    }
}