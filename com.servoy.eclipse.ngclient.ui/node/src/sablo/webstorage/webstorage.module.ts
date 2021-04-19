import { NgModule } from '@angular/core';
import { SessionStorageService } from './sessionstorage.service';
import { LocalStorageService } from './localstorage.service';

@NgModule( {
    providers: [
        LocalStorageService,
        SessionStorageService]
} )

export class WebStorageModule { }
