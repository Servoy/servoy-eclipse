import { Component, OnInit, Renderer2, ViewChild, ElementRef } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DesignSizeService } from '../services/designsize.service';
import { URLParserService } from '../services/urlparser.service';

@Component({
  selector: 'designer-editorcontent',
  templateUrl: './editorcontent.component.html',
  styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit{
    clientURL: SafeResourceUrl;
    @ViewChild('element', { static: true }) elementRef: ElementRef;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        protected designSize: DesignSizeService) {
        designSize.setEditor(this);
    }
    
     ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/designer/solution/'+this.urlParser.getSolutionName()+'/form/'+ this.urlParser.getFormName()+'/clientnr/'+ this.urlParser.getContentClientNr() +'/index.html');
        if (this.urlParser.isAbsoluteFormLayout())
        {
            this.renderer.setStyle(this.elementRef.nativeElement, 'width', this.urlParser.getFormWidth()+'px');
            this.renderer.setStyle(this.elementRef.nativeElement, 'height', this.urlParser.getFormHeight()+'px');
        }
        else
        {
             this.renderer.setStyle(this.elementRef.nativeElement, 'bottom', '20px');
             this.renderer.setStyle(this.elementRef.nativeElement, 'right', '20px');
        }
    }

    setContentSizeFull(redraw: boolean) {
        // TODO:
        // $scope.contentStyle = {
        //     position: "absolute",
        //     top: "20px",
        //     left: "20px",
        //     right: "20px",
        //     bottom: "20px"
        // };
        // $scope.contentSizeFull = true;
        // delete $scope.contentStyle.width;
        // delete $scope.contentStyle.height;
        // delete $scope.contentStyle.h
        // delete $scope.contentStyle.w
        // $($scope.contentDocument).find('.svy-form').css('height', '');
        // $($scope.contentDocument).find('.svy-form').css('width', '');
        // $scope.adjustGlassPaneSize();
        // if (redraw)
        // {
        //     $scope.redrawDecorators()
        // }	
    }

    getFormInitialWidth() {
        // TODO:
        // if (!initialWidth)
        // {
        //     initialWidth = $element.find('.content')[0].getBoundingClientRect().width + "px";
        // }
        // return initialWidth;
        return "800px";
    }

    setContentSize(width: string, height: string, fixedSize: boolean) {
        // TODO:
        // $scope.contentStyle.width = width;
        // $scope.contentStyle.height = height;
        // if (fixedSize) $scope.contentSizeFull = false;
        // delete $scope.contentStyle.top;
        // delete $scope.contentStyle.left;
        // delete $scope.contentStyle.position;
        // delete $scope.contentStyle.minWidth;
        // delete $scope.contentStyle.bottom;
        // delete $scope.contentStyle.right;
        // delete $scope.contentStyle.h
        // delete $scope.contentStyle.w
        // // we need to apply the changes to dom earlier in order to adjust the to the new size
        // $element.find('.content')[0].style.width = width + "px";
        // $element.find('.content')[0].style.right = "";
        // $element.find('.content')[0].style.minWidth = "";
        // if (!$scope.isAbsoluteFormLayout()) {
        //     $($scope.contentDocument).find('.svy-form').css('width', width);
        //     $($scope.contentDocument).find('.svy-form').css('height', height);
        //     $scope.setMainContainerSize();
        //     $element.find('.content')[0].style.height = height;
        //        $element.find('.contentframe')[0].style.height = height;
        // }
        // $scope.adjustGlassPaneSize(width, height);
        // $scope.redrawDecorators();
    }
}
