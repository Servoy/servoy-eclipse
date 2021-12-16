import { Directive, Input, ElementRef, OnChanges, SimpleChanges, Renderer2, Injector } from '@angular/core';
import { DesignFormComponent } from '../designer/designform_component.component';
import { AbstractFormComponent, FormComponent } from '../ngclient/form/form_component.component';
import { StructureCache } from '../ngclient/types';

@Directive({ selector: '[svyContainerStyle]' })
export class AddAttributeDirective implements OnChanges {
    @Input() svyContainerStyle: any;
    @Input() svyContainerLayout;
    @Input() svyContainerClasses : Array<string>;
    @Input() svyContainerAttributes;

    parent: AbstractFormComponent;

    constructor(private el: ElementRef, private renderer: Renderer2, private _injector: Injector) {
        try {
            this.parent = this._injector.get<FormComponent>(FormComponent);
        }
        catch (e) {
            //ignore
        }

        if (!this.parent) {
            this.parent = this._injector.get<DesignFormComponent>(DesignFormComponent);
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.svyContainerClasses && this.svyContainerClasses) {
            this.svyContainerClasses.forEach(cls => this.renderer.addClass(this.el.nativeElement, cls));
        }

        if (changes.svyContainerLayout && this.svyContainerLayout) {
            for (const key of Object.keys(this.svyContainerLayout)) {
                this.renderer.setStyle(this.el.nativeElement, key, this.svyContainerLayout[key]);
            }
        }
        if (changes.svyContainerAttributes && this.svyContainerAttributes) {
            for (const key of Object.keys(this.svyContainerAttributes)) {
                this.renderer.setAttribute(this.el.nativeElement, key, this.svyContainerAttributes[key]);
                if (key === 'name' && this.svyContainerStyle instanceof StructureCache) this.restoreCss(); //set the containers css and classes after a refresh if it's the case
            }
        }
    }

    private restoreCss() {
        if ('attributes' in this.svyContainerStyle && this.svyContainerStyle.attributes.name.indexOf('.') > 0) {
            const name = this.svyContainerStyle.attributes.name.split('.')[1];
            if (this.parent.cssstyles && this.parent.cssstyles[name]) {
                const stylesMap = this.parent.cssstyles[name];
                for (let k in stylesMap) {
                    this.renderer.setStyle(this.el.nativeElement, k, stylesMap[k]);
                }
            }
            if (this.parent.containers) {
                if (this.parent.containers.added && this.parent.containers.added[name]) {
                    this.parent.containers.added[name].forEach((cls: string) => this.renderer.addClass(this.el.nativeElement, cls));
                }
                if (this.parent.containers.removed && this.parent.containers.removed[name]) {
                    this.parent.containers.removed[name].forEach((cls: string) => this.renderer.removeClass(this.el.nativeElement, cls));
                }
            }
        }
    }
}