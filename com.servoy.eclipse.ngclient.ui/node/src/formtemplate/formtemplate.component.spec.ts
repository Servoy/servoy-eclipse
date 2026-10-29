import { TestBed, waitForAsync } from '@angular/core/testing';
import { Renderer2, RendererFactory2 } from '@angular/core';

import { WindowRefService } from '@servoy/public';

import { ServoyFormTemplateComponent } from './formtemplate.component';
import { FormService } from '../ngclient/form.service';
import { TypesRegistry } from '../sablo/types_registry';

/**
 * Unit tests for the stateless form-template route root (SVY-21460).
 *
 * The single most important regression these tests guard is the ORDER of two calls in ngOnInit:
 * the component MUST register the injected component client-side specs into TypesRegistry BEFORE
 * it builds the form cache via FormService.createFormCache. If that order is ever inverted (or the
 * spec registration is dropped), typed-property conversion silently breaks because createFormCache
 * looks specs up from an empty registry (see spec section 2.3 / 5).
 */
describe('ServoyFormTemplateComponent (route root)', () => {

    const SPECS_ID = 'svy-formtemplate-specs';
    const FORMSTATE_ID = 'svy-formtemplate-formstate';

    const SPECS_BLOB: any = { myComp: { p: { someProp: { s: 2 } } } };
    const FORM_STATE = { responsive: false, size: { width: 100, height: 100 }, children: [] };

    let typesRegistry: jasmine.SpyObj<TypesRegistry>;
    let formService: jasmine.SpyObj<FormService>;
    let windowRef: jasmine.SpyObj<WindowRefService>;
    let renderer: jasmine.SpyObj<Renderer2>;
    // shared recorder so we can assert the RELATIVE order of the two calls
    let callOrder: string[];

    const addBlob = (id: string, json: unknown): HTMLElement => {
        const el = document.createElement('script');
        el.id = id;
        el.setAttribute('type', 'application/json');
        el.textContent = json === undefined ? '' : (typeof json === 'string' ? json : JSON.stringify(json));
        document.body.appendChild(el);
        return el;
    };

    const removeBlobs = () => {
        document.getElementById(SPECS_ID)?.remove();
        document.getElementById(FORMSTATE_ID)?.remove();
    };

    const createComponent = (): ServoyFormTemplateComponent =>
        new ServoyFormTemplateComponent(windowRef, formService, typesRegistry, renderer, document);

    beforeEach(waitForAsync(() => {
        callOrder = [];

        typesRegistry = jasmine.createSpyObj<TypesRegistry>('TypesRegistry', ['addComponentClientSideSpecs']);
        typesRegistry.addComponentClientSideSpecs.and.callFake(() => { callOrder.push('addComponentClientSideSpecs'); });

        formService = jasmine.createSpyObj<FormService>('FormService', ['createFormCache']);
        formService.createFormCache.and.callFake(() => { callOrder.push('createFormCache'); });

        // default: no injected marker, and a path that does not match /formtemplate/ so parsing must be exercised explicitly per test
        windowRef = { get nativeWindow() { return { location: { pathname: '/' } } as unknown as Window; } } as jasmine.SpyObj<WindowRefService>;

        renderer = jasmine.createSpyObj<Renderer2>('Renderer2', ['setStyle']);

        TestBed.configureTestingModule({
            providers: [
                { provide: TypesRegistry, useValue: typesRegistry },
                { provide: FormService, useValue: formService },
                { provide: WindowRefService, useValue: windowRef },
                { provide: Renderer2, useValue: renderer },
                { provide: RendererFactory2, useValue: { createRenderer: () => renderer } }
            ]
        });
    }));

    afterEach(() => {
        removeBlobs();
    });

    const setWindow = (win: Partial<{ formtemplateName: string; location: { pathname: string } }>) => {
        Object.defineProperty(windowRef, 'nativeWindow', {
            get: () => ({ location: { pathname: '/' }, ...win }) as unknown as Window,
            configurable: true
        });
    };

    describe('spec registration vs cache-build ordering', () => {

        it('registers the injected component specs BEFORE building the form cache', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            // both must have happened, and specs registration must come first
            expect(typesRegistry.addComponentClientSideSpecs).toHaveBeenCalledTimes(1);
            expect(formService.createFormCache).toHaveBeenCalledTimes(1);
            expect(callOrder).toEqual(['addComponentClientSideSpecs', 'createFormCache']);
        });

        it('passes the exact parsed specs blob to the registry', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            expect(typesRegistry.addComponentClientSideSpecs).toHaveBeenCalledWith(SPECS_BLOB);
        });

        it('builds the cache with the form-state entry that matches the resolved form name, url null', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE, otherForm: { junk: true } });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            expect(formService.createFormCache).toHaveBeenCalledWith('aForm', FORM_STATE, null);
        });
    });

    describe('form-name resolution', () => {

        it('prefers the injected formtemplateName marker over the path', () => {
            addBlob(FORMSTATE_ID, { markerForm: FORM_STATE });
            setWindow({ formtemplateName: 'markerForm', location: { pathname: '/formtemplate/pathForm.html' } });

            const comp = createComponent();
            comp.ngOnInit();

            expect(comp.formName).toBe('markerForm');
            expect(formService.createFormCache).toHaveBeenCalledWith('markerForm', FORM_STATE, null);
        });

        it('ignores an unresolved ${formtemplateName} placeholder and falls back to the path', () => {
            addBlob(FORMSTATE_ID, { pathForm: FORM_STATE });
            // eslint-disable-next-line no-template-curly-in-string
            setWindow({ formtemplateName: '${formtemplateName}', location: { pathname: '/formtemplate/pathForm.html' } });

            const comp = createComponent();
            comp.ngOnInit();

            expect(comp.formName).toBe('pathForm');
        });

        it('strips the .html suffix when parsing the form name from the path', () => {
            setWindow({ location: { pathname: '/formtemplate/myForm.html' } });

            const comp = createComponent();
            comp.ngOnInit();

            expect(comp.formName).toBe('myForm');
        });

        it('parses a path without the .html suffix', () => {
            setWindow({ location: { pathname: '/formtemplate/myForm' } });

            const comp = createComponent();
            comp.ngOnInit();

            expect(comp.formName).toBe('myForm');
        });

        it('url-decodes an encoded form name from the path', () => {
            setWindow({ location: { pathname: '/formtemplate/my%20form.html' } });

            const comp = createComponent();
            comp.ngOnInit();

            expect(comp.formName).toBe('my form');
        });

        it('leaves formName undefined when the path is not a formtemplate path', () => {
            setWindow({ location: { pathname: '/something/else' } });

            const comp = createComponent();
            comp.ngOnInit();

            expect(comp.formName).toBeNull();
            expect(formService.createFormCache).not.toHaveBeenCalled();
        });
    });

    describe('robustness of the injected blobs', () => {

        it('does not build a cache when there is no form-state blob at all', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            // specs may still register, but with no form-state there is nothing to build
            expect(formService.createFormCache).not.toHaveBeenCalled();
        });

        it('does not build a cache for the resolved form when the form-state has no entry for it', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { anotherForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            // 'aForm' itself has no entry, so it gets no cache; 'anotherForm' is still cached because every entry in the
            // blob is a form the server generated for this page (SVY-21610 nested forms)
            expect(formService.createFormCache).not.toHaveBeenCalledWith('aForm', jasmine.anything(), null);
            expect(formService.createFormCache).toHaveBeenCalledWith('anotherForm', FORM_STATE, null);
        });

        it('does not register specs when the specs blob is missing', () => {
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            expect(typesRegistry.addComponentClientSideSpecs).not.toHaveBeenCalled();
            // but the cache should still be built from the form-state
            expect(formService.createFormCache).toHaveBeenCalledWith('aForm', FORM_STATE, null);
        });

        it('tolerates a malformed specs JSON blob without throwing and still builds the cache', () => {
            addBlob(SPECS_ID, '{ this is not json');
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            const comp = createComponent();
            expect(() => comp.ngOnInit()).not.toThrow();
            expect(typesRegistry.addComponentClientSideSpecs).not.toHaveBeenCalled();
            expect(formService.createFormCache).toHaveBeenCalledWith('aForm', FORM_STATE, null);
        });

        it('tolerates a malformed form-state JSON blob without throwing and without building a cache', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, 'not-json-at-all');
            setWindow({ formtemplateName: 'aForm' });

            const comp = createComponent();
            expect(() => comp.ngOnInit()).not.toThrow();
            expect(formService.createFormCache).not.toHaveBeenCalled();
        });

        it('treats an empty/whitespace blob as absent', () => {
            addBlob(SPECS_ID, '   ');
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            expect(typesRegistry.addComponentClientSideSpecs).not.toHaveBeenCalled();
            expect(formService.createFormCache).toHaveBeenCalledTimes(1);
        });
    });

    // SVY-21610: a form shown inside another form (a tabpanel tab, tablesspanel, accordion, splitpane, formcontainer)
    // is rendered by the generated template only when FormService.hasFormCacheEntry(name) is true - the template guards
    // it with isFormAvailable(name). So every form the server put in the blob needs a cache entry here, or the tab body
    // renders empty even though its state was delivered.
    describe('nested forms', () => {
        const NESTED_STATE = { responsive: false, size: { width: 100, height: 100 }, children: [{ name: '', model: {} }] };

        it('builds a cache for every form in the blob, not only the requested one', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE, categories: NESTED_STATE, details: NESTED_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            expect(formService.createFormCache).toHaveBeenCalledTimes(3);
            expect(formService.createFormCache).toHaveBeenCalledWith('aForm', FORM_STATE, null);
            expect(formService.createFormCache).toHaveBeenCalledWith('categories', NESTED_STATE, null);
            expect(formService.createFormCache).toHaveBeenCalledWith('details', NESTED_STATE, null);
        });

        it('builds the requested form first, so a nested lookup can already resolve it', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            // the requested form deliberately NOT first in the blob's key order
            addBlob(FORMSTATE_ID, { categories: NESTED_STATE, aForm: FORM_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            const firstCachedForm = formService.createFormCache.calls.argsFor(0)[0];
            expect(firstCachedForm).toBe('aForm');
        });

        it('caches each form once even when the requested form is also listed again', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE, categories: NESTED_STATE });
            setWindow({ formtemplateName: 'aForm' });

            createComponent().ngOnInit();

            const cachedForms = formService.createFormCache.calls.allArgs().map((c: unknown[]) => c[0]);
            expect(cachedForms).toEqual(['aForm', 'categories']);
        });

        it('still renders only the requested form at the top level', () => {
            addBlob(SPECS_ID, SPECS_BLOB);
            addBlob(FORMSTATE_ID, { aForm: FORM_STATE, categories: NESTED_STATE });
            setWindow({ formtemplateName: 'aForm' });

            const comp = createComponent();
            comp.ngOnInit();

            // the nested forms are only in the cache; the template binds svy-formtemplate to this one name
            expect(comp.formName).toBe('aForm');
        });
    });
});
