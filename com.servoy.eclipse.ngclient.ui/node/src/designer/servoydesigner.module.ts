import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServoyDesignerRoutingModule } from './servoydesigner-routing.module';
import { ServoyDesignerComponent } from './servoydesigner.component';

@NgModule({
  imports: [
    CommonModule,
    ServoyDesignerRoutingModule
  ],
  declarations: [ServoyDesignerComponent]
})
export class ServoyDesignerModule { }
