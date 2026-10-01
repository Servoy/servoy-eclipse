import { Routes } from '@angular/router';
import { ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceFormTemplateImpl } from '../formtemplate/servoy_public_formtemplate_impl.service';

export const APP_ROUTES: Routes = [
  {
    path: 'designer/solution/:solutionname/form/:formname/clientnr/:clientnr',
    loadChildren: () => import('../designer/servoydesigner.module').then((m) => m.ServoyDesignerModule),
  },
  {
    path: 'formtemplate/:formname',
    // SVY-21460: provide the stateless ServoyPublicService at the ROUTE level (above the whole
    // svy-formtemplate component tree), like the designer does on its module. Providing it on the
    // route-root component instead puts it below components that inject FormattingService ->
    // ServoyPublicService, which fails with NG0201 and makes the root's own injection circular (NG0200).
    providers: [ServoyPublicServiceFormTemplateImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceFormTemplateImpl }],
    loadComponent: () => import('../formtemplate/formtemplate.component').then((m) => m.ServoyFormTemplateComponent),
  },
  {
    path: '**',
    loadChildren: () => import('../ngclient/servoy.module').then((m) => m.ServoyModule),
  },
];
