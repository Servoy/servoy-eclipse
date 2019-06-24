import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { MainComponent } from './main.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatOptionModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
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
