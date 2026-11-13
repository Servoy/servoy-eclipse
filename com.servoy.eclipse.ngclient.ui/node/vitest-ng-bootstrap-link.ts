import { existsSync, lstatSync, mkdirSync, rmSync, symlinkSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * Vitest globalSetup that makes the @servoy/servoydefault project-local dependencies resolvable
 * from the node/ workspace root during a test run.
 *
 * @ng-bootstrap/ng-bootstrap (plus its own deps: bootstrap, @popperjs/core, @angular/cdk),
 * @eonasdan/tempus-dominus and the TinyMCE packages (@tinymce/tinymce-angular, tinymce,
 * tinymce-i18n) are dependencies of the @servoy/servoydefault project ONLY, installed under
 * projects/servoydefault/node_modules. They are deliberately NOT root dependencies: a Servoy app
 * that does not include the default-components package must stay free of them, so adding them to
 * the root package.json is not an option.
 *
 * When the Angular unit-test (Vitest) builder batches several servoydefault specs - which it does
 * notably once --coverage is on - it emits SHARED chunks hoisted to the node/ build root, whose
 * imports of these packages can no longer be resolved from the project-local node_modules and the
 * build fails ("Failed to resolve import @ng-bootstrap/ng-bootstrap/nav from chunk-*.js", or the
 * same for @eonasdan/tempus-dominus / @tinymce/tinymce-angular from spec-lib-*.js). A Vite
 * resolve.alias cannot fix this because an alias to a filesystem path bypasses the package
 * "exports" subpath map and breaks the per-spec builds that currently pass.
 *
 * The robust fix is a real node_modules entry: a directory junction (Windows) / symlink
 * (Linux/CI) from node/node_modules/<pkg> to the servoydefault copy, so normal node resolution
 * (honouring the exports map) finds it from the root too. The links are created only for a test
 * run and are gitignored; the servoydefault npm install (run in the Maven initialize phase, and
 * present locally) guarantees the source folders exist.
 */

// Packages that live under projects/servoydefault/node_modules and must be linked into the root.
// Scoped packages ('@scope/name') and plain packages ('name') are both supported.
const PACKAGES = [
    '@ng-bootstrap/ng-bootstrap',
    '@eonasdan/tempus-dominus',
    '@popperjs/core',
    '@tinymce/tinymce-angular',
    'bootstrap',
    'tinymce',
    'tinymce-i18n'
];

function linkPackage(nodeRoot: string, pkg: string): void {
    const target = resolve(nodeRoot, 'projects/servoydefault/node_modules', pkg);
    const linkPath = resolve(nodeRoot, 'node_modules', pkg);

    if (!existsSync(target)) {
        throw new Error(
            `[vitest-ng-bootstrap-link] expected ${pkg} at ${target}. ` +
            `Run "npm install" in projects/servoydefault first.`
        );
    }

    // Already linked (or a real install exists) and resolvable: nothing to do.
    if (existsSync(linkPath)) {
        return;
    }

    // Ensure the parent (the @scope dir for scoped packages, node_modules otherwise) exists.
    const lastSep = Math.max(linkPath.lastIndexOf('/'), linkPath.lastIndexOf('\\'));
    mkdirSync(linkPath.slice(0, lastSep), { recursive: true });

    // Clean up a dangling link if one is left over from an interrupted run.
    try {
        if (lstatSync(linkPath)) {
            rmSync(linkPath, { recursive: true, force: true });
        }
    } catch {
        // lstatSync throws when the path does not exist, which is the normal case.
    }

    // 'junction' works on Windows without elevated rights and is treated as a dir symlink
    // elsewhere, so it is the portable choice for a directory link.
    symlinkSync(target, linkPath, 'junction');
}

export default function setup(): void {
    const nodeRoot = __dirname;
    for (const pkg of PACKAGES) {
        linkPackage(nodeRoot, pkg);
    }
}
