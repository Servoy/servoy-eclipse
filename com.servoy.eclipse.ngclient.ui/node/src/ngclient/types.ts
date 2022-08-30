import { IComponentCache, IFormCache } from '@servoy/public';
import { IType, TypesRegistry, PushToServerEnum } from '../sablo/types_registry';
import { SubpropertyChangeByReferenceHandler, IParentAccessForSubpropertyChanges } from '../sablo/converter.service';

export class FormSettings {
  public name: string;
  public size: { width: number; height: number };
}

/** Cache for a Servoy form. Also keeps the component caches, Servoy form component caches etc. */
export class FormCache implements IFormCache {
    public navigatorForm: FormSettings;
    public size: Dimension;
    private _mainStructure: StructureCache;

    private componentCache: Map<string, ComponentCache>;
    private _formComponents: Map<string, FormComponentCache>; // components (extends ComponentCache) that have servoy-form-component properties in them

    private _parts: Array<PartCache>;

    constructor(readonly formname: string, size: Dimension, public readonly url: string, private readonly typesRegistry: TypesRegistry) {
        this.size = size;
        this.componentCache = new Map();
        this._parts = [];
        this._formComponents = new Map();
    }
    public add(comp: ComponentCache) {
        this.componentCache.set(comp.name, comp);
    }

    public addPart(part: PartCache) {
        this._parts.push(part);
    }

    set mainStructure(structure: StructureCache) {
        this._mainStructure = structure;
        this.findComponents(structure);
    }

    get mainStructure(): StructureCache {
        return this._mainStructure;
    }

    get absolute(): boolean {
        return this._mainStructure == null;
    }
    get parts(): Array<PartCache> {
        return this._parts;
    }

    public addFormComponent(formComponent: FormComponentCache) {
        this._formComponents.set(formComponent.name, formComponent);
    }

    public getFormComponent(name: string): FormComponentCache {
        return this._formComponents.get(name);
    }

    public getComponent(name: string): ComponentCache {
        const cc = this.componentCache.get(name);
        return cc ? cc : this.getFormComponent(name);
    }

    public getComponentSpecification(componentName: string) {
        let componentCache: ComponentCache = this.componentCache.get(componentName);
        if (!componentCache) componentCache = this._formComponents.get(componentName);
        return componentCache ? this.typesRegistry.getComponentSpecification(componentCache.type) : undefined;
    }

    public getClientSideType(componentName: string, propertyName: string) {
        const componentSpec = this.getComponentSpecification(componentName);

        let type = componentSpec.getPropertyType(propertyName);
        if (!type) type = this.componentCache.get(componentName)?.dynamicClientSideTypes[propertyName];
        if (!type) type = this._formComponents.get(componentName)?.dynamicClientSideTypes[propertyName];

        return type;
    }

    private findComponents(structure: StructureCache | FormComponentCache) {
        structure.items.forEach(item => {
            if (item instanceof StructureCache || item instanceof FormComponentCache) {
                this.findComponents(item);
            } else {
                this.add(item);
            }
        });
    }
}

/**
 * This interface is not for the servoy form component concept, but it's rather the client side angular component of an actual servoy form.
 */
export interface IFormComponent extends IApiExecutor {
    name: string;
    // called when there are changed pushed to this form, so this form can trigger a detection change
    detectChanges(): void;
    // called when there are changed pushed to this form, so this form can trigger a detection change
    formCacheChanged(cache: FormCache): void;
    // called when a model property is updated for the given compponent, but the value itself didn't change (only nested)
    triggerNgOnChangeWithSameRefDueToSmartPropUpdate(componentName: string, propertiesChangedButNotByRef: {propertyName: string; newPropertyValue: any}[]): void;
}

export interface IApiExecutor {
    callApi(componentName: string, apiName: string, args: Array<any>, path?: string[]): any;
}

export const instanceOfApiExecutor = (obj: any): obj is IApiExecutor =>
    obj != null && (obj).callApi instanceof Function;

export const instanceOfFormComponent = (obj: any): obj is IFormComponent =>
    obj != null && (obj).detectChanges instanceof Function;

/** More (but internal not servoy public) impl. for IComponentCache implementors. */
export class ComponentCache implements IComponentCache {

    /**
     * The dynamic client side types of a component's properties (never null, can be an empty obj). These are client side types sent from server that are
     * only known at runtime (so not directly from .spec). For example dataproviders could decide that they send 'date' types.
     *
     * Call FormCache.getClientSideType(componentName, propertyName) instead if you want a combination of static client-side-type from it's spec and dynamic client
     * side type for a component's property when converting data to be sent to server.
     */
    public readonly dynamicClientSideTypes: Record<string, IType<any>> = {};
    public readonly model: { [property: string]: any };

    private readonly subPropertyChangeByReferenceHandler: SubpropertyChangeByReferenceHandler;


    constructor(public readonly name: string,
        public readonly type: string, // the directive name / component name (can be used to identify it's WebObjectSpecification)
        public readonly handlers: Array<string>,
        public readonly layout: { [property: string]: string },
        private readonly typesRegistry: TypesRegistry,
        parentAccessForSubpropertyChanges: IParentAccessForSubpropertyChanges<number | string>) {
            this.model = this.createModel();
            this.subPropertyChangeByReferenceHandler = new SubpropertyChangeByReferenceHandler(parentAccessForSubpropertyChanges);
    }

    private createModel(): { [property: string]: any } {
        // if object & elements have SHALLOW or DEEP (which in ng2 does the same as SHALLOW) pushToServer, add a Proxy obj to intercept client side changes to array and send them to server
        let modelOfComponent: { [property: string]: any };

        if (this.hasSubPropsWithShallowOrDeep()) {
            // hmm the proxy itself might not be needed for actual push to server when the values change by reference because
            // the component normally emits those via it's @Output and FormComponent.datachange(...) will send them to server
            // but we use it to also handle the scenario where a change-aware value (object / array) is changed by reference and we need to set it's setChangeListener(...)
            modelOfComponent = new Proxy({}, this.getProxyHandler());
        } else modelOfComponent = {};

        return modelOfComponent;
    }

    private hasSubPropsWithShallowOrDeep(): boolean {
        const componentSpec = this.typesRegistry.getComponentSpecification(this.type);
        if (componentSpec) for (const propertyDescription of Object.values(componentSpec.getPropertyDescriptions())) {
            if (propertyDescription.getPropertyPushToServer() > PushToServerEnum.ALLOW) return true;
        }
        return false;
    }

    /**
     * Handler for the Proxy object that will detect reference changes in the component model where it is needed
     * This implements the shallow PushToServer behavior.
     */
    private getProxyHandler() {
        return {
            set: (underlyingModelObject: { [property: string]: any }, prop: any, v: any) => {
                if (this.subPropertyChangeByReferenceHandler.parentAccess.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.set(underlyingModelObject, prop, v);

                const propertyDescription = this.typesRegistry.getComponentSpecification(this.type)?.getPropertyDescription(prop);
                const pushToServer = propertyDescription ? propertyDescription.getPropertyPushToServer() : PushToServerEnum.REJECT;

                if (pushToServer > PushToServerEnum.ALLOW) {
                    // we give to setPropertyAndHandleChanges(...) here also doNotPushNow arg === true, so that it does not auto-push;
                    // push normally executes afterwards due to the @Output emitter of that prop. from the component which calls FormComponent.datachange()
                    this.subPropertyChangeByReferenceHandler.setPropertyAndHandleChanges(underlyingModelObject, prop, v, true); // 1 element has changed by ref
                    return true;
                } else return Reflect.set(underlyingModelObject, prop, v);
            },

            deleteProperty: (underlyingModelObject: { [property: string]: any }, prop: any) => {
                if (this.subPropertyChangeByReferenceHandler.parentAccess.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.deleteProperty(underlyingModelObject, prop);

                const propertyDescription = this.typesRegistry.getComponentSpecification(this.type)?.getPropertyDescription(prop);
                const pushToServer = propertyDescription ? propertyDescription.getPropertyPushToServer() : PushToServerEnum.REJECT;

                if (pushToServer > PushToServerEnum.ALLOW) {
                    // we give to setPropertyAndHandleChanges(...) here also doNotPushNow arg === true, so that it does not auto-push;
                    // push normally executes afterwards due to the @Output emitter of that prop. from the component which calls FormComponent.datachange()
                    this.subPropertyChangeByReferenceHandler.setPropertyAndHandleChanges(underlyingModelObject, prop, undefined, true); // 1 element deleted
                    return true;
                } else return Reflect.deleteProperty(underlyingModelObject, prop);
            }
        };
    }

}

/** This is for generated html structures in responsive forms - like for example bootstrap 12grid divs, flex containter and so on. */
export class StructureCache {
    constructor(public readonly tagname: string, public readonly classes: Array<string>,  public readonly attributes?: { [property: string]: string },
        public readonly items?: Array<StructureCache | ComponentCache | FormComponentCache>) {
        if (!this.items) this.items = [];
    }

    addChild(child: StructureCache | ComponentCache | FormComponentCache): StructureCache {
        this.items.push(child);
        if (child instanceof StructureCache)
            return child as StructureCache;
        return null;
    }
}

/** This is a cache that represents a form part (body/header/etc.). */
export class PartCache {
    constructor(public readonly classes: Array<string>,
        public readonly layout: { [property: string]: string },
        public readonly items?: Array<ComponentCache | FormComponentCache  >) {
        if (!this.items) this.items = [];
    }

    addChild(child: ComponentCache | FormComponentCache) {
        if (child instanceof ComponentCache && child.type && child.type === 'servoycoreNavigator')
            return;
        this.items.push(child);
    }
}

/**
 * Cache for an component that has Servoy form component properties (children).
 * So it is a normal component that has servoy-form-component properties in it's .spec.
 */
export class FormComponentCache extends ComponentCache {
    public items: Array<StructureCache | ComponentCache | FormComponentCache> = [];

    constructor(name: string,
        type: string,
        handlers: Array<string>,
        public readonly responsive: boolean,
        layout: { [property: string]: string },
        public readonly formComponentProperties: FormComponentProperties,
        public readonly hasFoundset: boolean,
        typesRegistry: TypesRegistry,
        parentAccessForSubpropertyChanges: IParentAccessForSubpropertyChanges<number | string>) {
            super(name, type, handlers, layout, typesRegistry, parentAccessForSubpropertyChanges);
    }

    addChild(child: StructureCache | ComponentCache | FormComponentCache) {
        if (!(child instanceof ComponentCache && (child as ComponentCache).type === 'servoycoreNavigator'))
            this.items.push(child);
    }
}

export class FormComponentProperties {
    constructor(public readonly classes: Array<string>,
        public readonly layout: { [property: string]: string },
        public readonly attributes: { [property: string]: string }) {
    }
}

export class Dimension{
    public width: number;
    public height: number;
}
