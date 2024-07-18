import { ServoyBaseComponent } from './basecomponent';

/**
  * This is the servoy api class for components that is injected into the component by default.
  * The  {@link ServoyBaseComponent} has this as an Input property, so every component extending the  {@link ServoyBaseComponent} will have "this.servoyApi"
  * to interact with the servoy system and notify the server.  
  */ 
export abstract class ServoyApi {
    /**
     * This should be called by components when they want to show a form, optionally a relationname or formindex can be given.
     * It will return a Promise that gives a boolean if the form could be shown (then the component can actually show this form)
     */
    public abstract formWillShow(formname: string, relationname?: string, formIndex?: number): Promise<boolean>;
    /**
     * This should be called by components when they it wants to hide a form, optionally a relationname or formindex can be given and
     * also the form/relation/index that should be shown when the given form can be hidden so there is only 1 call to hide one form and show another.
     * 
     * It will return a Promise that gives a boolean if the form could be hidden (onHide of a form doesn't block the hide)
     */
    public abstract hideForm(formname: string, relationname?: string, formIndex?: number,
        formNameThatWillShow?: string, relationnameThatWillBeShown?: string, formIndexThatWillBeShown?: number): Promise<boolean>;

    /**
      * Call to notify the server when this property is gone in edit mode (focus in a field). This can also be configured by using the {@link StartEditDirective}
      */
    public abstract startEdit(propertyName: string);

    /**
      * This apply is only needed for nested dataproviders, so a dataprovider property of custom type, this will push and apply the data to the data model.
      * Normal main spec model dataprovider should be pushed using an Change Emitter. 
      */
    public abstract apply(propertyName: string, value: unknown);

    /**
      * Call this components serverside api with the given arguments
      */ 
    public abstract callServerSideApi(methodName: string, args: Array<unknown>);

    /**
      * Returns true if the component is currently rendered in the designer (so it can render with some sample data if needed)
      */
    public abstract isInDesigner(): boolean;

    /**
      * Returns true if this component can trust the html it wants to display (no need to sanatize it). 
      * This can be a client property set on this component itself or more application wide.  
      */
    public abstract trustAsHtml(): boolean;

    /**
      * Returns true if this component is placed on a css positioned form (not responsive)
      */
    public abstract isInAbsoluteLayout(): boolean;

    /**
      * Returns the markupid that this component can use for itself, can be used like this in the template:  [id]="servoyApi.getMarkupId()"
      */
    public abstract getMarkupId(): string;

    /**
      * Returns the formname where this component is part of.
      */
    public abstract getFormName(): string;

    /**
      * Internal api, used by {@link ServoyBaseComponent} to register itself to implementors of this ServoyApi object (like form containers)
      */
    public abstract registerComponent(component: ServoyBaseComponent<HTMLElement>);

    /**
     * Internal api, used by {@link ServoyBaseComponent} to unregister itself to implementors of this ServoyApi object (like form containers)
      */
    public abstract unRegisterComponent(component: ServoyBaseComponent<HTMLElement>);

    /**
      * Returns the value for the given client property key that was set at the server side on this component.
      */
    public abstract getClientProperty(key: string): any;
}
