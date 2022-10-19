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
    public partComponentsCache: Array<ComponentCache | StructureCache>;
    public layoutContainersCache: Map<string, StructureCache>;

    private componentCache: Map<string, ComponentCache>;
    private formComponents: Map<string, FormComponentCache>; // components (extends ComponentCache) that have servoy-form-component properties in them

    private _mainStructure: StructureCache;
    private _parts: Array<PartCache>;
    private responsive: boolean;

    constructor(readonly formname: string, size: Dimension, responsive: boolean, public readonly url: string, private readonly typesRegistry: TypesRegistry) {
        this.size = size;
        this.responsive = responsive;
        this.componentCache = new Map();
        this.partComponentsCache = new Array();
        this._parts = [];
        this.formComponents = new Map();
        this.layoutContainersCache = new Map();
    }

    get absolute(): boolean {
        return !this.responsive;
    }
    get parts(): Array<PartCache> {
        return this._parts;
    }

    get mainStructure(): StructureCache {
        return this._mainStructure;
    }

    set mainStructure(structure: StructureCache) {
        this._mainStructure = structure;
        this.findComponents(structure);
    }


    public add(comp: ComponentCache | StructureCache, parent?: StructureCache | FormComponentCache | PartCache) {
        if (comp instanceof ComponentCache)
            this.componentCache.set(comp.name, comp);
        if (parent != null) {
            parent.addChild(comp);
        }
        if (parent instanceof PartCache){
             this.partComponentsCache.push(comp);
        }
    }

    public addLayoutContainer(container: StructureCache) {
        if (container.id){
           this.layoutContainersCache.set(container.id, container);
        }
    }

    public addPart(part: PartCache) {
        this._parts.push(part);
    }

    public addFormComponent(formComponent: FormComponentCache) {
        this.formComponents.set(formComponent.name, formComponent);
    }

    public getFormComponent(name: string): FormComponentCache {
        return this.formComponents.get(name);
    }

    public getComponent(name: string): ComponentCache {
        const cc = this.componentCache.get(name);
        return cc ? cc : this.getFormComponent(name);
    }

    public getLayoutContainer(id: string): StructureCache {
        return this.layoutContainersCache.get(id);
    }

    public removeComponent(name: string) {
        const comp = this.componentCache.get(name);
        this.componentCache.delete(name);
        if (comp){
            const index = this.partComponentsCache.indexOf(comp);
            if (index !== -1) this.partComponentsCache.splice(index,1);
        }
    }

    public removeLayoutContainer(id: string) {
        const layout = this.layoutContainersCache.get(id);
        this.layoutContainersCache.delete(id);
        if (layout) {
            const index = this.partComponentsCache.indexOf(layout);
            if (index !== -1) this.partComponentsCache.splice(index,1);
        }
    }

    public removeFormComponent(name: string) {
         this.formComponents.delete(name);
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
                if (item instanceof StructureCache && item.id) {
                    this.addLayoutContainer(item);
                }
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

    updateFormStyleClasses(ngutilsstyleclasses: string): void;
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

    public parent: StructureCache;

    constructor(public readonly name: string,
        public readonly type: string, // the directive name / component name (can be used to identify it's WebObjectSpecification)
        public readonly handlers: Array<string>,
        public layout: { [property: string]: string },
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

    sendChanges(propertyName: string, newValue: any, oldValue: any, rowId?: string, isDataprovider?: boolean) {
        // empty method with no impl for the designer (for example in LFC when the components are plain ComponentCache objects not the Subcass.) 
    }

    toString() {
        return 'ComponentCache(' + this.name + ', '+ this.type + ')';
    }

}

export class StructureCache {
    public parent: StructureCache;
    public model:  { [property: string]: any } = {};
    constructor(public readonly tagname: string, public classes: Array<string>, public attributes?: { [property: string]: string },
        public readonly items?: Array<StructureCache | ComponentCache | FormComponentCache>,
        public readonly id?: string, public readonly cssPositionContainer?: boolean, public layout?: { [property: string]: string }) {
        if (!this.items) this.items = [];
    }

    addChild(child: StructureCache | ComponentCache | FormComponentCache, insertBefore?: StructureCache | ComponentCache): StructureCache {
        if (insertBefore) {
            const idx =  this.items.indexOf(insertBefore);
           this.items.splice( idx, 0, child);
        } else {
            this.items.push(child);
        }
        if (child instanceof StructureCache) {
            child.parent = this;
            return child as StructureCache;
        }
        if (child instanceof ComponentCache) {
            child.parent = this;
        }
        return null;
    }

    removeChild(child: StructureCache | ComponentCache | FormComponentCache): boolean {
        const index = this.items.indexOf(child);
        if (index >= 0) {
            this.items.splice(index, 1);
            return true;
        }
        if (child instanceof StructureCache) {
            child.parent = undefined;
        }
    }

    getDepth(): number {
        let level = -1;
        let parent = this.parent;
        while (parent !== undefined) {
            level += 1;
            parent = parent.parent;
        }
        return level;
    }

    toString() {
        return 'StructureCache(' + this.id + ')';
    }
}

/** This is a cache that represents a form part (body/header/etc.). */
export class PartCache {
    constructor(public readonly classes: Array<string>,
        public readonly layout: { [property: string]: string },
        public readonly items?: Array<ComponentCache | FormComponentCache | StructureCache>) {
        if (!this.items) this.items = [];
    }

    addChild(child: ComponentCache | FormComponentCache | StructureCache) {
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

    constructor(
        name: string,
        handlers: Array<string>,
        public responsive: boolean,
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

    removeChild(child: StructureCache | ComponentCache | FormComponentCache): boolean {
        const index = this.items.indexOf(child);
        if (index >= 0) {
            this.items.splice(index, 1);
            return true;
        }
        if (child instanceof StructureCache) {
            child.parent = undefined;
        }
    }

}

export class FormComponentProperties {
    constructor(public readonly classes: Array<string>,
        public readonly layout: { [property: string]: string },
        public readonly attributes: { [property: string]: string }) {
    }
}

export class Dimension {
    public width: number;
    public height: number;
}
