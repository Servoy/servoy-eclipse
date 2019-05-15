import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { MainComponent } from './main.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
  MatButtonModule,
  MatTabsModule,
  MatSelectModule,
  MatOptionModule,
  MatIconModule,
  MatTooltipModule,
  MatCardModule,
  MatProgressBarModule,
  MatDialogModule,
  MatInputModule } from '@angular/material';
import { HeaderComponent, AddRepositoryDialog, ErrorDialog } from './header/header.component';
import { ContentComponent } from './content/content.component';
import { PackagesComponent } from './packages/packages.component'
import { WebsocketService } from './websocket.service';
import { WpmService } from './wpm.service';
import { FormsModule } from '@angular/forms'

@NgModule({
  declarations: [
    MainComponent,
    HeaderComponent,
    ContentComponent,
    PackagesComponent,
    AddRepositoryDialog,
    ErrorDialog
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
    MatProgressBarModule,
    MatDialogModule,
    MatInputModule,
    FormsModule
  ],
  providers: [WebsocketService, WpmService],
  bootstrap: [MainComponent],
  entryComponents: [AddRepositoryDialog, ErrorDialog]
})
export class WpmModule { }
