import { Component, OnInit } from '@angular/core';
import {PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE, PACKAGE_TYPE_WEB_LAYOUT, PACKAGE_TYPE_SOLUTION} from '../wpm.service'

export interface PackageList {
  title: string;
  type: string;
  updateCount: number;
}

@Component({
  selector: 'app-content',
  templateUrl: './content.component.html',
  styleUrls: ['./content.component.css']
})
export class ContentComponent implements OnInit {

  packageLists: PackageList[] = [
    {
      title: "NG Components",
      type: PACKAGE_TYPE_WEB_COMPONENT,
      updateCount: 0
    },
    {
      title: "NG Services",
      type: PACKAGE_TYPE_WEB_SERVICE,
      updateCount: 0
    },
    {
      title: "NG Layouts",
      type: PACKAGE_TYPE_WEB_LAYOUT,
      updateCount: 0
    },
    {
      title: "Servoy solutions",
      type: PACKAGE_TYPE_SOLUTION,
      updateCount: 0
    }
  ]

  constructor() { }

  ngOnInit() {
  }

  getPackageTabLabel(packageList: PackageList): string {
    return packageList.updateCount > 0 ? packageList.title + " (" + packageList.updateCount + ")" : packageList.title;
  }
}
