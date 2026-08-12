import { inject, Injectable, Renderer2, RendererFactory2, DOCUMENT } from '@angular/core';
import { SabloService } from './sablo.service';
import { Deferred, IDeferred } from '@servoy/public';

@Injectable({
  providedIn: 'root',
})
export class ClientFunctionService {
  private readonly renderer: Renderer2;

  private script!: HTMLScriptElement;
  private deferred!: IDeferred<void>;
  private readonly doc: Document;
  private readonly sabloService = inject(SabloService);

  constructor() {
    const rendererFactory = inject(RendererFactory2);
    this.renderer = rendererFactory.createRenderer(null, null);
    this.doc = inject(DOCUMENT) as Document;
  }

  public reloadClientFunctions() {
    if (this.script) {
      this.script.remove();
    }
    let context = this.doc.getElementsByTagName('base')[0].getAttribute('href');
    if (!context!.endsWith('/')) context += '/';
    this.script = this.doc.createElement('script');
    this.script.type = 'text/javascript';
    this.script.src = context + 'clientfunctions.js?clientnr=' + this.sabloService.getClientnr() + '&stamp=' + new Date().getTime();
    // only create a defered when there is not one yet. if there was already one just reuse that one (could be waited already)
    // because we removed the script above, that one should not really resolve it now anymore.
    // seems is possible to have multiple onload called, just have a null check
    if (!this.deferred) this.deferred = new Deferred();
    this.script.onload = () => {
      if (this.deferred) {
        this.deferred.resolve();
      }
      this.deferred = null!;
    };
    this.renderer.appendChild(this.doc.body, this.script);
  }

  public waitForLoading(): IPromiseLike {
    if (this.deferred) return this.deferred.promise;
    return new PromiseLike();
  }
}

export interface IPromiseLike {
  finally(fun: () => void): void;
}

class PromiseLike implements IPromiseLike {
  finally(fun: () => void): void {
    fun();
  }
}
