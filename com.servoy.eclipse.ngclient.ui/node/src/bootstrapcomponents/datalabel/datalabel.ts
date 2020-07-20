import { Component, Input, AfterViewInit, Renderer2, Pipe, PipeTransform } from "@angular/core";
import { ServoyBootstrapBaseLabel } from "../bts_baselabel";

@Component({
    selector: 'servoybootstrap-datalabel',
    templateUrl: './datalabel.html',
    styleUrls: ['./datalabel.scss'] 
})
export class ServoyBootstrapDatalabel extends ServoyBootstrapBaseLabel {

    @Input() dataProviderID;
    @Input() styleClassExpression;
    @Input() valuelistID;
    @Input() format;

    constructor(renderer: Renderer2) {
        super(renderer);
    }
}

@Pipe( { name: 'designFilter'} )
export class DesignFilterPipe implements PipeTransform {
    transform(input: any, inDesigner: boolean) {
        if (inDesigner)
        {
            return "DataLabel"
        }
        return input;
    }

}