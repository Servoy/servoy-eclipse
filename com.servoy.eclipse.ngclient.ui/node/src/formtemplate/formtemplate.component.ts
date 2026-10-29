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
      <svy-formtemplate [name]="formName" [sampleData]="sampleData"></svy-formtemplate>
    }`,
    standalone: false
})
export class ServoyFormTemplateComponent implements OnInit {

    formName: string;

    /**
     * SVY-21610: whether the injected form state carries synthetic sample values. Set from the
     * window.formtemplateSampleData marker the Java endpoint injects; defaults to false when the marker is absent.
     */
    sampleData = false;

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

        this.sampleData = (this.windowRef.nativeWindow as any).formtemplateSampleData === true;

        const formStateContainer = this.readJsonBlob('svy-formtemplate-formstate');
        this.formName = this.getFormName();
        if (formStateContainer && this.formName) {
            // SVY-21610: the blob carries the requested form AND every form nested in it through a form-typed property
            // (tabpanel tabs, tablesspanel, accordion, splitpane, formcontainer). Build a cache for each: the generated
            // nested-form template is guarded by isFormAvailable(name) -> FormService.hasFormCacheEntry(name), so a form
            // without a cache entry renders as an empty tab body. Only the requested form is rendered at the top level;
            // the others are picked up from the cache by whichever component contains them.
            const formState = formStateContainer[this.formName];
            if (formState) {
                // the requested form first, so it is the one a nested-form lookup can already resolve against
                this.formService.createFormCache(this.formName, formState, null);
            }
            for (const nestedFormName of Object.keys(formStateContainer)) {
                if (nestedFormName === this.formName) continue;
                this.formService.createFormCache(nestedFormName, formStateContainer[nestedFormName], null);
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
