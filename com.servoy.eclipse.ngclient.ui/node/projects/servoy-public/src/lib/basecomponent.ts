import { OnInit, AfterViewInit, OnChanges, SimpleChanges, Input, Renderer2, ElementRef, ViewChild, Directive, ChangeDetectorRef, OnDestroy, Injectable } from '@angular/core';
import { ServoyApi } from './servoy_api';

/**
 * This is the BaseComponent all Titanium NGClient components should be extending
 *  It takes care of the initialization of the component by calling {@link #svyOnInit} when the component is fully constructed
 * This will only be fully done when the main div of the component has the #element reference in the tag. So that the ViewChild of angular can be resolved.
 * So the main div of a component should be something like: <div #element>. This will the be the element that will be returned by  {@link #getNativeElement}
 *
 * This base component provides the servoyApi {@link ServoyApi}, the name and also takes care of the custom attributes set in the designer.
 *
 * For performance reasons all compoents should be configured to use:  changeDetection: ChangeDetectionStrategy.OnPush in there Component configuration.
 * this way component detection is only done when a push happens on the Input fields.  If you need to trigger a change detection besides that (because of timeouts or other events)
 * call this.detectChanges()
 */
@Directive()
// eslint-disable-next-line
export class ServoyBaseComponent<T extends HTMLElement> implements AfterViewInit, OnInit, OnChanges, OnDestroy {
    @Input() name: string;
    @Input() servoyApi: ServoyApi;
    @Input() servoyAttributes: { [property: string]: string };

    @ViewChild('element', { static: false, read: ElementRef }) elementRef: ElementRef<T>;

    private viewStateListeners: Set<IViewStateListener> = new Set();
    private componentContributor: ComponentContributor;
    private initialized: boolean;
    private changes: SimpleChanges;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        this.componentContributor = new ComponentContributor();
    }

    /**
     *  final method, do not override use {@link #svyOnInit} for this
     */
    ngOnInit() {
        this.initializeComponent();
        this.servoyApi.registerComponent(this);
    }

    /**
     *  final method, do not override use {@link #svyOnInit} for this
     */
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

    /**
     *  final method, do not override use {@link #svyOnChanges} for this
     */
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

    /**
     * Angular onDestroy hook, called when the component is removed from the dom, use to clean up stuff
     * make sure you call super.ngOnDestroy()
     */
    ngOnDestroy() {
        this.servoyApi.unRegisterComponent(this);
        if (this.getNativeElement()) this.getNativeElement()['svyHostComponent'] = null;
    }

    /**
     * This method should be overwritten to get a callback when the component is initialized
     * The template of the component must have an #element in its main tag, else this init will not resolve.
     * make sure you call super.svyOnInit() to get the default behavior
     */
    svyOnInit() {
        this.addAttributes();
        this.componentContributor.componentCreated(this);
        this.viewStateListeners.forEach(listener => listener.afterViewInit());
        if (this.getNativeElement()) this.getNativeElement()['svyHostComponent'] = this;
    }

    /**
     * The Servoy replacement of the angular ngOnChanges, this will only be called when the component is initalized.
     *  So that you are sure that you can reference the native element through the renderer to configure it.
     */
    svyOnChanges(_changes: SimpleChanges) {
        if (_changes['servoyAttributes'] && !_changes['servoyAttributes'].firstChange) {
            if (_changes['servoyAttributes'].previousValue) {
                for (const key of Object.keys(_changes['servoyAttributes'].previousValue)) {
                    this.renderer.removeAttribute(this.getNativeElement(), key);
                }
            }
            if (_changes['servoyAttributes'].currentValue) {
                for (const key of Object.keys(_changes['servoyAttributes'].currentValue)) {
                    this.renderer.setAttribute(this.getNativeElement(), key, _changes['servoyAttributes'].currentValue[key]);
                }
            }
        }
    }

    /**
     * Call this method to trigger a change detection if something of this component is changed which didn't came directly from an @Input field change.
     * other events like timeouts or promise resolvements could result in that the component needs to detect stuff.
     */
    public detectChanges() {
        this.cdRef.detectChanges();
    }
    /**
     * this should return the main native element (like the first div) which is marked as #element in the main div.
     */
    public getNativeElement(): T {
        return this.elementRef ? this.elementRef.nativeElement : null;
    }

    /**
     * sub classes can return a different native child then the default main element.
     */
    public getNativeChild(): HTMLElement {
        return this.elementRef.nativeElement;
    }

    /**
     * returns the Renderer2 object which must be used to access/change the dom elements for this component
     */
    public getRenderer(): Renderer2 {
        return this.renderer;
    }

    /**
     * returns the Renderer2 object which must be used to access/change the dom elements for this component
     */
    public getWidth(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetWidth;
    }

    /**
     * returns the Renderer2 object which must be used to access/change the dom elements for this component
     */
    public getHeight(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetHeight;
    }

    /**
     * returns the Renderer2 object which must be used to access/change the dom elements for this component
     */
    public getLocationX(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetLeft;
    }

    /**
     * returns the Renderer2 object which must be used to access/change the dom elements for this component
     */
    public getLocationY(): number {
        return (this.getNativeElement().parentNode.parentNode as HTMLElement).offsetTop;
    }

    /**
     * Direcives can add a {@link IViewStateListener} to this component so they are notified when the component is fully initialized
     * so this is for directives the same event and the {@link #svyOnInit} for subcomponents.
     * Directives needs to have a property to gain access to the component by using a attribute on the tag (besides the attribute directive itself) like [hostcomponent]="this"
     */
    public addViewStateListener(listener: IViewStateListener) {
        this.viewStateListeners.add(listener);
    }

    /**
     * Removes the viewstatelistener
     */
    public removeViewStateListener(listener: IViewStateListener) {
        this.viewStateListeners.delete(listener);
    }

    /**
     * @internal
     */
    protected initializeComponent() {
        if (!this.initialized && this.elementRef) {
            this.initialized = true;
            this.svyOnInit();
        }
    }

    /**
     * @internal
     */
    protected addAttributes() {
        if (!this.servoyAttributes) return;
        for (const key of Object.keys(this.servoyAttributes)) {
            this.renderer.setAttribute(this.getNativeElement(), key, this.servoyAttributes[key]);
        }
    }
}
/**
 * Interface for diretives to register itself to the the ServoyBaseComponent, to get the "svyOnInit" event.
 */
export interface IViewStateListener {
    afterViewInit(): void;
}

/**
 * Services can use this to inject itself this contributor and then registering a listener {@link IComponentContributorListener}
 * then a service will get an event when a component is created over all forms.
 */
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

/**
 * The interface used for {@link ComponentContributor} to listen for {@link ServoyBaseComponent} creation
 */
export interface IComponentContributorListener {

    componentCreated(component: ServoyBaseComponent<any>): void;

}
