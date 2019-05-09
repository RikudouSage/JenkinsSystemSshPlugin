## Installation

### From source

1. Download the source code:

    `git clone https://github.com/RikudouSage/JenkinsSystemSshPlugin`

2. Compile the plugin using maven:
    - `cd JenkinsSystemSshPlugin`
    - `mvn package`
    
3. Install the plugin to Jenkins:
    - Click `Manage Jenkins` -> `Manage Plugins` -> `Advanced`
    - In the section titled `Upload Plugin` select the compiled file
        - The file should be located at `target/system-ssh-plugin.hpi` in the cloned sources
    - Click upload and it's done

