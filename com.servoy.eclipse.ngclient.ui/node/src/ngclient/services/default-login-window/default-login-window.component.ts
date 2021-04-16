import { Component } from '@angular/core';
import { LocalStorageService } from '../../../sablo/webstorage/localstorage.service';
import { SabloService } from '../../../sablo/sablo.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  templateUrl: './default-login-window.component.html',
  styleUrls: ['./default-login-window.component.css']
})
export class DefaultLoginWindowComponent {

  username: string;
  password: string;
  remember = true;
  message: string;

  constructor(
    private sabloService: SabloService,
    private activeModal: NgbActiveModal,
    private localStorageService: LocalStorageService
  ) { }

  doLogin() {
    const promise = this.sabloService.callService<{username: string;password: string}>('applicationServerService', 'login',
      {username : this.username, password : this.password, remember: this.remember }, false);
    promise.then((ok) =>{
			if(ok) {
        if(ok.username) this.localStorageService.set('servoy_username', ok.username);
        if(ok.password) this.localStorageService.set('servoy_password', ok.password);
        this.activeModal.close(ok);
			} else {
				this.message = 'Invalid username or password, try again';
			}
    });
  }
}
