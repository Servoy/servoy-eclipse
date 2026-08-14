import { DefaultNavigator } from './default-navigator/default-navigator';
import { ErrorBean } from './error-bean/error-bean';
import { ServoyCoreSlider } from './slider/slider';
import { SessionView } from './session-view/session-view';
import { ServoyCoreFormContainer } from './formcontainer/formcontainer';
import { AddAttributeDirective } from './addattribute.directive';
import { ServoyCoreFormcomponentResponsiveCotainer } from './formcomponent-responsive-container/formcomponent-responsive-container';

export const SERVOYCORE_COMPONENTS = [DefaultNavigator, SessionView, ErrorBean, ServoyCoreSlider, ServoyCoreFormContainer, AddAttributeDirective, ServoyCoreFormcomponentResponsiveCotainer] as const;
