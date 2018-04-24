import { Injectable } from '@angular/core';

function getWindow (): Window {
    return window;
}

@Injectable()
export class WindowRefService {
    get nativeWindow (): Window {
        return getWindow();
    }
}