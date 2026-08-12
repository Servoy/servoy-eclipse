import {
  Component, Input, TemplateRef, Renderer2, ChangeDetectionStrategy, SimpleChange
} from '@angular/core';

import { FormCache, StructureCache, FormComponentCache, ComponentCache } from '../types';
import { ServoyBaseComponent, LoggerService } from '@servoy/public';
import { ConverterService } from '../../sablo/converter.service';
import { IWebObjectSpecification, PushToServerUtils } from '../../sablo/types_registry';

@Component({
    template: '',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true
})
export abstract class AbstractFormComponent {

    _containers!: { added: { [container: string]: string[] }; removed: { [container: string]: string[] } };
    _cssstyles!: { [container: string]: { [classname: string]: string } };
    protected componentCache: { [property: string]: ServoyBaseComponent<any> } = {};

    constructor(protected renderer: Renderer2) {
    }

    get containers() {
        return this._containers;
    }

    @Input()
    set containers(containers: { added: { [container: string]: string[] }; removed: { [container: string]: string[] } }) {
        if (!containers) return;
        for (const containername of Object.keys(containers.added)) {
            const container = this.getContainerByName(containername);
            if (container) {
                containers.added[containername].forEach((cls: string) => this.renderer.addClass(container, cls));
            }
        }
        if (this._containers && this._containers.added) {
            for (const containername of Object.keys(this._containers.added)) {
                const container = this.getContainerByName(containername);
                if (container) {
                    let classesToRemove = this._containers.added[containername];
                    if (containers.added[containername]) {
                        const stillToAdd = containers.added[containername];
                        classesToRemove = classesToRemove.filter((value: string) => stillToAdd.indexOf(value) === -1);
                    }
                    classesToRemove.forEach((cls: string) => this.renderer.removeClass(container, cls));
                }
            }
        }
        for (const containername of Object.keys(containers.removed)) {
            const container = this.getContainerByName(containername);
            if (container) {
                containers.removed[containername].forEach((cls: string) => this.renderer.removeClass(container, cls));
            }
        }
        if (this._containers && this._containers.removed) {
            for (const containername of Object.keys(this._containers.removed)) {
                const container = this.getContainerByName(containername);
                if (container) {
                    let classesToAddBackIn = this._containers.removed[containername];
                    if (containers.removed[containername]) {
                        const stillToRemove = containers.removed[containername];
                        classesToAddBackIn = classesToAddBackIn.filter((value: string) => stillToRemove.indexOf(value) === -1);
                    }
                    classesToAddBackIn.forEach((cls: string) => this.renderer.addClass(container, cls));
                }
            }
        }
        this._containers = containers;
    }

    // eslint-disable-next-line @typescript-eslint/member-ordering
    get cssstyles() {
        return this._cssstyles;
    }

    @Input()
    set cssstyles(cssStyles: { [container: string]: { [classname: string]: string } }) {
        if (!cssStyles) return;
        this._cssstyles = cssStyles;
        for (const containername of Object.keys(cssStyles)) {
            const container = this.getContainerByName(containername);
            if (container) {
                const stylesMap = cssStyles[containername];
                for (const key of Object.keys(stylesMap)) {
                    this.renderer.setStyle(container, key, stylesMap[key]);
                }
            }
        }
    }

    triggerNgOnChangeWithSameRefDueToSmartPropUpdate(componentName: string, propertiesChangedButNotByRef: { propertyName: string; newPropertyValue: any }[]): void {
        const comp = this.componentCache[componentName];
        if (comp) {
            const changes: Record<string, any> = {};
            propertiesChangedButNotByRef.forEach((propertyChangedButNotByRef) => {
                changes[propertyChangedButNotByRef.propertyName] = new SimpleChange(propertyChangedButNotByRef.newPropertyValue, propertyChangedButNotByRef.newPropertyValue, false);
            });
            comp.ngOnChanges(changes);
            comp.detectChanges();
        }
    }

    abstract getFormCache(): FormCache;

    abstract getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any>;

    abstract getTemplateForLFC(state: ComponentCache): TemplateRef<any>;

    abstract getContainerByName(containername: string): Element;

    getNGClass(_item: StructureCache): { [klass: string]: any } | null {
        return null;
    }

    isDesigner(): boolean {
        return false;
    }

    public static doCallApiOnComponent(comp: ServoyBaseComponent<any>, componentSpec: IWebObjectSpecification, apiName: string, args: any[],
        converterService: ConverterService<unknown>, log: LoggerService, compName: string): Promise<any> {
        const callSpec = componentSpec?.getApiFunction(apiName);

        (args as any[])?.forEach((val: any, i: number) =>
            args[i] = converterService.convertFromServerToClient(val, callSpec?.getArgumentType(i),
                undefined!, undefined!, undefined!, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES));

        if (comp) {
            const proto = Object.getPrototypeOf(comp);
            if (proto[apiName]) {
                return Promise.resolve(proto[apiName].apply(comp, args)).then((ret) =>
                    converterService.convertFromClientToServer(ret, callSpec?.returnType!, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES)[0]
                );
            } else {
                log.error(log.buildMessage(() => ('Api ' + apiName + ' for component ' + comp.name + ' was not found, please check component implementation.')));
                return null!;
            }
        }
        else {
            log.error(log.buildMessage(() => ('Trying to call api ' + apiName + ' while its component ' + compName + ' was not found,make sure component is present and visible.')));
            return null!;
        }

    }

}
