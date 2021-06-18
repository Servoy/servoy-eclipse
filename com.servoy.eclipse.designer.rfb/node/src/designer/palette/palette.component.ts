import { Component } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';
import { HttpClient } from '@angular/common/http';
import { URLParserService } from '../services/urlparser.service';

@Component({
    selector: 'designer-palette',
    templateUrl: './palette.component.html',
    styleUrls: ['./palette.component.css']
})
export class PaletteComponent {

    public searchText: string;
    public packages: any;
    public activeIds : Array<string>;
    
    constructor(protected readonly editorSession: EditorSessionService, private http: HttpClient, private urlParser: URLParserService) {
        let layoutType: string;
        if (urlParser.isAbsoluteFormLayout())
            layoutType = "Absolute-Layout";
        else
            layoutType = "Responsive-Layout";
        this.activeIds = new Array();
        this.http.get('/designer/palette?layout=' + layoutType + '&formName=' + this.urlParser.getFormName()).subscribe((got : Array<any>) => {
            let propertyValues;
            if (got[got.length - 1] && got[got.length - 1]['propertyValues']) {
                propertyValues = got[got.length - 1]['propertyValues'];
                this.packages = got.slice(0, got.length - 1);
            }
            else {
                this.packages = got;
            }
            for (let i = 0; i < this.packages.length; i++) {
                this.packages[i].id = this.packages[i].packageName.trim();
                this.activeIds.push(this.packages[i].id);
                if (this.packages[i].components) {
                    for (let j = 0; j < this.packages[i].components.length; j++) {
                        if (propertyValues && this.packages[i].components[j].properties) {
                            this.packages[i].components[j].isOpen = false;
                            //we still need to have the components with properties on the component for filtering

                            if (propertyValues && propertyValues.length && this.packages[i].components[j].name == "servoycore-formcomponent") {
                                var newPropertyValues = [];
                                for (var n = 0; n < propertyValues.length; n++) {
                                    if (!propertyValues[n]["isAbsoluteCSSPositionMix"]) {
                                        newPropertyValues.push(propertyValues[n]);
                                    }
                                }
                                this.packages[i].components[j].components = newPropertyValues;
                            }
                            else {
                                this.packages[i].components[j].components = propertyValues;
                            }
                        }
                    }
                }
            }
        });
    }


    openPackageManager() {
        this.editorSession.openPackageManager();
    }
}
