import { OnInit, AfterViewInit, OnChanges, SimpleChanges, Input, Renderer2, ElementRef, ViewChild, Directive, ChangeDetectorRef, OnDestroy, Injectable } from '@angular/core';
import { ServoyApi } from './servoy_api';

@Directive()
// eslint-disable-next-line
export class ServoyBaseComponent<T extends HTMLElement> implements AfterViewInit, OnInit, OnChanges, OnDestroy {
    @Input() name: string;
    @Input() servoyApi: ServoyApi;
    @Input() servoyAttributes: {  [property: string]: string };

    @ViewChild('element', { static: false, read: ElementRef}) elementRef: ElementRef<T>;

    private viewStateListeners: Set<IViewStateListener> = new Set();
    private componentContributor: ComponentContributor;
    private initialized: boolean;
    private changes: SimpleChanges;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        this.componentContributor = new ComponentContributor();
    }

    // final method, do not override
    ngOnInit() {
        this.initializeComponent();
        this.servoyApi.registerComponent(this);
    }

    // final method, do not override
    ngAfterViewInit() {
        this.initializeComponent();
        if (this.elementRef && this.changes) {
            this.svyOnChanges(this.changes);
            this.changes = null;
        }
        if (!this.elementRef) {
                // using logger would be better, but that would break all extends..
                console.log('component should have #element it its template for correct initalization for targetting the main div');
                console.log(this);
        }
        this.cdRef.detectChanges();
    }

    // final method, do not override
    ngOnChanges(changes: SimpleChanges) {
        this.initializeComponent();
        if (!this.elementRef) {
            if (this.changes == null) {
                this.changes = changes;
            } else {
                for (const property of Object.keys(changes)) {
                    this.changes[property] = changes[property];
                }
            }
        } else {
            if (this.changes == null) {
                this.svyOnChanges(changes);
            } else {
                for (const property of Object.keys(changes)) {
                    this.changes[property] = changes[property];
                }
                this.svyOnChanges(this.changes);
                this.changes = null;
            }
        }
    }

    ngOnDestroy() {
        this.servoyApi.unRegisterComponent(this);
         if(this.getNativeElement()) this.getNativeElement()['svyHostComponent'] = null;
    }

    // our init event that is called when dom is ready
    svyOnInit() {
        this.addAttributes();
        this.componentContributor.componentCreated(this);
        this.viewStateListeners.forEach(listener => listener.afterViewInit());
        if(this.getNativeElement()) this.getNativeElement()['svyHostComponent'] = this;
    }

    // our change event that is called when dom is ready
    svyOnChanges(_changes: SimpleChanges) {
    }

    public detectChanges() {
        this.cdRef.detectChanges();
    }
    /**
     * this should return the main native element (like the first div)
     */
    public getNativeElement(): T {
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
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetWidth;
    }

    public getHeight(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetHeight;
    }

    public getLocationX(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetLeft;
    }

    public getLocationY(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetTop;
    }

    public addViewStateListener(listener: IViewStateListener) {
        this.viewStateListeners.add(listener);
    }

    public removeViewStateListener(listener: IViewStateListener) {
        this.viewStateListeners.delete(listener);
    }

    protected initializeComponent() {
        if (!this.initialized && this.elementRef) {
            this.initialized = true;
            this.svyOnInit();
        }
    }

    protected addAttributes() {
        if (!this.servoyAttributes) return;
        for (const key of Object.keys(this.servoyAttributes)) {
             this.renderer.setAttribute(this.getNativeElement(), key, this.servoyAttributes[key]);
        }
    }
}

export interface IViewStateListener {
    afterViewInit(): void;
}

@Injectable()
export class ComponentContributor {
    private static listeners: Set<IComponentContributorListener> = new Set();

    public componentCreated(component: ServoyBaseComponent<any>) {
        ComponentContributor.listeners.forEach(listener => listener.componentCreated(component));
    }

    public addComponentListener(listener: IComponentContributorListener) {
       ComponentContributor.listeners.add(listener);
    }
}

export interface IComponentContributorListener {

    componentCreated(component: ServoyBaseComponent<any>): void;

}
