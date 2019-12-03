import {OnInit, AfterViewInit , Input, Renderer2, ElementRef,ViewChild} from '@angular/core';
import {ComponentContributor} from '../ngclient/component_contributor.service';

export class ServoyBaseComponent implements AfterViewInit, OnInit {
    @Input() name;
    @Input() servoyApi;
    @Input() servoyAttributes;
    
    @ViewChild('element', {static: true}) elementRef:ElementRef;
    
    private componentContributor:ComponentContributor;

    constructor(protected readonly renderer: Renderer2) { 
        this.componentContributor = new ComponentContributor();
    }
    
    ngOnInit(){
        this.addAttributes(); 
    }
    
    ngAfterViewInit() {
        this.componentContributor.componentCreated(this.getNativeChild(), this.renderer);
     }
    
    protected addAttributes() {
        if (!this.servoyAttributes) return;
        this.servoyAttributes.forEach( attribute => this.renderer.setAttribute(this.getNativeElement(), attribute.key,  attribute.value));
    }

    /**
     * this should return the main native element (like the first div) 
     */
    protected getNativeElement():any {
        return this.elementRef.nativeElement;
    }

   /**
    * sub classes can return a different native child then the default main element.
    * used currently only for horizontal aligment
    */
    protected getNativeChild():any {
        return this.elementRef.nativeElement;
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
}