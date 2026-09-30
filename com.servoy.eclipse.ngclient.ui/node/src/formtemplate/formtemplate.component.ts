import { Component, OnInit, Inject, Renderer2, DOCUMENT } from '@angular/core';
import { WindowRefService } from '@servoy/public';
import { FormService } from '../ngclient/form.service';
import { TypesRegistry } from '../sablo/types_registry';

/**
 * Route root for the stateless form-template render route (SVY-21460).
 *
 * Reads the form-state JSON and the component client-side specs that the Java
 * endpoint injected into the page as inline <script type="application/json"> blobs,
 * registers the specs into the TypesRegistry itself (there is no server handshake),
 * builds the FormCache locally and renders <svy-formtemplate>. No websocket, no session.
 */
@Component({
    selector: 'servoy-formtemplate',
    template: `@if (formName) {
      <svy-formtemplate [name]="formName"></svy-formtemplate>
    }`,
    standalone: false
})
export class ServoyFormTemplateComponent implements OnInit {

    formName: string;

    constructor(private windowRef: WindowRefService,
        private formService: FormService,
        private typesRegistry: TypesRegistry,
        protected renderer: Renderer2,
        @Inject(DOCUMENT) private doc: Document) { }

    ngOnInit() {
        this.renderer.setStyle(this.doc.body, 'overflow', 'hidden');

        // register the component client-side specs this form needs BEFORE building the cache,
        // so typed properties can be converted (there is no server handshake to fill the registry)
        const specs = this.readJsonBlob('svy-formtemplate-specs');
        if (specs) {
            this.typesRegistry.addComponentClientSideSpecs(specs);
        }

        const formStateContainer = this.readJsonBlob('svy-formtemplate-formstate');
        this.formName = this.getFormName();
        if (formStateContainer && this.formName) {
            const formState = formStateContainer[this.formName];
            if (formState) {
                this.formService.createFormCache(this.formName, formState, null);
            }
        }
    }

    private getFormName(): string {
        // the Java endpoint injects a marker via IndexPageEnhancer
        const injected = (this.windowRef.nativeWindow as any).formtemplateName;
        if (injected && injected !== '${formtemplateName}') {
            return injected;
        }
        // fall back to parsing /formtemplate/<formname>.html or /formtemplate/<formname>
        const path: string = this.windowRef.nativeWindow.location.pathname;
        const marker = '/formtemplate/';
        const start = path.indexOf(marker);
        if (start < 0) return null;
        let name = path.substring(start + marker.length);
        if (name.endsWith('.html')) name = name.substring(0, name.length - '.html'.length);
        return name.length > 0 ? decodeURIComponent(name) : null;
    }

    private readJsonBlob(id: string): any {
        const el = this.doc.getElementById(id);
        if (!el || !el.textContent || el.textContent.trim().length === 0) return null;
        try {
            return JSON.parse(el.textContent);
        } catch (e) {
            return null;
        }
    }
}
