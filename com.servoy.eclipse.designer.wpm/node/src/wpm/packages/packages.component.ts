import { Component, OnInit, Input } from '@angular/core';
import { Package, WpmService } from '../wpm.service';
import { PackageList } from '../content/content.component'

@Component({
  selector: 'app-packages',
  templateUrl: './packages.component.html',
  styleUrls: ['./packages.component.css']
})
export class PackagesComponent implements OnInit {

  @Input() packageList: PackageList;
  packages: Package[];
  selectedPackage: Package;
  descriptionExpanded: boolean;

  constructor(public wpmService: WpmService) {
  }

  ngOnInit() {
    this.wpmService.getPackages().subscribe(p => {
      if(p.packageType == this.packageList.type) {
        this.packages = p.packages;
        this.packageList.updateCount = this.getUpgradeCount(this.packages);
      }
    })
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
    return p.selected == p.releases[0].version ? "Latest" : "";
  }

  installAvailable(p: Package): boolean {
    const installedVersion = p.installed == 'unknown' ? '' : p.installed;
    return !p.installed || (p.installedIsWPA && p.selected != installedVersion) || (!p.installedIsWPA && p.selected > installedVersion);
  }

  canBeRemoved(p: Package): boolean {
    return p.installed && p.installedIsWPA && (p.packageType != 'Solution');
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

  getUpgradeCount(packages: Package[]): number {
    let count = 0;
    try {
      for (let i = 0; i < packages.length; i++) {
          let pckg = packages[i];
          if (pckg.installed && pckg.installed < pckg.releases[0].version) {
            count++;
          }
      }
    } catch (e) {}
    return count;
  }

  getReleaseTooltip(p: Package): string {
    if (p.installed) { 
      return "Released versions";
    } else {
      return "Version to add to the active solution or modules...";
    }
  }

  getInstallTooltip(p: Package): string {
    if (p.installed) { 
      const installedVersion = p.installed == 'unknown' ? '' : p.installed;
      return p.installing ?
        (p.selected > installedVersion ? "Upgrading the web package..." : "Downgrading the web package...") :
        (p.selected > installedVersion ? "Upgrade the web package to the selected release version." : "Downgrade the web package to the selected release version.");
    } else if(p.installing) {
      return "Adding the web package...";		      
    } else {
      return "Add the web package to solution '" + p.activeSolution + "'.";
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
}
