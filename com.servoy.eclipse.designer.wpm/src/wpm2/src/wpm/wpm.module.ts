import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { MainComponent } from './main.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MatButtonModule, MatTabsModule, MatSelectModule, MatOptionModule, MatIconModule, MatTooltipModule, MatCardModule, MatProgressBarModule } from '@angular/material';
import { HeaderComponent } from './header/header.component';
import { ContentComponent } from './content/content.component';
import { PackagesComponent } from './packages/packages.component'
import { WebsocketService } from './websocket.service';
import { WpmService } from './wpm.service';

@NgModule({
  declarations: [
    MainComponent,
    HeaderComponent,
    ContentComponent,
    PackagesComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    MatButtonModule,
    MatTabsModule,
    MatSelectModule,
    MatOptionModule,
    MatIconModule,
    MatTooltipModule,
    MatCardModule,
    MatProgressBarModule
  ],
  providers: [WebsocketService, WpmService],
  bootstrap: [MainComponent]
})
export class WpmModule { }
