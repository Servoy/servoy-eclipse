import { Injectable, ViewContainerRef } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class MainViewRefService {
    private _mainRef: ViewContainerRef;

    get mainContainer(): ViewContainerRef {
        return this._mainRef;
    }

    set mainContainer(ref: ViewContainerRef) {
        this._mainRef = ref;
    }

}
