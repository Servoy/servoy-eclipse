var AdmZip = require('adm-zip');

// creating archives
var zip = new AdmZip();

zip.addLocalFolder("./META-INF/", "/META-INF/");
zip.addLocalFolder("./dist/servoy/ng2package/", "/dist/servoy/ng2package/");
// add here all ng1 components/services 
//zip.addLocalFolder("./mycomponent/", "/mycomponent/");

zip.writeZip("##packagename##.zip");