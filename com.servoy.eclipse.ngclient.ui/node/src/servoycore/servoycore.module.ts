import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DefaultNavigator } from './default-navigator/default-navigator';
import { ErrorBean } from './error-bean/error-bean';
import { ServoyCoreSlider } from './slider/slider';
import { SessionView } from './session-view/session-view';
import { ServoyCoreFormContainer } from './formcontainer/formcontainer';
import {AddAttributeDirective} from './addattribute.directive';
import { ListFormComponent } from './listformcomponent/listformcomponent';
import { ServoyPublicModule } from '@servoy/public';

@NgModule( {
    declarations: [
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        ListFormComponent,
        AddAttributeDirective
    ],
    imports: [CommonModule,
        FormsModule,
        ServoyPublicModule
    ],
    providers: [],
    bootstrap: [],
    exports: [
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        ListFormComponent,
        AddAttributeDirective
    ]
} )
export class ServoyCoreComponentsModule { }
