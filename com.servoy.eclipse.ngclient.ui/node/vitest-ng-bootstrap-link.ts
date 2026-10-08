import { existsSync, lstatSync, mkdirSync, rmSync, symlinkSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * Vitest globalSetup that makes @ng-bootstrap/ng-bootstrap resolvable from the node/ workspace
 * root during a test run.
 *
 * @ng-bootstrap/ng-bootstrap (plus its own deps: bootstrap, @popperjs/core, @angular/cdk) is a
 * dependency of the @servoy/servoydefault project ONLY, installed under
 * projects/servoydefault/node_modules. It is deliberately NOT a root dependency: a Servoy app that
 * does not include the default-components package must stay bootstrap-free, so adding it to the
 * root package.json is not an option.
 *
 * When the Angular unit-test (Vitest) builder batches several servoydefault specs it emits a SHARED
 * chunk hoisted to the node/ build root, whose @ng-bootstrap import can no longer be resolved from
 * the project-local node_modules and the build fails ("Failed to resolve import
 * @ng-bootstrap/ng-bootstrap/nav from chunk-*.js"). A Vite resolve.alias cannot fix this because an
 * alias to a filesystem path bypasses the package "exports" subpath map and breaks the per-spec
 * builds that currently pass.
 *
 * The robust fix is a real node_modules entry: a directory junction (Windows) / symlink
 * (Linux/CI) from node/node_modules/@ng-bootstrap/ng-bootstrap to the servoydefault copy, so normal
 * node resolution (honouring the exports map) finds it from the root too. The link is created only
 * for a test run and is gitignored; the servoydefault npm install (run in the Maven initialize
 * phase, and present locally) guarantees the source folder exists.
 */
export default function setup(): void {
    const nodeRoot = __dirname;
    const target = resolve(nodeRoot, 'projects/servoydefault/node_modules/@ng-bootstrap/ng-bootstrap');
    const scopeDir = resolve(nodeRoot, 'node_modules/@ng-bootstrap');
    const linkPath = resolve(scopeDir, 'ng-bootstrap');

    if (!existsSync(target)) {
        throw new Error(
            `[vitest-ng-bootstrap-link] expected @ng-bootstrap/ng-bootstrap at ${target}. ` +
            `Run "npm install --legacy-peer-deps" in projects/servoydefault first.`
        );
    }

    // Already linked (or a real install exists) and resolvable: nothing to do.
    if (existsSync(linkPath)) {
        return;
    }

    mkdirSync(scopeDir, { recursive: true });

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
