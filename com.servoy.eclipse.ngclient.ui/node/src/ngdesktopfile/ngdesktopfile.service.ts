/// <reference path="./electron.d.ts"/>
import { Injectable } from '@angular/core';
import { LoggerFactory, LoggerService, Deferred, WindowRefService, ServoyPublicService } from 'servoy-public';


import * as fs from 'node:fs';
import * as os from 'node:os';
import * as electron from 'electron';
import * as chokidar from 'chokidar';


@Injectable()
export class NGDesktopFileService {

    private defer: Deferred<any>;
    private log: LoggerService;
    private watchers = new Map();
    private fs: typeof fs;
    private os: typeof os;
    private chokidar: typeof chokidar;
    private request: any;
    private remote: electron.Remote;
    private shell: electron.Shell;
//    private session: typeof electron.Session;
    private dialog: electron.Dialog;

    constructor(private servoyService: ServoyPublicService, private windowRef: WindowRefService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('NGDesktopFileService');
        const userAgent = navigator.userAgent.toLowerCase();
        const r = this.windowRef.nativeWindow['require'];
        if (userAgent.indexOf(' electron/') > -1  && r) {
            this.fs = r('fs');
            this.os = r('os');
            this.chokidar = r('chokidar');
            this.request = r('request');
            this.remote = r('electron').remote;
            this.shell = r('electron').shell;
//            this.session = this.remote.session;
            this.dialog = this.remote.dialog;
        } else {
            this.log.warn('ngdesktopfile service/plugin loaded in a none electron environment');
        }
    }
    waitForDefered(func) {
        if (this.defer != null) {
            this.defer.promise.then(() => {
                func();
            });
        } else func();
    }

    /**
     * Returns the home dir of the user like c:/users/[username] under windows.
     * Will return always a both with forward slashes.
     */
    homeDir() {
        return this.os.homedir().replace(/\\/g, '/');
    }

    /**
     * Returns the tmp directory of the client machine.
     * Will return always a both with forward slashes.
     */
    tmpDir() {
        return this.os.tmpdir().replace(/\\/g, '/');
    }

    /**
     * returns an array of filenames that are in the given path.
     * Please use forward slashes (/) instead of backward slashes.
     */
    listDir(path) {
        const defer = new Deferred();;
        this.waitForDefered(() => {
            this.fs.readdir(path, (error, files) => {
                defer.resolve(files);
            });
        });
        return defer.promise;
    }

    /**
     * Watches a directory for changes at the given path.
     *
     * @param dir - directory's full path
     * @param callback - the callback method to be executed
     */
    watchDir(dir: string, callback) {
        /** Please check the below used library here: https://github.com/paulmillr/chokidar
         * add, addDir, change, unlink, unlinkDir these are all events.
         * add is for adding file
         * addDir is for adding folders
         * unlink is for deleting files
         * unlinkDir is for delete folders
         * change is for changing files **/
        if (!this.watchers.get(dir)) {
            // Initialize watcher
            const watcher = this.chokidar.watch(dir, {
                ignoreInitial: true,
                alwaysStat: true
            });
            this.waitForDefered(() => {
                watcher.on('add', (path, stats) => {
                    this.log.debug('this is an add event\n', 'path: ' + path + '\n', stats);
                    this.servoyService.executeInlineScript(callback.formname, callback.script, [path]);
                }).on('addDir', (path, stats) => {
                    this.log.debug('this is an addDir event\n', 'path: ' + path + '\n', stats);
                    this.servoyService.executeInlineScript(callback.formname, callback.script, [path]);
                }).on('change', (path, stats) => {
                    // For MacOS: Do not make the callback when .DS_Store is changed.
                    // DS_Store is a file that stores custom attributes of its containing folder,
                    // such as the position of icons or the choice of a background image
                    if (!path.includes('.DS_Store')) {
                        this.log.debug('this is a change file event\n', 'path: ' + path + '\n', stats);
                        this.servoyService.executeInlineScript(callback.formname, callback.script, [path]);
                    }
                }).on('unlink', (path) => {
                    this.log.debug('unlink (delete) event\n', 'path: ' + path);
                    this.servoyService.executeInlineScript(callback.forname, callback.script, [path]);
                }).on('unlinkDir', (path) => {
                    this.log.debug('unlinkDir (delete folder) event\n', 'path: ' + path);
                    this.servoyService.executeInlineScript(callback.formname, callback.script, [path]);
                }).on('error', (error) => {
                    this.log.error('Watcher error: ' + error);
                });
            });
            // Save the watchers in a map so that they can be removed later if wanted.
            this.watchers.set(dir, watcher);
            this.log.debug('A new watcher has been set for the following path: ' + dir);
        } else {
            this.log.debug('A watcher has already been set for this path: ' + dir);
        }
    }

    /**
     * Stop watching a directory found at the given path.
     */
    unwatchDir(path) {
        const watcher = this.watchers.get(path);
        if (watcher) {
            watcher.close();
            this.watchers.delete(path);
            this.log.debug('The watcher at the following path has been removed: ' + path);
        } else {
            this.log.debug('There is no watcher to be removed for the given path: ' + path);
        }
    }

    /**
     * Watches a give path, that should represent a file, for modifications.
     * Please use forward slashes (/) instead of backward slashes in the path/filename
     */
    watchFile(path: string, callback) {
        this.waitForDefered(() => {
            this.fs.watchFile(path, (curr, prev) => {
                if (curr.mtime !== prev.mtime)
                    this.servoyService.executeInlineScript(callback.formname, callback.script, [path]);
            });
        });
    }

    /**
     * Removes the watch to the file that was added by the watchFile() function.
     * Please use forward slashes (/) instead of backward slashes in the path/filename
     */
    unwatchFile(path) {
        this.fs.unwatchFile(path);
    }

    /**
     * Writes the given bytes to the path, if the path has sub directories that are not there
     * then those are made. If the path is missing or contain only the file name then the
     * native system dialog for saving files it is called.
     * Please use forward slashes (/) instead of backward slashes in the path/filename
     */
    writeFile(path, bytes) {
        // empty impl, is implemented in server side api calling the impl method below.
    }

    writeFileImpl(path, url) {
        this.waitForDefered(() => {
            this.defer = new Deferred();
            path = (path != null) ? path : '';
            let dir = path;
            const index = path.lastIndexOf('/');
            if (index >= 0) {
                dir = path.substring(0, index);
                this.saveUrlToPath(dir, path, url);
            } else {
                const options = {
                    title: 'Save file',
                    defaultPath: path,
                    buttonLabel: 'Save'
                };
                this.dialog.showSaveDialog(this.remote.getCurrentWindow(), options)
                    .then((result) => {
                        if (!result.canceled) {
                            const realPath = result.filePath.replace(/\\/g, '/'); //on Windows the path contains backslash
                            const indexOf = realPath.lastIndexOf('/');
                            if (indexOf > 0) {
                                dir = realPath.substring(0, indexOf);
                                this.saveUrlToPath(dir, realPath, url);
                            } else {
                                this.defer.resolve(false);
                                this.defer = null;
                            }
                        } else {
                            this.defer.resolve(true);
                            this.defer = null;
                        }
                    }).catch((err) => {
                        this.log.info(err);
                        this.defer.resolve(false);
                        this.defer = null;
                    });
            }
        });
    }

    /**
     * Reads the given bytes of a path, the callback is a function that will get as parameters the 'path' as a String and the 'file' as a JSUpload object
     * If the path is missing or contain only the file name then the native system dialog for opening files it is called.
     * Please use forward slashes (/) instead of backward slashes in the path/filename
     *
     */
    readFile(callback, path) {
        // empty impl, is implemented in server side api calling the impl method below.
    }

    readFileImpl(path, id) {
        this.waitForDefered(() => {
            path = (path != null) ? path : '';
            if (path.lastIndexOf('/') >= 0) {
                this.readUrlFromPath(path, id);
            } else {
                const options = {
                    title: 'Open file',
                    defaultPath: path,
                    buttonLabel: 'Open'
                };
                this.dialog.showOpenDialog(this.remote.getCurrentWindow(), options)
                    .then((result) => {
                        if (!result.canceled) {
                            this.readUrlFromPath(result.filePaths[0].replace(/\\/g, '/'), id); //on Windows the path contains backslash
                        }
                    }).catch((err) => {
                        this.log.info(err);
                    });
            }
        });
    }

    /**
     * Select a folder and pass its path to the callback.
     */
    selectDirectory(callback) {
        this.waitForDefered(() => {
            const options: electron.OpenDialogOptions = {
                title: 'Select folder',
                buttonLabel: 'Select',
                properties: ['openDirectory']
            };
            this.dialog.showOpenDialog(this.remote.getCurrentWindow(), options)
                .then((result) => {
                    if (!result.canceled) {
                        this.servoyService.executeInlineScript(callback.formname, callback.script, [result.filePaths[0]]);
                    }
                }).catch((err) => {
                    this.log.info(err);
                });
        });
    }

    /**
     * Shows a file save dialog and calls the callback method with the file path
     *
     * For the options object see https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogbrowserwindow-options
     *
     * @param callback
     * @param [options]
     *
     * Core options are
     *
     * title: String the dialog title
     * defaultPath: String - absolute directory path, absolute file path, or file name to use by default.
     * buttonLabel: String - custom label for the confirmation button, when left empty the default label will be used.
     * filters: Array<{name: String, extensions: Array<String>}> - an array of file filters (e.g. [{ name: 'Images', extensions: ['jpg', 'png', 'gif'] }])
     */
    showSaveDialog(callback, options) {
        this.waitForDefered(() => {
            if (!options) {
                options = {};
            }
            this.dialog.showSaveDialog(this.remote.getCurrentWindow(), options)
                .then((result) => {
                    if (!result.canceled) {
                        this.servoyService.executeInlineScript(callback.formname, callback.script, [result.filePath]);
                    }
                }).catch((err) => {
                    this.log.info(err);
                });
        });
    }

    /**
     * Shows a file save dialog
     *
     * To not block any process, showSaveDialog with a callback method is preferred over this method
     *
     * For the options object see https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogsyncbrowserwindow-options
     *
     * @param [options]
     *
     * Core options are
     *
     * title: String the dialog title
     * defaultPath: String - absolute directory path, absolute file path, or file name to use by default.
     * buttonLabel: String - custom label for the confirmation button, when left empty the default label will be used.
     * filters: Array<{name: String, extensions: Array<String>}> - an array of file filters (e.g. [{ name: 'Images', extensions: ['jpg', 'png', 'gif'] }])
     *
     * @return
     */
    showSaveDialogSync(options) {
        try {
            return this.dialog.showSaveDialogSync(this.remote.getCurrentWindow(), options);
        } catch (e) {
            this.log.info(e);
        }
    }

    /**
     * Shows a file open dialog and calls the callback with the selected file path(s)
     *
     * For the options object see https://www.electronjs.org/docs/api/dialog#dialogshowopendialogbrowserwindow-options
     *
     * Core options are
     *
     * title: String the dialog title
     * defaultPath: String the default (starting) path
     * buttonLabel: String custom label for the confirmation button, when left empty the default label will be used.
     * filters: Array<{name: String, extensions: Array<String>}> an array of file filters (e.g. [{ name: 'Images', extensions: ['jpg', 'png', 'gif'] }])
     * properties: an Array of property keywords such as
     *  <code>openFile</code> - Allow files to be selected.
     *  <code>openDirectory</code> - Allow directories to be selected.
     *  <code>multiSelections</code> - Allow multiple paths to be selected.
     *
     * @param callback
     * @param [options]
     */
    showOpenDialog(callback, options) {
        this.waitForDefered(() => {
            if (!options) {
                options = {};
            }
            this.dialog.showOpenDialog(this.remote.getCurrentWindow(), options)
                .then((result) => {
                    if (!result.canceled) {
                        this.servoyService.executeInlineScript(callback.formname, callback.script, [result.filePaths]);
                    }
                }).catch((err) => {
                    this.log.info(err);
                });
        });
    }

    /**
     * Shows a file open dialog and returns the selected file path(s)
     *
     * To not block any process, showOpenDialog with a callback method is preferred over this method
     *
     * For the options object see https://www.electronjs.org/docs/api/dialog#dialogshowopendialogsyncbrowserwindow-options
     *
     * Core options are
     *
     * title: String the dialog title
     * defaultPath: String the default (starting) path
     * buttonLabel: String custom label for the confirmation button, when left empty the default label will be used.
     * filters: Array<{name: String, extensions: Array<String>}> an array of file filters (e.g. [{ name: 'Images', extensions: ['jpg', 'png', 'gif'] }])
     * properties: an Array of property keywords such as
     *  <code>openFile</code> - Allow files to be selected.
     *  <code>openDirectory</code> - Allow directories to be selected.
     *  <code>multiSelections</code> - Allow multiple paths to be selected.
     *
     * @param [options]
     * @return <Array<String>}
     */
    showOpenDialogSync(options) {
        try {
            return this.dialog.showOpenDialogSync(this.remote.getCurrentWindow(), options);
        } catch (e) {
            this.log.info(e);
        }
    }

    /**
     * Deletes the given file, optionally calling the error callback when unsuccessful
     *
     * @param path
     * @param [errorCallback]
     */
    deleteFile(path, errorCallback) {
        this.waitForDefered(() => {
            this.fs.unlink(path, (err) => {
                if (err && errorCallback) this.servoyService.executeInlineScript(errorCallback.formname, errorCallback.script, [err]);
            });
        });
    }

    /**
     * Return a 'stats' object containing related file's information's.
     * Please use forward slashes (/) instead of backward slashes in the path
     *
     * @return
     */
    getFileStats(path) {
        try {
            let fsStats = this.fs.statSync(path);
            if (fsStats.isSymbolicLink()) {
                fsStats = this.fs.lstatSync(path);
            }
            const retStats = {
                isBlockDevice: fsStats.isBlockDevice(),
                isCharacterDevice: fsStats.isCharacterDevice(),
                isDirectory: fsStats.isDirectory(),
                isFIFO: fsStats.isFIFO(),
                isFile: fsStats.isFile(),
                isSocket: fsStats.isSocket(),
                isSymbolicLink: fsStats.isSymbolicLink(),
                dev: fsStats.dev,
                ino: fsStats.ino,
                mode: fsStats.mode,
                nlink: fsStats.nlink,
                uid: fsStats.uid,
                gid: fsStats.gid,
                rdev: fsStats.rdev,
                size: fsStats.size,
                blksize: fsStats.blksize,
                blocks: fsStats.blocks,
                atimeMs: fsStats.atimeMs,
                mtimeMs: fsStats.mtimeMs,
                ctimeMs: fsStats.ctimeMs,
                birthtimeMs: fsStats.birthtimeMs
            };
            return retStats;
        } catch (err) {
            this.log.info(err);
        }
    }

    /**
     * Opens a file specified at the given path.
     * It returns a string value.
     * If the value is empty, then the file has been successfully opened, otherwise the string contains the error message.
     *
     * @param path - file's full path
     * @return
     */
    openFile(path) {
        return this.shell.openPath(path);
    }

    /**
     * Test whether or not the given path exists by checking with the file system.
     * It returns true if the path exists, false otherwise.
     *
     * @param path - file's full path
     * @return
     */
    exists(path) {
        try {
            let result = false;

            if (path) {
                result = this.fs.existsSync(path);
            }

            return result;
        } catch (err) {
            this.log.info(err);
        }
    }

    /**
     * Synchronously append data to a file, creating the file if it does not yet exist.
     *
     * @param path - file's full path
     * @param text - text to be added
     * @param [encoding] - default utf8
     * @return
     */
    appendToTXTFile(path, text, encoding) {
        let result = true;
        try {
            encoding = encoding || null;


            if (path && text) {
                this.fs.appendFileSync(path, text, encoding);
            } else {
                result = false;
            }
        } catch (err) {
            result = false;
            this.log.info(err);
        }
        return result;
    }

    /**
     * Synchronously copies src to dest. By default, dest is overwritten if it already exists.
     *
     * @param src - source filepath to copy
     * @param dest - destination filepath of the copy operation
     * @param [overwriteDest] - default true
     * @return
     */
    copyFile(src, dest, overwriteDest) {
        let result = true;
        try {
            const mode = (overwriteDest === false) ? 1 : 0;

            if (src && dest) {
                this.fs.copyFileSync(src, dest, mode);
            } else {
                result = false;
            }
        } catch (err) {
            result = false;
            this.log.info(err);
        }
        return result;
    }

    /**
     * Synchronously creates a folder, including any necessary but nonexistent parent folders.
     *
     * @param path - folders full path
     * @return
     */
    createFolder(path) {
        let result = true;
        try {

            if (path) {
                this.fs.mkdirSync(path, { recursive: true });
                result = this.fs.existsSync(path);
            } else {
                result = false;
            }
        } catch (err) {
            result = false;
            this.log.info(err);
        }
        return result;
    }

    /**
     * Synchronously deletes a folder, fails when folder is not empty
     *
     * @param path - folders full path
     * @return
     */
    deleteFolder(path) {
        let result = true;
        try {

            if (path) {
                this.fs.rmdirSync(path);
                result = !this.fs.existsSync(path);
            } else {
                result = false;
            }
        } catch (err) {
            result = false;
            this.log.info(err);
        }
        return result;
    }

    /**
     * Synchronously rename file at oldPath to the pathname provided as newPath. In the case that newPath already exists, it will be overwritten.
     *
     * @param oldPath - old file full path
     * @param newPath - new file full path
     *
     * @return
     */
    renameFile(oldPath, newPath) {
        let result = true;
        try {

            if (oldPath && newPath) {
                this.fs.renameSync(oldPath, newPath);
            } else {
                result = false;
            }
        } catch (err) {
            result = false;
            this.log.info(err);
        }
        return result;
    }

    /**
     * Writes text to the given path/filename
     *
     * @param path
     * @param text_data
     * @param [encoding] optional, default 'utf8'
     *
     * @return
     */
    writeTXTFileSync(path, text_data, encoding) {
        let result = true;
        try {

            text_data = text_data || '';
            const options: fs.BaseEncodingOptions = { encoding: 'utf8' };

            if (encoding) {
                options.encoding = encoding;
            }

            if (path) {
                this.fs.writeFileSync(path, text_data, options);
            } else {
                result = false;
            }
        } catch (err) {
            result = false;
            this.log.info(err);
        }
        return result;
    }

    /**
     * Reads and returns the text of the given path/filename
     *
     * @param path
     * @param [encoding] optional, default 'utf8'
     *
     * @return
     */
    readTXTFileSync(path, encoding) {
        let result = null;
        try {

            const options: fs.BaseEncodingOptions = { encoding: 'utf8' };

            if (encoding) {
                options.encoding = encoding;
            }

            if (path) {
                result = this.fs.readFileSync(path, options);
            }
        } catch (err) {
            this.log.info(err);
        }
        return result;
    }

    private getFullUrl(url: string) {
        let base = document.baseURI;
        if (!base.endsWith('/')) base = base + '/';
        return base + url;
    }

    private saveUrlToPath(dir: string, realPath: string, url: string) {
        this.fs.mkdir(dir, { recursive: true }, (err) => {
            if (err) {
                this.defer.resolve(false);
                this.defer = null;
                throw err;
            } else {
                const pipe = this.request(this.getFullUrl(url)).pipe(this.fs.createWriteStream(realPath));
                pipe.on('error', (err2) => {
                    this.defer.resolve(false);
                    this.defer = null;
                    throw err2;
                });
                pipe.on('close', () => {
                    this.defer.resolve(true);
                    this.defer = null;
                });
            }
        });
    }

    private readUrlFromPath(path, id) {
        const formData = {
            path,
            id,
            file: this.fs.createReadStream(path)
        };
        this.request.post({ url: this.getFullUrl(this.servoyService.generateServiceUploadUrl('ngdesktopfile', 'callback')), formData },
            (err, httpResponse, body) => {
                if (err) {
                    return this.log.error('upload failed:', err);
                }
            });
    }

}
