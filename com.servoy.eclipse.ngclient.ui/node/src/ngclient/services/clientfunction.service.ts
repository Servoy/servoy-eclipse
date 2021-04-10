import { DOCUMENT } from '@angular/common';
import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';
import { Deferred, IDeferred } from 'servoy-public';

@Injectable()
export class ClientFunctionService {

    private renderer: Renderer2;

    private script: HTMLScriptElement;
    private deferred: IDeferred<void>;
    private doc: Document;

    constructor(private sabloService: SabloService, rendererFactory: RendererFactory2,  @Inject(DOCUMENT) _doc: any) {
        this.renderer = rendererFactory.createRenderer(null, null);
        this.doc = _doc;
    }

    public reloadClientFunctions() {
        if (this.script) {
            this.script.remove();
        }
        this.script = this.doc.createElement('script');
        this.script.type = 'text/javascript';
        this.script.src = '/clientfunctions.js?clientnr='  +  this.sabloService.getClientnr() + '&stamp=' + new Date().getTime();
        // only create a defered when there is not one yet. if there was already one just reuse that one (could be waited already)
        // because we removed the script above, that one should not really resolve it now anymore.
        if (!this.deferred) this.deferred = new Deferred();
        this.script.onload = () => {
         this.deferred.resolve();
         this.deferred = null;
        };
        this.renderer.appendChild(this.doc.body, this.script);
    }

    public waitForLoading(): Promise<void> {
        if (this.deferred)  return this.deferred.promise;
        return Promise.resolve();
    }
}
