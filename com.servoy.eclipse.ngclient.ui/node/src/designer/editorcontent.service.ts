import { Injectable } from '@angular/core';
import { FormService, ServerElement } from '../ngclient/form.service';
import { IDesignFormComponent } from './servoydesigner.component';
import { ComponentCache, StructureCache, FormComponentCache, FormComponentProperties, FormCache, CSSPosition, Position, PartCache } from '../ngclient/types';
import { ConverterService } from '../sablo/converter.service';
import { IComponentCache } from '@servoy/public';
import { TypesRegistry, IWebObjectSpecification, RootPropertyContextCreator } from '../sablo/types_registry';

@Injectable()
export class EditorContentService {
    designFormCallback: IDesignFormComponent;

    constructor(private formService: FormService, protected converterService: ConverterService<unknown>, private typesRegistry: TypesRegistry) {

    }

    setDesignFormComponent(designFormCallback: IDesignFormComponent) {
        this.designFormCallback = designFormCallback;
    }

    updateFormData(updates: string) {
        if (this.designFormCallback.getFormName() === 'VariantsForm') return;
        const formCache = this.formService.getFormCacheByName(this.designFormCallback.getFormName());

        const data = JSON.parse(updates);
        const reorderLayoutContainers: Array<StructureCache> = new Array();
        const orphanLayoutContainers: Array<StructureCache> = new Array();
        let renderGhosts = false;
        let redrawDecorators = false;
        let refresh = false;
        let reorderPartComponents: boolean;

        if (data.updatedFormComponentsDesignId) {
            for (const index of Object.keys(data.updatedFormComponentsDesignId)) {
                const fcname = data.updatedFormComponentsDesignId[index].startsWith('_') ? data.updatedFormComponentsDesignId[index].substring(1) : data.updatedFormComponentsDesignId[index];
                const fc = formCache.getFormComponent(fcname.replace(/_/g, '-'));
                if (fc) {
                    fc.items.forEach(item => {
                        if (item instanceof ComponentCache)
                            formCache.removeComponent(item.name);
                        else if (item instanceof StructureCache) {
                            formCache.removeLayoutContainer(item.id);
                            this.removeChildrenRecursively(item, formCache);
                        }
                    });
                    const parentUUID = data.childParentMap[fc.name] ? data.childParentMap[fc.name].uuid : undefined;
                    if (parentUUID) {
                        const parent = formCache.getLayoutContainer(parentUUID);
                        if (parent) {
							if (!parent.removeChild(fc)) {
								const parent = formCache.getLayoutContainerByFCName(fc.name);
								if (parent) {
									parent.removeChild(fc);
								}
							}
                            
                        }
                    }
                }
                formCache.removeFormComponent(fcname.replace(/_/g, '-'));
                renderGhosts = true;
                redrawDecorators = true;
            }
        }

        if (data.ng2containers) {
            data.ng2containers.forEach((elem) => {
                let container = formCache.getLayoutContainer(elem.attributes['svy-id']);
                if (container) {
                    redrawDecorators = true;
                    container.classes = elem.styleclass;
                    container.attributes = elem.attributes;
                    container.layout = elem.position;
                    const parentUUID = data.childParentMap[container.id].uuid;
                    if (container.parent) {
                        let newParent = container.parent;
                        if (parentUUID) {
                            newParent = formCache.getLayoutContainer(parentUUID);
                        } else {
                            newParent = formCache.mainStructure;
                        }
                        if (container?.parent?.id !== newParent?.id) {
                            // we moved it to another parent
                            container.parent.removeChild(container);
                            newParent.addChild(container);
                        } else if (newParent?.items.indexOf(container) < 0) {
                            newParent.addChild(container);
                        }
                        if (reorderLayoutContainers.indexOf(newParent) < 0) {
                            // existing layout container in parent layout container , make sure is inserted in correct position
                            reorderLayoutContainers.push(newParent);
                        }
                    }
                } else {
                    container = new StructureCache(elem.tagname, elem.styleclass, elem.attributes, [], elem.attributes ? elem.attributes['svy-id'] : null, elem.cssPositionContainer, elem.position);
                    formCache.addLayoutContainer(container);
                    redrawDecorators = true;
                    const parentUUID = data.childParentMap[container.id].uuid;
                    if (parentUUID) {
                        const parent = formCache.getLayoutContainer(parentUUID);
                        if (parent) {
                            parent.addChild(container);
                            if (reorderLayoutContainers.indexOf(parent) < 0) {
                                // new layout container in parent layout container , make sure is inserted in correct position
                                reorderLayoutContainers.push(parent);
                            }
                        } else {
                            const fc = formCache.getFormComponent(parentUUID);
                            if (fc) {
                                fc.addChild(container);
                            } else {
                                // parent is not created yet, look for it later
                                orphanLayoutContainers.push(container);
                            }
                        }
                    } else if (formCache.absolute) {
                        formCache.partComponentsCache.push(container);
                    } else {
                        // dropped directly on form
                        if (formCache.mainStructure == null) {
                            formCache.mainStructure = new StructureCache(null, null);
                        }
                        formCache.mainStructure.addChild(container);
                        if (reorderLayoutContainers.indexOf(formCache.mainStructure) < 0) {
                            // new layout container in parent form , make sure is inserted in correct position
                            reorderLayoutContainers.push(formCache.mainStructure);
                        }
                    }
                }
            });
            refresh = true;
        }
        if (data.ng2components) {
            data.ng2components.forEach((elem) => {
                const component = formCache.getComponent(elem.name);
                if (component) {
                    redrawDecorators = this.updateComponentProperties(component, elem) || redrawDecorators;
                    // existing component updated, make sure it is in correct position relative to its sibblings
                    if (component instanceof ComponentCache && component.parent) {
                        const currentParent = data.childParentMap[component.name];
                        if (currentParent && component.parent.id !== currentParent.uuid) {
                            component.parent.removeChild(component);
                            formCache.getLayoutContainer(currentParent.uuid).addChild(component);
                        }
                        redrawDecorators = true;
                        if (reorderLayoutContainers.indexOf(component.parent) < 0) {
                            reorderLayoutContainers.push(component.parent);
                        }
                    } else if (formCache.absolute) {
                        reorderPartComponents = true;
                    }
                    if ((elem.specName === 'servoycore-formcomponent' || elem.specName === 'servoycore-listformcomponent') && !elem.responsive) {
                        const layout: { [property: string]: string } = {};
                        this.fillLayout(elem, formCache, layout);
                        (component as FormComponentCache).formComponentProperties.layout = layout;
                        (component as FormComponentCache).formComponentProperties.classes = elem.model.styleClass ? elem.model.styleClass.trim().split(' ') : new Array();
                    }
                } else if (formCache.getFormComponent(elem.name) == null) {
                    redrawDecorators = true;

                    const rawModelProperties = elem.model;
                    elem.model = {};

                    const componentSpec: IWebObjectSpecification = this.typesRegistry.getComponentSpecification(elem.specName);
                    const componentDynamicTypesHolder = {};
                    const propertyContextCreator = new RootPropertyContextCreator((propertyName: string) => elem.model?.[propertyName], componentSpec);

                    for (const propName of Object.keys(rawModelProperties)) {
                        elem.model[propName] = this.converterService.convertFromServerToClient(rawModelProperties[propName],
                            componentSpec?.getPropertyType(propName), null,
                            componentDynamicTypesHolder, propName, propertyContextCreator.withPushToServerFor(propName));
                        // as we are in designer here, we don't listen for any potential change aware values after conversions (see FormService.handleComponentModelConversionsAndChangeListeners(...))
                    }

                    if (elem.specName === 'servoycore-formcomponent' || elem.specName === 'servoycore-listformcomponent') {
                        const classes: Array<string> = elem.model.styleClass ? elem.model.styleClass.trim().split(' ') : new Array();
                        const layout: { [property: string]: string } = {};
                        this.fillLayout(elem, formCache, layout);
                        const formComponentProperties: FormComponentProperties = new FormComponentProperties(classes, layout, elem.model.servoyAttributes);
                        const fcc = new FormComponentCache(elem.name, elem.specName, undefined, elem.handlers, elem.responsive, elem.position,
                            formComponentProperties, elem.model.foundset, this.typesRegistry).initForDesigner(elem.model);
                        formCache.addFormComponent(fcc);
                        const parentUUID = data.childParentMap[elem.name] ? data.childParentMap[elem.name].uuid : undefined;
                        if (parentUUID) {
                            const parent = formCache.getLayoutContainer(parentUUID);
                            if (parent) {
                                parent.addChild(fcc);
                                if (reorderLayoutContainers.indexOf(parent) < 0) {
                                    // new component in layout container , make sure is inserted in correct position
                                    reorderLayoutContainers.push(parent);
                                }
                            }
                        } else if (formCache.absolute) {
                            formCache.partComponentsCache.push(fcc);
                        }
                        const containers = data?.formComponentContainers?.[elem.name];
                        containers?.forEach((elem) => {
                            const container = new StructureCache(elem.tagname, elem.styleclass, elem.attributes, [], elem.attributes ? elem.attributes['svy-id'] : null, elem.cssPositionContainer, elem.position);
                            formCache.addLayoutContainer(container);
                            const parentUUID = data.childParentMap[container.id] ? data.childParentMap[container.id].uuid : undefined;
                            
                            if (!parentUUID || parentUUID == fcc.name) {
                                // parent is null or the form component component itself, so it is the child of the fcc directly.
                                fcc.addChild(container);
                            } else {
                               // else parent is a layout container from inside the fcc; we rely here on the fact that "data?.formComponentContainers?.[elem.name]"
                               // comes from the server in the correct order (top most containers first, so always when we iterate the parents are already created/added by this loop)
                               const parent = this.findStructureCache(fcc.items, parentUUID);
                               if (parent) parent.addChild(container);
                            }
                        });
                    } else {
                        const comp = new ComponentCache(elem.name, elem.specName, elem.elType, elem.handlers, elem.position, this.typesRegistry).initForDesigner(elem.model);
                        formCache.add(comp);
                        const parentUUID = data.childParentMap[elem.name] ? data.childParentMap[elem.name].uuid : undefined;
                        if (parentUUID) {
                            const parent = formCache.getLayoutContainer(parentUUID);
                            if (parent) {
                                parent.addChild(comp);
                                if (reorderLayoutContainers.indexOf(parent) < 0) {
                                    // new component in layout container , make sure is inserted in correct position
                                    reorderLayoutContainers.push(parent);
                                }
                            }
                        } else if (!data.formComponentsComponents || data.formComponentsComponents.indexOf(elem.name) === -1) {
                            formCache.partComponentsCache.push(comp);
                            reorderPartComponents = true;
                        }
                    }
                }
            });
            data.ng2components.forEach((elem) => {
                //FORM COMPONENTS
                const component = formCache.getFormComponent(elem.name); // elem.name is actually the design id; see PersistIdentifier java class
                if (component) {
                    if (data.updatedFormComponentsDesignId) {
//                        let fixedName = elem.name.replace(/-/g, '_');
//                        if (!isNaN(fixedName[0])) {
//                            fixedName = '_' + fixedName;
//                        }
                        if ((data.updatedFormComponentsDesignId.indexOf(elem.name)) !== -1) {
                            refresh = true;
                            const formComponent = component as FormComponentCache;
                            if (formComponent.responsive) {
                                formComponent.items.slice().forEach((item) => {
                                    if (item['id'] !== undefined && data.childParentMap[item['id']] === undefined) {
                                        formCache.removeLayoutContainer(item['id']);
                                        this.removeChildFromParentRecursively(item, formComponent);
                                    }
                                });
                            }
                            component.responsive = elem.responsive;
                            const formComponentComponentIdentifier = PersistIdentifier.fromJSONString(elem.name);
                            data.formComponentsComponents?.forEach((child: string) => {  // so "child" is actually a design id here; see PersistIdentifier java class
                                if (PersistIdentifier.fromJSONString(child).isDirectlyNestedInside(formComponentComponentIdentifier)) {
                                    const formComponentComponent = formCache.getComponent(child);
                                    const container = this.findStructureCache(formComponent.items,data.childParentMap[child].uuid); 
                                    if ((formComponent.responsive && container) || container) {
                                        formComponent.removeChild(formComponentComponent);
                                        container.removeChild(formComponentComponent);
                                        container.addChild(formComponentComponent);
                                    } else {
                                        formComponent.removeChild(formComponentComponent);
                                        formComponent.addChild(formComponentComponent);
                                    }
                                }
                            });
                        }
                    }
                    redrawDecorators = this.updateComponentProperties(component, elem) || redrawDecorators;
                    if (!component.model.containedForm && component.items && component.items.length > 0) {
                        this.removeChildrenRecursively(component, formCache);
                        component.items = [];
                        // how can we know if the old components had ghosts or not
                        renderGhosts = true;
                    }
                }

                // TODO else create FC SVY-16912
            });
            refresh = true;
        }
        
        if (data.parts){
            for (let name in data.parts) {
                // string style suffix
                const style = data.parts[name];
                name = name.substring(0,name.length-5);
                let partCache = formCache.getPart(name);
                if (partCache){
                    partCache.layout = JSON.parse(style);
                    refresh = true;
                }else{
                    const part = new PartCache(name, null, JSON.parse(style));
                    formCache.addPart(part);
                }
            }
        }
        
        if (data.deleted) {
            data.deleted.forEach((elem) => {
                const comp = formCache.getComponent(elem);
                if (comp) {
                    formCache.removeComponent(elem);
                    formCache.removeFormComponent(elem);
                    if (!formCache.absolute) {
                        this.removeChildFromParentRecursively(comp, formCache.mainStructure);
                    } else if (comp.parent) {
                        comp.parent.removeChild(comp);
                    }
                }
            });
            refresh = true;
            redrawDecorators = true;
        }

        if (data.deletedContainers) {
            data.deletedContainers.forEach((elem) => {
                const container = formCache.getLayoutContainer(elem);
                if (container) {
                    formCache.removeLayoutContainer(elem);
                    if (formCache.mainStructure) this.removeChildFromParentRecursively(container, formCache.mainStructure);
                    else if (container.parent) container.parent.removeChild(container);
                    this.removeChildrenRecursively(container, formCache);
                }
            });
            refresh = true;
            redrawDecorators = true;
        }

        if (reorderPartComponents) {
            // make sure the order of components in absolute layout is correct, based on formindex
            this.sortChildren(formCache.partComponentsCache);
        }

        for (const container of orphanLayoutContainers) {
            const parentUUID = data.childParentMap[container.id].uuid;
            if (parentUUID) {
                const parent = formCache.getLayoutContainer(parentUUID);
                if (parent) {
                    parent.addChild(container);
                } else {
                    const fc = formCache.getFormComponent(parentUUID);
                    if (fc) {
                        fc.addChild(container);
                    }
                }
            }
        }

        for (const container of reorderLayoutContainers) {
            // make sure the order of components in responsive layout containers is correct, based on location
            this.sortChildren(container.items);
        }
        if (data.renderGhosts || renderGhosts) {
            this.designFormCallback.renderGhosts();
        }
        if (refresh) {
            this.designFormCallback.refresh();
        }
        if (redrawDecorators) {
            this.designFormCallback.redrawDecorators();
        }
    }

    updateComponentProperties(component: IComponentCache, elem: ServerElement): boolean {
        let redrawDecorators = false;
        component.layout = elem.position;

        const componentSpec: IWebObjectSpecification = this.typesRegistry.getComponentSpecification(elem.specName);
        const componentDynamicTypesHolder = {};
        const propertyContextCreator = new RootPropertyContextCreator((propertyName: string) => component.model?.[propertyName], componentSpec);

        for (const propName of Object.keys(elem.model)) {
            const value = this.converterService.convertFromServerToClient(elem.model[propName],
                componentSpec?.getPropertyType(propName), component.model[propName],
                componentDynamicTypesHolder, propName, propertyContextCreator.withPushToServerFor(propName)) as (CSSPosition & Position);
            // as we are in designer here, we don't listen for any potential change aware values after conversions (see FormService.handleComponentModelConversionsAndChangeListeners(...))
            if (
                (propName === 'size' && (component.model[propName].width !== value.width || component.model[propName].height !== value.height)) ||
                (propName === 'location' && (component.model[propName].x !== value.x || component.model[propName].y !== value.y)) ||
                (propName === 'anchors' && component.model[propName] !== value) ||
                (
                    propName === 'cssPosition' &&
                    (component.model[propName].top !== value.top || component.model[propName].bottom !== value.bottom ||
                        component.model[propName].left !== value.left || component.model[propName].right !== value.right ||
                        component.model[propName].width !== value.width || component.model[propName].height !== value.height)
                )
            ) {
                redrawDecorators = true;
            }

            component.model[propName] = value;
        }
        for (const property of Object.keys(component.model)) {
            if (elem.model[property] === undefined) {
                delete component.model[property];
            }
        }
        return redrawDecorators;
    }

    updateForm(_uuid: string, _parentUuid: string, width: number, height: number) {
        /*if (formData.parentUuid !== parentUuid)
        {
            this.contentRefresh();
        }*/
        this.designFormCallback.updateForm(width, height);
    }

    contentRefresh() {
        this.designFormCallback.contentRefresh();
    }

    updateStyleSheets() {

    }

    setDirty() {

    }
    
    private findStructureCache(items: Array<StructureCache | ComponentCache | FormComponentCache>, id: string): StructureCache {
        for(const item of items) {
            if (item instanceof StructureCache) {
                if ( item.id == id) {
                    return item;
                }
                else {
                    const child =  this.findStructureCache(item.items, id);
                    if (child) return child;
                }
            }
        }
        return null;
    }

    private removeChildFromParentRecursively(child: ComponentCache | StructureCache | FormComponentCache, parent: StructureCache | FormComponentCache) {
        if (!parent.removeChild(child)) {
            if (parent.items) {
                parent.items.forEach((elem) => {
                    if (elem instanceof StructureCache) {
                        this.removeChildFromParentRecursively(child, elem);
                    }
                });
            }
        }
    }

    private removeChildrenRecursively(parent: StructureCache | FormComponentCache, formCache: FormCache) {
        if (parent.items) {
            parent.items.forEach((elem) => {
                if (elem instanceof StructureCache) {
                    formCache.removeLayoutContainer(elem.id);
                    this.removeChildrenRecursively(elem, formCache);
                } else if (elem instanceof FormComponentCache) {
                    formCache.removeFormComponent(elem.name);
                } else if (elem instanceof ComponentCache) {
                    formCache.removeComponent(elem.name);
                }
            });
        }
    }

    private sortChildren(items: Array<StructureCache | ComponentCache | FormComponentCache>) {
        if (items) {
            items.sort((comp1, comp2): number => {
                const priocomp1 = comp1 instanceof StructureCache ? parseInt(comp1.attributes['svy-priority'], 10) : parseInt(comp1.model.servoyAttributes['svy-priority'], 10);
                const priocomp2 = comp2 instanceof StructureCache ? parseInt(comp2.attributes['svy-priority'], 10) : parseInt(comp2.model.servoyAttributes['svy-priority'], 10);

                // priority is location in responsive form and formindex in absolute form
                if (priocomp2 > priocomp1) {
                    return -1;
                }
                return 1;
            });
        }
    }

    private fillLayout(elem: any, formCache: FormCache, layout: { [property: string]: string }) {
        if (!elem.responsive) {
            // form component content is anchored layout
            const continingFormIsResponsive = !formCache.absolute;
            let minHeight = elem.model.minHeight !== undefined ? elem.model.minHeight : elem.model.height; // height is deprecated in favor of minHeight but they do the same thing;
            let minWidth = elem.model.minWidth !== undefined ? elem.model.minWidth : elem.model.width; // width is deprecated in favor of minWidth but they do the same thing;;
            let widthExplicitlySet: boolean;

            if (!minHeight && elem.model.containedForm) minHeight = elem.model.containedForm.formHeight;
            if (!minWidth && elem.model.containedForm) {
                widthExplicitlySet = false;
                minWidth = elem.model.containedForm.formWidth;
            } else widthExplicitlySet = true;

            if (minHeight) {
                layout['min-height'] = minHeight + 'px';
                if (!continingFormIsResponsive) layout['height'] = '100%'; // allow anchoring to bottom in anchored form + anchored form component
            }
            if (minWidth) {
                layout['min-width'] = minWidth + 'px'; // if the form that includes this form component is responsive and this form component is anchored,
                // allow it to grow in width to fill responsive space

                if (continingFormIsResponsive && widthExplicitlySet) {
                    // if container is in a responsive form, content is anchored and width model property is explicitly set
                    // then we assume that developer wants to really set width of the form component so it can put multiple of them inside
                    // for example a 12grid column; that means they should not simply be div / block elements; we change float as well
                    layout['float'] = 'left';
                }
            }
        }
    }
}

/**
 * Note that this class has a Java side equivalent; meant to identify persists in designer, be it simple components,
 * components nested inside form component components or custom types nested or not inside form component components.
 * 
 * @author acostescu
 */
class PersistIdentifier {
    
    private constructor(
        /**
         * something like ["D9884DBA_C5E7_4395_A934_52030EB8F1F0", "containedForm", "button_1"] if it's a persist from inside a
         * form component container or ["Z9884DBA_C5E7_4395_A934_52030EB8F1F0"] for a simple persist
         */
        public persistUUIDAndFCPropAndComponentPath: Array<string>,
        
        /** in case it wants to identify a "ghost" (form editor) persist that might be inside form components) */
        public customTypeOrComponentTypePropertyUUIDInsidePersist: string) {}

    /**
     * Writes it as a String in JSON format - that will probably be used client side in svy-id attributes,
     */
    public static fromJSONString(jsonContent: string): PersistIdentifier {
        if (jsonContent == null) return null;

        const firstChar = jsonContent.charAt(0);

        if (firstChar == '{') {
            // const GHOST_IDENTIFIER_INSIDE_COMPONENT = 'g';
            // const COMPONENT_LOCATOR_KEY = 'p';

            const parsed = JSON.parse(jsonContent) as { p: string | Array<string>, g: string };
            const componentLocator: string | Array<string> = parsed.p;
            const ghostIdentifierInsideComp: string = parsed.g;

            if (typeof componentLocator === 'string')
                return new PersistIdentifier([componentLocator], ghostIdentifierInsideComp);

            // if componentLocator is not a String, it can only be a JSONArray of Strings
            return new PersistIdentifier(componentLocator, ghostIdentifierInsideComp);
        } else if (firstChar == '[') {
            return new PersistIdentifier(JSON.parse(jsonContent) as Array<string>, undefined);
        } else return new PersistIdentifier([ jsonContent ], undefined);
    }
    
    /**
     * Returns true if this PersistIdentifier is of a persist that is a direct child of a form component identified by "parentPersistIdentifier".
     * 
     * For example if th identifier is in form component component fCC1 -> containedForm -> fCC2 -> containedForm -> myPersist
     * then this methods should only return true when "parentPersistIdentifier" is fCC1 -> containedForm -> fCC2 but
     * it should return false when "parentPersistIdentifier" is fCC1 or fCC1 -> containedForm -> otherRandomPersist or anotherRandomPersist... 
     */
    public isDirectlyNestedInside(parentPersistIdentifier: PersistIdentifier): boolean {
        if (parentPersistIdentifier.persistUUIDAndFCPropAndComponentPath.length + 2 !== this.persistUUIDAndFCPropAndComponentPath.length) return false;
        
        for (let i = parentPersistIdentifier.persistUUIDAndFCPropAndComponentPath.length - 1; i >= 0; i--) {
            if (parentPersistIdentifier.persistUUIDAndFCPropAndComponentPath[i] !== this.persistUUIDAndFCPropAndComponentPath[i]) return false;
        }
        return true;
    }
    
}