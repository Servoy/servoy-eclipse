import { IComponentCache, IFormCache } from '@servoy/public';
import { IType, TypesRegistry } from '../sablo/types_registry';

export class FormSettings {
    public name: string;
    public size: { width: number; height: number };
}

/** Cache for a Servoy form (data/model instances, no UI). Also keeps the component caches, Servoy form component caches etc. */
export class FormCache implements IFormCache {
    public navigatorForm: FormSettings;
    public partComponentsCache: Array<ComponentCache | StructureCache | FormComponentCache>;
    public layoutContainersCache: Map<string, StructureCache>;
    public formComponents: Map<string, FormComponentCache>; // components (extends ComponentCache) that have servoy-form-component properties in them
    public componentCache: Map<string, ComponentCache>;

    private _mainStructure: StructureCache;
    private _parts: Array<PartCache>;

    constructor(readonly formname: string, 
                public size: Dimension, 
                readonly responsive: boolean, 
                readonly url: string, 
                private readonly typesRegistry: TypesRegistry) {
        this.componentCache = new Map();
        this.partComponentsCache = [];
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


    public add(comp: ComponentCache | StructureCache | FormComponentCache, parent?: StructureCache | FormComponentCache | PartCache) {
        if (comp instanceof FormComponentCache){
             this.addFormComponent(comp);
        } else if (comp instanceof ComponentCache){    
            this.componentCache.set(comp.name, comp);
        }
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

    public getPart(name: string): PartCache{
         return this._parts.find(elem => elem.name == name);
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
    
    public getLayoutContainerByFCName(name: string): StructureCache {
		const arrHelp = Array.from(this.layoutContainersCache).filter(cont => {
			if (cont[1].items.length) {
				return cont[1].items.map(item => {
					if (item instanceof FormComponentCache) {
						return item.name;
					}
					return '';
				}).includes(name);
			}
			return false;
		});
        return arrHelp.length === 1 ? arrHelp[0][1] : undefined;
    }

    public removeComponent(name: string) {
        this.removeComponentFromCache(this.componentCache.get(name));
        this.componentCache.delete(name);
    }

    public removeLayoutContainer(id: string) {
        this.removeComponentFromCache(this.layoutContainersCache.get(id));
        this.layoutContainersCache.delete(id);
    }

    public removeFormComponent(name: string) {
        this.removeComponentFromCache(this.formComponents.get(name));
        this.formComponents.delete(name);
    }

    public getComponentSpecification(componentName: string) {
        let componentCache: ComponentCache = this.componentCache.get(componentName);
        if (!componentCache) componentCache = this.getFormComponent(componentName);
        return componentCache ? this.typesRegistry.getComponentSpecification(componentCache.specName) : undefined;
    }

    public getClientSideType(componentName: string, propertyName: string) {
        const componentSpec = this.getComponentSpecification(componentName);

        let type = componentSpec?.getPropertyType(propertyName);
        if (!type) type = this.componentCache.get(componentName)?.dynamicClientSideTypes[propertyName];
        if (!type) type = this.getFormComponent(componentName)?.dynamicClientSideTypes[propertyName];

        return type;
    }
    
    private removeComponentFromCache(comp: ComponentCache | StructureCache | FormComponentCache){
        if (comp){
            const index = this.partComponentsCache.indexOf(comp);
            if (index !== -1) this.partComponentsCache.splice(index,1);
        }
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

    /** called when there are changes pushed into this form's existing cache/state, so that this form can trigger a detection change */
    detectChanges(): void;
    
    /** called when there are changed pushed to this form (and a new form cache/state), so that this form can store the new cache and trigger a detection change on it's new cache/state */
    formCacheChanged(cache: FormCache): void;

    /** called when a model property is updated for the given compponent, but the value itself didn't change (only nested) */
    triggerNgOnChangeWithSameRefDueToSmartPropUpdate(componentName: string, propertiesChangedButNotByRef: {propertyName: string; newPropertyValue: unknown}[]): void;

    updateFormStyleClasses(ngutilsstyleclasses: string): void;
}

export interface IApiExecutor {
    callApi(componentName: string, apiName: string, args: Array<unknown>, path?: string[]): unknown;
}

export const instanceOfApiExecutor = (obj: unknown): obj is IApiExecutor =>
    obj != null && (obj as IApiExecutor).callApi instanceof Function;

export const instanceOfFormComponent = (obj: unknown): obj is IFormComponent =>
    obj != null && (obj as IFormComponent).detectChanges instanceof Function;

/** More (but internal not servoy public) impl. for IComponentCache implementors. */
export class ComponentCache implements IComponentCache {

    /**
     * The dynamic client side types of a component's properties (never null, can be an empty obj). These are client side types sent from server that are
     * only known at runtime (so not directly from .spec). For example dataproviders could decide that they send 'date' types.
     *
     * Call FormCache.getClientSideType(componentName, propertyName) instead if you want a combination of static client-side-type from it's spec and dynamic client
     * side type for a component's property when converting data to be sent to server.
     */
    public readonly dynamicClientSideTypes: Record<string, IType<unknown>> = {};
    public readonly model: { [property: string]: unknown,
                                containedForm?: {
                                    absoluteLayout?: boolean,
                                },
                                styleClass?: string,
                                size?: {width: number, height: number},
                                containers?: {  added: { [container: string]: string[] }; removed: { [container: string]: string[] } }, 
                                cssstyles?: { [container: string]: { [classname: string]: string } } };

    /** this is used as #ref inside form_component.component.ts and it has camel-case instead of dashes */
    public readonly type: string;

    public parent: StructureCache;

    constructor(public readonly name: string,
        public readonly specName: string, // the directive name / component name (can be used to identify it's WebObjectSpecification)
        elType: string, // can be undefined in which case specName is used (this will only be defined in case of default tabless/accordion)
        public readonly handlers: Array<string>,
        public layout: { [property: string]: string },
        public readonly typesRegistry: TypesRegistry) {
            this.type = ComponentCache.convertToJSName(elType ? elType : specName);
            this.model = {visible:true};
    }

    private static convertToJSName(webObjectSpecName: string) {
        if (webObjectSpecName) {
            // transform webObjectSpecName like testpackage-myTestService (as it is defined in the .spec files) into
            // testPackageMyTestService - as this is needed sometimes client-side
            // but who knows, maybe someone will try the dashed version and wonder why it doesn't work

            // this should do the same as ClientService.java #convertToJSName()
            const packageAndName = webObjectSpecName.split('-');
            if (packageAndName.length > 1) {
                webObjectSpecName = packageAndName[0];
                for (let i = 1; i < packageAndName.length; i++) {
                    if (packageAndName[1].length > 0) webObjectSpecName += packageAndName[i].charAt(0).toUpperCase() + packageAndName[i].slice(1);
                }
            }
        }
        return webObjectSpecName;
    }

    initForDesigner(initialModelProperties: { [property: string]: unknown }): ComponentCache {
        // set initial model contents
        for (const key of Object.keys(initialModelProperties)) {
            this.model[key] = initialModelProperties[key];
        }
        return this;
    }
	
	initForDesignerIsVariant(initialModelProperties: { [property: string]: unknown }, isComponentVariant: boolean ): ComponentCache {
	      // set initial model contents
	      for (const key of Object.keys(initialModelProperties)) {
			if(isComponentVariant && key!='styleClass') this.model[key] = initialModelProperties[key];
	      }
	      return this;
	  }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    sendChanges(_propertyName: string, _newValue: unknown, _oldValue: unknown, _rowId?: string, _isDataprovider?: boolean) {
        // empty method with no impl for the designer (for example in LFC when the components are plain ComponentCache objects not the Subcass.)
    }

    toString() {
        return 'ComponentCache(' + this.name + ', '+ this.type + ')';
    }

}

export class StructureCache {
    public parent: StructureCache;
    public model:  { [property: string]: unknown } = {};
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
            return child;
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
    constructor(public readonly name: string,
        public readonly classes: Array<string>,
        public layout: { [property: string]: string },
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
        specName: string,
        elType: string,
        handlers: Array<string>,
        public responsive: boolean,
        layout: { [property: string]: string },
        public readonly formComponentProperties: FormComponentProperties,
        public readonly hasFoundset: boolean,
        typesRegistry: TypesRegistry) {
            super(name, specName, elType, handlers, layout, typesRegistry);
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

    initForDesigner(initialModelProperties: { [property: string]: unknown }): FormComponentCache {
        super.initForDesigner(initialModelProperties);
        return this;
    }

}

export class FormComponentProperties {
    constructor(public classes: Array<string>,
        public layout: { [property: string]: string },
        public readonly attributes: { [property: string]: string }) {
    }
}


export class Dimension {
    public width: number;
    public height: number;
}

export interface Position {
    x: number;
    y: number;
}

export interface CSSPosition {
    top: string;
    left: string;
    bottom: string;
    right: string;
    width: string;
    height: string;
}

