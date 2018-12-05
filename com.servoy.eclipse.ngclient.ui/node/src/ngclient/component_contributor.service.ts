import { Injectable, ElementRef, Renderer2 } from '@angular/core';

@Injectable()
export class ComponentContributor {    
    private static listeners: Set<IComponentContributorListener> = new Set();
    
    public componentCreated(elementRef: ElementRef, renderer: Renderer2) {
        ComponentContributor.listeners.forEach(listener => listener.componentCreated(elementRef, renderer));
    }
    
    public addComponentListener(listener:IComponentContributorListener) {
       ComponentContributor.listeners.add(listener);
    }
}

export interface IComponentContributorListener {
    
    componentCreated(elementRef: ElementRef, renderer: Renderer2);
    
}