import { Injectable } from '@angular/core';

const getWindow = () => {
    return window;
}

@Injectable({
  providedIn: 'root'
})
export class WindowRefService {
    get nativeWindow(): Window {
        return getWindow();
    }
}
