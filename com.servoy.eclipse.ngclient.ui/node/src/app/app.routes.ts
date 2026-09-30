import { Routes } from '@angular/router';

export const APP_ROUTES: Routes = [
  {
    path: 'designer/solution/:solutionname/form/:formname/clientnr/:clientnr',
    loadChildren: () => import('../designer/servoydesigner.module').then((m) => m.ServoyDesignerModule),
  },
  {
    path: 'formtemplate/:formname',
    loadComponent: () => import('../formtemplate/formtemplate.component').then((m) => m.ServoyFormTemplateComponent),
  },
  {
    path: '**',
    loadChildren: () => import('../ngclient/servoy.module').then((m) => m.ServoyModule),
  },
];
