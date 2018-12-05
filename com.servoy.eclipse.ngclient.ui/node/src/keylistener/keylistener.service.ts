import { Injectable, Renderer2, ElementRef } from '@angular/core';

import { ComponentContributor, IComponentContributorListener } from '../ngclient/component_contributor.service';
import { NGUtilsService } from '../servoy_ng_only_services/ngutils/ngutils.service';
import { ServoyService } from '../ngclient/servoy.service'

@Injectable()
export class KeyListener implements IComponentContributorListener {   
    private static callbacks: any[] = []; //TODO getServiceScope??
    
    constructor(private componentContributor: ComponentContributor, private servoyService: ServoyService, private utils: NGUtilsService){
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