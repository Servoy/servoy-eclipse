import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgModule, provideZoneChangeDetection } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

@NgModule({ declarations: [
        AppComponent
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        AppRoutingModule], providers: [provideHttpClient(withInterceptorsFromDi()),provideZoneChangeDetection({ eventCoalescing: true, runCoalescing: true })] })
export class AppModule { }