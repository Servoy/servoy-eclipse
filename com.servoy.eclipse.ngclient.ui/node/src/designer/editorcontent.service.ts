import { Injectable } from '@angular/core';
import { FormService } from '../ngclient/form.service';
import { IDesignFormComponent } from './servoydesigner.component';
import { ComponentCache, StructureCache } from '../ngclient/types';
import { ConverterService } from '../sablo/converter.service';

@Injectable()
export class EditorContentService {
    designFormCallback: IDesignFormComponent;

    constructor(private formService: FormService,protected converterService:ConverterService ) {

    }

    setDesignFormComponent(designFormCallback: IDesignFormComponent) {
        this.designFormCallback = designFormCallback;
    }

    updateFormData(updates) {
        let data = JSON.parse(updates);
        let formCache = this.formService.getFormCacheByName(this.designFormCallback.getFormName());
        let refresh = false;
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
                        component.model[property] = value;
                    }
                }
                else {
                    const comp = new ComponentCache(elem.name, elem.type, elem.model, elem.handlers, elem.position);
                    formCache.add(comp);
                    if (!formCache.absolute) {
                        let parentUUID = data.childParentMap[elem.name].uuid;
                        if (parentUUID) {
                            let parent = formCache.getLayoutContainer(parentUUID);
                            if (parent) {
                                parent.addChild(comp);
                            }
                        }
                    }
                }
            });
            refresh = true;
        }
        if (data.ng2containers) {
            data.ng2containers.forEach((elem) => {
                let container = formCache.getLayoutContainer(elem.id);
                if (container) {
                    container.classes = elem.styleclass;
                    container.attributes = elem.attributes;
                }
                else {
                    container = new StructureCache(elem.tagname, elem.styleclass, elem.attributes, elem.id);
                    formCache.addLayourContainer(container);
                    let parentUUID = data.childParentMap[elem.name].uuid;
                    if (parentUUID) {
                        let parent = formCache.getLayoutContainer(parentUUID);
                        if (parent) {
                            parent.addChild(container);
                        }
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
        if (data.renderGhosts) {
            this.designFormCallback.renderGhosts();
        }
        if (refresh) {
            this.designFormCallback.refresh();
        }

    }

    updateForm() {

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

}