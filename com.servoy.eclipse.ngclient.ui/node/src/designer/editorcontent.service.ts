import { Injectable } from '@angular/core';
import { FormService } from '../ngclient/form.service';
import { IDesignFormComponent } from './servoydesigner.component';
import { ComponentCache, StructureCache, FormComponentCache } from '../ngclient/types';
import { ConverterService } from '../sablo/converter.service';

@Injectable()
export class EditorContentService {
    designFormCallback: IDesignFormComponent;

    constructor(private formService: FormService, protected converterService: ConverterService) {

    }

    setDesignFormComponent(designFormCallback: IDesignFormComponent) {
        this.designFormCallback = designFormCallback;
    }

    updateFormData(updates) {
        let data = JSON.parse(updates);
        let formCache = this.formService.getFormCacheByName(this.designFormCallback.getFormName());
        let refresh = false;
        let redrawDecorators = false;
        let reorderPartComponents: boolean;
        let reorderLayoutContainers: Array<StructureCache> = new Array();
        if (data.ng2containers) {
            data.ng2containers.forEach((elem) => {
                let container = formCache.getLayoutContainer(elem.attributes['svy-id']);
                if (container) {
                    container.classes = elem.styleclass;
                    container.attributes = elem.attributes;
                    const parentUUID = data.childParentMap[container.id].uuid;
                    let newParent = container.parent;
                    if (parentUUID) {
                        newParent = formCache.getLayoutContainer(parentUUID);
                    }
                    else{
                        newParent = formCache.mainStructure
                    }
                    if (container.parent.id !=  newParent.id){
                        // we moved it to another parent
                        container.parent.removeChild(container);
                        newParent.addChild(container);
                    }
                    if (reorderLayoutContainers.indexOf(newParent) < 0) {
                        // existing layout container in parent layout container , make sure is inserted in correct position
                        reorderLayoutContainers.push(newParent);
                    }
                }
                else {
                    container = new StructureCache(elem.tagname, elem.styleclass, elem.attributes, [], elem.attributes ? elem.attributes['svy-id'] : null);
                    formCache.addLayoutContainer(container);
                    const parentUUID = data.childParentMap[container.id].uuid;
                    if (parentUUID) {
                        let parent = formCache.getLayoutContainer(parentUUID);
                        if (parent) {
                            parent.addChild(container);
                            if (reorderLayoutContainers.indexOf(parent) < 0) {
                                // new layout container in parent layout container , make sure is inserted in correct position
                                reorderLayoutContainers.push(parent);
                            }
                        }
                    }
                    else {
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
                let component = formCache.getComponent(elem.name);
                if (component) {
                    component.layout = elem.position;
                    const beanConversion = elem.model[ConverterService.TYPES_KEY];
                    for (const property of Object.keys(elem.model)) {
                        let value = elem.model[property];
                        if (beanConversion && beanConversion[property]) {
                            value = this.converterService.convertFromServerToClient(value, beanConversion[property], component.model[property],
                                (prop: string) => component.model ? component.model[prop] : component.model);
                        }
                        if (property === 'size' && (component.model[property].width !== value.width || component.model[property].height !== value.height) ||
                                property === 'location' && (component.model[property].x !== value.x || component.model[property].y !== value.y)) {
                            redrawDecorators = true;
                        }
                        component.model[property] = value;
                    }
                    // existing component updated, make sure it is in correct position relative to its sibblings
                    if (component.parent) {
                        if (reorderLayoutContainers.indexOf(component.parent) < 0) {
                            reorderLayoutContainers.push(component.parent);
                        }
                    }
                    else if (formCache.absolute) {
                        reorderPartComponents = true;
                    }
                }
                else {
                    redrawDecorators = true;
                    if (elem.model[ConverterService.TYPES_KEY] != null) {
                        this.converterService.convertFromServerToClient(elem.model, elem.model[ConverterService.TYPES_KEY], null,
                            (property: string) => elem.model ? elem.model[property] : elem.model);
                    }
                    const comp = new ComponentCache(elem.name, elem.type, elem.model, elem.handlers, elem.position);
                    formCache.add(comp);
                    if (!formCache.absolute) {
                        let parentUUID = data.childParentMap[elem.name].uuid;
                        if (parentUUID) {
                            let parent = formCache.getLayoutContainer(parentUUID);
                            if (parent) {
                                parent.addChild(comp);
                                if (reorderLayoutContainers.indexOf(parent) < 0) {
                                    // new component in layout container , make sure is inserted in correct position
                                    reorderLayoutContainers.push(parent);
                                }
                            }
                        }
                    }
                    else {
                        formCache.partComponentsCache.push(comp);
                        reorderPartComponents = true;
                    }
                }
            });
            refresh = true;
        }
        if (data.deleted) {
            data.deleted.forEach((elem) => {
                const comp = formCache.getComponent(elem);
                if (comp) {
                    formCache.removeComponent(elem);
                    if (!formCache.absolute) {
                        this.removeChildRecursively(comp, formCache.mainStructure);
                    }
                }
            });
            refresh = true;
        }
        if (data.deletedContainers) {
            data.deletedContainers.forEach((elem) => {
                const container = formCache.getLayoutContainer(elem);
                if (container) {
                    formCache.removeLayoutContainer(elem);
                    this.removeChildRecursively(container, formCache.mainStructure);
                }
            });
            refresh = true;
        }
        if (reorderPartComponents) {
            // make sure the order of components in absolute layout is correct, based on formindex
            this.sortChildren(formCache.partComponentsCache);
        }
        for (let container of reorderLayoutContainers) {
            // make sure the order of components in responsive layout containers is correct, based on location
            this.sortChildren(container.items);
        }
        if (data.renderGhosts) {
            this.designFormCallback.renderGhosts();
        }
        if (refresh) {
            this.designFormCallback.refresh();
        }
        if (redrawDecorators) {
            this.designFormCallback.redrawDecorators();
        }
    }

    updateForm(uuid: string, parentUuid: string, width: number, height: number) {
        /*if (formData.parentUuid !== parentUuid)
        {
            this.contentRefresh();
        }*/
        this.designFormCallback.updateForm(width, height);
    }
    contentRefresh() {

    }

    updateStyleSheets() {

    }

    setDirty() {

    }

    private removeChildRecursively(child: ComponentCache | StructureCache, parent: StructureCache) {
        if (!parent.removeChild(child)) {
            if (parent.items) {
                parent.items.forEach((elem) => {
                    if (elem instanceof StructureCache) {
                        this.removeChildRecursively(child, elem);
                    }
                });
            }
        }
    }

    private sortChildren(items: Array<StructureCache | ComponentCache | FormComponentCache>) {
        if (items) {
            items.sort((comp1, comp2): number => {
                let priocomp1 = comp1 instanceof StructureCache ? parseInt(comp1.attributes["svy-priority"]) : parseInt(comp1.model.servoyAttributes["svy-priority"]);
                let priocomp2 = comp2 instanceof StructureCache ? parseInt(comp2.attributes["svy-priority"]) : parseInt(comp2.model.servoyAttributes["svy-priority"]);
                // priority is location in responsive form and formindex in absolute form
                if (priocomp2 > priocomp1) {
                    return -1;
                }
                return 1;
            });
        }
    }
}