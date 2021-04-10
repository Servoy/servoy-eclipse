// or using a script like:
try {
  const pkg = require('../dist/servoy-public/package.json');
  delete pkg.scripts;
  require('fs').writeFileSync('./dist/servoy-public/package.json', JSON.stringify(pkg, null, 2));
}
catch (err) {
  console.log(`Ivy package fix failed: ${err}`);
  process.exit();
}