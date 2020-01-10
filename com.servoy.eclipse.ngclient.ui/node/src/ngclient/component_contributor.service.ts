import { Injectable, Renderer2 } from '@angular/core';
import { ServoyBaseComponent } from './servoy_public';

@Injectable()
export class ComponentContributor {    
    private static listeners: Set<IComponentContributorListener> = new Set();
    
    public componentCreated(component: ServoyBaseComponent) {
        ComponentContributor.listeners.forEach(listener => listener.componentCreated(component));
    }
    
    public addComponentListener(listener:IComponentContributorListener) {
       ComponentContributor.listeners.add(listener);
    }
}

export interface IComponentContributorListener {
    
    componentCreated(component: ServoyBaseComponent);
    
}