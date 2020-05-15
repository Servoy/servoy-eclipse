import { Subject } from "rxjs";
import { Injectable } from "@angular/core";

export class LoadingIndicatorService { 
    
    showLoadingIndicator: Subject<boolean> = new Subject<boolean>();
    private showCounter = 0;
    private timeoutShow = null;
    private timeoutHide = null;
    
    
    public showLoading() {
        this.showCounter++;
        if (this.showCounter == 1) {
            if (this.timeoutHide) {
                clearTimeout(this.timeoutHide);
                this.timeoutHide = null;
            } else if (!this.timeoutShow) {
                this.timeoutShow = setTimeout(() => {
                    this.timeoutShow = null;
                    this.showLoadingIndicator.next(true);
                }, 400);
            }
        }
    }
    
    public hideLoading() {
        this.showCounter--;
        if (this.showCounter == 0) {
            this.timeoutHide = setTimeout(() => {
                this.timeoutHide = null;
                if (this.timeoutShow) {
                    clearTimeout(this.timeoutShow);
                    this.timeoutShow = null;
                } else {
                    this.showLoadingIndicator.next(false);
                }
            }, 50);
        } 
    }
    
    isShowing() {
        return this.showCounter > 0;
    }
}