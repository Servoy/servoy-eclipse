import { Component, Input } from '@angular/core';
import { Package, WpmService, PACKAGE_TYPE_SOLUTION, PACKAGE_TYPE_MODULE } from '../wpm.service';

@Component({
  selector: 'app-packages',
  templateUrl: './packages.component.html',
  styleUrls: ['./packages.component.css']
})
export class PackagesComponent {

  @Input() packages: Package[];
  selectedPackage: Package;
  descriptionExpanded: boolean;

  constructor(public wpmService: WpmService) {
  }

  install(p: Package) {
    this.wpmService.install(p);
  }

  uninstall(p: Package) {
    this.wpmService.uninstall(p);
  }

  showUrl(url: string) {
    this.wpmService.showUrl(url);
  }

  getSelectedRelease(p: Package): string {
    if(!p.selected) {
      p.selected = p.releases[0].version;
    }
    return this.versionCompare(p.selected, p.releases[0].version) == 0 ? "Latest" : "";
  }

  installAvailable(p: Package): boolean {
    const installedVersion = p.installed == 'unknown' ? '' : p.installed;
    return !p.installed || (p.installedIsWPA && (this.versionCompare(p.selected, installedVersion) != 0)) || (!p.installedIsWPA && (this.versionCompare(p.selected, installedVersion) > 0));
  }

  canBeRemoved(p: Package): boolean {
    return p.installed && p.installedIsWPA && (p.packageType != PACKAGE_TYPE_MODULE) && (p.packageType != PACKAGE_TYPE_SOLUTION);
  }

  isLatestRelease(p: Package): boolean {
    return p.installed == p.releases[0].version;
  }

  isInstallingOrRemoving(p: Package): boolean {
    return p.installing || p.removing;
  }

  installEnabled(p: Package): boolean {
    return !this.isInstallingOrRemoving(p) && this.installAvailable(p);
  }

  isPackageSelected(p: Package): boolean {
    return this.selectedPackage == p;
  }

  isSelectedAndWithDescriptionExpanded(p: Package): boolean {
    return this.isPackageSelected(p) && this.descriptionExpanded;    
  }

  togglePackageSelection(event: MouseEvent, p:Package){
    if (this.isPackageSelected(p)) {
        this.descriptionExpanded = !this.descriptionExpanded;
        if (this.descriptionExpanded) this.descriptionExpanded = !!p.description; // allow expand only if it has a description
    } else {
        this.selectedPackage = p;
        this.descriptionExpanded = !!p.description;
    }
    event.stopPropagation();
  }

  getPackageDescription(p: Package){
    // if (p.description) {
    //   return $sce.trustAsHtml(p.description);
    // }
    // return "";
    return p.description;
  }

  getSolutions(): string[] {
    return this.wpmService.getAllSolutions();
  }

  getReleaseTooltip(p: Package): string {
    if (p.installed) { 
      return "Released versions";
    } else {
      return this.needsActiveSolution(p) ? "Version to add to the active solution or modules..." : "Version to install";
    }
  }

  getInstallTooltip(p: Package): string {
    const packageType = p.packageType == PACKAGE_TYPE_MODULE ? "module" : p.packageType == PACKAGE_TYPE_SOLUTION ? "solution" : "web package";
    if (p.installed) { 
      const installedVersion = p.installed == 'unknown' ? '' : p.installed;
      return p.installing ?
        (this.versionCompare(p.selected, installedVersion) > 0 ? "Upgrading the " + packageType + "..." : "Downgrading the " + packageType + "...") :
        (this.versionCompare(p.selected, installedVersion) > 0 ? "Upgrade the " + packageType + " to the selected release version." : "Downgrade the " + packageType + " to the selected release version.");
    } else if(p.installing) {
      return "Adding the " + packageType + "...";		      
    } else {
      return this.needsActiveSolution(p) ? "Add the " + packageType + " to solution '" + p.activeSolution + "'." : "Install the " + packageType;
    }    
  }

  getRemoveTooltip(p: Package): string {
    return "Remove the web package from solution " + p.activeSolution;
  }

  getDescriptionTooltip(p: Package): string {
    return "Click to read more about package " + p.displayName;
  }

  getNotWPAAddedTooltip(): string {
    return "Package not added using Web Package Manager"; 
  }

  getSolutionTooltip(p: Package): string {
    if (p.installed) { 
      return p.installing ? "Solution that will contain this upgrading package..." : "The solution that already contains/references this package.";
    } else if(p.installing) {
      return "Solution that will contain this package...";		      
    } else {
      return "Solution that this package will be added to if you press the 'Add' button.";
    }    
  }

  needsActiveSolution(p: Package): boolean {
    return p.packageType != PACKAGE_TYPE_SOLUTION;
  }

  versionCompare(v1: string, v2: string): number {
    const av1 = v1.split('.');
    const av2 = v2.split('.');

    const sizeDiff = av2.length - av1.length;
    if(sizeDiff) {
      for(let i = 0; i < Math.abs(sizeDiff); i++) {
        if(sizeDiff > 0) av1.push('0')
        else av2.push('0');
      }
    }

    for(let i = 0; i < av1.length; i++) {
      const ival1 = parseInt(av1[i]);
      const ival2 = parseInt(av2[i]);
      if(ival1 != NaN && ival2 !=NaN) {
        if(ival1 != ival2) return ival1 - ival2;
      }
      else if(av1[i] < av2[i]) return -1;
      else if(av1[i] > av2[i]) return 1;
    }

    return 0;
  }
}
