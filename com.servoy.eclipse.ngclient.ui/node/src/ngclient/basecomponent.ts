import { OnInit, AfterViewInit, OnChanges, SimpleChanges, Input, Renderer2, ElementRef, ViewChild, Directive, ChangeDetectorRef } from '@angular/core';
import {ComponentContributor} from '../ngclient/component_contributor.service';
import { ServoyApi } from './servoy_api';

@Directive()
export class ServoyBaseComponent implements AfterViewInit, OnInit, OnChanges {
    @Input() name;
    @Input() servoyApi: ServoyApi;
    @Input() servoyAttributes;

    @ViewChild('element', {static: false}) elementRef: ElementRef;

    readonly self: ServoyBaseComponent;
    private viewStateListeners: Set<IViewStateListener> = new Set();
    private componentContributor: ComponentContributor;
    private initialized: boolean;
    private changes: SimpleChanges;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        this.self = this;
        this.componentContributor = new ComponentContributor();
    }

    // final method, do not override
    ngOnInit() {
        this.initializeComponent();
    }

    // final method, do not override
    ngAfterViewInit() {
        this.initializeComponent();
        if (this.elementRef && this.changes) {
            this.svyOnChanges( this.changes );
            this.changes = null;
        }
        this.cdRef.detectChanges();
    }

    // final method, do not override
    ngOnChanges( changes: SimpleChanges ) {
        this.initializeComponent();
        if ( !this.elementRef ) {
            if ( this.changes == null ) {
                this.changes = changes;
            } else {
                for ( const property of Object.keys(changes) ) {
                    this.changes[property] = changes[property];
                }
            }
        } else {
            if ( this.changes == null ) {
                this.svyOnChanges( changes );
            } else {
                for ( const property of Object.keys(changes) ) {
                    this.changes[property] = changes[property];
                }
                this.svyOnChanges( this.changes );
                this.changes = null;
            }
        }
    }

    // our init event that is called when dom is ready
    svyOnInit() {
        this.addAttributes();
        this.componentContributor.componentCreated(this);
        this.viewStateListeners.forEach(listener => listener.afterViewInit());
    }

    // our change event that is called when dom is ready
    svyOnChanges(changes: SimpleChanges) {

    }

    protected initializeComponent() {
        if (!this.initialized && this.elementRef) {
            this.initialized = true;
            this.svyOnInit();
        }
    }

    protected addAttributes() {
        if (!this.servoyAttributes) return;
        this.servoyAttributes.forEach( attribute => this.renderer.setAttribute(this.getNativeElement(), attribute.key,  attribute.value));
    }

    /**
     * this should return the main native element (like the first div)
     */
    public getNativeElement(): any {
        return this.elementRef ? this.elementRef.nativeElement : null;
    }

   /**
    * sub classes can return a different native child then the default main element.
    * used currently only for horizontal aligment
    */
    public getNativeChild(): any {
        return this.elementRef.nativeElement;
    }

    public getRenderer(): Renderer2 {
        return this.renderer;
    }

    public getWidth(): number {
        return this.getNativeElement().parentNode.parentNode.offsetWidth;
    }

    public getHeight(): number {
        return this.getNativeElement().parentNode.parentNode.offsetHeight;
    }

    public getLocationX(): number {
        return this.getNativeElement().parentNode.parentNode.offsetLeft;
    }

    public getLocationY(): number {
        return this.getNativeElement().parentNode.parentNode.offsetTop;
    }

    public addViewStateListener(listener: IViewStateListener) {
        this.viewStateListeners.add(listener);
    }

    public removeViewStateListener(listener: IViewStateListener) {
        this.viewStateListeners.delete(listener);
    }
}

export interface IViewStateListener {
    afterViewInit();
}
