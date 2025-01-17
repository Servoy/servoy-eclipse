import { Component } from '@angular/core';
import { LocalStorageService } from '@servoy/public';
import { SabloService } from '../../../sablo/sablo.service';

@Component({
    templateUrl: './default-login-window.component.html',
    styleUrls: ['./default-login-window.component.css'],
    standalone: false
})
export class DefaultLoginWindowComponent {

    username: string;
    password: string;
    remember = true;
    message: string;
    onLoginCallback : () => void;
    
    constructor(
        private sabloService: SabloService,
        private localStorageService: LocalStorageService
    ) { }

    doLogin() {
        const promise = this.sabloService.callService<{ username: string; password: string }>('applicationServerService', 'login',
            { username: this.username, password: this.password, remember: this.remember }, false);
        promise.then((ok) => {
            if (ok) {
                if (ok.username) this.localStorageService.set('servoy_username', ok.username);
                if (ok.password) this.localStorageService.set('servoy_password', ok.password);
                if (this.onLoginCallback) this.onLoginCallback();
            } else {
                this.message = 'Invalid username or password, try again';
            }
        });
    }
    
    public setOnLoginCallback(callback){
        this.onLoginCallback = callback;
    }
}
