import { IComponentCache, IFormCache } from '@servoy/public';

export class FormSettings {
    public name: string;
    public size: { width: number; height: number };
}

export class FormCache implements IFormCache {
    public navigatorForm: FormSettings;
    public size: Dimension;
    public componentCache: Map<string, ComponentCache>;
    public partComponentsCache: Array<ComponentCache | StructureCache>;
    public layoutContainersCache: Map<string, StructureCache>;
    public formComponents: Map<string, FormComponentCache>;
    private _mainStructure: StructureCache;
    private _parts: Array<PartCache>;
    private conversionInfo = {};
    private responsive: boolean;

    constructor(readonly formname: string, size: Dimension, responsive: boolean, public readonly url: string) {
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
        return this.componentCache.get(name);
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

    public getConversionInfo(beanname: string) {
        return this.conversionInfo[beanname];
    }
    public addConversionInfo(beanname: string, conversionInfo: { [property: string]: string }) {
        const beanConversion = this.conversionInfo[beanname];
        if (beanConversion == null) {
            this.conversionInfo[beanname] = conversionInfo;
        } else for (const key of Object.keys(conversionInfo)) {
            beanConversion[key] = conversionInfo[key];
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

export interface IFormComponent extends IApiExecutor {
    name: string;
    // called when there are changed pushed to this form, so this form can trigger a detection change
    detectChanges(): void;
    // called when there are changed pushed to this form, so this form can trigger a detection change
    formCacheChanged(cache: FormCache): void;
    // called when a model property is updated for the given compponent, but the value itself didn't change  (only nested)
    propertyChanged(componentName: string, property: string, value: any): void;

    updateFormStyleClasses(ngutilsstyleclasses: string): void;
}

export interface IApiExecutor {
    callApi(componentName: string, apiName: string, args: Array<any>, path?: string[]): any;
}

export const instanceOfApiExecutor = (obj: any): obj is IApiExecutor =>
    obj != null && (obj).callApi instanceof Function;

export const instanceOfFormComponent = (obj: any): obj is IFormComponent =>
    obj != null && (obj).detectChanges instanceof Function;

export class ComponentCache implements IComponentCache {
    public parent: StructureCache;

    constructor(public readonly name: string,
        public readonly type: string,
        public model: { [property: string]: any },
        public readonly handlers: Array<string>,
        public layout: { [property: string]: string }) {
    }

    sendChanges(dp: string, value: any, oldValue: any, rowId: any, dataprovider?: boolean) {
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

export class FormComponentCache implements IComponentCache {
    public items: Array<StructureCache | ComponentCache | FormComponentCache>;

    constructor(
        public readonly name: string,
        public readonly model: { [property: string]: any },
        public readonly handlers: Array<string>,
        public responsive: boolean,
        public layout: { [property: string]: string },
        public readonly formComponentProperties: FormComponentProperties,
        public readonly hasFoundset: boolean,
        items?: Array<StructureCache | ComponentCache | FormComponentCache>) {
        if (!items) this.items = [];
        else this.items = items;
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
