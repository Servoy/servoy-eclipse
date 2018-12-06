import { Injectable, Renderer2 } from '@angular/core';

@Injectable()
export class ComponentContributor {    
    private static listeners: Set<IComponentContributorListener> = new Set();
    
    public componentCreated(nativeElement: any, renderer: Renderer2) {
        ComponentContributor.listeners.forEach(listener => listener.componentCreated(nativeElement, renderer));
    }
    
    public addComponentListener(listener:IComponentContributorListener) {
       ComponentContributor.listeners.add(listener);
    }
}

export interface IComponentContributorListener {
    
    componentCreated(nativeElement: any, renderer: Renderer2);
    
}