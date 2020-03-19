eclipse_sources.target is a target file where we generate a p2 site which will be used in the pom files for the tycho build.

locally you should just export that sources target to a directory that you use then as your local p2 site (so you are not depending on remote sites)
