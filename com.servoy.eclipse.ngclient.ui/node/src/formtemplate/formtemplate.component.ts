import { Component, OnInit, Renderer2, DOCUMENT, inject } from '@angular/core';
import { WindowRefService, ServoyPublicService } from '@servoy/public';
import { FormService } from '../ngclient/form.service';
import { TypesRegistry } from '../sablo/types_registry';
import { FormTemplateComponent } from './formtemplate_component.component';
import { ServoyPublicServiceFormTemplateImpl } from './servoy_public_formtemplate_impl.service';

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
  standalone: true,
  imports: [FormTemplateComponent],
  template: `@if (formName) {
    <svy-formtemplate [name]="formName"></svy-formtemplate>
  }`,
  providers: [ServoyPublicServiceFormTemplateImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceFormTemplateImpl }],
})
export class ServoyFormTemplateComponent implements OnInit {
  formName: string | null = null;

  private windowRef = inject(WindowRefService);
  private formService = inject(FormService);
  private typesRegistry = inject(TypesRegistry);
  protected renderer = inject(Renderer2);
  private doc = inject(DOCUMENT) as Document;

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
        this.formService.createFormCache(this.formName, formState, null!);
      }
    }
  }

  private getFormName(): string | null {
    // the Java endpoint injects a marker via IndexPageEnhancer
    const injected = (this.windowRef.nativeWindow as any).formtemplateName;
    const unresolvedMarker = '${' + 'formtemplateName}';
    if (injected && injected !== unresolvedMarker) {
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
    } catch {
      return null;
    }
  }
}
