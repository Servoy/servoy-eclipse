import { Injectable} from "@angular/core";
import { Subject} from "rxjs";

@Injectable()
export class TooltipService {
   isTooltipActive: Subject<boolean>;
  private tooltipDiv: HTMLDivElement;
  private tipmousemouveEventX: any;
  private tipmousemouveEventY: any;
  private tipmousemouveEventIsPage: boolean;
  tipInitialTimeout: any;
  tipTimeout: any;
  constructor(){
    this.isTooltipActive = new Subject<boolean>();
  }

  private getTooltipDiv(): HTMLDivElement {
    if (!this.tooltipDiv) {
      this.tooltipDiv = window.document.createElement('div');
      this.tooltipDiv.id = 'mktipmsg';
      this.tooltipDiv.className = 'mktipmsg tooltip-inner'; // tooltip-inner class is also used by ui-bootstrap-tpls-0.10.0
      window.document.getElementsByTagName('body')[0].appendChild(this.tooltipDiv);
    }
    return this.tooltipDiv;
  }

  private tipmousemove(e): void {
    if (e.pageX || e.pageY) {
      this.tipmousemouveEventIsPage = true;
      this.tipmousemouveEventX = e.pageX;
      this.tipmousemouveEventY = e.pageY;
    }
    else if (e.clientX || e.clientY) {
      this.tipmousemouveEventIsPage = false;
      this.tipmousemouveEventX = e.clientX;
      this.tipmousemouveEventY = e.clientY;
    }
  }

  private adjustAndShowTooltip(dismissDelay) {
    let x = 0;
    let y = 0;

    if (this.tipmousemouveEventX || this.tipmousemouveEventY) {
      if (this.tipmousemouveEventIsPage) {
        x = this.tipmousemouveEventX;
        y = this.tipmousemouveEventY;
      }
      else {
        x = this.tipmousemouveEventX + window.document.body.scrollLeft + window.document.documentElement.scrollLeft;
        y = this.tipmousemouveEventY + window.document.body.scrollTop + window.document.documentElement.scrollTop;
      }
    }

    let wWidth = 0, wHeight = 0;
    if (typeof(window.innerWidth) == 'number') {
      //Non-IE
      wWidth = window.innerWidth;
      wHeight = window.innerHeight;
    }
    else if (window.document.documentElement && (window.document.documentElement.clientWidth || window.document.documentElement.clientHeight)) {
      //IE 6+ in 'standards compliant mode'
      wWidth = window.document.documentElement.clientWidth;
      wHeight = window.document.documentElement.clientHeight;
    }


    let tDiv = this.getTooltipDiv();
    tDiv.style.left = x + 10 + "px";
    tDiv.style.top = y + 10 + "px";
    tDiv.style.display = "block";
    let tooltipOffsetWidth = x + 10 + tDiv.offsetWidth;

    if (wWidth < tooltipOffsetWidth) {
      let newLeft = x - 10 - tDiv.offsetWidth;
      if (newLeft < 0) {
        newLeft = 0;
        tDiv.style.width = x - 10 + "px";
      }
      if (newLeft == 0)
        newLeft = tDiv.offsetWidth;
      tDiv.style.left = newLeft + "px";
    }

    let tooltipOffsetHeight = y + 10 + tDiv.offsetHeight;
    if (wHeight < tooltipOffsetHeight) {
      let newTop = y - 10 - tDiv.offsetHeight;
      tDiv.style.top = newTop + "px";
    }
    this.isTooltipActive.next(true);
    this.tipTimeout = setTimeout(() => this.hideTooltip(), dismissDelay);


  }

  private internalHideTooltip() {
    if (window.removeEventListener) {
      window.removeEventListener('mousemove', this.tipmousemove, false);
    }
    else {
      window.removeEventListener('mousemove', this.tipmousemove);
    }
    clearTimeout(this.tipInitialTimeout);
    clearTimeout(this.tipTimeout);

    let tDiv = this.getTooltipDiv();
    tDiv.style.display = "none";
    this.isTooltipActive.next(false);
  }

  public showTooltip(event, message, initialDelay, dismissDelay) {
    let e = event;
    if (!e) e = window.event;

    let targ;
    if (e.target) targ = e.target;
    else if (e.srcElement) targ = e.srcElement;
    if (targ.nodeType == 3) // defeat Safari bug
      targ = targ.parentNode;

    if (targ.tagName && targ.tagName.toLowerCase() == "option")	// stop tooltip if over option element
    {
      this.hideTooltip();
      return;
    }

    let tDiv = this.getTooltipDiv();
    tDiv.innerHTML = message;
    tDiv.style.zIndex = '1600';
    tDiv.style.width = "";
    tDiv.style.overflow = "hidden";

    this.tipmousemove(e);
    if (window.addEventListener) {
      window.document.addEventListener('mousemove', this.tipmousemove, false);
    }
    this.tipInitialTimeout = setTimeout(() => this.adjustAndShowTooltip(dismissDelay), initialDelay);
  }

  public hideTooltip() {
    this.internalHideTooltip();
  }
}





