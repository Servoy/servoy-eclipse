import { OnInit, AfterViewInit, Input, Renderer2, ElementRef, ViewChild, Directive } from '@angular/core';
import {ComponentContributor} from '../ngclient/component_contributor.service';

@Directive()
export class ServoyBaseComponent implements AfterViewInit, OnInit {
    @Input() name;
    @Input() servoyApi;
    @Input() servoyAttributes;
    
    @ViewChild('element', {static: true}) elementRef:ElementRef;
    
    readonly self: ServoyBaseComponent;
    private viewStateListeners: Set<IViewStateListener> = new Set();
    private componentContributor:ComponentContributor;

    constructor(protected readonly renderer: Renderer2) {
        this.self = this;
        this.componentContributor = new ComponentContributor();
    }
    
    ngOnInit(){
        this.addAttributes(); 
    }
    
    ngAfterViewInit() {
        this.componentContributor.componentCreated(this);
        this.viewStateListeners.forEach(listener => listener.afterViewInit());
     }
    
    protected addAttributes() {
        if (!this.servoyAttributes) return;
        this.servoyAttributes.forEach( attribute => this.renderer.setAttribute(this.getNativeElement(), attribute.key,  attribute.value));
    }

    /**
     * this should return the main native element (like the first div) 
     */
    public getNativeElement():any {
        return this.elementRef.nativeElement;
    }

   /**
    * sub classes can return a different native child then the default main element.
    * used currently only for horizontal aligment
    */
    public getNativeChild():any {
        return this.elementRef.nativeElement;
    }
    
    public getRenderer(): Renderer2 {
        return this.renderer;
    }

    public getWidth() : number{
        return this.getNativeElement().parentNode.parentNode.offsetWidth;
    }
    
    public getHeight() : number{
        return this.getNativeElement().parentNode.parentNode.offsetHeight;
    }
    
    public getLocationX() : number{
        return this.getNativeElement().parentNode.parentNode.offsetLeft;
    }
    
    public getLocationY() : number{
        return this.getNativeElement().parentNode.parentNode.offsetTop;
    }

    public addViewStateListener(listener:IViewStateListener) {
        this.viewStateListeners.add(listener);
    }

    public removeViewStateListener(listener:IViewStateListener) {
        this.viewStateListeners.delete(listener);
    }    
}

export interface IViewStateListener {
    afterViewInit();
}