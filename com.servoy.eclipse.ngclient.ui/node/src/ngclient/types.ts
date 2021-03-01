
export class FormSettings {
  public name: string;
  public size: { width: number; height: number };
}

export class FormCache {
    public navigatorForm: FormSettings;
    public size: Dimension;
    private componentCache: Map<string, ComponentCache>;
    private _mainStructure: StructureCache;
    private _formComponents: Map<string, FormComponentCache>;
    private _parts: Array<PartCache>;
    private conversionInfo = {};

    constructor(readonly formname: string, size: Dimension) {
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
        return this.componentCache.get(name);
    }

    public getConversionInfo(beanname: string) {
        return this.conversionInfo[beanname];
    }
    public addConversionInfo(beanname: string, conversionInfo: { [property: string]: string}) {
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
}

export interface IApiExecutor {
 callApi(componentName: string, apiName: string, args: Array<any>, path?: string[]): any;
}

export const instanceOfApiExecutor = (obj: any): obj is IApiExecutor =>
    obj != null && (obj).callApi instanceof Function;

export const instanceOfFormComponent = (obj: any): obj is IFormComponent =>
    obj != null && (obj).detectChanges instanceof Function;

export interface IComponentCache {
    name: string;
    model: { [property: string]: any };
}

export class ComponentCache implements IComponentCache {
    constructor(public readonly name: string,
        public readonly type: string,
        public readonly model: { [property: string]: any },
        public readonly handlers: Array<string>,
        public readonly layout: { [property: string]: string }) {
    }
}

export class StructureCache {
    constructor(public readonly classes: Array<string>,  public readonly attributes?: { [property: string]: string },
        public readonly styles?: string,
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

export class FormComponentCache implements IComponentCache {
    public items: Array<StructureCache | ComponentCache | FormComponentCache>;

    constructor(
            public readonly name: string,
            public readonly model: { [property: string]: any },
            public readonly handlers: { [property: string]: any },
            public readonly responsive: boolean,
            public readonly layout: { [property: string]: string },
            public readonly formComponentProperties: FormComponentProperties,
            public readonly hasFoundset: boolean,
            items?: Array<StructureCache | ComponentCache | FormComponentCache> ) {
            if ( !items ) this.items = [];
            else this.items = items;
        }

    addChild(child: StructureCache | ComponentCache | FormComponentCache) {
        if (!(child instanceof ComponentCache && (child as ComponentCache).type === 'servoycoreNavigator'))
            this.items.push(child);
    }
}

export class FormComponentProperties {
    constructor(public readonly classes: Array<string>,
        public readonly layout: { [property: string]: string }) {
    }
}

export class Dimension{
    public width: number;
    public height: number;
}
