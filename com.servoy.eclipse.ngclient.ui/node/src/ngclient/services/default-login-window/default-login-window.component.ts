import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LocalStorageService } from '@servoy/public';
import { SabloService } from '../../../sablo/sablo.service';

@Component({
    templateUrl: './default-login-window.component.html',
    styleUrls: ['./default-login-window.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true,
    imports: [CommonModule, FormsModule]
})
export class DefaultLoginWindowComponent {

    username!: string;
    password!: string;
    remember = true;
    message!: string;
    onLoginCallback!: () => void;

    private readonly sabloService = inject(SabloService);
    private readonly localStorageService = inject(LocalStorageService);
    
    constructor() { }

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
    
    public setOnLoginCallback(callback: any){
        this.onLoginCallback = callback;
    }
}
