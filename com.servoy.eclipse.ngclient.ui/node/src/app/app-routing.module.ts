import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';


const routes: Routes = [
  {
    path: 'designer/solution/:solutionname/form/:formname/clientnr/:clientnr',
    loadChildren: () => import('../designer/servoydesigner.module').then(m => m.ServoyDesignerModule)
  },
  {
    path: 'formtemplate/:formname',
    loadChildren: () => import('../formtemplate/formtemplate.module').then(m => m.FormTemplateModule)
  },
  {
    path: '**',
    loadChildren: () => import('../ngclient/servoy.module').then(m => m.ServoyModule)
  }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes)
  ],
  exports: [RouterModule],
  providers: []
})
export class AppRoutingModule { }