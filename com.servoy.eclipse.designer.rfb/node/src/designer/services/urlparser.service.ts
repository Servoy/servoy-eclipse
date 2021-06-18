import { Injectable } from '@angular/core';
import { WebsocketService } from '@servoy/sablo';

@Injectable()
export class URLParserService {

    formName: string;
    solutionName: string;
    layout: string;
    showingInContainer: string;
    hideDefault: string;
    marqueeSelectOuter: string;
    formWidth: number;
    formHeight: number;
    formComponent: string;

    constructor(private websocketService: WebsocketService) {
        this.parseURL();
    }

    parseURL() {
        this.formName = this.websocketService.getURLParameter('f');
        this.solutionName = this.websocketService.getURLParameter('s');
        this.layout = this.websocketService.getURLParameter('l');
        this.hideDefault = this.websocketService.getURLParameter("hd");
        this.marqueeSelectOuter = this.websocketService.getURLParameter("mso");
        this.formWidth = parseInt(this.websocketService.getURLParameter("w"), 10);
        this.formHeight = parseInt(this.websocketService.getURLParameter("h"), 10);
        this.formComponent = this.websocketService.getURLParameter("fc");
        this.showingInContainer = this.websocketService.getURLParameter("cont");
    }

    public getFormName() {
        return this.formName;
    }

    public getSolutionName() {
        return this.solutionName;
    }

    public isAbsoluteFormLayout() {
        return (this.layout == "absolute" || this.layout == "csspos");
    }

    public isCSSPositionFormLayout() {
        return this.layout == "csspos";
    }

    public isShowingContainer() {
        return this.showingInContainer;
    }

    public isHideDefault() {
        return this.hideDefault == "true";
    }

    public isFormComponent() {
        return this.formComponent === "true";
    }

    public isMarqueeSelectOuter() {
        return this.marqueeSelectOuter === "true";
    }

    public getFormWidth() {
        return this.formWidth;
    }

    public getFormHeight() {
        return this.formHeight;
    }

}