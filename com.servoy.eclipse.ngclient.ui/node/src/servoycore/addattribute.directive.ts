import { Directive, ElementRef, OnChanges, SimpleChanges, Renderer2, Injector, input } from '@angular/core';
import { DesignFormComponent } from '../designer/designform_component.component';
import { AbstractFormComponent, FormComponent } from '../ngclient/form/form_component.component';
import { StructureCache } from '../ngclient/types';

@Directive({
    selector: '[svyContainerStyle]',
    standalone: false
})
export class AddAttributeDirective implements OnChanges {
    readonly svyContainerStyle = input<any>(undefined);
    readonly svyContainerLayout = input(undefined);
    readonly svyContainerClasses = input<Array<string>>(undefined);
    readonly svyContainerAttributes = input(undefined);

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
        if (changes.svyContainerClasses) {
            if (changes.svyContainerClasses.previousValue) {
                changes.svyContainerClasses.previousValue.forEach(cls => this.renderer.removeClass(this.el.nativeElement, cls));
            }
            const svyContainerClasses = this.svyContainerClasses();
            if (svyContainerClasses) {
                svyContainerClasses.forEach(cls => this.renderer.addClass(this.el.nativeElement, cls));
            }
        }

        if (changes.svyContainerLayout) {
            if (changes.svyContainerLayout.previousValue) {
                for (const key of Object.keys(changes.svyContainerLayout.previousValue)) {
                    this.renderer.removeStyle(this.el.nativeElement, key);
                }
            }
            if (changes.svyContainerLayout.currentValue) {
                for (const key of Object.keys(changes.svyContainerLayout.currentValue)) {
                    this.renderer.setStyle(this.el.nativeElement, key, changes.svyContainerLayout.currentValue[key]);
                }
            }
        }
        const svyContainerAttributes = this.svyContainerAttributes();
        if (changes.svyContainerAttributes && svyContainerAttributes) {
            for (const key of Object.keys(svyContainerAttributes)) {
                this.renderer.setAttribute(this.el.nativeElement, key, svyContainerAttributes[key]);
                if (key === 'name' && this.svyContainerStyle() instanceof StructureCache) this.restoreCss(); //set the containers css and classes after a refresh if it's the case
            }
        }
        const svyContainerStyle = this.svyContainerStyle();
        if (changes.svyContainerStyle && svyContainerStyle && svyContainerStyle.cssPositionContainer) {
            this.renderer.setStyle(this.el.nativeElement, 'position', 'relative');
            this.renderer.setStyle(this.el.nativeElement, 'height', svyContainerAttributes.size.height + 'px');
        }
    }

    private restoreCss() {
        const svyContainerStyle = this.svyContainerStyle();
        if ('attributes' in svyContainerStyle && svyContainerStyle.attributes.name.indexOf('.') > 0) {
            const name = svyContainerStyle.attributes.name.split('.')[1];
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