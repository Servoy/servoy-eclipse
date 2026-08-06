import { enableProdMode, importProvidersFrom } from '@angular/core';

import { environment } from './environments/environment';
import { WebsocketService } from './wpm/websocket.service';
import { WpmService } from './wpm/wpm.service';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { MainComponent } from './wpm/main.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(MainComponent, {
    providers: [
        importProvidersFrom(
            BrowserModule, BrowserAnimationsModule,
            MatButtonModule, MatTabsModule, MatSelectModule, MatOptionModule,
            MatIconModule, MatTooltipModule, MatCardModule, MatProgressBarModule,
            MatDialogModule, MatInputModule, MatCheckboxModule, FormsModule
        ),
        WebsocketService, WpmService
    ]
})
  .catch(err => console.error(err));
