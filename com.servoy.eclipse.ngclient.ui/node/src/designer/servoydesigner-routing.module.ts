import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { ServoyDesignerComponent } from './servoydesigner.component';


const routes: Routes = [
  {
    path: '',
    component: ServoyDesignerComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ServoyDesignerRoutingModule { }