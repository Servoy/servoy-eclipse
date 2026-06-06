/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
"use strict";

window.FileError = window.FileError || {
		NOT_FOUND_ERR: 'NOT_FOUND_ERR',
		SECURITY_ERR: 'SECURITY_ERR',
		ABORT_ERR: 'ABORT_ERR', 
		NOT_READABLE_ERR: 'NOT_READABLE_ERR',
		ENCODING_ERR: 'ENCODING_ERR',
		NO_MODIFICATION_ALLOWED_ERR: 'NO_MODIFICATION_ALLOWED_ERR',
		INVALID_STATE_ERR: 'INVALID_STATE_ERR',
		SYNTAX_ERR: 'SYNTAX_ERR',
		INVALID_MODIFICATION_ERR: 'INVALID_MODIFICATION_ERR',
		QUOTA_EXCEEDED_ERR: 'QUOTA_EXCEEDED_ERR',
		TYPE_MISMATCH_ERR: 'TYPE_MISMATCH_ERR',
		PATH_EXISTS_ERR: 'PATH_EXISTS_ERR'
}

function InMemBlob(contentsarray) { this.contents = contentsarray[0] }

function InMemFileReader() {
	this.onloadend = undefined
	this.onError = undefined
	this.readAsText = function(blob) { 
       		this.onloadend && this.onloadend({target:{result:blob.contents}})
	}
	this.readAsBinaryString = this.readAsText
}

function InMemFileWriter(entry) {
		this.entry = entry
		this.offset = 0

		this.seek = function(len)
		{
			this.offset = len
		}
		this.write = function(towrite)
		{
			if (this.offset == 0)
			{
				this.entry.contents = towrite
				if (typeof(this.entry.contents) == 'string')
				{
					this.offset = this.entry.contents.length
				}
				else
				{
					this.offset++
				}
			}
			else
			{
				if (typeof(this.entry.contents) != 'string')
				{
					this.entry.contents = '' + this.entry.contents
				}
				if (this.entry.contents.length > this.offset)
				{
					this.entry.contents = this.entry.contents.substring(0, this.offset)
				}
				this.entry.contents += towrite
				this.offset = this.entry.contents.length
			}
		}
}

function InMemDirectoryEntry(parentDir, name, isDir) {
		this.parentDir = parentDir
		this.name = name
		this.isDirectory = isDir
		this.isFile = !isDir
		this.contents = ''
		this.entries = {}

		function calcFullPath()
		{
			if (!parentDir) return '/'
			return (parentDir.fullPath == '/' ? '' :parentDir.fullPath) + '/' + name
		}

		this.fullPath = calcFullPath()

		this.toURL = function() {
			return 'file:' + this.fullPath
		} 

		this._getEntry = function(path, create, exclusive, isDir) {

			if (!path || this.isFile)
			{
				return null;
			}

			if (!parentDir && path.length > 0 && path.charAt(0) == '/')
			{
				path = path.slice(1)
			}
			var split = path.split('/');
			var name = split[0]
			var entry = this.entries[name]
			if (create)
			{
				if (!entry)
				{
					this.entries[name] = entry = new InMemDirectoryEntry(this, name, isDir || split.length > 1)
				}
				else if (exclusive)
				{
					return null;
				}
			}
			if (entry && split.length > 1)
			{
				return entry._getEntry (path.slice(name.length+1), create, exclusive, isDir)
			}
			return entry
		}
		this.getDirectory = function(path, options, success, error) {
			this.getEntry(path, options, success, error, true)
		}
		this.getFile = function(path, options, success, error) {
			this.getEntry(path, options, success, error, false)
		}
		this.getEntry = function(path, options, success, error, isDir) {
			var entry = this._getEntry(path, options && options.create, options && options.exclusive, isDir)
			if (entry && (entry.isDirectory == isDir))
			{
				success & success(entry)
			}
			else
			{
				error && error({code:FileError.NOT_FOUND_ERR})
			}
		}
		this.createReader = function() {
			var dir = this
			return {
				readEntries : function(success, error)
				{
					var entries = []
					for (name in dir.entries)
					{
						entries.push(dir.entries[name])
					}
					success && success(entries)
				}
			}
		}
		this.createWriter = function(success, error) {
			 success && success(new InMemFileWriter(this))
		}
		this.remove = function(success, error) {
			if (this.parentDir)
			{
				delete this.parentDir.entries[this.name]
				success && success()
			}
			else
			{
				error && error({code:FileError.NO_MODIFICATION_ALLOWED_ERR})
			}
		}
		this.removeRecursively = this.remove

		this.copyTo = function(parentDir, newName, success, error) {
			if (this.isDirectory)
			{
				error && error({code:FileError.NO_MODIFICATION_ALLOWED_ERR}) // not implemented
				return
			}
			var copy = parentDir._getEntry (newName ? newName: this.name, true, false, this.isDirectory)
			if (!copy)
			{
				error && error({code:FileError.NO_MODIFICATION_ALLOWED_ERR})
				return
			}
			copy.contents = this.contents
			success && success(copy)
		}

		this.moveTo = function(parentDir, newName, success, error) {
			parentDir.entries[newName ? newName: this.name] = this
			delete this.parentDir.entries[this.name]
			this.parentDir = parentDir
			this.fullPath = calcFullPath()
			success && success(this)
		}

		this.file = function(success, error) {
			success && success(this.contents) // contents is currently always Blob
		}
	}

window.inMemFileSystem = {
		name: 'In-Memory System',
		root: new InMemDirectoryEntry(null, null, true),
}

window.resolveLocalFileSystemURL = function(url, success, error)
{
	if (url)
	{
		var path = url.split('/');
		if (path && path.length > 0 && path[0] == 'file:')
		{
			var entry = window.inMemFileSystem.root
			for(var i = 1; i < path.length; i++)
			{
				entry = entry._getEntry(path[i], false, false, i < path.length-1)
				if (!entry) 
				{
					error && error({code:FileError.NOT_FOUND_ERR})
					return
				}
			}

			success && success(entry)
			return
		}
	}

	error && error(FileError.SECURITY_ERR)
}

// install InMemory file system
window.FileReader = InMemFileReader
window.Blob = InMemBlob
window.requestFileSystem = function(type, grantedBytes, success, error) {
	success && success(window.inMemFileSystem)
}
window.storageInfo = {
		queryUsageAndQuota: function(storageType, success, error) {success && success(0, 2147483647) }
}
