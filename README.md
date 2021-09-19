# Godot Build Manager

## Features
* Addons publishing
* Publishing to file repository
* Publishing to webdav repository
* Publishing to several repositories
* Transitive dependencies system
* The ability to define internal dependencies, for not publishing them to artifact

## Using
Execute `gorep` and all tasks separating by space. For example `gorep publish_central publish_remote`.
You can find all available tasks executing `gorep tasks`.

### Init project setup
Just execute `gorep init` Creates gorep project file

## Defaults tasks
* `dependencies` - Prints dependencies tree
* `tasks` - Prints Tasks list
* `publish` - Publishing to all repositories
* `build` - Builds tar.gz file with addon
* `config` - Generates plugin.cfg file
* `check` - Copy all dependencies to `/addons`

## Gorep Project file
Project file placed in root of your Godot project. It's name is `gorep_project.json`.
It is simple json file.<br>
### Project file struct
`{`<br>
`"name"`:`string` - Name of project. Using for publication to repositories<br>
`"title"`:`string` - Plugin title. Display in Godo plugin manager. Optional. Default value is `name` of project<br>
`"version"`:`string` - Project version. Using for publication to repositories. Optional. Default value `0.1`<br>
`"dependencies":[` - Dependency array. Optional<br>
&nbsp;&nbsp;`{`<br>
&nbsp;&nbsp;&nbsp;&nbsp;`"name"`:`string` - dependency name<br>
&nbsp;&nbsp;&nbsp;&nbsp;`"version"`:`string` - dependency version<br>
&nbsp;&nbsp;&nbsp;&nbsp;`"type"`:`string` - dependency type. Can be `EXTERNAL` or `INTERNAL`. Optional. Default value `EXTERNAL`<br>
&nbsp;&nbsp;`}`<br>
`]`<br>
`"repositories":[` - List of repositories. Optional<br>
&nbsp;&nbsp;`"name"`:`string` - Repository name<br>
&nbsp;&nbsp;`"path"`:`string` - Repository path. Can be local path or remote url<br>
&nbsp;&nbsp;`"type"`:`string` - Repository type. Can be `LOCAL` or `WEBDAV`<br>
&nbsp;&nbsp;`"basicAuth":{` - Basic Authorization. Optional<br>
&nbsp;&nbsp;&nbsp;&nbsp;`"login"`:`string` - Login Name<br>
&nbsp;&nbsp;&nbsp;&nbsp;`"password"`:`string` - Password<br>
&nbsp;&nbsp;`}`<br>
`]`<br>
`}`