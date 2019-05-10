## Installation

1. Grab the latest `.hpi` file from [releases](https://github.com/RikudouSage/JenkinsSystemSshPlugin/releases/latest).
2. Install the plugin to Jenkins:
    - Click `Manage Jenkins` -> `Manage Plugins` -> `Advanced`
    - In the section titled `Upload Plugin` select the compiled file
    - Click upload and it's done

### From source

1. Download the source code:

    `git clone https://github.com/RikudouSage/JenkinsSystemSshPlugin`

2. Compile the plugin using maven:
    - `cd JenkinsSystemSshPlugin`
    - `mvn package`
    
3. Grab the compiled `.hpi` file located in `target/system-ssh-plugin.hpi` and upload it to
Jenkins.